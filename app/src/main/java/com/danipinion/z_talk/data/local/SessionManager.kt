package com.danipinion.z_talk.data.local

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("z_talk_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USERNAME = "username"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_AVATAR = "avatar"
    }

    fun saveSession(token: String, username: String, userId: String) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_USERNAME, username)
            putString(KEY_USER_ID, userId)
            apply()
        }
    }

    fun saveAvatar(avatar: String?) {
        prefs.edit().putString(KEY_AVATAR, avatar).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getAvatar(): String? = prefs.getString(KEY_AVATAR, null)

    fun isLoggedIn(): Boolean = getToken() != null

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
