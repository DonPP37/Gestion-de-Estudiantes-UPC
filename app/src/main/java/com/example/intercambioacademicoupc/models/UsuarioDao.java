package com.example.intercambioacademicoupc.models;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UsuarioDao {
    @Insert
    void insertarUsuario(Usuario usuario);

    @Query("SELECT * FROM usuarios WHERE correo = :correo LIMIT 1")
    Usuario buscarPorCorreo(String correo);

    // ---------------- HU-04 ----------------

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    Usuario buscarPorId(int id);

    /** Actualiza únicamente los datos de contacto/foto que el usuario sí puede editar. */
    @Query("UPDATE usuarios SET telefono = :telefono, fotoUri = :fotoUri, fechaActualizacion = :fecha WHERE id = :id")
    int actualizarPerfil(int id, String telefono, String fotoUri, long fecha);

    @Query("UPDATE usuarios SET passwordHash = :passwordHash, fechaActualizacion = :fecha WHERE id = :id")
    int actualizarPassword(int id, String passwordHash, long fecha);

    @Insert
    void registrarCambio(CambioPerfil cambio);

    @Query("SELECT * FROM cambios_perfil WHERE usuarioId = :usuarioId ORDER BY fecha DESC LIMIT 20")
    List<CambioPerfil> historialDeCambios(int usuarioId);
}
