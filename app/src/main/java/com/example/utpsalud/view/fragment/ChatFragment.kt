package com.example.utpsalud.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.utpsalud.databinding.FragmentChatBinding
import com.example.utpsalud.view.adapter.ContactoAdapter
import com.example.utpsalud.viewmodel.ChatViewModel

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ContactoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ContactoAdapter(emptyList()) { usuario ->
            // Acción cuando se hace click sobre un contacto
            // Por ejemplo: abrir conversación
        }

        binding.rvPacientes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPacientes.adapter = adapter

        observarViewModel()
        viewModel.cargarContactos()
    }

    private fun observarViewModel() {
        viewModel.listaContactos.observe(viewLifecycleOwner) { lista ->
            adapter.actualizarLista(lista)
        }

        viewModel.mostrarLoading.observe(viewLifecycleOwner) {
            binding.progressBarContactos.visibility = if (it) View.VISIBLE else View.GONE
        }

        viewModel.mostrarSinResultados.observe(viewLifecycleOwner) {
            binding.tvSinPacientes.visibility = if (it) View.VISIBLE else View.GONE
            binding.rvPacientes.visibility = if (it) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}