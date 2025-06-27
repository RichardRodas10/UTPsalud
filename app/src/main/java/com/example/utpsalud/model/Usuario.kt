package com.example.utpsalud.model

data class Usuario(
    val uid: String = "",
    val nombre: String = "",
    val apellido: String = "",
    val fotoPerfilBase64: String? = null,
    val esAdministrador: Boolean = false,
    var estadoSolicitud: String? = null // puede ser "enviada", "recibida", "confirmada", null
)
