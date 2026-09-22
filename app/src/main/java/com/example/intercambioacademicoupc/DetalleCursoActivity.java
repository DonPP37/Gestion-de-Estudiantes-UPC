package com.example.intercambioacademicoupc;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.intercambioacademicoupc.models.AppDatabase;
import com.example.intercambioacademicoupc.models.ContenidoCurso;
import com.example.intercambioacademicoupc.models.Curso;
import com.example.intercambioacademicoupc.session.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetalleCursoActivity extends AppCompatActivity {

    private TextView tvNombre, tvCodigoPeriodo, tvDescripcion, tvMaterialesVacios, tvMaterialesLista;
    private LinearLayout layoutAddContenido;
    private EditText etTitulo, etDescContenido;
    private Spinner spTipoMaterial;
    private Button btnSubirMaterial, btnEditarCurso;

    private AppDatabase db;
    private SessionManager sessionManager;
    private ExecutorService executorService;
    private int cursoId;
    private Curso cursoActualObjeto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_curso);

        db = AppDatabase.getDatabase(getApplicationContext());
        sessionManager = new SessionManager(this);
        executorService = Executors.newSingleThreadExecutor();

        cursoId = getIntent().getIntExtra("CURSO_ID", -1);

        vincularVistas();
        configurarSpinner();

        if (cursoId != -1) {
            cargarDetalleCurso();
            cargarMateriales();
        }

        // Criterio: Solo el docente creador o encargado puede agregar contenido
        btnEditarCurso = findViewById(R.id.btn_editar_info_curso);
        if ("docente".equals(sessionManager.getRol())) {
            layoutAddContenido.setVisibility(View.VISIBLE);
            if (btnEditarCurso != null) {
                btnEditarCurso.setVisibility(View.VISIBLE);
                btnEditarCurso.setOnClickListener(v -> mostrarDialogoEdicion());
            }
        }

        btnSubirMaterial.setOnClickListener(v -> subirMaterial());
    }

    private void vincularVistas() {
        tvNombre = findViewById(R.id.tv_det_nombre);
        tvCodigoPeriodo = findViewById(R.id.tv_det_codigo_periodo);
        tvDescripcion = findViewById(R.id.tv_det_descripcion);
        tvMaterialesVacios = findViewById(R.id.tv_materiales_vacios);
        tvMaterialesLista = findViewById(R.id.tv_materiales_lista);
        
        layoutAddContenido = findViewById(R.id.layout_add_contenido);
        etTitulo = findViewById(R.id.et_cont_titulo);
        etDescContenido = findViewById(R.id.et_cont_descripcion);
        spTipoMaterial = findViewById(R.id.sp_cont_tipo);
        btnSubirMaterial = findViewById(R.id.btn_subir_material);
    }

    private void configurarSpinner() {
        String[] tipos = {"PDF", "DOCX", "PPTX", "Imagen", "Texto Guía"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tipos);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTipoMaterial.setAdapter(adapter);
    }

    private void cargarDetalleCurso() {
        executorService.execute(() -> {
            // Buscaremos los cursos por id, creamos un helper rápido mapeado
            List<Curso> todos = db.cursoDao().obtenerTodosLosCursos();
            Curso actual = null;
            for (Curso c : todos) {
                if (c.id == cursoId) {
                    actual = c;
                    break;
                }
            }

            if (actual != null) {
                cursoActualObjeto = actual;
                Curso finalActual = actual;
                runOnUiThread(() -> {
                    tvNombre.setText(finalActual.nombre);
                    tvCodigoPeriodo.setText("Código: " + finalActual.codigo + " | Periodo: " + finalActual.periodo + " | Estado: " + finalActual.estado.toUpperCase());
                    tvDescripcion.setText(finalActual.descripcion);
                });
            }
        });
    }

    private void cargarMateriales() {
        executorService.execute(() -> {
            List<ContenidoCurso> materiales = db.contenidoCursoDao().obtenerContenidosPorCurso(cursoId);
            runOnUiThread(() -> {
                if (materiales == null || materiales.isEmpty()) {
                    tvMaterialesVacios.setVisibility(View.VISIBLE);
                    tvMaterialesLista.setVisibility(View.GONE);
                } else {
                    tvMaterialesVacios.setVisibility(View.GONE);
                    tvMaterialesLista.setVisibility(View.VISIBLE);

                    StringBuilder sb = new StringBuilder();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    for (ContenidoCurso c : materiales) {
                        sb.append("📌 [").append(c.tipoMaterial).append("] ").append(c.titulo)
                          .append("\n📄 Detalle: ").append(c.descripcionContenido)
                          .append("\n📅 Publicado: ").append(sdf.format(new Date(c.fechaPublicacion)))
                          .append("\n----------------------------------------\n");
                    }
                    tvMaterialesLista.setText(sb.toString());
                }
            });
        });
    }

    private void subirMaterial() {
        String titulo = etTitulo.getText().toString().trim();
        String desc = etDescContenido.getText().toString().trim();
        String tipo = spTipoMaterial.getSelectedItem().toString();

        if (titulo.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Completa el título y descripción del material", Toast.LENGTH_SHORT).show();
            return;
        }

        executorService.execute(() -> {
            ContenidoCurso cont = new ContenidoCurso();
            cont.cursoId = cursoId;
            cont.titulo = titulo;
            cont.descripcionContenido = desc;
            cont.tipoMaterial = tipo;
            cont.fechaPublicacion = System.currentTimeMillis();

            db.contenidoCursoDao().insertarContenido(cont);

            runOnUiThread(() -> {
                Toast.makeText(this, "¡Material publicado con éxito!", Toast.LENGTH_SHORT).show();
                etTitulo.setText("");
                etDescContenido.setText("");
                cargarMateriales(); // Recargar vista
            });
        });
    }

    private void mostrarDialogoEdicion() {
        if (cursoActualObjeto == null) return;

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Editar Asignatura");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText inputNombre = new EditText(this);
        inputNombre.setHint("Nombre del curso");
        inputNombre.setText(cursoActualObjeto.nombre);
        layout.addView(inputNombre);

        final EditText inputDesc = new EditText(this);
        inputDesc.setHint("Descripción");
        inputDesc.setText(cursoActualObjeto.descripcion);
        layout.addView(inputDesc);

        final EditText inputCupo = new EditText(this);
        inputCupo.setHint("Cupo Máximo");
        inputCupo.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        inputCupo.setText(String.valueOf(cursoActualObjeto.cupoMaximo));
        layout.addView(inputCupo);

        builder.setView(layout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nuevoNombre = inputNombre.getText().toString().trim();
            String nuevaDesc = inputDesc.getText().toString().trim();
            String nuevoCupoStr = inputCupo.getText().toString().trim();

            if (nuevoNombre.isEmpty() || nuevaDesc.isEmpty() || nuevoCupoStr.isEmpty()) {
                Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            int cupo = Integer.parseInt(nuevoCupoStr);
            executorService.execute(() -> {
                db.cursoDao().actualizarCurso(cursoId, nuevoNombre, nuevaDesc, cupo);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Asignatura actualizada correctamente", Toast.LENGTH_SHORT).show();
                    cargarDetalleCurso(); // Refrescar los textos en pantalla
                });
            });
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }
}