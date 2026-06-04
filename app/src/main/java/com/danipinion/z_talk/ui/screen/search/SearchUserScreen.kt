package com.danipinion.z_talk.ui.screen.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import com.danipinion.z_talk.ui.component.PremiumTopToast
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.danipinion.z_talk.ui.utils.AvatarHelper
import com.danipinion.z_talk.ui.theme.*
import com.danipinion.z_talk.ui.screen.friend.FriendViewModel
import com.danipinion.z_talk.ui.screen.auth.AuthState
import com.danipinion.z_talk.data.remote.SearchUserResponse
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUserScreen(
    viewModel: FriendViewModel,
    token: String,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    var searchQuery by remember { mutableStateOf("") }
    val searchState by viewModel.searchState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    // Trigger search on query change with delay (debouncing)
    LaunchedEffect(searchQuery) {
        delay(300) // Debounce typing
        viewModel.searchUsers(token, searchQuery)
    }

    var toastMessage by remember { mutableStateOf("") }
    var isToastSuccess by remember { mutableStateOf(true) }
    var isToastVisible by remember { mutableStateOf(false) }

    LaunchedEffect(actionState) {
        if (actionState is AuthState.Success) {
            toastMessage = (actionState as AuthState.Success<String>).data
            isToastSuccess = true
            isToastVisible = true
            viewModel.resetActionState()
            // Refresh search results
            viewModel.searchUsers(token, searchQuery)
        } else if (actionState is AuthState.Error) {
            toastMessage = (actionState as AuthState.Error).message
            isToastSuccess = false
            isToastVisible = true
            viewModel.resetActionState()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Add Friends", 
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "Back",
                                tint = Black
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
                )
            },
            containerColor = White
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search username...", color = GreyText) },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Search, 
                        contentDescription = null,
                        tint = GreyText
                    ) 
                },
                textStyle = TextStyle(color = Black, fontSize = 16.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = GreyLight,
                    unfocusedContainerColor = GreyLight,
                    disabledContainerColor = GreyLight,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = RedPrimary,
                    focusedTextColor = Black,
                    unfocusedTextColor = Black
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = searchState) {
                    is AuthState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = RedPrimary
                        )
                    }
                    is AuthState.Success -> {
                        val users = state.data
                        if (users.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No exact match found", 
                                    color = GreyText,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(users, key = { it.id }) { user ->
                                    UserSearchItem(
                                        user = user,
                                        onAddClick = {
                                            viewModel.sendFriendRequest(token, user.username)
                                        },
                                        onAcceptClick = {
                                            viewModel.respondToFriendRequest(token, user.id, true)
                                        }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 82.dp, end = 16.dp),
                                        thickness = 0.5.dp,
                                        color = GreyDivider
                                    )
                                }
                            }
                        }
                    }
                    is AuthState.Error -> {
                        Text(
                            text = state.message,
                            color = RedPrimary,
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Search for friends to start talking!", 
                                color = GreyText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
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

@Composable
fun UserSearchItem(
    user: SearchUserResponse,
    onAddClick: () -> Unit,
    onAcceptClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(GreyLight),
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current
            val avatarResId = AvatarHelper.getAvatarResourceId(context, user.avatar)
            Image(
                painter = androidx.compose.ui.res.painterResource(id = avatarResId),
                contentDescription = "Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = user.username,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Black
            )
            if (!user.mood.isNullOrEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = user.mood,
                    fontSize = 16.sp
                )
            }
        }

        when (user.relation) {
            "none" -> {
                Button(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Add Friend",
                        modifier = Modifier.size(16.dp),
                        tint = White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            "sent" -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GreyLight,
                    border = BorderStroke(1.dp, GreyDivider)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = "Pending Request",
                            modifier = Modifier.size(14.dp),
                            tint = GreyText
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pending", color = GreyText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            "received" -> {
                Button(
                    onClick = onAcceptClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Accept Request",
                        modifier = Modifier.size(14.dp),
                        tint = White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Accept", color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            "friend" -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F5E9),
                    border = BorderStroke(1.dp, Color(0xFFC8E6C9))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Friend",
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Friend", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
