package com.danipinion.z_talk.data.repository

import com.danipinion.z_talk.data.remote.ApiService
import com.danipinion.z_talk.data.remote.model.AuthRequest
import com.danipinion.z_talk.data.remote.model.AuthResponse
import com.danipinion.z_talk.data.remote.UsernameCheckResponse
import retrofit2.Response

class AuthRepository(private val apiService: ApiService) {
    suspend fun login(request: AuthRequest): Response<AuthResponse> = 
        apiService.login(request)

    suspend fun register(request: AuthRequest): Response<AuthResponse> = 
        apiService.register(request)

    suspend fun checkUsername(username: String): Response<UsernameCheckResponse> = 
        apiService.checkUsername(username)
}
