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
    private lateinit var uidActual: String
    private val listaUsuarios = mutableListOf<Usuario>()
    private val estadoSolicitudes = mutableMapOf<String, String>()
    private lateinit var adapter: UsuarioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBuscarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        uidActual = auth.currentUser?.uid ?: ""

        binding.iconInfo.setOnClickListener { finish() }

        adapter = UsuarioAdapter(
            listaUsuarios,
            estadoSolicitudes,
            uidActual,
            onAgregar = { usuario -> enviarSolicitud(usuario.uid) },
            onCancelar = { usuario -> cancelarSolicitud(usuario.uid) },
            onConfirmar = { usuario -> aceptarSolicitud(usuario.uid) }
        )

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
                    binding.tvNoResultados.visibility = View.VISIBLE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun buscarUsuarios(texto: String) {
        val coleccion = db.collection("usuarios")

        coleccion
            .whereEqualTo("esAdministrador", !esAdmin)
            .get()
            .addOnSuccessListener { documentos ->
                listaUsuarios.clear()
                estadoSolicitudes.clear()

                for (doc in documentos) {
                    val uid = doc.id
                    val nombre = doc.getString("nombre") ?: ""
                    val apellido = doc.getString("apellido") ?: ""

                    if (nombre.contains(texto, ignoreCase = true) || apellido.contains(texto, ignoreCase = true)) {
                        val usuario = Usuario(
                            uid = uid,
                            nombre = nombre,
                            apellido = apellido,
                            esAdministrador = doc.getBoolean("esAdministrador") ?: false,
                            fotoPerfilBase64 = doc.getString("fotoPerfilBase64")
                        )
                        listaUsuarios.add(usuario)
                    }
                }

                cargarSolicitudesBusqueda()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al buscar usuarios", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarSolicitudesBusqueda() {
        db.collection("solicitudes")
            .whereEqualTo("emisorId", uidActual)
            .get()
            .addOnSuccessListener { enviadas ->
                for (doc in enviadas) {
                    val receptorId = doc.getString("receptorId") ?: continue
                    val estado = doc.getString("estado") ?: "pendiente"
                    estadoSolicitudes[receptorId] = estado
                }

                db.collection("solicitudes")
                    .whereEqualTo("receptorId", uidActual)
                    .get()
                    .addOnSuccessListener { recibidas ->
                        for (doc in recibidas) {
                            val emisorId = doc.getString("emisorId") ?: continue
                            val estado = doc.getString("estado") ?: "pendiente"
                            estadoSolicitudes[emisorId] =
                                if (estado == "pendiente") "recibida" else estado
                        }

                        if (listaUsuarios.isEmpty()) {
                            binding.rvResultados.visibility = View.GONE
                            binding.tvNoResultados.visibility = View.VISIBLE
                        } else {
                            binding.rvResultados.visibility = View.VISIBLE
                            binding.tvNoResultados.visibility = View.GONE
                            adapter.notifyDataSetChanged()
                        }
                    }
            }
    }

    private fun enviarSolicitud(receptorId: String) {
        val solicitud = hashMapOf(
            "emisorId" to uidActual,
            "receptorId" to receptorId,
            "estado" to "pendiente"
        )
        db.collection("solicitudes")
            .add(solicitud)
            .addOnSuccessListener {
                estadoSolicitudes[receptorId] = "pendiente"
                adapter.actualizarEstado(receptorId, "pendiente")
                Toast.makeText(this, "Solicitud enviada", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cancelarSolicitud(receptorId: String) {
        db.collection("solicitudes")
            .whereEqualTo("emisorId", uidActual)
            .whereEqualTo("receptorId", receptorId)
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    db.collection("solicitudes").document(doc.id).delete()
                }
                estadoSolicitudes.remove(receptorId)
                adapter.actualizarEstado(receptorId, null)
                Toast.makeText(this, "Solicitud cancelada", Toast.LENGTH_SHORT).show()
            }
    }

    private fun aceptarSolicitud(emisorId: String) {
        db.collection("solicitudes")
            .whereEqualTo("emisorId", emisorId)
            .whereEqualTo("receptorId", uidActual)
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    db.collection("solicitudes").document(doc.id)
                        .update("estado", "aceptado")
                }
                estadoSolicitudes[emisorId] = "aceptado"
                adapter.actualizarEstado(emisorId, "aceptado")
                Toast.makeText(this, "Solicitud aceptada", Toast.LENGTH_SHORT).show()
            }
    }
}