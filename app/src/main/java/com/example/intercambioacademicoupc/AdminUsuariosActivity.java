package com.example.intercambioacademicoupc;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.example.intercambioacademicoupc.adapters.UsuarioAdapter;
import com.example.intercambioacademicoupc.models.AppDatabase;
import com.example.intercambioacademicoupc.models.Usuario;
import com.example.intercambioacademicoupc.session.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminUsuariosActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText etBusqueda;
    private UsuarioAdapter adapter;
    private AppDatabase db;
    private SessionManager sessionManager;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_usuarios); // Asume este layout

        sessionManager = new SessionManager(this);

        // Validación de seguridad HU-05: Solo administrador
        if (!"administrador".equals(sessionManager.getRol())) {
            Toast.makeText(this, "Error de autorización. Acceso denegado.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        recyclerView = findViewById(R.id.rv_usuarios);
        etBusqueda = findViewById(R.id.et_buscar_usuario);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Inicializar Room DB y Executor
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "intercambio_db").build();
        executorService = Executors.newSingleThreadExecutor();

        // Configurar el adaptador con los callbacks
        adapter = new UsuarioAdapter(this, new ArrayList<>(), new UsuarioAdapter.OnUsuarioInteractionListener() {
            @Override
            public void onRolCambiado(int usuarioId, String nuevoRol) {
                actualizarRolEnDb(usuarioId, nuevoRol);
            }

            @Override
            public void onUsuarioDesactivado(int usuarioId) {
                desactivarUsuarioEnDb(usuarioId);
            }
        });
        recyclerView.setAdapter(adapter);

        cargarUsuarios();

        // Buscador en tiempo real
        etBusqueda.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                buscarUsuarios(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void cargarUsuarios() {
        executorService.execute(() -> {
            List<Usuario> lista = db.usuarioDao().obtenerUsuariosActivos();
            runOnUiThread(() -> adapter.setUsuarios(lista));
        });
    }

    private void buscarUsuarios(String query) {
        executorService.execute(() -> {
            List<Usuario> lista;
            if (query.trim().isEmpty()) {
                lista = db.usuarioDao().obtenerUsuariosActivos();
            } else {
                lista = db.usuarioDao().buscarUsuarios(query);
            }
            runOnUiThread(() -> adapter.setUsuarios(lista));
        });
    }

    private void actualizarRolEnDb(int usuarioId, String nuevoRol) {
        executorService.execute(() -> {
            db.usuarioDao().actualizarRol(usuarioId, nuevoRol);
            runOnUiThread(() -> {
                Toast.makeText(this, "Rol actualizado correctamente", Toast.LENGTH_SHORT).show();
                cargarUsuarios(); // Recargar la lista
            });
        });
    }

    private void desactivarUsuarioEnDb(int usuarioId) {
        executorService.execute(() -> {
            db.usuarioDao().desactivarUsuario(usuarioId);
            runOnUiThread(() -> {
                Toast.makeText(this, "Usuario desactivado", Toast.LENGTH_SHORT).show();
                cargarUsuarios(); // Recargar la lista para que desaparezca
            });
        });
    }
}