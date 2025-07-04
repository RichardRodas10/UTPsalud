package com.example.utpsalud.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.utpsalud.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class UsuariosViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _toast = MutableLiveData<String>()
    val toast: LiveData<String> = _toast

    private val _uidActual = MutableLiveData<String>()
    val uidActual: LiveData<String> = _uidActual

    private val _esAdmin = MutableLiveData<Boolean>()
    val esAdmin: LiveData<Boolean> = _esAdmin

    private val _estadoSolicitudes = MutableLiveData<MutableMap<String, String>>(mutableMapOf())
    val estadoSolicitudes: LiveData<MutableMap<String, String>> = _estadoSolicitudes

    private val _listaDisponibles = MutableLiveData<List<Usuario>>(emptyList())
    val listaDisponibles: LiveData<List<Usuario>> = _listaDisponibles

    private val _listaEnviadas = MutableLiveData<List<Usuario>>(emptyList())
    val listaEnviadas: LiveData<List<Usuario>> = _listaEnviadas

    private val _listaRecibidas = MutableLiveData<List<Usuario>>(emptyList())
    val listaRecibidas: LiveData<List<Usuario>> = _listaRecibidas

    private var usuariosListener: ListenerRegistration? = null
    private var solicitudesListener: ListenerRegistration? = null

    fun obtenerRolYUsuarios() {
        _loading.value = true
        val uid = auth.currentUser?.uid ?: return

        _uidActual.value = uid

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                val admin = doc.getBoolean("esAdministrador") ?: false
                _esAdmin.value = admin
                cargarUsuarios(admin, uid)
            }
            .addOnFailureListener {
                _loading.value = false
                _toast.value = "Error al obtener rol de usuario"
            }
    }

    private fun cargarUsuarios(esAdmin: Boolean, uidActual: String) {
        _loading.value = true

        usuariosListener?.remove()
        solicitudesListener?.remove()

        usuariosListener = db.collection("usuarios")
            .whereEqualTo("esAdministrador", !esAdmin)
            .addSnapshotListener { snapshotUsuarios, errorUsuarios ->
                if (errorUsuarios != null || snapshotUsuarios == null) {
                    _loading.value = false
                    _toast.value = "Error al escuchar usuarios"
                    return@addSnapshotListener
                }

                val todos = mutableListOf<Usuario>()
                for (doc in snapshotUsuarios) {
                    val uid = doc.id
                    if (uid == uidActual) continue

                    todos.add(
                        Usuario(
                            uid = uid,
                            nombre = doc.getString("nombre") ?: "",
                            apellido = doc.getString("apellido") ?: "",
                            esAdministrador = doc.getBoolean("esAdministrador") ?: false,
                            fotoPerfilBase64 = doc.getString("fotoPerfilBase64")
                        )
                    )
                }

                solicitudesListener = db.collection("solicitudes")
                    .addSnapshotListener { snapshotSolicitudes, errorSolicitudes ->
                        if (errorSolicitudes != null || snapshotSolicitudes == null) {
                            _loading.value = false
                            _toast.value = "Error al escuchar solicitudes"
                            return@addSnapshotListener
                        }

                        val pacientesVinculados = mutableSetOf<String>()
                        val pacientesConSolicitudPendienteDeOtro = mutableSetOf<String>()
                        val medicosQueMeEnviaronSolicitud = mutableSetOf<String>()
                        val estadoMap = mutableMapOf<String, String>()

                        for (sol in snapshotSolicitudes) {
                            val emisorId = sol.getString("emisorId") ?: continue
                            val receptorId = sol.getString("receptorId") ?: continue
                            val estado = sol.getString("estado") ?: "pendiente"

                            if (estado == "aceptado") {
                                pacientesVinculados.add(emisorId)
                                pacientesVinculados.add(receptorId)
                            }

                            if (emisorId == uidActual) {
                                estadoMap[receptorId] = estado
                            }

                            if (receptorId == uidActual && estado == "pendiente") {
                                estadoMap[emisorId] = "recibida"
                                medicosQueMeEnviaronSolicitud.add(emisorId)
                            }

                            if (esAdmin && estado == "pendiente") {
                                if (emisorId != uidActual) {
                                    pacientesConSolicitudPendienteDeOtro.add(receptorId)
                                }
                                if (receptorId != uidActual) {
                                    pacientesConSolicitudPendienteDeOtro.add(emisorId)
                                }
                            }
                        }

                        val disponibles = mutableListOf<Usuario>()
                        val enviadas = mutableListOf<Usuario>()
                        val recibidas = mutableListOf<Usuario>()

                        for (usuario in todos) {
                            val estadoActual = estadoMap[usuario.uid]

                            if (esAdmin) {
                                when {
                                    pacientesVinculados.contains(usuario.uid) -> {}
                                    estadoActual == "recibida" -> recibidas.add(usuario)
                                    estadoActual == "pendiente" -> enviadas.add(usuario)
                                    pacientesConSolicitudPendienteDeOtro.contains(usuario.uid) -> {
                                        estadoMap[usuario.uid] = "no_disponible"
                                        disponibles.add(usuario)
                                    }
                                    else -> disponibles.add(usuario)
                                }
                            } else {
                                when (estadoActual) {
                                    "recibida" -> recibidas.add(usuario)
                                    "pendiente" -> enviadas.add(usuario)
                                    "aceptado" -> {}
                                    else -> {
                                        if (medicosQueMeEnviaronSolicitud.isNotEmpty() &&
                                            !medicosQueMeEnviaronSolicitud.contains(usuario.uid)) {
                                            estadoMap[usuario.uid] = "no_disponible"
                                        }
                                        disponibles.add(usuario)
                                    }
                                }
                            }
                        }

                        _estadoSolicitudes.value = estadoMap
                        _listaDisponibles.value = disponibles
                        _listaEnviadas.value = enviadas
                        _listaRecibidas.value = recibidas
                        _loading.value = false
                    }
            }
    }

    fun enviarSolicitud(receptorId: String) {
        val uid = auth.currentUser?.uid ?: return
        val data = hashMapOf(
            "emisorId" to uid,
            "receptorId" to receptorId,
            "estado" to "pendiente"
        )
        db.collection("solicitudes").add(data)
            .addOnSuccessListener {
                _toast.value = "Solicitud enviada"
                obtenerRolYUsuarios()
            }
            .addOnFailureListener {
                _toast.value = "Error al enviar solicitud"
            }
    }

    fun cancelarSolicitud(receptorId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("solicitudes")
            .whereEqualTo("emisorId", uid)
            .whereEqualTo("receptorId", receptorId)
            .get()
            .addOnSuccessListener { docs ->
                val batch = db.batch()
                docs.forEach { batch.delete(it.reference) }
                batch.commit().addOnSuccessListener {
                    _toast.value = "Solicitud cancelada"
                    obtenerRolYUsuarios()
                }
            }
            .addOnFailureListener {
                _toast.value = "Error al cancelar solicitud"
            }
    }

    fun aceptarSolicitud(emisorId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("solicitudes")
            .whereEqualTo("emisorId", emisorId)
            .whereEqualTo("receptorId", uid)
            .get()
            .addOnSuccessListener { docs ->
                val batch = db.batch()
                docs.forEach { batch.update(it.reference, "estado", "aceptado") }
                batch.commit().addOnSuccessListener {
                    _toast.value = "Solicitud aceptada"
                    obtenerRolYUsuarios()
                }
            }
            .addOnFailureListener {
                _toast.value = "Error al aceptar solicitud"
            }
    }

    override fun onCleared() {
        super.onCleared()
        usuariosListener?.remove()
        solicitudesListener?.remove()
    }
}