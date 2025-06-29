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
import com.google.firebase.firestore.ListenerRegistration

class ListapacientesFragment : Fragment() {
    private var _binding: FragmentListapacientesBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var uidActual: String
    private val listaPacientes = mutableListOf<Usuario>()
    private lateinit var adapter: PacienteAdapter

    private var solicitudesListenerEmisor: ListenerRegistration? = null
    private var solicitudesListenerReceptor: ListenerRegistration? = null
    private var usuariosListener: ListenerRegistration? = null

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

        startListeners()
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

    private fun startListeners() {
        mostrarLoading()

        val solicitudesRef = db.collection("solicitudes")

        // Listener para solicitudes donde soy emisor y aceptadas
        solicitudesListenerEmisor = solicitudesRef
            .whereEqualTo("estado", "aceptado")
            .whereEqualTo("emisorId", uidActual)
            .addSnapshotListener { snapshotEmisor, e1 ->
                if (e1 != null) {
                    mostrarSinResultados()
                    return@addSnapshotListener
                }

                // Listener para solicitudes donde soy receptor y aceptadas
                solicitudesListenerReceptor = solicitudesRef
                    .whereEqualTo("estado", "aceptado")
                    .whereEqualTo("receptorId", uidActual)
                    .addSnapshotListener { snapshotReceptor, e2 ->
                        if (e2 != null) {
                            mostrarSinResultados()
                            return@addSnapshotListener
                        }

                        val agregadosIds = mutableSetOf<String>()

                        snapshotEmisor?.documents?.forEach { doc ->
                            doc.getString("receptorId")?.let { agregadosIds.add(it) }
                        }

                        snapshotReceptor?.documents?.forEach { doc ->
                            doc.getString("emisorId")?.let { agregadosIds.add(it) }
                        }

                        if (agregadosIds.isEmpty()) {
                            listaPacientes.clear()
                            adapter.notifyDataSetChanged()
                            mostrarSinResultados()
                            return@addSnapshotListener
                        }

                        usuariosListener?.remove()
                        usuariosListener = db.collection("usuarios")
                            .whereIn(FieldPath.documentId(), agregadosIds.toList())
                            .addSnapshotListener { usuariosSnapshot, e3 ->
                                if (e3 != null) {
                                    mostrarSinResultados()
                                    return@addSnapshotListener
                                }

                                listaPacientes.clear()
                                usuariosSnapshot?.documents?.forEach { doc ->
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
                    }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        solicitudesListenerEmisor?.remove()
        solicitudesListenerReceptor?.remove()
        usuariosListener?.remove()
    }
}