package com.example.intercambioacademicoupc;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.intercambioacademicoupc.models.AppDatabase;
import com.example.intercambioacademicoupc.models.Curso;
import com.example.intercambioacademicoupc.models.Matricula;
import com.example.intercambioacademicoupc.session.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MatriculaEstudianteActivity extends AppCompatActivity {

    private Spinner spCursos;
    private Button btnMatricularme;
    private AppDatabase db;
    private SessionManager sessionManager;
    private ExecutorService executorService;
    private List<Curso> listaCursos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matricula_estudiante);

        sessionManager = new SessionManager(this);
        db = AppDatabase.getDatabase(getApplicationContext());
        executorService = Executors.newSingleThreadExecutor();

        spCursos = findViewById(R.id.sp_cursos_disponibles);
        btnMatricularme = findViewById(R.id.btn_confirmar_matricula);

        cargarCursosDisponibles();

        btnMatricularme.setOnClickListener(v -> realizarMatricula());
    }

    private void cargarCursosDisponibles() {
        executorService.execute(() -> {
            listaCursos = db.cursoDao().obtenerCursosPublicados();
            List<String> opciones = new ArrayList<>();
            
            if (listaCursos.isEmpty()) {
                opciones.add("No hay cursos disponibles para publicación");
            } else {
                for (Curso c : listaCursos) {
                    opciones.add(c.nombre + " (" + c.codigo + ") - Periodo: " + c.periodo);
                }
            }

            runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, opciones);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spCursos.setAdapter(adapter);
            });
        });
    }

    private void realizarMatricula() {
        if (listaCursos.isEmpty()) {
            Toast.makeText(this, "No hay un curso válido seleccionado", Toast.LENGTH_SHORT).show();
            return;
        }

        int seleccionIdx = spCursos.getSelectedItemPosition();
        Curso cursoSeleccionado = listaCursos.get(seleccionIdx);
        int estudianteId = sessionManager.getUsuarioId();

        executorService.execute(() -> {
            // Criterio HU-07: Impedir matrículas duplicadas
            int yaMatriculado = db.matriculaDao().verificarSiYaEstaInscrito(estudianteId, cursoSeleccionado.id);
            if (yaMatriculado > 0) {
                runOnUiThread(() -> Toast.makeText(this, "Ya te encuentras matriculado en este curso.", Toast.LENGTH_LONG).show());
                return;
            }

            // Criterio HU-07: Control de cupos máximos
            int cupoActual = db.matriculaDao().obtenerCupoActual(cursoSeleccionado.id);
            if (cupoActual >= cursoSeleccionado.cupoMaximo) {
                runOnUiThread(() -> Toast.makeText(this, "El cupo está lleno. Bloqueando inscripción.", Toast.LENGTH_LONG).show());
                return;
            }

            // Proceder con la matrícula
            Matricula nuevaMatricula = new Matricula();
            nuevaMatricula.estudianteId = estudianteId;
            nuevaMatricula.cursoId = cursoSeleccionado.id;
            nuevaMatricula.fechaMatricula = System.currentTimeMillis();

            db.matriculaDao().matricular(nuevaMatricula);

            runOnUiThread(() -> {
                Toast.makeText(this, "¡Matrícula realizada con éxito!", Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            });
        });
    }
}