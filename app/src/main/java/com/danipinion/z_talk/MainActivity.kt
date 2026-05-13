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
import com.danipinion.z_talk.ui.screen.dashboard.ChatDashboardScreen
import com.danipinion.z_talk.ui.screen.search.SearchUserScreen
import com.danipinion.z_talk.ui.screen.scan.ScanScreen
import com.danipinion.z_talk.ui.screen.chat.ChatDetailScreen
import com.danipinion.z_talk.ui.theme.ZtalkTheme

sealed class Screen {
    data class Dashboard(val initialTab: Int = 0) : Screen()
    object SearchUser : Screen()
    object Scan : Screen()
    data class ChatDetail(val username: String) : Screen()
    object EditProfile : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZtalkTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard()) }

                Crossfade(targetState = currentScreen, label = "navigation") { screen ->
                    when (screen) {
                        is Screen.Dashboard -> ChatDashboardScreen(
                            initialBottomNav = screen.initialTab,
                            onNavigateToSearch = { currentScreen = Screen.SearchUser },
                            onNavigateToScan = { currentScreen = Screen.Scan },
                            onNavigateToChat = { username -> currentScreen = Screen.ChatDetail(username) },
                            onNavigateToEditProfile = { currentScreen = Screen.EditProfile }
                        )
                        is Screen.SearchUser -> SearchUserScreen(
                            onBack = { currentScreen = Screen.Dashboard() }
                        )
                        is Screen.Scan -> ScanScreen(
                            onBack = { currentScreen = Screen.Dashboard() }
                        )
                        is Screen.ChatDetail -> ChatDetailScreen(
                            username = screen.username,
                            onBack = { currentScreen = Screen.Dashboard() }
                        )
                        is Screen.EditProfile -> {
                            // We'll create this screen next
                            com.danipinion.z_talk.ui.screen.profile.EditProfileScreen(
                                onBack = { currentScreen = Screen.Dashboard(initialTab = 1) }
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
        ChatDashboardScreen()
    }
}