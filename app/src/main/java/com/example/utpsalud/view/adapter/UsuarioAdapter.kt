package com.example.utpsalud.view.adapter

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
    var usuarios: List<Usuario>,
    var estadoSolicitudes: MutableMap<String, String>,
    var uidActual: String,
    var esAdmin: Boolean,
    private val onAgregar: (Usuario) -> Unit,
    private val onCancelar: (Usuario) -> Unit,
    private val onConfirmar: (Usuario) -> Unit,
    private val onClickItem: ((Usuario) -> Unit)? = null
) : RecyclerView.Adapter<UsuarioAdapter.UsuarioViewHolder>() {

    inner class UsuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textNombre: TextView = itemView.findViewById(R.id.textName)
        val textApellido: TextView = itemView.findViewById(R.id.textApe)
        val imgPerfil: CircleImageView = itemView.findViewById(R.id.profileImage)
        val btnAccion: Button = itemView.findViewById(R.id.btnInstrucciones)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycle_perfil, parent, false)
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

        // Click en el item completo
        holder.itemView.setOnClickListener {
            onClickItem?.invoke(usuario)
        }

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

            estado == "vinculado_admin" || estado == "solicitud_en_curso" || estado == "no_disponible" || (!esAdmin && otroVinculoActivo) -> {
                holder.btnAccion.text = "No disponible"
                holder.btnAccion.setBackgroundColor(Color.LTGRAY)
                holder.btnAccion.setOnClickListener {
                    val mensaje = when {
                        esAdmin && estado == "vinculado_admin" ->
                            "Este paciente ya está vinculado con un médico. No puede recibir nuevas solicitudes."
                        esAdmin && estado == "solicitud_en_curso" ->
                            "Este paciente ya tiene una solicitud pendiente con otro médico."
                        estado == "no_disponible" && !esAdmin && estadoSolicitudes.values.any { it == "recibida" } ->
                            "Tienes una solicitud pendiente de un médico por confirmar antes de contactar a otro."
                        estado == "no_disponible" && !esAdmin ->
                            "Ya tienes un médico asignado. Cancela tu vínculo actual para enviar otra solicitud."
                        else -> "No disponible"
                    }

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
        if (nuevoEstado == null) {
            estadoSolicitudes.remove(uid)
        } else {
            estadoSolicitudes[uid] = nuevoEstado
        }
        val index = usuarios.indexOfFirst { it.uid == uid }
        if (index != -1) {
            notifyItemChanged(index)
        }
    }

    // Para facilitar copias parciales (útil si quieres crear nuevos adapters con cambios mínimos)
    fun copy(
        usuarios: List<Usuario> = this.usuarios,
        estadoSolicitudes: MutableMap<String, String> = this.estadoSolicitudes,
        uidActual: String = this.uidActual,
        esAdmin: Boolean = this.esAdmin
    ): UsuarioAdapter {
        return UsuarioAdapter(
            usuarios,
            estadoSolicitudes,
            uidActual,
            esAdmin,
            onAgregar,
            onCancelar,
            onConfirmar,
            onClickItem
        )
    }

    fun actualizarUsuarios(nuevosUsuarios: List<Usuario>) {
        usuarios = nuevosUsuarios
        notifyDataSetChanged()
    }

    fun actualizarEstadoSolicitudes(nuevosEstados: MutableMap<String, String>) {
        estadoSolicitudes = nuevosEstados
        notifyDataSetChanged()
    }

}