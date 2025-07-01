package com.example.utpsalud.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
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
                    correo = document.getString("correo") ?: user.email ?: "Correo no disponible",
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

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }
}