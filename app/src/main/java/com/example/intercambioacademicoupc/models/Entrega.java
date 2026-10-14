package com.example.intercambioacademicoupc.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "entregas")
public class Entrega {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int cursoId;
    public int actividadId; // Vínculo con ActividadCurso (HU-13)
    public int estudianteId;
    public String tituloTarea;
    public String descripcionEntrega; // Texto del estudiante
    public String archivoUrl; // Archivos adjuntos (separados por ; o nombres)
    public String nombresArchivos; // Nombres legibles de los archivos
    public long tamanoTotalBytes; // Tamaño acumulado de archivos
    public long fechaEntrega;
    public String estado; // "entregada", "calificada"
    public String comprobanteCodigo; // Código de constancia/comprobante formal

    // Campos de calificación y seguimiento (HU-14)
    public Double calificacion; // Nota numérica (ej: 0.0 - 5.0 o 0 - 100)
    public String retroalimentacion; // Comentarios del docente
    public long fechaCalificacion; // Fecha en que se calificó o modificó
    public String autorModificacion; // Docente que calificó/modificó
    public String historialModificaciones; // Registro de cambios de nota: "Fecha | Autor | Nota previa -> Nueva nota"
}
