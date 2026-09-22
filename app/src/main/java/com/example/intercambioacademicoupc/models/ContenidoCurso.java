package com.example.intercambioacademicoupc.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "contenidos_curso")
public class ContenidoCurso {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int cursoId;
    public String titulo;
    public String descripcionContenido;
    public String tipoMaterial; // PDF, DOCX, PPTX, Imagenes, Texto
    public long fechaPublicacion;
}