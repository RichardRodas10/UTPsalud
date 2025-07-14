package com.example.utpsalud.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.utpsalud.model.Medicion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MedicionesViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _estadoGuardado = MutableLiveData<EstadoGuardado>()
    val estadoGuardado: LiveData<EstadoGuardado> = _estadoGuardado

    sealed class EstadoGuardado {
        object Guardando : EstadoGuardado()
        object Exito : EstadoGuardado()
        data class Error(val error: String) : EstadoGuardado()
    }

    fun subirMedicion(frecuencia: Int, spo2: Int, temperatura: Float, fecha: String, hora: String) {
        val usuario = auth.currentUser
        if (usuario == null) {
            _estadoGuardado.postValue(EstadoGuardado.Error("Usuario no autenticado"))
            return
        }

        _estadoGuardado.postValue(EstadoGuardado.Guardando)

        obtenerEdadUsuario { edad ->
            if (edad == null) {
                _estadoGuardado.postValue(EstadoGuardado.Error("No se pudo obtener la edad del usuario"))
                return@obtenerEdadUsuario
            }

            val maxFC = 220 - edad

            val resultadoFC = when {
                frecuencia > maxFC * 0.85 -> "Alta"
                frecuencia >= maxFC * 0.5 -> "Normal"
                else -> "Baja"
            }

            val resultadoOxi = when (spo2) {
                in 95..100 -> "Normal"
                in 91..94 -> "Hipoxia leve"
                in 86..90 -> "Hipoxia moderada"
                else -> if (spo2 < 86) "Hipoxia severa" else "Valor no usual"
            }

            val estadoSalud = when {
                resultadoOxi == "Hipoxia severa" -> "Crítico"
                resultadoFC == "Baja" && (resultadoOxi == "Hipoxia moderada" || resultadoOxi == "Hipoxia severa") -> "Crítico"
                resultadoFC == "Baja" && resultadoOxi == "Hipoxia leve" -> "Moderado"
                resultadoFC == "Baja" && resultadoOxi == "Normal" -> "Moderado"
                resultadoFC == "Alta" && resultadoOxi == "Normal" -> "Moderado"
                resultadoFC == "Normal" && resultadoOxi != "Normal" -> "Moderado"
                resultadoFC == "Normal" && resultadoOxi == "Normal" -> "Saludable"
                else -> "Crítico"
            }

            val fechaLong = convertirFechaHoraALong(fecha, hora)

            val medicion = Medicion(
                temperatura = temperatura,
                frecuenciaCardiaca = frecuencia,
                oxigenoSangre = spo2,
                resultadoFrecuenciaCardiaca = resultadoFC,
                resultadoOxigeno = resultadoOxi,
                fechaMedicion = fechaLong,
                estadoSalud = estadoSalud
            )

            db.collection("usuarios")
                .document(usuario.uid)
                .collection("mediciones")
                .add(medicion)
                .addOnSuccessListener {
                    _estadoGuardado.postValue(EstadoGuardado.Exito)
                }
                .addOnFailureListener {
                    _estadoGuardado.postValue(
                        EstadoGuardado.Error("Error al guardar: ${it.message}")
                    )
                }
        }
    }

    private fun obtenerEdadUsuario(onResult: (Int?) -> Unit) {
        val usuario = auth.currentUser ?: return onResult(null)

        db.collection("usuarios").document(usuario.uid).get()
            .addOnSuccessListener { doc ->
                val fechaNacimientoStr = doc.getString("fechaNacimiento")
                val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fechaNacimientoDate = try {
                    formato.parse(fechaNacimientoStr ?: "")
                } catch (e: Exception) {
                    null
                }

                val edad = fechaNacimientoDate?.let { calcularEdad(it) }
                onResult(edad)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    private fun calcularEdad(fechaNacimiento: Date): Int {
        val hoy = Calendar.getInstance()
        val nacimiento = Calendar.getInstance().apply { time = fechaNacimiento }

        var edad = hoy.get(Calendar.YEAR) - nacimiento.get(Calendar.YEAR)
        if (hoy.get(Calendar.DAY_OF_YEAR) < nacimiento.get(Calendar.DAY_OF_YEAR)) {
            edad--
        }
        return edad
    }

    private fun convertirFechaHoraALong(fecha: String, hora: String): Long {
        return try {
            val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fechaCompleta = formato.parse("$fecha $hora")
            fechaCompleta?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
