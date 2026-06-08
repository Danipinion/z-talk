package com.danipinion.z_talk.data.remote

import com.danipinion.z_talk.data.remote.model.AuthRequest
import com.danipinion.z_talk.data.remote.model.AuthResponse
import com.danipinion.z_talk.data.remote.model.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Header
import retrofit2.http.Query

interface ApiService {
    @GET("users")
    suspend fun getUsers(): List<UserResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: AuthRequest): Response<AuthResponse>

    @GET("api/auth/check-username/{username}")
    suspend fun checkUsername(@Path("username") username: String): Response<UsernameCheckResponse>

    @POST("api/friends/request")
    suspend fun sendFriendRequest(
        @Header("Authorization") token: String,
        @Body request: SendFriendRequestPayload
    ): Response<GenericResponse>

    @GET("api/friends/requests")
    suspend fun getFriendRequests(
        @Header("Authorization") token: String
    ): Response<List<FriendRequestResponse>>

    @POST("api/friends/respond")
    suspend fun respondToFriendRequest(
        @Header("Authorization") token: String,
        @Body request: RespondRequestPayload
    ): Response<GenericResponse>

    @POST("api/friends/add-direct")
    suspend fun addFriendDirectly(
        @Header("Authorization") token: String,
        @Body request: AddDirectPayload
    ): Response<GenericResponse>

    @GET("api/friends/list")
    suspend fun getFriends(
        @Header("Authorization") token: String
    ): Response<List<FriendResponse>>

    @GET("api/friends/search")
    suspend fun searchUsers(
        @Header("Authorization") token: String,
        @Query("q") query: String
    ): Response<List<SearchUserResponse>>

    @GET("api/auth/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): Response<UserProfileResponse>

    @PUT("api/auth/profile")
    suspend fun updateAvatar(
        @Header("Authorization") token: String,
        @Body payload: UpdateAvatarPayload
    ): Response<GenericResponse>
}

data class UsernameCheckResponse(
    val available: Boolean
)

data class SendFriendRequestPayload(
    val receiverUsername: String
)

data class RespondRequestPayload(
    val senderId: String,
    val accept: Boolean
)

data class AddDirectPayload(
    val friendId: String
)

data class GenericResponse(
    val message: String? = null,
    val error: String? = null
)

data class FriendRequestResponse(
    val senderId: String,
    val senderUsername: String,
    val status: String,
    val createdAt: Long,
    val senderAvatar: String? = null,
    val senderMood: String? = null,
    val senderBackground: String? = null
)

data class FriendResponse(
    val id: String,
    val username: String,
    val avatar: String? = null,
    val mood: String? = null,
    val background: String? = null
)

data class SearchUserResponse(
    val id: String,
    val username: String,
    val relation: String, // "none", "sent", "received", "friend"
    val avatar: String? = null,
    val mood: String? = null,
    val background: String? = null
)

data class UserProfileResponse(
    val id: String,
    val username: String,
    val avatar: String? = null,
    val mood: String? = null,
    val background: String? = null
)

data class UpdateAvatarPayload(
    val avatar: String? = null,
    val mood: String? = null,
    val fcmToken: String? = null,
    val background: String? = null
)

