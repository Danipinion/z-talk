package com.danipinion.z_talk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.danipinion.z_talk.data.local.entity.ChatRoomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatRoomDao {
    @Query("SELECT * FROM chat_rooms ORDER BY lastTimestamp DESC")
    fun getAllChatRooms(): Flow<List<ChatRoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChatRoom(chatRoom: ChatRoomEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChatRooms(chatRooms: List<ChatRoomEntity>)

    @Query("DELETE FROM chat_rooms WHERE roomId = :roomId")
    fun deleteChatRoom(roomId: String): Int

    @Query("DELETE FROM chat_rooms")
    fun deleteAllChatRooms(): Int
}
