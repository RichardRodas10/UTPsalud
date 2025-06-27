package com.example.utpsalud.adapter

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.utpsalud.R
import com.example.utpsalud.model.Usuario
import de.hdodenhof.circleimageview.CircleImageView

class UsuarioAdapter(
    private val usuarios: List<Usuario>,
    private val estadoSolicitudes: Map<String, String>, // uid -> estado ("pendiente", "aceptado", etc.)
    private val uidActual: String,
    private val onAgregar: (Usuario) -> Unit,
    private val onCancelar: (Usuario) -> Unit,
    private val onConfirmar: (Usuario) -> Unit
) : RecyclerView.Adapter<UsuarioAdapter.UsuarioViewHolder>() {

    inner class UsuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textNombre: TextView = itemView.findViewById(R.id.textName)
        val textApellido: TextView = itemView.findViewById(R.id.textApe)
        val imgPerfil: CircleImageView = itemView.findViewById(R.id.profileImage)
        val btnAccion: Button = itemView.findViewById(R.id.btnInstrucciones)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contenedor_perfil, parent, false)
        return UsuarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = usuarios[position]
        holder.textNombre.text = usuario.nombre
        holder.textApellido.text = usuario.apellido

        // Imagen
        if (!usuario.fotoPerfilBase64.isNullOrEmpty()) {
            val decodedBytes = Base64.decode(usuario.fotoPerfilBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            holder.imgPerfil.setImageBitmap(bitmap)
        } else {
            holder.imgPerfil.setImageResource(R.drawable.ic_account)
        }

        // Determinar estado actual de solicitud
        val estado = estadoSolicitudes[usuario.uid]

        when (estado) {
            "pendiente" -> {
                // Yo envié solicitud → mostrar botón Cancelar
                holder.btnAccion.text = "Cancelar"
                holder.btnAccion.setBackgroundColor(Color.GRAY)
                holder.btnAccion.setOnClickListener { onCancelar(usuario) }
            }

            "recibida" -> {
                // Me enviaron solicitud → mostrar Confirmar
                holder.btnAccion.text = "Confirmar"
                holder.btnAccion.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.azul_marino)
                )
                holder.btnAccion.setOnClickListener { onConfirmar(usuario) }
            }

            "aceptado" -> {
                // Ya está aceptado → desactivar botón
                holder.btnAccion.text = "Vinculado"
                holder.btnAccion.setBackgroundColor(Color.LTGRAY)
                holder.btnAccion.isEnabled = false
            }

            else -> {
                // No hay solicitud aún → botón Agregar
                holder.btnAccion.text = "Vincular"
                holder.btnAccion.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.azul_marino)
                )
                holder.btnAccion.setOnClickListener { onAgregar(usuario) }
            }
        }
    }

    override fun getItemCount(): Int = usuarios.size
}
