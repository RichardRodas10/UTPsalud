package com.example.utpsalud.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.utpsalud.model.ChatMessage
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

    private var esAdminActual: Boolean = false

    fun cargarContactos() {
        _mostrarLoading.value = true
        _mostrarSinResultados.value = false

        db.collection("usuarios").document(uidActual).get()
            .addOnSuccessListener { userSnapshot ->
                esAdminActual = userSnapshot.getBoolean("esAdministrador") == true
                consultarContactos()
            }
            .addOnFailureListener {
                _mostrarLoading.value = false
                _mostrarSinResultados.value = true
            }
    }

    private fun consultarContactos() {
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
    }

    private fun actualizarUsuarios(ids: Set<String>) {
        if (ids.isEmpty()) {
            _listaContactos.value = emptyList()
            _mostrarLoading.value = false
            _mostrarSinResultados.value = true
            return
        }

        val usuarios = mutableListOf<Usuario>()
        val usuariosRef = db.collection("usuarios")

        usuariosRef.whereIn(FieldPath.documentId(), ids.toList()).get()
            .addOnSuccessListener { snapshot ->
                val docs = snapshot.documents
                var completados = 0

                docs.forEach { doc ->
                    val usuario = Usuario(
                        uid = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        apellido = doc.getString("apellido") ?: "",
                        fotoPerfilBase64 = doc.getString("fotoPerfilBase64"),
                        esAdministrador = doc.getBoolean("esAdministrador") ?: false
                    )

                    val chatId = generarChatId(uidActual, usuario.uid)

                    db.collection("chats").document(chatId)
                        .collection("mensajes")
                        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { msgSnapshot ->
                            if (!msgSnapshot.isEmpty) {
                                val ultimo = msgSnapshot.documents.first().toObject(ChatMessage::class.java)
                                usuario.ultimoMensaje = if (ultimo?.emisorId == uidActual) "Tú: ${ultimo.mensaje}" else ultimo?.mensaje
                                usuario.timestampUltimoMensaje = ultimo?.timestamp
                            }
                            usuarios.add(usuario)
                            completados++
                            if (completados == docs.size) {
                                _listaContactos.value = usuarios.sortedByDescending { it.timestampUltimoMensaje ?: 0L }
                                _mostrarLoading.value = false
                                _mostrarSinResultados.value = usuarios.isEmpty()
                            }
                        }
                }
            }
    }

    fun obtenerRolUsuario(callback: (Boolean) -> Unit) {
        callback(esAdminActual)
    }

    private val _mensajes = MutableLiveData<List<ChatMessage>>()
    val mensajes: LiveData<List<ChatMessage>> get() = _mensajes

    private var mensajesListener: ListenerRegistration? = null

    fun escucharMensajes(receptorId: String) {
        val chatId = generarChatId(uidActual, receptorId)
        mensajesListener?.remove() // Limpiar escucha previa si la hay

        mensajesListener = db.collection("chats")
            .document(chatId)
            .collection("mensajes")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _mensajes.value = emptyList()
                    return@addSnapshotListener
                }

                val listaMensajes = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)
                }
                _mensajes.value = listaMensajes
            }
    }

    fun enviarMensaje(receptorId: String, mensaje: String) {
        if (mensaje.isBlank()) return

        val chatId = generarChatId(uidActual, receptorId)
        val mensajeObj = ChatMessage(
            emisorId = uidActual,
            receptorId = receptorId,
            mensaje = mensaje,
            timestamp = System.currentTimeMillis()
        )

        db.collection("chats")
            .document(chatId)
            .collection("mensajes")
            .add(mensajeObj)
    }

    private fun generarChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_$uid2" else "${uid2}_$uid1"
    }

    override fun onCleared() {
        super.onCleared()
        solicitudesListener1?.remove()
        solicitudesListener2?.remove()
        usuariosListener?.remove()
    }
}