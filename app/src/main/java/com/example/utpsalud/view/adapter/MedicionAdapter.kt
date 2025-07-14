package com.example.utpsalud.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.utpsalud.databinding.RecycleMedicionBinding
import com.example.utpsalud.model.MedicionManual
import java.text.SimpleDateFormat
import java.util.*

class MedicionAdapter(private var mediciones: List<MedicionManual>) :
    RecyclerView.Adapter<MedicionAdapter.MedicionViewHolder>() {

    inner class MedicionViewHolder(val binding: RecycleMedicionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicionViewHolder {
        val binding = RecycleMedicionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MedicionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MedicionViewHolder, position: Int) {
        val medicion = mediciones[position]

        // Formatear hora
        val horaFormat = SimpleDateFormat("HH:mm", Locale("es", "ES"))
        val hora = horaFormat.format(Date(medicion.fechaMedicion))

        // Establecer textos con etiquetas
        holder.binding.textFrecuencia.text = "Frecuencia cardíaca: ${medicion.frecuenciaCardiaca} ppm"
        holder.binding.resultadoFrecuencia.text = medicion.resultadoFrecuenciaCardiaca

        holder.binding.textOxigeno.text = "Oxígeno en sangre: ${medicion.oxigenoSangre} %"
        holder.binding.resultadoOxigeno.text = medicion.resultadoOxigeno

        holder.binding.textEstadoSalud.text = "Estado de salud: ${medicion.estadoSalud}"
        holder.binding.textHoraMedicion.text = "Hora: $hora"

        holder.binding.textTemperatura.text = "Temperatura: ${medicion.temperatura} °C"

        // Cambiar color del estado de salud
        val context = holder.itemView.context
        val colorEstado = when (medicion.estadoSalud) {
            "Crítico" -> android.graphics.Color.RED
            "Moderado" -> 0xFFFFA500.toInt() // Naranja
            "Saludable" -> androidx.core.content.ContextCompat.getColor(context, com.example.utpsalud.R.color.green)
            else -> android.graphics.Color.BLACK
        }
        holder.binding.textEstadoSalud.setTextColor(colorEstado)
    }


    override fun getItemCount(): Int = mediciones.size

    fun actualizarLista(nuevaLista: List<MedicionManual>) {
        mediciones = nuevaLista
        notifyDataSetChanged()
    }
}