package com.example.utpsalud.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.utpsalud.view.adapter.PacienteAdapter
import com.example.utpsalud.databinding.FragmentListapacientesBinding
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

        adapter = PacienteAdapter(emptyList())
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