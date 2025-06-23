package com.example.utpsalud

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.utpsalud.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Constante para el picker de imagen
    private val PICK_IMAGE_REQUEST = 1001

    // Almacena la imagen en Base64
    private var profileImageBase64: String = ""

    // Formato de fecha usado en etFechaEmision
    // Formato de fecha usado en etFechaEmision
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 0) FOTO DE PERFIL
        // Al hacer clic en el ícono de cámara, abrimos galería
        binding.cameraIcon.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

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
            val celularEmergencia = binding.etCelularEmergencia.text.toString().trim()
            val email = binding.etRegisterEmail.text.toString().trim()
            val password = binding.etRegisterPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            // Validación registro
            if (nombre.isEmpty() || apellido.isEmpty() || dni.isEmpty() ||
                celular.isEmpty() || celularEmergencia.isEmpty() || email.isEmpty() ||
                password.isEmpty() || confirmPassword.isEmpty()
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
            if (!celularEmergencia.matches(Regex("^9\\d{8}$"))) {
                Toast.makeText(this, "El contacto de emergencia debe empezar con 9", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (dni.length != 8) {
                Toast.makeText(this, "El DNI debe tener 8 dígitos", Toast.LENGTH_SHORT).show()
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
                if (fechaEmisionDate.after(Calendar.getInstance().time)) {
                    Toast.makeText(this, "La fecha de emisión no puede ser en el futuro", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Crear usuario en Firebase Authentication
            // Verificar si el DNI o celular ya están registrados en Firestore
            db.collection("usuarios")
                .whereIn("dni", listOf(dni))
                .get()
                .addOnSuccessListener { dniResult ->
                    if (!dniResult.isEmpty) {
                        Toast.makeText(this, "Este DNI ya está registrado.", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    db.collection("usuarios")
                        .whereIn("celular", listOf(celular))
                        .get()
                        .addOnSuccessListener { celularResult ->
                            if (!celularResult.isEmpty) {
                                Toast.makeText(this, "Este número de celular ya está registrado.", Toast.LENGTH_LONG).show()
                                return@addOnSuccessListener
                            }

                            // Si pasa ambas validaciones, se crea el usuario en Firebase Auth
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val firebaseUser = auth.currentUser!!
                                        updateUserProfile(firebaseUser, "$nombre $apellido")

                                        saveUserDataToFirestore(
                                            firebaseUser,
                                            nombre,
                                            apellido,
                                            dni,
                                            celular,
                                            celularEmergencia,
                                            email,
                                            esAdmin,
                                            binding.etColegiatura.text.toString().trim(),
                                            binding.etFechaEmision.text.toString().trim(),
                                            profileImageBase64
                                        )
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
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al verificar celular: ${it.message}", Toast.LENGTH_LONG).show()
                        }

                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al verificar DNI: ${it.message}", Toast.LENGTH_LONG).show()
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

    // Recibe la imagen seleccionada y la convierte a Base64
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data?.data != null) {
            val uri: Uri = data.data!!
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            binding.profileImage.setImageBitmap(bitmap)
            profileImageBase64 = encodeImageToBase64(bitmap)
        }
    }

    // Codifica el bitmap a Base64 (redimensionado a 300×300, JPEG 80%)
    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val resized = Bitmap.createScaledBitmap(bitmap, 300, 300, true)
        val baos = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }

    // Muestra un DatePickerDialog y asigna la fecha al EditText
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, y, m, d ->
                val sel = Calendar.getInstance().apply { set(y, m, d) }
                binding.etFechaEmision.setText(dateFormat.format(sel.time))
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
        firebaseUser.updateProfile(profileUpdates).addOnCompleteListener { }
    }

    // Guarda los datos en Firestore, incluyendo la foto en Base64
    private fun saveUserDataToFirestore(
        firebaseUser: FirebaseUser,
        nombre: String,
        apellido: String,
        dni: String,
        celular: String,
        celularEmergencia: String,
        email: String,
        esAdmin: Boolean,
        colegAdmin: String,
        fechaEmision: String,
        photoBase64: String
    ) {
        val userId = firebaseUser.uid
        val userData = hashMapOf(
            "nombre" to nombre,
            "apellido" to apellido,
            "dni" to dni,
            "celular" to celular,
            "celularEmergencia" to celularEmergencia,
            "email" to email,
            "esAdministrador" to esAdmin,
            "fotoPerfilBase64" to photoBase64
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
                Toast.makeText(this, "Error al guardar datos: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
