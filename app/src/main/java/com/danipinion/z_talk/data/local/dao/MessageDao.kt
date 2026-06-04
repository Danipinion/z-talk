package com.danipinion.z_talk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.danipinion.z_talk.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE roomId = :roomId ORDER BY timestamp ASC")
    fun getMessagesForRoom(roomId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE roomId = :roomId")
    fun deleteMessagesForRoom(roomId: String): Int

    @Query("DELETE FROM messages WHERE roomId = :roomId AND isTemporary = 1")
    fun deleteTemporaryMessages(roomId: String): Int

    @Query("UPDATE messages SET isUsed = 1 WHERE messageId = :messageId")
    fun markGhostAsUsed(messageId: String): Int

    @Query("UPDATE messages SET isUnread = 0 WHERE roomId = :roomId AND isSentByMe = 0")
    fun markMessagesAsRead(roomId: String): Int

    @Query("SELECT COUNT(*) FROM messages WHERE roomId = :roomId AND isSentByMe = 0 AND isUnread = 1")
    fun getUnreadCountForRoom(roomId: String): Flow<Int>

    @Query("SELECT * FROM messages WHERE isUnread = 1 AND isSentByMe = 0")
    fun getAllUnreadMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE isPending = 1 ORDER BY timestamp ASC")
    fun getPendingMessages(): List<MessageEntity>

    @Query("DELETE FROM messages WHERE messageId = (SELECT messageId FROM messages WHERE roomId = :roomId AND text = :text AND isPending = 1 LIMIT 1)")
    fun deleteOnePendingMessage(roomId: String, text: String): Int

    @Query("DELETE FROM messages WHERE messageId IN (:messageIds)")
    fun deleteMessagesByIds(messageIds: List<String>): Int

    @Query("SELECT * FROM messages WHERE roomId = :roomId ORDER BY timestamp DESC LIMIT 1")
    fun getNewestMessageForRoom(roomId: String): MessageEntity?

    @Query("DELETE FROM messages")
    fun deleteAllMessages(): Int
}
