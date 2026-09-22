package com.example.intercambioacademicoupc;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.intercambioacademicoupc.models.AppDatabase;
import com.example.intercambioacademicoupc.models.Curso;
import com.example.intercambioacademicoupc.session.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminCursosGeneralActivity extends AppCompatActivity {

    private Spinner spCursos;
    private Button btnAbrirAula, btnCrearNuevo;
    private TextView tvTitulo;
    private AppDatabase db;
    private SessionManager sessionManager;
    private ExecutorService executorService;
    private List<Curso> listaCursos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_cursos_general);

        sessionManager = new SessionManager(this);
        db = AppDatabase.getDatabase(getApplicationContext());
        executorService = Executors.newSingleThreadExecutor();

        spCursos = findViewById(R.id.sp_todos_los_cursos);
        btnAbrirAula = findViewById(R.id.btn_abrir_aula_curso);
        btnCrearNuevo = findViewById(R.id.btn_crear_nuevo_curso_docente);
        tvTitulo = findViewById(R.id.tv_titulo_panel_cursos);

        String rol = sessionManager.getRol();
        if ("docente".equals(rol)) {
            tvTitulo.setText("Mis Asignaturas (Docente)");
            btnCrearNuevo.setVisibility(View.VISIBLE);
        } else if ("administrador".equals(rol)) {
            tvTitulo.setText("Panel Global de Asignaturas (Admin)");
            btnCrearNuevo.setVisibility(View.GONE);
        }

        btnCrearNuevo.setOnClickListener(v -> {
            startActivity(new Intent(this, CrearCursoActivity.class));
        });

        btnAbrirAula.setOnClickListener(v -> abrirAsignatura());
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarListaDeCursos();
    }

    private void cargarListaDeCursos() {
        executorService.execute(() -> {
            String rol = sessionManager.getRol();
            if ("docente".equals(rol)) {
                listaCursos = db.cursoDao().obtenerCursosPorDocente(sessionManager.getUsuarioId());
            } else {
                listaCursos = db.cursoDao().obtenerTodosLosCursos();
            }

            List<String> nombres = new ArrayList<>();
            if (listaCursos.isEmpty()) {
                nombres.add("No hay asignaturas registradas");
            } else {
                for (Curso c : listaCursos) {
                    nombres.add(c.nombre + " [" + c.codigo + "] - Per: " + c.periodo);
                }
            }

            runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, nombres);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spCursos.setAdapter(adapter);
            });
        });
    }

    private void abrirAsignatura() {
        if (listaCursos.isEmpty()) {
            Toast.makeText(this, "No hay una asignatura seleccionada", Toast.LENGTH_SHORT).show();
            return;
        }

        int idx = spCursos.getSelectedItemPosition();
        Curso curso = listaCursos.get(idx);

        Intent intent = new Intent(this, DetalleCursoActivity.class);
        intent.putExtra("CURSO_ID", curso.id);
        startActivity(intent);
    }
}