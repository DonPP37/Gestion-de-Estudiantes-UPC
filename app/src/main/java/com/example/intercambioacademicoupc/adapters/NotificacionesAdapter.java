package com.example.intercambioacademicoupc.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.intercambioacademicoupc.R;
import com.example.intercambioacademicoupc.models.NotificacionItem;

import java.util.List;

public class NotificacionesAdapter extends RecyclerView.Adapter<NotificacionesAdapter.NotificacionViewHolder> {

    public interface OnNotificacionClickListener {
        void onNotificacionClick(NotificacionItem item);
    }

    private Context context;
    private List<NotificacionItem> listaNotificaciones;
    private OnNotificacionClickListener listener;

    public NotificacionesAdapter(Context context, List<NotificacionItem> listaNotificaciones) {
        this.context = context;
        this.listaNotificaciones = listaNotificaciones;
    }

    public NotificacionesAdapter(Context context, List<NotificacionItem> listaNotificaciones, OnNotificacionClickListener listener) {
        this.context = context;
        this.listaNotificaciones = listaNotificaciones;
        this.listener = listener;
    }

    public void setOnNotificacionClickListener(OnNotificacionClickListener listener) {
        this.listener = listener;
    }

    public void setListaNotificaciones(List<NotificacionItem> lista) {
        this.listaNotificaciones = lista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notificacion, parent, false);
        return new NotificacionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificacionViewHolder holder, int position) {
        NotificacionItem item = listaNotificaciones.get(position);
        holder.tvTitulo.setText(item.titulo);
        holder.tvMensaje.setText(item.mensaje);
        
        String fechaStr = DateFormat.format("dd/MM/yyyy HH:mm", item.fecha).toString();
        holder.tvFecha.setText(fechaStr);

        if (!item.leida) {
            holder.itemView.setBackgroundColor(0xFFE3F2FD); // Light blue tint for unread
            holder.tvTitulo.setTypeface(null, Typeface.BOLD);
        } else {
            holder.itemView.setBackgroundColor(0xFFFFFFFF); // White for read
            holder.tvTitulo.setTypeface(null, Typeface.NORMAL);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificacionClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaNotificaciones != null ? listaNotificaciones.size() : 0;
    }

    static class NotificacionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvMensaje, tvFecha;

        public NotificacionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTituloNotif);
            tvMensaje = itemView.findViewById(R.id.tvMensajeNotif);
            tvFecha = itemView.findViewById(R.id.tvFechaNotif);
        }
    }
}
