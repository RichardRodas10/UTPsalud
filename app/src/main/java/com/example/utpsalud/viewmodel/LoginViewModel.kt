package com.example.utpsalud.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginViewModel : ViewModel() {

    // Instancio Firebase Auth y Firestore para usarlos en el login
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // LiveData privada para controlar el estado del login y exponer solo lectura
    private val _loginEstado = MutableLiveData<LoginEstado>()
    val loginEstado: LiveData<LoginEstado> = _loginEstado

    // Función que me sirve para iniciar sesión con email y password
    fun login(email: String, password: String) {
        // Primero verifico que no estén vacíos los campos
        if (email.isBlank() || password.isBlank()) {
            _loginEstado.value = LoginEstado.Error("Completa los campos")
            return
        }

        // Emito estado de carga para que la UI desactive el botón mientras intenta loguear
        _loginEstado.value = LoginEstado.Loading

        // Intento autenticar con Firebase Auth
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Si logré hacer login, obtengo el uid del usuario actual
                    val uid = auth.currentUser!!.uid

                    // Con uid voy a Firestore a buscar si existe ese usuario y traer info extra
                    db.collection("usuarios").document(uid).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                // Si el documento existe, leo si es admin o no
                                val esAdmin = doc.getBoolean("esAdministrador") ?: false
                                // Aviso que el login fue exitoso y envío si es admin o no
                                _loginEstado.value = LoginEstado.Success(esAdmin)
                            } else {
                                // Si el documento no existe, muestro error
                                _loginEstado.value = LoginEstado.Error("Datos de usuario no encontrados")
                            }
                        }
                        .addOnFailureListener { e ->
                            // Si hay error leyendo Firestore, lo muestro
                            _loginEstado.value = LoginEstado.Error("Error al leer datos: ${e.message}")
                        }
                } else {
                    // Si falla login (email/pass incorrectos), aviso error
                    _loginEstado.value = LoginEstado.Error("Credenciales incorrectas")
                }
            }
    }

    // Clase sellada para manejar los estados del login
    sealed class LoginEstado {
        data class Success(val esAdmin: Boolean) : LoginEstado()  // Login OK con info admin
        data class Error(val mensaje: String) : LoginEstado()     // Login fallido con mensaje
        object Loading : LoginEstado()                             // Estado mientras se está procesando login
    }
}