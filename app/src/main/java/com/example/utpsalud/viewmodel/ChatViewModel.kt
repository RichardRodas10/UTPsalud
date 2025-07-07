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

    private val chatListeners = mutableMapOf<String, ListenerRegistration>()

    private var esAdminActual: Boolean = false

    fun cargarContactos() {
        _mostrarLoading.value = true
        _mostrarSinResultados.value = false

        db.collection("usuarios").document(uidActual).get()
            .addOnSuccessListener { userSnapshot ->
                esAdminActual = userSnapshot.getBoolean("esAdministrador") == true
                consultarContactosTiempoReal()
            }
            .addOnFailureListener {
                _mostrarLoading.value = false
                _mostrarSinResultados.value = true
            }
    }

    private fun consultarContactosTiempoReal() {
        val solicitudesRef = db.collection("solicitudes")
        val idsRelacionados = mutableSetOf<String>()

        solicitudesListener1 = solicitudesRef
            .whereEqualTo("estado", "aceptado")
            .whereEqualTo("emisorId", uidActual)
            .addSnapshotListener { snapshot1, e1 ->
                if (e1 != null) return@addSnapshotListener

                snapshot1?.forEach { doc ->
                    doc.getString("receptorId")?.let { idsRelacionados.add(it) }
                }

                solicitudesListener2 = solicitudesRef
                    .whereEqualTo("estado", "aceptado")
                    .whereEqualTo("receptorId", uidActual)
                    .addSnapshotListener { snapshot2, e2 ->
                        if (e2 != null) return@addSnapshotListener

                        snapshot2?.forEach { doc ->
                            doc.getString("emisorId")?.let { idsRelacionados.add(it) }
                        }

                        actualizarUsuariosEnTiempoReal(idsRelacionados)
                    }
            }
    }

    private fun actualizarUsuariosEnTiempoReal(ids: Set<String>) {
        if (ids.isEmpty()) {
            _listaContactos.value = emptyList()
            _mostrarLoading.value = false
            _mostrarSinResultados.value = true
            return
        }

        val usuariosRef = db.collection("usuarios")
        usuariosListener?.remove()

        usuariosListener = usuariosRef
            .whereIn(FieldPath.documentId(), ids.toList())
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _mostrarLoading.value = false
                    _mostrarSinResultados.value = true
                    return@addSnapshotListener
                }

                val usuariosTemporales = mutableListOf<Usuario>()

                snapshot.documents.forEach { doc ->
                    val usuario = Usuario(
                        uid = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        apellido = doc.getString("apellido") ?: "",
                        fotoPerfilBase64 = doc.getString("fotoPerfilBase64"),
                        esAdministrador = doc.getBoolean("esAdministrador") ?: false
                    )

                    val chatId = generarChatId(uidActual, usuario.uid)

                    // Remueve listener anterior si existía
                    chatListeners[chatId]?.remove()

                    // Agrega nuevo listener de mensajes en tiempo real
                    val chatListener = db.collection("chats")
                        .document(chatId)
                        .collection("mensajes")
                        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(1)
                        .addSnapshotListener { msgSnapshot, errorMsg ->
                            if (errorMsg != null || msgSnapshot == null) return@addSnapshotListener

                            val ultimo = msgSnapshot.documents.firstOrNull()?.toObject(ChatMessage::class.java)
                            usuario.ultimoMensaje = if (ultimo?.emisorId == uidActual) "Tú: ${ultimo.mensaje}" else ultimo?.mensaje
                            usuario.timestampUltimoMensaje = ultimo?.timestamp

                            val index = usuariosTemporales.indexOfFirst { it.uid == usuario.uid }
                            if (index >= 0) {
                                usuariosTemporales[index] = usuario
                            } else {
                                usuariosTemporales.add(usuario)
                            }

                            _listaContactos.postValue(
                                usuariosTemporales.sortedByDescending { it.timestampUltimoMensaje ?: 0L }
                            )
                        }

                    chatListeners[chatId] = chatListener
                }

                _mostrarLoading.value = false
                _mostrarSinResultados.value = usuariosTemporales.isEmpty()
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
        mensajesListener?.remove()

        mensajesListener = db.collection("chats")
            .document(chatId)
            .collection("mensajes")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _mensajes.value = emptyList()
                    return@addSnapshotListener
                }

                val listaMensajes = snapshot.documents.mapNotNull {
                    it.toObject(ChatMessage::class.java)
                }

                _mensajes.value = listaMensajes

                // 🔥 Marcar como leídos los mensajes que el usuario actual ha recibido y aún no están leídos
                snapshot.documents.forEach { doc ->
                    val mensaje = doc.toObject(ChatMessage::class.java)
                    if (mensaje != null &&
                        mensaje.receptorId == uidActual &&
                        mensaje.leido != true
                    ) {
                        doc.reference.update("leido", true)
                    }
                }
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

    fun marcarMensajesComoLeidos(receptorId: String) {
        val chatId = generarChatId(uidActual, receptorId)

        db.collection("chats")
            .document(chatId)
            .collection("mensajes")
            .whereEqualTo("receptorId", uidActual)
            .whereEqualTo("leido", false)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.update("leido", true)
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        solicitudesListener1?.remove()
        solicitudesListener2?.remove()
        usuariosListener?.remove()
        mensajesListener?.remove()
        chatListeners.values.forEach { it.remove() }
        chatListeners.clear()
    }
}