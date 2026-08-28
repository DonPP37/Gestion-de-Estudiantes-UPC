package com.example.intercambioacademicoupc.models;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface UsuarioDao {
    @Insert
    void insertarUsuario(Usuario usuario);

    @Query("SELECT * FROM usuarios WHERE correo = :correo LIMIT 1")
    Usuario buscarPorCorreo(String correo);

}