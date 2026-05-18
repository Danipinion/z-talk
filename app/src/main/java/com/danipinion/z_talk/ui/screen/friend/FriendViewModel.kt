package com.danipinion.z_talk.ui.screen.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danipinion.z_talk.data.repository.FriendRepository
import com.danipinion.z_talk.data.remote.FriendRequestResponse
import com.danipinion.z_talk.data.remote.FriendResponse
import com.danipinion.z_talk.data.remote.SearchUserResponse
import com.danipinion.z_talk.ui.screen.auth.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import com.danipinion.z_talk.data.local.AppDatabase
import com.danipinion.z_talk.data.local.entity.FriendEntity
import com.danipinion.z_talk.data.local.entity.FriendRequestEntity

class FriendViewModel(
    private val repository: FriendRepository,
    private val database: AppDatabase
) : ViewModel() {

    private val _searchState = MutableStateFlow<AuthState<List<SearchUserResponse>>>(AuthState.Idle)
    val searchState: StateFlow<AuthState<List<SearchUserResponse>>> = _searchState

    private val _requestsState = MutableStateFlow<AuthState<List<FriendRequestResponse>>>(AuthState.Idle)
    val requestsState: StateFlow<AuthState<List<FriendRequestResponse>>> = _requestsState

    private val _friendsState = MutableStateFlow<AuthState<List<FriendResponse>>>(AuthState.Idle)
    val friendsState: StateFlow<AuthState<List<FriendResponse>>> = _friendsState

    private val _actionState = MutableStateFlow<AuthState<String>>(AuthState.Idle)
    val actionState: StateFlow<AuthState<String>> = _actionState

    init {
        // Collect cached friends from Room DB and emit immediately
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            database.friendDao().getAllFriends().collect { localFriends ->
                val mapped = localFriends.map { FriendResponse(it.id, it.username) }
                // Only push if we haven't successfully fetched fresh remote data
                if (_friendsState.value !is AuthState.Success) {
                    _friendsState.value = AuthState.Success(mapped)
                }
            }
        }
        // Collect cached friend requests from Room DB and emit immediately
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            database.friendRequestDao().getAllFriendRequests().collect { localRequests ->
                val mapped = localRequests.map { FriendRequestResponse(it.senderId, it.senderUsername, it.status, it.createdAt) }
                if (_requestsState.value !is AuthState.Success) {
                    _requestsState.value = AuthState.Success(mapped)
                }
            }
        }
    }

    fun searchUsers(token: String, query: String) {
        if (query.isBlank()) {
            _searchState.value = AuthState.Idle
            return
        }
        _searchState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response = repository.searchUsers(token, query)
                if (response.isSuccessful && response.body() != null) {
                    _searchState.value = AuthState.Success(response.body()!!)
                } else {
                    _searchState.value = AuthState.Error(response.message() ?: "Search failed")
                }
            } catch (e: Exception) {
                _searchState.value = AuthState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun getFriendRequests(token: String) {
        // Only show loading if we don't already have success (cached) data to avoid visual flickers
        if (_requestsState.value !is AuthState.Success) {
            _requestsState.value = AuthState.Loading
        }
        viewModelScope.launch {
            try {
                val response = repository.getFriendRequests(token)
                if (response.isSuccessful && response.body() != null) {
                    val remoteRequests = response.body()!!
                    _requestsState.value = AuthState.Success(remoteRequests)
                    // Update Room DB Cache in background
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        database.friendRequestDao().deleteAllFriendRequests()
                        database.friendRequestDao().insertFriendRequests(remoteRequests.map {
                            FriendRequestEntity(it.senderId, it.senderUsername, it.status, it.createdAt)
                        })
                    }
                } else {
                    if (_requestsState.value !is AuthState.Success) {
                        _requestsState.value = AuthState.Error(response.message() ?: "Failed to fetch requests")
                    }
                }
            } catch (e: Exception) {
                if (_requestsState.value !is AuthState.Success) {
                    _requestsState.value = AuthState.Error(e.localizedMessage ?: "Unknown error")
                }
            }
        }
    }

    fun getFriends(token: String) {
        // Only show loading if we don't already have success (cached) data to avoid visual flickers
        if (_friendsState.value !is AuthState.Success) {
            _friendsState.value = AuthState.Loading
        }
        viewModelScope.launch {
            try {
                val response = repository.getFriends(token)
                if (response.isSuccessful && response.body() != null) {
                    val remoteFriends = response.body()!!
                    _friendsState.value = AuthState.Success(remoteFriends)
                    // Update Room DB Cache in background
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        database.friendDao().deleteAllFriends()
                        database.friendDao().insertFriends(remoteFriends.map {
                            FriendEntity(it.id, it.username)
                        })
                    }
                } else {
                    if (_friendsState.value !is AuthState.Success) {
                        _friendsState.value = AuthState.Error(response.message() ?: "Failed to fetch friends")
                    }
                }
            } catch (e: Exception) {
                if (_friendsState.value !is AuthState.Success) {
                    _friendsState.value = AuthState.Error(e.localizedMessage ?: "Unknown error")
                }
            }
        }
    }

    fun sendFriendRequest(token: String, receiverUsername: String, onComplete: () -> Unit = {}) {
        _actionState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response = repository.sendFriendRequest(token, receiverUsername)
                if (response.isSuccessful) {
                    _actionState.value = AuthState.Success("Friend request sent successfully!")
                    onComplete()
                } else {
                    _actionState.value = AuthState.Error(response.body()?.error ?: response.message())
                }
            } catch (e: Exception) {
                _actionState.value = AuthState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun respondToFriendRequest(token: String, senderId: String, accept: Boolean, onComplete: () -> Unit = {}) {
        _actionState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response = repository.respondToFriendRequest(token, senderId, accept)
                if (response.isSuccessful) {
                    _actionState.value = AuthState.Success(if (accept) "Request accepted!" else "Request declined!")
                    onComplete()
                } else {
                    _actionState.value = AuthState.Error(response.body()?.error ?: response.message())
                }
            } catch (e: Exception) {
                _actionState.value = AuthState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun addFriendDirectly(token: String, friendId: String, onComplete: (String) -> Unit = {}) {
        _actionState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response = repository.addFriendDirectly(token, friendId)
                if (response.isSuccessful) {
                    _actionState.value = AuthState.Success("Friend added directly successfully!")
                    onComplete("Friend added directly!")
                } else {
                    val errMsg = response.body()?.error ?: response.message()
                    _actionState.value = AuthState.Error(errMsg)
                    onComplete(errMsg)
                }
            } catch (e: Exception) {
                _actionState.value = AuthState.Error(e.localizedMessage ?: "Unknown error")
                onComplete(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun resetActionState() {
        _actionState.value = AuthState.Idle
    }
}
