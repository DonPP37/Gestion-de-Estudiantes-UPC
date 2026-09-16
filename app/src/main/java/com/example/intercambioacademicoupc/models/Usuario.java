package com.example.intercambioacademicoupc.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "usuarios")
public class Usuario {
    @PrimaryKey(autoGenerate = true)
    public int id;

    // --- Datos académicos: SOLO los edita el administrador (HU-04 / HU-05) ---
    public String nombre;
    public String apellido;
    public String documento;
    public String codigoEstudiantil;
    public String programa;
    public String correo;

    // --- Seguridad ---
    public String passwordHash;
    public String rol;
    public String codigoVerificacion2FA;
    public long tiempoExpiracion2FA;

    // --- HU-04: datos que el propio usuario puede editar ---
    public String telefono;
    public String fotoUri;

    // --- HU-05: Creación de roles ---
    public boolean activo = true;

    /** Fecha (epoch millis) del último cambio hecho al perfil. 0 = nunca se ha editado. */
    @ColumnInfo(defaultValue = "0")
    public long fechaActualizacion;
}