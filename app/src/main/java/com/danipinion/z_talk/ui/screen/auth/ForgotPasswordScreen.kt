package com.danipinion.z_talk.ui.screen.auth

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danipinion.z_talk.ui.theme.*

@Composable
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
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

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary, contentColor = White)
        ) {
            Text("Send Code", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StepTwo(email: String, otp: String, onOtpChange: (String) -> Unit, onNext: () -> Unit) {
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

        AuthTextField(
            value = otp,
            onValueChange = onOtpChange,
            label = "Verification Code",
            leadingIcon = Icons.Default.VpnKey,
            placeholder = "Enter 6-digit code"
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary, contentColor = White)
        ) {
            Text("Verify", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        TextButton(onClick = { /* Resend */ }) {
            Text("Resend Code", color = RedPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StepThree(pass: String, confirm: String, onPassChange: (String) -> Unit, onConfirmChange: (String) -> Unit, onNext: () -> Unit) {
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

        Spacer(modifier = Modifier.height(16.dp))

        AuthTextField(
            value = confirm,
            onValueChange = onConfirmChange,
            label = "Confirm New Password",
            leadingIcon = Icons.Default.LockClock,
            placeholder = "Repeat new password",
            isPassword = true
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary, contentColor = White)
        ) {
            Text("Reset Password", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
