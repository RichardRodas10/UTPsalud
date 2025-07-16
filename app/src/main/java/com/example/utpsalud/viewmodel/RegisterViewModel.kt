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

// ViewModel encargado del registro de usuarios en Firebase
class RegisterViewModel : ViewModel() {

    // Referencias a Firebase Auth y Firestore
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // LiveData para observar el estado del registro (éxito, error, cargando)
    private val _registroEstado = MutableLiveData<RegistroEstado>()
    val registroEstado: LiveData<RegistroEstado> = _registroEstado

    // Formato para la validación de la fecha de emisión del administrador
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Función principal para validar los datos y proceder con el registro
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
        fotoBase64: String?,
        fechaNacimientoStr: String?
    ) {
        // Validaciones comunes para todos los usuarios
        if (nombre.isBlank() || apellido.isBlank() || dni.isBlank() || celular.isBlank() || email.isBlank()
            || password.isBlank() || confirmPassword.isBlank()
        ) {
            _registroEstado.value = RegistroEstado.Error("Por favor, completa todos los campos")
            return
        }

        // Validación de contraseñas iguales
        if (password != confirmPassword) {
            _registroEstado.value = RegistroEstado.Error("Las contraseñas no coinciden")
            return
        }

        // Validación del número de celular (formato peruano)
        if (!celular.matches(Regex("^9\\d{8}$"))) {
            _registroEstado.value = RegistroEstado.Error("El celular debe empezar con 9 y tener 9 dígitos")
            return
        }

        // Si no es administrador, validar el contacto de emergencia
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

        // Validación de longitud del DNI
        if (dni.length != 8) {
            _registroEstado.value = RegistroEstado.Error("El DNI debe tener 8 dígitos")
            return
        }

        // Validación de formato de correo electrónico
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _registroEstado.value = RegistroEstado.Error("El correo electrónico no tiene un formato válido")
            return
        }

        // Validación de seguridad de la contraseña
        val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d])[A-Za-z\\d\\W]{6,}$")
        if (!passwordRegex.matches(password)) {
            _registroEstado.value = RegistroEstado.Error(
                "La contraseña debe tener mínimo 6 caracteres, una mayúscula, una minúscula, un número y un símbolo especial"
            )
            return
        }

        if (fechaNacimientoStr.isNullOrBlank()) {
            _registroEstado.value = RegistroEstado.Error("Por favor, selecciona tu fecha de nacimiento")
            return
        }

        val fechaNacimiento: Date = try {
            dateFormat.parse(fechaNacimientoStr)!!
        } catch (e: Exception) {
            _registroEstado.value = RegistroEstado.Error("Formato de fecha de nacimiento inválido")
            return
        }

        val hoy = Calendar.getInstance()
        val fechaNacimientoCal = Calendar.getInstance().apply { time = fechaNacimiento }

        if (fechaNacimientoCal.after(hoy)) {
            _registroEstado.value = RegistroEstado.Error("La fecha de nacimiento no puede ser en el futuro")
            return
        }

        hoy.add(Calendar.YEAR, -18)
        if (fechaNacimientoCal.after(hoy)) {
            _registroEstado.value = RegistroEstado.Error("Debes tener al menos 18 años para registrarte")
            return
        }

        // Validaciones exclusivas para administrador
        if (esAdmin) {
            if (colegAdmin == null || colegAdmin.length != 10) {
                _registroEstado.value = RegistroEstado.Error("Ingresa un N° de colegiatura de 10 dígitos")
                return
            }
            if (fechaEmisionStr.isNullOrBlank()) {
                _registroEstado.value = RegistroEstado.Error("Ingresa la fecha de emisión")
                return
            }

            // Parseo de fecha de emisión
            val fechaEmisionDate: Date = try {
                dateFormat.parse(fechaEmisionStr)!!
            } catch (e: Exception) {
                _registroEstado.value = RegistroEstado.Error("Formato de fecha inválido")
                return
            }

            // Validar que la fecha de emisión no esté en el futuro
            if (fechaEmisionDate.after(Calendar.getInstance().time)) {
                _registroEstado.value = RegistroEstado.Error("La fecha de emisión no puede ser en el futuro")
                return
            }
        }

        // Mostramos estado de carga
        _registroEstado.value = RegistroEstado.Loading

        // Procedemos con la verificación y posterior creación del usuario
        verificarDniYCelularYColegiaturaYCrear(
            nombre, apellido, dni, celular, celularEmergencia,
            email, password, esAdmin, colegAdmin, fechaEmisionStr, fotoBase64, fechaNacimientoStr
        )
    }

    // Verifica que no existan duplicados en DNI, celular y colegiatura (si es admin)
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
        fotoBase64: String?,
        fechaNacimientoStr: String?
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

                        // Verificación adicional si es administrador
                        if (esAdmin) {
                            db.collection("usuarios").whereEqualTo("nColegiatura", colegAdmin)
                                .get()
                                .addOnSuccessListener { colegResult ->
                                    if (!colegResult.isEmpty) {
                                        _registroEstado.value = RegistroEstado.Error("Este número de colegiatura ya está registrado.")
                                        return@addOnSuccessListener
                                    }

                                    // Si todo está validado, se procede a crear usuario
                                    crearUsuarioAuthYFirestore(
                                        nombre, apellido, dni, celular, celularEmergencia,
                                        email, password, esAdmin, colegAdmin, fechaEmisionStr, fotoBase64, fechaNacimientoStr
                                    )
                                }
                                .addOnFailureListener {
                                    _registroEstado.value = RegistroEstado.Error("Error al verificar colegiatura: ${it.message}")
                                }
                        } else {
                            // Si no es admin, se crea directamente el usuario
                            crearUsuarioAuthYFirestore(
                                nombre, apellido, dni, celular, celularEmergencia,
                                email, password, esAdmin, colegAdmin, fechaEmisionStr, fotoBase64,   fechaNacimientoStr
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

    // Crea el usuario tanto en Firebase Authentication como en Firestore
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
        fotoBase64: String?,
        fechaNacimientoStr: String?
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser!!

                    // Creamos el mapa con los datos del usuario
                    val userData = hashMapOf(
                        "nombre" to nombre,
                        "apellido" to apellido,
                        "dni" to dni,
                        "celular" to celular,
                        "email" to email,
                        "esAdministrador" to esAdmin,
                        "fotoPerfilBase64" to (fotoBase64 ?: ""),
                        "fechaNacimiento" to (fechaNacimientoStr ?: "")
                    ).apply {
                        // Agregamos campos según el tipo de usuario
                        if (!esAdmin) {
                            put("celularEmergencia", celularEmergencia)
                        }
                        if (esAdmin) {
                            put("nColegiatura", colegAdmin ?: "")
                            put("fechaEmision", fechaEmisionStr ?: "")
                        }
                    }

                    // Guardamos los datos en Firestore
                    db.collection("usuarios").document(firebaseUser.uid)
                        .set(userData)
                        .addOnSuccessListener {
                            _registroEstado.value = RegistroEstado.Success
                        }
                        .addOnFailureListener {
                            _registroEstado.value = RegistroEstado.Error("Error al guardar datos: ${it.message}")
                        }
                } else {
                    // Manejamos errores de autenticación
                    val ex = task.exception
                    if (ex is FirebaseAuthUserCollisionException) {
                        _registroEstado.value = RegistroEstado.Error("Este correo ya está registrado.")
                    } else {
                        _registroEstado.value = RegistroEstado.Error("Error en el registro: ${ex?.message}")
                    }
                }
            }
    }

    // Estado del proceso de registro (éxito, error, cargando)
    sealed class RegistroEstado {
        object Success : RegistroEstado()
        data class Error(val mensaje: String) : RegistroEstado()
        object Loading : RegistroEstado()
    }

    // Función para convertir imagen en base64 (usado para foto de perfil)
    fun bitmapToBase64(bitmap: Bitmap): String {
        val maxSize = 600 // más grande para más detalle
        val scaled = scaleBitmapProportionally(bitmap, maxSize)
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 100, baos)  // calidad máxima
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }

    private fun scaleBitmapProportionally(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val scaledWidth: Int
        val scaledHeight: Int

        if (width >= height) {
            scaledWidth = maxSize
            scaledHeight = (height.toFloat() / width.toFloat() * maxSize).toInt()
        } else {
            scaledHeight = maxSize
            scaledWidth = (width.toFloat() / height.toFloat() * maxSize).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
    }
}