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
<<<<<<< Updated upstream

=======
    public abstract MatriculaDao matriculaDao();
    public abstract ContenidoCursoDao contenidoCursoDao();
>>>>>>> Stashed changes
    private static volatile AppDatabase INSTANCIA;
    public abstract RegistroAccesoMaterialDao registroAccesoMaterialDao();

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

<<<<<<< Updated upstream
=======
    static final Migration MIGRACION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `matriculas` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `estudianteId` INTEGER NOT NULL, `cursoId` INTEGER NOT NULL, `fechaMatricula` INTEGER NOT NULL)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_matriculas_estudianteId_cursoId` ON `matriculas` (`estudianteId`, `cursoId`)");
        }
    };

    static final Migration MIGRACION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Estructura para almacenar materiales/contenidos de asignaturas
            db.execSQL("CREATE TABLE IF NOT EXISTS `contenidos_curso` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `cursoId` INTEGER NOT NULL, `titulo` TEXT, `descripcionContenido` TEXT, `tipoMaterial` TEXT, `fechaPublicacion` INTEGER NOT NULL)");
        }
    };

    static final Migration MIGRACION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE contenidos_curso ADD COLUMN unidad TEXT");
            db.execSQL("ALTER TABLE contenidos_curso ADD COLUMN tamanoArchivo TEXT");
            db.execSQL("ALTER TABLE contenidos_curso ADD COLUMN urlArchivo TEXT");

            db.execSQL("CREATE TABLE IF NOT EXISTS `acceso_material` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `estudianteId` INTEGER NOT NULL, `materialId` INTEGER NOT NULL, `fechaUltimoAcceso` INTEGER NOT NULL)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_acceso_material_estudianteId_materialId` ON `acceso_material` (`estudianteId`, `materialId`)");
        }
    };

>>>>>>> Stashed changes
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCIA == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCIA == null) {
                    INSTANCIA = Room.databaseBuilder(context.getApplicationContext(),
<<<<<<< Updated upstream
                                    AppDatabase.class, "intercambios_db_v2") // <-- Nombre cambiado
=======
                                    AppDatabase.class, "intercambios_db_v2")
                            .addMigrations(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4, MIGRACION_4_5, MIGRACION_5_6)
>>>>>>> Stashed changes
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCIA;
    }
}