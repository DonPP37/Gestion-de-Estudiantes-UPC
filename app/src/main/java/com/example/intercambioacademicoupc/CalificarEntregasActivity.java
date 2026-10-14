package com.example.intercambioacademicoupc;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.intercambioacademicoupc.adapters.EntregasAdapter;
import com.example.intercambioacademicoupc.models.ActividadCurso;
import com.example.intercambioacademicoupc.models.AppDatabase;
import com.example.intercambioacademicoupc.models.Entrega;
import com.example.intercambioacademicoupc.models.Usuario;
import com.example.intercambioacademicoupc.session.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class CalificarEntregasActivity extends AppCompatActivity {

    private TextView tvTituloActividadEntregas, tvResumenEntregas;
    private RecyclerView recyclerEntregasActividad;

    private AppDatabase db;
    private SessionManager sessionManager;
    private int actividadId;
    private int cursoId;
    private ActividadCurso actividadActual;
    private Usuario usuarioActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calificar_entregas);

        tvTituloActividadEntregas = findViewById(R.id.tvTituloActividadEntregas);
        tvResumenEntregas = findViewById(R.id.tvResumenEntregas);
        recyclerEntregasActividad = findViewById(R.id.recyclerEntregasActividad);
        recyclerEntregasActividad.setLayoutManager(new LinearLayoutManager(this));

        db = AppDatabase.getDatabase(this);
        sessionManager = new SessionManager(this);

        actividadId = getIntent().getIntExtra("ACTIVIDAD_ID", -1);
        cursoId = getIntent().getIntExtra("CURSO_ID", -1);

        if (actividadId == -1) {
            Toast.makeText(this, "Actividad no encontrada", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        cargarDatosYEntregas();
    }

    private void cargarDatosYEntregas() {
        Executors.newSingleThreadExecutor().execute(() -> {
            usuarioActual = db.usuarioDao().buscarPorId(sessionManager.getUsuarioId());
            List<ActividadCurso> lista = db.actividadCursoDao().obtenerActividadesPorCurso(cursoId);
            for (ActividadCurso a : lista) {
                if (a.id == actividadId) {
                    actividadActual = a;
                    break;
                }
            }

            List<Entrega> entregas = db.entregaDao().obtenerEntregasPorActividad(actividadId);

            // Obtener estudiantes asociados
            Map<Integer, Usuario> mapaEstudiantes = new HashMap<>();
            int calificadas = 0;
            for (Entrega e : entregas) {
                if (!mapaEstudiantes.containsKey(e.estudianteId)) {
                    Usuario est = db.usuarioDao().buscarPorId(e.estudianteId);
                    if (est != null) {
                        mapaEstudiantes.put(e.estudianteId, est);
                    }
                }
                if ("calificada".equalsIgnoreCase(e.estado)) {
                    calificadas++;
                }
            }

            final int total = entregas.size();
            final int cal = calificadas;
            final int pend = total - cal;

            runOnUiThread(() -> {
                if (actividadActual != null) {
                    tvTituloActividadEntregas.setText("Entregas: " + actividadActual.titulo + " (" + actividadActual.porcentaje + "%)");
                }
                tvResumenEntregas.setText("Total de entregas: " + total + " | Calificadas: " + cal + " | Pendientes: " + pend);

                EntregasAdapter adapter = new EntregasAdapter(entregas, mapaEstudiantes, true, new EntregasAdapter.OnEntregaClickListener() {
                    @Override
                    public void onCalificar(Entrega entrega, Usuario estudiante) {
                        mostrarDialogoCalificar(entrega, estudiante);
                    }

                    @Override
                    public void onVerArchivos(Entrega entrega) {
                        mostrarOpcionesArchivos(entrega);
                    }
                });
                recyclerEntregasActividad.setAdapter(adapter);
            });
        });
    }

    private void mostrarOpcionesArchivos(Entrega entrega) {
        if (entrega.archivoUrl == null || entrega.archivoUrl.trim().isEmpty()) {
            Toast.makeText(this, "Esta entrega no tiene archivos adjuntos", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] uris = entrega.archivoUrl.split(";");
        String[] nombres = entrega.nombresArchivos != null ? entrega.nombresArchivos.split(" \\| ") : new String[0];

        List<String> items = new ArrayList<>();
        for (int i = 0; i < uris.length; i++) {
            String label = (i < nombres.length && !nombres[i].isEmpty()) ? nombres[i] : ("Archivo " + (i + 1));
            items.add("📄 " + label);
        }

        new AlertDialog.Builder(this)
                .setTitle("Archivos Adjuntos de la Entrega")
                .setItems(items.toArray(new String[0]), (dialog, which) -> {
                    String selectedUri = uris[which];
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse(selectedUri), "*/*");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    } catch (Exception e) {
                        // Fallback con Intent genérico
                        try {
                            Intent genericIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(selectedUri));
                            startActivity(genericIntent);
                        } catch (Exception ex) {
                            Toast.makeText(this, "No se puede abrir el archivo seleccionado en el dispositivo", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void mostrarDialogoCalificar(Entrega entrega, Usuario estudiante) {
        String nombreEstudiante = estudiante != null ? estudiante.nombre + " " + estudiante.apellido : "Estudiante";

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int p = 36;
        layout.setPadding(p, p, p, p);

        TextView tvInfo = new TextView(this);
        tvInfo.setText("Estudiante: " + nombreEstudiante + "\nEscala de evaluación: 0.0 - 5.0");
        tvInfo.setTextSize(14f);
        layout.addView(tvInfo);

        final EditText etNota = new EditText(this);
        etNota.setHint("Nota (ej: 4.5)");
        etNota.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (entrega.calificacion != null) {
            etNota.setText(String.format(Locale.US, "%.1f", entrega.calificacion));
        }
        layout.addView(etNota);

        final EditText etFeedback = new EditText(this);
        etFeedback.setHint("Retroalimentación / Comentarios para el estudiante");
        etFeedback.setLines(3);
        if (entrega.retroalimentacion != null) {
            etFeedback.setText(entrega.retroalimentacion);
        }
        layout.addView(etFeedback);

        String tituloModal = (entrega.calificacion != null) ? "Modificar Calificación" : "Registrar Calificación";

        new AlertDialog.Builder(this)
                .setTitle(tituloModal)
                .setView(layout)
                .setPositiveButton("Guardar Calificación", (dialog, which) -> {
                    String notaStr = etNota.getText().toString().trim().replace(",", ".");
                    String feedback = etFeedback.getText().toString().trim();

                    if (notaStr.isEmpty()) {
                        Toast.makeText(this, "Debes ingresar una nota numérica", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double nota;
                    try {
                        nota = Double.parseDouble(notaStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Formato de nota inválido", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Validación escala 0.0 a 5.0
                    if (nota < 0.0 || nota > 5.0) {
                        Toast.makeText(this, "La nota debe estar dentro de la escala [0.0 - 5.0]", Toast.LENGTH_LONG).show();
                        return;
                    }

                    long ahora = System.currentTimeMillis();
                    String nombreDocente = usuarioActual != null ? (usuarioActual.nombre + " " + usuarioActual.apellido) : "Docente";
                    String fechaHoraStr = DateFormat.format("dd/MM/yyyy HH:mm", ahora).toString();

                    // HU-14: Si ya tenía calificación, auditar el cambio con autor y fecha
                    if (entrega.calificacion != null) {
                        String lineaAuditoria = "Modificado el " + fechaHoraStr + " por " + nombreDocente + ": Nota anterior " + entrega.calificacion + " -> " + nota;
                        if (entrega.historialModificaciones == null || entrega.historialModificaciones.isEmpty()) {
                            entrega.historialModificaciones = lineaAuditoria;
                        } else {
                            entrega.historialModificaciones = entrega.historialModificaciones + "\n" + lineaAuditoria;
                        }
                    } else {
                        entrega.historialModificaciones = "Calificado el " + fechaHoraStr + " por " + nombreDocente;
                    }

                    entrega.calificacion = nota;
                    entrega.retroalimentacion = feedback;
                    entrega.fechaCalificacion = ahora;
                    entrega.autorModificacion = nombreDocente;
                    entrega.estado = "calificada"; // HU-14: Al guardar el estado cambia a calificada

                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.entregaDao().actualizarEntrega(entrega);
                        runOnUiThread(() -> {
                            // HU-14: Notificación al estudiante generada
                            Toast.makeText(this, "¡Calificación guardada y estudiante notificado exitosamente!", Toast.LENGTH_LONG).show();
                            cargarDatosYEntregas();
                        });
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
