package com.example.utpsalud.view.activity

import MensajesSoporteAdapter
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.utpsalud.R
import com.example.utpsalud.model.MensajeSoporte
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore

class HabilitarActivity : AppCompatActivity() {

    private lateinit var txtNombreUsuario: TextView
    private lateinit var recyclerMensajes: RecyclerView
    private lateinit var btnActivarCuenta: Button
    private val db = FirebaseFirestore.getInstance()

    private var usuarioId: String? = null
    private var nombreUsuario: String? = null

    private val listaMensajes = mutableListOf<MensajeSoporte>()
    private lateinit var adapter: MensajesSoporteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habilitar)

        txtNombreUsuario = findViewById(R.id.txtNombreUsuario)
        recyclerMensajes = findViewById(R.id.recyclerMensajesSoporte)
        btnActivarCuenta = findViewById(R.id.btnActivarCuenta)

        usuarioId = intent.getStringExtra("usuarioId")
        nombreUsuario = intent.getStringExtra("nombreUsuario")

        if (nombreUsuario != null) {
            txtNombreUsuario.text = nombreUsuario
        }

        adapter = MensajesSoporteAdapter(listaMensajes)
        recyclerMensajes.layoutManager = LinearLayoutManager(this)
        recyclerMensajes.adapter = adapter

        cargarMensajesSoporte()

        btnActivarCuenta.setOnClickListener {
            activarCuenta()
        }
    }

    private fun cargarMensajesSoporte() {
        val uid = usuarioId
        if (uid.isNullOrEmpty()) {
            Toast.makeText(this, "Usuario inválido para cargar mensajes", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("soporte")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                listaMensajes.clear()

                if (snapshot.isEmpty) {
                    Toast.makeText(this, "No hay mensajes de soporte para este usuario", Toast.LENGTH_LONG).show()
                    adapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                for (doc in snapshot.documents) {
                    val mensaje = doc.getString("mensaje") ?: "Mensaje sin contenido"
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    listaMensajes.add(MensajeSoporte(mensaje, timestamp))
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar mensajes: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun activarCuenta() {
        if (usuarioId == null) {
            Toast.makeText(this, "Usuario inválido", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("usuarios").document(usuarioId!!)
            .update("activo", true)
            .addOnSuccessListener {
                Toast.makeText(this, "Cuenta activada correctamente", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Snackbar.make(btnActivarCuenta, "Error al activar cuenta: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
    }
}