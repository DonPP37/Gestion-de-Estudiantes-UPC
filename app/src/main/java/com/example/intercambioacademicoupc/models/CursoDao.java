package com.example.intercambioacademicoupc.models;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface CursoDao {
    // Devuelve el ID insertado. Fallará (Exception) si se viola el índice único.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(Curso curso);

    @Query("SELECT * FROM cursos WHERE docenteId = :docenteId")
    List<Curso> obtenerCursosPorDocente(int docenteId);

    @Query("SELECT * FROM cursos WHERE estado = 'publicado'")
    List<Curso> obtenerCursosPublicados();

    @Query("SELECT * FROM cursos")
    List<Curso> obtenerTodosLosCursos();

    @Query("UPDATE cursos SET nombre = :nombre, descripcion = :descripcion, cupoMaximo = :cupo WHERE id = :cursoId")
    void actualizarCurso(int cursoId, String nombre, String descripcion, int cupo);
}