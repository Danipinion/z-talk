package com.danipinion.z_talk.ui.screen.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danipinion.z_talk.data.remote.model.AuthRequest
import com.danipinion.z_talk.ui.theme.*

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (token: String, username: String, userId: String, avatar: String?) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val loginState by viewModel.loginState.collectAsState()

    // Handle Auth States
    LaunchedEffect(loginState) {
        val state = loginState
        if (state is AuthState.Success) {
            val response = state.data
            val token = response.token ?: ""
            val uName = response.user?.username ?: ""
            val uId = response.user?.id ?: ""
            val avatar = response.user?.avatar
            onLoginSuccess(token, uName, uId, avatar)
            viewModel.resetStates()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo / Welcome Area
            Surface(
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(28.dp),
                color = RedPastel
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Z", fontSize = 48.sp, fontWeight = FontWeight.Black, color = RedPrimary)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Welcome Back!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Black
            )
            Text(
                text = "Sign in to continue chatting",
                fontSize = 16.sp,
                color = GreyText
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Inputs
            AuthTextField(
                value = username,
                onValueChange = { username = it },
                label = "Username",
                leadingIcon = Icons.Default.Person,
                placeholder = "Enter your username"
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                leadingIcon = Icons.Default.Lock,
                placeholder = "Enter your password",
                isPassword = true,
                isPasswordVisible = isPasswordVisible,
                onPasswordToggle = { isPasswordVisible = !isPasswordVisible }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onNavigateToForgotPassword) {
                    Text("Forgot Password?", color = RedPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Handle Error Alert
            val errorMessage = (loginState as? AuthState.Error)?.message ?: ""
            ZErrorCard(
                message = getFriendlyErrorMessage(errorMessage)
            )

            ZAuthButton(
                text = "Login",
                onClick = {
                    if (username.isNotEmpty() && password.isNotEmpty()) {
                        viewModel.login(AuthRequest(username, password))
                    }
                },
                isLoading = loginState is AuthState.Loading,
                enabled = username.isNotEmpty() && password.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Don't have an account?", color = GreyText)
                TextButton(onClick = {
                    viewModel.resetStates()
                    onNavigateToRegister()
                }) {
                    Text("Sign Up", color = RedPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
