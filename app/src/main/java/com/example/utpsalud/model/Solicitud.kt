package com.example.utpsalud.model

data class Solicitud(
    val emisorId: String = "",
    val receptorId: String = "",
    val estado: String = "pendiente" // "pendiente", "aceptado", "cancelado"
)