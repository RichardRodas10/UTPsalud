package com.example.utpsalud

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.utpsalud.adapter.UsuarioAdapter
import com.example.utpsalud.databinding.ActivityBuscarBinding
import com.example.utpsalud.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BuscarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBuscarBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var esAdmin: Boolean = false
    private val listaUsuarios = mutableListOf<Usuario>()
    private lateinit var adapter: UsuarioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBuscarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.iconInfo.setOnClickListener { finish() }

        adapter = UsuarioAdapter(listaUsuarios) { usuarioSeleccionado ->
            Toast.makeText(
                this,
                "Agregar: ${usuarioSeleccionado.nombre} ${usuarioSeleccionado.apellido}",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.rvResultados.layoutManager = LinearLayoutManager(this)
        binding.rvResultados.adapter = adapter

        obtenerRolUsuarioActual {
            configurarBusqueda()
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

    private fun configurarBusqueda() {
        binding.editBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val texto = s.toString().trim()
                if (texto.length >= 2) {
                    buscarUsuarios(texto)
                } else {
                    listaUsuarios.clear()
                    adapter.notifyDataSetChanged()
                    binding.rvResultados.visibility = View.GONE
                    binding.tvNoResultados.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun buscarUsuarios(texto: String) {
        db.collection("usuarios")
            .whereEqualTo("esAdministrador", !esAdmin)
            .get()
            .addOnSuccessListener { documentos ->
                listaUsuarios.clear()

                for (doc in documentos) {
                    val nombre = doc.getString("nombre") ?: ""
                    val apellido = doc.getString("apellido") ?: ""

                    val nombreCompleto = "$nombre $apellido"
                    if (nombre.contains(texto, ignoreCase = true) ||
                        apellido.contains(texto, ignoreCase = true) ||
                        nombreCompleto.contains(texto, ignoreCase = true)) {
                        val usuario = Usuario(
                            nombre = nombre,
                            apellido = apellido,
                            esAdministrador = doc.getBoolean("esAdministrador") ?: false,
                            fotoPerfilBase64 = doc.getString("fotoPerfilBase64")
                        )
                        listaUsuarios.add(usuario)
                    }
                }

                if (listaUsuarios.isEmpty()) {
                    binding.rvResultados.visibility = View.GONE
                    binding.tvNoResultados.visibility = View.VISIBLE
                } else {
                    binding.tvNoResultados.visibility = View.GONE
                    binding.rvResultados.visibility = View.VISIBLE
                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al buscar usuarios", Toast.LENGTH_SHORT).show()
            }
    }
}
