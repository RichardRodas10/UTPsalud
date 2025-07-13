package com.example.utpsalud.model

data class MedicionManual(
    val frecuenciaCardiaca: Int = 0,
    val oxigenoSangre: Int = 0,
    val resultadoFrecuenciaCardiaca: String = "",
    val resultadoOxigeno: String = "",
    val fechaMedicion: Long = 0L,
    val estadoSalud: String = ""
)