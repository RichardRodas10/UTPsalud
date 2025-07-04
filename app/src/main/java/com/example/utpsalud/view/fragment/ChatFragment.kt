package com.example.utpsalud.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.utpsalud.databinding.FragmentChatBinding
import com.example.utpsalud.model.Usuario
import com.example.utpsalud.view.activity.ChatActivity
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
            abrirChatConUsuario(usuario)
        }

        binding.rvPacientes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPacientes.adapter = adapter

        observarViewModel()
        viewModel.cargarContactos()
    }

    private fun abrirChatConUsuario(usuario: Usuario) {
        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra("uid", usuario.uid)
            putExtra("nombre", usuario.nombre)
            putExtra("apellido", usuario.apellido)
            putExtra("fotoPerfilBase64", usuario.fotoPerfilBase64)
        }
        startActivity(intent)
    }

    private fun observarViewModel() {
        viewModel.mostrarLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarContactos.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.listaContactos.observe(viewLifecycleOwner) { contactos ->
            adapter.actualizarLista(contactos)

            if (contactos.isNotEmpty()) {
                binding.rvPacientes.visibility = View.VISIBLE
                binding.tvSinPacientes.visibility = View.GONE
            }
        }

        viewModel.mostrarSinResultados.observe(viewLifecycleOwner) { sinResultados ->
            if (sinResultados && viewModel.listaContactos.value.isNullOrEmpty()) {
                viewModel.obtenerRolUsuario { esAdmin ->
                    binding.tvSinPacientes.text = if (esAdmin) {
                        "No tienes pacientes vinculados"
                    } else {
                        "No tienes un médico vinculado"
                    }

                    binding.tvSinPacientes.visibility = View.VISIBLE
                    binding.rvPacientes.visibility = View.GONE
                }
            } else {
                binding.tvSinPacientes.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}