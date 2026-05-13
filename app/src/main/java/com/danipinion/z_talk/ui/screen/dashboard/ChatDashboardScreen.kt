package com.danipinion.z_talk.ui.screen.dashboard

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danipinion.z_talk.ui.screen.profile.ProfileScreen
import com.danipinion.z_talk.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDashboardScreen(
    onNavigateToSearch: () -> Unit = {}, 
    onNavigateToScan: () -> Unit = {},
    onNavigateToChat: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var debouncedSearchQuery by remember { mutableStateOf("") }
    var isSearchBarVisible by remember { mutableStateOf(false) }
    val chats = remember { getMockChats() }
    val focusRequester = remember { FocusRequester() }
    
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
            ChatFilterTabs(selectedTab) { selectedTab = it }
            
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
            
            AnimatedContent(
                targetState = showEmptyState,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.95f))
                        .togetherWith(fadeOut(animationSpec = tween(400)))
                },
                label = "contentTransition"
            ) { targetEmptyState ->
                if (targetEmptyState) {
                    EmptyChatState()
                } else {
                    val filteredChats = when (selectedTab) {
                        1 -> chats.filter { it.isUnread && !it.isRequest }
                        2 -> chats.filter { it.isRequest }
                        else -> chats.filter { !it.isRequest }
                    }.filter { 
                        it.name.contains(debouncedSearchQuery, ignoreCase = true) || 
                        it.lastMessage.contains(debouncedSearchQuery, ignoreCase = true)
                    }
                    ChatList(filteredChats, onNavigateToChat)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    onTitleClick: () -> Unit,
    onAddClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSearchToggle: () -> Unit
) {
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
                    .background(RedPastel)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "D",
                    color = RedPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
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
fun ChatList(chats: List<ChatItemData>, onItemClick: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(chats, key = { it.name }) { chat ->
            ChatListItem(chat, onItemClick, Modifier.animateItem())
            HorizontalDivider(
                modifier = Modifier.padding(start = 82.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = GreyDivider
            )
        }
    }
}

@Composable
fun ChatListItem(chat: ChatItemData, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !chat.isRequest) { onClick(chat.name) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(RedPastel),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chat.name.take(1),
                color = RedPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Black
                )
                
                if (chat.isUnread) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(RedPrimary)
                    )
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
                    onClick = { /* Accept */ },
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
                    onClick = { /* Decline */ },
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
fun EmptyChatState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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

        Box(
            modifier = Modifier
                .size(150.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(RedPastel.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
             Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = RedPrimary,
                modifier = Modifier.size(80.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Welcome to Chat!",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Black
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Feel free to start a new conversation\nby tapping the button below.",
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = GreyText,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = { /* TODO */ },
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
    val name: String,
    val lastMessage: String,
    val time: String,
    val avatarUrl: String = "",
    val isUnread: Boolean = false,
    val isRequest: Boolean = false
)

data class NavigationItem(
    val title: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector
)

fun getMockChats() = listOf(
    ChatItemData("Devon Robinson", "Let's catch up tomorrow.", "08:00", isUnread = true),
    ChatItemData("Zane Barber", "Can you send the files?", "02:03", isUnread = false),
    ChatItemData("Andre James", "Meeting rescheduled to 11 AM.", "00:05", isUnread = true),
    ChatItemData("Luboš Volkov", "Looking forward to the event!", "02:45", isUnread = false),
    ChatItemData("Gordon Walker", "Don't forget the deadline.", "01:17", isUnread = false),
    ChatItemData("Roger Jameson", "See you at the gym later. And we can connect later.", "05:12", isUnread = true),
    ChatItemData("Kevin Chen", "Are you free this weekend?", "17:33", isUnread = false),
    ChatItemData("Salvatore Roberts", "Happy birthday! Enjoy your day!", "07:12", isUnread = false),
    ChatItemData("Sarah Wilson", "Wants to be your friend", "10:15", isRequest = true),
    ChatItemData("Michael Scott", "Wants to be your friend", "11:20", isRequest = true)
)

