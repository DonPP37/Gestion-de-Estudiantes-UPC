package com.example.intercambioacademicoupc;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.intercambioacademicoupc.models.AppDatabase;
import com.example.intercambioacademicoupc.models.Usuario;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class LoginActivity extends AppCompatActivity {

    private EditText etLoginCorreo, etLoginPassword;
    private Button btnLogin;
    private TextView tvIrRegistro;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = AppDatabase.getDatabase(getApplicationContext());

        etLoginCorreo = findViewById(R.id.etLoginCorreo);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvIrRegistro = findViewById(R.id.tvIrRegistro);

        btnLogin.setOnClickListener(v -> iniciarSesion());

        // Si el usuario toca "Regístrate aquí", lo devolvemos al MainActivity
        tvIrRegistro.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void iniciarSesion() {
        String correo = etLoginCorreo.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();

        if (correo.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Ingresa correo y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            Usuario usuario = db.usuarioDao().buscarPorCorreo(correo);

            if (usuario == null) {
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Credenciales inválidas", Toast.LENGTH_SHORT).show());
                return;
            }

            // Verificar la contraseña contra el hash de la base de datos
            BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), usuario.passwordHash);

            runOnUiThread(() -> {
                if (result.verified) {
                    Toast.makeText(LoginActivity.this, "¡Bienvenido, " + usuario.nombre + "!", Toast.LENGTH_SHORT).show();
                    // Aquí navegaremos al Dashboard del Perfil (HU-04) más adelante
                } else {
                    Toast.makeText(LoginActivity.this, "Credenciales inválidas", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}