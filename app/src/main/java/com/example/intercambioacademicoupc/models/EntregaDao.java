package com.example.intercambioacademicoupc.models;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface EntregaDao {
    @Insert
    long insertarEntrega(Entrega entrega);

    @androidx.room.Update
    void actualizarEntrega(Entrega entrega);

    @Query("SELECT * FROM entregas WHERE id = :entregaId LIMIT 1")
    Entrega obtenerPorId(int entregaId);

    @Query("SELECT * FROM entregas WHERE actividadId = :actividadId AND estudianteId = :estudianteId LIMIT 1")
    Entrega obtenerEntregaPorActividadYEstudiante(int actividadId, int estudianteId);

    @Query("SELECT * FROM entregas WHERE cursoId = :cursoId AND estudianteId = :estudianteId")
    List<Entrega> obtenerEntregasPorEstudianteYCurso(int estudianteId, int cursoId);

    @Query("SELECT * FROM entregas WHERE cursoId = :cursoId")
    List<Entrega> obtenerEntregasPorCurso(int cursoId);

    @Query("SELECT * FROM entregas WHERE actividadId = :actividadId ORDER BY fechaEntrega DESC")
    List<Entrega> obtenerEntregasPorActividad(int actividadId);
}
