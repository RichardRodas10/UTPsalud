package com.example.utpsalud.adapter

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

class PacienteAdapter(
    private val pacientes: List<Usuario>
) : RecyclerView.Adapter<PacienteAdapter.PacienteViewHolder>() {

    inner class PacienteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgPerfil: CircleImageView = itemView.findViewById(R.id.profileImagePaciente)
        val textNombre: TextView = itemView.findViewById(R.id.textNombrePaciente)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PacienteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contenedor_pacientes, parent, false)
        return PacienteViewHolder(view)
    }

    override fun onBindViewHolder(holder: PacienteViewHolder, position: Int) {
        val paciente = pacientes[position]
        val primerNombre = paciente.nombre.split(" ").firstOrNull() ?: ""
        val primerApellido = paciente.apellido.split(" ").firstOrNull() ?: ""
        holder.textNombre.text = "$primerNombre $primerApellido"

        if (!paciente.fotoPerfilBase64.isNullOrEmpty()) {
            val decodedBytes = Base64.decode(paciente.fotoPerfilBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            holder.imgPerfil.setImageBitmap(bitmap)
        } else {
            holder.imgPerfil.setImageResource(R.drawable.ic_account)
        }
    }

    override fun getItemCount(): Int = pacientes.size
}
