package com.example.utpsalud.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.utpsalud.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val uidActual = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _listaContactos = MutableLiveData<List<Usuario>>()
    val listaContactos: LiveData<List<Usuario>> get() = _listaContactos

    private val _mostrarLoading = MutableLiveData<Boolean>()
    val mostrarLoading: LiveData<Boolean> get() = _mostrarLoading

    private val _mostrarSinResultados = MutableLiveData<Boolean>()
    val mostrarSinResultados: LiveData<Boolean> get() = _mostrarSinResultados

    private var solicitudesListener1: ListenerRegistration? = null
    private var solicitudesListener2: ListenerRegistration? = null
    private var usuariosListener: ListenerRegistration? = null

    fun cargarContactos() {
        _mostrarLoading.value = true

        db.collection("usuarios").document(uidActual).get()
            .addOnSuccessListener { userSnapshot ->
                val esAdmin = userSnapshot.getBoolean("esAdministrador") == true
                consultarContactos(esAdmin)
            }
            .addOnFailureListener {
                _mostrarLoading.value = false
                _mostrarSinResultados.value = true
            }
    }

    private fun consultarContactos(esAdmin: Boolean) {
        val solicitudesRef = db.collection("solicitudes")

        val idsRelacionados = mutableSetOf<String>()

        solicitudesListener1 = solicitudesRef
            .whereEqualTo("estado", "aceptado")
            .whereEqualTo("emisorId", uidActual)
            .addSnapshotListener { snapshot1, e1 ->
                if (e1 != null) return@addSnapshotListener

                snapshot1?.forEach { doc ->
                    val receptor = doc.getString("receptorId")
                    if (!receptor.isNullOrEmpty()) idsRelacionados.add(receptor)
                }

                actualizarUsuarios(idsRelacionados)
            }

        solicitudesListener2 = solicitudesRef
            .whereEqualTo("estado", "aceptado")
            .whereEqualTo("receptorId", uidActual)
            .addSnapshotListener { snapshot2, e2 ->
                if (e2 != null) return@addSnapshotListener

                snapshot2?.forEach { doc ->
                    val emisor = doc.getString("emisorId")
                    if (!emisor.isNullOrEmpty()) idsRelacionados.add(emisor)
                }

                actualizarUsuarios(idsRelacionados)
            }
    }

    private fun actualizarUsuarios(ids: Set<String>) {
        if (ids.isEmpty()) {
            _mostrarLoading.value = false
            _listaContactos.value = emptyList()
            _mostrarSinResultados.value = true
            return
        }

        usuariosListener?.remove()
        usuariosListener = db.collection("usuarios")
            .whereIn(FieldPath.documentId(), ids.toList())
            .addSnapshotListener { snapshot, error ->
                _mostrarLoading.value = false

                if (error != null || snapshot == null) {
                    _listaContactos.value = emptyList()
                    _mostrarSinResultados.value = true
                    return@addSnapshotListener
                }

                val contactos = snapshot.documents.map { doc ->
                    Usuario(
                        uid = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        apellido = doc.getString("apellido") ?: "",
                        fotoPerfilBase64 = doc.getString("fotoPerfilBase64"),
                        esAdministrador = doc.getBoolean("esAdministrador") ?: false
                    )
                }

                _listaContactos.value = contactos
                _mostrarSinResultados.value = contactos.isEmpty()
            }
    }

    override fun onCleared() {
        super.onCleared()
        solicitudesListener1?.remove()
        solicitudesListener2?.remove()
        usuariosListener?.remove()
    }
}