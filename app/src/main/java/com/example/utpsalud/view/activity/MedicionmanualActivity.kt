package com.example.utpsalud.view.activity

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.utpsalud.R
import com.example.utpsalud.databinding.ActivityMedicionmanualBinding
import com.example.utpsalud.viewmodel.MedicionmanualViewModel
import com.google.android.material.snackbar.Snackbar

class MedicionmanualActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMedicionmanualBinding
    private val viewModel: MedicionmanualViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicionmanualBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ocultar elementos al iniciar
        binding.contenedorResultado.visibility = View.GONE
        binding.tvEstadoSalud.visibility = View.GONE
        binding.tvMensajeEstadoSalud.visibility = View.GONE
        binding.containerInfoConectado.visibility = View.GONE

        binding.iconBack.setOnClickListener { finish() }

        viewModel.estadoGuardado.observe(this) { estado ->
            when (estado) {
                is MedicionmanualViewModel.EstadoGuardado.Guardando -> {
                    binding.btnInstrucciones.isEnabled = false
                    binding.btnInstrucciones.text = "Analizando..."
                    binding.contenedorResultado.visibility = View.GONE
                    binding.tvEstadoSalud.visibility = View.GONE
                    binding.tvMensajeEstadoSalud.visibility = View.GONE
                    binding.containerInfoConectado.visibility = View.GONE
                }

                is MedicionmanualViewModel.EstadoGuardado.Exito -> {
                    binding.btnInstrucciones.isEnabled = true
                    binding.btnInstrucciones.text = "Registrar medición"

                    binding.tvFrecuenciaCardiaca.text = "Frecuencia cardiaca: ${estado.resultadoFrecuenciaCardiaca}"
                    binding.tvOxigenoSangre.text = "Oxígeno en sangre: ${estado.resultadoOxigeno}"

                    binding.tvEstadoSalud.text = "Estado de salud: ${estado.estadoSalud}"
                    binding.tvEstadoSalud.setTypeface(null, Typeface.BOLD)

                    // Color del estado de salud
                    val color = when (estado.estadoSalud) {
                        "Crítico" -> Color.RED
                        "Moderado" -> 0xFFFFA500.toInt() // Naranja
                        "Saludable" -> ContextCompat.getColor(this, R.color.green)
                        else -> Color.BLACK
                    }
                    binding.tvEstadoSalud.setTextColor(color)

                    // Mostrar mensaje informativo según estado
                    val mensajeEstado = when (estado.estadoSalud) {
                        "Saludable" -> "¡Buen trabajo! Tus signos vitales están en un rango saludable. Continúa con tu estilo de vida saludable."
                        "Crítico" -> "Tus signos vitales están en un rango crítico. Busca atención médica inmediata."
                        "Moderado" -> "Uno o ambos valores están fuera del rango ideal. Se recomienda descanso o consultar a un profesional."
                        else -> null
                    }

                    if (mensajeEstado != null) {
                        binding.tvMensajeEstadoSalud.text = mensajeEstado
                        binding.tvMensajeEstadoSalud.visibility = View.VISIBLE
                    } else {
                        binding.tvMensajeEstadoSalud.visibility = View.GONE
                    }

                    // Mostrar u ocultar información adicional si hay hipoxia
                    if (estado.resultadoOxigeno.contains("Hipoxia", ignoreCase = true)) {
                        binding.containerInfoConectado.visibility = View.VISIBLE
                    } else {
                        binding.containerInfoConectado.visibility = View.GONE
                    }

                    binding.contenedorResultado.visibility = View.VISIBLE
                    binding.tvEstadoSalud.visibility = View.VISIBLE

                    binding.editFrecuenciCardiaca.text?.clear()
                    binding.editOxigenoSangre.text?.clear()
                    binding.editTemperatura.text?.clear()
                }

                is MedicionmanualViewModel.EstadoGuardado.Error -> {
                    binding.btnInstrucciones.isEnabled = true
                    binding.btnInstrucciones.text = "Registrar medición"
                    binding.contenedorResultado.visibility = View.GONE
                    binding.tvEstadoSalud.visibility = View.GONE
                    binding.tvMensajeEstadoSalud.visibility = View.GONE
                    binding.containerInfoConectado.visibility = View.GONE
                    Snackbar.make(binding.root, estado.error, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        binding.btnInstrucciones.setOnClickListener {
            ocultarTeclado()

            val frecStr = binding.editFrecuenciCardiaca.text?.toString()
            val oxiStr = binding.editOxigenoSangre.text?.toString()
            val tempStr = binding.editTemperatura.text?.toString()

            when {
                frecStr.isNullOrBlank() -> {
                    Snackbar.make(binding.root, "Por favor ingresa la frecuencia cardiaca", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                oxiStr.isNullOrBlank() -> {
                    Snackbar.make(binding.root, "Por favor ingresa el oxígeno en sangre", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                tempStr.isNullOrBlank() -> {
                    Snackbar.make(binding.root, "Por favor ingresa la temperatura", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                frecStr.toIntOrNull() == null || frecStr.toInt() < 30 || frecStr.toInt() > 220 -> {
                    Snackbar.make(binding.root, "Ingresa una frecuencia cardiaca válida (30-220)", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                oxiStr.toIntOrNull() == null || oxiStr.toInt() <= 0 || oxiStr.toInt() > 100 -> {
                    Snackbar.make(binding.root, "Ingresa un oxígeno en sangre válido (0-100)", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                tempStr.toFloatOrNull() == null || tempStr.toFloat() < 30f || tempStr.toFloat() > 45f -> {
                    Snackbar.make(binding.root, "Ingresa una temperatura válida (30.0 - 45.0 °C)", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                else -> {
                    val frec = frecStr.toInt()
                    val oxi = oxiStr.toInt()
                    val temp = tempStr.toFloat()
                    viewModel.guardarMedicion(frec, oxi, temp)
                }
            }

        }
    }

    private fun ocultarTeclado() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { view ->
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}