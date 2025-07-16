package com.example.utpsalud.view.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.utpsalud.R
import de.hdodenhof.circleimageview.CircleImageView

class UsuarioDesactivadoAdapter(
    private var listaUsuarios: List<Map<String, Any>>,
    private val onClickItem: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<UsuarioDesactivadoAdapter.ViewHolder>() {

    fun setData(nuevaLista: List<Map<String, Any>>) {
        listaUsuarios = nuevaLista
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombreText: TextView = view.findViewById(R.id.textNombrePaciente)
        val imagenPerfil: CircleImageView = view.findViewById(R.id.profileImagePaciente)

        init {
            view.setOnClickListener {
                val posicion = adapterPosition
                if (posicion != RecyclerView.NO_POSITION) {
                    onClickItem(listaUsuarios[posicion])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycle_desactivado, parent, false)
        return ViewHolder(vista)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val usuario = listaUsuarios[position]
        val nombre = usuario["nombre"] as? String ?: ""
        val apellido = usuario["apellido"] as? String ?: ""
        val nombreCompleto = "$nombre $apellido"

        holder.nombreText.text = nombreCompleto

        val fotoBase64 = usuario["foto"] as? String ?: ""
        if (fotoBase64.isNotEmpty()) {
            try {
                val bytes = Base64.decode(fotoBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.imagenPerfil.setImageBitmap(bitmap)
            } catch (_: Exception) {
                holder.imagenPerfil.setImageResource(R.drawable.ic_account)
            }
        } else {
            holder.imagenPerfil.setImageResource(R.drawable.ic_account)
        }
    }

    override fun getItemCount(): Int = listaUsuarios.size
}