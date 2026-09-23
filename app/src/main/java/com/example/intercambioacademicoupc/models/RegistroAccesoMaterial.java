package com.example.intercambioacademicoupc.models;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "acceso_material",
        indices = {@Index(value = {"estudianteId", "materialId"}, unique = true)})
public class RegistroAccesoMaterial {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int estudianteId;
    public int materialId;
    public long fechaUltimoAcceso;
}