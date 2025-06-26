package com.example.utpsalud.model

data class Usuario(
    val nombre: String = "",
    val apellido: String = "",
    val fotoPerfilBase64: String? = null,
    val esAdministrador: Boolean = false
)
