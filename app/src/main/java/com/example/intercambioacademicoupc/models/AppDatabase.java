package com.example.intercambioacademicoupc.models;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Usuario.class, CambioPerfil.class, Curso.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UsuarioDao usuarioDao();
    public abstract CursoDao cursoDao();

    private static volatile AppDatabase INSTANCIA;

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

    static final Migration MIGRACION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `cursos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT, `codigo` TEXT, `descripcion` TEXT, `periodo` TEXT, `cupoMaximo` INTEGER NOT NULL, `estado` TEXT, `docenteId` INTEGER NOT NULL)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cursos_codigo_periodo` ON `cursos` (`codigo`, `periodo`)");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCIA == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCIA == null) {
                    INSTANCIA = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "intercambios_db_v2") // <-- Nombre cambiado
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCIA;
    }
}