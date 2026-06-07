package com.danipinion.z_talk.ui.screen.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import com.danipinion.z_talk.data.local.SessionManager
import com.danipinion.z_talk.ui.utils.AvatarHelper
import com.danipinion.z_talk.ui.theme.*
import com.danipinion.z_talk.data.remote.RetrofitClient
import com.danipinion.z_talk.data.remote.UpdateAvatarPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(
    onBack: () -> Unit = {}, 
    onLogout: () -> Unit = {}
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    val username = remember { sessionManager.getUsername() ?: "Dani Pinion" }
    var avatarName by remember { mutableStateOf(sessionManager.getAvatar() ?: "panda") }

    var backgroundName by remember { mutableStateOf(sessionManager.getBackground() ?: "bg_1") }
    var showBackgroundPicker by remember { mutableStateOf(false) }

    var showMoodPicker by remember { mutableStateOf(false) }
    var selectedMood by remember { mutableStateOf<String?>(sessionManager.getMood()) }
    val moods = listOf("😊", "😎", "😴", "🔥", "🚀", "🎮", "📚", "🎨", "💻", "🍕", "🏖️", "✨")

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .verticalScroll(rememberScrollState())
    ) {
        // Banner & Avatar Section
        // Banner Gambar Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
        ) {
            Image(
                painter = androidx.compose.ui.res.painterResource(
                    id = AvatarHelper.getBackgroundResourceId(context, backgroundName)
                ),
                contentDescription = "Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Kumpulan Tombol Kanan Atas (Edit Background & Mood)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp) // Jarak otomatis antar tombol
            ) {
                // 1. Tombol Edit Background (Pencil)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(White.copy(alpha = 0.9f))
                        .clickable { showBackgroundPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Background",
                        tint = Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 2. Tombol Mood (+)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(White.copy(alpha = 0.9f))
                        .clickable { showMoodPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedMood == null) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Mood",
                            tint = Black,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(text = selectedMood!!, fontSize = 24.sp)
                    }
                }
            }

            // Avatar with Gradient Border
            val gradientBrush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFE91E63), // Pink
                    Color(0xFF9C27B0), // Purple
                    Color(0xFF2196F3), // Blue
                    Color(0xFF4CAF50), // Green
                    Color(0xFFFFEB3B), // Yellow
                    Color(0xFFFF9800), // Orange
                    Color(0xFFE91E63)  // Back to Pink
                )
            )

            // Avatar & Camera Section
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(120.dp)
            ) {
                // Avatar with Gradient Border
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(width = 4.dp, brush = gradientBrush, shape = CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(GreyLight)
                        .clickable { showAvatarPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    val avatarResId = AvatarHelper.getAvatarResourceId(context, avatarName)
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = avatarResId),
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Camera Shortcut (Floating above border)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        // Slightly offset to sit exactly on the border
                        .offset(x = (2).dp, y = (2).dp)
                        .size(36.dp)
                        .shadow(elevation = 4.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(White)
                        .border(1.dp, GreyDivider, CircleShape)
                        .clickable { showAvatarPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt, 
                        contentDescription = null, 
                        tint = Black, 
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Name
        Text(
            text = username,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Black,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage!!,
                color = Color.Red,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )
        }

        if (isSaving) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(
                color = RedPrimary,
                modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Action List
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileActionItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Logout",
                isDestructive = true,
                onClick = { showLogoutDialog = true }
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { 
                Text(
                    text = "Logout Account", 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.Bold,
                    color = Black
                ) 
            },
            text = { 
                Text(
                    text = "Are you sure you want to logout from Z-Talk? You'll need to sign in again to access your chats.",
                    fontSize = 15.sp,
                    color = GreyText
                ) 
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showLogoutDialog = false 
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Yes, Logout", color = White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Cancel", color = GreyText, fontWeight = FontWeight.Medium)
                }
            },
            containerColor = White,
            shape = RoundedCornerShape(28.dp)
        )
    }

    // Mood Picker Dialog
    if (showMoodPicker) {
        AlertDialog(
            onDismissRequest = { showMoodPicker = false },
            confirmButton = {},
            title = { 
                Text(
                    text = "Select your mood", 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.Bold,
                    color = Black
                ) 
            },
            text = {
                Box(modifier = Modifier.height(200.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(moods) { mood ->
                            val isSelected = selectedMood == mood
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) RedPrimary.copy(alpha = 0.1f) else GreyLight)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) RedPrimary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (isSaving) return@clickable
                                        val targetMood = if (selectedMood == mood) null else mood
                                        selectedMood = targetMood
                                        showMoodPicker = false
                                        isSaving = true
                                        errorMessage = null
                                        val token = sessionManager.getToken()
                                        if (token.isNullOrEmpty()) {
                                            errorMessage = "Not logged in"
                                            isSaving = false
                                            return@clickable
                                        }
                                        scope.launch {
                                            try {
                                                val response = withContext(Dispatchers.IO) {
                                                    RetrofitClient.apiService.updateAvatar(
                                                        "Bearer $token",
                                                        UpdateAvatarPayload(mood = targetMood ?: "")
                                                    )
                                                }
                                                if (response.isSuccessful) {
                                                    sessionManager.saveMood(targetMood)
                                                } else {
                                                    errorMessage = response.body()?.error ?: response.message() ?: "Failed to update mood"
                                                }
                                            } catch (e: Exception) {
                                                errorMessage = e.localizedMessage ?: "Unknown error occurred"
                                            } finally {
                                                isSaving = false
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = mood, fontSize = 28.sp)
                            }
                        }
                    }
                }
            },
            containerColor = White,
            shape = RoundedCornerShape(28.dp)
        )
    }

    // Avatar Picker Dialog
    if (showAvatarPicker) {
        AlertDialog(
            onDismissRequest = { showAvatarPicker = false },
            confirmButton = {},
            title = {
                Text(
                    text = "Select Avatar",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )
            },
            text = {
                Box(modifier = Modifier.height(280.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(AvatarHelper.AVATAR_LIST) { targetAvatar ->
                            val avatarResId = AvatarHelper.getAvatarResourceId(context, targetAvatar)
                            val isSelected = targetAvatar == avatarName
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) RedPrimary.copy(alpha = 0.1f) else GreyLight)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) RedPrimary else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        if (isSaving) return@clickable
                                        showAvatarPicker = false
                                        isSaving = true
                                        errorMessage = null
                                        val token = sessionManager.getToken()
                                        if (token.isNullOrEmpty()) {
                                            errorMessage = "Not logged in"
                                            isSaving = false
                                            return@clickable
                                        }
                                        scope.launch {
                                            try {
                                                val response = withContext(Dispatchers.IO) {
                                                    RetrofitClient.apiService.updateAvatar(
                                                        "Bearer $token",
                                                        UpdateAvatarPayload(targetAvatar)
                                                    )
                                                }
                                                if (response.isSuccessful) {
                                                    sessionManager.saveAvatar(targetAvatar)
                                                    avatarName = targetAvatar
                                                } else {
                                                    errorMessage = response.body()?.error ?: response.message() ?: "Failed to update profile picture"
                                                }
                                            } catch (e: Exception) {
                                                errorMessage = e.localizedMessage ?: "Unknown error occurred"
                                            } finally {
                                                isSaving = false
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = androidx.compose.ui.res.painterResource(id = avatarResId),
                                    contentDescription = targetAvatar,
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            },
            containerColor = White,
            shape = RoundedCornerShape(28.dp)
        )
    }

    // Background Picker Dialog
    if (showBackgroundPicker) {
        AlertDialog(
            onDismissRequest = { showBackgroundPicker = false },
            confirmButton = {},
            title = { Text("Select Background", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.height(280.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(AvatarHelper.BACKGROUND_LIST) { targetBg ->
                            val bgResId = AvatarHelper.getBackgroundResourceId(context, targetBg)
                            val isSelected = targetBg == backgroundName
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) RedPrimary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (isSaving) return@clickable
                                        showBackgroundPicker = false
                                        isSaving = true
                                        errorMessage = null
                                        val token = sessionManager.getToken()
                                        if (token.isNullOrEmpty()) { isSaving = false; return@clickable }
                                        scope.launch {
                                            try {
                                                val response = withContext(Dispatchers.IO) {
                                                    RetrofitClient.apiService.updateAvatar(
                                                        "Bearer $token",
                                                        UpdateAvatarPayload(background = targetBg)
                                                    )
                                                }
                                                if (response.isSuccessful) {
                                                    sessionManager.saveBackground(targetBg)
                                                    backgroundName = targetBg
                                                } else {
                                                    errorMessage = "Gagal memperbarui background"
                                                }
                                            } catch (e: Exception) {
                                                errorMessage = e.localizedMessage
                                            } finally {
                                                isSaving = false
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = androidx.compose.ui.res.painterResource(id = bgResId),
                                    contentDescription = targetBg,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            },
            containerColor = White,
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
fun ProfileActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDestructive) Color(0xFFFFEBEE) else Color(0xFFF7F7F7),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) RedPrimary else Black,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDestructive) RedPrimary else Black
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = if (isDestructive) RedPrimary.copy(alpha = 0.4f) else GreyText,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
