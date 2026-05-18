package com.danipinion.z_talk

import android.os.Bundle
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
import com.danipinion.z_talk.ui.theme.ZtalkTheme

sealed class Screen {
    object Login : Screen()
    object Register : Screen()
    object ForgotPassword : Screen()
    object Dashboard : Screen()
    object SearchUser : Screen()
    object Scan : Screen()
    data class ChatDetail(val username: String) : Screen()
    object EditProfile : Screen()
    object Profile : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZtalkTheme {
                val sessionManager = remember { SessionManager(applicationContext) }
                 val authViewModel = remember {
                    AuthViewModel(AuthRepository(RetrofitClient.apiService))
                }
                val friendViewModel = remember {
                    FriendViewModel(FriendRepository(RetrofitClient.apiService))
                }
                var currentScreen by remember { 
                    mutableStateOf<Screen>(
                        if (sessionManager.isLoggedIn()) Screen.Dashboard else Screen.Login
                    ) 
                }
                var dashboardTab by rememberSaveable { mutableIntStateOf(0) }

                Crossfade(targetState = currentScreen, label = "navigation") { screen ->
                    when (screen) {
                        is Screen.Login -> LoginScreen(
                            viewModel = authViewModel,
                            onLoginSuccess = { token, uName, uId ->
                                sessionManager.saveSession(token, uName, uId)
                                currentScreen = Screen.Dashboard
                            },
                            onNavigateToRegister = { currentScreen = Screen.Register },
                            onNavigateToForgotPassword = { currentScreen = Screen.ForgotPassword }
                        )
                        is Screen.Register -> RegisterScreen(
                            viewModel = authViewModel,
                            onRegisterSuccess = { token, uName, uId ->
                                sessionManager.saveSession(token, uName, uId)
                                currentScreen = Screen.Dashboard
                            },
                            onNavigateToLogin = { currentScreen = Screen.Login }
                        )
                        is Screen.ForgotPassword -> ForgotPasswordScreen(
                            onBackToLogin = { currentScreen = Screen.Login }
                        )
                        is Screen.Dashboard -> ChatDashboardScreen(
                            selectedTab = dashboardTab,
                            onTabSelected = { dashboardTab = it },
                            onNavigateToSearch = { currentScreen = Screen.SearchUser },
                            onNavigateToScan = { currentScreen = Screen.Scan },
                            onNavigateToChat = { username -> currentScreen = Screen.ChatDetail(username) },
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
                            onBack = { currentScreen = Screen.Dashboard }
                        )
                        is Screen.Profile -> {
                            com.danipinion.z_talk.ui.screen.profile.ProfileScreen(
                                onBack = { currentScreen = Screen.Dashboard },
                                onEditProfile = { currentScreen = Screen.EditProfile },
                                onLogout = {
                                    sessionManager.clearSession()
                                    currentScreen = Screen.Login
                                }
                            )
                        }
                        is Screen.EditProfile -> {
                            // We'll create this screen next
                            com.danipinion.z_talk.ui.screen.profile.EditProfileScreen(
                                onBack = { currentScreen = Screen.Profile }
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