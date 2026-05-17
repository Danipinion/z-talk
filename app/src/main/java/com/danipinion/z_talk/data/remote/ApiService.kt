package com.danipinion.z_talk.data.remote

import com.danipinion.z_talk.data.remote.model.AuthRequest
import com.danipinion.z_talk.data.remote.model.AuthResponse
import com.danipinion.z_talk.data.remote.model.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("users")
    suspend fun getUsers(): List<UserResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: AuthRequest): Response<AuthResponse>
}
