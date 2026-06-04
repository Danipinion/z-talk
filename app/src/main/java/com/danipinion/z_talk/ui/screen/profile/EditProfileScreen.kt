package com.danipinion.z_talk.ui.screen.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danipinion.z_talk.data.local.SessionManager
import com.danipinion.z_talk.data.remote.RetrofitClient
import com.danipinion.z_talk.data.remote.UpdateAvatarPayload
import com.danipinion.z_talk.ui.utils.AvatarHelper
import com.danipinion.z_talk.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    
    var bio by remember { mutableStateOf("Mobile Developer who focuses on\nsimplicity & aesthetics.") }
    var selectedAvatar by remember { mutableStateOf(sessionManager.getAvatar() ?: "panda") }
    var isSaving by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Edit Profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF7F7F7))
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = GreyText,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = White),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar Selector Section
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clickable { showAvatarPicker = true },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .border(width = 3.dp, color = RedPrimary, shape = CircleShape)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(GreyLight),
                    contentAlignment = Alignment.Center
                ) {
                    val avatarResId = AvatarHelper.getAvatarResourceId(context, selectedAvatar)
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = avatarResId),
                        contentDescription = "Selected Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Camera overlay / edit badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(36.dp)
                        .shadow(elevation = 4.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(RedPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change Avatar",
                        tint = White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "About You",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = GreyText,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = { Text("Tell us about yourself...") },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Black,
                    unfocusedTextColor = Black,
                    focusedBorderColor = RedPrimary,
                    unfocusedBorderColor = GreyDivider,
                    cursorColor = RedPrimary
                )
            )
            
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (isSaving) return@Button
                    isSaving = true
                    errorMessage = null
                    val token = sessionManager.getToken()
                    if (token.isNullOrEmpty()) {
                        errorMessage = "Not logged in"
                        isSaving = false
                        return@Button
                    }
                    scope.launch {
                        try {
                            val response = withContext(Dispatchers.IO) {
                                RetrofitClient.apiService.updateAvatar(
                                    "Bearer $token",
                                    UpdateAvatarPayload(selectedAvatar)
                                )
                            }
                            if (response.isSuccessful) {
                                sessionManager.saveAvatar(selectedAvatar)
                                onBack()
                            } else {
                                errorMessage = response.body()?.error ?: response.message() ?: "Failed to update profile"
                            }
                        } catch (e: Exception) {
                            errorMessage = e.localizedMessage ?: "Unknown error occurred"
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "Save Changes", 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                }
            }
        }
    }

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
                        items(AvatarHelper.AVATAR_LIST) { avatarName ->
                            val avatarResId = AvatarHelper.getAvatarResourceId(context, avatarName)
                            val isSelected = avatarName == selectedAvatar
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
                                        selectedAvatar = avatarName
                                        showAvatarPicker = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = androidx.compose.ui.res.painterResource(id = avatarResId),
                                    contentDescription = avatarName,
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
}
