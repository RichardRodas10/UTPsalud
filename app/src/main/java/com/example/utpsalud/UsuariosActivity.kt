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
    private lateinit var uidActual: String
    private val listaUsuarios = mutableListOf<Usuario>()
    private val estadoSolicitudes = mutableMapOf<String, String>()
    private lateinit var adapter: UsuarioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsuariosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        uidActual = auth.currentUser?.uid ?: ""

        binding.iconBack.setOnClickListener { finish() }

        adapter = UsuarioAdapter(
            listaUsuarios,
            estadoSolicitudes,
            uidActual,
            onAgregar = { usuario -> enviarSolicitud(usuario.uid) },
            onCancelar = { usuario -> cancelarSolicitud(usuario.uid) },
            onConfirmar = { usuario -> aceptarSolicitud(usuario.uid) }
        )

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
                estadoSolicitudes.clear()

                for (doc in documentos) {
                    val uid = doc.id
                    val nombre = doc.getString("nombre") ?: ""
                    val apellido = doc.getString("apellido") ?: ""
                    val usuario = Usuario(
                        uid = uid,
                        nombre = nombre,
                        apellido = apellido,
                        esAdministrador = doc.getBoolean("esAdministrador") ?: false,
                        fotoPerfilBase64 = doc.getString("fotoPerfilBase64")
                    )
                    listaUsuarios.add(usuario)
                }

                cargarSolicitudes()
            }
            .addOnFailureListener {
                binding.progressBarUsuarios.visibility = View.GONE
                Toast.makeText(this, "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarSolicitudes() {
        db.collection("solicitudes")
            .whereEqualTo("emisorId", uidActual)
            .get()
            .addOnSuccessListener { enviadas ->
                for (doc in enviadas) {
                    val receptorId = doc.getString("receptorId") ?: continue
                    val estado = doc.getString("estado") ?: "pendiente"
                    estadoSolicitudes[receptorId] = estado // ej: pendiente
                }

                db.collection("solicitudes")
                    .whereEqualTo("receptorId", uidActual)
                    .get()
                    .addOnSuccessListener { recibidas ->
                        for (doc in recibidas) {
                            val emisorId = doc.getString("emisorId") ?: continue
                            val estado = doc.getString("estado") ?: "pendiente"
                            if (estado == "pendiente") {
                                estadoSolicitudes[emisorId] = "recibida"
                            } else {
                                estadoSolicitudes[emisorId] = estado
                            }
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
                adapter.notifyDataSetChanged()
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
                adapter.notifyDataSetChanged()
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
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Solicitud aceptada", Toast.LENGTH_SHORT).show()
            }
    }
}