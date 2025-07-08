package com.example.utpsalud.view.activity

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.utpsalud.R
import com.example.utpsalud.databinding.ActivityPerfilBinding
import com.example.utpsalud.viewmodel.PerfilViewModel
import com.example.utpsalud.model.UsuarioPerfil
import com.google.android.material.snackbar.Snackbar

class PerfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilBinding
    private val viewModel: PerfilViewModel by viewModels()
    private var uidPerfil = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.iconBack.setOnClickListener { finish() }

        uidPerfil = intent.getStringExtra("uid") ?: ""

        observarViewModel()

        viewModel.cargarDatosUsuario(uidPerfil)

        binding.btnEstadoSolicitud.setOnClickListener {
            when (viewModel.estadoRelacion.value) {
                "vinculado" -> { /* no acción o toast opcional */ }
                "pendiente" -> viewModel.cancelarSolicitud(uidPerfil)
                "recibida" -> viewModel.aceptarSolicitud(uidPerfil)
                "no_disponible" -> mostrarMensajeNoDisponible()
                else -> viewModel.enviarSolicitud(uidPerfil) // "agregar" que llamaremos "vincular"
            }
        }

        binding.btnMensaje.setOnClickListener {
            val estado = viewModel.estadoRelacion.value ?: ""
            if (estado == "vinculado") {
                // Acción normal para enviar mensaje o abrir chat
                abrirChatConUsuario() // Puedes definir este método o la acción que corresponda
            } else {
                Snackbar.make(binding.root, "No puedes enviar mensaje en este estado", Snackbar.LENGTH_SHORT).show()
            }
        }

    }

    private fun observarViewModel() {
        viewModel.usuario.observe(this) { usuario ->
            mostrarUsuario(usuario)
        }

        viewModel.estadoRelacion.observe(this) { estado ->
            actualizarEstadoBoton(estado)
        }

        viewModel.mensaje.observe(this) { mensaje ->
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
        }

        viewModel.cerrarPantalla.observe(this) { cerrar ->
            if (cerrar == true) finish()
        }
    }

    private fun mostrarUsuario(usuario: UsuarioPerfil) {
        binding.textNombre.text = usuario.nombre
        binding.textApellido.text = usuario.apellido
        binding.textDni.text = usuario.dni
        binding.textCelular.text = usuario.celular
        binding.textEmail.text = usuario.correo

        if (!usuario.fotoPerfilBase64.isNullOrEmpty()) {
            val bytes = Base64.decode(usuario.fotoPerfilBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            binding.profileImage.setImageBitmap(bitmap)
        } else {
            binding.profileImage.setImageResource(R.drawable.ic_account)
        }

        if (!usuario.esAdministrador && !usuario.celularEmergencia.isNullOrEmpty()) {
            binding.textCelularEmergencia.text = usuario.celularEmergencia
            binding.textContactoEmergencia.visibility = android.view.View.VISIBLE
            binding.contenedorContactoEm.visibility = android.view.View.VISIBLE
        } else {
            binding.textContactoEmergencia.visibility = android.view.View.GONE
            binding.contenedorContactoEm.visibility = android.view.View.GONE
        }

        binding.textCelular.setOnClickListener {
            abrirLlamada(usuario.celular)
        }

        binding.textCelularEmergencia.setOnClickListener {
            usuario.celularEmergencia?.let { abrirLlamada(it) }
        }
    }

    private fun abrirLlamada(numero: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$numero")
        }
        startActivity(intent)
    }

    private fun actualizarEstadoBoton(estado: String) {
        val icNormal = getDrawable(R.drawable.ic_chat)      // Icono normal
        val icBloqueado = getDrawable(R.drawable.ic_block) // Icono bloqueado

        when (estado) {
            "pendiente" -> {
                binding.btnEstadoSolicitud.text = "Cancelar"
                binding.btnEstadoSolicitud.setBackgroundColor(getColor(R.color.gris))
                binding.btnDesvincular.visibility = View.GONE
                binding.btnMensaje.setBackgroundColor(getColor(R.color.gris))
                binding.btnMensaje.setCompoundDrawablesWithIntrinsicBounds(icBloqueado, null, null, null)
            }
            "recibida" -> {
                binding.btnEstadoSolicitud.text = "Confirmar"
                binding.btnEstadoSolicitud.setBackgroundColor(getColor(R.color.azul_marino))
                binding.btnDesvincular.visibility = View.GONE
                binding.btnMensaje.setBackgroundColor(getColor(R.color.gris))
                binding.btnMensaje.setCompoundDrawablesWithIntrinsicBounds(icBloqueado, null, null, null)
            }
            "vinculado" -> {
                binding.btnEstadoSolicitud.text = "Vinculado"
                binding.btnEstadoSolicitud.setBackgroundColor(Color.LTGRAY)
                binding.btnDesvincular.visibility = View.VISIBLE
                binding.btnMensaje.setBackgroundColor(getColor(R.color.button))
                binding.btnMensaje.setCompoundDrawablesWithIntrinsicBounds(icNormal, null, null, null)
            }
            "no_disponible" -> {
                binding.btnEstadoSolicitud.text = "No disponible"
                binding.btnEstadoSolicitud.setBackgroundColor(Color.LTGRAY)
                binding.btnDesvincular.visibility = View.GONE
                binding.btnMensaje.setBackgroundColor(getColor(R.color.gris))
                binding.btnMensaje.setCompoundDrawablesWithIntrinsicBounds(icBloqueado, null, null, null)
            }
            else -> { // agregar
                binding.btnEstadoSolicitud.text = "Vincular"
                binding.btnEstadoSolicitud.setBackgroundColor(getColor(R.color.azul_marino))
                binding.btnDesvincular.visibility = View.GONE
                binding.btnMensaje.setBackgroundColor(getColor(R.color.gris))
                binding.btnMensaje.setCompoundDrawablesWithIntrinsicBounds(icBloqueado, null, null, null)
            }
        }
    }

    private fun abrirChatConUsuario() {
        val uidUsuario = uidPerfil // ya tienes el uid del perfil cargado
        if (uidUsuario.isEmpty()) {
            Toast.makeText(this, "Usuario no válido para chat", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("uid", uidUsuario)
        intent.putExtra("nombre", viewModel.usuario.value?.nombre ?: "")
        intent.putExtra("apellido", viewModel.usuario.value?.apellido ?: "")
        intent.putExtra("fotoPerfilBase64", viewModel.usuario.value?.fotoPerfilBase64)
        startActivity(intent)
    }

    private fun mostrarMensajeNoDisponible() {
        val esAdmin = viewModel.usuario.value?.esAdministrador ?: false
        val mensaje = if (esAdmin) {
            "No disponible."
        } else {
            "No disponible."
        }
        Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_LONG).show()
    }
}