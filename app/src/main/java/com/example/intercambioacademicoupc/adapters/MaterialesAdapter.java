package com.example.intercambioacademicoupc.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.intercambioacademicoupc.R;
import com.example.intercambioacademicoupc.models.ContenidoCurso;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MaterialesAdapter extends RecyclerView.Adapter<MaterialesAdapter.MaterialViewHolder> {

    private List<ContenidoCurso> materialesList;
    private Map<Integer, Long> accesosMap;
    private OnMaterialClickListener listener;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public interface OnMaterialClickListener {
        void onDescargarClick(ContenidoCurso material);
    }

    public MaterialesAdapter(List<ContenidoCurso> list, Map<Integer, Long> accesosMap, OnMaterialClickListener listener) {
        this.materialesList = list;
        this.accesosMap = accesosMap;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MaterialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_material, parent, false);
        return new MaterialViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MaterialViewHolder holder, int position) {
        ContenidoCurso material = materialesList.get(position);

        holder.tvUnidad.setText(material.unidad != null ? material.unidad : "Unidad General");
        holder.tvTitulo.setText(material.titulo);
        holder.tvTipoTamano.setText(material.tipoMaterial + " • " + material.tamanoArchivo);
        holder.tvFecha.setText("Publicado: " + sdf.format(new Date(material.fechaPublicacion)));

        Long ultimoAcceso = accesosMap.get(material.id);
        if (ultimoAcceso != null && ultimoAcceso > 0) {
            holder.tvUltimoAcceso.setText("Último acceso: " + sdf.format(new Date(ultimoAcceso)));
        } else {
            holder.tvUltimoAcceso.setText("Último acceso: Nunca");
        }

        holder.btnDescargar.setOnClickListener(v -> {
            if (listener != null) listener.onDescargarClick(material);
        });
    }

    @Override
    public int getItemCount() {
        return materialesList.size();
    }

    public static class MaterialViewHolder extends RecyclerView.ViewHolder {
        TextView tvUnidad, tvTitulo, tvTipoTamano, tvFecha, tvUltimoAcceso;
        Button btnDescargar;

        public MaterialViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUnidad = itemView.findViewById(R.id.tvUnidad);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvTipoTamano = itemView.findViewById(R.id.tvTipoTamano);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvUltimoAcceso = itemView.findViewById(R.id.tvUltimoAcceso);
            btnDescargar = itemView.findViewById(R.id.btnDescargar);
        }
    }
}