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
    Entrega.class,
    ActividadCurso.class,
    NotificacionItem.class,
    Convocatoria.class,
    Postulacion.class
}, version = 16) // Migración 15 -> 16 para preferencias de notificaciones del docente

public abstract class AppDatabase extends RoomDatabase {
    public abstract UsuarioDao usuarioDao();
    public abstract CursoDao cursoDao();

    public abstract MatriculaDao matriculaDao();
    public abstract ContenidoCursoDao contenidoCursoDao();
    public abstract RegistroAccesoMaterialDao registroAccesoMaterialDao();
    public abstract EntregaDao entregaDao();
    public abstract ActividadCursoDao actividadCursoDao();
    public abstract NotificacionDao notificacionDao();
    public abstract ConvocatoriaDao convocatoriaDao();
    public abstract PostulacionDao postulacionDao();

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

    static final Migration MIGRACION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `actividades_curso` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `cursoId` INTEGER NOT NULL, `titulo` TEXT, `instrucciones` TEXT, `fechaLimite` INTEGER NOT NULL, `porcentaje` INTEGER NOT NULL, `aceptaFueraDePlazo` INTEGER NOT NULL)");
        }
    };

    static final Migration MIGRACION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE actividades_curso ADD COLUMN archivoUrl TEXT");
        }
    };

    static final Migration MIGRACION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE actividades_curso ADD COLUMN fechaApertura INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE actividades_curso ADD COLUMN fechaCierre INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE actividades_curso ADD COLUMN visible INTEGER NOT NULL DEFAULT 1");
        }
    };

    static final Migration MIGRACION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            if (!existeColumna(db, "entregas", "actividadId")) {
                db.execSQL("ALTER TABLE entregas ADD COLUMN actividadId INTEGER NOT NULL DEFAULT 0");
            }
            if (!existeColumna(db, "entregas", "nombresArchivos")) {
                db.execSQL("ALTER TABLE entregas ADD COLUMN nombresArchivos TEXT");
            }
            if (!existeColumna(db, "entregas", "tamanoTotalBytes")) {
                db.execSQL("ALTER TABLE entregas ADD COLUMN tamanoTotalBytes INTEGER NOT NULL DEFAULT 0");
            }
            if (!existeColumna(db, "entregas", "comprobanteCodigo")) {
                db.execSQL("ALTER TABLE entregas ADD COLUMN comprobanteCodigo TEXT");
            }
            if (!existeColumna(db, "entregas", "calificacion")) {
                db.execSQL("ALTER TABLE entregas ADD COLUMN calificacion REAL");
            }
            if (!existeColumna(db, "entregas", "retroalimentacion")) {
                db.execSQL("ALTER TABLE entregas ADD COLUMN retroalimentacion TEXT");
            }
            if (!existeColumna(db, "entregas", "fechaCalificacion")) {
                db.execSQL("ALTER TABLE entregas ADD COLUMN fechaCalificacion INTEGER NOT NULL DEFAULT 0");
            }
            if (!existeColumna(db, "entregas", "autorModificacion")) {
                db.execSQL("ALTER TABLE entregas ADD COLUMN autorModificacion TEXT");
            }
            if (!existeColumna(db, "entregas", "historialModificaciones")) {
                db.execSQL("ALTER TABLE entregas ADD COLUMN historialModificaciones TEXT");
            }
        }
    };

    static final Migration MIGRACION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE entregas ADD COLUMN solicitudRevision INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE entregas ADD COLUMN motivoRevision TEXT");
            db.execSQL("ALTER TABLE entregas ADD COLUMN estadoRevision TEXT");
        }
    };

    static final Migration MIGRACION_12_13 = new Migration(12, 13) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            if (!existeColumna(db, "usuarios", "notifNuevoMaterial")) {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN notifNuevoMaterial INTEGER NOT NULL DEFAULT 1");
            }
            if (!existeColumna(db, "usuarios", "notifNuevaActividad")) {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN notifNuevaActividad INTEGER NOT NULL DEFAULT 1");
            }
            if (!existeColumna(db, "usuarios", "notifEntrega48h")) {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN notifEntrega48h INTEGER NOT NULL DEFAULT 1");
            }
            if (!existeColumna(db, "usuarios", "notifNotaPublicada")) {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN notifNotaPublicada INTEGER NOT NULL DEFAULT 1");
            }
            db.execSQL("CREATE TABLE IF NOT EXISTS `notificaciones` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `usuarioId` INTEGER NOT NULL, `titulo` TEXT, `mensaje` TEXT, `tipo` TEXT, `fecha` INTEGER NOT NULL, `leida` INTEGER NOT NULL DEFAULT 0)");
        }
    };

    static final Migration MIGRACION_13_14 = new Migration(13, 14) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `convocatorias` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `institucionDestino` TEXT, `pais` TEXT, `idioma` TEXT, `periodo` TEXT, `requisitos` TEXT, `documentosExigidos` TEXT, `enlaceTerminos` TEXT, `fechaCierre` INTEGER NOT NULL, `estado` TEXT)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `postulaciones` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `convocatoriaId` INTEGER NOT NULL, `estudianteId` INTEGER NOT NULL, `semestreActual` TEXT, `cartaIntencionPdf` TEXT, `certificadoNotasPdf` TEXT, `pazYSalvoPdf` TEXT, `certificadoIdiomaPdf` TEXT, `cartaMotivacionPdf` TEXT, `fechaPostulacion` INTEGER NOT NULL, `estado` TEXT, `observacionAdmin` TEXT)");
        }
    };

    static final Migration MIGRACION_14_15 = new Migration(14, 15) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            if (!existeColumna(db, "notificaciones", "cursoId")) {
                db.execSQL("ALTER TABLE notificaciones ADD COLUMN cursoId INTEGER NOT NULL DEFAULT 0");
            }
            if (!existeColumna(db, "notificaciones", "actividadId")) {
                db.execSQL("ALTER TABLE notificaciones ADD COLUMN actividadId INTEGER NOT NULL DEFAULT 0");
            }
        }
    };

    static final Migration MIGRACION_15_16 = new Migration(15, 16) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            if (!existeColumna(db, "usuarios", "notifEntregaTarea")) {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN notifEntregaTarea INTEGER NOT NULL DEFAULT 1");
            }
            if (!existeColumna(db, "usuarios", "notifReemplazoTarea")) {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN notifReemplazoTarea INTEGER NOT NULL DEFAULT 1");
            }
            if (!existeColumna(db, "usuarios", "notifEntregaTarde")) {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN notifEntregaTarde INTEGER NOT NULL DEFAULT 1");
            }
            if (!existeColumna(db, "usuarios", "notifSolicitudRevision")) {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN notifSolicitudRevision INTEGER NOT NULL DEFAULT 1");
            }
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCIA == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCIA == null) {
                    INSTANCIA = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "intercambios_db_v2") // <-- Nombre cambiado
                            .addMigrations(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4, MIGRACION_4_5, MIGRACION_5_6, MIGRACION_6_7, MIGRACION_7_8, MIGRACION_8_9, MIGRACION_9_10, MIGRACION_10_11, MIGRACION_11_12, MIGRACION_12_13, MIGRACION_13_14, MIGRACION_14_15, MIGRACION_15_16)
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

                    // Convocatorias de Movilidad de prueba (HU-19 y HU-20)
                    Convocatoria c1 = new Convocatoria();
                    c1.institucionDestino = "Universidad Politécnica de Madrid";
                    c1.pais = "España";
                    c1.idioma = "Español";
                    c1.periodo = "2027-1";
                    c1.requisitos = "Promedio acumulado >= 3.8, haber cursado al menos 50% de la carrera.";
                    c1.documentosExigidos = "Certificado de notas, paz y salvo, carta de motivación e intención.";
                    c1.enlaceTerminos = "https://www.upm.es/intercambio";
                    c1.fechaCierre = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000);
                    c1.estado = "activa";
                    db.convocatoriaDao().insertarConvocatoria(c1);

                    Convocatoria c2 = new Convocatoria();
                    c2.institucionDestino = "Technical University of Munich";
                    c2.pais = "Alemania";
                    c2.idioma = "Inglés";
                    c2.periodo = "2027-1";
                    c2.requisitos = "Promedio >= 4.0, certificado TOEFL/IELTS B2+, paz y salvo.";
                    c2.documentosExigidos = "Certificado de idioma, certificado de notas, carta de motivación.";
                    c2.enlaceTerminos = "https://www.tum.de/exchange";
                    c2.fechaCierre = System.currentTimeMillis() + (45L * 24 * 60 * 60 * 1000);
                    c2.estado = "activa";
                    db.convocatoriaDao().insertarConvocatoria(c2);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}