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
        private const val WS_URL = "ws://127.0.0.1:3000"
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

                        val isSentByMe = senderId == userId

                        val messageEntity = MessageEntity(
                            messageId = messageId,
                            roomId = roomId,
                            senderId = senderId,
                            text = messageText,
                            timestamp = timestamp,
                            isSentByMe = isSentByMe
                        )

                        Thread {
                            // 1. Insert message to Room
                            db.messageDao().insertMessage(messageEntity)

                            // 2. Derive partner info and insert/update ChatRoom
                            val partnerUid = if (isSentByMe) {
                                val parts = roomId.split("_")
                                if (parts.size == 2) {
                                    if (parts[0] == userId) parts[1] else parts[0]
                                } else ""
                            } else senderId

                            val cachedFriend = db.friendDao().getFriendById(partnerUid)
                            val partnerUsername = cachedFriend?.username ?: "Chat Partner"

                            val chatRoom = ChatRoomEntity(
                                roomId = roomId,
                                partnerUid = partnerUid,
                                partnerUsername = partnerUsername,
                                lastMessage = messageText,
                                lastTimestamp = timestamp
                            )
                            db.chatRoomDao().insertChatRoom(chatRoom)
                        }.start()
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

    fun sendMessage(roomId: String, receiverId: String, text: String) {
        val payload = JSONObject().apply {
            put("type", "message")
            put("roomId", roomId)
            put("senderId", userId)
            put("receiverId", receiverId)
            put("text", text)
        }
        webSocket?.send(payload.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "User logout")
        webSocket = null
    }
}
