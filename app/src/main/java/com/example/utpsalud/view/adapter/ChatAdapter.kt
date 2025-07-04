package com.example.utpsalud.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.utpsalud.R
import com.example.utpsalud.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private var mensajes: List<ChatMessage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val uidActual = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    companion object {
        const val VIEW_TYPE_ENVIADO = 1
        const val VIEW_TYPE_RECIBIDO = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (mensajes[position].emisorId == uidActual) VIEW_TYPE_ENVIADO else VIEW_TYPE_RECIBIDO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_ENVIADO) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mensaje_enviado, parent, false)
            EnviadoViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mensaje_recibido, parent, false)
            RecibidoViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mensaje = mensajes[position]
        val horaFormateada = formatearHora(mensaje.timestamp)

        if (holder is EnviadoViewHolder) {
            holder.textMensaje.text = mensaje.mensaje
            holder.textHora.text = horaFormateada
        } else if (holder is RecibidoViewHolder) {
            holder.textMensaje.text = mensaje.mensaje
            holder.textHora.text = horaFormateada
        }
    }

    override fun getItemCount(): Int = mensajes.size

    fun actualizarMensajes(nuevos: List<ChatMessage>) {
        mensajes = nuevos
        notifyDataSetChanged()
    }

    class EnviadoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textMensaje: TextView = view.findViewById(R.id.textMensaje)
        val textHora: TextView = view.findViewById(R.id.textHora)
    }

    class RecibidoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textMensaje: TextView = view.findViewById(R.id.textMensaje)
        val textHora: TextView = view.findViewById(R.id.textHora)
    }

    private fun formatearHora(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}