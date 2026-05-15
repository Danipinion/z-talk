package com.danipinion.z_talk.ui.screen.auth

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.danipinion.z_talk.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    BackHandler { onNavigateToLogin() }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmVisible by remember { mutableStateOf(false) }
    var isCheckingUsername by remember { mutableStateOf(false) }
    var usernameStatus by remember { mutableStateOf<Boolean?>(null) } // null = initial, true = available, false = taken

    // Username Check Simulation
    LaunchedEffect(username) {
        if (username.isEmpty()) {
            usernameStatus = null
            isCheckingUsername = false
            return@LaunchedEffect
        }
        isCheckingUsername = true
        delay(1000) // Simulate network delay
        usernameStatus = !(username.lowercase() == "admin" || username.lowercase() == "user")
        isCheckingUsername = false
    }

    val isUsernameTaken = usernameStatus == false
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
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onNavigateToLogin) {
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

            AuthTextField(
                value = username,
                onValueChange = { username = it },
                label = "Username",
                leadingIcon = Icons.Default.Person,
                placeholder = "Choose a username"
            )
            
            if (username.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
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
                    } else if (usernameStatus != null) {
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
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
                        animationSpec = tween(durationMillis = 400)
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
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
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

            if (confirmPassword.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
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

            Button(
                onClick = onRegisterSuccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary, contentColor = White)
            ) {
                Text("Sign Up", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already have an account?", color = GreyText)
                TextButton(onClick = onNavigateToLogin) {
                    Text("Login", color = RedPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PasswordCriteriaItem(text: String, isMet: Boolean) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isMet) Color(0xFF4CAF50) else GreyText.copy(alpha = 0.3f))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = if (isMet) Color(0xFF4CAF50) else GreyText,
            fontWeight = if (isMet) FontWeight.Bold else FontWeight.Normal
        )
    }
}
