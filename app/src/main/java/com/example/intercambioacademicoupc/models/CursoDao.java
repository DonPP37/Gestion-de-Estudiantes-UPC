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
}