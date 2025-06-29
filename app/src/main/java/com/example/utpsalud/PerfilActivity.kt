package com.example.utpsalud

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.utpsalud.databinding.ActivityPerfilBinding
import com.google.firebase.firestore.FirebaseFirestore

class PerfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilBinding
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        binding.iconBack.setOnClickListener { finish() }

        val uid = intent.getStringExtra("uid")
        if (uid.isNullOrEmpty()) {
            Toast.makeText(this, "Usuario no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        cargarDatosUsuario(uid)
    }

    private fun cargarDatosUsuario(uid: String) {
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val nombre = doc.getString("nombre") ?: "Sin nombre"
                    val apellido = doc.getString("apellido") ?: "Sin apellido"
                    val dni = doc.getString("dni") ?: "Sin DNI"
                    val celular = doc.getString("celular") ?: "Sin celular"
                    val email = doc.getString("email") ?: "Sin correo"
                    val fotoBase64 = doc.getString("fotoPerfilBase64")
                    val esAdmin = doc.getBoolean("esAdministrador") ?: false
                    val celularEmergencia = doc.getString("celularEmergencia")

                    // Mostrar datos en los TextView
                    binding.textNombre.text = nombre
                    binding.textApellido.text = apellido
                    binding.textDni.text = dni
                    binding.textCelular.text = celular
                    binding.textEmail.text = email

                    // Mostrar imagen si hay
                    if (!fotoBase64.isNullOrEmpty()) {
                        val bytes = Base64.decode(fotoBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        binding.profileImage.setImageBitmap(bitmap)
                    } else {
                        binding.profileImage.setImageResource(R.drawable.ic_account)
                    }

                    // Mostrar campo de emergencia solo si es paciente
                    if (!esAdmin && !celularEmergencia.isNullOrEmpty()) {
                        binding.textCelularEmergencia.text = celularEmergencia
                        binding.textContactoEmergencia.visibility = android.view.View.VISIBLE
                        binding.contenedorContactoEm.visibility = android.view.View.VISIBLE
                    } else {
                        binding.textContactoEmergencia.visibility = android.view.View.GONE
                        binding.contenedorContactoEm.visibility = android.view.View.GONE
                    }

                    // Hacer clickeables los celulares para abrir app de llamadas
                    binding.textCelular.setOnClickListener {
                        abrirLlamada(celular)
                    }

                    binding.textCelularEmergencia.setOnClickListener {
                        if (!celularEmergencia.isNullOrEmpty()) {
                            abrirLlamada(celularEmergencia)
                        }
                    }

                } else {
                    Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar usuario", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun abrirLlamada(numero: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$numero")
        startActivity(intent)
    }
}