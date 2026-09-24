package com.example.intercambioacademicoupc.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.intercambioacademicoupc.AdminUsuariosActivity;
import com.example.intercambioacademicoupc.CrearCursoActivity;
import com.example.intercambioacademicoupc.MaterialesCursoActivity;
import com.example.intercambioacademicoupc.R;
import com.example.intercambioacademicoupc.models.AppDatabase;
import com.example.intercambioacademicoupc.models.Curso;
import com.example.intercambioacademicoupc.models.Entrega;
import com.example.intercambioacademicoupc.models.Matricula;
import com.example.intercambioacademicoupc.models.Usuario;
import com.example.intercambioacademicoupc.session.SessionManager;

import java.util.List;
import java.util.concurrent.Executors;

public class InicioFragment extends Fragment {

    private TextView tvBienvenidaInicio, tvRolInfo;
    private Button btnAccionRol, btnBuscarInscribirCurso, btnEntregarTarea, btnDocentePublicar;
    private AppDatabase db;
    private SessionManager sessionManager;
    private Usuario usuarioActual;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inicio, container, false);

        tvBienvenidaInicio = view.findViewById(R.id.tvBienvenidaInicio);
        tvRolInfo = view.findViewById(R.id.tvRolInfo);
        btnAccionRol = view.findViewById(R.id.btnAccionRol);
        btnBuscarInscribirCurso = view.findViewById(R.id.btnBuscarInscribirCurso);
        btnEntregarTarea = view.findViewById(R.id.btnEntregarTarea);
        btnDocentePublicar = view.findViewById(R.id.btnDocentePublicar);

        db = AppDatabase.getDatabase(requireContext());
        sessionManager = new SessionManager(requireContext());

        cargarDatosUsuario();

        return view;
    }

    private void cargarDatosUsuario() {
        int usuarioId = sessionManager.getUsuarioId();
        Executors.newSingleThreadExecutor().execute(() -> {
            usuarioActual = db.usuarioDao().buscarPorId(usuarioId);
            if (usuarioActual != null && isAdded()) {
                requireActivity().runOnUiThread(this.updateUIByRole());
            }
        });
    }

    private Runnable updateUIByRole() {
        return () -> {
            tvBienvenidaInicio.setText("¡Hola, " + usuarioActual.nombre + "!");
            String rol = usuarioActual.rol != null ? usuarioActual.rol : "estudiante";

            if ("estudiante".equals(rol)) {
                tvRolInfo.setText("Rol: Estudiante. Aquí puedes consultar tus cursos, materiales oficiales y entregar trabajos académicos.");
                btnAccionRol.setText("Ver Materiales de mis Cursos");
                btnAccionRol.setOnClickListener(v -> elegirCursoMatriculadoParaMateriales());

                btnBuscarInscribirCurso.setVisibility(View.VISIBLE);
                btnBuscarInscribirCurso.setOnClickListener(v -> mostrarDialogoBuscarCursoParaInscribirse());

                btnEntregarTarea.setVisibility(View.VISIBLE);
                btnEntregarTarea.setOnClickListener(v -> elegirCursoParaEntrega());

                btnDocentePublicar.setVisibility(View.GONE);

            } else if ("docente".equals(rol)) {
                tvRolInfo.setText("Rol: Docente. Publica material de estudio, administra asignaturas y revisa entregas de estudiantes.");
                btnAccionRol.setText("Crear Curso / Administrar Asignatura");
                btnAccionRol.setOnClickListener(v -> startActivity(new Intent(requireContext(), CrearCursoActivity.class)));

                btnBuscarInscribirCurso.setVisibility(View.GONE);
                btnEntregarTarea.setVisibility(View.GONE);
                btnDocentePublicar.setVisibility(View.VISIBLE);
                btnDocentePublicar.setOnClickListener(v -> elegirCursoDocenteParaPublicar());

            } else if ("administrador".equals(rol)) {
                tvRolInfo.setText("Rol: Administrador. Gestiona usuarios, accesos y configuración general del sistema.");
                btnAccionRol.setText("Gestionar Usuarios");
                btnAccionRol.setOnClickListener(v -> startActivity(new Intent(requireContext(), AdminUsuariosActivity.class)));

                btnBuscarInscribirCurso.setVisibility(View.GONE);
                btnEntregarTarea.setVisibility(View.GONE);
                btnDocentePublicar.setVisibility(View.GONE);
            }
        };
    }

    private void mostrarDialogoBuscarCursoParaInscribirse() {
        EditText input = new EditText(requireContext());
        input.setHint("Código o nombre del curso (Ej: INF321)");

        new AlertDialog.Builder(requireContext())
                .setTitle("Buscar e Inscribirse a Curso")
                .setView(input)
                .setPositiveButton("Buscar", (dialog, which) -> {
                    String query = input.getText().toString().trim();
                    if (query.isEmpty()) {
                        Toast.makeText(requireContext(), "Ingresa un término de búsqueda", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    buscarYMostrarCursos(query);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void buscarYMostrarCursos(String termino) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Curso> resultados = db.cursoDao().buscarCursos(termino);
            requireActivity().runOnUiThread(() -> {
                if (resultados.isEmpty()) {
                    Toast.makeText(requireContext(), "No se encontraron cursos con ese código o nombre", Toast.LENGTH_SHORT).show();
                    return;
                }
                String[] nombres = new String[resultados.size()];
                for (int i = 0; i < resultados.size(); i++) {
                    nombres[i] = resultados.get(i).codigo + " - " + resultados.get(i).nombre + " (" + resultados.get(i).periodo + ")";
                }
                new AlertDialog.Builder(requireContext())
                        .setTitle("Resultados de Cursos")
                        .setItems(nombres, (d, which) -> {
                            Curso cursoElegido = resultados.get(which);
                            inscribirEstudianteEnCurso(cursoElegido.id, cursoElegido.nombre);
                        })
                        .show();
            });
        });
    }

    private void inscribirEstudianteEnCurso(int cursoId, String nombreCurso) {
        Executors.newSingleThreadExecutor().execute(() -> {
            int yaInscrito = db.matriculaDao().verificarSiYaEstaInscrito(usuarioActual.id, cursoId);
            if (yaInscrito > 0) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Ya estás matriculado en este curso", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            Matricula m = new Matricula();
            m.estudianteId = usuarioActual.id;
            m.cursoId = cursoId;
            m.fechaMatricula = System.currentTimeMillis();
            db.matriculaDao().matricular(m);

            requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "¡Te has matriculado exitosamente en " + nombreCurso + "!", Toast.LENGTH_LONG).show()
            );
        });
    }

    private void elegirCursoMatriculadoParaMateriales() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Curso> cursos = db.matriculaDao().obtenerCursosMatriculados(usuarioActual.id);
            requireActivity().runOnUiThread(() -> {
                if (cursos.isEmpty()) {
                    Toast.makeText(requireContext(), "No estás matriculado en ningún curso. Usa 'Buscar e Inscribirme' para unirte a uno.", Toast.LENGTH_LONG).show();
                    return;
                }
                String[] nombres = new String[cursos.size()];
                for (int i = 0; i < cursos.size(); i++) {
                    nombres[i] = cursos.get(i).codigo + " - " + cursos.get(i).nombre;
                }
                new AlertDialog.Builder(requireContext())
                        .setTitle("Seleccionar curso (Materiales)")
                        .setItems(nombres, (d, which) -> {
                            Intent intent = new Intent(requireContext(), MaterialesCursoActivity.class);
                            intent.putExtra("CURSO_ID", cursos.get(which).id);
                            startActivity(intent);
                        })
                        .show();
            });
        });
    }

    private void elegirCursoParaEntrega() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Curso> cursos = db.matriculaDao().obtenerCursosMatriculados(usuarioActual.id);
            requireActivity().runOnUiThread(() -> {
                if (cursos.isEmpty()) {
                    Toast.makeText(requireContext(), "No estás matriculado en ningún curso", Toast.LENGTH_SHORT).show();
                    return;
                }
                String[] nombres = new String[cursos.size()];
                for (int i = 0; i < cursos.size(); i++) {
                    nombres[i] = cursos.get(i).codigo + " - " + cursos.get(i).nombre;
                }
                new AlertDialog.Builder(requireContext())
                        .setTitle("Entregar Tarea / Trabajo en Curso")
                        .setItems(nombres, (d, which) -> mostrarDialogoEntrega(cursos.get(which).id))
                        .show();
            });
        });
    }

    private void mostrarDialogoEntrega(int cursoId) {
        EditText inputTitulo = new EditText(requireContext());
        inputTitulo.setHint("Título de la tarea o entregable (Ej: Avance Proyecto)");

        new AlertDialog.Builder(requireContext())
                .setTitle("Nueva Entrega de Trabajo")
                .setView(inputTitulo)
                .setPositiveButton("Entregar", (dialog, which) -> {
                    String titulo = inputTitulo.getText().toString().trim();
                    if (titulo.isEmpty()) {
                        Toast.makeText(requireContext(), "El título no puede estar vacío", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Executors.newSingleThreadExecutor().execute(() -> {
                        Entrega entrega = new Entrega();
                        entrega.cursoId = cursoId;
                        entrega.estudianteId = usuarioActual.id;
                        entrega.tituloTarea = titulo;
                        entrega.descripcionEntrega = "Enviado por el estudiante desde la app";
                        entrega.fechaEntrega = System.currentTimeMillis();
                        entrega.estado = "Entregado";
                        db.entregaDao().insertarEntrega(entrega);

                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "¡Tarea entregada con éxito!", Toast.LENGTH_LONG).show()
                        );
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void elegirCursoDocenteParaPublicar() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Curso> cursos = db.cursoDao().obtenerCursosPorDocente(usuarioActual.id);
            requireActivity().runOnUiThread(() -> {
                if (cursos.isEmpty()) {
                    Toast.makeText(requireContext(), "No tienes cursos asignados. Puedes crear uno.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(requireContext(), CrearCursoActivity.class));
                    return;
                }
                String[] nombres = new String[cursos.size()];
                for (int i = 0; i < cursos.size(); i++) {
                    nombres[i] = cursos.get(i).codigo + " - " + cursos.get(i).nombre;
                }
                new AlertDialog.Builder(requireContext())
                        .setTitle("Gestionar Materiales de mis Cursos")
                        .setItems(nombres, (d, which) -> {
                            Intent intent = new Intent(requireContext(), MaterialesCursoActivity.class);
                            intent.putExtra("CURSO_ID", cursos.get(which).id);
                            startActivity(intent);
                        })
                        .show();
            });
        });
    }
}
