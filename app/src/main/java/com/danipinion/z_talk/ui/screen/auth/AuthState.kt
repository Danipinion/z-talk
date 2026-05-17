package com.danipinion.z_talk.ui.screen.auth

sealed interface AuthState<out T> {
    object Idle : AuthState<Nothing>
    object Loading : AuthState<Nothing>
    data class Success<out T>(val data: T) : AuthState<T>
    data class Error(val message: String) : AuthState<Nothing>
}
