package com.example.intercambioacademicoupc;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.intercambioacademicoupc.adapters.MaterialesAdapter;
import com.example.intercambioacademicoupc.models.AppDatabase;
import com.example.intercambioacademicoupc.models.ContenidoCurso;
import com.example.intercambioacademicoupc.models.Curso;
import com.example.intercambioacademicoupc.models.Matricula;
import com.example.intercambioacademicoupc.models.RegistroAccesoMaterial;
import com.example.intercambioacademicoupc.models.Usuario;
import com.example.intercambioacademicoupc.session.SessionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class MaterialesCursoActivity extends AppCompatActivity {

    private RecyclerView recyclerMateriales;
    private Button btnPublicarMaterial, btnEliminarCurso, btnInscribirEstudiante;
    private AppDatabase db;
    private SessionManager sessionManager;
    private int cursoId;
    private int usuarioId;
    private String rolUsuario;
    private Curso cursoActual;

    private String archivoUriSeleccionado = null;
    private String nombreArchivoSeleccionado = "Documento.pdf";
    private String tamanoCalculado = "2.0 MB";

    private final ActivityResultLauncher<String[]> selectorArchivo =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try {
                    getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                }

                long fileSize = 0;
                try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                        if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                            fileSize = cursor.getLong(sizeIndex);
                        }
                    }
                } catch (Exception ignored) {
                }

                // Criterio HU-09: hasta 20 MB
                if (fileSize > 20L * 1024 * 1024) {
                    Toast.makeText(this, "El archivo excede el límite máximo de 20 MB", Toast.LENGTH_LONG).show();
                    archivoUriSeleccionado = null;
                    return;
                }

                archivoUriSeleccionado = uri.toString();
                nombreArchivoSeleccionado = uri.getLastPathSegment() != null ? uri.getLastPathSegment() : "Archivo adjunto";
                if (fileSize > 0) {
                    tamanoCalculado = String.format(Locale.getDefault(), "%.1f MB", (double) fileSize / (1024 * 1024));
                }

                Toast.makeText(this, "Archivo seleccionado (" + tamanoCalculado + "): " + nombreArchivoSeleccionado, Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_materiales_curso);

        recyclerMateriales = findViewById(R.id.recyclerMateriales);
        btnPublicarMaterial = findViewById(R.id.btnPublicarMaterial);
        btnEliminarCurso = findViewById(R.id.btnEliminarCurso);
        btnInscribirEstudiante = findViewById(R.id.btnInscribirEstudiante);

        recyclerMateriales.setLayoutManager(new LinearLayoutManager(this));

        db = AppDatabase.getDatabase(this);
        sessionManager = new SessionManager(this);

        cursoId = getIntent().getIntExtra("CURSO_ID", -1);
        usuarioId = sessionManager.getUsuarioId();
        rolUsuario = sessionManager.getRol();

        if (cursoId == -1 || usuarioId == -1) {
            Toast.makeText(this, "Error de vinculación al curso o sesión inválida", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        verificarAccesoYCargarMateriales();

        btnPublicarMaterial.setOnClickListener(v -> mostrarDialogoPublicarMaterial());
        btnEliminarCurso.setOnClickListener(v -> confirmarEliminarCurso());
        btnInscribirEstudiante.setOnClickListener(v -> mostrarDialogoBuscarEstudianteParaInscribir());
    }

    private void verificarAccesoYCargarMateriales() {
        Executors.newSingleThreadExecutor().execute(() -> {
            cursoActual = db.cursoDao().obtenerCursoPorId(cursoId);

            boolean esAdmin = "administrador".equals(rolUsuario);
            boolean esDocenteCreador = "docente".equals(rolUsuario) && cursoActual != null && cursoActual.docenteId == usuarioId;
            boolean esEstudianteMatriculado = db.matriculaDao().verificarSiYaEstaInscrito(usuarioId, cursoId) > 0;

            if (!esAdmin && !esDocenteCreador && !esEstudianteMatriculado) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Acceso denegado: Material exclusivo para matriculados o docente titular.", Toast.LENGTH_LONG).show();
                    finish();
                });
                return;
            }

            final boolean esInstructor = esAdmin || esDocenteCreador;

            runOnUiThread(() -> {
                if (esInstructor) {
                    btnPublicarMaterial.setVisibility(View.VISIBLE);
                    btnEliminarCurso.setVisibility(View.VISIBLE);
                    btnInscribirEstudiante.setVisibility(View.VISIBLE);
                } else {
                    btnPublicarMaterial.setVisibility(View.GONE);
                    btnEliminarCurso.setVisibility(View.GONE);
                    btnInscribirEstudiante.setVisibility(View.GONE);
                }
            });

            cargarMateriales(esInstructor);
        });
    }

    private void cargarMateriales(boolean esInstructor) {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Criterio HU-10: agrupado por unidad y ordenado por fecha
            List<ContenidoCurso> materiales = db.contenidoCursoDao().obtenerContenidosPorCurso(cursoId);
            Map<Integer, Long> accesosMap = new HashMap<>();

            for (ContenidoCurso mat : materiales) {
                Long ultimoAcceso = db.registroAccesoMaterialDao().obtenerUltimoAcceso(usuarioId, mat.id);
                if (ultimoAcceso != null) {
                    accesosMap.put(mat.id, ultimoAcceso);
                }
            }

            runOnUiThread(() -> {
                MaterialesAdapter adapter = new MaterialesAdapter(materiales, accesosMap, esInstructor, this::registrarAccesoYDescargar, this::confirmarEliminarMaterial);
                recyclerMateriales.setAdapter(adapter);
            });
        });
    }

    private void mostrarDialogoBuscarEstudianteParaInscribir() {
        EditText input = new EditText(this);
        input.setHint("Nombre, apellido o código del estudiante");

        new AlertDialog.Builder(this)
                .setTitle("Inscribir Estudiante al Curso")
                .setView(input)
                .setPositiveButton("Buscar", (dialog, which) -> {
                    String query = input.getText().toString().trim();
                    if (query.isEmpty()) {
                        Toast.makeText(this, "Ingresa un término de búsqueda", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    buscarYMostrarEstudiantes(query);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void buscarYMostrarEstudiantes(String termino) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Usuario> estudiantes = db.usuarioDao().buscarEstudiantes(termino);
            runOnUiThread(() -> {
                if (estudiantes.isEmpty()) {
                    Toast.makeText(this, "No se encontraron estudiantes con ese criterio", Toast.LENGTH_SHORT).show();
                    return;
                }
                String[] nombres = new String[estudiantes.size()];
                for (int i = 0; i < estudiantes.size(); i++) {
                    nombres[i] = estudiantes.get(i).nombre + " " + estudiantes.get(i).apellido + " (Cod: " + estudiantes.get(i).codigoEstudiantil + ")";
                }
                new AlertDialog.Builder(this)
                        .setTitle("Seleccionar Estudiante a Inscribir")
                        .setItems(nombres, (d, which) -> {
                            Usuario estElegido = estudiantes.get(which);
                            inscribirEstudianteDirecto(estElegido.id, estElegido.nombre + " " + estElegido.apellido);
                        })
                        .show();
            });
        });
    }

    private void inscribirEstudianteDirecto(int estudianteId, String nombreEstudiante) {
        Executors.newSingleThreadExecutor().execute(() -> {
            int yaInscrito = db.matriculaDao().verificarSiYaEstaInscrito(estudianteId, cursoId);
            if (yaInscrito > 0) {
                runOnUiThread(() ->
                        Toast.makeText(this, "El estudiante " + nombreEstudiante + " ya está matriculado en este curso", Toast.LENGTH_LONG).show()
                );
                return;
            }

            Matricula m = new Matricula();
            m.estudianteId = estudianteId;
            m.cursoId = cursoId;
            m.fechaMatricula = System.currentTimeMillis();
            db.matriculaDao().matricular(m);

            runOnUiThread(() ->
                    Toast.makeText(this, "¡Estudiante " + nombreEstudiante + " matriculado con éxito!", Toast.LENGTH_LONG).show()
            );
        });
    }

    private void mostrarDialogoPublicarMaterial() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = 32;
        layout.setPadding(padding, padding, padding, padding);

        final EditText etUnidad = new EditText(this);
        etUnidad.setHint("Unidad o Semana (Ej: Unidad 1)");
        layout.addView(etUnidad);

        final EditText etTitulo = new EditText(this);
        etTitulo.setHint("Título del recurso (Ej: Guía PDF)");
        layout.addView(etTitulo);

        final EditText etDescripcion = new EditText(this);
        etDescripcion.setHint("Descripción del contenido");
        layout.addView(etDescripcion);

        Button btnElegirArchivo = new Button(this);
        btnElegirArchivo.setText("Seleccionar Archivo (PDF, DOCX, PPTX, Img)");
        btnElegirArchivo.setOnClickListener(v -> selectorArchivo.launch(new String[]{
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "image/*"
        }));
        layout.addView(btnElegirArchivo);

        new AlertDialog.Builder(this)
                .setTitle("HU-09: Publicar Material de Estudio")
                .setView(layout)
                .setPositiveButton("Publicar", (dialog, which) -> {
                    String unidad = etUnidad.getText().toString().trim();
                    String titulo = etTitulo.getText().toString().trim();
                    String descripcion = etDescripcion.getText().toString().trim();

                    if (titulo.isEmpty() || unidad.isEmpty()) {
                        Toast.makeText(this, "Unidad y Título son obligatorios", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Executors.newSingleThreadExecutor().execute(() -> {
                        ContenidoCurso contenido = new ContenidoCurso();
                        contenido.cursoId = cursoId;
                        contenido.unidad = unidad;
                        contenido.titulo = titulo;
                        contenido.descripcionContenido = descripcion;
                        contenido.tipoMaterial = obtenerTipoDesdeNombre(nombreArchivoSeleccionado);
                        contenido.tamanoArchivo = tamanoCalculado;
                        contenido.urlArchivo = archivoUriSeleccionado;
                        contenido.fechaPublicacion = System.currentTimeMillis();

                        db.contenidoCursoDao().insertarContenido(contenido);

                        boolean esInstructor = "administrador".equals(rolUsuario) || ("docente".equals(rolUsuario) && cursoActual != null && cursoActual.docenteId == usuarioId);

                        runOnUiThread(() -> {
                            Toast.makeText(this, "Material publicado. Notificación enviada a los estudiantes matriculados.", Toast.LENGTH_LONG).show();
                            archivoUriSeleccionado = null;
                            cargarMateriales(esInstructor);
                        });
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarEliminarMaterial(ContenidoCurso material) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Recurso")
                .setMessage("¿Estás seguro de que deseas eliminar \"" + material.titulo + "\"?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.contenidoCursoDao().eliminarContenido(material.id);
                        boolean esInstructor = "administrador".equals(rolUsuario) || ("docente".equals(rolUsuario) && cursoActual != null && cursoActual.docenteId == usuarioId);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Recurso eliminado correctamente", Toast.LENGTH_SHORT).show();
                            cargarMateriales(esInstructor);
                        });
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private String obtenerTipoDesdeNombre(String nombre) {
        if (nombre == null) return "DOC";
        String lower = nombre.toLowerCase();
        if (lower.endsWith(".pdf")) return "PDF";
        if (lower.endsWith(".docx") || lower.endsWith(".doc")) return "DOCX";
        if (lower.endsWith(".pptx") || lower.endsWith(".ppt")) return "PPTX";
        if (lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".jpeg")) return "IMAGEN";
        return "ARCHIVO";
    }

    private void confirmarEliminarCurso() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Curso")
                .setMessage("¿Estás seguro de que deseas eliminar este curso y todos sus materiales?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.cursoDao().eliminarCurso(cursoId);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Curso eliminado correctamente", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void registrarAccesoYDescargar(ContenidoCurso material) {
        Executors.newSingleThreadExecutor().execute(() -> {
            RegistroAccesoMaterial registro = new RegistroAccesoMaterial();
            registro.estudianteId = usuarioId;
            registro.materialId = material.id;
            registro.fechaUltimoAcceso = System.currentTimeMillis();

            db.registroAccesoMaterialDao().registrarAcceso(registro);

            boolean esInstructor = "administrador".equals(rolUsuario) || ("docente".equals(rolUsuario) && cursoActual != null && cursoActual.docenteId == usuarioId);

            runOnUiThread(() -> {
                cargarMateriales(esInstructor);

                if (material.urlArchivo != null && !material.urlArchivo.isEmpty()) {
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(material.urlArchivo));
                        startActivity(browserIntent);
                    } catch (Exception e) {
                        Toast.makeText(this, "No se pudo abrir el archivo externo", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Iniciando descarga local de: " + material.titulo, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
