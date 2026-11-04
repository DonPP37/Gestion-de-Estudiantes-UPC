package com.example.intercambioacademicoupc.models;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface PostulacionDao {
    @Insert
    long insertarPostulacion(Postulacion postulacion);

    @Update
    void actualizarPostulacion(Postulacion postulacion);

    @Query("SELECT * FROM postulaciones WHERE convocatoriaId = :convocatoriaId AND estudianteId = :estudianteId LIMIT 1")
    Postulacion verificarPostulacionEstudiante(int convocatoriaId, int estudianteId);

    @Query("SELECT * FROM postulaciones WHERE estudianteId = :estudianteId ORDER BY fechaPostulacion DESC")
    List<Postulacion> obtenerPostulacionesPorEstudiante(int estudianteId);

    @Query("SELECT * FROM postulaciones WHERE estado = 'en revisión' ORDER BY fechaPostulacion DESC")
    List<Postulacion> obtenerPostulacionesEnRevision();

    @Query("SELECT * FROM postulaciones ORDER BY fechaPostulacion DESC")
    List<Postulacion> obtenerTodasPostulaciones();
}
