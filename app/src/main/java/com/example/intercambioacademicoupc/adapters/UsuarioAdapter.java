package com.example.intercambioacademicoupc.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.intercambioacademicoupc.R;
import com.example.intercambioacademicoupc.models.Usuario;
import java.util.List;

public class UsuarioAdapter extends RecyclerView.Adapter<UsuarioAdapter.UsuarioViewHolder> {

    private List<Usuario> usuarios;
    private Context context;
    private OnUsuarioInteractionListener listener;
    private String[] roles = {"estudiante", "docente", "administrador"};

    public interface OnUsuarioInteractionListener {
        void onRolCambiado(int usuarioId, String nuevoRol);
        void onUsuarioDesactivado(int usuarioId);
    }

    public UsuarioAdapter(Context context, List<Usuario> usuarios, OnUsuarioInteractionListener listener) {
        this.context = context;
        this.usuarios = usuarios;
        this.listener = listener;
    }

    public void setUsuarios(List<Usuario> nuevosUsuarios) {
        this.usuarios = nuevosUsuarios;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UsuarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Asume que crearás un XML llamado item_usuario.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_usuario, parent, false);
        return new UsuarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsuarioViewHolder holder, int position) {
        Usuario usuario = usuarios.get(position);
        holder.tvNombre.setText(usuario.nombre + " (" + usuario.codigoEstudiantil + ")");
        holder.tvCorreo.setText(usuario.correo);

        // Configurar Spinner de roles
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, roles);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.spRol.setAdapter(spinnerAdapter);

        // Seleccionar el rol actual del usuario en el spinner
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equals(usuario.rol)) {
                holder.spRol.setSelection(i);
                break;
            }
        }

        // Listener para cambiar el rol (evitar llamados automáticos al cargar)
        holder.spRol.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String rolSeleccionado = roles[pos];
                if (!rolSeleccionado.equals(usuario.rol)) {
                    listener.onRolCambiado(usuario.id, rolSeleccionado);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Botón Desactivar
        holder.btnDesactivar.setOnClickListener(v -> {
            listener.onUsuarioDesactivado(usuario.id);
        });
    }

    @Override
    public int getItemCount() {
        return usuarios != null ? usuarios.size() : 0;
    }

    static class UsuarioViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvCorreo;
        Spinner spRol;
        Button btnDesactivar;

        public UsuarioViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tv_item_nombre);
            tvCorreo = itemView.findViewById(R.id.tv_item_correo);
            spRol = itemView.findViewById(R.id.sp_item_rol);
            btnDesactivar = itemView.findViewById(R.id.btn_item_desactivar);
        }
    }
}