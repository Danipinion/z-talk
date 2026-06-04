package com.danipinion.z_talk.data.remote

import android.content.Context
import android.util.Log
import com.danipinion.z_talk.data.local.AppDatabase
import com.danipinion.z_talk.data.local.entity.ChatRoomEntity
import com.danipinion.z_talk.data.local.entity.MessageEntity
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WebSocketManager(private val context: Context, private val userId: String) {
    private var client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private var webSocket: WebSocket? = null
    private val db = AppDatabase.getDatabase(context)

    companion object {
        private const val TAG = "WebSocketManager"
        // private const val WS_URL = "ws://127.0.0.1:3000"
        private const val WS_URL = "wss://ztalkapi.danipinion.my.id"
    }

    fun connect() {
        if (webSocket != null) return

        val request = Request.Builder()
            .url(WS_URL)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected! Registering user $userId")
                val registerPayload = JSONObject().apply {
                    put("type", "register")
                    put("userId", userId)
                }
                webSocket.send(registerPayload.toString())

                // Sync pending messages
                Thread {
                    try {
                        Thread.sleep(500)
                        val pending = db.messageDao().getPendingMessages()
                        Log.d(TAG, "Syncing ${pending.size} pending messages")
                        for (msg in pending) {
                            val partnerId = if (userId == msg.senderId) {
                                val parts = msg.roomId.split("_")
                                if (parts.size == 2) {
                                    if (parts[0] == userId) parts[1] else parts[0]
                                } else ""
                            } else msg.senderId

                            if (partnerId.isNotEmpty()) {
                                sendMessage(
                                    roomId = msg.roomId,
                                    receiverId = partnerId,
                                    text = msg.text,
                                    isGhost = msg.isGhost,
                                    isTemporary = msg.isTemporary,
                                    ghostMessageId = msg.ghostMessageId
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing pending messages: ${e.localizedMessage}")
                    }
                }.start()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WebSocket message received: $text")
                try {
                    val json = JSONObject(text)
                    if (json.getString("type") == "message") {
                        val messageId = json.getString("messageId")
                        val roomId = json.getString("roomId")
                        val senderId = json.getString("senderId")
                        val messageText = json.getString("text")
                        val timestamp = json.getLong("timestamp")
                        val isGhost = json.optBoolean("isGhost", false)
                        val isUsed = json.optBoolean("isUsed", false)
                        val isTemporary = json.optBoolean("isTemporary", false)
                        val ghostMessageId = json.optString("ghostMessageId").takeIf { it.isNotEmpty() }

                        val isSentByMe = senderId == userId

                        val messageEntity = MessageEntity(
                            messageId = messageId,
                            roomId = roomId,
                            senderId = senderId,
                            text = messageText,
                            timestamp = timestamp,
                            isSentByMe = isSentByMe,
                            isGhost = isGhost,
                            isUsed = isUsed,
                            isTemporary = isTemporary,
                            ghostMessageId = if (ghostMessageId == "null" || ghostMessageId.isNullOrEmpty()) null else ghostMessageId,
                            isUnread = !isSentByMe
                        )

                        Thread {
                            if (isSentByMe) {
                                db.messageDao().deleteOnePendingMessage(roomId, messageText)
                            }
                            // 1. Insert message to Room
                            db.messageDao().insertMessage(messageEntity)

                            // 2. Derive partner info and insert/update ChatRoom (only if not temporary)
                            if (!isTemporary) {
                                val partnerUid = if (isSentByMe) {
                                    val parts = roomId.split("_")
                                    if (parts.size == 2) {
                                        if (parts[0] == userId) parts[1] else parts[0]
                                    } else ""
                                } else senderId

                                val cachedFriend = db.friendDao().getFriendById(partnerUid)
                                val partnerUsername = cachedFriend?.username ?: "Chat Partner"
                                val partnerAvatar = cachedFriend?.avatar
                                val partnerMood = cachedFriend?.mood

                                val chatRoom = ChatRoomEntity(
                                    roomId = roomId,
                                    partnerUid = partnerUid,
                                    partnerUsername = partnerUsername,
                                    lastMessage = messageText,
                                    lastTimestamp = timestamp,
                                    partnerAvatar = partnerAvatar,
                                    partnerMood = partnerMood
                                )
                                db.chatRoomDao().insertChatRoom(chatRoom)
                            }
                        }.start()
                    } else if (json.getString("type") == "use_ghost") {
                        val messageId = json.getString("messageId")
                        Thread {
                            db.messageDao().markGhostAsUsed(messageId)
                        }.start()
                    } else if (json.getString("type") == "remove_friend") {
                        val senderId = json.getString("senderId")
                        val receiverId = json.getString("receiverId")
                        val partnerId = if (senderId == userId) receiverId else senderId
                        Thread {
                            db.friendDao().deleteFriendById(partnerId)
                        }.start()
                    } else if (json.getString("type") == "block_user") {
                        val senderId = json.getString("senderId")
                        val receiverId = json.getString("receiverId")
                        if (receiverId == userId) {
                            val roomId = if (userId < senderId) "${userId}_${senderId}" else "${senderId}_${userId}"
                            Thread {
                                db.messageDao().insertMessage(
                                    MessageEntity(
                                        messageId = "blocked_by_${System.currentTimeMillis()}",
                                        roomId = roomId,
                                        senderId = senderId,
                                        text = "You are blocked by this user",
                                        timestamp = System.currentTimeMillis(),
                                        isSentByMe = false
                                    )
                                )
                            }.start()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse WebSocket message: ${e.localizedMessage}")
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason ($code)")
                this@WebSocketManager.webSocket = null
                // Try reconnection
                Thread {
                    Thread.sleep(3000)
                    connect()
                }.start()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.localizedMessage}")
                this@WebSocketManager.webSocket = null
                // Try reconnection
                Thread {
                    Thread.sleep(5000)
                    connect()
                }.start()
            }
        })
    }

    fun sendMessage(
        roomId: String,
        receiverId: String,
        text: String,
        isGhost: Boolean = false,
        isTemporary: Boolean = false,
        ghostMessageId: String? = null
    ) {
        val payload = JSONObject().apply {
            put("type", "message")
            put("roomId", roomId)
            put("senderId", userId)
            put("receiverId", receiverId)
            put("text", text)
            put("isGhost", isGhost)
            put("isTemporary", isTemporary)
            if (ghostMessageId != null) {
                put("ghostMessageId", ghostMessageId)
            }
        }
        val sent = if (webSocket != null) {
            webSocket?.send(payload.toString()) ?: false
        } else {
            false
        }

        if (!sent && !isTemporary) {
            val timestamp = System.currentTimeMillis()
            val pendingMsg = MessageEntity(
                messageId = "pending_" + timestamp + "_" + (1000..9999).random(),
                roomId = roomId,
                senderId = userId,
                text = text,
                timestamp = timestamp,
                isSentByMe = true,
                isGhost = isGhost,
                isUsed = false,
                isTemporary = false,
                ghostMessageId = ghostMessageId,
                isUnread = false,
                isPending = true
            )
            Thread {
                db.messageDao().insertMessage(pendingMsg)

                val cachedFriend = db.friendDao().getFriendById(receiverId)
                val partnerUsername = cachedFriend?.username ?: "Chat Partner"
                val partnerAvatar = cachedFriend?.avatar
                val partnerMood = cachedFriend?.mood

                val chatRoom = ChatRoomEntity(
                    roomId = roomId,
                    partnerUid = receiverId,
                    partnerUsername = partnerUsername,
                    lastMessage = text,
                    lastTimestamp = timestamp,
                    partnerAvatar = partnerAvatar,
                    partnerMood = partnerMood
                )
                db.chatRoomDao().insertChatRoom(chatRoom)
            }.start()
        }
    }

    fun sendUseGhost(roomId: String, receiverId: String, messageId: String) {
        val payload = JSONObject().apply {
            put("type", "use_ghost")
            put("roomId", roomId)
            put("senderId", userId)
            put("receiverId", receiverId)
            put("messageId", messageId)
        }
        webSocket?.send(payload.toString())
    }

    fun sendClearChat(roomId: String, receiverId: String) {
        val payload = JSONObject().apply {
            put("type", "clear_chat")
            put("roomId", roomId)
            put("senderId", userId)
            put("receiverId", receiverId)
        }
        webSocket?.send(payload.toString())
    }

    fun sendRemoveFriend(receiverId: String) {
        val payload = JSONObject().apply {
            put("type", "remove_friend")
            put("senderId", userId)
            put("receiverId", receiverId)
        }
        webSocket?.send(payload.toString())
    }

    fun sendBlockUser(receiverId: String) {
        val payload = JSONObject().apply {
            put("type", "block_user")
            put("senderId", userId)
            put("receiverId", receiverId)
        }
        webSocket?.send(payload.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "User logout")
        webSocket = null
    }
}
