package com.example.intercambioacademicoupc;

import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.example.intercambioacademicoupc.models.AppDatabase;
import com.example.intercambioacademicoupc.models.Curso;
import com.example.intercambioacademicoupc.session.SessionManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CrearCursoActivity extends AppCompatActivity {

    private EditText etNombre, etCodigo, etDescripcion, etPeriodo, etCupo;
    private Button btnCrearCurso;
    private AppDatabase db;
    private SessionManager sessionManager;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_curso);

        sessionManager = new SessionManager(this);

        // Validación de seguridad
        if (!"docente".equals(sessionManager.getRol())) {
            Toast.makeText(this, "Solo los docentes pueden crear cursos.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        etNombre = findViewById(R.id.et_curso_nombre);
        etCodigo = findViewById(R.id.et_curso_codigo);
        etDescripcion = findViewById(R.id.et_curso_descripcion);
        etPeriodo = findViewById(R.id.et_curso_periodo);
        etCupo = findViewById(R.id.et_curso_cupo);
        btnCrearCurso = findViewById(R.id.btn_guardar_curso);

        // Se corrige el nombre de la db para que coincida con AppDatabase.java
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "intercambios_database").build();
        executorService = Executors.newSingleThreadExecutor();

        btnCrearCurso.setOnClickListener(v -> guardarCurso());
    }

    private void guardarCurso() {
        String nombre = etNombre.getText().toString().trim();
        String codigo = etCodigo.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String periodo = etPeriodo.getText().toString().trim();
        String cupoStr = etCupo.getText().toString().trim();

        // Validar campos obligatorios (HU-06)
        if (nombre.isEmpty() || codigo.isEmpty() || descripcion.isEmpty() || periodo.isEmpty() || cupoStr.isEmpty()) {
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        int cupo = Integer.parseInt(cupoStr);

        Curso nuevoCurso = new Curso();
        nuevoCurso.nombre = nombre;
        nuevoCurso.codigo = codigo;
        nuevoCurso.descripcion = descripcion;
        nuevoCurso.periodo = periodo;
        nuevoCurso.cupoMaximo = cupo;
        nuevoCurso.estado = "borrador"; // Por regla de negocio nace en borrador

        // El docente creador queda asociado automáticamente (HU-06)
        // Corrección de casteo: ya retorna un entero
        nuevoCurso.docenteId = sessionManager.getUsuarioId();

        executorService.execute(() -> {
            try {
                db.cursoDao().insert(nuevoCurso);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Curso creado exitosamente en borrador", Toast.LENGTH_SHORT).show();
                    finish(); // Cierra la actividad tras guardar
                });
            } catch (SQLiteConstraintException e) {
                // Captura el error de índice único definido en Curso.java (@Index(value = {"codigo", "periodo"}))
                runOnUiThread(() -> Toast.makeText(this, "Error: Ya existe un curso con este código en el mismo periodo.", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error inesperado al guardar el curso.", Toast.LENGTH_SHORT).show());
            }
        });
    }
}