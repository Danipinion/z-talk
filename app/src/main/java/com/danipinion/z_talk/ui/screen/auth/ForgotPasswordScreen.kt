package com.danipinion.z_talk.ui.screen.auth

import androidx.activity.compose.BackHandler

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danipinion.z_talk.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    
    BackHandler {
        if (step > 1) step-- else onBackToLogin()
    }

    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = {
                    if (step > 1) step-- else onBackToLogin()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Black)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Crossfade(targetState = step, label = "forgot_password_steps") { currentStep ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (currentStep) {
                        1 -> StepOne(email, onEmailChange = { email = it }, onNext = { step = 2 })
                        2 -> StepTwo(email, otp, onOtpChange = { otp = it }, onNext = { step = 3 })
                        3 -> StepThree(newPassword, confirmNewPassword, onPassChange = { newPassword = it }, onConfirmChange = { confirmNewPassword = it }, onNext = onBackToLogin)
                    }
                }
            }
        }
    }
}

@Composable
fun StepOne(email: String, onEmailChange: (String) -> Unit, onNext: () -> Unit) {
    val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Forgot Password", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Enter your email address and we'll send you a verification code",
            fontSize = 16.sp,
            color = GreyText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        AuthTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email Address",
            leadingIcon = Icons.Default.Email,
            placeholder = "Enter your email"
        )

        AnimatedVisibility(
            visible = email.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isEmailValid) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (isEmailValid) Color(0xFF4CAF50) else RedPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isEmailValid) "Email format is valid" else "Please enter a valid email address",
                    fontSize = 11.sp,
                    color = if (isEmailValid) Color(0xFF4CAF50) else RedPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        ZAuthButton(
            text = "Send Code",
            onClick = {
                scope.launch {
                    isLoading = true
                    delay(1200)
                    isLoading = false
                    onNext()
                }
            },
            isLoading = isLoading,
            enabled = isEmailValid
        )
    }
}

@Composable
fun StepTwo(email: String, otp: String, onOtpChange: (String) -> Unit, onNext: () -> Unit) {
    var timerSeconds by remember { mutableIntStateOf(60) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(timerSeconds) {
        if (timerSeconds > 0) {
            delay(1000)
            timerSeconds--
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Verify Code", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "We've sent a 6-digit code to\n$email",
            fontSize = 16.sp,
            color = GreyText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        OtpInputField(
            otpText = otp,
            onOtpTextChange = onOtpChange
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = if (timerSeconds > 0) {
                val mins = timerSeconds / 60
                val secs = timerSeconds % 60
                String.format("Resend code in %02d:%02d", mins, secs)
            } else {
                "You can now resend the code"
            },
            fontSize = 14.sp,
            color = GreyText
        )

        Spacer(modifier = Modifier.height(24.dp))

        ZAuthButton(
            text = "Verify",
            onClick = {
                scope.launch {
                    isLoading = true
                    delay(1200)
                    isLoading = false
                    onNext()
                }
            },
            isLoading = isLoading,
            enabled = otp.length == 6
        )
        
        TextButton(
            onClick = { 
                if (timerSeconds == 0) {
                    timerSeconds = 60 
                    // Add resend logic here
                }
            },
            enabled = timerSeconds == 0
        ) {
            Text(
                "Resend Code", 
                color = if (timerSeconds == 0) RedPrimary else GreyText.copy(alpha = 0.5f), 
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StepThree(pass: String, confirm: String, onPassChange: (String) -> Unit, onConfirmChange: (String) -> Unit, onNext: () -> Unit) {
    val hasUppercase = pass.any { it.isUpperCase() }
    val hasLowercase = pass.any { it.isLowerCase() }
    val hasNumber = pass.any { it.isDigit() }
    val hasSymbol = pass.any { !it.isLetterOrDigit() }
    val isPasswordStrong = hasUppercase && hasLowercase && hasNumber && hasSymbol && pass.length >= 8
    val passwordsMatch = confirm.isNotEmpty() && pass == confirm
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
 
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Reset Password", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Create a new strong password for your account",
            fontSize = 16.sp,
            color = GreyText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        AuthTextField(
            value = pass,
            onValueChange = onPassChange,
            label = "New Password",
            leadingIcon = Icons.Default.Lock,
            placeholder = "Enter new password",
            isPassword = true
        )

        // Password Strength Indicator
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val strength = when {
                isPasswordStrong -> 4
                pass.length >= 6 && (hasUppercase || hasNumber) -> 3
                pass.length >= 4 -> 2
                pass.isNotEmpty() -> 1
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
            PasswordCriteriaItem("Use 8 or more characters", pass.length >= 8)
            PasswordCriteriaItem("Mix upper and lower case letters", hasUppercase && hasLowercase)
            PasswordCriteriaItem("Add at least one number", hasNumber)
            PasswordCriteriaItem("Use a special character (e.g., #$@)", hasSymbol)
        }

        Spacer(modifier = Modifier.height(16.dp))

        AuthTextField(
            value = confirm,
            onValueChange = onConfirmChange,
            label = "Confirm New Password",
            leadingIcon = Icons.Default.LockClock,
            placeholder = "Repeat new password",
            isPassword = true
        )

        AnimatedVisibility(
            visible = confirm.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
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

        ZAuthButton(
            text = "Reset Password",
            onClick = {
                scope.launch {
                    isLoading = true
                    delay(1200)
                    isLoading = false
                    onNext()
                }
            },
            isLoading = isLoading,
            enabled = isPasswordStrong && passwordsMatch
        )
    }
}

