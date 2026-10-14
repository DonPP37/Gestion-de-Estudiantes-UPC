package com.example.intercambioacademicoupc.adapters;

import android.content.Context;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.intercambioacademicoupc.R;
import com.example.intercambioacademicoupc.models.Entrega;
import com.example.intercambioacademicoupc.models.Usuario;

import java.util.List;
import java.util.Map;

public class EntregasAdapter extends RecyclerView.Adapter<EntregasAdapter.EntregaViewHolder> {

    public interface OnEntregaClickListener {
        void onCalificar(Entrega entrega, Usuario estudiante);
        void onVerArchivos(Entrega entrega);
    }

    private final List<Entrega> listaEntregas;
    private final Map<Integer, Usuario> mapaEstudiantes;
    private final boolean esDocente;
    private final OnEntregaClickListener listener;

    public EntregasAdapter(List<Entrega> listaEntregas,
                           Map<Integer, Usuario> mapaEstudiantes,
                           boolean esDocente,
                           OnEntregaClickListener listener) {
        this.listaEntregas = listaEntregas;
        this.mapaEstudiantes = mapaEstudiantes;
        this.esDocente = esDocente;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EntregaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_entrega, parent, false);
        return new EntregaViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EntregaViewHolder holder, int position) {
        Entrega entrega = listaEntregas.get(position);
        Usuario estudiante = mapaEstudiantes.get(entrega.estudianteId);

        String nombreEstudiante = estudiante != null
                ? estudiante.nombre + " " + estudiante.apellido + " (" + estudiante.codigoEstudiantil + ")"
                : "Estudiante #" + entrega.estudianteId;
        holder.tvEntregaEstudiante.setText(nombreEstudiante);

        // Estado
        String estado = entrega.estado != null ? entrega.estado : "entregada";
        holder.tvEntregaEstado.setText(estado.toUpperCase());
        if ("calificada".equalsIgnoreCase(estado)) {
            holder.tvEntregaEstado.setBackgroundColor(Color.parseColor("#E8F5E9"));
            holder.tvEntregaEstado.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            holder.tvEntregaEstado.setBackgroundColor(Color.parseColor("#FFF3E0"));
            holder.tvEntregaEstado.setTextColor(Color.parseColor("#E65100"));
        }

        // Fecha y comprobante
        String fechaStr = DateFormat.format("dd/MM/yyyy HH:mm:ss", entrega.fechaEntrega).toString();
        String comprobante = entrega.comprobanteCodigo != null ? entrega.comprobanteCodigo : "N/A";
        holder.tvEntregaFechaYComprobante.setText("Entregado: " + fechaStr + "\nComprobante: " + comprobante);

        // Texto de entrega
        if (entrega.descripcionEntrega != null && !entrega.descripcionEntrega.trim().isEmpty()) {
            holder.tvEntregaTexto.setVisibility(View.VISIBLE);
            holder.tvEntregaTexto.setText("Texto / Respuesta: " + entrega.descripcionEntrega);
        } else {
            holder.tvEntregaTexto.setVisibility(View.GONE);
        }

        // Archivos adjuntos
        if (entrega.nombresArchivos != null && !entrega.nombresArchivos.trim().isEmpty()) {
            holder.tvEntregaArchivos.setVisibility(View.VISIBLE);
            String tamano = entrega.tamanoTotalBytes > 0
                    ? String.format(" (%.2f MB)", (double) entrega.tamanoTotalBytes / (1024 * 1024))
                    : "";
            holder.tvEntregaArchivos.setText("📎 Archivo(s): " + entrega.nombresArchivos + tamano);
            holder.tvEntregaArchivos.setOnClickListener(v -> {
                if (listener != null) listener.onVerArchivos(entrega);
            });
        } else if (entrega.archivoUrl != null && !entrega.archivoUrl.trim().isEmpty()) {
            holder.tvEntregaArchivos.setVisibility(View.VISIBLE);
            holder.tvEntregaArchivos.setText("📎 Archivo adjunto (clic para ver)");
            holder.tvEntregaArchivos.setOnClickListener(v -> {
                if (listener != null) listener.onVerArchivos(entrega);
            });
        } else {
            holder.tvEntregaArchivos.setVisibility(View.VISIBLE);
            holder.tvEntregaArchivos.setText("Sin archivos adjuntos (Entrega de texto)");
        }

        // Sección calificación
        if (entrega.calificacion != null) {
            holder.layoutCalificacionInfo.setVisibility(View.VISIBLE);
            holder.tvEntregaNota.setText(String.format("Nota: %.1f / 5.0", entrega.calificacion));

            if (entrega.retroalimentacion != null && !entrega.retroalimentacion.trim().isEmpty()) {
                holder.tvEntregaFeedback.setVisibility(View.VISIBLE);
                holder.tvEntregaFeedback.setText("Retroalimentación: " + entrega.retroalimentacion);
            } else {
                holder.tvEntregaFeedback.setVisibility(View.GONE);
            }

            if (entrega.historialModificaciones != null && !entrega.historialModificaciones.trim().isEmpty()) {
                holder.tvEntregaHistorial.setVisibility(View.VISIBLE);
                holder.tvEntregaHistorial.setText(entrega.historialModificaciones);
            } else if (entrega.autorModificacion != null) {
                holder.tvEntregaHistorial.setVisibility(View.VISIBLE);
                String fMod = entrega.fechaCalificacion > 0 ? DateFormat.format("dd/MM/yyyy HH:mm", entrega.fechaCalificacion).toString() : "";
                holder.tvEntregaHistorial.setText("Calificado por: " + entrega.autorModificacion + " (" + fMod + ")");
            } else {
                holder.tvEntregaHistorial.setVisibility(View.GONE);
            }
        } else {
            holder.layoutCalificacionInfo.setVisibility(View.GONE);
        }

        // Botón docente
        if (esDocente) {
            holder.btnCalificarEntrega.setVisibility(View.VISIBLE);
            holder.btnCalificarEntrega.setText(entrega.calificacion != null ? "Modificar Calificación" : "Calificar Entrega");
            holder.btnCalificarEntrega.setOnClickListener(v -> {
                if (listener != null) listener.onCalificar(entrega, estudiante);
            });
        } else {
            holder.btnCalificarEntrega.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return listaEntregas.size();
    }

    public static class EntregaViewHolder extends RecyclerView.ViewHolder {
        TextView tvEntregaEstudiante, tvEntregaEstado, tvEntregaFechaYComprobante, tvEntregaTexto, tvEntregaArchivos;
        LinearLayout layoutCalificacionInfo;
        TextView tvEntregaNota, tvEntregaFeedback, tvEntregaHistorial;
        Button btnCalificarEntrega;

        public EntregaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEntregaEstudiante = itemView.findViewById(R.id.tvEntregaEstudiante);
            tvEntregaEstado = itemView.findViewById(R.id.tvEntregaEstado);
            tvEntregaFechaYComprobante = itemView.findViewById(R.id.tvEntregaFechaYComprobante);
            tvEntregaTexto = itemView.findViewById(R.id.tvEntregaTexto);
            tvEntregaArchivos = itemView.findViewById(R.id.tvEntregaArchivos);
            layoutCalificacionInfo = itemView.findViewById(R.id.layoutCalificacionInfo);
            tvEntregaNota = itemView.findViewById(R.id.tvEntregaNota);
            tvEntregaFeedback = itemView.findViewById(R.id.tvEntregaFeedback);
            tvEntregaHistorial = itemView.findViewById(R.id.tvEntregaHistorial);
            btnCalificarEntrega = itemView.findViewById(R.id.btnCalificarEntrega);
        }
    }
}
