package com.example.utpsalud

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.utpsalud.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Formato de fecha usado en etFechaEmision
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 1) Mostrar u ocultar campos de administrador
        binding.switch1.setOnCheckedChangeListener { _, isChecked ->
            binding.adminFields.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // 2) Selector de fecha para etFechaEmision
        binding.etFechaEmision.setOnClickListener {
            showDatePickerDialog()
        }

        // 3) Botón de registro
        binding.btnRegister.setOnClickListener {
            val nombre = binding.etNombre.text.toString().trim()
            val apellido = binding.etApellido.text.toString().trim()
            val dni = binding.etDNI.text.toString().trim()
            val celular = binding.etCelular.text.toString().trim()
            val email = binding.etRegisterEmail.text.toString().trim()
            val password = binding.etRegisterPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            // Validación registro
            if (nombre.isEmpty() || apellido.isEmpty() || dni.isEmpty() || celular.isEmpty() || email.isEmpty()
                || password.isEmpty() || confirmPassword.isEmpty()
            ) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!celular.matches(Regex("^9\\d{8}$"))) {
                Toast.makeText(this, "El celular debe empezar con 9", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (dni.length != 8) {
                Toast.makeText(this, "Ingresa un N° de colegiatura de 10 dígitos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Si es administrador, validar colegiatura y fecha de emisión
            val esAdmin = binding.switch1.isChecked
            val colegAdmin = binding.etColegiatura.text.toString().trim()
            val fechaEmisionStr = binding.etFechaEmision.text.toString().trim()

            if (esAdmin) {
                if (colegAdmin.length != 10) {
                    Toast.makeText(this, "Ingresa un N° de colegiatura de 10 dígitos", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (fechaEmisionStr.isEmpty()) {
                    Toast.makeText(this, "Ingresa la fecha de emisión", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                // Validar que la fecha no sea en el futuro
                val fechaEmisionDate: Date = try {
                    dateFormat.parse(fechaEmisionStr)!!
                } catch (e: Exception) {
                    Toast.makeText(this, "Formato de fecha inválido", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val today = Calendar.getInstance().time
                if (fechaEmisionDate.after(today)) {
                    Toast.makeText(this, "La fecha de emisión no puede ser en el futuro", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Crear usuario en Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = auth.currentUser
                        if (firebaseUser != null) {
                            updateUserProfile(firebaseUser, "$nombre $apellido")
                            saveUserDataToFirestore(
                                firebaseUser,
                                nombre,
                                apellido,
                                dni,
                                celular,
                                email,
                                esAdmin,
                                colegAdmin,
                                fechaEmisionStr
                            )
                        } else {
                            Toast.makeText(this, "Error: usuario no encontrado tras el registro.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val exception = task.exception
                        if (exception is FirebaseAuthUserCollisionException) {
                            Toast.makeText(this, "Este correo ya está registrado.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Error en el registro: ${exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }

        // 4) Link a pantalla de login
        binding.loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    // Muestra un DatePickerDialog y asigna la fecha al EditText
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                binding.etFechaEmision.setText(dateFormat.format(selCal.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Actualiza el displayName del usuario en Firebase Auth
    private fun updateUserProfile(firebaseUser: FirebaseUser, displayName: String) {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        firebaseUser.updateProfile(profileUpdates)
            .addOnCompleteListener { }
    }

    // Guarda los datos en Firestore
    private fun saveUserDataToFirestore(
        firebaseUser: FirebaseUser,
        nombre: String,
        apellido: String,
        dni: String,
        celular: String,
        email: String,
        esAdmin: Boolean,
        colegAdmin: String,
        fechaEmision: String
    ) {
        val userId = firebaseUser.uid
        val userData = hashMapOf(
            "nombre" to nombre,
            "apellido" to apellido,
            "dni" to dni,
            "celular" to celular,
            "email" to email,
            "esAdministrador" to esAdmin
        ).apply {
            if (esAdmin) {
                put("nColegiatura", colegAdmin)
                put("fechaEmision", fechaEmision)
            }
        }

        db.collection("usuarios").document(userId)
            .set(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Registro exitoso y datos guardados", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Registro exitoso, pero error al guardar datos: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}