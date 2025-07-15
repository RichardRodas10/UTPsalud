package com.example.utpsalud.view.activity

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.utpsalud.databinding.ActivityRegisterBinding
import com.example.utpsalud.viewmodel.RegisterViewModel
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class RegisterActivity : AppCompatActivity() {

    // Para usar ViewBinding y acceder fácil a los views
    private lateinit var binding: ActivityRegisterBinding
    // ViewModel para separar la lógica y manejar estados de registro
    private val viewModel: RegisterViewModel by viewModels()
    // Aquí guardaré la foto de perfil codificada en base64 para enviar al backend
    private var profileImageBase64: String = ""

    // Registro para el launcher que abre la galería y me devuelve la imagen seleccionada
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Obtengo el bitmap de la imagen seleccionada para mostrar y convertir
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
            binding.profileImage.setImageBitmap(bitmap)
            profileImageBase64 = viewModel.bitmapToBase64(bitmap) // lo convierto a base64 para enviar
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflar la vista con ViewBinding
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configuro el ícono de la cámara para abrir galería y elegir foto
        binding.cameraIcon.setOnClickListener {
            pickImageLauncher.launch("image/*") // solo imágenes
        }

        // Cuando el switch cambia, muestro u oculto campos según si es admin o no
        binding.switch1.setOnCheckedChangeListener { _, isChecked ->
            toggleCampos(isChecked)
        }

        // Para el campo de fecha, muestro un DatePicker para que el usuario elija
        binding.etFechaEmision.setOnClickListener {
            mostrarDatePicker()
        }

        // Cuando presionan el botón registrar, recojo datos y se los paso al ViewModel para validar y registrar
        binding.btnRegister.setOnClickListener {
            ocultarTeclado(it)

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
            val fechaNacimiento = binding.etFechaNacimiento.text.toString().trim()

            // Solo paso los campos de admin si el switch está activado (esAdmin)
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
                colegAdmin.takeIf { esAdmin },         // si no es admin, será null
                fechaEmision.takeIf { esAdmin },        // idem
                profileImageBase64.takeIf { profileImageBase64.isNotEmpty() }, // solo si hay imagen
                fechaNacimiento
                )
        }

        // Link para volver a login si ya tengo cuenta
        binding.loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // cierro este activity para no poder volver atrás con botón
        }

        // Empiezo a observar los cambios de estado del ViewModel para reaccionar
        observarViewModel()

        binding.etFechaNacimiento.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, y, m, d ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(y, m, d)
                val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.etFechaNacimiento.setText(formato.format(selectedCalendar.time))
            }, year, month, day)

            // Limitar el rango de fechas: entre hoy - 120 años y hoy - 18 años
            val hoy = Calendar.getInstance()
            val minFecha = Calendar.getInstance().apply { add(Calendar.YEAR, -120) } // máx 120 años
            val maxFecha = Calendar.getInstance().apply { add(Calendar.YEAR, -18) }   // mínimo 18 años

            datePicker.datePicker.minDate = minFecha.timeInMillis
            datePicker.datePicker.maxDate = maxFecha.timeInMillis

            // Mostrar primero el selector de año
            try {
                datePicker.datePicker.touchables[0].performClick()
            } catch (_: Exception) {
            }

            datePicker.show()
        }
    }

    // Esta función me muestra u oculta campos extra para admin según switch
    private fun toggleCampos(esAdmin: Boolean) {
        binding.adminFields.visibility = if (esAdmin) View.VISIBLE else View.GONE
        binding.etCelularEmergencia.visibility = if (esAdmin) View.GONE else View.VISIBLE
    }

    // Muestra un DatePicker para que el usuario elija la fecha y la pongo en el EditText
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

    // Aquí escucho los cambios en el estado del registro para mostrar mensajes o cambiar pantalla
    private fun observarViewModel() {
        viewModel.registroEstado.observe(this) { estado ->
            when (estado) {
                is RegisterViewModel.RegistroEstado.Success -> {
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Registrar"
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        putExtra("registro_exitoso", true)
                    }
                    startActivity(intent)
                    finish()
                }
                is RegisterViewModel.RegistroEstado.Error -> {
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Registrar"
                    Snackbar.make(binding.root, estado.mensaje, Snackbar.LENGTH_LONG).show()
                }
                RegisterViewModel.RegistroEstado.Loading -> {
                    binding.btnRegister.isEnabled = false
                    binding.btnRegister.text = "Registrando..."
                }
            }
        }
    }

    // Función para ocultar el teclado, la uso cuando ya no necesito que esté visible
    private fun ocultarTeclado(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}