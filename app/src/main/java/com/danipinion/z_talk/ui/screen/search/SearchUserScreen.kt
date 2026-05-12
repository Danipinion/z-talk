package com.danipinion.z_talk.ui.screen.search

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
import androidx.compose.ui.text.style.TextAlign
import com.danipinion.z_talk.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUserScreen(onBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val dummyUsers = remember { getDummyUsers() }
    val selectedUsers = remember { mutableStateListOf<String>() }

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
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                Button(
                    onClick = { /* TODO */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedPrimary,
                        disabledContainerColor = RedPrimary.copy(alpha = 0.5f)
                    ),
                    enabled = selectedUsers.isNotEmpty()
                ) {
                    Text(
                        "Add Friends", 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                }
            }
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Logic: Show only selected users OR exact matches
                val filteredUsers = dummyUsers.filter { user ->
                    val isSelected = selectedUsers.contains(user.name)
                    val isExactMatch = searchQuery.isNotEmpty() && user.name.equals(searchQuery, ignoreCase = true)
                    isSelected || isExactMatch
                }

                items(filteredUsers, key = { it.name }) { user ->
                    UserItem(
                        user = user,
                        isSelected = selectedUsers.contains(user.name),
                        onToggle = {
                            if (selectedUsers.contains(user.name)) {
                                selectedUsers.remove(user.name)
                            } else {
                                selectedUsers.add(user.name)
                            }
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 82.dp, end = 16.dp),
                        thickness = 0.5.dp,
                        color = GreyDivider
                    )
                }
                
                if (filteredUsers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isEmpty()) "Search for friends to add" else "No exact match found", 
                                color = GreyText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun UserItem(user: UserData, isSelected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(RedPastel),
            contentAlignment = Alignment.Center
        ) {
            Text(
                user.name.take(1), 
                fontWeight = FontWeight.Bold, 
                color = RedPrimary,
                fontSize = 18.sp
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = user.name,
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Black
        )
        
        RadioButton(
            selected = isSelected,
            onClick = onToggle,
            colors = RadioButtonDefaults.colors(
                selectedColor = RedPrimary,
                unselectedColor = GreyDivider
            )
        )
    }
}

data class UserData(val name: String)

fun getDummyUsers() = listOf(
    UserData("Andreea Fox"),
    UserData("Rose Nelson"),
    UserData("Ronald Jordan"),
    UserData("Rebecca Andrews"),
    UserData("Victoria Reynolds"),
    UserData("Joan Coleman"),
    UserData("Devon Robinson"),
    UserData("Zane Barber")
)
