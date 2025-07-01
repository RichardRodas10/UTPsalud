package com.example.utpsalud.view.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.utpsalud.databinding.ActivityEditardatosBinding
import com.example.utpsalud.model.UsuarioEditable
import com.example.utpsalud.viewmodel.EditardatosViewModel

class EditardatosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditardatosBinding
    private val viewModel: EditardatosViewModel by viewModels()

    private var originalUsuario: UsuarioEditable? = null
    private var esAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditardatosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.iconInfo.setOnClickListener { finish() }
        binding.btnCancelar.setOnClickListener { finish() }
        binding.btnContinuar.setOnClickListener { actualizarDatosUsuario() }

        observarViewModel()
        viewModel.cargarUsuario()
    }

    private fun observarViewModel() {
        viewModel.usuarioEditable.observe(this) { usuario ->
            originalUsuario = usuario
            esAdmin = usuario.esAdministrador
            cargarDatosEnUI(usuario)
            configurarTextWatchers()
            ocultarBotones()
        }

        viewModel.mensaje.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.finalizar.observe(this) { shouldFinish ->
            if (shouldFinish == true) finish()
        }
    }

    private fun cargarDatosEnUI(usuario: UsuarioEditable) {
        binding.editSugerencia.setText(usuario.celular)
        binding.editCorreo.setText(usuario.correo)

        if (usuario.esAdministrador) {
            binding.textEmergencia.visibility = android.view.View.GONE
            binding.contenedorEmergencia.visibility = android.view.View.GONE
        } else {
            binding.textEmergencia.visibility = android.view.View.VISIBLE
            binding.contenedorEmergencia.visibility = android.view.View.VISIBLE
            binding.editNumeroEmergencia.setText(usuario.celularEmergencia)
        }
    }

    private fun actualizarDatosUsuario() {
        val nuevo = UsuarioEditable(
            celular = binding.editSugerencia.text.toString().trim(),
            correo = binding.editCorreo.text.toString().trim(),
            celularEmergencia = binding.editNumeroEmergencia.text.toString().trim(),
            esAdministrador = esAdmin
        )
        viewModel.actualizarUsuario(nuevo)
    }

    private fun configurarTextWatchers() {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val nuevo = UsuarioEditable(
                    celular = binding.editSugerencia.text.toString().trim(),
                    correo = binding.editCorreo.text.toString().trim(),
                    celularEmergencia = binding.editNumeroEmergencia.text.toString().trim(),
                    esAdministrador = esAdmin
                )
                val huboCambios = originalUsuario?.let { viewModel.detectarCambios(it, nuevo) } ?: false

                if (huboCambios) {
                    binding.btnContinuar.visibility = android.view.View.VISIBLE
                    binding.btnCancelar.visibility = android.view.View.VISIBLE
                } else {
                    ocultarBotones()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        binding.editSugerencia.addTextChangedListener(watcher)
        binding.editCorreo.addTextChangedListener(watcher)
        if (!esAdmin) {
            binding.editNumeroEmergencia.addTextChangedListener(watcher)
        }
    }

    private fun ocultarBotones() {
        binding.btnContinuar.visibility = android.view.View.GONE
        binding.btnCancelar.visibility = android.view.View.GONE
    }
}