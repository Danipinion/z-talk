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

class FriendViewModel(private val repository: FriendRepository) : ViewModel() {

    private val _searchState = MutableStateFlow<AuthState<List<SearchUserResponse>>>(AuthState.Idle)
    val searchState: StateFlow<AuthState<List<SearchUserResponse>>> = _searchState

    private val _requestsState = MutableStateFlow<AuthState<List<FriendRequestResponse>>>(AuthState.Idle)
    val requestsState: StateFlow<AuthState<List<FriendRequestResponse>>> = _requestsState

    private val _friendsState = MutableStateFlow<AuthState<List<FriendResponse>>>(AuthState.Idle)
    val friendsState: StateFlow<AuthState<List<FriendResponse>>> = _friendsState

    private val _actionState = MutableStateFlow<AuthState<String>>(AuthState.Idle)
    val actionState: StateFlow<AuthState<String>> = _actionState

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
        _requestsState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response = repository.getFriendRequests(token)
                if (response.isSuccessful && response.body() != null) {
                    _requestsState.value = AuthState.Success(response.body()!!)
                } else {
                    _requestsState.value = AuthState.Error(response.message() ?: "Failed to fetch requests")
                }
            } catch (e: Exception) {
                _requestsState.value = AuthState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun getFriends(token: String) {
        _friendsState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response = repository.getFriends(token)
                if (response.isSuccessful && response.body() != null) {
                    _friendsState.value = AuthState.Success(response.body()!!)
                } else {
                    _friendsState.value = AuthState.Error(response.message() ?: "Failed to fetch friends")
                }
            } catch (e: Exception) {
                _friendsState.value = AuthState.Error(e.localizedMessage ?: "Unknown error")
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
