package com.example.intercambioacademicoupc;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.intercambioacademicoupc.adapters.NotificacionesAdapter;
import com.example.intercambioacademicoupc.models.AppDatabase;
import com.example.intercambioacademicoupc.models.Curso;
import com.example.intercambioacademicoupc.models.NotificacionItem;
import com.example.intercambioacademicoupc.models.Usuario;
import com.example.intercambioacademicoupc.session.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class NotificacionesActivity extends AppCompatActivity {

    private RecyclerView rvNotificaciones;
    private Button btnMarcarTodasLeidas, btnConfigurarNotif;
    private CardView cardAjustesNotificaciones;
    private Switch switchNotifMaterial, switchNotifActividad, switchNotifEntrega48h, switchNotifNota;
    private TextView tvSinNotificaciones;

    private AppDatabase db;
    private SessionManager sessionManager;
    private NotificacionesAdapter adapter;
    private List<NotificacionItem> listaNotificaciones = new ArrayList<>();
    private int usuarioId;
    private boolean cargandoPreferencias = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificaciones);

        db = AppDatabase.getDatabase(this);
        sessionManager = new SessionManager(this);
        usuarioId = sessionManager.getUsuarioId();

        rvNotificaciones = findViewById(R.id.rvNotificaciones);
        btnMarcarTodasLeidas = findViewById(R.id.btnMarcarTodasLeidas);
        btnConfigurarNotif = findViewById(R.id.btnConfigurarNotif);
        cardAjustesNotificaciones = findViewById(R.id.cardAjustesNotificaciones);

        switchNotifMaterial = findViewById(R.id.switchNotifMaterial);
        switchNotifActividad = findViewById(R.id.switchNotifActividad);
        switchNotifEntrega48h = findViewById(R.id.switchNotifEntrega48h);
        switchNotifNota = findViewById(R.id.switchNotifNota);

        tvSinNotificaciones = findViewById(R.id.tvSinNotificaciones);

        rvNotificaciones.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificacionesAdapter(this, listaNotificaciones, this::abrirDetalleNotificacion);
        rvNotificaciones.setAdapter(adapter);

        btnMarcarTodasLeidas.setOnClickListener(v -> marcarTodasLeidas());
        btnConfigurarNotif.setOnClickListener(v -> {
            boolean visible = cardAjustesNotificaciones.getVisibility() == View.VISIBLE;
            cardAjustesNotificaciones.setVisibility(visible ? View.GONE : View.VISIBLE);
        });

        configurarListenersSwitches();
        cargarNotificacionesYPreferencias();
    }

    private void configurarListenersSwitches() {
        View.OnClickListener listenerGuarda = v -> {
            if (!cargandoPreferencias) {
                guardarPreferenciasNotificacion();
            }
        };

        switchNotifMaterial.setOnClickListener(listenerGuarda);
        switchNotifActividad.setOnClickListener(listenerGuarda);
        switchNotifEntrega48h.setOnClickListener(listenerGuarda);
        switchNotifNota.setOnClickListener(listenerGuarda);
    }

    private void cargarNotificacionesYPreferencias() {
        cargandoPreferencias = true;
        Executors.newSingleThreadExecutor().execute(() -> {
            List<NotificacionItem> items = db.notificacionDao().obtenerNotificacionesPorUsuario(usuarioId);
            Usuario u = db.usuarioDao().buscarPorId(usuarioId);

            runOnUiThread(() -> {
                listaNotificaciones = items;
                adapter.setListaNotificaciones(items);
                if (items == null || items.isEmpty()) {
                    tvSinNotificaciones.setVisibility(View.VISIBLE);
                    rvNotificaciones.setVisibility(View.GONE);
                } else {
                    tvSinNotificaciones.setVisibility(View.GONE);
                    rvNotificaciones.setVisibility(View.VISIBLE);
                }

                if (u != null) {
                    boolean esDocenteOAdmin = "docente".equals(u.rol) || "administrador".equals(u.rol);
                    if (esDocenteOAdmin) {
                        switchNotifMaterial.setText("Entrega de tarea");
                        switchNotifActividad.setText("Reemplazo / Actualización de tarea");
                        switchNotifEntrega48h.setText("Entrega tarde de tarea");
                        switchNotifNota.setText("Solicitudes de revisión de nota");

                        switchNotifMaterial.setChecked(u.notifEntregaTarea);
                        switchNotifActividad.setChecked(u.notifReemplazoTarea);
                        switchNotifEntrega48h.setChecked(u.notifEntregaTarde);
                        switchNotifNota.setChecked(u.notifSolicitudRevision);
                    } else {
                        switchNotifMaterial.setText("Nuevos materiales publicados");
                        switchNotifActividad.setText("Nuevas actividades / tareas asignadas");
                        switchNotifEntrega48h.setText("Recordatorio de entregas (< 48h)");
                        switchNotifNota.setText("Publicación de calificaciones");

                        switchNotifMaterial.setChecked(u.notifNuevoMaterial);
                        switchNotifActividad.setChecked(u.notifNuevaActividad);
                        switchNotifEntrega48h.setChecked(u.notifEntrega48h);
                        switchNotifNota.setChecked(u.notifNotaPublicada);
                    }
                }
                cargandoPreferencias = false;
            });
        });
    }

    private void guardarPreferenciasNotificacion() {
        boolean s1 = switchNotifMaterial.isChecked();
        boolean s2 = switchNotifActividad.isChecked();
        boolean s3 = switchNotifEntrega48h.isChecked();
        boolean s4 = switchNotifNota.isChecked();

        String rol = sessionManager.getRol();
        boolean esDocenteOAdmin = "docente".equals(rol) || "administrador".equals(rol);

        Executors.newSingleThreadExecutor().execute(() -> {
            if (esDocenteOAdmin) {
                db.usuarioDao().actualizarPreferenciasNotificacionDocente(usuarioId, s1, s2, s3, s4);
            } else {
                db.usuarioDao().actualizarPreferenciasNotificacion(usuarioId, s1, s2, s3, s4);
            }
            runOnUiThread(() -> Toast.makeText(NotificacionesActivity.this, "Preferencias de notificación guardadas", Toast.LENGTH_SHORT).show());
        });
    }

    private void abrirDetalleNotificacion(NotificacionItem item) {
        Executors.newSingleThreadExecutor().execute(() -> {
            // 1. Marcar como leída
            db.notificacionDao().marcarComoLeida(item.id);

            String rol = sessionManager.getRol();
            boolean esDocenteOAdmin = "docente".equals(rol) || "administrador".equals(rol);

            int targetCursoId = item.cursoId;
            int targetActividadId = item.actividadId;

            // Si cursoId es 0, buscar en BD por concordancia de título/mensaje o cursos del usuario
            if (targetCursoId <= 0) {
                List<Curso> cursos = db.cursoDao().obtenerTodosLosCursos();
                if (item.titulo != null || item.mensaje != null) {
                    String text = ((item.titulo != null ? item.titulo : "") + " " + (item.mensaje != null ? item.mensaje : "")).toLowerCase();
                    for (Curso c : cursos) {
                        if (c.nombre != null && !c.nombre.isEmpty() && text.contains(c.nombre.toLowerCase())) {
                            targetCursoId = c.id;
                            break;
                        }
                        if (c.codigo != null && !c.codigo.isEmpty() && text.contains(c.codigo.toLowerCase())) {
                            targetCursoId = c.id;
                            break;
                        }
                    }
                }
                if (targetCursoId <= 0 && !cursos.isEmpty()) {
                    if (esDocenteOAdmin) {
                        List<Curso> misCursos = db.cursoDao().obtenerCursosPorDocente(usuarioId);
                        if (misCursos.size() == 1) targetCursoId = misCursos.get(0).id;
                    } else {
                        List<Curso> misCursos = db.matriculaDao().obtenerCursosMatriculados(usuarioId);
                        if (misCursos.size() == 1) targetCursoId = misCursos.get(0).id;
                    }
                }
            }

            final int finalCursoId = targetCursoId;
            final int finalActividadId = targetActividadId;

            runOnUiThread(() -> {
                cargarNotificacionesYPreferencias(); // Refrescar estado visual a leída

                if (finalCursoId <= 0) {
                    Toast.makeText(NotificacionesActivity.this, "Detalle: " + item.titulo, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (esDocenteOAdmin && finalActividadId > 0 && ("entrega".equals(item.tipo) || "actividad".equals(item.tipo))) {
                    Intent intent = new Intent(NotificacionesActivity.this, CalificarEntregasActivity.class);
                    intent.putExtra("CURSO_ID", finalCursoId);
                    intent.putExtra("ACTIVIDAD_ID", finalActividadId);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(NotificacionesActivity.this, MaterialesCursoActivity.class);
                    intent.putExtra("CURSO_ID", finalCursoId);

                    if ("nota".equals(item.tipo)) {
                        intent.putExtra("PESTANA", 3); // Pestaña de Calificaciones
                    } else if ("material".equals(item.tipo)) {
                        intent.putExtra("PESTANA", 0); // Pestaña de Materiales
                    } else {
                        intent.putExtra("PESTANA", 1); // Pestaña de Actividades/Tareas
                    }
                    startActivity(intent);
                }
            });
        });
    }

    private void marcarTodasLeidas() {
        Executors.newSingleThreadExecutor().execute(() -> {
            db.notificacionDao().marcarTodasComoLeidas(usuarioId);
            List<NotificacionItem> items = db.notificacionDao().obtenerNotificacionesPorUsuario(usuarioId);
            runOnUiThread(() -> {
                Toast.makeText(this, "Todas las notificaciones marcadas como leídas", Toast.LENGTH_SHORT).show();
                listaNotificaciones = items;
                adapter.setListaNotificaciones(items);
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarNotificacionesYPreferencias();
    }
}
