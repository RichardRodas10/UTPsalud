package com.example.utpsalud.view.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.utpsalud.R
import com.example.utpsalud.databinding.FragmentHomeBinding
import com.example.utpsalud.ui.home.HomeFragmentViewModel
import com.example.utpsalud.view.activity.BluetoothActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeFragmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Mostrar datos recibidos desde LecturaActivity si están presentes
        val frecuencia = arguments?.getInt("frecuencia_cardiaca", -1) ?: -1
        val saturacion = arguments?.getInt("saturacion_oxigeno", -1) ?: -1
        val temperatura = arguments?.getFloat("temperatura_promedio", -1f) ?: -1f

        if (frecuencia != -1 && saturacion != -1 && temperatura != -1f) {
            binding.textMedFrecuencia.text = "$frecuencia bpm"
            binding.textMedSaturacion.text = "$saturacion %"
            binding.textMedTemperatura.text = "%.2f °C".format(temperatura)
        }

        // Leer fecha y hora desde SharedPreferences
        val prefs = requireContext().getSharedPreferences("BLE_PREFS", Context.MODE_PRIVATE)
        val fecha = prefs.getString("ultima_fecha", "Fecha: --/--/----")
        val hora = prefs.getString("ultima_hora", "Hora: --:--")

        // Mostrar fecha y hora si están disponibles
        binding.txtFecha.text = fecha
        binding.txtHora.text = hora

        viewModel.ultimaMedicion.observe(viewLifecycleOwner) { m ->
            m?.let {
                binding.textMedFrecuencia.text = "${it.frecuenciaCardiaca} bpm"
                binding.textMedSaturacion.text = "${it.oxigenoSangre} %"
                binding.textMedTemperatura.text = "%.2f °C".format(it.temperatura)
                binding.txtFecha.text =
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        .format(Date(it.fechaMedicion))
            }
        }


        // Observa LiveData para mostrar el diálogo de instrucciones
        viewModel.mostrarInstrucciones.observe(viewLifecycleOwner) { mostrar ->
            if (mostrar) {
                showInstruccionesDialog()
                viewModel.instruccionesMostradas()
            }
        }

        binding.btnInstrucciones.setOnClickListener {
            viewModel.pedirMostrarInstrucciones()
        }

        binding.btnIniciarMedicion.setOnClickListener {
            val intent = Intent(requireContext(), BluetoothActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showInstruccionesDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_instrucciones, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<TextView>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
