package com.example.utpsalud.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.utpsalud.model.UsuarioPerfil
import com.google.firebase.firestore.FirebaseFirestore

class PerfilViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _usuario = MutableLiveData<UsuarioPerfil>()
    val usuario: LiveData<UsuarioPerfil> = _usuario

    private val _mensaje = MutableLiveData<String>()
    val mensaje: LiveData<String> = _mensaje

    private val _cerrarPantalla = MutableLiveData<Boolean>()
    val cerrarPantalla: LiveData<Boolean> = _cerrarPantalla

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
}