package com.example.intercambioacademicoupc.models;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface EntregaDao {
    @Insert
    void insertarEntrega(Entrega entrega);

    @Query("SELECT * FROM entregas WHERE cursoId = :cursoId AND estudianteId = :estudianteId")
    List<Entrega> obtenerEntregasPorEstudianteYCurso(int estudianteId, int cursoId);

    @Query("SELECT * FROM entregas WHERE cursoId = :cursoId")
    List<Entrega> obtenerEntregasPorCurso(int cursoId);
}
