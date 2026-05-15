package com.danipinion.z_talk.ui.screen.chat

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.danipinion.z_talk.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Message(
    val id: Int,
    val text: String,
    val isFromMe: Boolean,
    val isUnread: Boolean = false,
    val isGhost: Boolean = false,
    val isTemporary: Boolean = false,
    val isUsed: Boolean = false,
    val isStatus: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(username: String, onBack: () -> Unit) {
    val allMessages = remember { mutableStateListOf<Message>().apply { addAll(getDummyMessages()) } }
    var textState by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    var isGhostMode by remember { mutableStateOf(false) }
    var isTemporaryMode by remember { mutableStateOf(false) }
    var activeGhostId by remember { mutableIntStateOf(-1) }
    var showMenu by remember { mutableStateOf(false) }
    var isMenuVisible by remember { mutableStateOf(false) }
    var showRemoveFriendDialog by remember { mutableStateOf(false) }
    var isDialogVisible by remember { mutableStateOf(false) }
    var showBlockUserDialog by remember { mutableStateOf(false) }
    var isBlockDialogVisible by remember { mutableStateOf(false) }
    var isBlocked by remember { mutableStateOf(false) }
    var isBlockedByOther by remember { mutableStateOf(false) }
    var isRemovedByOther by remember { mutableStateOf(false) }
    var isRequestPending by remember { mutableStateOf(false) }
    var showDeleteChatDialog by remember { mutableStateOf(false) }
    var isDeleteChatDialogVisible by remember { mutableStateOf(false) }
    var showAddFriendConfirmDialog by remember { mutableStateOf(false) }
    var isAddFriendDialogVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(showMenu) {
        if (showMenu) isMenuVisible = true
    }

    LaunchedEffect(showRemoveFriendDialog) {
        if (showRemoveFriendDialog) isDialogVisible = true
    }

    LaunchedEffect(showBlockUserDialog) {
        if (showBlockUserDialog) isBlockDialogVisible = true
    }

    LaunchedEffect(showDeleteChatDialog) {
        if (showDeleteChatDialog) isDeleteChatDialogVisible = true
    }

    LaunchedEffect(showAddFriendConfirmDialog) {
        if (showAddFriendConfirmDialog) isAddFriendDialogVisible = true
    }
    
    // Screenshot Protection (FLAG_SECURE) logic
    val context = LocalContext.current
    DisposableEffect(isTemporaryMode) {
        val activity = context.findActivity()
        if (isTemporaryMode) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Handle system back button to exit Ghost Mode first, then go back to dashboard
    BackHandler {
        if (isTemporaryMode) {
            isTemporaryMode = false
        } else {
            onBack()
        }
    }

    // Auto scroll to bottom
    val isKeyboardVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
    LaunchedEffect(allMessages.size, isKeyboardVisible, isTemporaryMode) {
        if (allMessages.isNotEmpty()) {
            listState.animateScrollToItem(allMessages.size) // Approximate
        }
    }

    val displayMessages = if (isTemporaryMode) {
        // Show only temporary messages for the current active ghost session
        allMessages.filter { it.isTemporary && it.id > activeGhostId && it.id < activeGhostId + 1000 }
        // Note: Using a simpler logic since dummy messages use high IDs
    } else {
        allMessages.filter { !it.isTemporary }
    }
    
    val unreadCount = remember(displayMessages) { displayMessages.count { it.isUnread } }
    val firstUnreadIndex = remember(displayMessages) { displayMessages.indexOfFirst { it.isUnread } }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .combinedClickable(
                                        onLongClick = { if (!isTemporaryMode) showMenu = true },
                                        onClick = { /* Could open profile */ }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (isTemporaryMode) "Ghost Session" else username,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTemporaryMode) RedPrimary else Black
                                    )
                                }
                            }

                            if (showMenu) {
                                val density = LocalDensity.current
                                Popup(
                                    onDismissRequest = { isMenuVisible = false },
                                    offset = IntOffset(0, with(density) { 60.dp.roundToPx() }), // Lowered position
                                    alignment = Alignment.BottomCenter,
                                    properties = PopupProperties(focusable = true)
                                ) {
                                    AnimatedVisibility(
                                        visible = isMenuVisible,
                                        enter = fadeIn() + scaleIn(transformOrigin = TransformOrigin(0.5f, 0f)),
                                        exit = fadeOut() + scaleOut(transformOrigin = TransformOrigin(0.5f, 0f))
                                    ) {
                                        Surface(
                                            modifier = Modifier.width(220.dp),
                                            shape = RoundedCornerShape(18.dp),
                                            color = White,
                                            border = BorderStroke(0.5.dp, Color(0xFFEEEEEE)),
                                            shadowElevation = 0.dp
                                        ) {
                                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                                DropdownMenuItem(
                                                    text = { Text("Search in Chat", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333)) },
                                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF888888), modifier = Modifier.size(22.dp)) },
                                                    onClick = { isMenuVisible = false }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Mute Notifications", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333)) },
                                                    leadingIcon = { Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = Color(0xFF888888), modifier = Modifier.size(22.dp)) },
                                                    onClick = { isMenuVisible = false }
                                                )
                                                
                                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = Color(0xFFF5F5F5), thickness = 1.dp)

                                                DropdownMenuItem(
                                                    text = { Text("Clear Chat", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333)) },
                                                    leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFF888888), modifier = Modifier.size(22.dp)) },
                                                    onClick = { 
                                                        allMessages.clear()
                                                        isMenuVisible = false 
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar("Chat cleared successfully")
                                                        }
                                                    }
                                                )
                                                
                                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = Color(0xFFF5F5F5), thickness = 1.dp)
                                                
                                                DropdownMenuItem(
                                                    text = { Text("Remove Friend", color = RedPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) },
                                                    leadingIcon = { Icon(Icons.Default.PersonRemove, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(22.dp)) },
                                                    onClick = { 
                                                        isMenuVisible = false
                                                        showRemoveFriendDialog = true
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(if (isBlocked) "Unblock User" else "Block User", color = RedPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) },
                                                    leadingIcon = { Icon(if (isBlocked) Icons.Default.CheckCircle else Icons.Default.Block, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(22.dp)) },
                                                    onClick = { 
                                                        isMenuVisible = false
                                                        if (isBlocked) {
                                                            isBlocked = false
                                                            allMessages.add(
                                                                Message(
                                                                    id = allMessages.size + 1,
                                                                    text = "You unblocked this friend",
                                                                    isFromMe = true,
                                                                    isStatus = true
                                                                )
                                                            )
                                                        } else {
                                                            showBlockUserDialog = true
                                                        }
                                                    }
                                                )

                                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = Color(0xFFF5F5F5), thickness = 1.dp)

                                                DropdownMenuItem(
                                                    text = { Text("Simulate: They Block Me", fontSize = 13.sp, color = Color.Gray) },
                                                    onClick = { 
                                                        isBlockedByOther = !isBlockedByOther
                                                        isMenuVisible = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Simulate: They Remove Me", fontSize = 13.sp, color = Color.Gray) },
                                                    onClick = { 
                                                        isRemovedByOther = !isRemovedByOther
                                                        isMenuVisible = false
                                                    }
                                                )
                                            }
                                        }

                                        // Cleanup showMenu after exit animation
                                        LaunchedEffect(isMenuVisible) {
                                            if (!isMenuVisible) {
                                                delay(300) // Wait for exit animation to complete
                                                showMenu = false
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF7F7F7))
                            .clickable { 
                                if (isTemporaryMode) {
                                    isTemporaryMode = false
                                } else {
                                    onBack()
                                }
                            },
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
                actions = {
                    if (!isTemporaryMode) {
                        IconButton(
                            onClick = { 
                                allMessages.add(
                                    Message(
                                        id = allMessages.size + 1,
                                        text = "Ghost Message",
                                        isFromMe = true,
                                        isGhost = true
                                    )
                                )
                            },
                            enabled = !isBlocked && !isBlockedByOther && !isRemovedByOther,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .then(if (isBlocked || isBlockedByOther || isRemovedByOther) Modifier.alpha(0.5f) else Modifier)
                        ) {
                            Text("👻", fontSize = 22.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = White),
                modifier = Modifier.statusBarsPadding()
            )
        },

        snackbarHost = { 
            SnackbarHost(snackbarHostState) { data ->
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = White,
                    border = BorderStroke(1.dp, Color(0xFFF0F0F0)),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(28.dp),
                            shape = CircleShape,
                            color = RedPrimary.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = RedPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = data.visuals.message,
                            color = Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.1.sp
                        )
                    }
                }
            }
        },

        bottomBar = {
            if (isRemovedByOther) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = White,
                    tonalElevation = 8.dp,
                    border = BorderStroke(1.dp, Color(0xFFF0F0F0))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isRequestPending) "Friend request sent!" else "You are no longer friends",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isRequestPending) 
                                "Waiting for $username to accept your invitation." 
                                else "Add $username back to start chatting again.",
                            fontSize = 13.sp,
                            color = GreyText,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isRequestPending) {
                                Button(
                                    onClick = { 
                                        isRequestPending = false 
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F5F5), contentColor = Black)
                                ) {
                                    Text("Cancel Request")
                                }
                            } else {
                                Button(
                                    onClick = { showDeleteChatDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F5F5), contentColor = Black)
                                ) {
                                    Text("Delete Chat")
                                }
                                Button(
                                    onClick = { showAddFriendConfirmDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary, contentColor = White)
                                ) {
                                    Text("Add Friend")
                                }
                            }
                        }
                    }
                }
            } else {
                ChatInputBar(
                    text = textState,
                    onTextChange = { textState = it },
                    onSend = { 
                        if (textState.isNotBlank()) {
                            allMessages.add(
                                Message(
                                    id = allMessages.size + 1,
                                    text = textState,
                                    isFromMe = true,
                                    isGhost = isGhostMode,
                                    isTemporary = isTemporaryMode
                                )
                            )
                            textState = ""
                        }
                    },
                    enabled = !isBlocked && !isBlockedByOther,
                    disabledReason = if (isBlockedByOther) "You cannot send messages to this contact" else null
                )
            }
        },
        containerColor = White
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (displayMessages.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = Color(0xFFF7F7F7)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = Color(0xFFCCCCCC)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "No Messages Yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Send a message to start the conversation",
                        fontSize = 14.sp,
                        color = GreyText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(displayMessages) { index, message ->
                        if (message.isStatus) {
                            StatusSeparator(text = message.text)
                        } else {
                            if (index == firstUnreadIndex && unreadCount > 0 && !isTemporaryMode) {
                                UnreadSeparator(unreadCount)
                            }
                            
                            val prevMessage = if (index > 0) displayMessages[index - 1] else null
                            val nextMessage = if (index + 1 < displayMessages.size) displayMessages[index + 1] else null
                            
                            val isFirstInGroup = prevMessage == null || prevMessage.isFromMe != message.isFromMe
                            val isLastInGroup = nextMessage == null || nextMessage.isFromMe != message.isFromMe
                            
                            if (isFirstInGroup && index > 0 && index != firstUnreadIndex && (prevMessage?.isStatus == false)) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            
                            ChatBubble(
                                message = message, 
                                showAvatar = isLastInGroup && !message.isFromMe && !isTemporaryMode,
                                onGhostClick = { 
                                    if (message.isGhost && !message.isUsed) {
                                        activeGhostId = message.id
                                        
                                        // Mark as used
                                        val idx = allMessages.indexOfFirst { it.id == message.id }
                                        if (idx != -1) {
                                            allMessages[idx] = allMessages[idx].copy(isUsed = true)
                                        }

                                        // Add dummy messages for THIS specific session
                                        allMessages.addAll(listOf(
                                            Message(id = activeGhostId + 1, text = "Psst... This is a fresh Ghost Session.", isFromMe = false, isTemporary = true),
                                            Message(id = activeGhostId + 2, text = "Only messages from this invite appear here.", isFromMe = true, isTemporary = true),
                                            Message(id = activeGhostId + 3, text = "Everything wipes when you leave. 🤫", isFromMe = false, isTemporary = true)
                                        ))
                                        isTemporaryMode = true 
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Remove Friend Confirmation Dialog
        if (showRemoveFriendDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { isDialogVisible = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Dim background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable(enabled = false) {}
                    )

                    AnimatedVisibility(
                        visible = isDialogVisible,
                        enter = fadeIn() + scaleIn(initialScale = 0.9f),
                        exit = fadeOut() + scaleOut(targetScale = 0.9f)
                    ) {
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = 32.dp)
                                .widthIn(max = 320.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = White,
                            tonalElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Warning Icon
                                Surface(
                                    modifier = Modifier.size(64.dp),
                                    shape = CircleShape,
                                    color = RedPrimary.copy(alpha = 0.1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = RedPrimary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = "Remove Friend?",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Black
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Are you sure you want to remove $username? This action will also delete your chat history.",
                                    fontSize = 14.sp,
                                    color = GreyText,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 20.sp
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Cancel Button
                                    Button(
                                        onClick = { isDialogVisible = false },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFF5F5F5),
                                            contentColor = GreyText
                                        ),
                                        contentPadding = PaddingValues(vertical = 12.dp)
                                    ) {
                                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                                    }

                                    // Remove Button
                                    Button(
                                        onClick = { 
                                            isDialogVisible = false
                                            showRemoveFriendDialog = false
                                            onBack()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = RedPrimary,
                                            contentColor = White
                                        ),
                                        contentPadding = PaddingValues(vertical = 12.dp)
                                    ) {
                                        Text("Remove", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        // Cleanup dialog state after exit animation
                        LaunchedEffect(isDialogVisible) {
                            if (!isDialogVisible) {
                                delay(300)
                                showRemoveFriendDialog = false
                            }
                        }
                    }
                }
            }
        }

        // Block User Confirmation Dialog
        if (showBlockUserDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { isBlockDialogVisible = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable(enabled = false) {}
                    )

                    AnimatedVisibility(
                        visible = isBlockDialogVisible,
                        enter = fadeIn() + scaleIn(initialScale = 0.9f),
                        exit = fadeOut() + scaleOut(targetScale = 0.9f)
                    ) {
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = 32.dp)
                                .widthIn(max = 320.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = White,
                            tonalElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    modifier = Modifier.size(64.dp),
                                    shape = CircleShape,
                                    color = RedPrimary.copy(alpha = 0.1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Block,
                                            contentDescription = null,
                                            tint = RedPrimary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = "Block $username?",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Black
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Blocked users will not be able to send you messages or see your status. You can unblock them later in settings.",
                                    fontSize = 14.sp,
                                    color = GreyText,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 20.sp
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { isBlockDialogVisible = false },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFF5F5F5),
                                            contentColor = GreyText
                                        ),
                                        contentPadding = PaddingValues(vertical = 12.dp)
                                    ) {
                                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                                    }

                                    Button(
                                        onClick = { 
                                            isBlocked = true
                                            isBlockDialogVisible = false
                                            allMessages.add(
                                                Message(
                                                    id = allMessages.size + 1,
                                                    text = "You blocked this friend",
                                                    isFromMe = true,
                                                    isStatus = true
                                                )
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Black,
                                            contentColor = White
                                        ),
                                        contentPadding = PaddingValues(vertical = 12.dp)
                                    ) {
                                        Text("Block", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        LaunchedEffect(isBlockDialogVisible) {
                            if (!isBlockDialogVisible) {
                                delay(300)
                                showBlockUserDialog = false
                            }
                        }
                    }
                }
            }
        }

        // Delete Chat Confirmation Dialog
        if (showDeleteChatDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { isDeleteChatDialogVisible = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable(enabled = false) {}
                    )

                    AnimatedVisibility(
                        visible = isDeleteChatDialogVisible,
                        enter = fadeIn() + scaleIn(initialScale = 0.9f),
                        exit = fadeOut() + scaleOut(targetScale = 0.9f)
                    ) {
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = 32.dp)
                                .widthIn(max = 320.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = White,
                            tonalElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    modifier = Modifier.size(64.dp),
                                    shape = CircleShape,
                                    color = RedPrimary.copy(alpha = 0.1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = RedPrimary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = "Delete Conversation?",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Black
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "This will permanently delete your chat with $username from your device.",
                                    fontSize = 14.sp,
                                    color = GreyText,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 20.sp
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { isDeleteChatDialogVisible = false },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFF5F5F5),
                                            contentColor = GreyText
                                        ),
                                        contentPadding = PaddingValues(vertical = 12.dp)
                                    ) {
                                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                                    }

                                    Button(
                                        onClick = { 
                                            isDeleteChatDialogVisible = false
                                            onBack()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = RedPrimary,
                                            contentColor = White
                                        ),
                                        contentPadding = PaddingValues(vertical = 12.dp)
                                    ) {
                                        Text("Delete", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        LaunchedEffect(isDeleteChatDialogVisible) {
                            if (!isDeleteChatDialogVisible) {
                                delay(300)
                                showDeleteChatDialog = false
                            }
                        }
                    }
                }
            }
        }

        // Add Friend Confirmation Dialog
        if (showAddFriendConfirmDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { isAddFriendDialogVisible = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable(enabled = false) {}
                    )

                    AnimatedVisibility(
                        visible = isAddFriendDialogVisible,
                        enter = fadeIn() + scaleIn(initialScale = 0.9f),
                        exit = fadeOut() + scaleOut(targetScale = 0.9f)
                    ) {
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = 32.dp)
                                .widthIn(max = 320.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = White,
                            tonalElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    modifier = Modifier.size(64.dp),
                                    shape = CircleShape,
                                    color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = null,
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = "Send Friend Request?",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Black
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Send a new friend request to $username to start communicating again.",
                                    fontSize = 14.sp,
                                    color = GreyText,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 20.sp
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { isAddFriendDialogVisible = false },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFF5F5F5),
                                            contentColor = GreyText
                                        ),
                                        contentPadding = PaddingValues(vertical = 12.dp)
                                    ) {
                                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                                    }

                                    Button(
                                        onClick = { 
                                            isAddFriendDialogVisible = false
                                            isRequestPending = true 
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4CAF50),
                                            contentColor = White
                                        ),
                                        contentPadding = PaddingValues(vertical = 12.dp)
                                    ) {
                                        Text("Send", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        LaunchedEffect(isAddFriendDialogVisible) {
                            if (!isAddFriendDialogVisible) {
                                delay(300)
                                showAddFriendConfirmDialog = false
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnreadSeparator(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = Color(0xFFEEEEEE))
        Surface(
            color = Color(0xFFF7F7F7),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = "$count New Messages",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GreyText
            )
        }
        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = Color(0xFFEEEEEE))
    }
}

@Composable
fun StatusSeparator(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            color = Color(0xFFF7F7F7),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GreyText
            )
        }
    }
}

@Composable
fun ChatBubble(
    message: Message, 
    showAvatar: Boolean,
    onGhostClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isFromMe) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (message.isFromMe) Arrangement.End else Arrangement.Start
        ) {
            if (!message.isFromMe && !message.isTemporary) {
                if (showAvatar) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GreyDivider),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "A",
                            color = Black, 
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            val bubbleColor = when {
                message.isGhost -> if (message.isFromMe) Color(0xFF311B92) else Color(0xFF004D40)
                message.isTemporary -> if (message.isFromMe) Color(0xFFFFF1F1) else Color(0xFFF3E5F5)
                else -> if (message.isFromMe) RedPrimary else Color(0xFFF7F7F7)
            }

            Surface(
                color = bubbleColor,
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (message.isFromMe) 20.dp else 4.dp,
                    bottomEnd = if (message.isFromMe) 4.dp else 20.dp
                ),
                border = if (message.isTemporary) BorderStroke(1.5.dp, if (message.isFromMe) RedPrimary else Color(0xFF9C27B0)) else null,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clickable(enabled = message.isGhost && !message.isUsed) { onGhostClick() }
            ) {
                if (message.isGhost) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (message.isUsed) "💨" else "👻", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (message.isUsed) "Expired Ghost" else "Ghost Message",
                                color = White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (message.isUsed) "Conversation already wiped" else "Tap to enter temporary chat",
                                color = White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    Text(
                        text = message.text,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        color = if (message.isTemporary && message.isFromMe) Black 
                                else if (message.isFromMe || message.isGhost) White 
                                else Black,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean = true,
    disabledReason: String? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .then(if (!enabled) Modifier.alpha(0.6f) else Modifier),
        color = White,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = if (enabled) Color(0xFFF7F7F7) else Color(0xFFEEEEEE),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = if (enabled) onTextChange else { _ -> },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    textStyle = LocalTextStyle.current.copy(
                        color = if (enabled) Black else GreyText, 
                        fontSize = 15.sp
                    ),
                    enabled = enabled,
                    decorationBox = { innerTextField ->
                        if (text.isEmpty()) {
                            Text(
                                text = if (enabled) "Type here..." else (disabledReason ?: "You have blocked this contact"), 
                                color = GreyText, 
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (enabled) RedPrimary else Color(0xFFDDDDDD))
                    .clickable(enabled = enabled) { onSend() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

fun getDummyMessages(): List<Message> = listOf(
    Message(1, "How's the tour plan for the summer vacation?", false),
    Message(2, "vocation?", false),
    Message(3, "Yeah, I was thinking we could explore the West Coast this year.", true),
    Message(4, "Hey Olivia! have you thought about our tour plan for the summer vocation?", false),
    Message(5, "Yeah, I was thinking we could explore the West Coast this year.", true),
    Message(6, "That sounds awesome! I've always wanted to see the Golden Gate Bridge", false, isUnread = true),
    Message(7, "Besides the Golden Gate Bridge, we should definitely visit Alcatraz, Fisherman's Wharf.", true, isUnread = true),
    Message(8, "That sounds perfect. What do you want to do in San Francisco?", false, isUnread = true)
)

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
