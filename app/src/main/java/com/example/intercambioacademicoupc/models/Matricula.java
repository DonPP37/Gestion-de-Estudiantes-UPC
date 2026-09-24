package com.example.intercambioacademicoupc.models;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "matriculas",
        indices = {@Index(value = {"estudianteId", "cursoId"}, unique = true)} // Impide matrículas duplicadas (HU-07)
)
public class Matricula {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int estudianteId;
    public int cursoId;
    public long fechaMatricula;
}