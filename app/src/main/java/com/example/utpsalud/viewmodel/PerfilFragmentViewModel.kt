package com.example.utpsalud.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class PerfilFragmentViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _userInfo = MutableLiveData<UserUIState>()
    val userInfo: LiveData<UserUIState> get() = _userInfo

    private val _fotoActualizada = MutableLiveData<Boolean>()
    val fotoActualizada: LiveData<Boolean> get() = _fotoActualizada

    fun cargarDatosUsuario() {
        val user = auth.currentUser ?: return
        val userId = user.uid
        val userDocRef = db.collection("usuarios").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val datos = UserUIState(
                    nombre = document.getString("nombre") ?: "Nombre no disponible",
                    apellido = document.getString("apellido") ?: "Apellido no disponible",
                    correo = document.getString("email") ?: user.email ?: "Correo no disponible",
                    dni = document.getString("dni") ?: "DNI no disponible",
                    celular = document.getString("celular") ?: "Celular no disponible",
                    celularEmergencia = document.getString("celularEmergencia") ?: "Contacto no disponible",
                    fotoBase64 = document.getString("fotoPerfilBase64"),
                    esAdmin = document.getBoolean("esAdministrador") ?: false
                )
                _userInfo.value = datos
            } else {
                _userInfo.value = UserUIState(
                    nombre = "Nombre no disponible",
                    apellido = "Apellido no disponible",
                    correo = user.email ?: "Correo no disponible",
                    dni = "DNI no disponible",
                    celular = "Celular no disponible",
                    celularEmergencia = "Contacto no disponible",
                    fotoBase64 = null,
                    esAdmin = false
                )
            }
        }.addOnFailureListener {
            _userInfo.value = UserUIState(
                nombre = "Error al cargar",
                apellido = "Error al cargar",
                correo = user.email ?: "Correo no disponible",
                dni = "Error al cargar",
                celular = "Error al cargar",
                celularEmergencia = "Error al cargar",
                fotoBase64 = null,
                esAdmin = false
            )
        }
    }

    fun actualizarFoto(bitmap: Bitmap) {
        val uid = auth.currentUser?.uid ?: return
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val imageBytes = stream.toByteArray()
        val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)

        db.collection("usuarios").document(uid)
            .update("fotoPerfilBase64", base64Image)
            .addOnSuccessListener {
                _fotoActualizada.value = true
                cargarDatosUsuario() // Refresca los datos
            }
            .addOnFailureListener {
                _fotoActualizada.value = false
            }
    }

    data class UserUIState(
        val nombre: String,
        val apellido: String,
        val correo: String,
        val dni: String,
        val celular: String,
        val celularEmergencia: String,
        val fotoBase64: String?,
        val esAdmin: Boolean
    )

    private val _cuentaEliminada = MutableLiveData<Boolean>()
    val cuentaEliminada: LiveData<Boolean> get() = _cuentaEliminada

    fun eliminarCuenta() {
        val user = auth.currentUser ?: return
        val userId = user.uid
        _eliminacionEstado.value = EliminacionEstado.Cargando

        eliminarSubcoleccion(userId, "mediciones") {
            eliminarSugerenciasDelUsuario(userId) {
                eliminarSolicitudesDelUsuario(userId) {
                    eliminarChatsDelUsuario(userId) {
                        db.collection("usuarios").document(userId).delete()
                            .addOnSuccessListener {
                                user.delete()
                                    .addOnSuccessListener {
                                        _eliminacionEstado.value = EliminacionEstado.Exito
                                    }
                                    .addOnFailureListener {
                                        _eliminacionEstado.value = EliminacionEstado.Error("No se pudo eliminar la cuenta de autenticación")
                                    }
                            }
                            .addOnFailureListener {
                                _eliminacionEstado.value = EliminacionEstado.Error("No se pudo eliminar los datos del usuario")
                            }
                    }
                }
            }
        }
    }

    fun desactivarCuenta() {
        val userId = auth.currentUser?.uid ?: return
        _eliminacionEstado.value = EliminacionEstado.Cargando

        db.collection("usuarios").document(userId)
            .update("activo", false)
            .addOnSuccessListener {
                _eliminacionEstado.value = EliminacionEstado.Exito
            }
            .addOnFailureListener {
                _eliminacionEstado.value = EliminacionEstado.Error("No se pudo desactivar la cuenta")
            }
    }

    private fun eliminarChatsDelUsuario(userId: String, onComplete: () -> Unit) {
        val chatsRef = db.collection("chats")

        chatsRef.get().addOnSuccessListener { snapshot ->
            val chatsAEliminar = snapshot.documents.filter { doc ->
                doc.id.contains(userId)
            }

            if (chatsAEliminar.isEmpty()) {
                onComplete() // Nada que eliminar
                return@addOnSuccessListener
            }

            val lotesPendientes = mutableListOf<() -> Unit>()
            var completados = 0

            chatsAEliminar.forEach { chatDoc ->
                // Paso 1: eliminar subcolección 'mensajes'
                eliminarSubcoleccionChat(chatDoc.reference, "mensajes") {
                    // Paso 2: eliminar el documento de chat
                    chatDoc.reference.delete()
                        .addOnSuccessListener {
                            completados++
                            if (completados == chatsAEliminar.size) {
                                onComplete()
                            }
                        }
                        .addOnFailureListener {
                            _eliminacionEstado.value = EliminacionEstado.Error("Error al eliminar el chat ${chatDoc.id}")
                        }
                }
            }
        }.addOnFailureListener {
            _eliminacionEstado.value = EliminacionEstado.Error("Error al buscar los chats del usuario")
        }
    }

    private fun eliminarSubcoleccionChat(
        docRef: DocumentReference,
        subcoleccion: String,
        onComplete: () -> Unit
    ) {
        docRef.collection(subcoleccion)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    onComplete() // No hay nada que eliminar
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                for (doc in snap.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit()
                    .addOnSuccessListener {
                        // ✅ Solo después de que se borren los mensajes se ejecuta onComplete
                        onComplete()
                    }
                    .addOnFailureListener {
                        _eliminacionEstado.value = EliminacionEstado.Error("Error al eliminar subcolección $subcoleccion")
                    }
            }
            .addOnFailureListener {
                _eliminacionEstado.value = EliminacionEstado.Error("Error al leer subcolección $subcoleccion")
            }
    }

    private fun eliminarSolicitudesDelUsuario(
        userId: String,
        onComplete: () -> Unit
    ) {
        // Acumularemos las refs que hay que borrar
        val refsAEliminar = mutableListOf<com.google.firebase.firestore.DocumentReference>()

        // 1. Solicitudes enviadas por el usuario
        db.collection("solicitudes")
            .whereEqualTo("emisorId", userId)
            .get()
            .addOnSuccessListener { snap1 ->
                refsAEliminar += snap1.documents.map { it.reference }

                // 2. Solicitudes recibidas por el usuario
                db.collection("solicitudes")
                    .whereEqualTo("receptorId", userId)
                    .get()
                    .addOnSuccessListener { snap2 ->
                        refsAEliminar += snap2.documents.map { it.reference }

                        // 🔄 Dividir en lotes de a 500 (límite de Firestore)
                        val lotes = refsAEliminar.chunked(500)
                        eliminarLotesRecursivos(lotes, onComplete)
                    }
                    .addOnFailureListener {
                        _eliminacionEstado.value =
                            EliminacionEstado.Error("Error al buscar solicitudes recibidas")
                    }
            }
            .addOnFailureListener {
                _eliminacionEstado.value =
                    EliminacionEstado.Error("Error al buscar solicitudes enviadas")
            }
    }

    /* Borra los lotes uno tras otro para no exceder 500 ops por commit */
    private fun eliminarLotesRecursivos(
        lotes: List<List<com.google.firebase.firestore.DocumentReference>>,
        onComplete: () -> Unit,
        indice: Int = 0
    ) {
        if (indice >= lotes.size) {
            onComplete()   // ✔️ Terminamos
            return
        }
        val batch = db.batch()
        for (ref in lotes[indice]) batch.delete(ref)
        batch.commit()
            .addOnSuccessListener { eliminarLotesRecursivos(lotes, onComplete, indice + 1) }
            .addOnFailureListener {
                _eliminacionEstado.value =
                    EliminacionEstado.Error("Error al eliminar algunas solicitudes")
            }
    }

    private fun eliminarSugerenciasDelUsuario(userId: String, onComplete: () -> Unit) {
        db.collection("sugerencias")
            .whereEqualTo("uid", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (document in snapshot.documents) {
                    batch.delete(document.reference)
                }
                batch.commit()
                    .addOnSuccessListener { onComplete() }
                    .addOnFailureListener {
                        _eliminacionEstado.value = EliminacionEstado.Error("Error al eliminar sugerencias del usuario")
                    }
            }
            .addOnFailureListener {
                _eliminacionEstado.value = EliminacionEstado.Error("Error al acceder a sugerencias del usuario")
            }
    }

    private fun eliminarSubcoleccion(
        userId: String,
        subcoleccion: String,
        onComplete: () -> Unit
    ) {
        val subcollectionRef = db.collection("usuarios").document(userId).collection(subcoleccion)
        subcollectionRef.get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (document in snapshot) {
                    batch.delete(document.reference)
                }
                batch.commit().addOnSuccessListener {
                    onComplete()
                }.addOnFailureListener {
                    _eliminacionEstado.value = EliminacionEstado.Error("Error al eliminar subcolección $subcoleccion")
                }
            }
            .addOnFailureListener {
                _eliminacionEstado.value = EliminacionEstado.Error("Error al acceder a subcolección $subcoleccion")
            }
    }

    sealed class EliminacionEstado {
        object Cargando : EliminacionEstado()
        object Exito : EliminacionEstado()
        data class Error(val mensaje: String) : EliminacionEstado()
    }

    private val _eliminacionEstado = MutableLiveData<EliminacionEstado>()
    val eliminacionEstado: LiveData<EliminacionEstado> get() = _eliminacionEstado

    fun enviarSugerencia(mensaje: String, onResult: (Boolean) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                val nombreCompleto = doc.getString("nombre") + " " + doc.getString("apellido")

                val sugerencia = hashMapOf(
                    "uid" to uid,
                    "nombre" to nombreCompleto,
                    "mensaje" to mensaje,
                    "timestamp" to System.currentTimeMillis()
                )

                db.collection("sugerencias")
                    .add(sugerencia)
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(false) }
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }
}