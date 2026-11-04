package com.example.intercambioacademicoupc.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "convocatorias")
public class Convocatoria {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String institucionDestino;
    public String pais;
    public String idioma;
    public String periodo;
    public String requisitos;
    public String documentosExigidos;
    public String enlaceTerminos;
    public long fechaCierre;
    public String estado = "activa"; // "activa", "cerrada"
}
