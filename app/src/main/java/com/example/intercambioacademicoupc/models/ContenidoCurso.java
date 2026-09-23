package com.example.intercambioacademicoupc.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "contenidos_curso")
public class ContenidoCurso {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int cursoId;
    public String unidad; // Unidad de la asignatura (Ej: "Unidad 1")
    public String titulo;
    public String descripcionContenido;
    public String tipoMaterial; // PDF, PPTX, etc.
    public String tamanoArchivo; // Ej: "2.5 MB"
    public String urlArchivo;
    public long fechaPublicacion;
}