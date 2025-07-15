package com.example.utpsalud.view.adapter

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.utpsalud.R
import com.example.utpsalud.model.Usuario
import de.hdodenhof.circleimageview.CircleImageView

class ContactoAdapter(
    private var contactos: List<Usuario>,
    private val onClickItem: ((Usuario) -> Unit)? = null,
    private val esFragment: Boolean = false
) : RecyclerView.Adapter<ContactoAdapter.ContactoViewHolder>() {

    inner class ContactoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgPerfil: CircleImageView = itemView.findViewById(R.id.profileImageChat)
        val textNombre: TextView = itemView.findViewById(R.id.textNombreChat)
        val textUltimoMensaje: TextView = itemView.findViewById(R.id.textUltimoMensaje)
        val textHora: TextView = itemView.findViewById(R.id.textHoraChat)
        val textBadge: TextView = itemView.findViewById(R.id.textBadgeMensajes)
        val imageVisto: CircleImageView = itemView.findViewById(R.id.imageVisto)  // Cambiado a CircleImageView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycle_contacto, parent, false)
        return ContactoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactoViewHolder, position: Int) {
        val contacto = contactos[position]

        val primerNombre = contacto.nombre.split(" ").firstOrNull() ?: ""
        val primerApellido = contacto.apellido.split(" ").firstOrNull() ?: ""
        holder.textNombre.text = "$primerNombre $primerApellido"

        // Foto perfil principal
        if (!contacto.fotoPerfilBase64.isNullOrEmpty()) {
            val decodedBytes = Base64.decode(contacto.fotoPerfilBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            holder.imgPerfil.setImageBitmap(bitmap)
        } else {
            holder.imgPerfil.setImageResource(R.drawable.ic_account)
        }

        if (contacto.ultimoMensaje.isNullOrBlank()) {
            holder.textUltimoMensaje.text = "Envía un mensaje"
            holder.textUltimoMensaje.setTypeface(null, android.graphics.Typeface.ITALIC)
            holder.textUltimoMensaje.setTextColor(Color.parseColor("#888888")) // color gris claro
        } else {
            holder.textUltimoMensaje.text = contacto.ultimoMensaje
            holder.textUltimoMensaje.setTypeface(null, android.graphics.Typeface.NORMAL)
            holder.textUltimoMensaje.setTextColor(Color.parseColor("#888888"))
        }

        holder.textHora.text = contacto.timestampUltimoMensaje?.let {
            val date = java.util.Date(it)
            val formatoHora = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            formatoHora.format(date)
        } ?: ""

        if (contacto.mensajesNoLeidos > 0) {
            // Mostrar badge y ocultar visto
            holder.textBadge.text = contacto.mensajesNoLeidos.toString()
            holder.textBadge.visibility = View.VISIBLE

            holder.imageVisto.visibility = View.GONE

            holder.textHora.setTextColor(holder.itemView.context.getColor(R.color.button))
            holder.textHora.setTypeface(null, android.graphics.Typeface.BOLD)

            holder.textUltimoMensaje.setTextColor(holder.itemView.context.getColor(R.color.gris))
            holder.textUltimoMensaje.setTypeface(null, android.graphics.Typeface.BOLD)

        } else {
            // Sin mensajes no leídos

            holder.textBadge.visibility = View.GONE

            // Mostrar foto de perfil en "visto" solo si el último mensaje enviado fue leído
            if (
                contacto.ultimoMensajeEnviadoLeido &&
                !contacto.fotoPerfilBase64.isNullOrEmpty() &&
                (!esFragment || contacto.ultimoMensajeEsMio == true) // Solo en fragment si es mío
            ) {
                val decodedBytes = Base64.decode(contacto.fotoPerfilBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.imageVisto.setImageBitmap(bitmap)
                holder.imageVisto.visibility = View.VISIBLE
            } else {
                holder.imageVisto.visibility = View.GONE
            }

            holder.textHora.setTextColor(Color.parseColor("#888888"))
            holder.textHora.setTypeface(null, android.graphics.Typeface.NORMAL)

            holder.textUltimoMensaje.setTextColor(Color.parseColor("#888888"))
            holder.textUltimoMensaje.setTypeface(null, android.graphics.Typeface.NORMAL)
        }

        holder.itemView.setOnClickListener {
            onClickItem?.invoke(contacto)
        }
    }

    override fun getItemCount(): Int = contactos.size

    fun actualizarLista(nuevaLista: List<Usuario>) {
        contactos = nuevaLista
        notifyDataSetChanged()
    }
}