package com.example.intercambioacademicoupc.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.intercambioacademicoupc.R;
import com.example.intercambioacademicoupc.models.AppDatabase;
import com.example.intercambioacademicoupc.models.NotificacionItem;
import java.util.concurrent.Executors;

public class NotificacionHelper {

    private static final String CHANNEL_ID = "canal_notificaciones_upc";

    public static void registrarYNotificar(Context context, int usuarioId, String titulo, String mensaje, String tipo) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);

            NotificacionItem item = new NotificacionItem();
            item.usuarioId = usuarioId;
            item.titulo = titulo;
            item.mensaje = mensaje;
            item.tipo = tipo;
            item.fecha = System.currentTimeMillis();
            item.leida = false;

            db.notificacionDao().insertarNotificacion(item);

            mostrarNotificacionSistema(context, titulo, mensaje);
        });
    }

    public static void mostrarNotificacion(Context context, String titulo, String mensaje, int notificationId) {
        mostrarNotificacionSistema(context, titulo, mensaje);
    }

    private static void mostrarNotificacionSistema(Context context, String titulo, String mensaje) {
        try {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Notificaciones UPC", NotificationManager.IMPORTANCE_DEFAULT);
                manager.createNotificationChannel(channel);
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(titulo)
                    .setContentText(mensaje)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            manager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (Exception ignored) {}
    }
}
