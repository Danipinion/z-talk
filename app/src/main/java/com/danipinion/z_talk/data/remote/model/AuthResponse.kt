package com.danipinion.z_talk.data.remote.model

data class AuthResponse(
    val message: String? = null,
    val token: String? = null,
    val user: UserInfo? = null,
    val error: String? = null
)

data class UserInfo(
    val id: String,
    val username: String,
    val avatar: String? = null,
    val mood: String? = null,
    val background: String? = null
)
