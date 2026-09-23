package com.example.intercambioacademicoupc.models;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.Executors;

import at.favre.lib.crypto.bcrypt.BCrypt;

@Database(entities = {
    Usuario.class,
    CambioPerfil.class,
    Curso.class,
    ContenidoCurso.class,
    RegistroAccesoMaterial.class,
    Matricula.class,
    Entrega.class
}, version = 7) // Debe coincidir con la última migración (6 -> 7)

public abstract class AppDatabase extends RoomDatabase {
    public abstract UsuarioDao usuarioDao();
    public abstract CursoDao cursoDao();

    public abstract MatriculaDao matriculaDao();
    public abstract ContenidoCursoDao contenidoCursoDao();
    public abstract RegistroAccesoMaterialDao registroAccesoMaterialDao();
    public abstract EntregaDao entregaDao();

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
            // HU-05: columna 'activo' que no se estaba creando en ninguna migración
            if (!existeColumna(db, "usuarios", "activo")) {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN activo INTEGER NOT NULL DEFAULT 1");
            }
        }
    };

    private static boolean existeColumna(SupportSQLiteDatabase db, String tabla, String columna) {
        try (Cursor c = db.query("PRAGMA table_info(" + tabla + ")")) {
            int idx = c.getColumnIndex("name");
            while (c.moveToNext()) {
                if (columna.equals(c.getString(idx))) return true;
            }
        }
        return false;
    }

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

    static final Migration MIGRACION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `entregas` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `cursoId` INTEGER NOT NULL, `estudianteId` INTEGER NOT NULL, `tituloTarea` TEXT, `descripcionEntrega` TEXT, `archivoUrl` TEXT, `fechaEntrega` INTEGER NOT NULL, `estado` TEXT)");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCIA == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCIA == null) {
                    INSTANCIA = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "intercambios_db_v2") // <-- Nombre cambiado
                            .addMigrations(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4, MIGRACION_4_5, MIGRACION_5_6, MIGRACION_6_7)
                            .fallbackToDestructiveMigration()
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    // Carga admin, docente, estudiante, curso y materiales de prueba
                                    Executors.newSingleThreadExecutor().execute(() -> precargarDatosDePrueba(INSTANCIA));
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCIA;
    }
    private static synchronized void precargarDatosDePrueba(AppDatabase db) {
        try {
            Usuario adminExistente = db.usuarioDao().buscarPorCorreo("admin@upc.edu");
            if (adminExistente == null) {
                // 1. Crear Admin
                Usuario admin = new Usuario();
                admin.nombre = "Administrador";
                admin.apellido = "General";
                admin.documento = "10000001";
                admin.codigoEstudiantil = "00000001";
                admin.programa = "Sistemas";
                admin.correo = "admin@upc.edu";
                admin.passwordHash = BCrypt.withDefaults().hashToString(12, "Admin1234".toCharArray());
                admin.rol = "administrador";
                admin.activo = true;
                db.usuarioDao().insertarUsuario(admin);

                // 2. Crear Docente
                Usuario docente = new Usuario();
                docente.nombre = "Carlos";
                docente.apellido = "Docente";
                docente.documento = "20000002";
                docente.codigoEstudiantil = "00000002";
                docente.programa = "Ingeniería";
                docente.correo = "docente@upc.edu";
                docente.passwordHash = BCrypt.withDefaults().hashToString(12, "Docente1234".toCharArray());
                docente.rol = "docente";
                docente.activo = true;
                db.usuarioDao().insertarUsuario(docente);

                // 3. Crear Estudiante
                Usuario estudiante = new Usuario();
                estudiante.nombre = "Juan";
                estudiante.apellido = "Estudiante";
                estudiante.documento = "30000003";
                estudiante.codigoEstudiantil = "00000003";
                estudiante.programa = "Sistemas";
                estudiante.correo = "estudiante@upc.edu";
                estudiante.passwordHash = BCrypt.withDefaults().hashToString(12, "Estudiante1234".toCharArray());
                estudiante.rol = "estudiante";
                estudiante.activo = true;
                db.usuarioDao().insertarUsuario(estudiante);

                Usuario docenteDb = db.usuarioDao().buscarPorCorreo("docente@upc.edu");
                Usuario estudianteDb = db.usuarioDao().buscarPorCorreo("estudiante@upc.edu");

                if (docenteDb != null && estudianteDb != null) {
                    Curso curso = new Curso();
                    curso.nombre = "Desarrollo de Aplicaciones Móviles";
                    curso.codigo = "INF321";
                    curso.descripcion = "Curso avanzado de Android con Room y Clean Architecture.";
                    curso.periodo = "2026-2";
                    curso.cupoMaximo = 30;
                    curso.estado = "publicado";
                    curso.docenteId = docenteDb.id;
                    long cursoIdInserted = db.cursoDao().insert(curso);

                    Matricula matriculas = new Matricula();
                    matriculas.estudianteId = estudianteDb.id;
                    matriculas.cursoId = (int) cursoIdInserted;
                    matriculas.fechaMatricula = System.currentTimeMillis();
                    db.matriculaDao().matricular(matriculas);

                    // Materiales de prueba para HU de materiales del curso
                    ContenidoCurso m1 = new ContenidoCurso();
                    m1.cursoId = (int) cursoIdInserted;
                    m1.unidad = "Unidad 1";
                    m1.titulo = "Introducción a Android";
                    m1.descripcionContenido = "Ciclo de vida de las Activities";
                    m1.tipoMaterial = "PDF";
                    m1.tamanoArchivo = "2.5 MB";
                    m1.fechaPublicacion = System.currentTimeMillis();
                    db.contenidoCursoDao().insertarContenido(m1);

                    ContenidoCurso m2 = new ContenidoCurso();
                    m2.cursoId = (int) cursoIdInserted;
                    m2.unidad = "Unidad 2";
                    m2.titulo = "Persistencia con Room";
                    m2.descripcionContenido = "Entidades, DAOs y migraciones";
                    m2.tipoMaterial = "PPTX";
                    m2.tamanoArchivo = "4.1 MB";
                    m2.fechaPublicacion = System.currentTimeMillis();
                    db.contenidoCursoDao().insertarContenido(m2);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}