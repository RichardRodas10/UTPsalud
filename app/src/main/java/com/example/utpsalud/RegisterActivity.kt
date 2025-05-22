package com.example.utpsalud

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.utpsalud.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.btnRegister.setOnClickListener {
            val nombre = binding.etNombre.text.toString().trim()
            val email = binding.etRegisterEmail.text.toString().trim()
            val password = binding.etRegisterPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if(nombre.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(password != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Crear usuario en Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Registro en Authentication exitoso
                        val firebaseUser = auth.currentUser
                        if (firebaseUser != null) {
                            // Actualizar el displayName en Firebase Auth
                            updateUserProfile(firebaseUser, nombre)

                            // Guardar información adicional del usuario en Cloud Firestore
                            saveUserDataToFirestore(firebaseUser, nombre, email)
                        } else {
                            // Si task.isSuccessful es false
                            Toast.makeText(this, "Error: Usuario no encontrado después del registro.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // Error en el registro de Authentication
                        Toast.makeText(this, "Error en el registro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        binding.loginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun updateUserProfile(firebaseUser: FirebaseUser, nombre: String) {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(nombre)
            .build()

        firebaseUser.updateProfile(profileUpdates)
            .addOnCompleteListener { profileTask ->
                if (profileTask.isSuccessful) {
                } else {
                }
            }
    }

    private fun saveUserDataToFirestore(firebaseUser: FirebaseUser, nombre: String, email: String) {
        val userId = firebaseUser.uid

        // Mapa con información del usuario
        val userData = hashMapOf(
            "nombre" to nombre,
            "email" to email
        )

        // Referencia a la colección "usuarios" y al documento con el ID del usuario
        db.collection("usuarios").document(userId)
            .set(userData) // .set() crea el documento si no existe, o lo sobrescribe si ya existe
            .addOnSuccessListener {
                // Datos guardados exitosamente en Firestore
                Toast.makeText(this, "Registro exitoso y datos guardados", Toast.LENGTH_SHORT).show()

                // Navegar a LoginActivity
                val intent = Intent(this, LoginActivity::class.java)
                // Limpiar el stack de actividades para que el usuario no pueda volver a RegisterActivity
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Registro exitoso, pero error al guardar datos: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
