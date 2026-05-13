package com.danipinion.z_talk.ui.screen.chat

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danipinion.z_talk.ui.theme.*
import kotlinx.coroutines.delay

data class Message(
    val id: Int,
    val text: String,
    val isFromMe: Boolean,
    val isUnread: Boolean = false,
    val isGhost: Boolean = false,
    val isTemporary: Boolean = false,
    val isUsed: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(username: String, onBack: () -> Unit) {
    val allMessages = remember { mutableStateListOf<Message>().apply { addAll(getDummyMessages()) } }
    var textState by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    var isGhostMode by remember { mutableStateOf(false) }
    var isTemporaryMode by remember { mutableStateOf(false) }
    var activeGhostId by remember { mutableIntStateOf(-1) }
    
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isTemporaryMode) "Ghost Session" else username,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTemporaryMode) RedPrimary else Black
                        )  
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
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("👻", fontSize = 22.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = White),
                modifier = Modifier.statusBarsPadding()
            )
        },

        bottomBar = {
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
                }
            )
        },
        containerColor = White
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(displayMessages) { index, message ->
                if (index == firstUnreadIndex && unreadCount > 0 && !isTemporaryMode) {
                    UnreadSeparator(unreadCount)
                }
                
                val prevMessage = if (index > 0) displayMessages[index - 1] else null
                val nextMessage = if (index + 1 < displayMessages.size) displayMessages[index + 1] else null
                
                val isFirstInGroup = prevMessage == null || prevMessage.isFromMe != message.isFromMe
                val isLastInGroup = nextMessage == null || nextMessage.isFromMe != message.isFromMe
                
                if (isFirstInGroup && index > 0 && index != firstUnreadIndex) {
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
                            // We give them IDs based on the activeGhostId to "isolate" them
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
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding(),
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
                color = Color(0xFFF7F7F7),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    textStyle = LocalTextStyle.current.copy(color = Black, fontSize = 15.sp),
                    decorationBox = { innerTextField ->
                        if (text.isEmpty()) {
                            Text("Type here...", color = GreyText, fontSize = 15.sp)
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
                    .background(RedPrimary)
                    .clickable { onSend() },
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
