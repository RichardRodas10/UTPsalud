package com.example.utpsalud.view

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.utpsalud.databinding.ActivityRegisterBinding
import com.example.utpsalud.viewmodel.RegisterViewModel
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()
    private var profileImageBase64: String = ""

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
            binding.profileImage.setImageBitmap(bitmap)
            profileImageBase64 = viewModel.bitmapToBase64(bitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Abrir galería
        binding.cameraIcon.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Mostrar u ocultar campos según el tipo de usuario
        binding.switch1.setOnCheckedChangeListener { _, isChecked ->
            toggleCampos(isChecked)
        }

        // Selector de fecha
        binding.etFechaEmision.setOnClickListener {
            mostrarDatePicker()
        }

        // Botón Registrar
        binding.btnRegister.setOnClickListener {
            val nombre = binding.etNombre.text.toString().trim()
            val apellido = binding.etApellido.text.toString().trim()
            val dni = binding.etDNI.text.toString().trim()
            val celular = binding.etCelular.text.toString().trim()
            val celularEmergencia = binding.editCelularEmergencia.text.toString().trim()
            val email = binding.etRegisterEmail.text.toString().trim()
            val password = binding.etRegisterPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()
            val esAdmin = binding.switch1.isChecked
            val colegAdmin = binding.etColegiatura.text.toString().trim()
            val fechaEmision = binding.etFechaEmision.text.toString().trim()

            viewModel.validarYRegistrar(
                nombre,
                apellido,
                dni,
                celular,
                celularEmergencia,
                email,
                password,
                confirmPassword,
                esAdmin,
                colegAdmin.takeIf { esAdmin },
                fechaEmision.takeIf { esAdmin },
                profileImageBase64.takeIf { profileImageBase64.isNotEmpty() }
            )
        }

        // Login Link
        binding.loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        observarViewModel()
    }

    private fun toggleCampos(esAdmin: Boolean) {
        binding.adminFields.visibility = if (esAdmin) View.VISIBLE else View.GONE
        binding.etCelularEmergencia.visibility = if (esAdmin) View.GONE else View.VISIBLE
    }

    private fun mostrarDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, y, m, d ->
                val sel = Calendar.getInstance().apply { set(y, m, d) }
                val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.etFechaEmision.setText(formato.format(sel.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun observarViewModel() {
        viewModel.registroEstado.observe(this) { estado ->
            when (estado) {
                is RegisterViewModel.RegistroEstado.Success -> {
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        putExtra("registro_exitoso", true)
                    }
                    startActivity(intent)
                    finish()
                }
                is RegisterViewModel.RegistroEstado.Error -> {
                    Snackbar.make(binding.root, estado.mensaje, Snackbar.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }
}