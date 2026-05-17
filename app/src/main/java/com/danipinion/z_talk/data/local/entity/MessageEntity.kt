package com.danipinion.z_talk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,   // ID unik dari Firebase (push key)
    val roomId: String,                  // Relasi ke ChatRoomEntity
    val senderId: String,
    val text: String,
    val timestamp: Long,
    val isSentByMe: Boolean              // Flag lokal untuk mempermudah UI di Jetpack Compose
)
