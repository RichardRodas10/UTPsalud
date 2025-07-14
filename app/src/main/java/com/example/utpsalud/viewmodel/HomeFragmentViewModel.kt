package com.example.utpsalud.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.utpsalud.model.Medicion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragmentViewModel : ViewModel() {

    private val _mostrarInstrucciones = MutableLiveData<Boolean>()
    val mostrarInstrucciones: LiveData<Boolean> get() = _mostrarInstrucciones
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _ultimaMedicion = MutableLiveData<Medicion?>()
    val ultimaMedicion: LiveData<Medicion?> = _ultimaMedicion

    init { cargarUltimaMedicion() }

    fun cargarUltimaMedicion() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("usuarios").document(uid)
            .collection("mediciones")
            .orderBy("fechaMedicion", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snaps ->
                val doc = snaps.documents.firstOrNull()
                _ultimaMedicion.postValue(doc?.toObject(Medicion::class.java))
            }
            .addOnFailureListener {
                _ultimaMedicion.postValue(null)
            }
    }

    fun pedirMostrarInstrucciones() {
        _mostrarInstrucciones.value = true
    }

    fun instruccionesMostradas() {
        _mostrarInstrucciones.value = false
    }


}