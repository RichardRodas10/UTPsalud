package com.example.utpsalud.model

data class ChatMessage(
    val emisorId: String = "",
    val receptorId: String = "",
    val mensaje: String = "",
    val timestamp: Long = 0L,
    var leido: Boolean = false
)