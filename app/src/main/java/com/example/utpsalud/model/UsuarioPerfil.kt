package com.example.utpsalud.model

data class UsuarioPerfil(
    val nombre: String = "",
    val apellido: String = "",
    val dni: String = "",
    val celular: String = "",
    val correo: String = "",
    val celularEmergencia: String? = null,
    val fotoPerfilBase64: String? = null,
    val esAdministrador: Boolean = false
)