package com.example.utpsalud.model

sealed class ChatItem {
    data class Mensaje(val chatMessage: ChatMessage) : ChatItem()
    data class Encabezado(val texto: String) : ChatItem()
}