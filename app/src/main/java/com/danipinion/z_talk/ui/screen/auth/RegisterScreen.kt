package com.danipinion.z_talk.ui.screen.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import com.danipinion.z_talk.data.remote.model.AuthRequest
import com.danipinion.z_talk.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: (token: String, username: String, userId: String, avatar: String?, mood: String?, background: String?) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    BackHandler { 
        viewModel.resetStates()
        onNavigateToLogin() 
    }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmVisible by remember { mutableStateOf(false) }

    val registerState by viewModel.registerState.collectAsState()
    val usernameAvailable by viewModel.usernameAvailable.collectAsState()
    val isCheckingUsername by viewModel.isCheckingUsername.collectAsState()

    // Handle typing debounce for username check
    LaunchedEffect(username) {
        if (username.isEmpty()) {
            viewModel.resetStates()
            return@LaunchedEffect
        }
        delay(500) // Debounce typing
        viewModel.checkUsername(username)
    }

    // Handle Success Redirection
    LaunchedEffect(registerState) {
        val state = registerState
        if (state is AuthState.Success) {
            val response = state.data
            val token = response.token ?: ""
            val uName = response.user?.username ?: ""
            val uId = response.user?.id ?: ""
            val avatar = response.user?.avatar
            val mood = response.user?.mood
            val background = response.user?.background
            onRegisterSuccess(token, uName, uId, avatar, mood, background)
            viewModel.resetStates()
        }
    }

    val isUsernameTaken = usernameAvailable == false
    val hasUppercase = password.any { it.isUpperCase() }
    val hasLowercase = password.any { it.isLowerCase() }
    val hasNumber = password.any { it.isDigit() }
    val hasSymbol = password.any { !it.isLetterOrDigit() }
    val isPasswordStrong = hasUppercase && hasLowercase && hasNumber && hasSymbol && password.length >= 8
    val passwordsMatch = confirmPassword.isNotEmpty() && password == confirmPassword

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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = {
                    viewModel.resetStates()
                    onNavigateToLogin()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Black)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Create Account",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Black
            )
            Text(
                text = "Join Z-Talk and start connecting",
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
                placeholder = "Choose a username"
            )
            
            AnimatedVisibility(
                visible = username.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCheckingUsername) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp,
                            color = RedPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Checking availability...",
                            fontSize = 11.sp,
                            color = GreyText,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (usernameAvailable != null) {
                        Icon(
                            imageVector = if (isUsernameTaken) Icons.Default.Close else Icons.Default.Check,
                            contentDescription = null,
                            tint = if (isUsernameTaken) RedPrimary else Color(0xFF4CAF50),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isUsernameTaken) "This username is already claimed" else "Awesome! This username is available",
                            fontSize = 11.sp,
                            color = if (isUsernameTaken) RedPrimary else Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                leadingIcon = Icons.Default.Lock,
                placeholder = "Create a password",
                isPassword = true,
                isPasswordVisible = isPasswordVisible,
                onPasswordToggle = { isPasswordVisible = !isPasswordVisible }
            )

            // Password Strength Indicator
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val strength = when {
                    isPasswordStrong -> 4
                    password.length >= 6 && (hasUppercase || hasNumber) -> 3
                    password.length >= 4 -> 2
                    password.isNotEmpty() -> 1
                    else -> 0
                }

                repeat(4) { index ->
                    val isActive = index < strength
                    val targetColor = when {
                        strength == 4 -> Color(0xFF4CAF50)
                        strength == 3 -> Color(0xFFFFC107)
                        strength == 2 -> Color(0xFFFF9800)
                        strength == 1 -> RedPrimary
                        else -> GreyDivider
                    }
                    
                    val barColor by animateColorAsState(
                        targetValue = if (isActive) targetColor else GreyDivider,
                        animationSpec = tween(durationMillis = 400),
                        label = "strength_color"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(barColor)
                    )
                }
            }

            // Criteria Checklist
            Spacer(modifier = Modifier.height(12.dp))
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)) {
                PasswordCriteriaItem("Use 8 or more characters", password.length >= 8)
                PasswordCriteriaItem("Mix upper and lower case letters", hasUppercase && hasLowercase)
                PasswordCriteriaItem("Add at least one number", hasNumber)
                PasswordCriteriaItem("Use a special character (e.g., #$@)", hasSymbol)
            }

            Spacer(modifier = Modifier.height(16.dp))

            AuthTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm Password",
                leadingIcon = Icons.Default.LockClock,
                placeholder = "Repeat your password",
                isPassword = true,
                isPasswordVisible = isConfirmVisible,
                onPasswordToggle = { isConfirmVisible = !isConfirmVisible }
            )

            AnimatedVisibility(
                visible = confirmPassword.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (passwordsMatch) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (passwordsMatch) Color(0xFF4CAF50) else RedPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (passwordsMatch) "Perfect! Your passwords match" else "Passwords don't match yet",
                        fontSize = 11.sp,
                        color = if (passwordsMatch) Color(0xFF4CAF50) else RedPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Handle Error Alert
            val errorMessage = (registerState as? AuthState.Error)?.message ?: ""
            ZErrorCard(
                message = getFriendlyErrorMessage(errorMessage)
            )

            ZAuthButton(
                text = "Sign Up",
                onClick = {
                    if (username.isNotEmpty() && password.isNotEmpty() && passwordsMatch) {
                        viewModel.register(AuthRequest(username, password))
                    }
                },
                isLoading = registerState is AuthState.Loading,
                enabled = username.isNotEmpty() && password.isNotEmpty() && !isUsernameTaken && isPasswordStrong && passwordsMatch
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already have an account?", color = GreyText)
                TextButton(onClick = {
                    viewModel.resetStates()
                    onNavigateToLogin()
                }) {
                    Text("Login", color = RedPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
