package com.example.intercambioacademicoupc.models;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ActividadCursoDao {
    @Insert
    void insertarActividad(ActividadCurso actividad);

    @Update
    void actualizarActividad(ActividadCurso actividad);

    @Query("DELETE FROM actividades_curso WHERE id = :actividadId")
    void eliminarActividad(int actividadId);

    @Query("SELECT * FROM actividades_curso WHERE cursoId = :cursoId ORDER BY fechaCierre ASC")
    List<ActividadCurso> obtenerActividadesPorCurso(int cursoId);

    @Query("SELECT SUM(porcentaje) FROM actividades_curso WHERE cursoId = :cursoId")
    Integer obtenerSumaPorcentajes(int cursoId);

    @Query("SELECT a.* FROM actividades_curso a INNER JOIN matriculas m ON a.cursoId = m.cursoId WHERE m.estudianteId = :estudianteId AND a.visible = 1 AND a.fechaApertura <= :ahora AND a.fechaCierre >= :ahora ORDER BY a.fechaCierre ASC")
    List<ActividadCurso> obtenerActividadesParaEstudiante(int estudianteId, long ahora);
}
