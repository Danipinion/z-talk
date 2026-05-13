package com.danipinion.z_talk.ui.screen.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.danipinion.z_talk.ui.theme.*
import androidx.compose.animation.*

@Composable
fun ScanScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    var isFlashOn by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Scan, 1: My Code

    BackHandler {
        if (selectedTab == 1) {
            selectedTab = 0
        } else {
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Black)) {
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }.using(SizeTransform(clip = false))
            },
            label = "tabTransition"
        ) { targetTab ->
            if (targetTab == 0) {
                // Scan Content
                Box(modifier = Modifier.fillMaxSize()) {
                    if (hasCameraPermission) {
                        CameraPreview(isFlashOn)
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Camera permission required", color = White)
                        }
                    }
                    QRScannerOverlay(onBack, isFlashOn, onToggleFlash = { isFlashOn = !isFlashOn })
                }
            } else {
                // My Code Content
                MyCodeContent(onBack)
            }
        }

        // Bottom Tab Switcher
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
        ) {
            ScanTabSwitcher(selectedTab) { selectedTab = it }
        }
    }
}


@Composable
fun CameraPreview(isFlashOn: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    LaunchedEffect(isFlashOn) {
        camera?.cameraControl?.enableTorch(isFlashOn)
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                try {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, executor)
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun QRScannerOverlay(onBack: () -> Unit, isFlashOn: Boolean, onToggleFlash: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "textAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash Button without ripple
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(White.copy(alpha = 0.2f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggleFlash() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Flash",
                    tint = White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Close Button without ripple
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(White.copy(alpha = 0.2f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Center QR Frame
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .padding(20.dp)
            ) {
                // Corner Borders (Red Primary)
                val strokeWidth = 5.dp
                val cornerSize = 40.dp
                
                // Top Left
                Box(modifier = Modifier.align(Alignment.TopStart).size(cornerSize).border(width = strokeWidth, color = RedPrimary, shape = RoundedCornerShape(topStart = 24.dp)))
                // Top Right
                Box(modifier = Modifier.align(Alignment.TopEnd).size(cornerSize).border(width = strokeWidth, color = RedPrimary, shape = RoundedCornerShape(topEnd = 24.dp)))
                // Bottom Left
                Box(modifier = Modifier.align(Alignment.BottomStart).size(cornerSize).border(width = strokeWidth, color = RedPrimary, shape = RoundedCornerShape(bottomStart = 24.dp)))
                // Bottom Right
                Box(modifier = Modifier.align(Alignment.BottomEnd).size(cornerSize).border(width = strokeWidth, color = RedPrimary, shape = RoundedCornerShape(bottomEnd = 24.dp)))

                Text(
                    text = "Scan a QR Code",
                    color = White.copy(alpha = alpha),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}


@Composable
fun MyCodeContent(onBack: () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(RedPrimary)
    ) {
        val width = maxWidth.value.toInt()
        val height = maxHeight.value.toInt()

        // "Confetti" / Decorations (Random positions)
        repeat(30) { index ->
            FloatingDecoration(index, width, height)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(White.copy(alpha = 0.2f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = White, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "Z-Talk Connect",
                color = White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Your gateway to new stories",
                color = White.copy(alpha = 0.8f),
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // QR Card with slight tilt for style
            Surface(
                modifier = Modifier
                    .size(280.dp)
                    .graphicsLayer(rotationZ = -2f),
                shape = RoundedCornerShape(32.dp),
                color = White,
                shadowElevation = 24.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(180.dp),
                        tint = Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Text(
                text = "“Every connection starts with a simple scan.”",
                color = White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Text(
                text = "@danipinion",
                color = White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
fun FloatingDecoration(index: Int, screenWidth: Int, screenHeight: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "snowfall")
    
    // Random initial horizontal position
    val startX = remember { (0..screenWidth).random().toFloat() }
    
    // Slow falling animation (Y axis)
    val durationY = 8000 + (index * 1200)
    val delayY = (index * 400)
    
    val yOffset by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = screenHeight.toFloat() + 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationY, delayMillis = delayY, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "y"
    )

    // Slight horizontal sway (X axis)
    val swayAmount = 25f + (index * 3)
    val durationX = 3000 + (index * 500)
    val xSway by infiniteTransition.animateFloat(
        initialValue = -swayAmount,
        targetValue = swayAmount,
        animationSpec = infiniteRepeatable(
            animation = tween(durationX, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "xSway"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5000 + (index * 1000), easing = LinearEasing)),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .offset(x = (startX + xSway).dp, y = yOffset.dp)
            .rotate(rotation)
            .size(if (index % 4 == 0) 12.dp else 7.dp)
            .background(White.copy(alpha = if (index % 3 == 0) 0.4f else 0.2f), if (index % 2 == 0) CircleShape else RoundedCornerShape(2.dp))
    )
}


@Composable
fun ScanTabSwitcher(selectedIndex: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .background(White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
            .padding(4.dp)
            .width(220.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tabs = listOf("Scan", "My Code")
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isSelected) White.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabSelected(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isSelected) White else White.copy(alpha = 0.6f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}
