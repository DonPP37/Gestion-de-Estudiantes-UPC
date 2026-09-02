package com.example.intercambioacademicoupc.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * HU-04: "Cada cambio queda registrado con fecha de actualización".
 * Cada fila es una modificación puntual del perfil (teléfono, foto o contraseña).
 */
@Entity(tableName = "cambios_perfil")
public class CambioPerfil {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int usuarioId;

    /** Campo modificado: "telefono", "foto" o "contrasena". */
    public String campo;

    /** Fecha del cambio en epoch millis. */
    public long fecha;

    public CambioPerfil() { }

    public CambioPerfil(int usuarioId, String campo, long fecha) {
        this.usuarioId = usuarioId;
        this.campo = campo;
        this.fecha = fecha;
    }
}
