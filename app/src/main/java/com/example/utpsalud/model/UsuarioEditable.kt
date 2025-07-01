package com.example.utpsalud.model

data class UsuarioEditable(
    val celular: String = "",
    val correo: String = "",
    val celularEmergencia: String = "",
    val esAdministrador: Boolean = false
)