package com.example.utpsalud.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _usuarioEsAdmin = MutableLiveData<Boolean?>()
    val usuarioEsAdmin: LiveData<Boolean?> = _usuarioEsAdmin

    fun verificarUsuario() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            db.collection("usuarios").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val esAdmin = doc.getBoolean("esAdministrador") ?: false
                        _usuarioEsAdmin.postValue(esAdmin)
                    } else {
                        _usuarioEsAdmin.postValue(null)
                    }
                }
                .addOnFailureListener {
                    _usuarioEsAdmin.postValue(null)
                }
        } else {
            _usuarioEsAdmin.postValue(null)
        }
    }
}