package com.example.intercambioacademicoupc.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.intercambioacademicoupc.AdminUsuariosActivity;
import com.example.intercambioacademicoupc.CrearCursoActivity;
import com.example.intercambioacademicoupc.MaterialesCursoActivity;
import com.example.intercambioacademicoupc.R;
import com.example.intercambioacademicoupc.models.ActividadCurso;
import com.example.intercambioacademicoupc.models.AppDatabase;
import com.example.intercambioacademicoupc.models.Curso;
import com.example.intercambioacademicoupc.models.Entrega;
import com.example.intercambioacademicoupc.models.Matricula;
import com.example.intercambioacademicoupc.models.Usuario;
import com.example.intercambioacademicoupc.session.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class InicioFragment extends Fragment {

    private TextView tvBienvenidaInicio, tvRolInfo;
    private Button btnAccionRol, btnBuscarInscribirCurso, btnEntregarTarea, btnMisActividades, btnDocentePublicar;
    private RecyclerView recyclerInicioCursos;
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
        btnMisActividades = view.findViewById(R.id.btnMisActividades);
        btnDocentePublicar = view.findViewById(R.id.btnDocentePublicar);
        recyclerInicioCursos = view.findViewById(R.id.recyclerInicioCursos);

        recyclerInicioCursos.setLayoutManager(new LinearLayoutManager(requireContext()));

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
                requireActivity().runOnUiThread(() -> {
                    updateUIByRole().run();
                    cargarCursosEnLista();
                });
            }
        });
    }

    private Runnable updateUIByRole() {
        return () -> {
            tvBienvenidaInicio.setText("¡Hola, " + usuarioActual.nombre + "!");
            String rol = usuarioActual.rol != null ? usuarioActual.rol : "estudiante";

            if ("estudiante".equals(rol)) {
                tvRolInfo.setText("Rol: Estudiante. Selecciona abajo un curso para entrar a su espacio de trabajo y ver las publicaciones del profesor.");
                btnAccionRol.setVisibility(View.GONE); // Reemplazado por el listado directo en el RecyclerView

                btnBuscarInscribirCurso.setVisibility(View.VISIBLE);
                btnBuscarInscribirCurso.setOnClickListener(v -> mostrarDialogoBuscarCursoParaInscribirse());

                btnEntregarTarea.setVisibility(View.VISIBLE);
                btnEntregarTarea.setOnClickListener(v -> elegirCursoParaEntrega());

                btnMisActividades.setVisibility(View.VISIBLE);
                btnMisActividades.setOnClickListener(v -> mostrarDialogoMisActividades());

                btnDocentePublicar.setVisibility(View.GONE);

            } else if ("docente".equals(rol)) {
                tvRolInfo.setText("Rol: Docente. Selecciona tu curso abajo para gestionarlo, o crea uno nuevo.");
                btnAccionRol.setText("Crear Nuevo Curso");
                btnAccionRol.setVisibility(View.VISIBLE);
                btnAccionRol.setOnClickListener(v -> startActivity(new Intent(requireContext(), CrearCursoActivity.class)));

                btnBuscarInscribirCurso.setVisibility(View.GONE);
                btnEntregarTarea.setVisibility(View.GONE);
                btnMisActividades.setVisibility(View.GONE);
                btnDocentePublicar.setVisibility(View.GONE);

            } else if ("administrador".equals(rol)) {
                tvRolInfo.setText("Rol: Administrador. Gestiona usuarios, accesos y configuración general del sistema.");
                btnAccionRol.setText("Gestionar Usuarios");
                btnAccionRol.setVisibility(View.VISIBLE);
                btnAccionRol.setOnClickListener(v -> startActivity(new Intent(requireContext(), AdminUsuariosActivity.class)));

                btnBuscarInscribirCurso.setVisibility(View.GONE);
                btnEntregarTarea.setVisibility(View.GONE);
                btnMisActividades.setVisibility(View.GONE);
                btnDocentePublicar.setVisibility(View.GONE);
            }
        };
    }

    private void cargarCursosEnLista() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Curso> cursosList;
            String rol = usuarioActual.rol != null ? usuarioActual.rol : "estudiante";

            if ("estudiante".equals(rol)) {
                cursosList = db.matriculaDao().obtenerCursosMatriculados(usuarioActual.id);
            } else if ("docente".equals(rol)) {
                cursosList = db.cursoDao().obtenerCursosPorDocente(usuarioActual.id);
            } else {
                cursosList = new ArrayList<>(); // admin puede ver lista vacía o general
            }

            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                recyclerInicioCursos.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    @NonNull
                    @Override
                    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
                        return new RecyclerView.ViewHolder(v) {};
                    }

                    @Override
                    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                        Curso curso = cursosList.get(position);
                        TextView tv1 = holder.itemView.findViewById(android.R.id.text1);
                        TextView tv2 = holder.itemView.findViewById(android.R.id.text2);
                        tv1.setText(curso.codigo + " - " + curso.nombre);
                        tv2.setText("Periodo: " + curso.periodo + " | " + curso.descripcion);

                        holder.itemView.setOnClickListener(v -> {
                            Intent intent = new Intent(requireContext(), MaterialesCursoActivity.class);
                            intent.putExtra("CURSO_ID", curso.id);
                            startActivity(intent);
                        });
                    }

                    @Override
                    public int getItemCount() {
                        return cursosList.size();
                    }
                });
            });
        });
    }

    private void mostrarDialogoMisActividades() {
        Executors.newSingleThreadExecutor().execute(() -> {
            long ahora = System.currentTimeMillis();
            List<ActividadCurso> actividades = db.actividadCursoDao().obtenerActividadesParaEstudiante(usuarioActual.id, ahora);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (actividades.isEmpty()) {
                    Toast.makeText(requireContext(), "No tienes actividades abiertas o pendientes", Toast.LENGTH_SHORT).show();
                    return;
                }
                long dosDiasMs = 48L * 60 * 60 * 1000;

                String[] items = new String[actividades.size()];
                for (int i = 0; i < actividades.size(); i++) {
                    ActividadCurso act = actividades.get(i);
                    boolean menosDe48h = (act.fechaCierre - ahora) > 0 && (act.fechaCierre - ahora) <= dosDiasMs;
                    String alerta = menosDe48h ? " [⚠️ ¡VENCE EN < 48H!]" : "";
                    items[i] = act.titulo + " (" + act.porcentaje + "%) • Cierra: " + DateFormat.format("dd/MM/yyyy", act.fechaCierre) + alerta;
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("Mis Actividades Pendientes")
                        .setItems(items, (d, which) -> {
                            ActividadCurso seleccionada = actividades.get(which);
                            Executors.newSingleThreadExecutor().execute(() -> {
                                Entrega entregaPrevia = db.entregaDao().obtenerEntregaPorActividadYEstudiante(seleccionada.id, usuarioActual.id);
                                requireActivity().runOnUiThread(() -> {
                                    String estadoEntrega = "Pendiente";
                                    String infoExtra = "";
                                    if (entregaPrevia != null) {
                                        estadoEntrega = entregaPrevia.estado != null ? entregaPrevia.estado : "entregada";
                                        infoExtra = "\nComprobante: " + (entregaPrevia.comprobanteCodigo != null ? entregaPrevia.comprobanteCodigo : "N/A")
                                                + "\nFecha entrega: " + DateFormat.format("dd/MM/yyyy HH:mm", entregaPrevia.fechaEntrega)
                                                + (entregaPrevia.calificacion != null ? "\nNota: " + entregaPrevia.calificacion + " / 5.0" : "")
                                                + (entregaPrevia.retroalimentacion != null ? "\nRetroalimentación: " + entregaPrevia.retroalimentacion : "");
                                    }

                                    new AlertDialog.Builder(requireContext())
                                            .setTitle(seleccionada.titulo)
                                            .setMessage("Instrucciones: " + seleccionada.instrucciones
                                                    + "\nPeso en nota: " + seleccionada.porcentaje + "%"
                                                    + "\nEstado: " + estadoEntrega
                                                    + infoExtra
                                                    + "\nFecha límite: " + DateFormat.format("dd/MM/yyyy HH:mm", seleccionada.fechaCierre))
                                            .setPositiveButton("Cerrar", null)
                                            .show();
                                });
                            });
                        })
                        .setPositiveButton("Cerrar", null)
                        .show();
            });
        });
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

            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "¡Te has matriculado exitosamente en " + nombreCurso + "!", Toast.LENGTH_LONG).show();
                cargarCursosEnLista();
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
                        .setItems(nombres, (d, which) -> mostrarDialogoActividadesParaEntrega(cursos.get(which).id))
                        .show();
            });
        });
    }

    private void mostrarDialogoActividadesParaEntrega(int cursoId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            long ahora = System.currentTimeMillis();
            List<ActividadCurso> actividades = db.actividadCursoDao().obtenerActividadesParaEstudiante(usuarioActual.id, ahora);
            List<ActividadCurso> actividadesCurso = new ArrayList<>();
            for (ActividadCurso act : actividades) {
                if (act.cursoId == cursoId) actividadesCurso.add(act);
            }

            requireActivity().runOnUiThread(() -> {
                if (actividadesCurso.isEmpty()) {
                    Toast.makeText(requireContext(), "No hay actividades abiertas o disponibles en este curso", Toast.LENGTH_LONG).show();
                    return;
                }
                String[] nombres = new String[actividadesCurso.size()];
                for (int i = 0; i < actividadesCurso.size(); i++) {
                    nombres[i] = actividadesCurso.get(i).titulo + " (Cierra: " + DateFormat.format("dd/MM/yyyy", actividadesCurso.get(i).fechaCierre) + ")";
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("Selecciona la Actividad a Entregar")
                        .setItems(nombres, (d, which) -> {
                            ActividadCurso actElegida = actividadesCurso.get(which);
                            if (ahora > actElegida.fechaCierre && !actElegida.aceptaFueraDePlazo) {
                                Toast.makeText(requireContext(), "Plazo vencido: La actividad cerró y no acepta entregas fuera de plazo.", Toast.LENGTH_LONG).show();
                                return;
                            }
                            realizarEntrega(actElegida.id, cursoId, actElegida.titulo);
                        })
                        .show();
            });
        });
    }

    private void realizarEntrega(int actividadId, int cursoId, String tituloActividad) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Entrega entregaExistente = db.entregaDao().obtenerEntregaPorActividadYEstudiante(actividadId, usuarioActual.id);
            requireActivity().runOnUiThread(() -> {
                EditText inputTexto = new EditText(requireContext());
                inputTexto.setHint("Texto del trabajo o enlace...");
                if (entregaExistente != null && entregaExistente.descripcionEntrega != null) {
                    inputTexto.setText(entregaExistente.descripcionEntrega);
                }

                String accion = (entregaExistente != null) ? "Reemplazar Entrega" : "Enviar Entrega";

                new AlertDialog.Builder(requireContext())
                        .setTitle("Subir Trabajo: " + tituloActividad)
                        .setView(inputTexto)
                        .setPositiveButton(accion, (dialog, which) -> {
                            String desc = inputTexto.getText().toString().trim();
                            if (desc.isEmpty() && (entregaExistente == null || entregaExistente.archivoUrl == null)) {
                                Toast.makeText(requireContext(), "Ingresa el texto o detalle de tu entrega", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            long fechaEnvio = System.currentTimeMillis();
                            String codigoComprobante = "COMP-" + actividadId + "-" + usuarioActual.id + "-" + (fechaEnvio % 1000000);

                            Executors.newSingleThreadExecutor().execute(() -> {
                                Entrega entrega = (entregaExistente != null) ? entregaExistente : new Entrega();
                                entrega.cursoId = cursoId;
                                entrega.actividadId = actividadId;
                                entrega.estudianteId = usuarioActual.id;
                                entrega.tituloTarea = tituloActividad;
                                entrega.descripcionEntrega = desc;
                                entrega.fechaEntrega = fechaEnvio;
                                entrega.estado = "entregada";
                                entrega.comprobanteCodigo = codigoComprobante;

                                if (entregaExistente != null) {
                                    db.entregaDao().actualizarEntrega(entrega);
                                } else {
                                    db.entregaDao().insertarEntrega(entrega);
                                }

                                requireActivity().runOnUiThread(() -> {
                                    String fechaStr = DateFormat.format("dd/MM/yyyy HH:mm:ss", fechaEnvio).toString();
                                    new AlertDialog.Builder(requireContext())
                                            .setTitle("Comprobante Formal de Entrega (HU-13)")
                                            .setMessage("✅ ¡Entrega enviada con éxito!\n\n"
                                                    + "Actividad: " + tituloActividad + "\n"
                                                    + "Código Comprobante: " + codigoComprobante + "\n"
                                                    + "Fecha y Hora: " + fechaStr + "\n"
                                                    + "Estado: entregada")
                                            .setPositiveButton("Aceptar", null)
                                            .show();
                                });
                            });
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            });
        });
    }
}
