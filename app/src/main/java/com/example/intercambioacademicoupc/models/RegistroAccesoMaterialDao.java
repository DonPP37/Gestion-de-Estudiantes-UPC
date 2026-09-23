package com.example.intercambioacademicoupc.models;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface RegistroAccesoMaterialDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void registrarAcceso(RegistroAccesoMaterial acceso);

    @Query("SELECT fechaUltimoAcceso FROM acceso_material WHERE estudianteId = :estudianteId AND materialId = :materialId LIMIT 1")
    Long obtenerUltimoAcceso(int estudianteId, int materialId);
}