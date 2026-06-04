package com.danipinion.z_talk.ui.screen.chat

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.map
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import com.danipinion.z_talk.ui.utils.AvatarHelper
import com.danipinion.z_talk.ui.theme.*
import com.danipinion.z_talk.data.local.AppDatabase
import com.danipinion.z_talk.data.local.entity.MessageEntity
import com.danipinion.z_talk.data.local.entity.ChatRoomEntity
import com.danipinion.z_talk.data.local.SessionManager
import com.danipinion.z_talk.data.remote.WebSocketManager
import com.danipinion.z_talk.data.remote.RetrofitClient
import com.danipinion.z_talk.data.remote.SendFriendRequestPayload
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Message(
    val id: Int,
    val text: String,
    val isFromMe: Boolean,
    val isUnread: Boolean = false,
    val isGhost: Boolean = false,
    val isTemporary: Boolean = false,
    val isUsed: Boolean = false,
    val isStatus: Boolean = false,
    val ghostMessageId: String? = null,
    val realMessageId: String = "",
    val isPending: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(
    username: String,
    friendId: String,
    senderId: String,
    webSocketManager: WebSocketManager?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val roomId = remember(senderId, friendId) {
        if (senderId < friendId) "${senderId}_${friendId}" else "${friendId}_${senderId}"
    }

    val friendsList by db.friendDao().getAllFriends().collectAsState(initial = emptyList())
    val partnerAvatar = remember(friendsList, friendId) {
        friendsList.find { it.id == friendId }?.avatar
    }
    val partnerMood = remember(friendsList, friendId) {
        friendsList.find { it.id == friendId }?.mood
    }

    val selectedMessageIds = remember { mutableStateListOf<String>() }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isDeleteConfirmDialogVisible by remember { mutableStateOf(false) }
    var textState by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    var isGhostMode by remember { mutableStateOf(false) }
    var isTemporaryMode by remember { mutableStateOf(false) }
    var activeGhostId by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var isMenuVisible by remember { mutableStateOf(false) }
    var showRemoveFriendDialog by remember { mutableStateOf(false) }
    var isDialogVisible by remember { mutableStateOf(false) }
    var showBlockUserDialog by remember { mutableStateOf(false) }
    var isBlockDialogVisible by remember { mutableStateOf(false) }
    val isBlocked by remember(roomId) {
        db.messageDao().hasMessageWithTextFlow(roomId, "You blocked this friend")
    }.collectAsState(initial = false)

    val isBlockedByOther by remember(roomId) {
        db.messageDao().hasMessageWithTextFlow(roomId, "You are blocked by this user")
    }.collectAsState(initial = false)
    var isRemovedByOther by remember { mutableStateOf(false) }
    var isRequestPending by remember { mutableStateOf(false) }
    var showDeleteChatDialog by remember { mutableStateOf(false) }
    var isDeleteChatDialogVisible by remember { mutableStateOf(false) }
    var showAddFriendConfirmDialog by remember { mutableStateOf(false) }
    var isAddFriendDialogVisible by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var highlightedMessageId by remember { mutableStateOf<Int?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }


    LaunchedEffect(friendsList) {
        val isStillFriend = friendsList.any { it.id == friendId }
        isRemovedByOther = !isStillFriend
    }

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
    LaunchedEffect(highlightedMessageId) {
        if (highlightedMessageId != null) {
            delay(2000)
            highlightedMessageId = null
        }
    }

    LaunchedEffect(searchQuery) {
        delay(300) // Debounce
        debouncedQuery = searchQuery
    }

    val searchResultsState = remember(roomId, debouncedQuery) {
        if (debouncedQuery.isBlank()) {
            kotlinx.coroutines.flow.flowOf(emptyList<MessageEntity>())
        } else {
            db.messageDao().searchMessagesForRoom(roomId, "%$debouncedQuery%")
        }
    }.collectAsState(initial = emptyList())

    val searchResults = remember(searchResultsState.value) {
        searchResultsState.value.map { entity ->
            Message(
                id = entity.messageId.hashCode(),
                text = entity.text,
                isFromMe = entity.senderId == senderId,
                isUnread = false,
                isGhost = entity.isGhost,
                isUsed = entity.isUsed,
                isTemporary = entity.isTemporary,
                isStatus = entity.text.startsWith("You blocked") || entity.text.startsWith("You unblocked") || entity.text.startsWith("You are blocked"),
                ghostMessageId = entity.ghostMessageId,
                realMessageId = entity.messageId,
                isPending = entity.isPending
            )
        }
    }
    
    // Clean up temporary messages from database when leaving Ghost Session or screen
    LaunchedEffect(isTemporaryMode) {
        if (!isTemporaryMode) {
            withContext(Dispatchers.IO) {
                db.messageDao().deleteTemporaryMessages(roomId)
                if (activeGhostId.isNotEmpty()) {
                    db.messageDao().markGhostAsUsed(activeGhostId)
                }
            }
            if (activeGhostId.isNotEmpty()) {
                webSocketManager?.sendUseGhost(roomId, friendId, activeGhostId)
                activeGhostId = ""
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val ghostIdToUse = activeGhostId
            if (ghostIdToUse.isNotEmpty()) {
                Thread {
                    db.messageDao().deleteTemporaryMessages(roomId)
                    db.messageDao().markGhostAsUsed(ghostIdToUse)
                    webSocketManager?.sendUseGhost(roomId, friendId, ghostIdToUse)
                }.start()
            } else {
                Thread {
                    db.messageDao().deleteTemporaryMessages(roomId)
                }.start()
            }
        }
    }
    
    // Screenshot Protection (FLAG_SECURE) logic
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
        if (selectedMessageIds.isNotEmpty()) {
            selectedMessageIds.clear()
        } else if (isTemporaryMode) {
            isTemporaryMode = false
        } else {
            onBack()
        }
    }

    val pager = remember(roomId) {
        androidx.paging.Pager(
            config = androidx.paging.PagingConfig(
                pageSize = 15,
                enablePlaceholders = false,
                initialLoadSize = 15
            ),
            pagingSourceFactory = { db.messageDao().getMessagesForRoomPaging(roomId) }
        )
    }

    val messagesFlow = remember(roomId, isTemporaryMode, activeGhostId) {
        if (isTemporaryMode) {
            db.messageDao().getTemporaryMessagesFlow(roomId, activeGhostId).map { entities ->
                val sorted = entities.reversed()
                androidx.paging.PagingData.from(sorted.map { entity ->
                    Message(
                        id = entity.messageId.hashCode(),
                        text = entity.text,
                        isFromMe = entity.senderId == senderId,
                        isUnread = false,
                        isGhost = entity.isGhost,
                        isUsed = entity.isUsed,
                        isTemporary = entity.isTemporary,
                        isStatus = entity.text.startsWith("You blocked") || entity.text.startsWith("You unblocked") || entity.text.startsWith("You are blocked"),
                        ghostMessageId = entity.ghostMessageId,
                        realMessageId = entity.messageId,
                        isPending = entity.isPending
                    )
                })
            }
        } else {
            pager.flow.map { pagingData ->
                pagingData.map { entity ->
                    Message(
                        id = entity.messageId.hashCode(),
                        text = entity.text,
                        isFromMe = entity.senderId == senderId,
                        isUnread = false,
                        isGhost = entity.isGhost,
                        isUsed = entity.isUsed,
                        isTemporary = entity.isTemporary,
                        isStatus = entity.text.startsWith("You blocked") || entity.text.startsWith("You unblocked") || entity.text.startsWith("You are blocked"),
                        ghostMessageId = entity.ghostMessageId,
                        realMessageId = entity.messageId,
                        isPending = entity.isPending
                    )
                }
            }
        }
    }

    val lazyPagingItems = messagesFlow.collectAsLazyPagingItems()

    // Read unread count directly from database
    val unreadCount by db.messageDao().getUnreadCountForRoom(roomId).collectAsState(initial = 0)

    // Mark messages as read when unread count is > 0
    LaunchedEffect(roomId) {
        db.messageDao().getUnreadCountForRoom(roomId).collect { count ->
            if (count > 0) {
                withContext(Dispatchers.IO) {
                    db.messageDao().markMessagesAsRead(roomId)
                }
            }
        }
    }

    // derivedStateOf to locate the first unread message index from the bottom (newest messages first)
    val firstUnreadIndex by remember(lazyPagingItems.itemCount) {
        derivedStateOf {
            var highestIndex = -1
            for (i in 0 until lazyPagingItems.itemCount) {
                val msg = lazyPagingItems.peek(i)
                if (msg != null && msg.isUnread) {
                    highestIndex = i
                }
            }
            highestIndex
        }
    }

    // derivedStateOf to show/hide "Scroll to bottom" button
    val showScrollToBottomButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 3
        }
    }

    val isAtBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex <= 2
        }
    }

    // Auto scroll to bottom
    val lastMessageId = if (lazyPagingItems.itemCount > 0) lazyPagingItems.peek(0)?.realMessageId else null
    val isKeyboardVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
    LaunchedEffect(lastMessageId, isKeyboardVisible) {
        if (lazyPagingItems.itemCount > 0) {
            if (isAtBottom || isKeyboardVisible) {
                listState.animateScrollToItem(0)
            }
        }
    }

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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = if (isTemporaryMode) "Ghost Session" else username,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTemporaryMode) RedPrimary else Black
                                    )
                                    if (!isTemporaryMode && !partnerMood.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = partnerMood,
                                            fontSize = 18.sp
                                        )
                                    }
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
                                                    onClick = { 
                                                        isMenuVisible = false 
                                                        isSearchVisible = true
                                                    }
                                                )
                                                
                                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = Color(0xFFF5F5F5), thickness = 1.dp)

                                                DropdownMenuItem(
                                                    text = { Text("Clear Chat", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333)) },
                                                    leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFF888888), modifier = Modifier.size(22.dp)) },
                                                    onClick = { 
                                                        isMenuVisible = false 
                                                        showDeleteChatDialog = true
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
                                                            Thread {
                                                                db.messageDao().deleteMessagesByText(roomId, "You blocked this friend")
                                                            }.start()
                                                        } else {
                                                            showBlockUserDialog = true
                                                        }
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
                                if (selectedMessageIds.isNotEmpty()) {
                                    selectedMessageIds.clear()
                                } else if (isTemporaryMode) {
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
                    if (selectedMessageIds.isNotEmpty()) {
                        IconButton(
                            onClick = { 
                                showDeleteConfirmDialog = true
                                isDeleteConfirmDialogVisible = true
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Selected",
                                tint = RedPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    if (!isTemporaryMode) {
                        IconButton(
                            onClick = { 
                                webSocketManager?.sendMessage(
                                    roomId = roomId,
                                    receiverId = friendId,
                                    text = "Ghost Message",
                                    isGhost = true
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
                            if (isTemporaryMode) {
                                webSocketManager?.sendMessage(
                                    roomId = roomId,
                                    receiverId = friendId,
                                    text = textState,
                                    isTemporary = true,
                                    ghostMessageId = activeGhostId
                                )
                            } else {
                                webSocketManager?.sendMessage(roomId, friendId, textState)
                            }
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
            if (lazyPagingItems.itemCount == 0 && lazyPagingItems.loadState.refresh !is androidx.paging.LoadState.Loading) {
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
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    reverseLayout = true
                ) {
                    items(
                        count = lazyPagingItems.itemCount,
                        key = lazyPagingItems.itemKey { it.realMessageId },
                        contentType = lazyPagingItems.itemContentType {
                            when {
                                it.isStatus -> "status"
                                it.isGhost -> "ghost"
                                it.isTemporary -> "temporary"
                                else -> "text"
                            }
                        }
                    ) { index ->
                        val message = lazyPagingItems[index]
                        if (message != null) {
                            if (message.isStatus) {
                                StatusSeparator(text = message.text)
                            } else {
                                if (index == firstUnreadIndex && unreadCount > 0 && !isTemporaryMode) {
                                    UnreadSeparator(unreadCount)
                                }
                                
                                val prevMessage = if (index > 0) lazyPagingItems.peek(index - 1) else null
                                val nextMessage = if (index + 1 < lazyPagingItems.itemCount) lazyPagingItems.peek(index + 1) else null
                                
                                val isFirstInGroup = nextMessage == null || nextMessage.isFromMe != message.isFromMe
                                val isLastInGroup = prevMessage == null || prevMessage.isFromMe != message.isFromMe
                                
                                if (isFirstInGroup && index < lazyPagingItems.itemCount - 1 && index != firstUnreadIndex && (nextMessage?.isStatus == false)) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                                
                                ChatBubble(
                                    message = message, 
                                    showAvatar = isLastInGroup && !message.isFromMe && !isTemporaryMode,
                                    avatar = partnerAvatar,
                                    isHighlighted = message.id == highlightedMessageId,
                                    isSelected = selectedMessageIds.contains(message.realMessageId),
                                    onClick = {
                                        if (selectedMessageIds.isNotEmpty()) {
                                            if (selectedMessageIds.contains(message.realMessageId)) {
                                                selectedMessageIds.remove(message.realMessageId)
                                            } else {
                                                selectedMessageIds.add(message.realMessageId)
                                            }
                                        } else if (message.isGhost && !message.isUsed) {
                                            activeGhostId = message.realMessageId
                                            isTemporaryMode = true 
                                        }
                                    },
                                    onLongClick = {
                                        if (selectedMessageIds.contains(message.realMessageId)) {
                                            selectedMessageIds.remove(message.realMessageId)
                                        } else {
                                            selectedMessageIds.add(message.realMessageId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Scroll to Bottom FAB
            AnimatedVisibility(
                visible = showScrollToBottomButton,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = White,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Scroll to bottom",
                        modifier = Modifier.size(24.dp)
                    )
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

                                    Button(
                                        onClick = { 
                                            isDialogVisible = false
                                            showRemoveFriendDialog = false
                                            webSocketManager?.sendRemoveFriend(friendId)
                                            Thread {
                                                db.friendDao().deleteFriendById(friendId)
                                                db.messageDao().deleteMessagesForRoom(roomId)
                                                db.chatRoomDao().deleteChatRoom(roomId)
                                            }.start()
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

        // Delete Messages Confirmation Dialog
        if (showDeleteConfirmDialog) {
            val selectedMessages = remember(selectedMessageIds.toList(), lazyPagingItems.itemCount) {
                val list = mutableListOf<Message>()
                for (i in 0 until lazyPagingItems.itemCount) {
                    val msg = lazyPagingItems.peek(i)
                    if (msg != null && selectedMessageIds.contains(msg.realMessageId)) {
                        list.add(msg)
                    }
                }
                list
            }
            val allSentByMe = remember(selectedMessages) {
                selectedMessages.isNotEmpty() && selectedMessages.all { it.isFromMe }
            }

            androidx.compose.ui.window.Dialog(
                onDismissRequest = { isDeleteConfirmDialogVisible = false },
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
                        visible = isDeleteConfirmDialogVisible,
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
                                    text = "Hapus Pesan?",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Black
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = if (allSentByMe) {
                                        "Apakah Anda ingin menghapus pesan terpilih ini dari riwayat Anda atau untuk semua orang?"
                                    } else {
                                        "Apakah Anda ingin menghapus pesan terpilih ini dari riwayat Anda? Tindakan ini hanya bersifat sepihak."
                                    },
                                    fontSize = 14.sp,
                                    color = GreyText,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 20.sp
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (allSentByMe) {
                                        Button(
                                            onClick = {
                                                isDeleteConfirmDialogVisible = false
                                                showDeleteConfirmDialog = false
                                                val ids = selectedMessageIds.toList()
                                                webSocketManager?.sendDeleteMessages(roomId, friendId, ids)
                                                Thread {
                                                    db.messageDao().deleteMessagesByIds(ids)
                                                    val newestMsg = db.messageDao().getNewestMessageForRoom(roomId)
                                                    val cachedFriend = db.friendDao().getFriendById(friendId)
                                                    val partnerUsername = cachedFriend?.username ?: "Chat Partner"
                                                    val partnerAvatar = cachedFriend?.avatar
                                                    val partnerMood = cachedFriend?.mood

                                                    val chatRoom = ChatRoomEntity(
                                                        roomId = roomId,
                                                        partnerUid = friendId,
                                                        partnerUsername = partnerUsername,
                                                        lastMessage = newestMsg?.text ?: "Tap to open chat room",
                                                        lastTimestamp = newestMsg?.timestamp ?: 0L,
                                                        partnerAvatar = partnerAvatar,
                                                        partnerMood = partnerMood
                                                    )
                                                    db.chatRoomDao().insertChatRoom(chatRoom)
                                                }.start()
                                                selectedMessageIds.clear()
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = RedPrimary,
                                                contentColor = White
                                            ),
                                            contentPadding = PaddingValues(vertical = 12.dp)
                                        ) {
                                            Text("Hapus untuk Semua Orang", fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            isDeleteConfirmDialogVisible = false
                                            showDeleteConfirmDialog = false
                                            val ids = selectedMessageIds.toList()
                                            Thread {
                                                db.messageDao().deleteMessagesByIds(ids)
                                                val newestMsg = db.messageDao().getNewestMessageForRoom(roomId)
                                                val cachedFriend = db.friendDao().getFriendById(friendId)
                                                val partnerUsername = cachedFriend?.username ?: "Chat Partner"
                                                val partnerAvatar = cachedFriend?.avatar
                                                val partnerMood = cachedFriend?.mood

                                                val chatRoom = ChatRoomEntity(
                                                    roomId = roomId,
                                                    partnerUid = friendId,
                                                    partnerUsername = partnerUsername,
                                                    lastMessage = newestMsg?.text ?: "Tap to open chat room",
                                                    lastTimestamp = newestMsg?.timestamp ?: 0L,
                                                    partnerAvatar = partnerAvatar,
                                                    partnerMood = partnerMood
                                                )
                                                db.chatRoomDao().insertChatRoom(chatRoom)
                                            }.start()
                                            selectedMessageIds.clear()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (allSentByMe) Color(0xFFFEEBEE) else RedPrimary,
                                            contentColor = if (allSentByMe) RedPrimary else White
                                        ),
                                        contentPadding = PaddingValues(vertical = 12.dp)
                                    ) {
                                        Text("Hapus untuk Saya", fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { isDeleteConfirmDialogVisible = false },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFF5F5F5),
                                            contentColor = GreyText
                                        ),
                                        contentPadding = PaddingValues(vertical = 12.dp)
                                    ) {
                                        Text("Batal", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        LaunchedEffect(isDeleteConfirmDialogVisible) {
                            if (!isDeleteConfirmDialogVisible) {
                                delay(300)
                                showDeleteConfirmDialog = false
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
                                            isBlockDialogVisible = false
                                            webSocketManager?.sendBlockUser(friendId)
                                            Thread {
                                                db.messageDao().insertMessage(
                                                    MessageEntity(
                                                        messageId = "block_${System.currentTimeMillis()}",
                                                        roomId = roomId,
                                                        senderId = senderId,
                                                        text = "You blocked this friend",
                                                        timestamp = System.currentTimeMillis(),
                                                        isSentByMe = true
                                                    )
                                                )
                                            }.start()
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
                                            webSocketManager?.sendClearChat(roomId, friendId)
                                            Thread {
                                                db.messageDao().deleteMessagesForRoom(roomId)
                                                db.chatRoomDao().deleteChatRoom(roomId)
                                            }.start()
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
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    val sessionManager = SessionManager(context)
                                                    val token = sessionManager.getToken()
                                                    if (token != null) {
                                                        val response = RetrofitClient.apiService.sendFriendRequest(
                                                            token = "Bearer $token",
                                                            request = SendFriendRequestPayload(receiverUsername = username)
                                                        )
                                                        if (response.isSuccessful) {
                                                            Log.d("ChatDetailScreen", "Friend request sent to $username successfully")
                                                        } else {
                                                            Log.e("ChatDetailScreen", "Failed to send friend request: ${response.code()}")
                                                        }
                                                    }
                                                } catch (e: java.lang.Exception) {
                                                    Log.e("ChatDetailScreen", "Error sending friend request: ${e.localizedMessage}")
                                                }
                                            }
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

        // Search Overlay
        AnimatedVisibility(
            visible = isSearchVisible,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.zIndex(10f)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = White
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search Header
                    Surface(
                        modifier = Modifier.fillMaxWidth().statusBarsPadding(),
                        color = White,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { 
                                isSearchVisible = false
                                searchQuery = ""
                                debouncedQuery = ""
                            }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, modifier = Modifier.size(28.dp))
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(24.dp),
                                color = Color(0xFFF7F7F7),
                                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                            ) {
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    textStyle = LocalTextStyle.current.copy(color = Black, fontSize = 15.sp),
                                    cursorBrush = SolidColor(RedPrimary),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        if (searchQuery.isEmpty()) {
                                            Text("Search messages...", color = GreyText, fontSize = 15.sp)
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }
                    }

                    // Search Results
                    if (debouncedQuery.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFEEEEEE), modifier = Modifier.size(80.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Search for messages in this chat", color = GreyText, fontSize = 14.sp)
                            }
                        }
                    } else if (searchResults.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SearchOff, contentDescription = null, tint = Color(0xFFEEEEEE), modifier = Modifier.size(80.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No results found for \"$debouncedQuery\"", color = GreyText, fontSize = 14.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(searchResultsState.value) { entity ->
                                val message = remember(entity) {
                                    Message(
                                        id = entity.messageId.hashCode(),
                                        text = entity.text,
                                        isFromMe = entity.senderId == senderId,
                                        isUnread = false,
                                        isGhost = entity.isGhost,
                                        isUsed = entity.isUsed,
                                        isTemporary = entity.isTemporary,
                                        isStatus = entity.text.startsWith("You blocked") || entity.text.startsWith("You unblocked") || entity.text.startsWith("You are blocked"),
                                        ghostMessageId = entity.ghostMessageId,
                                        realMessageId = entity.messageId,
                                        isPending = entity.isPending
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                val index = withContext(Dispatchers.IO) {
                                                    db.messageDao().getMessageIndexByTimestamp(roomId, entity.timestamp)
                                                }
                                                highlightedMessageId = message.id
                                                isSearchVisible = false
                                                searchQuery = ""
                                                debouncedQuery = ""
                                                listState.animateScrollToItem(index)
                                            }
                                        }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (message.isFromMe) "Me" else username,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RedPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = message.text,
                                        fontSize = 14.sp,
                                        color = Black,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = Color(0xFFF5F5F5))
                                }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    message: Message, 
    showAvatar: Boolean,
    avatar: String?,
    isHighlighted: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val highlightColor by animateColorAsState(
        targetValue = if (isHighlighted) RedPrimary.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(durationMillis = 500)
    )
    val selectionColor = if (isSelected) RedPrimary.copy(alpha = 0.12f) else Color.Transparent

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(selectionColor)
            .background(highlightColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = if (isHighlighted) 8.dp else 4.dp),
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
                            .background(GreyLight),
                        contentAlignment = Alignment.Center
                    ) {
                        val avatarResId = AvatarHelper.getAvatarResourceId(context, avatar)
                        Image(
                            painter = androidx.compose.ui.res.painterResource(id = avatarResId),
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
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

            if (message.isFromMe && message.isPending) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "Pending",
                    tint = GreyText,
                    modifier = Modifier
                        .padding(end = 6.dp, bottom = 4.dp)
                        .size(14.dp)
                )
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
                modifier = Modifier.widthIn(max = 280.dp)
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
                    cursorBrush = SolidColor(RedPrimary),
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

fun getDummyMessages(): List<Message> = emptyList()


private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
