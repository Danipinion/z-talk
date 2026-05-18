package com.danipinion.z_talk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.danipinion.z_talk.data.local.entity.FriendEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY username ASC")
    fun getAllFriends(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE id = :id LIMIT 1")
    fun getFriendById(id: String): FriendEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFriends(friends: List<FriendEntity>)

    @Query("DELETE FROM friends")
    fun deleteAllFriends()
}
