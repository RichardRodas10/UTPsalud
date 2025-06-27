package com.example.utpsalud.ui.home

import android.os.Bundle
import android.util.Base64
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.utpsalud.adapter.PacienteAdapter
import com.example.utpsalud.databinding.FragmentListapacientesBinding
import com.example.utpsalud.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class ListapacientesFragment : Fragment() {
    private var _binding: FragmentListapacientesBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var uidActual: String
    private val listaPacientes = mutableListOf<Usuario>()
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

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        uidActual = auth.currentUser?.uid ?: ""

        adapter = PacienteAdapter(listaPacientes)
        binding.rvPacientes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPacientes.adapter = adapter

        obtenerPacientesAgregados()
    }

    private fun mostrarLoading() {
        binding.progressBarPacientes.visibility = View.VISIBLE
        binding.rvPacientes.visibility = View.GONE
        binding.tvSinPacientes.visibility = View.GONE
    }

    private fun mostrarPacientes() {
        binding.progressBarPacientes.visibility = View.GONE
        binding.rvPacientes.visibility = View.VISIBLE
        binding.tvSinPacientes.visibility = View.GONE
    }

    private fun mostrarSinResultados() {
        binding.progressBarPacientes.visibility = View.GONE
        binding.rvPacientes.visibility = View.GONE
        binding.tvSinPacientes.visibility = View.VISIBLE
    }

    private fun obtenerPacientesAgregados() {
        mostrarLoading()

        val solicitudesRef = db.collection("solicitudes")

        solicitudesRef
            .whereEqualTo("estado", "aceptado")
            .whereEqualTo("emisorId", uidActual)
            .get()
            .addOnSuccessListener { enviadas ->
                val agregadosIds = mutableSetOf<String>()
                for (doc in enviadas) {
                    val receptorId = doc.getString("receptorId")
                    if (receptorId != null) agregadosIds.add(receptorId)
                }

                solicitudesRef
                    .whereEqualTo("estado", "aceptado")
                    .whereEqualTo("receptorId", uidActual)
                    .get()
                    .addOnSuccessListener { recibidas ->
                        for (doc in recibidas) {
                            val emisorId = doc.getString("emisorId")
                            if (emisorId != null) agregadosIds.add(emisorId)
                        }

                        if (agregadosIds.isEmpty()) {
                            mostrarSinResultados()
                        } else {
                            cargarDatosUsuarios(agregadosIds.toList())
                        }
                    }
                    .addOnFailureListener {
                        mostrarSinResultados()
                    }
            }
            .addOnFailureListener {
                mostrarSinResultados()
            }
    }

    private fun cargarDatosUsuarios(uids: List<String>) {
        db.collection("usuarios")
            .whereIn(FieldPath.documentId(), uids)
            .get()
            .addOnSuccessListener { snapshot ->
                listaPacientes.clear()

                for (doc in snapshot) {
                    val uid = doc.id
                    val usuario = Usuario(
                        uid = uid,
                        nombre = doc.getString("nombre") ?: "",
                        apellido = doc.getString("apellido") ?: "",
                        fotoPerfilBase64 = doc.getString("fotoPerfilBase64"),
                        esAdministrador = doc.getBoolean("esAdministrador") ?: false
                    )
                    listaPacientes.add(usuario)
                }

                if (listaPacientes.isEmpty()) {
                    mostrarSinResultados()
                } else {
                    mostrarPacientes()
                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener {
                mostrarSinResultados()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        obtenerPacientesAgregados()
    }

}