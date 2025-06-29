package com.example.utpsalud

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.utpsalud.adapter.UsuarioAdapter
import com.example.utpsalud.databinding.ActivityBuscarBinding
import com.example.utpsalud.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class BuscarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBuscarBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var esAdmin: Boolean = false
    private lateinit var uidActual: String
    private val listaUsuarios = mutableListOf<Usuario>()
    private val estadoSolicitudes = mutableMapOf<String, String>()
    private lateinit var adapter: UsuarioAdapter
    private var ultimaBusqueda = ""
    private val handler = Handler()
    private var runnableBusqueda: Runnable? = null
    private var solicitudesListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBuscarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        uidActual = auth.currentUser?.uid ?: ""

        binding.iconInfo.setOnClickListener { finish() }
        binding.progressBarBuscar.visibility = View.GONE

        obtenerRolUsuarioActual {
            configurarAdapter()
            configurarBusqueda()
            escucharSolicitudesTiempoReal()
        }

        binding.editBuscar.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = binding.editBuscar.compoundDrawables[2]
                drawableEnd?.let {
                    val bounds: Rect = it.bounds
                    val x = event.rawX.toInt()
                    val editTextRight = binding.editBuscar.right
                    val drawableWidth = bounds.width()
                    if (x >= (editTextRight - drawableWidth - binding.editBuscar.paddingEnd)) {
                        binding.editBuscar.text?.clear()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        solicitudesListener?.remove()
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

    private fun configurarAdapter() {
        adapter = UsuarioAdapter(
            listaUsuarios,
            estadoSolicitudes,
            uidActual,
            esAdmin,
            onAgregar = { usuario -> enviarSolicitud(usuario.uid) },
            onCancelar = { usuario -> cancelarSolicitud(usuario.uid) },
            onConfirmar = { usuario -> aceptarSolicitud(usuario.uid) },
            onClickItem = { usuario ->
                val intent = Intent(this, PerfilActivity::class.java)
                intent.putExtra("uid", usuario.uid)
                startActivity(intent)
            }
        )
        binding.rvResultados.layoutManager = LinearLayoutManager(this)
        binding.rvResultados.adapter = adapter
    }


    private fun configurarBusqueda() {
        binding.editBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val texto = s.toString().trim()
                val icon = if (texto.isNotEmpty()) R.drawable.ic_clear else 0
                binding.editBuscar.setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0)

                if (texto.isEmpty()) {
                    limpiarResultados()
                    return
                }

                runnableBusqueda?.let { handler.removeCallbacks(it) }
                runnableBusqueda = Runnable {
                    if (texto.length >= 2) buscarUsuarios(texto) else limpiarResultados()
                }
                handler.postDelayed(runnableBusqueda!!, 300)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun limpiarResultados() {
        listaUsuarios.clear()
        estadoSolicitudes.clear()
        adapter.notifyDataSetChanged()
        binding.rvResultados.visibility = View.GONE
        binding.tvNoResultados.visibility = View.GONE
        binding.progressBarBuscar.visibility = View.GONE
    }

    private fun escucharSolicitudesTiempoReal() {
        solicitudesListener = db.collection("solicitudes")
            .addSnapshotListener { _, _ ->
                val texto = binding.editBuscar.text.toString().trim()
                if (texto.length >= 2) buscarUsuarios(texto)
            }
    }

    private fun buscarUsuarios(texto: String) {
        val textoNormalizado = texto.trim().lowercase()
        ultimaBusqueda = textoNormalizado

        binding.progressBarBuscar.visibility = View.VISIBLE
        binding.rvResultados.visibility = View.GONE
        binding.tvNoResultados.visibility = View.GONE

        db.collection("usuarios")
            .whereEqualTo("esAdministrador", !esAdmin)
            .get()
            .addOnSuccessListener { documentos ->
                if (textoNormalizado != ultimaBusqueda) {
                    binding.progressBarBuscar.visibility = View.GONE
                    return@addOnSuccessListener
                }

                listaUsuarios.clear()
                estadoSolicitudes.clear()
                adapter.notifyDataSetChanged()

                val candidatos = documentos.mapNotNull { doc ->
                    val uid = doc.id
                    if (uid == uidActual) return@mapNotNull null

                    val nombre = doc.getString("nombre") ?: ""
                    val apellido = doc.getString("apellido") ?: ""
                    val nombreCompleto = "$nombre $apellido".lowercase()
                    val palabras = textoNormalizado.split(" ").filter { it.isNotBlank() }
                    val palabrasNombre = nombreCompleto.split(" ").filter { it.isNotBlank() }

                    val coincide = palabras.all { palabraBuscada ->
                        palabraBuscada.length >= 3 &&
                                palabrasNombre.any { it.startsWith(palabraBuscada) }
                    }

                    if (coincide) {
                        Usuario(
                            uid = uid,
                            nombre = nombre,
                            apellido = apellido,
                            esAdministrador = doc.getBoolean("esAdministrador") ?: false,
                            fotoPerfilBase64 = doc.getString("fotoPerfilBase64")
                        )
                    } else null
                }

                db.collection("solicitudes")
                    .get()
                    .addOnSuccessListener { solicitudes ->
                        if (textoNormalizado != ultimaBusqueda) {
                            binding.progressBarBuscar.visibility = View.GONE
                            return@addOnSuccessListener
                        }

                        val pacientesVinculados = mutableSetOf<String>()
                        val pacientesConSolicitudPendienteDeOtro = mutableSetOf<String>()
                        val medicosQueMeEnviaronSolicitud = mutableSetOf<String>()

                        for (sol in solicitudes) {
                            val emisorId = sol.getString("emisorId") ?: continue
                            val receptorId = sol.getString("receptorId") ?: continue
                            val estado = sol.getString("estado") ?: "pendiente"

                            if (estado == "aceptado") {
                                if (uidActual == emisorId || uidActual == receptorId) {
                                    // Está vinculado conmigo
                                    estadoSolicitudes[if (uidActual == emisorId) receptorId else emisorId] = "aceptado"
                                } else {
                                    // Está vinculado con otro médico
                                    pacientesVinculados.add(emisorId)
                                    pacientesVinculados.add(receptorId)
                                }
                            }

                            if (emisorId == uidActual) estadoSolicitudes[receptorId] = estado
                            if (receptorId == uidActual && estado == "pendiente") {
                                estadoSolicitudes[emisorId] = "recibida"
                                medicosQueMeEnviaronSolicitud.add(emisorId)
                            }

                            if (esAdmin && estado == "pendiente") {
                                if (emisorId != uidActual) pacientesConSolicitudPendienteDeOtro.add(receptorId)
                                if (receptorId != uidActual) pacientesConSolicitudPendienteDeOtro.add(emisorId)
                            }
                        }

                        for (usuario in candidatos) {
                            val uid = usuario.uid
                            val estadoActual = estadoSolicitudes[uid]

                            if (esAdmin) {
                                when {
                                    estadoActual == "aceptado" -> {} // ya se asignó correctamente arriba
                                    pacientesVinculados.contains(uid) -> estadoSolicitudes[uid] = "no_disponible"
                                    estadoActual == "recibida" || estadoActual == "pendiente" -> {}
                                    pacientesConSolicitudPendienteDeOtro.contains(uid) -> estadoSolicitudes[uid] = "no_disponible"
                                }
                            } else {
                                when (estadoActual) {
                                    "recibida", "pendiente", "aceptado" -> {}
                                    else -> {
                                        if (medicosQueMeEnviaronSolicitud.isNotEmpty() &&
                                            !medicosQueMeEnviaronSolicitud.contains(uid)) {
                                            estadoSolicitudes[uid] = "no_disponible"
                                        }
                                        if (pacientesVinculados.contains(uidActual)) {
                                            estadoSolicitudes[uid] = "no_disponible"
                                        }
                                    }
                                }
                            }

                            if (listaUsuarios.none { it.uid == uid }) {
                                listaUsuarios.add(usuario)
                            }
                        }

                        adapter.notifyDataSetChanged()
                        binding.progressBarBuscar.visibility = View.GONE

                        if (listaUsuarios.isEmpty()) {
                            binding.rvResultados.visibility = View.GONE
                            binding.tvNoResultados.visibility = View.VISIBLE
                        } else {
                            binding.rvResultados.visibility = View.VISIBLE
                            binding.tvNoResultados.visibility = View.GONE
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
        db.collection("solicitudes").add(solicitud)
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