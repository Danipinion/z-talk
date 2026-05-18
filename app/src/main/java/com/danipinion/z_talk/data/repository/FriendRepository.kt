package com.danipinion.z_talk.data.repository

import com.danipinion.z_talk.data.remote.*
import retrofit2.Response

class FriendRepository(private val apiService: ApiService) {
    suspend fun sendFriendRequest(token: String, receiverUsername: String): Response<GenericResponse> =
        apiService.sendFriendRequest("Bearer $token", SendFriendRequestPayload(receiverUsername))

    suspend fun getFriendRequests(token: String): Response<List<FriendRequestResponse>> =
        apiService.getFriendRequests("Bearer $token")

    suspend fun respondToFriendRequest(token: String, senderId: String, accept: Boolean): Response<GenericResponse> =
        apiService.respondToFriendRequest("Bearer $token", RespondRequestPayload(senderId, accept))

    suspend fun addFriendDirectly(token: String, friendId: String): Response<GenericResponse> =
        apiService.addFriendDirectly("Bearer $token", AddDirectPayload(friendId))

    suspend fun getFriends(token: String): Response<List<FriendResponse>> =
        apiService.getFriends("Bearer $token")

    suspend fun searchUsers(token: String, query: String): Response<List<SearchUserResponse>> =
        apiService.searchUsers("Bearer $token", query)
}
