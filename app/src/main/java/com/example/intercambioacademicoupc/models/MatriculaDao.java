package com.example.intercambioacademicoupc.models;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface MatriculaDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long matricular(Matricula matricula);

    @Query("SELECT COUNT(*) FROM matriculas WHERE cursoId = :cursoId")
    int obtenerCupoActual(int cursoId);

    @Query("SELECT COUNT(*) FROM matriculas WHERE estudianteId = :estudianteId AND cursoId = :cursoId")
    int verificarSiYaEstaInscrito(int estudianteId, int cursoId);

    @Query("SELECT c.* FROM cursos c INNER JOIN matriculas m ON c.id = m.cursoId WHERE m.estudianteId = :estudianteId")
    List<Curso> obtenerCursosMatriculados(int estudianteId);
}