package com.example.utpsalud.view.activity

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.utpsalud.R
import com.example.utpsalud.databinding.ActivityPerfilBinding
import com.example.utpsalud.viewmodel.PerfilViewModel
import com.example.utpsalud.model.UsuarioPerfil

class PerfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilBinding
    private val viewModel: PerfilViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.iconBack.setOnClickListener { finish() }

        val uid = intent.getStringExtra("uid") ?: ""

        // Observar cambios en el ViewModel
        observarViewModel()

        // Cargar usuario
        viewModel.cargarDatosUsuario(uid)
    }

    private fun observarViewModel() {
        viewModel.usuario.observe(this) { usuario ->
            mostrarUsuario(usuario)
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

        // Imagen de perfil
        if (!usuario.fotoPerfilBase64.isNullOrEmpty()) {
            val bytes = Base64.decode(usuario.fotoPerfilBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            binding.profileImage.setImageBitmap(bitmap)
        } else {
            binding.profileImage.setImageResource(R.drawable.ic_account)
        }

        // Contacto de emergencia solo para pacientes
        if (!usuario.esAdministrador && !usuario.celularEmergencia.isNullOrEmpty()) {
            binding.textCelularEmergencia.text = usuario.celularEmergencia
            binding.textContactoEmergencia.visibility = android.view.View.VISIBLE
            binding.contenedorContactoEm.visibility = android.view.View.VISIBLE
        } else {
            binding.textContactoEmergencia.visibility = android.view.View.GONE
            binding.contenedorContactoEm.visibility = android.view.View.GONE
        }

        // Llamadas
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
}