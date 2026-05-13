package com.danipinion.z_talk.ui.screen.chat

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danipinion.z_talk.ui.theme.*

data class Message(
    val id: Int,
    val text: String,
    val isFromMe: Boolean,
    val isUnread: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(username: String, onBack: () -> Unit) {
    val messages = remember { getDummyMessages() }
    var textState by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // Auto scroll to bottom when keyboard appears or messages change
    val isKeyboardVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
    LaunchedEffect(messages.size, isKeyboardVisible) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val unreadCount = remember(messages) { messages.count { it.isUnread } }
    val firstUnreadIndex = remember(messages) { messages.indexOfFirst { it.isUnread } }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = username,
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

        bottomBar = {
            ChatInputBar(
                text = textState,
                onTextChange = { textState = it },
                onSend = { 
                    if (textState.isNotBlank()) {
                        // Handle send logic
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
            itemsIndexed(messages) { index, message ->
                if (index == firstUnreadIndex && unreadCount > 0) {
                    UnreadSeparator(unreadCount)
                }
                
                val prevMessage = if (index > 0) messages[index - 1] else null
                val nextMessage = if (index + 1 < messages.size) messages[index + 1] else null
                
                val isFirstInGroup = prevMessage == null || prevMessage.isFromMe != message.isFromMe
                val isLastInGroup = nextMessage == null || nextMessage.isFromMe != message.isFromMe
                
                // Add extra spacing before a new group starts
                if (isFirstInGroup && index > 0 && index != firstUnreadIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                ChatBubble(message, showAvatar = isLastInGroup && !message.isFromMe)
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
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = Color(0xFFEEEEEE)
        )
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
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = Color(0xFFEEEEEE)
        )
    }
}

@Composable
fun ChatBubble(message: Message, showAvatar: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isFromMe) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (message.isFromMe) Arrangement.End else Arrangement.Start
        ) {
            if (!message.isFromMe) {
                if (showAvatar) {
                    // Dummy Avatar
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GreyDivider),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "A", // Placeholder
                            color = Black, 
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Space for hidden avatar to keep alignment
                    Spacer(modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                color = if (message.isFromMe) RedPrimary else Color(0xFFF7F7F7),
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (message.isFromMe) 20.dp else 4.dp,
                    bottomEnd = if (message.isFromMe) 4.dp else 20.dp
                ),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = if (message.isFromMe) White else Black,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
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

            // Input Field
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFF7F7F7),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        modifier = Modifier.weight(1f),
                        textStyle = LocalTextStyle.current.copy(color = Black, fontSize = 15.sp),
                        decorationBox = { innerTextField ->
                            if (text.isEmpty()) {
                                Text("Type here...", color = GreyText, fontSize = 15.sp)
                            }
                            innerTextField()
                        }
                    )
                    
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Send Button
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
