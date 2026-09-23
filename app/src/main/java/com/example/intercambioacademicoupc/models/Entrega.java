package com.example.intercambioacademicoupc.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "entregas")
public class Entrega {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int cursoId;
    public int estudianteId;
    public String tituloTarea;
    public String descripcionEntrega;
    public String archivoUrl;
    public long fechaEntrega;
    public String estado; // "Entregado", "Calificado"
}
