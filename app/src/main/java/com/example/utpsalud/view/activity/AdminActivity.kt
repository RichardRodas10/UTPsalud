package com.example.utpsalud.view.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.utpsalud.R
import com.example.utpsalud.view.adapter.UsuarioDesactivadoAdapter
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AdminActivity : AppCompatActivity() {

    private lateinit var recyclerAdmin: RecyclerView
    private lateinit var adapter: UsuarioDesactivadoAdapter
    private val listaUsuarios = mutableListOf<Map<String, Any>>()
    private val db = FirebaseFirestore.getInstance()

    private var listenerRegistro: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        recyclerAdmin = findViewById(R.id.recyclerAdmin)
        recyclerAdmin.layoutManager = LinearLayoutManager(this)

        adapter = UsuarioDesactivadoAdapter(listaUsuarios) { usuarioMap ->
            val nombre = usuarioMap["nombre"] as? String ?: ""
            val apellido = usuarioMap["apellido"] as? String ?: ""
            val nombreCompleto = "$nombre $apellido"
            val uid = usuarioMap["uid"] as? String ?: ""

            if (uid.isNotEmpty()) {
                val intent = Intent(this, HabilitarActivity::class.java)
                intent.putExtra("usuarioId", uid)
                intent.putExtra("nombreUsuario", nombreCompleto)
                startActivity(intent)
            } else {
                Snackbar.make(recyclerAdmin, "Usuario inválido", Snackbar.LENGTH_SHORT).show()
            }
        }
        recyclerAdmin.adapter = adapter

        cargarUsuariosDesactivados()
    }

    private fun cargarUsuariosDesactivados() {
        listenerRegistro?.remove() // remover listener anterior si hay

        listenerRegistro = db.collection("usuarios")
            .whereEqualTo("activo", false)
            //.orderBy("nombre") // evitar problemas de índice
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Snackbar.make(
                        recyclerAdmin,
                        "Error al cargar usuarios: ${error.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    listaUsuarios.clear()
                    adapter.setData(listaUsuarios)
                    Snackbar.make(
                        recyclerAdmin,
                        "No hay usuarios desactivados.",
                        Snackbar.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                listaUsuarios.clear()

                for (document in snapshot.documents) {
                    val data = document.data ?: continue
                    val nombre = data["nombre"] as? String ?: ""
                    val apellido = data["apellido"] as? String ?: ""
                    val foto = data["fotoPerfilBase64"] as? String ?: ""

                    val usuarioMap = mapOf(
                        "uid" to document.id,
                        "nombre" to nombre,
                        "apellido" to apellido,
                        "foto" to foto
                    )
                    listaUsuarios.add(usuarioMap)
                }

                adapter.setData(listaUsuarios)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistro?.remove() // quitar listener para evitar fugas de memoria
    }
}