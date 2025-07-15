package com.example.utpsalud.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.utpsalud.model.MedicionManual
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HistorialViewModel : ViewModel() {

    private val _mediciones = MutableLiveData<List<MedicionManual>>()
    val mediciones: LiveData<List<MedicionManual>> get() = _mediciones

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Obtener lista de días con mediciones filtrando por mes y año
    fun obtenerMediciones(mes: Int, anio: Int) {
        val user = auth.currentUser ?: return

        db.collection("usuarios")
            .document(user.uid)
            .collection("mediciones")
            .get()
            .addOnSuccessListener { snapshot ->
                val listaFiltrada = snapshot.documents.mapNotNull { it.toObject(MedicionManual::class.java) }
                    .filter {
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = it.fechaMedicion
                        }
                        calendar.get(Calendar.MONTH) == mes && calendar.get(Calendar.YEAR) == anio
                    }
                    .groupBy { fechaFormateada(it.fechaMedicion) }
                    .map { it.value.first() }
                    .sortedByDescending { it.fechaMedicion }

                _mediciones.postValue(listaFiltrada)
            }
            .addOnFailureListener {
                _mediciones.postValue(emptyList())
            }
    }

    // Obtener todas las mediciones de un día específico (fechaExacta es timestamp en millis)
    fun obtenerMedicionesPorDia(fechaExacta: Long) {
        val user = auth.currentUser ?: return

        // Obtener mediciones que coincidan con la fechaExacta (mismo día)
        db.collection("usuarios")
            .document(user.uid)
            .collection("mediciones")
            .get()
            .addOnSuccessListener { snapshot ->
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fechaBuscada = sdf.format(Date(fechaExacta))

                val listaFiltrada = snapshot.documents.mapNotNull { it.toObject(MedicionManual::class.java) }
                    .filter {
                        val fechaMed = sdf.format(Date(it.fechaMedicion))
                        fechaMed == fechaBuscada
                    }
                    .sortedBy { it.fechaMedicion } // ordenar por hora ascendente

                _mediciones.postValue(listaFiltrada)
            }
            .addOnFailureListener {
                _mediciones.postValue(emptyList())
            }
    }


    // -------- Nuevas funciones para historial de cualquier paciente --------

    fun obtenerMedicionesPorUsuario(uidPaciente: String, mes: Int, anio: Int) {
        db.collection("usuarios")
            .document(uidPaciente)
            .collection("mediciones")
            .get()
            .addOnSuccessListener { snapshot ->
                val listaFiltrada = snapshot.documents.mapNotNull { it.toObject(MedicionManual::class.java) }
                    .filter {
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = it.fechaMedicion
                        }
                        calendar.get(Calendar.MONTH) == mes && calendar.get(Calendar.YEAR) == anio
                    }
                    .groupBy { fechaFormateada(it.fechaMedicion) }
                    .map { it.value.first() }
                    .sortedByDescending { it.fechaMedicion }

                _mediciones.postValue(listaFiltrada)
            }
            .addOnFailureListener {
                _mediciones.postValue(emptyList())
            }
    }

    fun obtenerMedicionesPorUsuarioPorDia(uidPaciente: String, fechaExacta: Long) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaBuscada = sdf.format(Date(fechaExacta))

        db.collection("usuarios")
            .document(uidPaciente)
            .collection("mediciones")
            .get()
            .addOnSuccessListener { snapshot ->
                val listaFiltrada = snapshot.documents.mapNotNull { it.toObject(MedicionManual::class.java) }
                    .filter {
                        val fechaMed = sdf.format(Date(it.fechaMedicion))
                        fechaMed == fechaBuscada
                    }
                    .sortedBy { it.fechaMedicion }

                _mediciones.postValue(listaFiltrada)
            }
            .addOnFailureListener {
                _mediciones.postValue(emptyList())
            }
    }

    private fun fechaFormateada(timestamp: Long): String {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return formato.format(Date(timestamp))
    }
}