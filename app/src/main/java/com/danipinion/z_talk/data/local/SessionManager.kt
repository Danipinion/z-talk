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
        private const val KEY_MOOD = "mood"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_BACKGROUND = "background"
    }

    fun saveBackground(bg: String?) {
        prefs.edit().putString(KEY_BACKGROUND, bg).apply()
    }

    fun getBackground(): String? = prefs.getString(KEY_BACKGROUND, null)
    fun saveFcmToken(token: String?) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun getFcmToken(): String? = prefs.getString(KEY_FCM_TOKEN, null)

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

    fun saveMood(mood: String?) {
        prefs.edit().putString(KEY_MOOD, mood).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getAvatar(): String? = prefs.getString(KEY_AVATAR, null)
    fun getMood(): String? = prefs.getString(KEY_MOOD, null)

    fun isLoggedIn(): Boolean = getToken() != null

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
