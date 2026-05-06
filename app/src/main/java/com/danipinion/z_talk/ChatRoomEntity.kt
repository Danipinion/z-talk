package com.danipinion.z_talk

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_rooms")
data class ChatRoomEntity(
    @PrimaryKey val roomId: String,      // Hasil gabungan UID_A dan UID_B
    val partnerUid: String,              // UID lawan bicara untuk mengambil foto/nama
    val partnerUsername: String,         // Cache nama lawan bicara
    val lastMessage: String,
    val lastTimestamp: Long
)