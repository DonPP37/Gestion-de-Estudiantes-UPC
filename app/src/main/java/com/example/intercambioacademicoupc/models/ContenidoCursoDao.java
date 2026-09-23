package com.example.intercambioacademicoupc.models;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ContenidoCursoDao {
    @Insert
    void insertarContenido(ContenidoCurso contenido);

    @Query("SELECT * FROM contenidos_curso WHERE cursoId = :cursoId ORDER BY unidad ASC, fechaPublicacion DESC")
    List<ContenidoCurso> obtenerContenidosPorCurso(int cursoId);
}