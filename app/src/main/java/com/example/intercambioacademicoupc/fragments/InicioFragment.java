package com.example.intercambioacademicoupc.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
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
import com.example.intercambioacademicoupc.R;
import com.example.intercambioacademicoupc.models.ActividadCurso;
import com.example.intercambioacademicoupc.models.AppDatabase;
import com.example.intercambioacademicoupc.models.Curso;
import com.example.intercambioacademicoupc.models.Entrega;
import com.example.intercambioacademicoupc.models.Matricula;
import com.example.intercambioacademicoupc.models.Usuario;
import com.example.intercambioacademicoupc.session.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class InicioFragment extends Fragment {

    private TextView tvBienvenidaInicio, tvRolInfo, tvTituloActividadesFecha;
    private CalendarView calendarView;
    private Button btnAccionRol, btnBuscarInscribirCurso, btnEntregarTarea, btnMisActividades, btnMisCalificaciones, btnDocentePublicar;
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
        tvTituloActividadesFecha = view.findViewById(R.id.tvTituloActividadesFecha);
        calendarView = view.findViewById(R.id.calendarView);
        btnAccionRol = view.findViewById(R.id.btnAccionRol);
        btnBuscarInscribirCurso = view.findViewById(R.id.btnBuscarInscribirCurso);
        btnEntregarTarea = view.findViewById(R.id.btnEntregarTarea);
        btnMisActividades = view.findViewById(R.id.btnMisActividades);
        btnMisCalificaciones = view.findViewById(R.id.btnMisCalificaciones);
        btnDocentePublicar = view.findViewById(R.id.btnDocentePublicar);
        recyclerInicioCursos = view.findViewById(R.id.recyclerInicioCursos);

        recyclerInicioCursos.setLayoutManager(new LinearLayoutManager(requireContext()));

        db = AppDatabase.getDatabase(requireContext());
        sessionManager = new SessionManager(requireContext());

        Calendar cal = Calendar.getInstance();
        cargarActividadesPorFecha(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            cargarActividadesPorFecha(year, month, dayOfMonth);
        });

        cargarDatosUsuario();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (usuarioActual != null) {
            Calendar cal = Calendar.getInstance();
            cargarActividadesPorFecha(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        }
    }

    private void cargarDatosUsuario() {
        int usuarioId = sessionManager.getUsuarioId();
        Executors.newSingleThreadExecutor().execute(() -> {
            usuarioActual = db.usuarioDao().buscarPorId(usuarioId);
            if (usuarioActual != null && isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    updateUIByRole().run();
                    Calendar cal = Calendar.getInstance();
                    cargarActividadesPorFecha(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
                });
            }
        });
    }

    private Runnable updateUIByRole() {
        return () -> {
            tvBienvenidaInicio.setText("¡Hola, " + usuarioActual.nombre + "!");
            String rol = usuarioActual.rol != null ? usuarioActual.rol : "estudiante";

            if ("estudiante".equals(rol)) {
                tvRolInfo.setText("Rol: Estudiante. Selecciona una fecha en el calendario o consulta tus calificaciones (HU-15/HU-16).");
                btnAccionRol.setVisibility(View.GONE);

                btnBuscarInscribirCurso.setVisibility(View.VISIBLE);
                btnBuscarInscribirCurso.setOnClickListener(v -> mostrarDialogoBuscarCursoParaInscribirse());

                btnEntregarTarea.setVisibility(View.VISIBLE);
                btnEntregarTarea.setOnClickListener(v -> elegirCursoParaEntrega());

                btnMisActividades.setVisibility(View.VISIBLE);
                btnMisActividades.setOnClickListener(v -> mostrarDialogoMisActividades());

                btnMisCalificaciones.setVisibility(View.VISIBLE);
                btnMisCalificaciones.setOnClickListener(v -> mostrarDialogoMisCalificaciones());

                btnDocentePublicar.setVisibility(View.GONE);

            } else if ("docente".equals(rol)) {
                tvRolInfo.setText("Rol: Docente. Selecciona una fecha en el calendario para consultar actividades o crea un nuevo curso.");
                btnAccionRol.setText("Crear Nuevo Curso");
                btnAccionRol.setVisibility(View.VISIBLE);
                btnAccionRol.setOnClickListener(v -> startActivity(new Intent(requireContext(), CrearCursoActivity.class)));

                btnBuscarInscribirCurso.setVisibility(View.GONE);
                btnEntregarTarea.setVisibility(View.GONE);
                btnMisActividades.setVisibility(View.GONE);
                btnMisCalificaciones.setVisibility(View.GONE);
                btnDocentePublicar.setVisibility(View.GONE);

            } else if ("administrador".equals(rol)) {
                tvRolInfo.setText("Rol: Administrador. Gestiona usuarios, accesos y configuración general del sistema.");
                btnAccionRol.setText("Gestionar Usuarios");
                btnAccionRol.setVisibility(View.VISIBLE);
                btnAccionRol.setOnClickListener(v -> startActivity(new Intent(requireContext(), AdminUsuariosActivity.class)));

                btnBuscarInscribirCurso.setVisibility(View.GONE);
                btnEntregarTarea.setVisibility(View.GONE);
                btnMisActividades.setVisibility(View.GONE);
                btnMisCalificaciones.setVisibility(View.GONE);
                btnDocentePublicar.setVisibility(View.GONE);
            }
        };
    }

    private void cargarActividadesPorFecha(int year, int month, int dayOfMonth) {
        if (usuarioActual == null) return;
        tvTituloActividadesFecha.setText("Actividades para la fecha: " + dayOfMonth + "/" + (month + 1) + "/" + year);

        Executors.newSingleThreadExecutor().execute(() -> {
            String rol = usuarioActual.rol != null ? usuarioActual.rol : "estudiante";
            List<ActividadCurso> todas;
            if ("docente".equals(rol)) {
                todas = db.actividadCursoDao().obtenerTodasActividadesParaDocente(usuarioActual.id);
            } else {
                todas = db.actividadCursoDao().obtenerTodasActividadesParaEstudiante(usuarioActual.id);
            }

            List<ActividadCurso> filtradas = new ArrayList<>();
            Calendar calTarget = Calendar.getInstance();

            for (ActividadCurso act : todas) {
                calTarget.setTimeInMillis(act.fechaCierre);
                int y = calTarget.get(Calendar.YEAR);
                int m = calTarget.get(Calendar.MONTH);
                int d = calTarget.get(Calendar.DAY_OF_MONTH);

                if (y == year && m == month && d == dayOfMonth) {
                    if ("estudiante".equals(rol) && (!act.visible || act.fechaApertura > System.currentTimeMillis())) {
                        continue;
                    }
                    filtradas.add(act);
                }
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
                        ActividadCurso act = filtradas.get(position);
                        TextView tv1 = holder.itemView.findViewById(android.R.id.text1);
                        TextView tv2 = holder.itemView.findViewById(android.R.id.text2);
                        tv1.setText(act.titulo + " (" + act.porcentaje + "%)");
                        tv2.setText("Instrucciones: " + act.instrucciones + " | Cierra a las: " + DateFormat.format("HH:mm", act.fechaCierre));

                        holder.itemView.setOnClickListener(v -> {
                            new AlertDialog.Builder(requireContext())
                                    .setTitle(act.titulo)
                                    .setMessage("Instrucciones:\n" + act.instrucciones + "\n\nPeso en nota: " + act.porcentaje + "%\nFecha de cierre: " + DateFormat.format("dd/MM/yyyy HH:mm", act.fechaCierre))
                                    .setPositiveButton("Cerrar", null)
                                    .show();
                        });
                    }

                    @Override
                    public int getItemCount() {
                        return filtradas.size();
                    }
                });
            });
        });
    }

    private void mostrarDialogoMisCalificaciones() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Entrega> entregas = db.entregaDao().obtenerTodasEntregasEstudiante(usuarioActual.id);
            List<Entrega> calificadas = new ArrayList<>();
            for (Entrega e : entregas) {
                if (e.calificacion != null) calificadas.add(e);
            }

            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (calificadas.isEmpty()) {
                    Toast.makeText(requireContext(), "No tienes calificaciones registradas aún", Toast.LENGTH_SHORT).show();
                    return;
                }

                double sumaNotasPonderadas = 0;
                double sumaPorcentajes = 0;

                String[] items = new String[calificadas.size()];
                for (int i = 0; i < calificadas.size(); i++) {
                    Entrega ent = calificadas.get(i);
                    ActividadCurso act = db.actividadCursoDao().obtenerPorId(ent.actividadId);
                    int porcentaje = act != null ? act.porcentaje : 20;
                    sumaNotasPonderadas += (ent.calificacion * porcentaje);
                    sumaPorcentajes += porcentaje;

                    items[i] = ent.tituloTarea + " — Nota: " + ent.calificacion + " / 5.0 (" + porcentaje + "%)" + (ent.estadoRevision != null ? " [Revisión: " + ent.estadoRevision + "]" : "");
                }

                double promedioPonderado = sumaPorcentajes > 0 ? (sumaNotasPonderadas / sumaPorcentajes) : 0;

                new AlertDialog.Builder(requireContext())
                        .setTitle("HU-15: Mis Calificaciones (Promedio: " + String.format(Locale.getDefault(), "%.2f", promedioPonderado) + " / 5.0)")
                        .setItems(items, (d, which) -> {
                            Entrega seleccionada = calificadas.get(which);
                            mostrarDetalleCalificacionYRevision(seleccionada);
                        })
                        .setPositiveButton("Cerrar", null)
                        .show();
            });
        });
    }

    private void mostrarDetalleCalificacionYRevision(Entrega entrega) {
        String retro = entrega.retroalimentacion != null ? entrega.retroalimentacion : "Sin comentarios";
        String estadoRev = entrega.estadoRevision != null ? "\nEstado de revisión: " + entrega.estadoRevision : "";

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle("Detalle: " + entrega.tituloTarea)
                .setMessage("Calificación: " + entrega.calificacion + " / 5.0" +
                        "\nRetroalimentación del docente:\n" + retro +
                        estadoRev);

        long cincoDiasMs = 5L * 24 * 60 * 60 * 1000;
        boolean dentroDe5Dias = (System.currentTimeMillis() - entrega.fechaCalificacion) <= cincoDiasMs;

        if (dentroDe5Dias && (entrega.estadoRevision == null || entrega.estadoRevision.isEmpty())) {
            builder.setNeutralButton("Solicitar Revisión (HU-16)", (d, w) -> {
                mostrarDialogoMotivoRevision(entrega);
            });
        }

        builder.setPositiveButton("Cerrar", null).show();
    }

    private void mostrarDialogoMotivoRevision(Entrega entrega) {
        EditText input = new EditText(requireContext());
        input.setHint("Explique detalladamente el motivo de la revisión (Mínimo 100 caracteres)...");

        new AlertDialog.Builder(requireContext())
                .setTitle("HU-16: Solicitud de Revisión de Calificación")
                .setView(input)
                .setPositiveButton("Enviar Solicitud", (dialog, which) -> {
                    String motivo = input.getText().toString().trim();
                    if (motivo.length() < 100) {
                        Toast.makeText(requireContext(), "El motivo debe tener al menos 100 caracteres (actuales: " + motivo.length() + ")", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Executors.newSingleThreadExecutor().execute(() -> {
                        entrega.solicitudRevision = true;
                        entrega.motivoRevision = motivo;
                        entrega.estadoRevision = "en revisión";
                        db.entregaDao().actualizarEntrega(entrega);

                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Solicitud enviada al docente con éxito (Estado: En revisión)", Toast.LENGTH_LONG).show()
                        );
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
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
                            new AlertDialog.Builder(requireContext())
                                    .setTitle(seleccionada.titulo)
                                    .setMessage("Instrucciones: " + seleccionada.instrucciones + "\nPeso en nota: " + seleccionada.porcentaje + "%\nAcepta fuera de plazo: " + (seleccionada.aceptaFueraDePlazo ? "Sí" : "No") + "\nFecha de cierre: " + DateFormat.format("dd/MM/yyyy HH:mm", seleccionada.fechaCierre))
                                    .setPositiveButton("Cerrar", null)
                                    .show();
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

            requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "¡Te has matriculado exitosamente en " + nombreCurso + "!", Toast.LENGTH_LONG).show()
            );
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
        EditText inputTitulo = new EditText(requireContext());
        inputTitulo.setHint("Comentarios o Enlace del trabajo (Ej: https://github.com/...)");

        new AlertDialog.Builder(requireContext())
                .setTitle("Enviar Entrega: " + tituloActividad)
                .setView(inputTitulo)
                .setPositiveButton("Enviar Entrega", (dialog, which) -> {
                    String desc = inputTitulo.getText().toString().trim();
                    Executors.newSingleThreadExecutor().execute(() -> {
                        Entrega entrega = new Entrega();
                        entrega.cursoId = cursoId;
                        entrega.estudianteId = usuarioActual.id;
                        entrega.tituloTarea = tituloActividad;
                        entrega.descripcionEntrega = desc.isEmpty() ? "Entrega de actividad" : desc;
                        entrega.fechaEntrega = System.currentTimeMillis();
                        entrega.estado = "Entregado";
                        db.entregaDao().insertarEntrega(entrega);

                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "¡Trabajo entregado con éxito!", Toast.LENGTH_LONG).show()
                        );
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
