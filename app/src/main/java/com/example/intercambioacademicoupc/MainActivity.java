package com.example.intercambioacademicoupc; // Ajusta esto según tu paquete

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.intercambioacademicoupc.models.AppDatabase;
import com.example.intercambioacademicoupc.models.Usuario;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class MainActivity extends AppCompatActivity {

    private EditText etNombre, etApellido, etDocumento, etCodigo, etPrograma, etCorreo, etPassword;
    private Button btnRegistrar;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Conecta con la vista que creaste

        // Inicializar la base de datos
        db = AppDatabase.getDatabase(getApplicationContext());

        // Vincular las vistas
        etNombre = findViewById(R.id.etNombre);
        etApellido = findViewById(R.id.etApellido);
        etDocumento = findViewById(R.id.etDocumento);
        etCodigo = findViewById(R.id.etCodigo);
        etPrograma = findViewById(R.id.etPrograma);
        etCorreo = findViewById(R.id.etCorreo);
        etPassword = findViewById(R.id.etPassword);
        btnRegistrar = findViewById(R.id.btnRegistrar);

        btnRegistrar.setOnClickListener(v -> registrarUsuario());
    }

    private void registrarUsuario() {
        String nombre = etNombre.getText().toString().trim();
        String apellido = etApellido.getText().toString().trim();
        String documento = etDocumento.getText().toString().trim();
        String codigo = etCodigo.getText().toString().trim();
        String programa = etPrograma.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validación de los datos del formulario
        if (!validarFormulario(nombre, apellido, documento, codigo, programa, correo, password)) {
            return;
        }

        // Ejecutar en segundo plano para no bloquear la pantalla
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            // Verificar si el correo ya existe
            Usuario usuarioExistente = db.usuarioDao().buscarPorCorreo(correo);

            if (usuarioExistente != null) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "El correo ya está registrado", Toast.LENGTH_LONG).show());
                return;
            }

            // Cifrar la contraseña con Bcrypt
            String passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray());

            // Crear el objeto y guardar
            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.nombre = nombre;
            nuevoUsuario.apellido = apellido;
            nuevoUsuario.documento = documento;
            nuevoUsuario.codigoEstudiantil = codigo;
            nuevoUsuario.programa = programa;
            nuevoUsuario.correo = correo;
            nuevoUsuario.passwordHash = passwordHash;
            nuevoUsuario.rol = "estudiante"; // Rol por defecto

            db.usuarioDao().insertarUsuario(nuevoUsuario);

            // Volver al hilo principal para mostrar mensaje y cambiar de pantalla
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                // Navegar a la pantalla de Login
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Cierra esta pantalla
            });
        });
    }

    /** Solo letras (incluidas tildes y ñ) y espacios entre palabras. */
    private static final String SOLO_LETRAS = "\\p{L}+( \\p{L}+)*";

    /** Solo dígitos. */
    private static final String SOLO_NUMEROS = "\\d+";

    private boolean validarFormulario(String nombre, String apellido, String documento,
                                      String codigo, String programa, String correo,
                                      String password) {

        if (nombre.isEmpty() || apellido.isEmpty() || documento.isEmpty() || codigo.isEmpty()
                || programa.isEmpty() || correo.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!nombre.matches(SOLO_LETRAS)) {
            mostrarError(etNombre, "El nombre solo puede contener letras");
            return false;
        }

        if (!apellido.matches(SOLO_LETRAS)) {
            mostrarError(etApellido, "El apellido solo puede contener letras");
            return false;
        }

        if (!documento.matches(SOLO_NUMEROS)) {
            mostrarError(etDocumento, "El documento solo puede contener números");
            return false;
        }

        if (!codigo.matches(SOLO_NUMEROS)) {
            mostrarError(etCodigo, "El código estudiantil solo puede contener números");
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            mostrarError(etCorreo, "Escribe un correo válido, por ejemplo nombre@dominio.com");
            return false;
        }

        return true;
    }

    private void mostrarError(EditText campo, String mensaje) {
        campo.setError(mensaje);
        campo.requestFocus();
    }
}
