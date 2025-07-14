package com.example.utpsalud.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.utpsalud.databinding.RecycleHistorialBinding
import com.example.utpsalud.model.MedicionManual
import java.text.SimpleDateFormat
import java.util.*

class HistorialAdapter(
    private var mediciones: List<MedicionManual>,
    private val onItemClick: (Long) -> Unit
) : RecyclerView.Adapter<HistorialAdapter.HistorialViewHolder>() {

    inner class HistorialViewHolder(val binding: RecycleHistorialBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistorialViewHolder {
        val binding = RecycleHistorialBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistorialViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistorialViewHolder, position: Int) {
        val medicion = mediciones[position]
        val fechaFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        val fechaFormateada = fechaFormat.format(Date(medicion.fechaMedicion))
        holder.binding.textFechaMedicion.text = fechaFormateada
        holder.binding.textFechaMedicion.textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER

        holder.itemView.setOnClickListener {
            onItemClick(medicion.fechaMedicion)
        }
    }

    override fun getItemCount(): Int = mediciones.size

    fun actualizarLista(nuevaLista: List<MedicionManual>) {
        mediciones = nuevaLista
        notifyDataSetChanged()
    }
}