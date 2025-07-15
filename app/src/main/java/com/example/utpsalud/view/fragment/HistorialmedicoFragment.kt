package com.example.utpsalud.view.fragment

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.utpsalud.R
import com.example.utpsalud.databinding.FragmentHistorialmedicoBinding
import com.example.utpsalud.view.activity.HistorialActivity
import com.example.utpsalud.view.adapter.HistorialAdapter
import com.example.utpsalud.viewmodel.HistorialViewModel
import com.google.firebase.firestore.FirebaseFirestore

class HistorialmedicoFragment : Fragment() {

    private var _binding: FragmentHistorialmedicoBinding? = null
    private val binding get() = _binding!!

    private val historialViewModel: HistorialViewModel by viewModels()
    private lateinit var adapter: HistorialAdapter

    private var pacienteUid: String? = null

    private val meses = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )
    private val anios = listOf("2024", "2025") // Ajusta años según necesidad

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistorialmedicoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ocultar foto, nombre y dni por defecto
        binding.profileImagePaciente.visibility = View.GONE
        binding.textNombrePaciente.visibility = View.GONE
        binding.textDni.visibility = View.GONE

        // Obtener SharedPreferences
        val prefs = requireContext().getSharedPreferences("UTPSalud", Context.MODE_PRIVATE)

        // Obtener datos desde argumentos (si los hay)
        val nombre = arguments?.getString("nombre")
        val apellido = arguments?.getString("apellido")
        val fotoBase64 = arguments?.getString("fotoPerfilBase64")
        pacienteUid = arguments?.getString("uid")

        if (pacienteUid == null) {
            // Intentar recuperar paciente de SharedPreferences
            pacienteUid = prefs.getString("ultimoPacienteUid", null)
        }

        if (pacienteUid == null) {
            // No hay paciente seleccionado
            binding.textSinDatos.text = "Seleccione un paciente desde la lista para ver su historial"
            binding.textSinDatos.visibility = View.VISIBLE
            binding.recyclerHistorial.visibility = View.GONE
            return
        }

        // Mostrar nombre y foto si están disponibles
        val nombreMostrar = nombre ?: prefs.getString("ultimoPacienteNombre", null)
        val apellidoMostrar = apellido ?: prefs.getString("ultimoPacienteApellido", null)
        val fotoMostrar = fotoBase64 ?: prefs.getString("ultimoPacienteFoto", null)

        if (nombreMostrar != null && apellidoMostrar != null) {
            val primerNombre = nombreMostrar.split(" ").firstOrNull() ?: ""
            val apellidos = apellidoMostrar.split(" ").take(2).joinToString(" ")
            binding.textNombrePaciente.text = "$primerNombre $apellidos"
            binding.textNombrePaciente.visibility = View.VISIBLE
        }

        if (!fotoMostrar.isNullOrEmpty()) {
            val decodedBytes = Base64.decode(fotoMostrar, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            binding.profileImagePaciente.setImageBitmap(bitmap)
            binding.profileImagePaciente.visibility = View.VISIBLE
        } else {
            binding.profileImagePaciente.setImageResource(com.example.utpsalud.R.drawable.ic_account)
            binding.profileImagePaciente.visibility = View.VISIBLE
        }

        // CONSULTAR DNI desde Firestore y mostrarlo
        pacienteUid?.let { uid ->
            db.collection("usuarios").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val dni = document.getString("dni") ?: ""
                        if (dni.isNotEmpty()) {
                            binding.textDni.text = dni
                            binding.textDni.visibility = View.VISIBLE
                        } else {
                            binding.textDni.visibility = View.GONE
                        }
                    } else {
                        binding.textDni.visibility = View.GONE
                    }
                }
                .addOnFailureListener {
                    binding.textDni.visibility = View.GONE
                }
        }

        // Configurar spinners
        val mesAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_selected, meses)
        mesAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
        binding.spinnerMes.adapter = mesAdapter

        val anioAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_selected, anios)
        anioAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
        binding.spinnerAnio.adapter = anioAdapter

        // Seleccionar mes y año actual por defecto
        val calendario = java.util.Calendar.getInstance()
        binding.spinnerMes.setSelection(calendario.get(java.util.Calendar.MONTH))
        binding.spinnerAnio.setSelection(anios.indexOf(calendario.get(java.util.Calendar.YEAR).toString()))

        // Configurar adapter RecyclerView
        adapter = HistorialAdapter(emptyList()) { fecha ->
            val intent = Intent(requireContext(), HistorialActivity::class.java).apply {
                putExtra("fechaMedicion", fecha)
                putExtra("uidPaciente", pacienteUid)
                putExtra("nombre", binding.textNombrePaciente.text.toString())
            }
            startActivity(intent)
        }

        binding.recyclerHistorial.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHistorial.adapter = adapter

        // Observar datos desde ViewModel
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

        // Listener para actualizar lista al cambiar mes o año
        val spinnerListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                actualizarLista()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        binding.spinnerMes.onItemSelectedListener = spinnerListener
        binding.spinnerAnio.onItemSelectedListener = spinnerListener

        // Cargar inicialmente la lista
        actualizarLista()
    }

    private fun actualizarLista() {
        val mesSeleccionado = binding.spinnerMes.selectedItemPosition
        val anioSeleccionado = binding.spinnerAnio.selectedItem.toString().toInt()
        pacienteUid?.let { uid ->
            historialViewModel.obtenerMedicionesPorUsuario(uid, mesSeleccionado, anioSeleccionado)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}