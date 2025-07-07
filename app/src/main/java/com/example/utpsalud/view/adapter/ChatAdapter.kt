package com.example.utpsalud.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.utpsalud.R
import com.example.utpsalud.model.ChatItem
import com.example.utpsalud.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private var items: List<ChatItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val uidActual = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    companion object {
        const val VIEW_TYPE_ENVIADO = 1
        const val VIEW_TYPE_RECIBIDO = 2
        const val VIEW_TYPE_ENCABEZADO = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when(val item = items[position]) {
            is ChatItem.Mensaje -> if (item.chatMessage.emisorId == uidActual) VIEW_TYPE_ENVIADO else VIEW_TYPE_RECIBIDO
            is ChatItem.Encabezado -> VIEW_TYPE_ENCABEZADO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            VIEW_TYPE_ENVIADO -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mensaje_enviado, parent, false)
                EnviadoViewHolder(view)
            }
            VIEW_TYPE_RECIBIDO -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mensaje_recibido, parent, false)
                RecibidoViewHolder(view)
            }
            VIEW_TYPE_ENCABEZADO -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_header_fecha, parent, false)
                EncabezadoViewHolder(view)
            }
            else -> throw IllegalArgumentException("Tipo de vista desconocido")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is EnviadoViewHolder -> if (item is ChatItem.Mensaje) {
                holder.textMensaje.text = item.chatMessage.mensaje
                holder.textHora.text = formatearHora(item.chatMessage.timestamp)

                val esUltimoMensaje = obtenerUltimoMensajeEnviado()?.timestamp == item.chatMessage.timestamp
                if (esUltimoMensaje && item.chatMessage.leido == true) {
                    holder.textVisto.visibility = View.VISIBLE
                } else {
                    holder.textVisto.visibility = View.GONE
                }

                aplicarMargenSiCambiaTipo(holder.itemView, position, item.chatMessage.emisorId)
            }
            is RecibidoViewHolder -> if (item is ChatItem.Mensaje) {
                holder.textMensaje.text = item.chatMessage.mensaje
                holder.textHora.text = formatearHora(item.chatMessage.timestamp)

                aplicarMargenSiCambiaTipo(holder.itemView, position, item.chatMessage.emisorId)
            }
            is EncabezadoViewHolder -> if (item is ChatItem.Encabezado) {
                holder.textEncabezado.text = item.texto
            }
        }
    }

    private fun aplicarMargenSiCambiaTipo(view: View, position: Int, emisorActual: String) {
        val layoutParams = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return

        // Verificamos si el mensaje anterior es de otro tipo (emisor distinto)
        val margenSuperior = if (
            position > 0 &&
            items[position - 1] is ChatItem.Mensaje &&
            (items[position - 1] as ChatItem.Mensaje).chatMessage.emisorId != emisorActual
        ) {
            24 // más espacio entre bloques
        } else {
            4 // espacio normal
        }

        layoutParams.topMargin = margenSuperior
        view.layoutParams = layoutParams
    }

    override fun getItemCount(): Int = items.size

    fun actualizarMensajesConEncabezados(nuevos: List<ChatMessage>) {
        items = nuevos.conEncabezados()
        notifyDataSetChanged()
    }

    class EnviadoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textMensaje: TextView = view.findViewById(R.id.textMensaje)
        val textHora: TextView = view.findViewById(R.id.textHora)
        val textVisto: TextView = view.findViewById(R.id.textVisto)
    }

    class RecibidoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textMensaje: TextView = view.findViewById(R.id.textMensaje)
        val textHora: TextView = view.findViewById(R.id.textHora)
    }

    class EncabezadoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textEncabezado: TextView = view.findViewById(R.id.textEncabezado)
    }

    private fun formatearHora(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun obtenerUltimoMensajeEnviado(): ChatMessage? {
        return items
            .filterIsInstance<ChatItem.Mensaje>()
            .map { it.chatMessage }
            .filter { it.emisorId == uidActual }
            .maxByOrNull { it.timestamp }
    }
}

// Función extensión para crear lista con encabezados

fun List<ChatMessage>.conEncabezados(): List<ChatItem> {
    if (this.isEmpty()) return emptyList()

    val listaConEncabezados = mutableListOf<ChatItem>()
    val calendarHoy = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    var fechaAnterior: Long = -1

    val sdfDiaSemana = SimpleDateFormat("EEEE", Locale.getDefault()) // lunes, martes, etc.
    val sdfFechaCompleta = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) // fecha completa

    this.sortedBy { it.timestamp }.forEach { mensaje ->
        val calMensaje = Calendar.getInstance().apply {
            timeInMillis = mensaje.timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val tiempoMensaje = calMensaje.timeInMillis

        if (tiempoMensaje != fechaAnterior) {
            val textoEncabezado = when {
                tiempoMensaje == calendarHoy.timeInMillis -> "Hoy"
                tiempoMensaje == calendarHoy.timeInMillis - 24 * 60 * 60 * 1000L -> "Ayer"
                tiempoMensaje >= calendarHoy.timeInMillis - 7 * 24 * 60 * 60 * 1000L -> sdfDiaSemana.format(calMensaje.time).replaceFirstChar { it.uppercaseChar() }
                else -> sdfFechaCompleta.format(calMensaje.time)
            }
            listaConEncabezados.add(ChatItem.Encabezado(textoEncabezado))
            fechaAnterior = tiempoMensaje
        }

        listaConEncabezados.add(ChatItem.Mensaje(mensaje))
    }

    return listaConEncabezados
}