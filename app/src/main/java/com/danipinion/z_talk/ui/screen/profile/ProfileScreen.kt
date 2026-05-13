package com.danipinion.z_talk.ui.screen.profile

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

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
import coil.compose.AsyncImage
import com.danipinion.z_talk.ui.theme.*

@Composable
fun ProfileScreen(onBack: () -> Unit = {}, onEditProfile: () -> Unit = {}) {
    BackHandler { onBack() }
    var showMoodPicker by remember { mutableStateOf(false) }
    var selectedMood by remember { mutableStateOf<String?>(null) }
    val moods = listOf("😊", "😎", "😴", "🔥", "🚀", "🎮", "📚", "🎨", "💻", "🍕", "🏖️", "✨")

    var showLogoutDialog by remember { mutableStateOf(false) }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .verticalScroll(rememberScrollState())
    ) {
        // Banner & Avatar Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            // Banner (Placeholder with clouds color)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                    .background(Color(0xFFDEEBF7)) // Light cloud blue
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(White.copy(alpha = 0.9f))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft, 
                        contentDescription = "Back", 
                        tint = Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Mood Button (+)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
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
                        .background(GreyLight),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Placeholder Image or Initial
                        Text(
                            text = "D",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )
                    }
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
                        .clickable { photoPickerLauncher.launch("image/*") },
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

        // Name & Bio
        Text(
            text = "Dani Pinion",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Black,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Mobile Developer who focuses on\nsimplicity & aesthetics.",
            fontSize = 15.sp,
            color = GreyText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Stats Card
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF7F7F7),
            border = BorderStroke(1.dp, Color(0xFFEEEEEE))
        ) {
            Row(
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(modifier = Modifier.weight(1f), count = "1.2K", label = "Friends")
                
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(GreyDivider))
                
                StatItem(modifier = Modifier.weight(1f), count = "8.4K", label = "Chat Sent")
                
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(GreyDivider))
                
                StatItem(modifier = Modifier.weight(1f), count = "124h", label = "Usage")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action List
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileActionItem(
                icon = Icons.Default.Edit,
                title = "Edit Profile",
                onClick = onEditProfile
            )
            
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
                    onClick = { showLogoutDialog = false },
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
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GreyLight)
                                    .clickable {
                                        selectedMood = mood
                                        showMoodPicker = false
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

@Composable
fun StatItem(modifier: Modifier = Modifier, count: String, label: String) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count, 
            fontSize = 20.sp, 
            fontWeight = FontWeight.ExtraBold, 
            color = Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label, 
            fontSize = 13.sp, 
            color = GreyText
        )
    }
}
