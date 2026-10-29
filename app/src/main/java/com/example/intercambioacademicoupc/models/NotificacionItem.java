package com.example.intercambioacademicoupc.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notificaciones")
public class NotificacionItem {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int usuarioId;
    public String titulo;
    public String mensaje;
    public String tipo; // "material", "actividad", "vencer", "nota"
    public long fecha;
    public boolean leida = false;
}
