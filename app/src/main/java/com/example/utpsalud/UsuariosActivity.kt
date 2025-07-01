package com.example.utpsalud

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.utpsalud.adapter.UsuarioAdapter
import com.example.utpsalud.databinding.ActivityUsuariosBinding
import com.example.utpsalud.model.Usuario
import com.example.utpsalud.view.activity.PerfilActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UsuariosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsuariosBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var esAdmin = false
    private lateinit var uidActual: String

    private val estadoSolicitudes = mutableMapOf<String, String>()
    private val listaDisponibles = mutableListOf<Usuario>()
    private val listaEnviadas = mutableListOf<Usuario>()
    private val listaRecibidas = mutableListOf<Usuario>()

    private lateinit var adapterDisp: UsuarioAdapter
    private lateinit var adapterEnv: UsuarioAdapter
    private lateinit var adapterRec: UsuarioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsuariosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        uidActual = auth.currentUser?.uid ?: ""

        binding.iconBack.setOnClickListener { finish() }

        obtenerRolUsuarioActual { cargarUsuarios() }
    }

    private fun obtenerRolUsuarioActual(onComplete: () -> Unit) {
        val id = auth.currentUser?.uid ?: return
        db.collection("usuarios").document(id).get()
            .addOnSuccessListener { doc ->
                esAdmin = doc.getBoolean("esAdministrador") ?: false
                configurarAdapters()
                onComplete()
            }
            .addOnFailureListener {
                toast("Error al obtener rol de usuario")
            }
    }

    private fun configurarAdapters() {
        adapterDisp = UsuarioAdapter(
            listaDisponibles, estadoSolicitudes, uidActual, esAdmin,
            onAgregar = { u -> enviarSolicitud(u.uid) },
            onCancelar = { u -> cancelarSolicitud(u.uid) },
            onConfirmar = { u -> aceptarSolicitud(u.uid) },
            onClickItem = { usuario ->
                val intent = Intent(this, PerfilActivity::class.java)
                intent.putExtra("uid", usuario.uid)
                startActivity(intent)
            }
        )

        adapterEnv = UsuarioAdapter(
            listaEnviadas, estadoSolicitudes, uidActual, esAdmin,
            onAgregar = {},
            onCancelar = { u -> cancelarSolicitud(u.uid) },
            onConfirmar = {},
            onClickItem = { usuario ->
                val intent = Intent(this, PerfilActivity::class.java)
                intent.putExtra("uid", usuario.uid)
                startActivity(intent)
            }
        )

        adapterRec = UsuarioAdapter(
            listaRecibidas, estadoSolicitudes, uidActual, esAdmin,
            onAgregar = {},
            onCancelar = {},
            onConfirmar = { u -> aceptarSolicitud(u.uid) },
            onClickItem = { usuario ->
                val intent = Intent(this, PerfilActivity::class.java)
                intent.putExtra("uid", usuario.uid)
                startActivity(intent)
            }
        )

        binding.rvUsuarios.layoutManager = LinearLayoutManager(this)
        binding.rvUsuarios.adapter = adapterDisp

        binding.rvSolicitudesEnviadas.layoutManager = LinearLayoutManager(this)
        binding.rvSolicitudesEnviadas.adapter = adapterEnv

        binding.rvSolicitudesRecibidas.layoutManager = LinearLayoutManager(this)
        binding.rvSolicitudesRecibidas.adapter = adapterRec
    }


    private fun cargarUsuarios() {
        mostrarCargando(true)

        db.collection("usuarios")
            .whereEqualTo("esAdministrador", !esAdmin)
            .get()
            .addOnSuccessListener { usuariosDocs ->

                listaDisponibles.clear()
                listaEnviadas.clear()
                listaRecibidas.clear()
                estadoSolicitudes.clear()

                val todos = mutableListOf<Usuario>()
                for (doc in usuariosDocs) {
                    val uid = doc.id
                    if (uid == uidActual) continue

                    todos += Usuario(
                        uid = uid,
                        nombre = doc.getString("nombre") ?: "",
                        apellido = doc.getString("apellido") ?: "",
                        esAdministrador = doc.getBoolean("esAdministrador") ?: false,
                        fotoPerfilBase64 = doc.getString("fotoPerfilBase64")
                    )
                }

                db.collection("solicitudes")
                    .get()
                    .addOnSuccessListener { solicitudesDocs ->

                        val pacientesVinculados = mutableSetOf<String>()
                        val pacientesConSolicitudPendienteDeOtro = mutableSetOf<String>()
                        val medicosQueMeEnviaronSolicitud = mutableSetOf<String>()

                        for (sol in solicitudesDocs) {
                            val emisorId = sol.getString("emisorId") ?: continue
                            val receptorId = sol.getString("receptorId") ?: continue
                            val estado = sol.getString("estado") ?: "pendiente"

                            if (estado == "aceptado") {
                                pacientesVinculados.add(emisorId)
                                pacientesVinculados.add(receptorId)
                            }

                            if (emisorId == uidActual) {
                                estadoSolicitudes[receptorId] = estado
                            }

                            if (receptorId == uidActual && estado == "pendiente") {
                                estadoSolicitudes[emisorId] = "recibida"
                                medicosQueMeEnviaronSolicitud.add(emisorId)
                            }

                            if (esAdmin && estado == "pendiente") {
                                if (emisorId != uidActual) {
                                    pacientesConSolicitudPendienteDeOtro.add(receptorId)
                                }

                                if (receptorId != uidActual) {
                                    pacientesConSolicitudPendienteDeOtro.add(emisorId)
                                }
                            }
                        }

                        for (usuario in todos) {
                            val estadoActual = estadoSolicitudes[usuario.uid]

                            if (esAdmin) {
                                when {
                                    pacientesVinculados.contains(usuario.uid) -> {
                                        continue
                                    }

                                    estadoActual == "recibida" -> {
                                        listaRecibidas.add(usuario) // ✅ Mostrar en recycler de recibidas
                                    }

                                    estadoActual == "pendiente" -> {
                                        listaEnviadas.add(usuario)
                                    }

                                    pacientesConSolicitudPendienteDeOtro.contains(usuario.uid) -> {
                                        estadoSolicitudes[usuario.uid] = "no_disponible"
                                        listaDisponibles.add(usuario)
                                    }

                                    else -> {
                                        listaDisponibles.add(usuario)
                                    }
                                }
                            } else {
                                when (estadoActual) {
                                    "recibida" -> listaRecibidas.add(usuario)
                                    "pendiente" -> listaEnviadas.add(usuario)
                                    "aceptado" -> {}
                                    else -> {
                                        if (medicosQueMeEnviaronSolicitud.isNotEmpty() &&
                                            !medicosQueMeEnviaronSolicitud.contains(usuario.uid)) {
                                            estadoSolicitudes[usuario.uid] = "no_disponible"
                                        }
                                        listaDisponibles.add(usuario)
                                    }
                                }
                            }
                        }

                        actualizarUI()
                        mostrarCargando(false)
                    }
            }
            .addOnFailureListener {
                mostrarCargando(false)
                toast("Error al cargar usuarios")
            }
    }


    private fun enviarSolicitud(receptorId: String) {
        val data = hashMapOf(
            "emisorId" to uidActual,
            "receptorId" to receptorId,
            "estado" to "pendiente"
        )
        db.collection("solicitudes").add(data)
            .addOnSuccessListener {
                toast("Solicitud enviada")
                cargarUsuarios()
            }
    }

    private fun cancelarSolicitud(receptorId: String) {
        db.collection("solicitudes")
            .whereEqualTo("emisorId", uidActual)
            .whereEqualTo("receptorId", receptorId)
            .get()
            .addOnSuccessListener { docs ->
                val batch = db.batch()
                docs.forEach { batch.delete(it.reference) }
                batch.commit().addOnSuccessListener {
                    toast("Solicitud cancelada")
                    cargarUsuarios()
                }
            }
    }

    private fun aceptarSolicitud(emisorId: String) {
        db.collection("solicitudes")
            .whereEqualTo("emisorId", emisorId)
            .whereEqualTo("receptorId", uidActual)
            .get()
            .addOnSuccessListener { docs ->
                val batch = db.batch()
                docs.forEach { batch.update(it.reference, "estado", "aceptado") }
                batch.commit().addOnSuccessListener {
                    toast("Solicitud aceptada")
                    cargarUsuarios()
                }
            }
    }

    private fun actualizarUI() {
        val hayRecibidas = listaRecibidas.isNotEmpty()
        binding.tvSolicitudesRecibidas.visibility = if (hayRecibidas) View.VISIBLE else View.GONE
        binding.rvSolicitudesRecibidas.visibility = if (hayRecibidas) View.VISIBLE else View.GONE
        binding.separadorRecibidas.visibility = if (hayRecibidas) View.VISIBLE else View.GONE
        adapterRec.notifyDataSetChanged()

        val hayEnviadas = listaEnviadas.isNotEmpty()
        binding.tvSolicitudesEnviadas.visibility = if (hayEnviadas) View.VISIBLE else View.GONE
        binding.rvSolicitudesEnviadas.visibility = if (hayEnviadas) View.VISIBLE else View.GONE
        binding.separadorEnviadas.visibility = if (hayEnviadas) View.VISIBLE else View.GONE
        adapterEnv.notifyDataSetChanged()

        val hayDisponibles = listaDisponibles.isNotEmpty()
        binding.rvUsuarios.visibility = if (hayDisponibles) View.VISIBLE else View.GONE
        adapterDisp.notifyDataSetChanged()

        binding.tvNoUsuarios.visibility =
            if (!hayRecibidas && !hayEnviadas && !hayDisponibles) View.VISIBLE else View.GONE
    }

    private fun mostrarCargando(show: Boolean) {
        binding.progressBarUsuarios.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
