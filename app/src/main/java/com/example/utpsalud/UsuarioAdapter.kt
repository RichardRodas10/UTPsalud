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
import com.google.android.material.snackbar.Snackbar
import de.hdodenhof.circleimageview.CircleImageView

class UsuarioAdapter(
    private val usuarios: List<Usuario>,
    private val estadoSolicitudes: MutableMap<String, String>,
    private val uidActual: String,
    private val esAdmin: Boolean, // NUEVO PARAMETRO: true si soy médico
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

        if (!usuario.fotoPerfilBase64.isNullOrEmpty()) {
            val decodedBytes = Base64.decode(usuario.fotoPerfilBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            holder.imgPerfil.setImageBitmap(bitmap)
        } else {
            holder.imgPerfil.setImageResource(R.drawable.ic_account)
        }

        val estado = estadoSolicitudes[usuario.uid]

        // Solo los pacientes tienen restricción de 1 vínculo
        val otroVinculoActivo = !esAdmin && estadoSolicitudes.any {
            val id = it.key
            val estadoVal = it.value
            id != usuario.uid && (estadoVal == "pendiente" || estadoVal == "aceptado")
        }

        holder.btnAccion.setBackgroundColor(
            ContextCompat.getColor(holder.itemView.context, R.color.azul_marino)
        )
        holder.btnAccion.setTextColor(Color.WHITE)
        holder.btnAccion.isEnabled = true

        when {
            estado == "pendiente" -> {
                holder.btnAccion.text = "Cancelar"
                holder.btnAccion.setBackgroundColor(Color.GRAY)
                holder.btnAccion.setOnClickListener {
                    onCancelar(usuario)
                }
            }

            estado == "recibida" -> {
                holder.btnAccion.text = "Confirmar"
                holder.btnAccion.setOnClickListener {
                    onConfirmar(usuario)
                }
            }

            estado == "aceptado" -> {
                holder.btnAccion.text = "Vinculado"
                holder.btnAccion.setBackgroundColor(Color.LTGRAY)
                holder.btnAccion.isEnabled = false
            }

            otroVinculoActivo -> {
                holder.btnAccion.text = "No disponible"
                holder.btnAccion.setBackgroundColor(Color.LTGRAY)
                holder.btnAccion.setOnClickListener {
                    val mensaje = if (esAdmin)
                        "Este paciente ya tiene un médico asignado."
                    else
                        "Ya tienes un médico asignado. Cancela el vínculo para enviar otra solicitud."

                    Snackbar.make(holder.itemView, mensaje, Snackbar.LENGTH_LONG).show()
                }
            }

            else -> {
                holder.btnAccion.text = "Vincular"
                holder.btnAccion.setOnClickListener {
                    onAgregar(usuario)
                }
            }
        }
    }

    override fun getItemCount(): Int = usuarios.size

    fun actualizarEstado(uid: String, nuevoEstado: String?) {
        estadoSolicitudes[uid] = nuevoEstado ?: ""
        val index = usuarios.indexOfFirst { it.uid == uid }
        if (index != -1) {
            notifyItemChanged(index)
        }
    }
}
