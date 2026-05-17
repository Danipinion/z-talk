package com.danipinion.z_talk.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danipinion.z_talk.data.remote.model.AuthRequest
import com.danipinion.z_talk.data.remote.model.AuthResponse
import com.danipinion.z_talk.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<AuthState<AuthResponse>>(AuthState.Idle)
    val loginState: StateFlow<AuthState<AuthResponse>> = _loginState

    private val _registerState = MutableStateFlow<AuthState<AuthResponse>>(AuthState.Idle)
    val registerState: StateFlow<AuthState<AuthResponse>> = _registerState

    private val _usernameAvailable = MutableStateFlow<Boolean?>(null)
    val usernameAvailable: StateFlow<Boolean?> = _usernameAvailable

    private val _isCheckingUsername = MutableStateFlow(false)
    val isCheckingUsername: StateFlow<Boolean> = _isCheckingUsername

    fun login(request: AuthRequest) {
        viewModelScope.launch {
            _loginState.value = AuthState.Loading
            try {
                val response = repository.login(request)
                if (response.isSuccessful && response.body()?.token != null) {
                    _loginState.value = AuthState.Success(response.body()!!)
                } else {
                    val errorMsg = response.body()?.error ?: "Login failed"
                    _loginState.value = AuthState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _loginState.value = AuthState.Error(e.message ?: "Network error")
            }
        }
    }

    fun register(request: AuthRequest) {
        viewModelScope.launch {
            _registerState.value = AuthState.Loading
            try {
                val response = repository.register(request)
                if (response.isSuccessful && response.body()?.token != null) {
                    _registerState.value = AuthState.Success(response.body()!!)
                } else {
                    val errorMsg = response.body()?.error ?: "Registration failed"
                    _registerState.value = AuthState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _registerState.value = AuthState.Error(e.message ?: "Network error")
            }
        }
    }

    fun checkUsername(username: String) {
        if (username.isEmpty()) {
            _usernameAvailable.value = null
            _isCheckingUsername.value = false
            return
        }
        viewModelScope.launch {
            _isCheckingUsername.value = true
            try {
                val response = repository.checkUsername(username)
                if (response.isSuccessful && response.body() != null) {
                    _usernameAvailable.value = response.body()?.available
                } else {
                    _usernameAvailable.value = null
                }
            } catch (e: Exception) {
                _usernameAvailable.value = null
            } finally {
                _isCheckingUsername.value = false
            }
        }
    }

    fun resetStates() {
        _loginState.value = AuthState.Idle
        _registerState.value = AuthState.Idle
        _usernameAvailable.value = null
        _isCheckingUsername.value = false
    }
}
