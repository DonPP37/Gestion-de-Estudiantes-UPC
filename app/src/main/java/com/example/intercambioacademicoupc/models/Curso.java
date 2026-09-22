package com.example.intercambioacademicoupc.models;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "cursos",
        indices = {@Index(value = {"codigo", "periodo"}, unique = true)} // Regla de negocio HU-06
)
public class Curso {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String nombre;
    public String codigo;
    public String descripcion;
    public String periodo;
    public int cupoMaximo;
    public String estado = "borrador"; // Inicia por defecto en borrador
    public int docenteId; // ID del creador (SessionManager lo debe proveer)

    // Genera constructores, getters y setters
}