package com.example.intercambioacademicoupc.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.intercambioacademicoupc.AdminUsuariosActivity;
import com.example.intercambioacademicoupc.CrearCursoActivity;
import com.example.intercambioacademicoupc.LoginActivity;
import com.example.intercambioacademicoupc.R;
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

public class PerfilFragment extends Fragment {

    private static final Pattern POLITICA_PASSWORD =
            Pattern.compile("^(?=.*[A-Z])(?=.*\\d).{8,}$");
    private static final Pattern TELEFONO_VALIDO = Pattern.compile("^\\d{7,15}$");

    private ImageView ivFoto;
    private TextView tvNombre, tvDocumento, tvCodigo, tvPrograma, tvCorreo, tvUltimaActualizacion;
    private EditText etTelefono, etPasswordActual, etPasswordNueva, etPasswordConfirmar;
    private Button btnCambiarFoto, btnGuardarContacto, btnCambiarPassword, btnHistorial, btnCerrarSesion;
    private Button btnAdminUsuarios, btnCrearCurso;

    private AppDatabase db;
    private SessionManager sesion;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private Usuario usuarioActual;
    private String fotoUriSeleccionada;

    private final ActivityResultLauncher<String[]> selectorFoto =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try {
                    requireActivity().getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                }
                fotoUriSeleccionada = uri.toString();
                ivFoto.setImageURI(uri);
                Toast.makeText(requireContext(), "Foto seleccionada. Pulsa \"Guardar cambios\".",
                        Toast.LENGTH_SHORT).show();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        db = AppDatabase.getDatabase(requireContext().getApplicationContext());
        sesion = new SessionManager(requireContext());

        vincularVistas(view);

        btnCambiarFoto.setOnClickListener(v -> selectorFoto.launch(new String[]{"image/*"}));
        btnGuardarContacto.setOnClickListener(v -> guardarDatosDeContacto());
        btnCambiarPassword.setOnClickListener(v -> cambiarPassword());
        btnHistorial.setOnClickListener(v -> mostrarHistorial());
        btnAdminUsuarios.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AdminUsuariosActivity.class)));
        btnCrearCurso.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CrearCursoActivity.class)));
        btnCerrarSesion.setOnClickListener(v -> {
            sesion.cerrarSesion();
            irALogin();
        });

        cargarPerfil();

        return view;
    }

    private void vincularVistas(View view) {
        ivFoto = view.findViewById(R.id.ivFotoPerfil);
        tvNombre = view.findViewById(R.id.tvNombre);
        tvDocumento = view.findViewById(R.id.tvDocumento);
        tvCodigo = view.findViewById(R.id.tvCodigo);
        tvPrograma = view.findViewById(R.id.tvPrograma);
        tvCorreo = view.findViewById(R.id.tvCorreo);
        tvUltimaActualizacion = view.findViewById(R.id.tvUltimaActualizacion);

        etTelefono = view.findViewById(R.id.etTelefono);
        etPasswordActual = view.findViewById(R.id.etPasswordActual);
        etPasswordNueva = view.findViewById(R.id.etPasswordNueva);
        etPasswordConfirmar = view.findViewById(R.id.etPasswordConfirmar);

        btnCambiarFoto = view.findViewById(R.id.btnCambiarFoto);
        btnGuardarContacto = view.findViewById(R.id.btnGuardarContacto);
        btnCambiarPassword = view.findViewById(R.id.btnCambiarPassword);
        btnHistorial = view.findViewById(R.id.btnHistorial);
        btnCerrarSesion = view.findViewById(R.id.btnCerrarSesion);
        btnAdminUsuarios = view.findViewById(R.id.btnAdminUsuarios);
        btnCrearCurso = view.findViewById(R.id.btnCrearCurso);
    }

    private void cargarPerfil() {
        final int id = sesion.getUsuarioId();
        executor.execute(() -> {
            Usuario u = db.usuarioDao().buscarPorId(id);
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (u == null) {
                        Toast.makeText(requireContext(), "No se encontró el usuario", Toast.LENGTH_SHORT).show();
                        sesion.cerrarSesion();
                        irALogin();
                        return;
                    }
                    usuarioActual = u;
                    pintarPerfil(u);
                });
            }
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

        btnAdminUsuarios.setVisibility("administrador".equals(u.rol) ? View.VISIBLE : View.GONE);
        btnCrearCurso.setVisibility("docente".equals(u.rol) ? View.VISIBLE : View.GONE);
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
            Toast.makeText(requireContext(), "No hay cambios por guardar", Toast.LENGTH_SHORT).show();
            return;
        }

        final long ahora = System.currentTimeMillis();
        final int id = usuarioActual.id;

        executor.execute(() -> {
            db.usuarioDao().actualizarPerfil(id, telefono.isEmpty() ? null : telefono, fotoFinal, ahora);

            if (cambioTelefono) db.usuarioDao().registrarCambio(new CambioPerfil(id, "telefono", ahora));
            if (cambioFoto) db.usuarioDao().registrarCambio(new CambioPerfil(id, "foto", ahora));

            Usuario actualizado = db.usuarioDao().buscarPorId(id);
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    usuarioActual = actualizado;
                    fotoUriSeleccionada = null;
                    pintarPerfil(actualizado);
                    Toast.makeText(requireContext(), "Perfil actualizado correctamente", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void cambiarPassword() {
        if (usuarioActual == null) return;

        String actual = etPasswordActual.getText().toString();
        String nueva = etPasswordNueva.getText().toString();
        String confirmar = etPasswordConfirmar.getText().toString();

        if (actual.isEmpty() || nueva.isEmpty() || confirmar.isEmpty()) {
            Toast.makeText(requireContext(), "Completa los tres campos de contraseña", Toast.LENGTH_SHORT).show();
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
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        etPasswordActual.setError("La contraseña actual no es correcta");
                        etPasswordActual.requestFocus();
                    });
                }
                return;
            }

            String nuevoHash = BCrypt.withDefaults().hashToString(12, nueva.toCharArray());
            db.usuarioDao().actualizarPassword(id, nuevoHash, ahora);
            db.usuarioDao().registrarCambio(new CambioPerfil(id, "contrasena", ahora));

            Usuario actualizado = db.usuarioDao().buscarPorId(id);
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    usuarioActual = actualizado;
                    etPasswordActual.setText("");
                    etPasswordNueva.setText("");
                    etPasswordConfirmar.setText("");
                    mostrarFechaActualizacion(actualizado.fechaActualizacion);
                    Toast.makeText(requireContext(), "Contraseña actualizada correctamente", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

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
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> new AlertDialog.Builder(requireContext())
                        .setTitle("Historial de cambios")
                        .setMessage(sb.toString())
                        .setPositiveButton("Cerrar", null)
                        .show());
            }
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
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
