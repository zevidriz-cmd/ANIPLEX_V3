package com.aniplex.app.domain.model

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val pin: String? = null // Hashed PIN or null
)
