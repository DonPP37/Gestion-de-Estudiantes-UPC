package com.example.intercambioacademicoupc;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.intercambioacademicoupc.adapters.MaterialesAdapter;
import com.example.intercambioacademicoupc.models.ActividadCurso;
import com.example.intercambioacademicoupc.models.AppDatabase;
import com.example.intercambioacademicoupc.models.ContenidoCurso;
import com.example.intercambioacademicoupc.models.Curso;
import com.example.intercambioacademicoupc.models.Entrega;
import com.example.intercambioacademicoupc.models.Matricula;
import com.example.intercambioacademicoupc.models.RegistroAccesoMaterial;
import com.example.intercambioacademicoupc.models.Usuario;
import com.example.intercambioacademicoupc.session.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class MaterialesCursoActivity extends AppCompatActivity {

    private TextView tvCursoNombre, tvCursoCodigoPeriodo, tvCursoDescripcion;
    private RecyclerView recyclerEspacioCurso;
    private Button btnTabMateriales, btnTabActividades, btnTabConfig, btnAccionEspacioCurso;

    private AppDatabase db;
    private SessionManager sessionManager;
    private int cursoId;
    private int usuarioId;
    private String rolUsuario;
    private Curso cursoActual;
    private boolean esInstructor = false;
    private int pestanaActual = 0; // 0 = Materiales, 1 = Actividades, 2 = Gestión

    private String archivoUriSeleccionado = null;
    private String nombreArchivoSeleccionado = "Documento.pdf";
    private String tamanoCalculado = "2.0 MB";

    private final SimpleDateFormat sdfFechaHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

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

        tvCursoNombre = findViewById(R.id.tvCursoNombre);
        tvCursoCodigoPeriodo = findViewById(R.id.tvCursoCodigoPeriodo);
        tvCursoDescripcion = findViewById(R.id.tvCursoDescripcion);
        recyclerEspacioCurso = findViewById(R.id.recyclerEspacioCurso);

        btnTabMateriales = findViewById(R.id.btnTabMateriales);
        btnTabActividades = findViewById(R.id.btnTabActividades);
        btnTabConfig = findViewById(R.id.btnTabConfig);
        btnAccionEspacioCurso = findViewById(R.id.btnAccionEspacioCurso);

        recyclerEspacioCurso.setLayoutManager(new LinearLayoutManager(this));

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

        verificarAccesoYConfigurarUI();

        btnTabMateriales.setOnClickListener(v -> cambiarPestaña(0));
        btnTabActividades.setOnClickListener(v -> cambiarPestaña(1));
        btnTabConfig.setOnClickListener(v -> cambiarPestaña(2));

        btnAccionEspacioCurso.setOnClickListener(v -> manejarAccionPestaña());
    }

    private void verificarAccesoYConfigurarUI() {
        Executors.newSingleThreadExecutor().execute(() -> {
            cursoActual = db.cursoDao().obtenerCursoPorId(cursoId);

            boolean esAdmin = "administrador".equals(rolUsuario);
            boolean esDocenteCreador = "docente".equals(rolUsuario) && cursoActual != null && cursoActual.docenteId == usuarioId;
            boolean esEstudianteMatriculado = db.matriculaDao().verificarSiYaEstaInscrito(usuarioId, cursoId) > 0;

            if (!esAdmin && !esDocenteCreador && !esEstudianteMatriculado) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Acceso denegado: Curso exclusivo para matriculados o docente titular.", Toast.LENGTH_LONG).show();
                    finish();
                });
                return;
            }

            esInstructor = esAdmin || esDocenteCreador;

            runOnUiThread(() -> {
                if (cursoActual != null) {
                    tvCursoNombre.setText(cursoActual.nombre);
                    tvCursoCodigoPeriodo.setText("Código: " + cursoActual.codigo + " • Periodo: " + cursoActual.periodo);
                    tvCursoDescripcion.setText(cursoActual.descripcion);
                }
                if (esInstructor) {
                    btnTabConfig.setVisibility(View.VISIBLE);
                } else {
                    btnTabConfig.setVisibility(View.GONE);
                }
                actualizarVistaPestaña();
            });
        });
    }

    private void cambiarPestaña(int pestana) {
        pestanaActual = pestana;
        btnTabMateriales.setBackgroundColor(Color.parseColor(pestanaActual == 0 ? "#1976D2" : "#757575"));
        btnTabActividades.setBackgroundColor(Color.parseColor(pestanaActual == 1 ? "#1976D2" : "#757575"));
        btnTabConfig.setBackgroundColor(Color.parseColor(pestanaActual == 2 ? "#1976D2" : "#757575"));
        actualizarVistaPestaña();
    }

    private void actualizarVistaPestaña() {
        if (pestanaActual == 0) {
            if (esInstructor) {
                btnAccionEspacioCurso.setVisibility(View.VISIBLE);
                btnAccionEspacioCurso.setText("+ Publicar Material");
            } else {
                btnAccionEspacioCurso.setVisibility(View.GONE);
            }
            cargarMateriales();
        } else if (pestanaActual == 1) {
            if (esInstructor) {
                btnAccionEspacioCurso.setVisibility(View.VISIBLE);
                btnAccionEspacioCurso.setText("+ Crear Actividad");
            } else {
                btnAccionEspacioCurso.setVisibility(View.VISIBLE);
                btnAccionEspacioCurso.setText("Entregar Tarea / Trabajo");
            }
            cargarActividades();
        } else if (pestanaActual == 2) {
            btnAccionEspacioCurso.setVisibility(View.GONE);
            cargarOpcionesGestion();
        }
    }

    private void manejarAccionPestaña() {
        if (pestanaActual == 0) {
            if (esInstructor) mostrarDialogoPublicarMaterial();
        } else if (pestanaActual == 1) {
            if (esInstructor) {
                mostrarDialogoCrearActividad();
            } else {
                mostrarDialogoEntregarTareaEstudiante();
            }
        }
    }

    private void cargarMateriales() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ContenidoCurso> materiales = db.contenidoCursoDao().obtenerContenidosPorCurso(cursoId);
            Map<Integer, Long> accesosMap = new HashMap<>();

            for (ContenidoCurso mat : materiales) {
                Long ultimoAcceso = db.registroAccesoMaterialDao().obtenerUltimoAcceso(usuarioId, mat.id);
                if (ultimoAcceso != null) {
                    accesosMap.put(mat.id, ultimoAcceso);
                }
            }

            runOnUiThread(() -> {
                MaterialesAdapter adapter = new MaterialesAdapter(materiales, accesosMap, esInstructor, material -> {
                    if (esMaterialVideo(material)) {
                        reproducirVideo(material);
                    } else {
                        registrarAccesoYDescargar(material);
                    }
                }, this::confirmarEliminarMaterial);
                recyclerEspacioCurso.setAdapter(adapter);
            });
        });
    }

    private void cargarActividades() {
        Executors.newSingleThreadExecutor().execute(() -> {
            long ahora = System.currentTimeMillis();
            List<ActividadCurso> actividades;
            if (esInstructor) {
                actividades = db.actividadCursoDao().obtenerActividadesPorCurso(cursoId);
            } else {
                List<ActividadCurso> todasEstudiante = db.actividadCursoDao().obtenerActividadesParaEstudiante(usuarioId, ahora);
                actividades = new ArrayList<>();
                for (ActividadCurso act : todasEstudiante) {
                    if (act.cursoId == cursoId) actividades.add(act);
                }
            }

            final List<ActividadCurso> finalActividades = actividades;
            runOnUiThread(() -> {
                recyclerEspacioCurso.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    @NonNull
                    @Override
                    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View v = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
                        return new RecyclerView.ViewHolder(v) {};
                    }

                    @Override
                    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                        ActividadCurso act = finalActividades.get(position);
                        TextView tv1 = holder.itemView.findViewById(android.R.id.text1);
                        TextView tv2 = holder.itemView.findViewById(android.R.id.text2);
                        String estadoVisibilidad = esInstructor ? (act.visible ? " [Visible]" : " [Oculta]") : "";
                        tv1.setText(act.titulo + " (" + act.porcentaje + "%)" + estadoVisibilidad);
                        tv2.setText("Abre: " + sdfFechaHora.format(new Date(act.fechaApertura)) + " | Cierra: " + sdfFechaHora.format(new Date(act.fechaCierre)));

                        holder.itemView.setOnClickListener(v -> {
                            if (esInstructor) {
                                mostrarOpcionesActividad(act);
                            } else {
                                mostrarDetalleActividadEstudiante(act);
                            }
                        });
                    }

                    @Override
                    public int getItemCount() {
                        return finalActividades.size();
                    }
                });
            });
        });
    }

    private void cargarOpcionesGestion() {
        List<String> opciones = new ArrayList<>();
        opciones.add("✏️ Editar información del curso");
        opciones.add("➕ Inscribir estudiante al curso");
        opciones.add("🗑️ Eliminar este curso");

        runOnUiThread(() -> {
            recyclerEspacioCurso.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                @NonNull
                @Override
                public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    View v = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
                    return new RecyclerView.ViewHolder(v) {};
                }

                @Override
                public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                    TextView tv = holder.itemView.findViewById(android.R.id.text1);
                    tv.setText(opciones.get(position));
                    tv.setTextSize(16f);
                    tv.setPadding(24, 24, 24, 24);

                    holder.itemView.setOnClickListener(v -> {
                        if (position == 0) mostrarDialogoEditarCurso();
                        else if (position == 1) mostrarDialogoBuscarEstudianteParaInscribir();
                        else if (position == 2) confirmarEliminarCurso();
                    });
                }

                @Override
                public int getItemCount() {
                    return opciones.size();
                }
            });
        });
    }

    private void mostrarDetalleActividadEstudiante(ActividadCurso act) {
        long ahora = System.currentTimeMillis();
        boolean vencida = ahora > act.fechaCierre;
        String estadoPlazo = vencida ? (act.aceptaFueraDePlazo ? "\n⚠️ Plazo vencido (Acepta fuera de plazo)" : "\n❌ Plazo vencido (No acepta fuera de plazo)") : "\n✅ Abierta";

        new AlertDialog.Builder(this)
                .setTitle(act.titulo)
                .setMessage("Instrucciones:\n" + act.instrucciones + "\n\nApertura: " + sdfFechaHora.format(new Date(act.fechaApertura)) + "\nCierre: " + sdfFechaHora.format(new Date(act.fechaCierre)) + "\n\nPeso en nota: " + act.porcentaje + "%" + estadoPlazo)
                .setPositiveButton("Entregar Tarea", (d, w) -> {
                    if (vencida && !act.aceptaFueraDePlazo) {
                        Toast.makeText(this, "Plazo vencido: No acepta entregas fuera de plazo.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    realizarEntrega(act.titulo);
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void realizarEntrega(String tituloActividad) {
        EditText input = new EditText(this);
        input.setHint("Enlace o comentarios del trabajo (Ej: https://github.com/...)");

        new AlertDialog.Builder(this)
                .setTitle("Entrega: " + tituloActividad)
                .setView(input)
                .setPositiveButton("Enviar Entrega", (dialog, which) -> {
                    String comentario = input.getText().toString().trim();
                    Executors.newSingleThreadExecutor().execute(() -> {
                        Entrega entrega = new Entrega();
                        entrega.cursoId = cursoId;
                        entrega.estudianteId = usuarioId;
                        entrega.tituloTarea = tituloActividad;
                        entrega.descripcionEntrega = comentario.isEmpty() ? "Entrega realizada" : comentario;
                        entrega.fechaEntrega = System.currentTimeMillis();
                        entrega.estado = "Entregado";
                        db.entregaDao().insertarEntrega(entrega);

                        runOnUiThread(() -> Toast.makeText(this, "¡Trabajo entregado con éxito!", Toast.LENGTH_LONG).show());
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoEntregarTareaEstudiante() {
        Executors.newSingleThreadExecutor().execute(() -> {
            long ahora = System.currentTimeMillis();
            List<ActividadCurso> todas = db.actividadCursoDao().obtenerActividadesParaEstudiante(usuarioId, ahora);
            List<ActividadCurso> delCurso = new ArrayList<>();
            for (ActividadCurso act : todas) {
                if (act.cursoId == cursoId) delCurso.add(act);
            }

            runOnUiThread(() -> {
                if (delCurso.isEmpty()) {
                    Toast.makeText(this, "No hay actividades abiertas disponibles para entrega en este curso", Toast.LENGTH_LONG).show();
                    return;
                }
                String[] nombres = new String[delCurso.size()];
                for (int i = 0; i < delCurso.size(); i++) {
                    nombres[i] = delCurso.get(i).titulo + " (" + delCurso.get(i).porcentaje + "%)";
                }
                new AlertDialog.Builder(this)
                        .setTitle("Seleccionar Actividad a Entregar")
                        .setItems(nombres, (d, which) -> {
                            ActividadCurso actElegida = delCurso.get(which);
                            if (ahora > actElegida.fechaCierre && !actElegida.aceptaFueraDePlazo) {
                                Toast.makeText(this, "Plazo vencido: No acepta entregas fuera de plazo.", Toast.LENGTH_LONG).show();
                                return;
                            }
                            realizarEntrega(actElegida.titulo);
                        })
                        .show();
            });
        });
    }

    private boolean esMaterialVideo(ContenidoCurso material) {
        if (material.tipoMaterial != null && material.tipoMaterial.equalsIgnoreCase("VIDEO")) return true;
        if (material.urlArchivo != null) {
            String lower = material.urlArchivo.toLowerCase();
            return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".mkv") || lower.endsWith(".3gp");
        }
        return false;
    }

    private void reproducirVideo(ContenidoCurso material) {
        if (material.urlArchivo == null || material.urlArchivo.isEmpty()) {
            Toast.makeText(this, "No hay archivo de video disponible", Toast.LENGTH_SHORT).show();
            return;
        }
        VideoView videoView = new VideoView(this);
        try {
            videoView.setVideoURI(Uri.parse(material.urlArchivo));
            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(videoView);
            videoView.setMediaController(mediaController);

            new AlertDialog.Builder(this)
                    .setTitle("Reproductor de Video: " + material.titulo)
                    .setView(videoView)
                    .setPositiveButton("Cerrar", (d, w) -> videoView.stopPlayback())
                    .setOnCancelListener(d -> videoView.stopPlayback())
                    .show();

            videoView.start();
        } catch (Exception e) {
            Toast.makeText(this, "Error al reproducir el video", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarDialogoEditarCurso() {
        if (cursoActual == null) return;
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int p = 32;
        layout.setPadding(p, p, p, p);

        final EditText etNombre = new EditText(this);
        etNombre.setText(cursoActual.nombre);
        layout.addView(etNombre);

        final EditText etCodigo = new EditText(this);
        etCodigo.setText(cursoActual.codigo);
        layout.addView(etCodigo);

        final EditText etDescripcion = new EditText(this);
        etDescripcion.setText(cursoActual.descripcion);
        layout.addView(etDescripcion);

        final EditText etPeriodo = new EditText(this);
        etPeriodo.setText(cursoActual.periodo);
        layout.addView(etPeriodo);

        final EditText etCupo = new EditText(this);
        etCupo.setText(String.valueOf(cursoActual.cupoMaximo));
        layout.addView(etCupo);

        new AlertDialog.Builder(this)
                .setTitle("Administrar / Editar Curso")
                .setView(layout)
                .setPositiveButton("Guardar Cambios", (dialog, which) -> {
                    cursoActual.nombre = etNombre.getText().toString().trim();
                    cursoActual.codigo = etCodigo.getText().toString().trim();
                    cursoActual.descripcion = etDescripcion.getText().toString().trim();
                    cursoActual.periodo = etPeriodo.getText().toString().trim();
                    try {
                        cursoActual.cupoMaximo = Integer.parseInt(etCupo.getText().toString().trim());
                    } catch (Exception ignored) {}

                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.cursoDao().actualizarCurso(cursoActual);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Curso actualizado correctamente", Toast.LENGTH_LONG).show();
                            tvCursoNombre.setText(cursoActual.nombre);
                            tvCursoCodigoPeriodo.setText("Código: " + cursoActual.codigo + " • Periodo: " + cursoActual.periodo);
                            tvCursoDescripcion.setText(cursoActual.descripcion);
                        });
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarSelectorFechaHora(long initialMillis, OnFechaHoraSelectedListener listener) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(initialMillis > 0 ? initialMillis : System.currentTimeMillis());

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                cal.set(Calendar.MINUTE, minute);
                listener.onSelected(cal.getTimeInMillis());
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();

        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private interface OnFechaHoraSelectedListener {
        void onSelected(long millis);
    }

    private void mostrarDialogoCrearActividad() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int p = 32;
        layout.setPadding(p, p, p, p);

        final EditText etTitulo = new EditText(this);
        etTitulo.setHint("Título de la Actividad");
        layout.addView(etTitulo);

        final EditText etInstrucciones = new EditText(this);
        etInstrucciones.setHint("Instrucciones detalladas");
        layout.addView(etInstrucciones);

        final EditText etPorcentaje = new EditText(this);
        etPorcentaje.setHint("Porcentaje de la nota (Ej: 20)");
        etPorcentaje.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etPorcentaje);

        final long[] apertura = {System.currentTimeMillis()};
        final long[] cierre = {System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000)};

        Button btnApertura = new Button(this);
        btnApertura.setText("Apertura: " + sdfFechaHora.format(new Date(apertura[0])));
        btnApertura.setOnClickListener(v -> mostrarSelectorFechaHora(apertura[0], millis -> {
            apertura[0] = millis;
            btnApertura.setText("Apertura: " + sdfFechaHora.format(new Date(millis)));
        }));
        layout.addView(btnApertura);

        Button btnCierre = new Button(this);
        btnCierre.setText("Cierre: " + sdfFechaHora.format(new Date(cierre[0])));
        btnCierre.setOnClickListener(v -> mostrarSelectorFechaHora(cierre[0], millis -> {
            cierre[0] = millis;
            btnCierre.setText("Cierre: " + sdfFechaHora.format(new Date(millis)));
        }));
        layout.addView(btnCierre);

        final CheckBox cbFueraPlazo = new CheckBox(this);
        cbFueraPlazo.setText("Acepta entregas fuera de plazo");
        layout.addView(cbFueraPlazo);

        final CheckBox cbVisible = new CheckBox(this);
        cbVisible.setText("Visible para los estudiantes (Publicada)");
        cbVisible.setChecked(true);
        layout.addView(cbVisible);

        Button btnAdjuntar = new Button(this);
        btnAdjuntar.setText("Adjuntar Archivo / Recurso");
        btnAdjuntar.setOnClickListener(v -> selectorArchivo.launch(new String[]{"*/*"}));
        layout.addView(btnAdjuntar);

        new AlertDialog.Builder(this)
                .setTitle("Crear Actividad o Trabajo")
                .setView(layout)
                .setPositiveButton("Publicar Actividad", (dialog, which) -> {
                    String titulo = etTitulo.getText().toString().trim();
                    String instrucciones = etInstrucciones.getText().toString().trim();
                    String porcentajeStr = etPorcentaje.getText().toString().trim();

                    if (titulo.isEmpty() || porcentajeStr.isEmpty()) {
                        Toast.makeText(this, "Título y porcentaje son obligatorios", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int porcentaje;
                    try {
                        porcentaje = Integer.parseInt(porcentajeStr);
                    } catch (Exception e) {
                        porcentaje = 0;
                    }

                    if (porcentaje <= 0 || porcentaje > 100) {
                        Toast.makeText(this, "El porcentaje debe estar entre 1 y 100", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (cierre[0] <= apertura[0]) {
                        Toast.makeText(this, "La fecha de cierre debe ser posterior a la de apertura", Toast.LENGTH_LONG).show();
                        return;
                    }

                    final int finalPorcentaje = porcentaje;
                    final String archivoAdjunto = archivoUriSeleccionado;
                    final boolean esVisible = cbVisible.isChecked();
                    final long finalApertura = apertura[0];
                    final long finalCierre = cierre[0];

                    Executors.newSingleThreadExecutor().execute(() -> {
                        Integer sumaActual = db.actividadCursoDao().obtenerSumaPorcentajes(cursoId);
                        int total = sumaActual != null ? sumaActual : 0;
                        if (total + finalPorcentaje > 100) {
                            runOnUiThread(() -> Toast.makeText(this, "Error: La suma de porcentajes (" + (total + finalPorcentaje) + "%) supera el 100%.", Toast.LENGTH_LONG).show());
                            return;
                        }

                        ActividadCurso actividad = new ActividadCurso();
                        actividad.cursoId = cursoId;
                        actividad.titulo = titulo;
                        actividad.instrucciones = instrucciones;
                        actividad.porcentaje = finalPorcentaje;
                        actividad.aceptaFueraDePlazo = cbFueraPlazo.isChecked();
                        actividad.archivoUrl = archivoAdjunto;
                        actividad.fechaApertura = finalApertura;
                        actividad.fechaCierre = finalCierre;
                        actividad.visible = esVisible;

                        db.actividadCursoDao().insertarActividad(actividad);

                        runOnUiThread(() -> {
                            Toast.makeText(this, "Actividad creada con éxito", Toast.LENGTH_LONG).show();
                            archivoUriSeleccionado = null;
                            if (pestanaActual == 1) cargarActividades();
                        });
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarOpcionesActividad(ActividadCurso actividad) {
        CharSequence[] opciones = {"Editar / Adjuntar Archivo / Fechas / Visibilidad", "Eliminar Actividad"};
        new AlertDialog.Builder(this)
                .setTitle("Acciones: " + actividad.titulo)
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        mostrarDialogoEditarActividad(actividad);
                    } else if (which == 1) {
                        eliminarActividad(actividad);
                    }
                })
                .show();
    }

    private void mostrarDialogoEditarActividad(ActividadCurso actividad) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int p = 32;
        layout.setPadding(p, p, p, p);

        final EditText etTitulo = new EditText(this);
        etTitulo.setText(actividad.titulo);
        layout.addView(etTitulo);

        final EditText etInstrucciones = new EditText(this);
        etInstrucciones.setText(actividad.instrucciones);
        layout.addView(etInstrucciones);

        final EditText etPorcentaje = new EditText(this);
        etPorcentaje.setText(String.valueOf(actividad.porcentaje));
        etPorcentaje.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etPorcentaje);

        final long[] apertura = {actividad.fechaApertura};
        final long[] cierre = {actividad.fechaCierre};

        Button btnApertura = new Button(this);
        btnApertura.setText("Apertura: " + sdfFechaHora.format(new Date(apertura[0])));
        btnApertura.setOnClickListener(v -> mostrarSelectorFechaHora(apertura[0], millis -> {
            apertura[0] = millis;
            btnApertura.setText("Apertura: " + sdfFechaHora.format(new Date(millis)));
        }));
        layout.addView(btnApertura);

        Button btnCierre = new Button(this);
        btnCierre.setText("Cierre: " + sdfFechaHora.format(new Date(cierre[0])));
        btnCierre.setOnClickListener(v -> mostrarSelectorFechaHora(cierre[0], millis -> {
            cierre[0] = millis;
            btnCierre.setText("Cierre: " + sdfFechaHora.format(new Date(millis)));
        }));
        layout.addView(btnCierre);

        final CheckBox cbFueraPlazo = new CheckBox(this);
        cbFueraPlazo.setText("Acepta entregas fuera de plazo");
        cbFueraPlazo.setChecked(actividad.aceptaFueraDePlazo);
        layout.addView(cbFueraPlazo);

        final CheckBox cbVisible = new CheckBox(this);
        cbVisible.setText("Visible para los estudiantes (Publicada)");
        cbVisible.setChecked(actividad.visible);
        layout.addView(cbVisible);

        Button btnAdjuntar = new Button(this);
        btnAdjuntar.setText(actividad.archivoUrl != null ? "Archivo adjunto (Cambiar)" : "Adjuntar Archivo / Recurso");
        btnAdjuntar.setOnClickListener(v -> selectorArchivo.launch(new String[]{"*/*"}));
        layout.addView(btnAdjuntar);

        new AlertDialog.Builder(this)
                .setTitle("Editar Actividad y Fechas")
                .setView(layout)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String titulo = etTitulo.getText().toString().trim();
                    String instrucciones = etInstrucciones.getText().toString().trim();
                    String porcentajeStr = etPorcentaje.getText().toString().trim();

                    if (titulo.isEmpty() || porcentajeStr.isEmpty()) {
                        Toast.makeText(this, "Título y porcentaje obligatorios", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int porcentaje;
                    try {
                        porcentaje = Integer.parseInt(porcentajeStr);
                    } catch (Exception e) {
                        porcentaje = actividad.porcentaje;
                    }

                    if (cierre[0] <= apertura[0]) {
                        Toast.makeText(this, "La fecha de cierre debe ser posterior a la de apertura", Toast.LENGTH_LONG).show();
                        return;
                    }

                    actividad.titulo = titulo;
                    actividad.instrucciones = instrucciones;
                    actividad.porcentaje = porcentaje;
                    actividad.aceptaFueraDePlazo = cbFueraPlazo.isChecked();
                    actividad.visible = cbVisible.isChecked();
                    actividad.fechaApertura = apertura[0];
                    actividad.fechaCierre = cierre[0];
                    if (archivoUriSeleccionado != null) {
                        actividad.archivoUrl = archivoUriSeleccionado;
                    }

                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.actividadCursoDao().actualizarActividad(actividad);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Actividad actualizada correctamente", Toast.LENGTH_LONG).show();
                            archivoUriSeleccionado = null;
                            if (pestanaActual == 1) cargarActividades();
                        });
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarActividad(ActividadCurso actividad) {
        Executors.newSingleThreadExecutor().execute(() -> {
            db.actividadCursoDao().eliminarActividad(actividad.id);
            runOnUiThread(() -> {
                Toast.makeText(this, "Actividad eliminada", Toast.LENGTH_SHORT).show();
                if (pestanaActual == 1) cargarActividades();
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
                .setTitle("Publicar Material de Estudio")
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

                        runOnUiThread(() -> {
                            Toast.makeText(this, "Material publicado con éxito.", Toast.LENGTH_LONG).show();
                            archivoUriSeleccionado = null;
                            if (pestanaActual == 0) cargarMateriales();
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
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Recurso eliminado correctamente", Toast.LENGTH_SHORT).show();
                            if (pestanaActual == 0) cargarMateriales();
                        });
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private String obtenerTipoDesdeNombre(String nombre) {
        if (nombre == null) return "DOC";
        String lower = nombre.toLowerCase();
        if (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".mkv") || lower.endsWith(".3gp")) return "VIDEO";
        if (lower.endsWith(".pdf")) return "PDF";
        if (lower.endsWith(".docx") || lower.endsWith(".doc")) return "DOCX";
        if (lower.endsWith(".pptx") || lower.endsWith(".ppt")) return "PPTX";
        if (lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".jpeg")) return "IMAGEN";
        return "ARCHIVO";
    }

    private void confirmarEliminarCurso() {
        Executors.newSingleThreadExecutor().execute(() -> {
            int inscritos = db.matriculaDao().obtenerCupoActual(cursoId);
            if (inscritos > 0) {
                runOnUiThread(() -> Toast.makeText(this, "Acción denegada: No se puede eliminar el curso porque tiene " + inscritos + " estudiante(s) matriculado(s).", Toast.LENGTH_LONG).show());
                return;
            }
            runOnUiThread(() -> {
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
            });
        });
    }

    private void registrarAccesoYDescargar(ContenidoCurso material) {
        Executors.newSingleThreadExecutor().execute(() -> {
            RegistroAccesoMaterial registro = new RegistroAccesoMaterial();
            registro.estudianteId = usuarioId;
            registro.materialId = material.id;
            registro.fechaUltimoAcceso = System.currentTimeMillis();

            db.registroAccesoMaterialDao().registrarAcceso(registro);

            runOnUiThread(() -> {
                if (pestanaActual == 0) cargarMateriales();

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
