package com.example.intercambioacademicoupc;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.intercambioacademicoupc.adapters.MaterialesAdapter;
import com.example.intercambioacademicoupc.models.AppDatabase;
import com.example.intercambioacademicoupc.models.ContenidoCurso;
import com.example.intercambioacademicoupc.models.RegistroAccesoMaterial;
import com.example.intercambioacademicoupc.session.SessionManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class MaterialesCursoActivity extends AppCompatActivity {

    private RecyclerView recyclerMateriales;
    private AppDatabase db;
    private SessionManager sessionManager;
    private int cursoId;
    private int estudianteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_materiales_curso);

        recyclerMateriales = findViewById(R.id.recyclerMateriales);
        recyclerMateriales.setLayoutManager(new LinearLayoutManager(this));

        db = AppDatabase.getDatabase(this);
        sessionManager = new SessionManager(this);

        cursoId = getIntent().getIntExtra("CURSO_ID", -1);

        estudianteId = sessionManager.getUsuarioId();

        if (cursoId == -1 || estudianteId == -1) {
            Toast.makeText(this, "Error de vinculación al curso o sesión inválida", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        verificarMatriculaYBuscarMateriales();
    }

    // CORRECCIÓN: Ya no recibe el correo, usa la variable global estudianteId
    private void verificarMatriculaYBuscarMateriales() {
        Executors.newSingleThreadExecutor().execute(() -> {

            // Validación restrictiva del criterio de aceptación
            int estaMatriculado = db.matriculaDao().verificarSiYaEstaInscrito(estudianteId, cursoId);

            if (estaMatriculado == 0) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Acceso denegado: Material exclusivo para matriculados.", Toast.LENGTH_LONG).show();
                    finish();
                });
                return;
            }

            // Ejecuta ordenamiento SQL nativo agrupado por Unidad
            List<ContenidoCurso> materiales = db.contenidoCursoDao().obtenerContenidosPorCurso(cursoId);
            Map<Integer, Long> accesosMap = new HashMap<>();

            for (ContenidoCurso mat : materiales) {
                Long ultimoAcceso = db.registroAccesoMaterialDao().obtenerUltimoAcceso(estudianteId, mat.id);
                if (ultimoAcceso != null) {
                    accesosMap.put(mat.id, ultimoAcceso);
                }
            }

            runOnUiThread(() -> {
                MaterialesAdapter adapter = new MaterialesAdapter(materiales, accesosMap, material -> {
                    registrarAccesoYDescargar(material);
                });
                recyclerMateriales.setAdapter(adapter);
            });
        });
    }

    private void registrarAccesoYDescargar(ContenidoCurso material) {
        Executors.newSingleThreadExecutor().execute(() -> {
            RegistroAccesoMaterial registro = new RegistroAccesoMaterial();
            registro.estudianteId = estudianteId;
            registro.materialId = material.id;
            registro.fechaUltimoAcceso = System.currentTimeMillis();

            db.registroAccesoMaterialDao().registrarAcceso(registro);

            runOnUiThread(() -> {
                // Refrescar el RecyclerView para actualizar etiqueta de acceso reciente
                verificarMatriculaYBuscarMateriales();

                // Simulación del gestor de descargas o apertura de URL externa
                if (material.urlArchivo != null && !material.urlArchivo.isEmpty()) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(material.urlArchivo));
                    startActivity(browserIntent);
                } else {
                    Toast.makeText(this, "Iniciando descarga local de: " + material.titulo, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}