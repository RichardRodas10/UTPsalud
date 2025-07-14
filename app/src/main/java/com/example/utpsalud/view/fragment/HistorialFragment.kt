package com.example.utpsalud.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.utpsalud.databinding.FragmentHistorialBinding
import com.example.utpsalud.view.activity.HistorialActivity
import com.example.utpsalud.view.adapter.HistorialAdapter
import com.example.utpsalud.viewmodel.HistorialViewModel
import java.util.*

class HistorialFragment : Fragment() {

    private var _binding: FragmentHistorialBinding? = null
    private val binding get() = _binding!!

    private val historialViewModel: HistorialViewModel by viewModels()
    private lateinit var adapter: HistorialAdapter

    private val meses = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )
    private val anios = listOf("2024", "2025")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistorialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Adaptadores para los Spinners
        val mesAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, meses)
        mesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMes.adapter = mesAdapter

        val anioAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, anios)
        anioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAnio.adapter = anioAdapter

        // Selección actual
        val calendario = Calendar.getInstance()
        binding.spinnerMes.setSelection(calendario.get(Calendar.MONTH))
        binding.spinnerAnio.setSelection(anios.indexOf(calendario.get(Calendar.YEAR).toString()))

        // Listener común para ambos spinners
        val spinnerListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                actualizarLista()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.spinnerMes.onItemSelectedListener = spinnerListener
        binding.spinnerAnio.onItemSelectedListener = spinnerListener

        // Observar cambios desde el ViewModel
        historialViewModel.mediciones.observe(viewLifecycleOwner) { lista ->
            if (lista.isEmpty()) {
                binding.recyclerHistorial.visibility = View.GONE
                binding.textSinDatos.visibility = View.VISIBLE
            } else {
                adapter.actualizarLista(lista)
                binding.recyclerHistorial.visibility = View.VISIBLE
                binding.textSinDatos.visibility = View.GONE
            }
        }

        // Inicializar con los valores actuales
        actualizarLista()

        adapter = HistorialAdapter(emptyList()) { fecha ->
            val intent = Intent(requireContext(), HistorialActivity::class.java)
            intent.putExtra("fechaMedicion", fecha)
            startActivity(intent)
        }

        binding.recyclerHistorial.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHistorial.adapter = adapter
    }

    private fun actualizarLista() {
        val mesSeleccionado = binding.spinnerMes.selectedItemPosition // enero = 0
        val anioSeleccionado = binding.spinnerAnio.selectedItem.toString().toInt()
        historialViewModel.obtenerMediciones(mesSeleccionado, anioSeleccionado)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}