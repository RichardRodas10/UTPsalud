package com.example.utpsalud

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.utpsalud.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.tvWelcome.text = "Cargando..."

        val currentUser = auth.currentUser!!
        val userId = currentUser.uid

        firestore.collection("usuarios").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val nombre = document.getString("nombre")
                binding.tvWelcome.text =
                    if (!nombre.isNullOrEmpty()) "Bienvenido, $nombre"
                    else "Bienvenido, ${currentUser.email ?: "Usuario"}"
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al obtener el nombre: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.tvWelcome.text = "Bienvenido, ${currentUser.email ?: "Usuario"}"
            }

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        // Cargar el layout del diálogo (ventana de cierre sesión)
        val dialogView = layoutInflater.inflate(R.layout.dialog_logout, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Botones del diálogo
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirmLogout)
        val btnCancel  = dialogView.findViewById<Button>(R.id.btnCancelLogout)

        btnConfirm.setOnClickListener {
            auth.signOut()  // Cierra sesión en Firebase
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            dialog.dismiss()
            finish()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Fondo transparente para la ventana de cierre sesión
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
}
