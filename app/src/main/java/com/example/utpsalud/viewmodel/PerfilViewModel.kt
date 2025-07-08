package com.example.utpsalud.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.utpsalud.model.UsuarioPerfil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class PerfilViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _usuario = MutableLiveData<UsuarioPerfil>()
    val usuario: LiveData<UsuarioPerfil> = _usuario

    private val _mensaje = MutableLiveData<String>()
    val mensaje: LiveData<String> = _mensaje

    private val _cerrarPantalla = MutableLiveData<Boolean>()
    val cerrarPantalla: LiveData<Boolean> = _cerrarPantalla

    // Estado de solicitud para este perfil (agregar, pendiente, recibida, vinculado, no_disponible)
    private val _estadoRelacion = MutableLiveData<String>()
    val estadoRelacion: LiveData<String> = _estadoRelacion

    private var solicitudListener: ListenerRegistration? = null

    fun cargarDatosUsuario(uid: String) {
        if (uid.isEmpty()) {
            _mensaje.value = "Usuario no válido"
            _cerrarPantalla.value = true
            return
        }

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val usuario = UsuarioPerfil(
                        nombre = doc.getString("nombre") ?: "",
                        apellido = doc.getString("apellido") ?: "",
                        dni = doc.getString("dni") ?: "",
                        celular = doc.getString("celular") ?: "",
                        correo = doc.getString("email") ?: "",
                        celularEmergencia = doc.getString("celularEmergencia"),
                        fotoPerfilBase64 = doc.getString("fotoPerfilBase64"),
                        esAdministrador = doc.getBoolean("esAdministrador") ?: false
                    )
                    _usuario.value = usuario

                    // Cargar estado relación para este usuario
                    cargarEstadoRelacion(uid)
                } else {
                    _mensaje.value = "Usuario no encontrado"
                    _cerrarPantalla.value = true
                }
            }
            .addOnFailureListener {
                _mensaje.value = "Error al cargar usuario"
                _cerrarPantalla.value = true
            }
    }

    fun cargarEstadoRelacion(uidPerfil: String) {
        val uidActual = auth.currentUser?.uid ?: return

        solicitudListener?.remove()
        solicitudListener = db.collection("solicitudes")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _estadoRelacion.value = "no_disponible"
                    return@addSnapshotListener
                }

                val soyAdmin = !(auth.currentUser == null || _usuario.value?.esAdministrador == true)
                var estadoFinal = "agregar"
                var solicitudEntreAmbos: String? = null

                var yoTengoVinculo = false
                var yoTengoPendiente = false
                var elPerfilTieneVinculo = false
                var elPerfilTienePendiente = false

                for (doc in snapshot.documents) {
                    val emisor = doc.getString("emisorId") ?: continue
                    val receptor = doc.getString("receptorId") ?: continue
                    val estadoSol = doc.getString("estado") ?: "pendiente"

                    val entreAmbos = (emisor == uidPerfil && receptor == uidActual) ||
                            (receptor == uidPerfil && emisor == uidActual)

                    // 🔄 Caso de solicitud entre ambos
                    if (entreAmbos) {
                        solicitudEntreAmbos = when {
                            estadoSol == "aceptado" -> "vinculado"
                            emisor == uidActual && estadoSol == "pendiente" -> "pendiente"
                            receptor == uidActual && estadoSol == "pendiente" -> "recibida"
                            else -> "agregar"
                        }
                        continue
                    }

                    // 🔍 Verificar otras relaciones del perfil
                    if (emisor == uidPerfil || receptor == uidPerfil) {
                        if (estadoSol == "aceptado") elPerfilTieneVinculo = true
                        if (estadoSol == "pendiente") elPerfilTienePendiente = true
                    }

                    // 🔍 Verificar otras relaciones del usuario actual
                    if (emisor == uidActual || receptor == uidActual) {
                        if (estadoSol == "aceptado") yoTengoVinculo = true
                        if (estadoSol == "pendiente") yoTengoPendiente = true
                    }
                }

                // 🔚 Determinar el estado final
                if (solicitudEntreAmbos != null) {
                    estadoFinal = solicitudEntreAmbos
                } else {
                    estadoFinal = when {
                        !soyAdmin && (yoTengoVinculo || yoTengoPendiente) -> "no_disponible"
                        soyAdmin && (elPerfilTieneVinculo || elPerfilTienePendiente) -> "no_disponible"
                        else -> "agregar"
                    }
                }

                _estadoRelacion.value = estadoFinal
            }
    }

    fun enviarSolicitud(uidPerfil: String) {
        val uidActual = auth.currentUser?.uid ?: return
        val solicitud = hashMapOf(
            "emisorId" to uidActual,
            "receptorId" to uidPerfil,
            "estado" to "pendiente"
        )
        db.collection("solicitudes").add(solicitud)
            .addOnSuccessListener {
                _mensaje.value = "Solicitud enviada"
            }
            .addOnFailureListener {
                _mensaje.value = "Error al enviar solicitud"
            }
    }

    fun cancelarSolicitud(uidPerfil: String) {
        val uidActual = auth.currentUser?.uid ?: return
        db.collection("solicitudes")
            .whereEqualTo("emisorId", uidActual)
            .whereEqualTo("receptorId", uidPerfil)
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    db.collection("solicitudes").document(doc.id).delete()
                }
                _mensaje.value = "Solicitud cancelada"
            }
            .addOnFailureListener {
                _mensaje.value = "Error al cancelar solicitud"
            }
    }

    fun aceptarSolicitud(uidEmisor: String) {
        val uidActual = auth.currentUser?.uid ?: return
        db.collection("solicitudes")
            .whereEqualTo("emisorId", uidEmisor)
            .whereEqualTo("receptorId", uidActual)
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    db.collection("solicitudes").document(doc.id).update("estado", "aceptado")
                }
                _mensaje.value = "Solicitud aceptada"
            }
            .addOnFailureListener {
                _mensaje.value = "Error al aceptar solicitud"
            }
    }

    override fun onCleared() {
        super.onCleared()
        solicitudListener?.remove()
    }
}