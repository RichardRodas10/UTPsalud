package com.example.utpsalud.viewmodel

import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class RegisterViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _registroEstado = MutableLiveData<RegistroEstado>()
    val registroEstado: LiveData<RegistroEstado> = _registroEstado

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun validarYRegistrar(
        nombre: String,
        apellido: String,
        dni: String,
        celular: String,
        celularEmergencia: String,
        email: String,
        password: String,
        confirmPassword: String,
        esAdmin: Boolean,
        colegAdmin: String?,
        fechaEmisionStr: String?,
        fotoBase64: String?
    ) {
        // Validaciones básicas
        if (nombre.isBlank() || apellido.isBlank() || dni.isBlank() || celular.isBlank() || email.isBlank()
            || password.isBlank() || confirmPassword.isBlank()
        ) {
            _registroEstado.value = RegistroEstado.Error("Por favor, completa todos los campos")
            return
        }
        if (password != confirmPassword) {
            _registroEstado.value = RegistroEstado.Error("Las contraseñas no coinciden")
            return
        }
        if (!celular.matches(Regex("^9\\d{8}$"))) {
            _registroEstado.value = RegistroEstado.Error("El celular debe empezar con 9 y tener 9 dígitos")
            return
        }
        if (!esAdmin && celularEmergencia.isBlank()) {
            _registroEstado.value = RegistroEstado.Error("Ingresa el contacto de emergencia")
            return
        }
        if (!esAdmin && !celularEmergencia.matches(Regex("^9\\d{8}$"))) {
            _registroEstado.value = RegistroEstado.Error("El contacto de emergencia debe empezar con 9 y tener 9 dígitos")
            return
        }
        if (!esAdmin && celularEmergencia == celular) {
            _registroEstado.value = RegistroEstado.Error("El contacto de emergencia debe ser diferente al celular personal")
            return
        }
        if (dni.length != 8) {
            _registroEstado.value = RegistroEstado.Error("El DNI debe tener 8 dígitos")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _registroEstado.value = RegistroEstado.Error("El correo electrónico no tiene un formato válido")
            return
        }
        val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d])[A-Za-z\\d\\W]{6,}$")
        if (!passwordRegex.matches(password)) {
            _registroEstado.value = RegistroEstado.Error(
                "La contraseña debe tener mínimo 6 caracteres, una mayúscula, una minúscula, un número y un símbolo especial"
            )
            return
        }

        if (esAdmin) {
            if (colegAdmin == null || colegAdmin.length != 10) {
                _registroEstado.value = RegistroEstado.Error("Ingresa un N° de colegiatura de 10 dígitos")
                return
            }
            if (fechaEmisionStr.isNullOrBlank()) {
                _registroEstado.value = RegistroEstado.Error("Ingresa la fecha de emisión")
                return
            }
            val fechaEmisionDate: Date = try {
                dateFormat.parse(fechaEmisionStr)!!
            } catch (e: Exception) {
                _registroEstado.value = RegistroEstado.Error("Formato de fecha inválido")
                return
            }
            if (fechaEmisionDate.after(Calendar.getInstance().time)) {
                _registroEstado.value = RegistroEstado.Error("La fecha de emisión no puede ser en el futuro")
                return
            }
        }

        _registroEstado.value = RegistroEstado.Loading

        // Aquí empieza la verificación en Firestore: DNI, celular, y ahora colegiatura para admin
        verificarDniYCelularYColegiaturaYCrear(
            nombre,
            apellido,
            dni,
            celular,
            celularEmergencia,
            email,
            password,
            esAdmin,
            colegAdmin,
            fechaEmisionStr,
            fotoBase64
        )
    }

    private fun verificarDniYCelularYColegiaturaYCrear(
        nombre: String,
        apellido: String,
        dni: String,
        celular: String,
        celularEmergencia: String,
        email: String,
        password: String,
        esAdmin: Boolean,
        colegAdmin: String?,
        fechaEmisionStr: String?,
        fotoBase64: String?
    ) {
        db.collection("usuarios").whereEqualTo("dni", dni)
            .get()
            .addOnSuccessListener { dniResult ->
                if (!dniResult.isEmpty) {
                    _registroEstado.value = RegistroEstado.Error("Este DNI ya está registrado.")
                    return@addOnSuccessListener
                }

                db.collection("usuarios").whereEqualTo("celular", celular)
                    .get()
                    .addOnSuccessListener { celularResult ->
                        if (!celularResult.isEmpty) {
                            _registroEstado.value = RegistroEstado.Error("Este número de celular ya está registrado.")
                            return@addOnSuccessListener
                        }

                        if (esAdmin) {
                            db.collection("usuarios").whereEqualTo("nColegiatura", colegAdmin)
                                .get()
                                .addOnSuccessListener { colegResult ->
                                    if (!colegResult.isEmpty) {
                                        _registroEstado.value = RegistroEstado.Error("Este número de colegiatura ya está registrado.")
                                        return@addOnSuccessListener
                                    }
                                    // Si colegiatura OK, creamos usuario
                                    crearUsuarioAuthYFirestore(
                                        nombre,
                                        apellido,
                                        dni,
                                        celular,
                                        celularEmergencia,
                                        email,
                                        password,
                                        esAdmin,
                                        colegAdmin,
                                        fechaEmisionStr,
                                        fotoBase64
                                    )
                                }
                                .addOnFailureListener {
                                    _registroEstado.value = RegistroEstado.Error("Error al verificar colegiatura: ${it.message}")
                                }
                        } else {
                            // No es admin, creamos usuario directamente
                            crearUsuarioAuthYFirestore(
                                nombre,
                                apellido,
                                dni,
                                celular,
                                celularEmergencia,
                                email,
                                password,
                                esAdmin,
                                colegAdmin,
                                fechaEmisionStr,
                                fotoBase64
                            )
                        }
                    }
                    .addOnFailureListener {
                        _registroEstado.value = RegistroEstado.Error("Error al verificar celular: ${it.message}")
                    }
            }
            .addOnFailureListener {
                _registroEstado.value = RegistroEstado.Error("Error al verificar DNI: ${it.message}")
            }
    }

    private fun crearUsuarioAuthYFirestore(
        nombre: String,
        apellido: String,
        dni: String,
        celular: String,
        celularEmergencia: String,
        email: String,
        password: String,
        esAdmin: Boolean,
        colegAdmin: String?,
        fechaEmisionStr: String?,
        fotoBase64: String?
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser!!
                    val userData = hashMapOf(
                        "nombre" to nombre,
                        "apellido" to apellido,
                        "dni" to dni,
                        "celular" to celular,
                        "email" to email,
                        "esAdministrador" to esAdmin,
                        "fotoPerfilBase64" to (fotoBase64 ?: "")
                    ).apply {
                        if (!esAdmin) {
                            put("celularEmergencia", celularEmergencia)
                        }
                        if (esAdmin) {
                            put("nColegiatura", colegAdmin ?: "")
                            put("fechaEmision", fechaEmisionStr ?: "")
                        }
                    }

                    db.collection("usuarios").document(firebaseUser.uid)
                        .set(userData)
                        .addOnSuccessListener {
                            _registroEstado.value = RegistroEstado.Success
                        }
                        .addOnFailureListener {
                            _registroEstado.value = RegistroEstado.Error("Error al guardar datos: ${it.message}")
                        }
                } else {
                    val ex = task.exception
                    if (ex is FirebaseAuthUserCollisionException) {
                        _registroEstado.value = RegistroEstado.Error("Este correo ya está registrado.")
                    } else {
                        _registroEstado.value = RegistroEstado.Error("Error en el registro: ${ex?.message}")
                    }
                }
            }
    }

    sealed class RegistroEstado {
        object Success : RegistroEstado()
        data class Error(val mensaje: String) : RegistroEstado()
        object Loading : RegistroEstado()
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val resized = Bitmap.createScaledBitmap(bitmap, 300, 300, true)
        val baos = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }
}