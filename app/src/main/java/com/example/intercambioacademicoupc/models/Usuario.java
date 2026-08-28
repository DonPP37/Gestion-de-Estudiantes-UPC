package com.example.intercambioacademicoupc.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "usuarios")
public class Usuario {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String nombre;
    public String apellido;
    public String documento;
    public String codigoEstudiantil;
    public String programa;
    public String correo;
    public String passwordHash;
    public String rol;
    public String codigoVerificacion2FA;
    public long tiempoExpiracion2FA;
}