package com.danipinion.z_talk.ui.screen.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.danipinion.z_talk.ui.utils.AvatarHelper
import com.danipinion.z_talk.data.local.SessionManager
import com.danipinion.z_talk.ui.theme.*
import com.danipinion.z_talk.ui.screen.friend.FriendViewModel
import com.danipinion.z_talk.ui.screen.auth.AuthState
import com.danipinion.z_talk.ui.component.PremiumTopToast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.danipinion.z_talk.data.local.AppDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDashboardScreen(
    viewModel: FriendViewModel? = null,
    token: String = "",
    userId: String = "",
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onNavigateToSearch: () -> Unit = {}, 
    onNavigateToScan: () -> Unit = {},
    onNavigateToChat: (String, String) -> Unit = { _, _ -> },
    onNavigateToProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val chatRooms by db.chatRoomDao().getAllChatRooms().collectAsState(initial = emptyList())
    val unreadMessages by db.messageDao().getAllUnreadMessages().collectAsState(initial = emptyList())

    // Handle system back button to return to "All" tab first
    BackHandler(enabled = selectedTab != 0) {
        onTabSelected(0)
    }

    var searchQuery by remember { mutableStateOf("") }
    var debouncedSearchQuery by remember { mutableStateOf("") }
    var isSearchBarVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val friendsStateFlow = remember(viewModel) { viewModel?.friendsState ?: kotlinx.coroutines.flow.MutableStateFlow(AuthState.Idle) }
    val requestsStateFlow = remember(viewModel) { viewModel?.requestsState ?: kotlinx.coroutines.flow.MutableStateFlow(AuthState.Idle) }
    val actionStateFlow = remember(viewModel) { viewModel?.actionState ?: kotlinx.coroutines.flow.MutableStateFlow(AuthState.Idle) }

    val friendsState by friendsStateFlow.collectAsState()
    val requestsState by requestsStateFlow.collectAsState()
    val actionState by actionStateFlow.collectAsState()

    var toastMessage by remember { mutableStateOf("") }
    var isToastSuccess by remember { mutableStateOf(true) }
    var isToastVisible by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTab) {
        if (token.isNotEmpty() && viewModel != null) {
            viewModel.getFriends(token)
            viewModel.getFriendRequests(token)
        }
    }

    LaunchedEffect(actionState) {
        if (actionState is AuthState.Success) {
            toastMessage = (actionState as AuthState.Success<String>).data
            isToastSuccess = true
            isToastVisible = true
            viewModel?.resetActionState()
            if (token.isNotEmpty() && viewModel != null) {
                viewModel.getFriends(token)
                viewModel.getFriendRequests(token)
            }
        } else if (actionState is AuthState.Error) {
            toastMessage = (actionState as AuthState.Error).message
            isToastSuccess = false
            isToastVisible = true
            viewModel?.resetActionState()
        }
    }

    // Debounce search query
    LaunchedEffect(searchQuery) {
        if (searchQuery.isEmpty()) {
            debouncedSearchQuery = ""
        } else {
            delay(500)
            debouncedSearchQuery = searchQuery
        }
    }

    // Auto-focus when search bar is visible
    LaunchedEffect(isSearchBarVisible) {
        if (isSearchBarVisible) {
            focusRequester.requestFocus()
        } else {
            // Smoothly clear search query after the animation completes
            delay(300)
            searchQuery = ""
        }
    }
    
    // Toggle for empty state demo
    var showEmptyState by remember { mutableStateOf(false) }
    
    // Bottom Sheet state
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                ChatTopBar(
                    onTitleClick = { showEmptyState = !showEmptyState },
                    onAddClick = { showBottomSheet = true },
                    onProfileClick = onNavigateToProfile,
                    onSearchToggle = { 
                        isSearchBarVisible = !isSearchBarVisible 
                    }
                )
            },
            containerColor = White
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(White)
        ) {
            ChatFilterTabs(selectedTab) { onTabSelected(it) }
            
            // Simple Search Bar with Toggle
            AnimatedVisibility(
                visible = isSearchBarVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(52.dp)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Search...", fontSize = 14.sp, color = GreyText) },
                    leadingIcon = { 
                        Icon(
                            imageVector = Icons.Default.Search, 
                            contentDescription = null, 
                            tint = GreyText,
                            modifier = Modifier.size(20.dp)
                        ) 
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = GreyText, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = RedPrimary,
                        focusedTextColor = Black,
                        unfocusedTextColor = Black
                    ),
                    singleLine = true
                )
            }
            
            val friends = (friendsState as? AuthState.Success)?.data ?: emptyList()
            val requests = (requestsState as? AuthState.Success)?.data ?: emptyList()

            // Stable sort order — only based on chatRooms timestamps (not unread count)
            // This ensures reading a message doesn't reorder the list
            val sortedFriendIds = remember(friends, chatRooms) {
                friends.sortedByDescending { friend ->
                    val rId = if (userId < friend.id) "${userId}_${friend.id}" else "${friend.id}_${userId}"
                    chatRooms.find { it.roomId == rId }?.lastTimestamp ?: 0L
                }.map { it.id }
            }

            val sortedFriends = remember(sortedFriendIds, friends) {
                val friendMap = friends.associateBy { it.id }
                sortedFriendIds.mapNotNull { friendMap[it] }
            }

            val mappedChats = sortedFriends.map { friend ->
                val rId = if (userId < friend.id) "${userId}_${friend.id}" else "${friend.id}_${userId}"
                val room = chatRooms.find { it.roomId == rId }
                val roomUnreadCount = unreadMessages.count { it.roomId == rId }
                ChatItemData(
                    id = friend.id,
                    name = friend.username,
                    lastMessage = room?.lastMessage ?: "Tap to open chat room",
                    time = if (room != null) {
                        try {
                            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                            sdf.format(java.util.Date(room.lastTimestamp))
                        } catch (e: Exception) {
                            ""
                        }
                    } else "",
                    avatarUrl = friend.avatar ?: "",
                    isUnread = roomUnreadCount > 0,
                    unreadCount = roomUnreadCount,
                    isRequest = false,
                    mood = friend.mood
                )
            }

            // No re-sort here — order is already stable from sortedFriends
            val sortedMappedChats = mappedChats

            val currentChats = when (selectedTab) {
                0 -> sortedMappedChats
                1 -> sortedMappedChats.filter { it.isUnread }
                2 -> requests.map { request ->
                    ChatItemData(
                        id = request.senderId,
                        name = request.senderUsername,
                        lastMessage = "Sent you a friend request",
                        time = "Pending",
                        avatarUrl = request.senderAvatar ?: "",
                        isUnread = false,
                        unreadCount = 0,
                        isRequest = true,
                        mood = request.senderMood
                    )
                }
                else -> emptyList()
            }

            val filteredChats = currentChats.filter { 
                it.name.contains(debouncedSearchQuery, ignoreCase = true) || 
                it.lastMessage.contains(debouncedSearchQuery, ignoreCase = true)
            }

            val isLoading = when (selectedTab) {
                0 -> friendsState is AuthState.Loading
                2 -> requestsState is AuthState.Loading
                else -> false
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = RedPrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else {
                val showEmpty = filteredChats.isEmpty() || showEmptyState

                AnimatedContent(
                    targetState = showEmpty,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.95f))
                            .togetherWith(fadeOut(animationSpec = tween(400)))
                    },
                    label = "contentTransition"
                ) { targetEmptyState ->
                    if (targetEmptyState) {
                        if (debouncedSearchQuery.isNotEmpty()) {
                            EmptySearchState(debouncedSearchQuery)
                        } else {
                            EmptyChatState(selectedTab = selectedTab, onActionClick = { showBottomSheet = true })
                        }
                    } else {
                        ChatList(
                            chats = filteredChats, 
                            selectedTab = selectedTab,
                            onItemClick = onNavigateToChat,
                            onAccept = { chat ->
                                viewModel?.respondToFriendRequest(token, chat.id, true)
                            },
                            onDecline = { chat ->
                                viewModel?.respondToFriendRequest(token, chat.id, false)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = GreyDivider) }
        ) {
            ChatOptionsContent(
                onSearchClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showBottomSheet = false
                            onNavigateToSearch()
                        }
                    }
                },
                onScanClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showBottomSheet = false
                            onNavigateToScan()
                        }
                    }
                },
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showBottomSheet = false
                        }
                    }
                }
            )
        }
    }

    PremiumTopToast(
        message = toastMessage,
        isSuccess = isToastSuccess,
        isVisible = isToastVisible,
        onDismiss = { isToastVisible = false }
    )
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    onTitleClick: () -> Unit,
    onAddClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSearchToggle: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val avatar = sessionManager.getAvatar()

    CenterAlignedTopAppBar(
        navigationIcon = {
            IconButton(onClick = onSearchToggle) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Toggle Search",
                    tint = RedPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Chat",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Black,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTitleClick() }
                    .padding(horizontal = 4.dp)
            )
        },
        actions = {
            IconButton(
                onClick = onAddClick,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = "New Chat",
                    tint = RedPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Profile Avatar Entry
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GreyLight)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                val avatarResId = AvatarHelper.getAvatarResourceId(context, avatar)
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = avatarResId),
                    contentDescription = "My Profile Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = White
        ),
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
fun ChatOptionsContent(onSearchClick: () -> Unit, onScanClick: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, top = 8.dp)
            .padding(horizontal = 24.dp)
    ) {
        OptionItem(
            icon = Icons.AutoMirrored.Outlined.Chat,
            title = "Search for new chat",
            onClick = onSearchClick
        )
        
        HorizontalDivider(color = GreyDivider, thickness = 0.5.dp)
        
        OptionItem(
            icon = Icons.Outlined.QrCodeScanner,
            title = "Scan for new chat",
            onClick = onScanClick
        )

        
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Cancel",
                color = RedPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun OptionItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Black,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = Black
        )
    }
}



@Composable
fun ChatFilterTabs(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("All", "Unread", "Request")
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(GreyLight, RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        val tabWidth = maxWidth / tabs.size
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedTab,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
            label = "tabIndicator"
        )

        // Sliding background indicator
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .height(40.dp) // Height of tabs
                .clip(RoundedCornerShape(8.dp))
                .background(White)
                .border(width = 0.5.dp, color = GreyDivider, shape = RoundedCornerShape(8.dp))
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Black else GreyText,
                    label = "textColor"
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ChatList(
    chats: List<ChatItemData>,
    selectedTab: Int,
    onItemClick: (String, String) -> Unit,
    onAccept: (ChatItemData) -> Unit = {},
    onDecline: (ChatItemData) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(chats, key = { it.id + it.name }) { chat ->
            ChatListItem(
                chat = chat,
                selectedTab = selectedTab,
                onClick = onItemClick,
                onAccept = { onAccept(chat) },
                onDecline = { onDecline(chat) },
                modifier = Modifier
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 82.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = GreyDivider
            )
        }
    }
}

@Composable
fun ChatListItem(
    chat: ChatItemData,
    selectedTab: Int,
    onClick: (String, String) -> Unit,
    onAccept: () -> Unit = {},
    onDecline: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !chat.isRequest) { onClick(chat.name, chat.id) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(GreyLight),
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current
            val avatarResId = AvatarHelper.getAvatarResourceId(context, chat.avatarUrl)
            Image(
                painter = androidx.compose.ui.res.painterResource(id = avatarResId),
                contentDescription = "Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chat.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Black
                    )
                    if (!chat.mood.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = chat.mood,
                            fontSize = 16.sp
                        )
                    }
                }
                
                if (chat.isUnread) {
                    val count = chat.unreadCount
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                                .clip(CircleShape)
                                .background(RedPrimary)
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = count.toString(),
                                color = White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(RedPrimary)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = chat.lastMessage,
                fontSize = 14.sp,
                color = GreyText,
                maxLines = 1,
                lineHeight = 20.sp
            )
        }

        if (chat.isRequest) {
            Row(
                modifier = Modifier.padding(start = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    onClick = onAccept,
                    modifier = Modifier
                        .size(36.dp)
                        .background(RedPrimary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check, 
                        contentDescription = "Accept", 
                        tint = White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFEEEEEE), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close, 
                        contentDescription = "Decline", 
                        tint = Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyChatState(selectedTab: Int, onActionClick: () -> Unit = {}) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val icon = when (selectedTab) {
        1 -> Icons.Default.CheckCircle
        2 -> Icons.Default.GroupAdd
        else -> Icons.Default.ChatBubbleOutline
    }

    val title = when (selectedTab) {
        1 -> "All Caught Up! ✨"
        2 -> "No Pending Requests"
        else -> "No Conversations Yet"
    }

    val subtitle = when (selectedTab) {
        1 -> "No unread messages right now.\nYou're completely caught up!"
        2 -> "Incoming friend requests will\nappear here once they arrive."
        else -> "Stay connected! Tap below to start\na new chat with your besties."
    }

    val showButton = selectedTab == 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    if (selectedTab == 1) Color(0xFFE8F5E9)
                    else RedPastel.copy(alpha = 0.5f)
                ),
            contentAlignment = Alignment.Center
        ) {
             Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selectedTab == 1) Color(0xFF4CAF50) else RedPrimary,
                modifier = Modifier.size(72.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Black,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = subtitle,
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
            color = GreyText,
            lineHeight = 22.sp
        )
        
        if (showButton) {
            Spacer(modifier = Modifier.height(40.dp))
            
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RedPastel,
                    contentColor = RedPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Start New Chat", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ChatBottomNavigation(selectedIndex: Int, onItemSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = White,
        tonalElevation = 0.dp,
        modifier = Modifier.height(64.dp)
    ) {
        val items = listOf(
            NavigationItem("Chat", Icons.AutoMirrored.Outlined.Chat, Icons.AutoMirrored.Filled.Chat),
            NavigationItem("Profile", Icons.Outlined.Person, Icons.Default.Person)
        )
        
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = { onItemSelected(index) },
                icon = {
                    Icon(
                        imageVector = if (selectedIndex == index) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = { Text(item.title, fontSize = 10.sp, fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = RedPrimary,
                    selectedTextColor = RedPrimary,
                    unselectedIconColor = GreyText,
                    unselectedTextColor = GreyText,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}


data class ChatItemData(
    val id: String = "",
    val name: String,
    val lastMessage: String,
    val time: String,
    val avatarUrl: String = "",
    val isUnread: Boolean = false,
    val unreadCount: Int = 0,
    val isRequest: Boolean = false,
    val mood: String? = null
)

data class NavigationItem(
    val title: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector
)

@Composable
fun EmptySearchState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(100.dp)) // Push it up from the center

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(GreyLight.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "🔎",
                    fontSize = 52.sp,
                    modifier = Modifier.graphicsLayer(alpha = 0.4f)
                )
                Text(
                    text = "❌",
                    fontSize = 18.sp,
                    modifier = Modifier
                        .offset(x = 14.dp, y = 14.dp)
                        .graphicsLayer(alpha = 0.5f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp)) // Reduced spacing
        
        Text(
            text = "No matches for \"$query\"",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Black,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Double-check the spelling or try searching for someone else in your list.",
            fontSize = 14.sp,
            color = GreyText,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}


fun getMockChats(): List<ChatItemData> = emptyList()


