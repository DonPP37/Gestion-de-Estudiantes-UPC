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

    // ---------------- HU-05 ----------------
    @Query("SELECT * FROM usuarios WHERE activo = 1")
    List<Usuario> obtenerUsuariosActivos();

    @Query("UPDATE usuarios SET activo = 0 WHERE id = :usuarioId")
    void desactivarUsuario(int usuarioId);

    @Query("UPDATE usuarios SET rol = :nuevoRol WHERE id = :usuarioId")
    void actualizarRol(int usuarioId, String nuevoRol);

    @Query("SELECT * FROM usuarios WHERE activo = 1 AND (nombre LIKE '%' || :busqueda || '%' "
            + "OR apellido LIKE '%' || :busqueda || '%' "
            + "OR codigoEstudiantil LIKE '%' || :busqueda || '%' "
            + "OR programa LIKE '%' || :busqueda || '%')")
    List<Usuario> buscarUsuarios(String busqueda);

    @Query("SELECT * FROM usuarios WHERE activo = 1 AND rol = 'estudiante'")
    List<Usuario> obtenerEstudiantesActivos();

}
