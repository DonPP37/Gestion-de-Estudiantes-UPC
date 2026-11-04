package com.example.intercambioacademicoupc;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.intercambioacademicoupc.models.AppDatabase;
import com.example.intercambioacademicoupc.models.Convocatoria;
import com.example.intercambioacademicoupc.models.Postulacion;
import com.example.intercambioacademicoupc.session.SessionManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

public class MovilidadActivity extends AppCompatActivity {

    private Button btnTabConvocatorias, btnTabHistorial, btnTabMisPostulaciones, btnFiltrarConvocatorias, btnCrearConvocatoriaAdmin;
    private EditText etFiltroPais, etFiltroIdioma;
    private LinearLayout layoutFiltrosMovilidad;
    private RecyclerView recyclerMovilidad;

    private AppDatabase db;
    private SessionManager sessionManager;
    private int usuarioId;
    private String rolUsuario;
    private int pestanaActual = 0; // 0 = Activas, 1 = Historial, 2 = Mis Postulaciones

    private String pdfSeleccionadoTemp = null;

    private final ActivityResultLauncher<String[]> selectorPdf =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try {
                    getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                }
                pdfSeleccionadoTemp = uri.toString();
                Toast.makeText(this, "Documento PDF adjuntado con éxito", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movilidad);

        btnTabConvocatorias = findViewById(R.id.btnTabConvocatorias);
        btnTabHistorial = findViewById(R.id.btnTabHistorial);
        btnTabMisPostulaciones = findViewById(R.id.btnTabMisPostulaciones);
        btnFiltrarConvocatorias = findViewById(R.id.btnFiltrarConvocatorias);
        btnCrearConvocatoriaAdmin = findViewById(R.id.btnCrearConvocatoriaAdmin);

        etFiltroPais = findViewById(R.id.etFiltroPais);
        etFiltroIdioma = findViewById(R.id.etFiltroIdioma);
        layoutFiltrosMovilidad = findViewById(R.id.layoutFiltrosMovilidad);
        recyclerMovilidad = findViewById(R.id.recyclerMovilidad);

        recyclerMovilidad.setLayoutManager(new LinearLayoutManager(this));

        db = AppDatabase.getDatabase(this);
        sessionManager = new SessionManager(this);

        usuarioId = sessionManager.getUsuarioId();
        rolUsuario = sessionManager.getRol();

        boolean esAdmin = "administrador".equals(rolUsuario);
        if (esAdmin) {
            btnCrearConvocatoriaAdmin.setVisibility(View.VISIBLE);
            btnTabMisPostulaciones.setText("Revisar Postulaciones");
        } else {
            btnCrearConvocatoriaAdmin.setVisibility(View.GONE);
        }

        btnTabConvocatorias.setOnClickListener(v -> cambiarPestaña(0));
        btnTabHistorial.setOnClickListener(v -> cambiarPestaña(1));
        btnTabMisPostulaciones.setOnClickListener(v -> cambiarPestaña(2));

        btnFiltrarConvocatorias.setOnClickListener(v -> aplicarFiltro());
        btnCrearConvocatoriaAdmin.setOnClickListener(v -> mostrarDialogoCrearConvocatoria());

        cargarConvocatoriasActivas();
    }

    private void cambiarPestaña(int pestana) {
        pestanaActual = pestana;
        btnTabConvocatorias.setBackgroundColor(Color.parseColor(pestanaActual == 0 ? "#1976D2" : "#757575"));
        btnTabHistorial.setBackgroundColor(Color.parseColor(pestanaActual == 1 ? "#1976D2" : "#757575"));
        btnTabMisPostulaciones.setBackgroundColor(Color.parseColor(pestanaActual == 2 ? "#1976D2" : "#757575"));

        layoutFiltrosMovilidad.setVisibility(pestanaActual == 0 ? View.VISIBLE : View.GONE);

        if (pestanaActual == 0) {
            cargarConvocatoriasActivas();
        } else if (pestanaActual == 1) {
            cargarHistorialCerradas();
        } else if (pestanaActual == 2) {
            if ("administrador".equals(rolUsuario)) {
                cargarPostulacionesAdmin();
            } else {
                cargarMisPostulacionesEstudiante();
            }
        }
    }

    private void aplicarFiltro() {
        String pais = etFiltroPais.getText().toString().trim();
        String idioma = etFiltroIdioma.getText().toString().trim();

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Convocatoria> lista = db.convocatoriaDao().buscarConvocatoriasFiltro(pais, idioma, "");
            mostrarListaConvocatoriasUI(lista, true);
        });
    }

    private void cargarConvocatoriasActivas() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Convocatoria> lista = db.convocatoriaDao().obtenerConvocatoriasActivas();
            mostrarListaConvocatoriasUI(lista, true);
        });
    }

    private void cargarHistorialCerradas() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Convocatoria> lista = db.convocatoriaDao().obtenerHistorialCerradas();
            mostrarListaConvocatoriasUI(lista, false);
        });
    }

    private void mostrarListaConvocatoriasUI(List<Convocatoria> lista, boolean esActiva) {
        runOnUiThread(() -> {
            if (lista.isEmpty()) {
                Toast.makeText(this, "No se encontraron convocatorias", Toast.LENGTH_SHORT).show();
            }
            recyclerMovilidad.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                @NonNull
                @Override
                public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    View v = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
                    return new RecyclerView.ViewHolder(v) {};
                }

                @Override
                public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                    Convocatoria conv = lista.get(position);
                    TextView tv1 = holder.itemView.findViewById(android.R.id.text1);
                    TextView tv2 = holder.itemView.findViewById(android.R.id.text2);

                    tv1.setText("🏫 " + conv.institucionDestino + " (" + conv.pais + ")");
                    tv2.setText("Idioma: " + conv.idioma + " | Periodo: " + conv.periodo + " | Cierra: " + DateFormat.format("dd/MM/yyyy", conv.fechaCierre));

                    holder.itemView.setOnClickListener(v -> mostrarDetalleConvocatoria(conv, esActiva));
                }

                @Override
                public int getItemCount() {
                    return lista.size();
                }
            });
        });
    }

    private void mostrarDetalleConvocatoria(Convocatoria conv, boolean esActiva) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Convocatoria: " + conv.institucionDestino)
                .setMessage("País: " + conv.pais + "\nIdioma: " + conv.idioma + "\nPeriodo: " + conv.periodo
                        + "\n\nRequisitos:\n" + conv.requisitos
                        + "\n\nDocumentos exigidos:\n" + conv.documentosExigidos
                        + "\n\nFecha de cierre: " + DateFormat.format("dd/MM/yyyy", conv.fechaCierre)
                        + "\nTérminos: " + conv.enlaceTerminos);

        if (esActiva && "estudiante".equals(rolUsuario)) {
            builder.setPositiveButton("Postularme (HU-20)", (d, w) -> prepararPostulacion(conv));
        }

        builder.setNegativeButton("Cerrar", null).show();
    }

    private void prepararPostulacion(Convocatoria conv) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Postulacion existente = db.postulacionDao().verificarPostulacionEstudiante(conv.id, usuarioId);
            runOnUiThread(() -> {
                if (existente != null) {
                    Toast.makeText(this, "HU-20: Ya te has postulado previamente a esta convocatoria (Estado: " + existente.estado + ").", Toast.LENGTH_LONG).show();
                    return;
                }
                mostrarFormularioPostulacion(conv);
            });
        });
    }

    private void mostrarFormularioPostulacion(Convocatoria conv) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int p = 32;
        layout.setPadding(p, p, p, p);

        final EditText etSemestre = new EditText(this);
        etSemestre.setHint("Semestre actual (Ej: 6to Semestre)");
        layout.addView(etSemestre);

        final EditText etCartaIntencion = new EditText(this);
        etCartaIntencion.setHint("Carta de Intención / Explicación del interés...");
        etCartaIntencion.setLines(3);
        layout.addView(etCartaIntencion);

        Button btnAdjuntarPdf = new Button(this);
        btnAdjuntarPdf.setText("Adjuntar Documentos PDF (Notas, Paz/Salvo, Idioma, Motivación)");
        btnAdjuntarPdf.setOnClickListener(v -> selectorPdf.launch(new String[]{"application/pdf"}));
        layout.addView(btnAdjuntarPdf);

        new AlertDialog.Builder(this)
                .setTitle("HU-20: Formulario de Postulación")
                .setView(layout)
                .setPositiveButton("Enviar Postulación", (dialog, which) -> {
                    String semestre = etSemestre.getText().toString().trim();
                    String cartaTexto = etCartaIntencion.getText().toString().trim();

                    if (semestre.isEmpty() || cartaTexto.isEmpty()) {
                        Toast.makeText(this, "El semestre y la carta de intención son obligatorios.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Executors.newSingleThreadExecutor().execute(() -> {
                        Postulacion pos = new Postulacion();
                        pos.convocatoriaId = conv.id;
                        pos.estudianteId = usuarioId;
                        pos.semestreActual = semestre;
                        pos.cartaIntencionPdf = cartaTexto;
                        pos.certificadoNotasPdf = pdfSeleccionadoTemp;
                        pos.pazYSalvoPdf = pdfSeleccionadoTemp;
                        pos.certificadoIdiomaPdf = pdfSeleccionadoTemp;
                        pos.cartaMotivacionPdf = pdfSeleccionadoTemp;
                        pos.fechaPostulacion = System.currentTimeMillis();
                        pos.estado = "en revisión";

                        db.postulacionDao().insertarPostulacion(pos);

                        runOnUiThread(() -> {
                            Toast.makeText(this, "¡Postulación registrada con éxito en estado 'en revisión'!", Toast.LENGTH_LONG).show();
                            pdfSeleccionadoTemp = null;
                            cambiarPestaña(2);
                        });
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void cargarMisPostulacionesEstudiante() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Postulacion> lista = db.postulacionDao().obtenerPostulacionesPorEstudiante(usuarioId);
            runOnUiThread(() -> {
                if (lista.isEmpty()) {
                    Toast.makeText(this, "Aún no te has postulado a ninguna convocatoria", Toast.LENGTH_SHORT).show();
                }
                recyclerMovilidad.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    @NonNull
                    @Override
                    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View v = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
                        return new RecyclerView.ViewHolder(v) {};
                    }

                    @Override
                    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                        Postulacion pos = lista.get(position);
                        TextView tv1 = holder.itemView.findViewById(android.R.id.text1);
                        TextView tv2 = holder.itemView.findViewById(android.R.id.text2);

                        tv1.setText("✈️ Postulación #" + pos.id + " — Semestre: " + pos.semestreActual);
                        tv2.setText("Estado: " + pos.estado + " | Fecha: " + DateFormat.format("dd/MM/yyyy", pos.fechaPostulacion)
                                + (pos.observacionAdmin != null ? (" | Obs: " + pos.observacionAdmin) : ""));

                        holder.itemView.setOnClickListener(v -> {
                            new AlertDialog.Builder(MovilidadActivity.this)
                                    .setTitle("Detalle de Postulación #" + pos.id)
                                    .setMessage("Semestre: " + pos.semestreActual + "\nEstado: " + pos.estado + "\nCarta/Intención:\n" + pos.cartaIntencionPdf + "\n\nObservaciones Admin:\n" + (pos.observacionAdmin != null ? pos.observacionAdmin : "Sin observaciones"))
                                    .setPositiveButton("Cerrar", null)
                                    .show();
                        });
                    }

                    @Override
                    public int getItemCount() {
                        return lista.size();
                    }
                });
            });
        });
    }

    private void cargarPostulacionesAdmin() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Postulacion> lista = db.postulacionDao().obtenerTodasPostulaciones();
            runOnUiThread(() -> {
                if (lista.isEmpty()) {
                    Toast.makeText(this, "No hay postulaciones registradas", Toast.LENGTH_SHORT).show();
                }
                recyclerMovilidad.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    @NonNull
                    @Override
                    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View v = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
                        return new RecyclerView.ViewHolder(v) {};
                    }

                    @Override
                    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                        Postulacion pos = lista.get(position);
                        TextView tv1 = holder.itemView.findViewById(android.R.id.text1);
                        TextView tv2 = holder.itemView.findViewById(android.R.id.text2);

                        tv1.setText("📋 Postulación #" + pos.id + " (Estudiante ID: " + pos.estudianteId + ")");
                        tv2.setText("Semestre: " + pos.semestreActual + " | Estado: " + pos.estado);

                        holder.itemView.setOnClickListener(v -> mostrarDialogoRevisionAdmin(pos));
                    }

                    @Override
                    public int getItemCount() {
                        return lista.size();
                    }
                });
            });
        });
    }

    private void mostrarDialogoRevisionAdmin(Postulacion pos) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int p = 32;
        layout.setPadding(p, p, p, p);

        final EditText etObs = new EditText(this);
        etObs.setHint("Observaciones del Administrador...");
        if (pos.observacionAdmin != null) etObs.setText(pos.observacionAdmin);
        layout.addView(etObs);

        new AlertDialog.Builder(this)
                .setTitle("Revisar Postulación #" + pos.id)
                .setMessage("Semestre: " + pos.semestreActual + "\nCarta de intención:\n" + pos.cartaIntencionPdf)
                .setView(layout)
                .setPositiveButton("Aprobar", (dialog, which) -> {
                    pos.estado = "aprobada";
                    pos.observacionAdmin = etObs.getText().toString().trim();
                    guardarRevisionAdmin(pos);
                })
                .setNegativeButton("Rechazar", (dialog, which) -> {
                    pos.estado = "rechazada";
                    pos.observacionAdmin = etObs.getText().toString().trim();
                    guardarRevisionAdmin(pos);
                })
                .setNeutralButton("Cancelar", null)
                .show();
    }

    private void guardarRevisionAdmin(Postulacion pos) {
        Executors.newSingleThreadExecutor().execute(() -> {
            db.postulacionDao().actualizarPostulacion(pos);
            runOnUiThread(() -> {
                Toast.makeText(this, "Postulación actualizada a estado '" + pos.estado + "'", Toast.LENGTH_SHORT).show();
                cargarPostulacionesAdmin();
            });
        });
    }

    private void mostrarDialogoCrearConvocatoria() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int p = 32;
        layout.setPadding(p, p, p, p);

        final EditText etInst = new EditText(this); etInst.setHint("Institución Destino (Ej: UPM Madrid)"); layout.addView(etInst);
        final EditText etPais = new EditText(this); etPais.setHint("País (Ej: España)"); layout.addView(etPais);
        final EditText etIdioma = new EditText(this); etIdioma.setHint("Idioma (Ej: Español)"); layout.addView(etIdioma);
        final EditText etPeriodo = new EditText(this); etPeriodo.setHint("Periodo (Ej: 2027-1)"); layout.addView(etPeriodo);
        final EditText etRequisitos = new EditText(this); etRequisitos.setHint("Requisitos"); layout.addView(etRequisitos);
        final EditText etDocExigidos = new EditText(this); etDocExigidos.setHint("Documentos exigidos"); layout.addView(etDocExigidos);
        final EditText etTerminos = new EditText(this); etTerminos.setHint("Enlace a términos URL"); layout.addView(etTerminos);

        new AlertDialog.Builder(this)
                .setTitle("Crear Convocatoria de Intercambio")
                .setView(layout)
                .setPositiveButton("Crear Convocatoria", (dialog, which) -> {
                    String inst = etInst.getText().toString().trim();
                    String pais = etPais.getText().toString().trim();

                    if (inst.isEmpty() || pais.isEmpty()) {
                        Toast.makeText(this, "Institución y País son obligatorios", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Executors.newSingleThreadExecutor().execute(() -> {
                        Convocatoria conv = new Convocatoria();
                        conv.institucionDestino = inst;
                        conv.pais = pais;
                        conv.idioma = etIdioma.getText().toString().trim();
                        conv.periodo = etPeriodo.getText().toString().trim();
                        conv.requisitos = etRequisitos.getText().toString().trim();
                        conv.documentosExigidos = etDocExigidos.getText().toString().trim();
                        conv.enlaceTerminos = etTerminos.getText().toString().trim();
                        conv.fechaCierre = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000);
                        conv.estado = "activa";

                        db.convocatoriaDao().insertarConvocatoria(conv);

                        runOnUiThread(() -> {
                            Toast.makeText(this, "Convocatoria creada exitosamente", Toast.LENGTH_SHORT).show();
                            cargarConvocatoriasActivas();
                        });
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
