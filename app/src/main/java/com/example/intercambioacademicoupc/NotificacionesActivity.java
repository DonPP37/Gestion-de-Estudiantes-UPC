package com.example.intercambioacademicoupc;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.intercambioacademicoupc.adapters.NotificacionesAdapter;
import com.example.intercambioacademicoupc.models.AppDatabase;
import com.example.intercambioacademicoupc.models.NotificacionItem;
import com.example.intercambioacademicoupc.session.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class NotificacionesActivity extends AppCompatActivity {

    private RecyclerView rvNotificaciones;
    private Button btnMarcarTodasLeidas;
    private TextView tvSinNotificaciones;
    private AppDatabase db;
    private SessionManager sessionManager;
    private NotificacionesAdapter adapter;
    private List<NotificacionItem> listaNotificaciones = new ArrayList<>();
    private int usuarioId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificaciones);

        db = AppDatabase.getDatabase(this);
        sessionManager = new SessionManager(this);
        usuarioId = sessionManager.getUsuarioId();

        rvNotificaciones = findViewById(R.id.rvNotificaciones);
        btnMarcarTodasLeidas = findViewById(R.id.btnMarcarTodasLeidas);
        tvSinNotificaciones = findViewById(R.id.tvSinNotificaciones);

        rvNotificaciones.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificacionesAdapter(this, listaNotificaciones);
        rvNotificaciones.setAdapter(adapter);

        btnMarcarTodasLeidas.setOnClickListener(v -> marcarTodasLeidas());

        cargarNotificaciones();
    }

    private void cargarNotificaciones() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<NotificacionItem> items = db.notificacionDao().obtenerNotificacionesPorUsuario(usuarioId);
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
        cargarNotificaciones();
    }
}
