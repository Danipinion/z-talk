package com.danipinion.z_talk.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.danipinion.z_talk.data.local.dao.UserDao
import com.danipinion.z_talk.data.local.dao.ChatRoomDao
import com.danipinion.z_talk.data.local.dao.MessageDao
import com.danipinion.z_talk.data.local.dao.FriendDao
import com.danipinion.z_talk.data.local.dao.FriendRequestDao
import com.danipinion.z_talk.data.local.entity.UserEntity
import com.danipinion.z_talk.data.local.entity.ChatRoomEntity
import com.danipinion.z_talk.data.local.entity.MessageEntity
import com.danipinion.z_talk.data.local.entity.FriendEntity
import com.danipinion.z_talk.data.local.entity.FriendRequestEntity

@Database(entities = [UserEntity::class, ChatRoomEntity::class, MessageEntity::class, FriendEntity::class, FriendRequestEntity::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatRoomDao(): ChatRoomDao
    abstract fun messageDao(): MessageDao
    abstract fun friendDao(): FriendDao
    abstract fun friendRequestDao(): FriendRequestDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "z_talk_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
