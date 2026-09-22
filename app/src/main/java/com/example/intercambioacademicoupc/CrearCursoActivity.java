package com.example.intercambioacademicoupc;

import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.intercambioacademicoupc.models.AppDatabase;
import com.example.intercambioacademicoupc.models.Curso;
import com.example.intercambioacademicoupc.models.Matricula;
import com.example.intercambioacademicoupc.models.Usuario;
import com.example.intercambioacademicoupc.session.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CrearCursoActivity extends AppCompatActivity {

    private EditText etNombre, etCodigo, etDescripcion, etPeriodo, etCupo;
    private Spinner spEstudiantes;
    private Button btnCrearCurso;
    private AppDatabase db;
    private SessionManager sessionManager;
    private ExecutorService executorService;
    private List<Usuario> listaEstudiantes = new ArrayList<>();

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
        spEstudiantes = findViewById(R.id.sp_estudiantes);
        btnCrearCurso = findViewById(R.id.btn_guardar_curso);

        db = AppDatabase.getDatabase(getApplicationContext());
        executorService = Executors.newSingleThreadExecutor();

        cargarEstudiantes();

        btnCrearCurso.setOnClickListener(v -> guardarCurso());
    }

    private void cargarEstudiantes() {
        executorService.execute(() -> {
            listaEstudiantes = db.usuarioDao().obtenerEstudiantesActivos();
            List<String> nombres = new ArrayList<>();
            nombres.add("Ninguno (Solo crear curso)");
            for (Usuario u : listaEstudiantes) {
                nombres.add(u.nombre + " " + u.apellido + " (" + u.codigoEstudiantil + ")");
            }

            runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, nombres);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spEstudiantes.setAdapter(adapter);
            });
        });
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

        int cupo;
        try {
            cupo = Integer.parseInt(cupoStr);
        } catch (NumberFormatException e) {
            etCupo.setError("El cupo debe ser un numero entero");
            etCupo.requestFocus();
            return;
        }

        if (cupo <= 0) {
            etCupo.setError("El cupo debe ser mayor que cero");
            etCupo.requestFocus();
            return;
        }

        int seleccionIdx = spEstudiantes.getSelectedItemPosition();

        executorService.execute(() -> {
            try {
                Curso nuevoCurso = new Curso();
                nuevoCurso.nombre = nombre;
                nuevoCurso.codigo = codigo;
                nuevoCurso.descripcion = descripcion;
                nuevoCurso.periodo = periodo;
                nuevoCurso.cupoMaximo = cupo;
                nuevoCurso.estado = "borrador";
                nuevoCurso.docenteId = sessionManager.getUsuarioId();

                long cursoId = db.cursoDao().insert(nuevoCurso);

                // Si seleccionó un estudiante, matricularlo de una vez
                if (seleccionIdx > 0) {
                    Usuario est = listaEstudiantes.get(seleccionIdx - 1);
                    Matricula m = new Matricula();
                    m.cursoId = (int) cursoId;
                    m.estudianteId = est.id;
                    m.fechaMatricula = System.currentTimeMillis();
                    db.matriculaDao().matricular(m);
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "Curso creado y alumno matriculado exitosamente", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (SQLiteConstraintException e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: Ya existe un curso con este código en el mismo periodo.", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error inesperado al guardar el curso.", Toast.LENGTH_SHORT).show());
            }
        });
    }
}