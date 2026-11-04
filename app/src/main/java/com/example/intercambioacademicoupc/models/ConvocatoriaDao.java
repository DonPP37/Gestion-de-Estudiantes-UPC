package com.example.intercambioacademicoupc.models;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ConvocatoriaDao {
    @Insert
    long insertarConvocatoria(Convocatoria convocatoria);

    @Update
    void actualizarConvocatoria(Convocatoria convocatoria);

    @Query("SELECT * FROM convocatorias WHERE id = :id LIMIT 1")
    Convocatoria obtenerPorId(int id);

    @Query("SELECT * FROM convocatorias WHERE estado = 'activa' ORDER BY fechaCierre ASC")
    List<Convocatoria> obtenerConvocatoriasActivas();

    @Query("SELECT * FROM convocatorias WHERE estado = 'cerrada' ORDER BY fechaCierre DESC")
    List<Convocatoria> obtenerHistorialCerradas();

    @Query("SELECT * FROM convocatorias WHERE estado = 'activa' AND (pais LIKE '%' || :pais || '%') AND (idioma LIKE '%' || :idioma || '%') AND (periodo LIKE '%' || :periodo || '%') ORDER BY fechaCierre ASC")
    List<Convocatoria> buscarConvocatoriasFiltro(String pais, String idioma, String periodo);

    @Query("SELECT * FROM convocatorias ORDER BY fechaCierre DESC")
    List<Convocatoria> obtenerTodasConvocatorias();
}
