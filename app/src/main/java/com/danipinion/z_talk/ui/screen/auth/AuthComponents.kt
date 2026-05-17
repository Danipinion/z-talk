package com.danipinion.z_talk.ui.screen.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danipinion.z_talk.ui.theme.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    placeholder: String,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordToggle: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Black, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = GreyLight,
            border = BorderStroke(1.dp, GreyDivider)
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = RedPrimary,
                    focusedTextColor = Black,
                    unfocusedTextColor = Black
                ),
                leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = RedPrimary) },
                trailingIcon = if (isPassword) {
                    {
                        IconButton(onClick = onPasswordToggle) {
                            Icon(
                                if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = GreyText
                            )
                        }
                    }
                } else null,
                placeholder = { Text(placeholder, color = GreyText.copy(alpha = 0.5f)) },
                visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text),
                singleLine = true
            )
        }
    }
}
@Composable
fun OtpInputField(
    otpText: String,
    onOtpTextChange: (String) -> Unit,
    otpCount: Int = 6
) {
    BasicTextField(
        value = otpText,
        onValueChange = {
            if (it.length <= otpCount) {
                onOtpTextChange(it)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(otpCount) { index ->
                    val char = when {
                        index >= otpText.length -> ""
                        else -> otpText[index].toString()
                    }
                    val isFocused = otpText.length == index

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(
                                color = if (isFocused) White else GreyLight,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) RedPrimary else GreyDivider,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFocused) RedPrimary else Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    )
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

@Composable
fun ZCircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = White,
    strokeWidth: Dp = 3.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier.size(24.dp)) {
        drawArc(
            color = color.copy(alpha = 0.2f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = color,
            startAngle = angle,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun ZAuthButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = RedPrimary,
            contentColor = White,
            disabledContainerColor = RedPrimary.copy(alpha = 0.5f),
            disabledContentColor = White.copy(alpha = 0.5f)
        ),
        enabled = enabled && !isLoading
    ) {
        if (isLoading) {
            ZCircularProgressIndicator(color = White)
        } else {
            Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

fun getFriendlyErrorMessage(rawError: String): String {
    if (rawError.isBlank()) return ""
    val lower = rawError.lowercase()
    return when {
        lower.contains("required") -> "Fill in all deets first, bestie! ✍️"
        lower.contains("already exists") || lower.contains("claimed") -> "Oof, that username is taken fr! 💀"
        lower.contains("invalid username or password") || lower.contains("incorrect") || lower.contains("failed") -> "Wait, wrong login deets! 🧐"
        lower.contains("internal server error") || lower.contains("500") -> "Server's taking a nap right now! ☕"
        lower.contains("connect") || lower.contains("timeout") || lower.contains("network") || lower.contains("host") -> "No internet, check your connection! 📡"
        else -> "Oof, something went wrong! 🛠️"
    }
}

@Composable
fun ZErrorCard(
    message: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = RedPastel,
            border = BorderStroke(1.dp, RedPrimary)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = RedPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = message,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Black
                )
            }
        }
    }
}
