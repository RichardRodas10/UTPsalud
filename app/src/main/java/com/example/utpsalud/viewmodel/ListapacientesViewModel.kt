package com.example.utpsalud.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.utpsalud.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ListapacientesViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val uidActual = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _listaPacientes = MutableLiveData<List<Usuario>>()
    val listaPacientes: LiveData<List<Usuario>> get() = _listaPacientes

    private val _mostrarLoading = MutableLiveData<Boolean>()
    val mostrarLoading: LiveData<Boolean> get() = _mostrarLoading

    private val _mostrarSinResultados = MutableLiveData<Boolean>()
    val mostrarSinResultados: LiveData<Boolean> get() = _mostrarSinResultados

    private var solicitudesListenerEmisor: ListenerRegistration? = null
    private var solicitudesListenerReceptor: ListenerRegistration? = null
    private var usuariosListener: ListenerRegistration? = null

    fun cargarPacientes() {
        _mostrarLoading.value = true

        val solicitudesRef = db.collection("solicitudes")

        solicitudesListenerEmisor = solicitudesRef
            .whereEqualTo("estado", "aceptado")
            .whereEqualTo("emisorId", uidActual)
            .addSnapshotListener { snapshotEmisor, e1 ->

                if (e1 != null) {
                    _mostrarLoading.value = false
                    _mostrarSinResultados.value = true
                    return@addSnapshotListener
                }

                solicitudesListenerReceptor = solicitudesRef
                    .whereEqualTo("estado", "aceptado")
                    .whereEqualTo("receptorId", uidActual)
                    .addSnapshotListener { snapshotReceptor, e2 ->

                        if (e2 != null) {
                            _mostrarLoading.value = false
                            _mostrarSinResultados.value = true
                            return@addSnapshotListener
                        }

                        val idsPacientes = mutableSetOf<String>()
                        snapshotEmisor?.forEach { doc ->
                            doc.getString("receptorId")?.let { idsPacientes.add(it) }
                        }
                        snapshotReceptor?.forEach { doc ->
                            doc.getString("emisorId")?.let { idsPacientes.add(it) }
                        }

                        if (idsPacientes.isEmpty()) {
                            _listaPacientes.value = emptyList()
                            _mostrarLoading.value = false
                            _mostrarSinResultados.value = true
                            return@addSnapshotListener
                        }

                        usuariosListener?.remove()
                        usuariosListener = db.collection("usuarios")
                            .whereIn(FieldPath.documentId(), idsPacientes.toList())
                            .addSnapshotListener { usuariosSnapshot, e3 ->
                                _mostrarLoading.value = false

                                if (e3 != null || usuariosSnapshot == null) {
                                    _mostrarSinResultados.value = true
                                    _listaPacientes.value = emptyList()
                                    return@addSnapshotListener
                                }

                                val pacientes = usuariosSnapshot.documents.map { doc ->
                                    Usuario(
                                        uid = doc.id,
                                        nombre = doc.getString("nombre") ?: "",
                                        apellido = doc.getString("apellido") ?: "",
                                        fotoPerfilBase64 = doc.getString("fotoPerfilBase64"),
                                        esAdministrador = doc.getBoolean("esAdministrador") ?: false
                                    )
                                }

                                _listaPacientes.value = pacientes
                                _mostrarSinResultados.value = pacientes.isEmpty()
                            }
                    }
            }
    }

    override fun onCleared() {
        super.onCleared()
        solicitudesListenerEmisor?.remove()
        solicitudesListenerReceptor?.remove()
        usuariosListener?.remove()
    }
}