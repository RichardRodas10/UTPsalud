package com.example.utpsalud

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.utpsalud.adapter.UsuarioAdapter
import com.example.utpsalud.databinding.ActivityUsuariosBinding
import com.example.utpsalud.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UsuariosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsuariosBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var esAdmin: Boolean = false
    private val listaUsuarios = mutableListOf<Usuario>()
    private lateinit var adapter: UsuarioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsuariosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.iconBack.setOnClickListener { finish() }

        adapter = UsuarioAdapter(listaUsuarios) { usuarioSeleccionado ->
            Toast.makeText(
                this,
                "Agregar: ${usuarioSeleccionado.nombre} ${usuarioSeleccionado.apellido}",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.rvUsuarios.layoutManager = LinearLayoutManager(this)
        binding.rvUsuarios.adapter = adapter

        obtenerRolUsuarioActual {
            cargarUsuarios()
        }
    }

    private fun obtenerRolUsuarioActual(onComplete: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("usuarios").document(userId).get()
            .addOnSuccessListener { doc ->
                esAdmin = doc.getBoolean("esAdministrador") ?: false
                onComplete()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener rol de usuario", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarUsuarios() {
        binding.progressBarUsuarios.visibility = View.VISIBLE
        binding.rvUsuarios.visibility = View.GONE
        binding.tvNoUsuarios.visibility = View.GONE

        db.collection("usuarios")
            .whereEqualTo("esAdministrador", !esAdmin)
            .get()
            .addOnSuccessListener { documentos ->
                listaUsuarios.clear()

                for (doc in documentos) {
                    val nombre = doc.getString("nombre") ?: ""
                    val apellido = doc.getString("apellido") ?: ""
                    val usuario = Usuario(
                        nombre = nombre,
                        apellido = apellido,
                        esAdministrador = doc.getBoolean("esAdministrador") ?: false,
                        fotoPerfilBase64 = doc.getString("fotoPerfilBase64")
                    )
                    listaUsuarios.add(usuario)
                }

                binding.progressBarUsuarios.visibility = View.GONE

                if (listaUsuarios.isEmpty()) {
                    binding.rvUsuarios.visibility = View.GONE
                    binding.tvNoUsuarios.visibility = View.VISIBLE
                } else {
                    binding.rvUsuarios.visibility = View.VISIBLE
                    binding.tvNoUsuarios.visibility = View.GONE
                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener {
                binding.progressBarUsuarios.visibility = View.GONE
                Toast.makeText(this, "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
            }
    }

}
