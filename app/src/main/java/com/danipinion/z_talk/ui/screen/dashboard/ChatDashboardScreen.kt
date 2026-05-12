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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danipinion.z_talk.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDashboardScreen(onNavigateToSearch: () -> Unit = {}, onNavigateToScan: () -> Unit = {}) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedBottomNav by remember { mutableIntStateOf(0) } // 0: Chat, 1: Profile
    val chats = remember { getMockChats() }
    
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
                onAddClick = { showBottomSheet = true }
            )
        },
        bottomBar = {
            ChatBottomNavigation(selectedBottomNav) { selectedBottomNav = it }
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
                    ChatList(chats)
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
fun ChatTopBar(onTitleClick: () -> Unit = {}, onAddClick: () -> Unit = {}) {
    TopAppBar(
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
    val tabs = listOf("All", "Unread")
    
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
fun ChatList(chats: List<ChatItemData>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(chats, key = { it.name }) { chat ->
            ChatListItem(chat, Modifier.animateItem())
            HorizontalDivider(
                modifier = Modifier.padding(start = 82.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = GreyDivider
            )
        }
    }
}

@Composable
fun ChatListItem(chat: ChatItemData, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { /* TODO */ }
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
                Text(
                    text = chat.time,
                    fontSize = 13.sp,
                    color = GreyText
                )
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
    val avatarUrl: String = ""
)

data class NavigationItem(
    val title: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector
)

fun getMockChats() = listOf(
    ChatItemData("Devon Robinson", "Let's catch up tomorrow.", "08:00"),
    ChatItemData("Zane Barber", "Can you send the files?", "02:03"),
    ChatItemData("Andre James", "Meeting rescheduled to 11 AM.", "00:05"),
    ChatItemData("Luboš Volkov", "Looking forward to the event!", "02:45"),
    ChatItemData("Gordon Walker", "Don't forget the deadline.", "01:17"),
    ChatItemData("Roger Jameson", "See you at the gym later. And we can connect later.", "05:12"),
    ChatItemData("Kevin Chen", "Are you free this weekend?", "17:33"),
    ChatItemData("Salvatore Roberts", "Happy birthday! Enjoy your day!", "07:12")
)

