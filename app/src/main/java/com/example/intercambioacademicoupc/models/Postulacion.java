package com.example.intercambioacademicoupc.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "postulaciones")
public class Postulacion {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int convocatoriaId;
    public int estudianteId;
    public String semestreActual;
    public String cartaIntencionPdf;
    public String certificadoNotasPdf;
    public String pazYSalvoPdf;
    public String certificadoIdiomaPdf;
    public String cartaMotivacionPdf;
    public long fechaPostulacion;
    public String estado = "en revisión"; // "en revisión", "aprobada", "rechazada"
    public String observacionAdmin;
}
