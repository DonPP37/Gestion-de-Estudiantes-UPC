package com.example.intercambioacademicoupc.models;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface NotificacionDao {
    @Insert
    void insertarNotificacion(NotificacionItem notificacion);

    @Query("SELECT * FROM notificaciones WHERE usuarioId = :usuarioId ORDER BY fecha DESC")
    List<NotificacionItem> obtenerNotificacionesPorUsuario(int usuarioId);

    @Query("SELECT COUNT(*) FROM notificaciones WHERE usuarioId = :usuarioId AND leida = 0")
    int obtenerContadorNoLeidas(int usuarioId);

    @Query("UPDATE notificaciones SET leida = 1 WHERE usuarioId = :usuarioId")
    void marcarTodasComoLeidas(int usuarioId);
}
