package com.example.utpsalud.viewmodel

import android.os.Handler
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.utpsalud.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class BuscarViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _usuarios = MutableLiveData<List<Usuario>>(emptyList())
    val usuarios: LiveData<List<Usuario>> = _usuarios

    private val _estadoSolicitudes = MutableLiveData<Map<String, String>>(emptyMap())
    val estadoSolicitudes: LiveData<Map<String, String>> = _estadoSolicitudes

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _toast = MutableLiveData<String>()
    val toast: LiveData<String> = _toast

    private val _esAdmin = MutableLiveData<Boolean>()
    val esAdmin: LiveData<Boolean> = _esAdmin

    private val _uidActual = MutableLiveData<String>()
    val uidActual: LiveData<String> = _uidActual

    private var ultimaBusqueda = ""

    private val handler = Handler()
    private var runnableBusqueda: Runnable? = null

    private var solicitudesListener: ListenerRegistration? = null

    init {
        _uidActual.value = auth.currentUser?.uid ?: ""
        obtenerRolUsuarioActual {}
    }

    fun obtenerRolUsuarioActual(onComplete: (() -> Unit)? = null) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("usuarios").document(userId).get()
            .addOnSuccessListener { doc ->
                _esAdmin.value = doc.getBoolean("esAdministrador") ?: false
                onComplete?.invoke()
            }
            .addOnFailureListener {
                _toast.value = "Error al obtener rol"
                onComplete?.invoke()
            }
    }

    fun buscarUsuariosDebounce(texto: String) {
        runnableBusqueda?.let { handler.removeCallbacks(it) }
        runnableBusqueda = Runnable {
            buscarUsuarios(texto)
        }
        handler.postDelayed(runnableBusqueda!!, 300)
    }

    fun buscarUsuarios(texto: String) {
        val textoNormalizado = texto.trim().lowercase()
        if (textoNormalizado == ultimaBusqueda) return
        ultimaBusqueda = textoNormalizado

        if (textoNormalizado.isEmpty()) {
            _usuarios.value = emptyList()
            _estadoSolicitudes.value = emptyMap()
            _loading.value = false
            return
        }

        _loading.value = true

        val esAdminLocal = _esAdmin.value ?: false
        val uidLocal = _uidActual.value ?: ""

        db.collection("usuarios")
            .whereEqualTo("esAdministrador", !esAdminLocal)
            .get()
            .addOnSuccessListener { docs ->
                val candidatos = docs.mapNotNull { doc ->
                    val uid = doc.id
                    if (uid == uidLocal) return@mapNotNull null

                    val nombre = doc.getString("nombre") ?: ""
                    val apellido = doc.getString("apellido") ?: ""
                    val nombreCompleto = "$nombre $apellido".lowercase()

                    val palabrasBuscadas = textoNormalizado.split(" ").filter { it.isNotBlank() }
                    val palabrasNombre = nombreCompleto.split(" ").filter { it.isNotBlank() }

                    val coincide = palabrasBuscadas.all { palabra ->
                        palabra.length >= 1 && palabrasNombre.any { it.startsWith(palabra) }
                    }

                    if (coincide) {
                        Usuario(
                            uid = uid,
                            nombre = nombre,
                            apellido = apellido,
                            esAdministrador = doc.getBoolean("esAdministrador") ?: false,
                            fotoPerfilBase64 = doc.getString("fotoPerfilBase64")
                        )
                    } else null
                }

                // Remueve cualquier listener previo antes de añadir uno nuevo
                solicitudesListener?.remove()
                solicitudesListener = db.collection("solicitudes")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            _toast.value = "Error al cargar solicitudes en tiempo real"
                            _loading.value = false
                            return@addSnapshotListener
                        }

                        if (snapshot == null) {
                            _loading.value = false
                            return@addSnapshotListener
                        }

                        val solicitudes = snapshot.documents

                        val estadoMap = mutableMapOf<String, String>()
                        val pacientesVinculados = mutableSetOf<String>()
                        val pacientesConSolicitudPendienteDeOtro = mutableSetOf<String>()
                        val medicosQueMeEnviaronSolicitud = mutableSetOf<String>()

                        for (sol in solicitudes) {
                            val emisorId = sol.getString("emisorId") ?: continue
                            val receptorId = sol.getString("receptorId") ?: continue
                            val estado = sol.getString("estado") ?: "pendiente"

                            if (estado == "aceptado") {
                                if (uidLocal == emisorId || uidLocal == receptorId) {
                                    estadoMap[if (uidLocal == emisorId) receptorId else emisorId] = "aceptado"
                                } else {
                                    pacientesVinculados.add(emisorId)
                                    pacientesVinculados.add(receptorId)
                                }
                            }

                            if (emisorId == uidLocal) estadoMap[receptorId] = estado
                            if (receptorId == uidLocal && estado == "pendiente") {
                                estadoMap[emisorId] = "recibida"
                                medicosQueMeEnviaronSolicitud.add(emisorId)
                            }

                            if (esAdminLocal && estado == "pendiente") {
                                if (emisorId != uidLocal) pacientesConSolicitudPendienteDeOtro.add(receptorId)
                                if (receptorId != uidLocal) pacientesConSolicitudPendienteDeOtro.add(emisorId)
                            }
                        }

                        val listaFinal = candidatos.filter { usuario ->
                            val uid = usuario.uid
                            val estadoActual = estadoMap[uid]

                            if (esAdminLocal) {
                                when {
                                    estadoActual == "aceptado" -> true
                                    pacientesVinculados.contains(uid) -> {
                                        estadoMap[uid] = "no_disponible"
                                        true
                                    }
                                    estadoActual == "recibida" || estadoActual == "pendiente" -> true
                                    pacientesConSolicitudPendienteDeOtro.contains(uid) -> {
                                        estadoMap[uid] = "no_disponible"
                                        true
                                    }
                                    else -> true
                                }
                            } else {
                                when (estadoActual) {
                                    "recibida", "pendiente", "aceptado" -> true
                                    else -> {
                                        if (medicosQueMeEnviaronSolicitud.isNotEmpty() &&
                                            !medicosQueMeEnviaronSolicitud.contains(uid)) {
                                            estadoMap[uid] = "no_disponible"
                                        }
                                        if (pacientesVinculados.contains(uidLocal)) {
                                            estadoMap[uid] = "no_disponible"
                                        }
                                        true
                                    }
                                }
                            }
                        }

                        _estadoSolicitudes.value = estadoMap
                        _usuarios.value = listaFinal
                        _loading.value = false
                    }
            }
            .addOnFailureListener {
                _toast.value = "Error al buscar usuarios"
                _loading.value = false
            }
    }

    fun enviarSolicitud(receptorId: String) {
        val uid = _uidActual.value ?: return
        val solicitud = hashMapOf(
            "emisorId" to uid,
            "receptorId" to receptorId,
            "estado" to "pendiente"
        )
        db.collection("solicitudes").add(solicitud)
            .addOnSuccessListener {
                val estadoMap = _estadoSolicitudes.value?.toMutableMap() ?: mutableMapOf()
                estadoMap[receptorId] = "pendiente"
                _estadoSolicitudes.value = estadoMap
                _toast.value = "Solicitud enviada"
            }
            .addOnFailureListener {
                _toast.value = "Error al enviar solicitud"
            }
    }

    fun cancelarSolicitud(receptorId: String) {
        val uid = _uidActual.value ?: return
        db.collection("solicitudes")
            .whereEqualTo("emisorId", uid)
            .whereEqualTo("receptorId", receptorId)
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    db.collection("solicitudes").document(doc.id).delete()
                }
                val estadoMap = _estadoSolicitudes.value?.toMutableMap() ?: mutableMapOf()
                estadoMap.remove(receptorId)
                _estadoSolicitudes.value = estadoMap
                _toast.value = "Solicitud cancelada"
            }
            .addOnFailureListener {
                _toast.value = "Error al cancelar solicitud"
            }
    }

    fun aceptarSolicitud(emisorId: String) {
        val uid = _uidActual.value ?: return
        db.collection("solicitudes")
            .whereEqualTo("emisorId", emisorId)
            .whereEqualTo("receptorId", uid)
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    db.collection("solicitudes").document(doc.id).update("estado", "aceptado")
                }
                val estadoMap = _estadoSolicitudes.value?.toMutableMap() ?: mutableMapOf()
                estadoMap[emisorId] = "aceptado"
                _estadoSolicitudes.value = estadoMap
                _toast.value = "Solicitud aceptada"
            }
            .addOnFailureListener {
                _toast.value = "Error al aceptar solicitud"
            }
    }

    override fun onCleared() {
        super.onCleared()
        solicitudesListener?.remove()
    }
}