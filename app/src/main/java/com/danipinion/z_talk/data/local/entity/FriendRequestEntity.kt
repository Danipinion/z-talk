package com.danipinion.z_talk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friend_requests")
data class FriendRequestEntity(
    @PrimaryKey val senderId: String,
    val senderUsername: String,
    val status: String,
    val createdAt: Long,
    val senderAvatar: String? = null,
    val senderMood: String? = null
)
