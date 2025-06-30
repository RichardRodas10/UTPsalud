package com.example.utpsalud.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _loginEstado = MutableLiveData<LoginEstado>()
    val loginEstado: LiveData<LoginEstado> = _loginEstado

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginEstado.value = LoginEstado.Error("Completa los campos")
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser!!.uid
                    db.collection("usuarios").document(uid).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                val esAdmin = doc.getBoolean("esAdministrador") ?: false
                                _loginEstado.value = LoginEstado.Success(esAdmin)
                            } else {
                                _loginEstado.value = LoginEstado.Error("Datos de usuario no encontrados")
                            }
                        }
                        .addOnFailureListener { e ->
                            _loginEstado.value = LoginEstado.Error("Error al leer datos: ${e.message}")
                        }
                } else {
                    _loginEstado.value = LoginEstado.Error("Credenciales incorrectas")
                }
            }
    }

    sealed class LoginEstado {
        data class Success(val esAdmin: Boolean) : LoginEstado()
        data class Error(val mensaje: String) : LoginEstado()
    }
}