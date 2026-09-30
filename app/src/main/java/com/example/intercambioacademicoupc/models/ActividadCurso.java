package com.example.intercambioacademicoupc.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "actividades_curso")
public class ActividadCurso {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int cursoId;
    public String titulo;
    public String instrucciones;
    public long fechaLimite; // timestamp
    public long fechaApertura; // Fecha desde cuando se abre
    public long fechaCierre; // Fecha de cierre / deadline
    public int porcentaje; // ej: 20 (%)
    public boolean aceptaFueraDePlazo;
    public String archivoUrl; // Archivo adjunto o recurso
    public boolean visible = true; // Si el profesor la deja ver u oculta
}
