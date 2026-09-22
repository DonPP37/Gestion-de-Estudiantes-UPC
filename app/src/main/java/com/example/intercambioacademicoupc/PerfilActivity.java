package com.example.intercambioacademicoupc;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.intercambioacademicoupc.models.AppDatabase;
import com.example.intercambioacademicoupc.models.CambioPerfil;
import com.example.intercambioacademicoupc.models.Usuario;
import com.example.intercambioacademicoupc.session.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * HU-04 — Consultar y actualizar los datos del perfil.
 *
 * Criterios de aceptación cubiertos:
 *  1. Muestra nombre, documento, código, programa, correo y foto.
 *  2. El usuario edita teléfono, foto y contraseña; los datos académicos son de solo lectura
 *     (los edita el administrador en HU-05).
 *  3. Los cambios se validan y se confirman con un mensaje de éxito.
 *  4. Cada cambio queda registrado con su fecha (campo fechaActualizacion + tabla cambios_perfil).
 */
public class PerfilActivity extends AppCompatActivity {

    /** Política mínima de contraseña (HU-03): 8 caracteres, una mayúscula y un número. */
    private static final Pattern POLITICA_PASSWORD =
            Pattern.compile("^(?=.*[A-Z])(?=.*\\d).{8,}$");

    private static final Pattern TELEFONO_VALIDO = Pattern.compile("^\\d{7,15}$");

    private ImageView ivFoto;
    private TextView tvNombre, tvDocumento, tvCodigo, tvPrograma, tvCorreo, tvUltimaActualizacion;
    private EditText etTelefono, etPasswordActual, etPasswordNueva, etPasswordConfirmar;
    private Button btnCambiarFoto, btnGuardarContacto, btnCambiarPassword, btnHistorial, btnCerrarSesion;
    /** HU-05 / HU-06: accesos que solo ve el rol correspondiente. */
    private Button btnAdminUsuarios, btnCrearCurso, btnAdminVerCursos;

    private AppDatabase db;
    private SessionManager sesion;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private Usuario usuarioActual;
    private String fotoUriSeleccionada;   // uri elegida pero aún no guardada

    /** Selector de imagen. Pedimos permiso persistente para poder mostrarla en próximos arranques. */
    private final ActivityResultLauncher<String[]> selectorFoto =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try {
                    getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                    // Algunos proveedores no otorgan permiso persistente; la foto se verá igual
                    // durante esta sesión.
                }
                fotoUriSeleccionada = uri.toString();
                ivFoto.setImageURI(uri);
                Toast.makeText(this, "Foto seleccionada. Pulsa \"Guardar cambios\".",
                        Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        db = AppDatabase.getDatabase(getApplicationContext());
        sesion = new SessionManager(this);

        // Criterio: solo un usuario autenticado accede al perfil.
        if (!sesion.haySesionActiva()) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            irALogin();
            return;
        }

        vincularVistas();

        btnCambiarFoto.setOnClickListener(v ->
                selectorFoto.launch(new String[]{"image/*"}));
        btnGuardarContacto.setOnClickListener(v -> guardarDatosDeContacto());
        btnCambiarPassword.setOnClickListener(v -> cambiarPassword());
        btnHistorial.setOnClickListener(v -> mostrarHistorial());
        btnCerrarSesion.setOnClickListener(v -> {
            sesion.cerrarSesion();
            irALogin();
        });

        btnAdminUsuarios.setOnClickListener(v ->
                startActivity(new Intent(this, AdminUsuariosActivity.class)));
        btnCrearCurso.setOnClickListener(v -> {
            // El docente ve directamente sus cursos creados para interactuar con ellos
            Intent intent = new Intent(this, AdminCursosGeneralActivity.class);
            startActivity(intent);
        });

        btnAdminVerCursos = findViewById(R.id.btnAdminVerCursos);
        if (btnAdminVerCursos != null) {
            btnAdminVerCursos.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminCursosGeneralActivity.class);
                startActivity(intent);
            });
        }

        Button btnIrAMatricularme = findViewById(R.id.btn_ir_a_matricularme);
        if (btnIrAMatricularme != null) {
            btnIrAMatricularme.setOnClickListener(v -> {
                Intent intent = new Intent(this, MatriculaEstudianteActivity.class);
                startActivityForResult(intent, 100);
            });
        }

        Button btnEstudianteVerAulas = findViewById(R.id.btn_estudiante_ver_aulas);
        if (btnEstudianteVerAulas != null) {
            btnEstudianteVerAulas.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminCursosGeneralActivity.class);
                startActivity(intent);
            });
        }

        aplicarMenuPorRol();

        cargarPerfil();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sesion.haySesionActiva() && "estudiante".equals(sesion.getRol())) {
            cargarCursosEstudiante(sesion.getUsuarioId());
        }
    }

    private void vincularVistas() {
        ivFoto = findViewById(R.id.ivFotoPerfil);
        tvNombre = findViewById(R.id.tvNombre);
        tvDocumento = findViewById(R.id.tvDocumento);
        tvCodigo = findViewById(R.id.tvCodigo);
        tvPrograma = findViewById(R.id.tvPrograma);
        tvCorreo = findViewById(R.id.tvCorreo);
        tvUltimaActualizacion = findViewById(R.id.tvUltimaActualizacion);

        etTelefono = findViewById(R.id.etTelefono);
        etPasswordActual = findViewById(R.id.etPasswordActual);
        etPasswordNueva = findViewById(R.id.etPasswordNueva);
        etPasswordConfirmar = findViewById(R.id.etPasswordConfirmar);

        btnCambiarFoto = findViewById(R.id.btnCambiarFoto);
        btnGuardarContacto = findViewById(R.id.btnGuardarContacto);
        btnCambiarPassword = findViewById(R.id.btnCambiarPassword);
        btnHistorial = findViewById(R.id.btnHistorial);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        btnAdminUsuarios = findViewById(R.id.btnAdminUsuarios);
        btnCrearCurso = findViewById(R.id.btnCrearCurso);
    }

    /**
     * HU-05: cada persona solo ve las funciones de su rol. El rol viene de la sesion, que se
     * abrio en el login, asi que un cambio de rol se aplica en el siguiente inicio de sesion.
     */
    private void aplicarMenuPorRol() {
        String rol = sesion.getRol();
        boolean isAdmin = "administrador".equals(rol);
        boolean isDocente = "docente".equals(rol);
        boolean isEstudiante = "estudiante".equals(rol);

        btnAdminUsuarios.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        btnCrearCurso.setVisibility(isDocente ? View.VISIBLE : View.GONE);
        if (btnAdminVerCursos != null) {
            btnAdminVerCursos.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        }

        // Ocultar todo el bloque si no tiene roles especiales
        View layoutAccesos = findViewById(R.id.layout_accesos_admin);
        if (layoutAccesos != null) {
            layoutAccesos.setVisibility((isAdmin || isDocente) ? View.VISIBLE : View.GONE);
        }

        // Mostrar panel de cursos activos si es estudiante (HU-08)
        View layoutCursos = findViewById(R.id.layout_cursos_estudiante);
        if (layoutCursos != null) {
            layoutCursos.setVisibility(isEstudiante ? View.VISIBLE : View.GONE);
        }
    }

    // ------------------------------------------------------------------
    // Criterio 1: consultar los datos del perfil
    // ------------------------------------------------------------------
    private void cargarPerfil() {
        final int id = sesion.getUsuarioId();
        executor.execute(() -> {
            Usuario u = db.usuarioDao().buscarPorId(id);
            runOnUiThread(() -> {
                if (u == null) {
                    Toast.makeText(this, "No se encontró el usuario", Toast.LENGTH_SHORT).show();
                    sesion.cerrarSesion();
                    irALogin();
                    return;
                }
                usuarioActual = u;
                pintarPerfil(u);
                
                // Si es estudiante, cargar sus asignaturas matriculadas
                if ("estudiante".equals(sesion.getRol())) {
                    cargarCursosEstudiante(u.id);
                }
            });
        });
    }

    private void cargarCursosEstudiante(int estudianteId) {
        executor.execute(() -> {
            List<com.example.intercambioacademicoupc.models.Curso> cursos = db.matriculaDao().obtenerCursosMatriculados(estudianteId);
            runOnUiThread(() -> {
                TextView tvVacio = findViewById(R.id.tv_lista_cursos_vacia);
                TextView tvContenido = findViewById(R.id.tv_cursos_matriculados);
                
                if (cursos != null && !cursos.isEmpty()) {
                    tvVacio.setVisibility(View.GONE);
                    tvContenido.setVisibility(View.VISIBLE);
                    
                    StringBuilder sb = new StringBuilder();
                    for (com.example.intercambioacademicoupc.models.Curso c : cursos) {
                        sb.append("📖 ").append(c.nombre)
                          .append("\n📌 Código: ").append(c.codigo)
                          .append(" | 🗓️ Periodo: ").append(c.periodo)
                          .append("\n----------------------------------------\n");
                    }
                    tvContenido.setText(sb.toString());
                } else {
                    tvVacio.setVisibility(View.VISIBLE);
                    tvContenido.setVisibility(View.GONE);
                }
            });
        });
    }

    private void pintarPerfil(Usuario u) {
        tvNombre.setText(texto(u.nombre) + " " + texto(u.apellido));
        tvDocumento.setText("Documento: " + texto(u.documento));
        tvCodigo.setText("Código estudiantil: " + texto(u.codigoEstudiantil));
        tvPrograma.setText("Programa: " + texto(u.programa));
        tvCorreo.setText("Correo: " + texto(u.correo));

        etTelefono.setText(u.telefono == null ? "" : u.telefono);

        if (!TextUtils.isEmpty(u.fotoUri)) {
            try {
                ivFoto.setImageURI(Uri.parse(u.fotoUri));
            } catch (Exception e) {
                ivFoto.setImageResource(R.drawable.ic_perfil_placeholder);
            }
        } else {
            ivFoto.setImageResource(R.drawable.ic_perfil_placeholder);
        }

        mostrarFechaActualizacion(u.fechaActualizacion);
    }

    private void mostrarFechaActualizacion(long fecha) {
        if (fecha <= 0) {
            tvUltimaActualizacion.setText("Sin cambios registrados");
        } else {
            tvUltimaActualizacion.setText("Última actualización: " + formatear(fecha));
        }
    }

    private String formatear(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("es", "CO"));
        return sdf.format(new Date(millis));
    }

    private String texto(String s) {
        return TextUtils.isEmpty(s) ? "—" : s;
    }

    // ------------------------------------------------------------------
    // Criterio 2 y 3: editar teléfono y foto, con validación y mensaje de éxito
    // ------------------------------------------------------------------
    private void guardarDatosDeContacto() {
        if (usuarioActual == null) return;

        final String telefono = etTelefono.getText().toString().trim();

        if (!telefono.isEmpty() && !TELEFONO_VALIDO.matcher(telefono).matches()) {
            etTelefono.setError("Ingresa un teléfono válido (solo números, 7 a 15 dígitos)");
            etTelefono.requestFocus();
            return;
        }

        final String fotoFinal = fotoUriSeleccionada != null
                ? fotoUriSeleccionada
                : usuarioActual.fotoUri;

        boolean cambioTelefono = !igual(telefono, usuarioActual.telefono);
        boolean cambioFoto = fotoUriSeleccionada != null && !igual(fotoFinal, usuarioActual.fotoUri);

        if (!cambioTelefono && !cambioFoto) {
            Toast.makeText(this, "No hay cambios por guardar", Toast.LENGTH_SHORT).show();
            return;
        }

        final long ahora = System.currentTimeMillis();
        final int id = usuarioActual.id;

        executor.execute(() -> {
            db.usuarioDao().actualizarPerfil(id, telefono.isEmpty() ? null : telefono, fotoFinal, ahora);

            // Criterio 4: cada cambio queda registrado con su fecha.
            if (cambioTelefono) db.usuarioDao().registrarCambio(new CambioPerfil(id, "telefono", ahora));
            if (cambioFoto) db.usuarioDao().registrarCambio(new CambioPerfil(id, "foto", ahora));

            Usuario actualizado = db.usuarioDao().buscarPorId(id);
            runOnUiThread(() -> {
                usuarioActual = actualizado;
                fotoUriSeleccionada = null;
                pintarPerfil(actualizado);
                Toast.makeText(this, "Perfil actualizado correctamente", Toast.LENGTH_LONG).show();
            });
        });
    }

    // ------------------------------------------------------------------
    // Criterio 2 y 3: cambio de contraseña
    // ------------------------------------------------------------------
    private void cambiarPassword() {
        if (usuarioActual == null) return;

        String actual = etPasswordActual.getText().toString();
        String nueva = etPasswordNueva.getText().toString();
        String confirmar = etPasswordConfirmar.getText().toString();

        if (actual.isEmpty() || nueva.isEmpty() || confirmar.isEmpty()) {
            Toast.makeText(this, "Completa los tres campos de contraseña", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!nueva.equals(confirmar)) {
            etPasswordConfirmar.setError("Las contraseñas no coinciden");
            etPasswordConfirmar.requestFocus();
            return;
        }
        if (!POLITICA_PASSWORD.matcher(nueva).matches()) {
            etPasswordNueva.setError("Mínimo 8 caracteres, una mayúscula y un número");
            etPasswordNueva.requestFocus();
            return;
        }
        if (nueva.equals(actual)) {
            etPasswordNueva.setError("La nueva contraseña debe ser distinta a la actual");
            etPasswordNueva.requestFocus();
            return;
        }

        final int id = usuarioActual.id;
        final String hashGuardado = usuarioActual.passwordHash;
        final long ahora = System.currentTimeMillis();

        executor.execute(() -> {
            BCrypt.Result verificacion =
                    BCrypt.verifyer().verify(actual.toCharArray(), hashGuardado);

            if (!verificacion.verified) {
                runOnUiThread(() -> {
                    etPasswordActual.setError("La contraseña actual no es correcta");
                    etPasswordActual.requestFocus();
                });
                return;
            }

            // Mismo factor 12 que en el registro (HU-01)
            String nuevoHash = BCrypt.withDefaults().hashToString(12, nueva.toCharArray());
            db.usuarioDao().actualizarPassword(id, nuevoHash, ahora);
            db.usuarioDao().registrarCambio(new CambioPerfil(id, "contrasena", ahora));

            Usuario actualizado = db.usuarioDao().buscarPorId(id);
            runOnUiThread(() -> {
                usuarioActual = actualizado;
                etPasswordActual.setText("");
                etPasswordNueva.setText("");
                etPasswordConfirmar.setText("");
                mostrarFechaActualizacion(actualizado.fechaActualizacion);
                Toast.makeText(this, "Contraseña actualizada correctamente", Toast.LENGTH_LONG).show();
            });
        });
    }

    // ------------------------------------------------------------------
    // Criterio 4: historial de cambios con fecha
    // ------------------------------------------------------------------
    private void mostrarHistorial() {
        if (usuarioActual == null) return;
        final int id = usuarioActual.id;

        executor.execute(() -> {
            List<CambioPerfil> cambios = db.usuarioDao().historialDeCambios(id);
            StringBuilder sb = new StringBuilder();
            if (cambios.isEmpty()) {
                sb.append("Todavía no has hecho cambios en tu perfil.");
            } else {
                for (CambioPerfil c : cambios) {
                    sb.append("• ").append(nombreCampo(c.campo))
                      .append(" — ").append(formatear(c.fecha)).append("\n");
                }
            }
            runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle("Historial de cambios")
                    .setMessage(sb.toString())
                    .setPositiveButton("Cerrar", null)
                    .show());
        });
    }

    private String nombreCampo(String campo) {
        if (campo == null) return "Cambio";
        switch (campo) {
            case "telefono": return "Teléfono actualizado";
            case "foto": return "Foto de perfil actualizada";
            case "contrasena": return "Contraseña actualizada";
            default: return campo;
        }
    }

    private boolean igual(String a, String b) {
        String x = a == null ? "" : a;
        String y = b == null ? "" : b;
        return x.equals(y);
    }

    private void irALogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
