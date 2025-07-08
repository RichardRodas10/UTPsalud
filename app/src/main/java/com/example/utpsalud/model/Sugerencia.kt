package com.example.utpsalud.model

data class Sugerencia(
    val uid: String = "",
    val nombre: String = "",
    val mensaje: String = "",
    val timestamp: Long = System.currentTimeMillis()
)