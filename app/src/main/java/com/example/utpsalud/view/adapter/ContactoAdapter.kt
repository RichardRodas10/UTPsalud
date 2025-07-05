package com.example.utpsalud.view.adapter

import android.graphics.BitmapFactory
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
    private val onClickItem: ((Usuario) -> Unit)? = null
) : RecyclerView.Adapter<ContactoAdapter.ContactoViewHolder>() {

    inner class ContactoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgPerfil: CircleImageView = itemView.findViewById(R.id.profileImageChat)
        val textNombre: TextView = itemView.findViewById(R.id.textNombreChat)
        val textUltimoMensaje: TextView = itemView.findViewById(R.id.textUltimoMensaje)
        val textHora: TextView = itemView.findViewById(R.id.textHoraChat)
        val textBadge: TextView = itemView.findViewById(R.id.textBadgeMensajes)
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

        if (!contacto.fotoPerfilBase64.isNullOrEmpty()) {
            val decodedBytes = Base64.decode(contacto.fotoPerfilBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            holder.imgPerfil.setImageBitmap(bitmap)
        } else {
            holder.imgPerfil.setImageResource(R.drawable.ic_account)
        }

        // Mostrar último mensaje y hora si existen
        holder.textUltimoMensaje.text = contacto.ultimoMensaje ?: ""

        holder.textHora.text = contacto.timestampUltimoMensaje?.let {
            val date = java.util.Date(it)
            val formatoHora = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            formatoHora.format(date)
        } ?: ""

        // Ocultar badge (puedes implementar lógica real después)
        holder.textBadge.visibility = View.GONE

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