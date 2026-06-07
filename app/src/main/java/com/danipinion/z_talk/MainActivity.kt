package com.danipinion.z_talk

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.danipinion.z_talk.ui.screen.dashboard.ChatDashboardScreen
import com.danipinion.z_talk.ui.screen.search.SearchUserScreen
import com.danipinion.z_talk.ui.screen.scan.ScanScreen
import com.danipinion.z_talk.ui.screen.chat.ChatDetailScreen
import com.danipinion.z_talk.ui.screen.auth.LoginScreen
import com.danipinion.z_talk.ui.screen.auth.RegisterScreen
import com.danipinion.z_talk.ui.screen.auth.ForgotPasswordScreen
import com.danipinion.z_talk.ui.screen.auth.AuthViewModel
import com.danipinion.z_talk.data.repository.AuthRepository
import com.danipinion.z_talk.ui.screen.friend.FriendViewModel
import com.danipinion.z_talk.data.repository.FriendRepository
import com.danipinion.z_talk.data.remote.RetrofitClient
import com.danipinion.z_talk.data.local.SessionManager
import com.danipinion.z_talk.data.local.AppDatabase
import com.danipinion.z_talk.data.remote.WebSocketManager
import com.danipinion.z_talk.ui.theme.ZtalkTheme
import android.os.Build
import com.danipinion.z_talk.data.remote.UpdateAvatarPayload
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

sealed class Screen {
    object Login : Screen()
    object Register : Screen()
    object ForgotPassword : Screen()
    object Dashboard : Screen()
    object SearchUser : Screen()
    object Scan : Screen()
    data class ChatDetail(val username: String, val friendId: String) : Screen()
    object Profile : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 101)
            }
        }

        setContent {
            ZtalkTheme {
                val sessionManager = remember { SessionManager(applicationContext) }
                 val authViewModel = remember {
                    AuthViewModel(AuthRepository(RetrofitClient.apiService))
                }
                 val friendViewModel = remember {
                    FriendViewModel(
                        repository = FriendRepository(RetrofitClient.apiService),
                        database = AppDatabase.getDatabase(applicationContext)
                    )
                }
                var currentScreen by remember { 
                    mutableStateOf<Screen>(
                        if (sessionManager.isLoggedIn()) Screen.Dashboard else Screen.Login
                    ) 
                }
                var dashboardTab by rememberSaveable { mutableIntStateOf(0) }

                // Manage WebSocket connection dynamically based on logged in user ID
                val userId = sessionManager.getUserId() ?: ""
                var webSocketManager by remember { mutableStateOf<WebSocketManager?>(null) }

                LaunchedEffect(userId) {
                    val scope = this
                    if (userId.isNotEmpty()) {
                        val manager = WebSocketManager(applicationContext, userId)
                        manager.connect()
                        webSocketManager = manager
                        Log.d("MainActivity", "WebSocketManager connected for user $userId")

                        // Retrieve and upload FCM Token
                        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val token = task.result
                                    Log.d("MainActivity", "FCM Token: $token")
                                    sessionManager.saveFcmToken(token)

                                    val jwtToken = sessionManager.getToken()
                                    if (jwtToken != null && token != null) {
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                RetrofitClient.apiService.updateAvatar(
                                                    token = "Bearer $jwtToken",
                                                    payload = UpdateAvatarPayload(fcmToken = token)
                                                )
                                                Log.d("MainActivity", "Uploaded FCM Token successfully")
                                            } catch (e: Exception) {
                                                Log.e("MainActivity", "Failed to upload FCM token: ${e.localizedMessage}")
                                            }
                                        }
                                    }
                                } else {
                                    Log.w("MainActivity", "Fetching FCM registration token failed", task.exception)
                                }
                            }
                    } else {
                        webSocketManager?.disconnect()
                        webSocketManager = null
                        Log.d("MainActivity", "WebSocketManager disconnected")
                    }
                }

                Crossfade(targetState = currentScreen, label = "navigation") { screen ->
                    when (screen) {
                        is Screen.Login -> LoginScreen(
                            viewModel = authViewModel,
                            onLoginSuccess = { token, uName, uId, avatar, mood, background ->
                                sessionManager.saveSession(token, uName, uId)
                                sessionManager.saveAvatar(avatar)
                                sessionManager.saveMood(mood)
                                sessionManager.saveBackground(background)
                                currentScreen = Screen.Dashboard
                            },
                            onNavigateToRegister = { currentScreen = Screen.Register },
                            onNavigateToForgotPassword = { currentScreen = Screen.ForgotPassword }
                        )
                        is Screen.Register -> RegisterScreen(
                            viewModel = authViewModel,
                            onRegisterSuccess = { token, uName, uId, avatar, mood, background ->
                                sessionManager.saveSession(token, uName, uId)
                                sessionManager.saveAvatar(avatar)
                                sessionManager.saveMood(mood)
                                sessionManager.saveBackground(background)
                                currentScreen = Screen.Dashboard
                            },
                            onNavigateToLogin = { currentScreen = Screen.Login }
                        )
                        is Screen.ForgotPassword -> ForgotPasswordScreen(
                            onBackToLogin = { currentScreen = Screen.Login }
                        )
                        is Screen.Dashboard -> ChatDashboardScreen(
                            viewModel = friendViewModel,
                            token = sessionManager.getToken() ?: "",
                            userId = sessionManager.getUserId() ?: "",
                            selectedTab = dashboardTab,
                            onTabSelected = { dashboardTab = it },
                            onNavigateToSearch = { currentScreen = Screen.SearchUser },
                            onNavigateToScan = { currentScreen = Screen.Scan },
                            onNavigateToChat = { username, friendId -> currentScreen = Screen.ChatDetail(username, friendId) },
                            onNavigateToProfile = { currentScreen = Screen.Profile }
                        )
                        is Screen.SearchUser -> SearchUserScreen(
                            viewModel = friendViewModel,
                            token = sessionManager.getToken() ?: "",
                            onBack = { currentScreen = Screen.Dashboard }
                        )
                        is Screen.Scan -> ScanScreen(
                            viewModel = friendViewModel,
                            token = sessionManager.getToken() ?: "",
                            username = sessionManager.getUsername() ?: "",
                            userId = sessionManager.getUserId() ?: "",
                            onBack = { currentScreen = Screen.Dashboard }
                        )
                        is Screen.ChatDetail -> ChatDetailScreen(
                            username = screen.username,
                            friendId = screen.friendId,
                            senderId = sessionManager.getUserId() ?: "",
                            webSocketManager = webSocketManager,
                            onBack = { currentScreen = Screen.Dashboard }
                        )
                        is Screen.Profile -> {
                            com.danipinion.z_talk.ui.screen.profile.ProfileScreen(
                                onBack = { currentScreen = Screen.Dashboard },
                                onLogout = {
                                    sessionManager.clearSession()
                                    currentScreen = Screen.Login
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}






@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    ZtalkTheme {
        ChatDashboardScreen(selectedTab = 0, onTabSelected = {})
    }
}