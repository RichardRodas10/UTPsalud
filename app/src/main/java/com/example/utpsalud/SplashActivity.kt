package com.example.utpsalud

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser

        if (currentUser != null) {
            val uid = currentUser.uid
            db.collection("usuarios").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val esAdmin = doc.getBoolean("esAdministrador") ?: false
                        if (esAdmin) {
                            startActivity(Intent(this, HomeAdminActivity::class.java))
                        } else {
                            startActivity(Intent(this, HomeActivity::class.java))
                        }
                    } else {
                        // No se encontró el documento del usuario
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                    finish()
                }
                .addOnFailureListener {
                    // Error al acceder a Firestore
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
        } else {
            // Usuario no autenticado
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}