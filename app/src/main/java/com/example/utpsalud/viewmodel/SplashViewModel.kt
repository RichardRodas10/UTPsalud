package com.example.utpsalud.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashViewModel : ViewModel() {

    // Instancio FirebaseAuth para manejar usuario actual y Firestore para datos adicionales
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // MutableLiveData privada para guardar si el usuario es admin o no, expuesta como LiveData pública
    private val _usuarioEsAdmin = MutableLiveData<Boolean?>()
    val usuarioEsAdmin: LiveData<Boolean?> = _usuarioEsAdmin

    // Función que se llama para verificar si el usuario actual está logueado y si es admin
    fun verificarUsuario() {
        val currentUser = auth.currentUser  // Obtengo el usuario actual de Firebase Auth
        if (currentUser != null) {
            // Si hay usuario logueado, obtengo su uid
            val uid = currentUser.uid

            // Consulto Firestore en la colección "usuarios" para obtener datos extra (si es admin)
            db.collection("usuarios").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        // Si existe el documento, saco el campo esAdministrador
                        val esAdmin = doc.getBoolean("esAdministrador") ?: false
                        // Actualizo el LiveData con el resultado (postValue porque puede venir de otro thread)
                        _usuarioEsAdmin.postValue(esAdmin)
                    } else {
                        // Si no existe el documento, pongo null para indicar problema o usuario no encontrado
                        _usuarioEsAdmin.postValue(null)
                    }
                }
                .addOnFailureListener {
                    // Si falla la consulta, pongo null para que la UI sepa que hubo un error
                    _usuarioEsAdmin.postValue(null)
                }
        } else {
            // Si no hay usuario logueado, directamente pongo null
            _usuarioEsAdmin.postValue(null)
        }
    }
}