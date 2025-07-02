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

    // Instancio Firebase Auth y Firestore para usarlos durante el registro
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // LiveData privada para estado de registro y su exposicion pública
    private val _registroEstado = MutableLiveData<RegistroEstado>()
    val registroEstado: LiveData<RegistroEstado> = _registroEstado

    // Formateador de fecha para validar la fecha de emisión
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Esta función valida los campos y si todo está bien llama a crear usuario
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
        // Validaciones básicas de campos vacíos
        if (nombre.isBlank() || apellido.isBlank() || dni.isBlank() || celular.isBlank() || email.isBlank()
            || password.isBlank() || confirmPassword.isBlank()
        ) {
            _registroEstado.value = RegistroEstado.Error("Por favor, completa todos los campos")
            return
        }

        // Las contraseñas deben coincidir
        if (password != confirmPassword) {
            _registroEstado.value = RegistroEstado.Error("Las contraseñas no coinciden")
            return
        }

        // El celular debe empezar con 9 y tener 9 dígitos (validación específica para Perú)
        if (!celular.matches(Regex("^9\\d{8}$"))) {
            _registroEstado.value = RegistroEstado.Error("El celular debe empezar con 9 y tener 9 dígitos")
            return
        }

        // Si no es admin, el contacto de emergencia es obligatorio
        if (!esAdmin && celularEmergencia.isBlank()) {
            _registroEstado.value = RegistroEstado.Error("Ingresa el contacto de emergencia")
            return
        }

        // El contacto de emergencia también debe cumplir la misma validación
        if (!esAdmin && !celularEmergencia.matches(Regex("^9\\d{8}$"))) {
            _registroEstado.value = RegistroEstado.Error("El contacto de emergencia debe empezar con 9 y tener 9 dígitos")
            return
        }

        // El contacto de emergencia debe ser distinto al celular personal
        if (!esAdmin && celularEmergencia == celular) {
            _registroEstado.value = RegistroEstado.Error("El contacto de emergencia debe ser diferente al celular personal")
            return
        }

        // El DNI debe tener exactamente 8 dígitos
        if (dni.length != 8) {
            _registroEstado.value = RegistroEstado.Error("El DNI debe tener 8 dígitos")
            return
        }

        // Validar formato correcto de email
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _registroEstado.value = RegistroEstado.Error("El correo electrónico no tiene un formato válido")
            return
        }

        // Contraseña con requisitos de seguridad: mínimo 6 caracteres, mayúscula, minúscula, número y símbolo
        val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d])[A-Za-z\\d\\W]{6,}$")
        if (!passwordRegex.matches(password)) {
            _registroEstado.value = RegistroEstado.Error(
                "La contraseña debe tener mínimo 6 caracteres, una mayúscula, una minúscula, un número y un símbolo especial"
            )
            return
        }

        // Validaciones extras para admin
        if (esAdmin) {
            // N° de colegiatura debe tener 10 dígitos
            if (colegAdmin == null || colegAdmin.length != 10) {
                _registroEstado.value = RegistroEstado.Error("Ingresa un N° de colegiatura de 10 dígitos")
                return
            }

            // Fecha de emisión no puede estar vacía
            if (fechaEmisionStr.isNullOrBlank()) {
                _registroEstado.value = RegistroEstado.Error("Ingresa la fecha de emisión")
                return
            }

            // Parseo y valido que la fecha no esté en el futuro
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

        // Si llegamos hasta aquí, todo bien, muestro estado de carga
        _registroEstado.value = RegistroEstado.Loading

        // Ahora paso a verificar en Firestore que dni y celular no estén registrados antes de crear usuario
        verificarDniYCelularYCrear(
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

    // Función que consulta Firestore para validar dni y celular únicos antes de crear usuario en Auth y Firestore
    private fun verificarDniYCelularYCrear(
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
        // Primero verifico si dni ya está registrado
        db.collection("usuarios").whereEqualTo("dni", dni)
            .get()
            .addOnSuccessListener { dniResult ->
                if (!dniResult.isEmpty) {
                    _registroEstado.value = RegistroEstado.Error("Este DNI ya está registrado.")
                    return@addOnSuccessListener
                }

                // Después verifico si celular ya está registrado
                db.collection("usuarios").whereEqualTo("celular", celular)
                    .get()
                    .addOnSuccessListener { celularResult ->
                        if (!celularResult.isEmpty) {
                            _registroEstado.value = RegistroEstado.Error("Este número de celular ya está registrado.")
                            return@addOnSuccessListener
                        }

                        // Si todo bien, creo el usuario con email y password en Firebase Auth
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Si el registro en Auth fue exitoso, guardo más datos en Firestore
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
                                        // Campos que dependen si es admin o no
                                        if (!esAdmin) {
                                            put("celularEmergencia", celularEmergencia)
                                        }
                                        if (esAdmin) {
                                            put("nColegiatura", colegAdmin ?: "")
                                            put("fechaEmision", fechaEmisionStr ?: "")
                                        }
                                    }

                                    // Guardo los datos del usuario en Firestore
                                    db.collection("usuarios").document(firebaseUser.uid)
                                        .set(userData)
                                        .addOnSuccessListener {
                                            // Si todo va bien, notifico éxito
                                            _registroEstado.value = RegistroEstado.Success
                                        }
                                        .addOnFailureListener {
                                            // Si falla, notifico error con mensaje
                                            _registroEstado.value = RegistroEstado.Error("Error al guardar datos: ${it.message}")
                                        }
                                } else {
                                    // Si falla el registro en Auth, manejo la excepción
                                    val ex = task.exception
                                    if (ex is FirebaseAuthUserCollisionException) {
                                        _registroEstado.value = RegistroEstado.Error("Este correo ya está registrado.")
                                    } else {
                                        _registroEstado.value = RegistroEstado.Error("Error en el registro: ${ex?.message}")
                                    }
                                }
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

    // Clase sellada para manejar estados de registro: éxito, error y loading
    sealed class RegistroEstado {
        object Success : RegistroEstado()              // Registro completado exitosamente
        data class Error(val mensaje: String) : RegistroEstado() // Error con mensaje
        object Loading : RegistroEstado()              // Estado de carga mientras se procesa
    }

    // Función para convertir un Bitmap a Base64, para guardar la foto de perfil en Firestore
    fun bitmapToBase64(bitmap: Bitmap): String {
        // Redimensiono la imagen para optimizar almacenamiento
        val resized = Bitmap.createScaledBitmap(bitmap, 300, 300, true)
        val baos = ByteArrayOutputStream()
        // Comprimir a JPEG con calidad 80%
        resized.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        // Codifico el byte array a Base64 y retorno el string
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }
}