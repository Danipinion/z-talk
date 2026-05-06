package com.danipinion.z_talk.data.remote

import com.danipinion.z_talk.data.remote.model.UserResponse
import retrofit2.http.GET

interface ApiService {
    @GET("users")
    suspend fun getUsers(): List<UserResponse>
}
