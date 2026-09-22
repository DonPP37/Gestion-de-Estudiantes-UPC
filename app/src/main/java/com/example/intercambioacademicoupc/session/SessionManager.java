package com.example.intercambioacademicoupc.session;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Guarda quién es el usuario autenticado.
 * HU-04 exige "usuario autenticado": sin sesión no se puede abrir el perfil.
 *
 * Nota: cuando implementen el token JWT de HU-02, ese token se guarda aquí mismo
 * (método guardarToken) y el perfil lo usará en vez del id local.
 */
public class SessionManager {

    private static final String PREFS = "sesion_upc";
    private static final String KEY_USUARIO_ID = "usuario_id";
    private static final String KEY_TOKEN = "token_jwt";
    private static final String KEY_ROL = "rol";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void iniciarSesion(int usuarioId) {
        iniciarSesion(usuarioId, "");
    }

    /**
     * HU-05: el rol se guarda al abrir la sesion. Por eso un cambio de rol hecho por el
     * administrador solo surte efecto en el siguiente inicio de sesion del usuario.
     */
    public void iniciarSesion(int usuarioId, String rol) {
        prefs.edit()
                .putInt(KEY_USUARIO_ID, usuarioId)
                .putString(KEY_ROL, rol == null ? "" : rol)
                .apply();
    }

    public void guardarToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    /** @return id del usuario autenticado, o -1 si no hay sesión. */
    public int getUsuarioId() {
        return prefs.getInt(KEY_USUARIO_ID, -1);
    }

    public boolean haySesionActiva() {
        return getUsuarioId() != -1;
    }

    public void cerrarSesion() {
        prefs.edit().clear().apply();
    }

    public String getRol(){
        return prefs.getString(KEY_ROL, "");
    }
}