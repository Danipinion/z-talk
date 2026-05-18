package com.danipinion.z_talk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.danipinion.z_talk.data.local.entity.FriendRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendRequestDao {
    @Query("SELECT * FROM friend_requests ORDER BY createdAt DESC")
    fun getAllFriendRequests(): Flow<List<FriendRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFriendRequests(requests: List<FriendRequestEntity>)

    @Query("DELETE FROM friend_requests")
    fun deleteAllFriendRequests()
}
