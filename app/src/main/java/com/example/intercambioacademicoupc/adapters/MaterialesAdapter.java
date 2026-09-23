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
    private OnMaterialDeleteListener deleteListener;
    private boolean esInstructor;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public interface OnMaterialClickListener {
        void onDescargarClick(ContenidoCurso material);
    }

    public interface OnMaterialDeleteListener {
        void onDeleteClick(ContenidoCurso material);
    }

    public MaterialesAdapter(List<ContenidoCurso> list, Map<Integer, Long> accesosMap, boolean esInstructor, OnMaterialClickListener listener, OnMaterialDeleteListener deleteListener) {
        this.materialesList = list;
        this.accesosMap = accesosMap;
        this.esInstructor = esInstructor;
        this.listener = listener;
        this.deleteListener = deleteListener;
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
        holder.tvDescripcion.setText(material.descripcionContenido != null ? material.descripcionContenido : "");
        holder.tvTipoTamano.setText((material.tipoMaterial != null ? material.tipoMaterial : "DOC") + " • " + (material.tamanoArchivo != null ? material.tamanoArchivo : "1.0 MB"));
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

        if (esInstructor) {
            holder.btnEliminarMaterial.setVisibility(View.VISIBLE);
            holder.btnEliminarMaterial.setOnClickListener(v -> {
                if (deleteListener != null) deleteListener.onDeleteClick(material);
            });
        } else {
            holder.btnEliminarMaterial.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return materialesList.size();
    }

    public static class MaterialViewHolder extends RecyclerView.ViewHolder {
        TextView tvUnidad, tvTitulo, tvDescripcion, tvTipoTamano, tvFecha, tvUltimoAcceso;
        Button btnDescargar, btnEliminarMaterial;

        public MaterialViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUnidad = itemView.findViewById(R.id.tvUnidad);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvTipoTamano = itemView.findViewById(R.id.tvTipoTamano);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvUltimoAcceso = itemView.findViewById(R.id.tvUltimoAcceso);
            btnDescargar = itemView.findViewById(R.id.btnDescargar);
            btnEliminarMaterial = itemView.findViewById(R.id.btnEliminarMaterial);
        }
    }
}
