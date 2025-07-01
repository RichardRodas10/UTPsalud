package com.example.utpsalud.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.utpsalud.model.UsuarioEditable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditardatosViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _usuarioEditable = MutableLiveData<UsuarioEditable>()
    val usuarioEditable: LiveData<UsuarioEditable> = _usuarioEditable

    private val _mensaje = MutableLiveData<String>()
    val mensaje: LiveData<String> = _mensaje

    private val _finalizar = MutableLiveData<Boolean>()
    val finalizar: LiveData<Boolean> = _finalizar

    fun cargarUsuario() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val editable = UsuarioEditable(
                        celular = document.getString("celular") ?: "",
                        correo = document.getString("correo") ?: user.email ?: "",
                        celularEmergencia = document.getString("celularEmergencia") ?: "",
                        esAdministrador = document.getBoolean("esAdministrador") ?: false
                    )
                    _usuarioEditable.value = editable
                } else {
                    _mensaje.value = "Datos no encontrados"
                }
            }
            .addOnFailureListener {
                _mensaje.value = "Error al cargar los datos"
            }
    }

    fun actualizarUsuario(nuevo: UsuarioEditable) {
        val user = auth.currentUser
        if (user == null) {
            _mensaje.value = "Usuario no autenticado"
            return
        }

        // Validaciones
        if (nuevo.celular.length != 9 || !nuevo.celular.startsWith("9")) {
            _mensaje.value = "El número de celular debe tener 9 dígitos y empezar con 9"
            return
        }

        if (!nuevo.esAdministrador) {
            if (nuevo.celularEmergencia.length != 9 || !nuevo.celularEmergencia.startsWith("9")) {
                _mensaje.value = "El número de emergencia debe tener 9 dígitos y empezar con 9"
                return
            }

            if (nuevo.celularEmergencia == nuevo.celular) {
                _mensaje.value = "El contacto de emergencia debe ser distinto al celular personal"
                return
            }
        }

        if (nuevo.correo.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(nuevo.correo).matches()) {
            _mensaje.value = "Correo inválido"
            return
        }

        // Verificar si celular ya existe para otro usuario
        db.collection("usuarios")
            .whereEqualTo("celular", nuevo.celular)
            .get()
            .addOnSuccessListener { documents ->
                val celularUsadoPorOtro = documents.any { it.id != user.uid }

                if (celularUsadoPorOtro) {
                    _mensaje.value = "Este número de celular ya está en uso por otro usuario"
                } else {
                    val datosActualizados = if (nuevo.esAdministrador) {
                        mapOf(
                            "celular" to nuevo.celular,
                            "correo" to nuevo.correo
                        )
                    } else {
                        mapOf(
                            "celular" to nuevo.celular,
                            "correo" to nuevo.correo,
                            "celularEmergencia" to nuevo.celularEmergencia
                        )
                    }

                    db.collection("usuarios").document(user.uid)
                        .update(datosActualizados)
                        .addOnSuccessListener {
                            _mensaje.value = "Datos actualizados correctamente"
                            _finalizar.value = true
                        }
                        .addOnFailureListener {
                            _mensaje.value = "Error al actualizar los datos"
                        }
                }
            }
            .addOnFailureListener {
                _mensaje.value = "Error al verificar el número de celular"
            }
    }

    fun detectarCambios(
        original: UsuarioEditable,
        nuevo: UsuarioEditable
    ): Boolean {
        return original.celular != nuevo.celular ||
                original.correo != nuevo.correo ||
                original.celularEmergencia != nuevo.celularEmergencia
    }

}