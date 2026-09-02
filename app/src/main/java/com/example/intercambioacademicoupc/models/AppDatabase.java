package com.example.intercambioacademicoupc.models;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Usuario.class, CambioPerfil.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UsuarioDao usuarioDao();

    private static volatile AppDatabase INSTANCIA;

    /**
     * HU-04: se agregan telefono, fotoUri y fechaActualizacion al usuario,
     * y la tabla de auditoría cambios_perfil.
     */
    static final Migration MIGRACION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE usuarios ADD COLUMN telefono TEXT");
            db.execSQL("ALTER TABLE usuarios ADD COLUMN fotoUri TEXT");
            db.execSQL("ALTER TABLE usuarios ADD COLUMN fechaActualizacion INTEGER NOT NULL DEFAULT 0");
            db.execSQL("CREATE TABLE IF NOT EXISTS `cambios_perfil` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`usuarioId` INTEGER NOT NULL, "
                    + "`campo` TEXT, "
                    + "`fecha` INTEGER NOT NULL)");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCIA == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCIA == null) {
                    INSTANCIA = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "intercambios_database")
                            .addMigrations(MIGRACION_1_2)
                            // Si prefieres no mantener migraciones durante el desarrollo,
                            // borra la línea de arriba y usa: .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCIA;
    }
}
