package com.example.utpsalud.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeFragmentViewModel : ViewModel() {

    private val _mostrarInstrucciones = MutableLiveData<Boolean>()
    val mostrarInstrucciones: LiveData<Boolean> get() = _mostrarInstrucciones

    fun pedirMostrarInstrucciones() {
        _mostrarInstrucciones.value = true
    }

    fun instruccionesMostradas() {
        _mostrarInstrucciones.value = false
    }
}