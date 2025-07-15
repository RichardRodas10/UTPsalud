package com.example.utpsalud.view.fragment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.utpsalud.R
import com.example.utpsalud.databinding.FragmentListapacientesBinding
import com.example.utpsalud.model.Usuario
import com.example.utpsalud.view.adapter.PacienteAdapter
import com.example.utpsalud.viewmodel.ListapacientesViewModel

class ListapacientesFragment : Fragment() {

    private var _binding: FragmentListapacientesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ListapacientesViewModel by viewModels()
    private lateinit var adapter: PacienteAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListapacientesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PacienteAdapter(
            emptyList(),
            onClickPaciente = { pacienteSeleccionado ->

                // Guardar paciente seleccionado en SharedPreferences
                val prefs = requireContext().getSharedPreferences("UTPSalud", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("ultimoPacienteUid", pacienteSeleccionado.uid)
                    .putString("ultimoPacienteNombre", pacienteSeleccionado.nombre)
                    .putString("ultimoPacienteApellido", pacienteSeleccionado.apellido)
                    .putString("ultimoPacienteFoto", pacienteSeleccionado.fotoPerfilBase64)
                    .apply()

                val bundle = Bundle().apply {
                    putString("uid", pacienteSeleccionado.uid)
                    putString("nombre", pacienteSeleccionado.nombre)
                    putString("apellido", pacienteSeleccionado.apellido)
                    putString("fotoPerfilBase64", pacienteSeleccionado.fotoPerfilBase64)
                }

                val fragment = HistorialmedicoFragment()
                fragment.arguments = bundle

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onLlamarPaciente = { paciente ->
                viewModel.obtenerNumeroDeUsuario(paciente.uid) { numero ->
                    if (!numero.isNullOrEmpty()) {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numero"))
                        startActivity(intent)
                    } else {
                        android.widget.Toast.makeText(requireContext(), "Número no disponible", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        binding.rvPacientes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPacientes.adapter = adapter

        observarViewModel()
        viewModel.cargarPacientes()
    }

    private fun observarViewModel() {
        viewModel.mostrarLoading.observe(viewLifecycleOwner) {
            binding.progressBarPacientes.visibility = if (it) View.VISIBLE else View.GONE
        }

        viewModel.mostrarSinResultados.observe(viewLifecycleOwner) {
            binding.tvSinPacientes.visibility = if (it) View.VISIBLE else View.GONE
            binding.rvPacientes.visibility = if (it) View.GONE else View.VISIBLE
        }

        viewModel.listaPacientes.observe(viewLifecycleOwner) { lista ->
            adapter.actualizarLista(lista)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}