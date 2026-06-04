import { WebSocketServer, WebSocket } from 'ws';
import { db } from '../config/firebase.js';

interface ClientMessage {
  type: 'register' | 'message' | 'clear_chat' | 'remove_friend' | 'block_user' | 'use_ghost' | 'delete_message';
  userId?: string;
  roomId?: string;
  senderId?: string;
  receiverId?: string;
  text?: string;
  isGhost?: boolean;
  isTemporary?: boolean;
  ghostMessageId?: string;
  messageId?: string;
}

const clients = new Map<string, WebSocket>();

export function initWebSocketServer(server: any) {
  const wss = new WebSocketServer({ noServer: true });

  server.on('upgrade', (request: any, socket: any, head: any) => {
    wss.handleUpgrade(request, socket, head, (ws) => {
      wss.emit('connection', ws, request);
    });
  });

  wss.on('connection', (ws) => {
    let currentUserId: string | null = null;

    ws.on('message', async (data) => {
      try {
        const payload: ClientMessage = JSON.parse(data.toString());

        if (payload.type === 'register' && payload.userId) {
          currentUserId = payload.userId;
          clients.set(currentUserId, ws);
          console.log(`WebSocket registered user: ${currentUserId}`);
        } else if (payload.type === 'message' && payload.roomId && payload.senderId && payload.receiverId && payload.text) {
          const { roomId, senderId, receiverId, text, isGhost, isTemporary, ghostMessageId } = payload;
          const timestamp = Date.now();
          let messageId: string;

          if (isTemporary) {
            // Temporary (Ghost Session) messages are broadcasted but NOT persisted in Firebase
            messageId = `temp_${timestamp}_${Math.floor(Math.random() * 1000)}`;
            console.log(`Processing temporary message ${messageId} in room ${roomId}`);
          } else {
            // 1. Save to Firebase Realtime Database
            const messagesRef = db.ref(`messages/${roomId}`);
            const newMessageRef = messagesRef.push();
            messageId = newMessageRef.key as string;

            const msgData: any = {
              messageId,
              roomId,
              senderId,
              text,
              timestamp
            };

            if (isGhost !== undefined) msgData.isGhost = isGhost;
            if (ghostMessageId !== undefined) msgData.ghostMessageId = ghostMessageId;

            await newMessageRef.set(msgData);
            console.log(`Saved message ${messageId} to Firebase`);

            // 2. Perform message trimming: Keep only the 3 most recent chats in Firebase!
            const snapshot = await messagesRef.once('value');
            if (snapshot.exists()) {
              const allMsgs: { key: string; timestamp: number }[] = [];
              snapshot.forEach((child) => {
                allMsgs.push({
                  key: child.key as string,
                  timestamp: child.val().timestamp || 0
                });
              });

              if (allMsgs.length > 3) {
                // Sort ascending (oldest first)
                allMsgs.sort((a, b) => a.timestamp - b.timestamp);
                const toDelete = allMsgs.slice(0, allMsgs.length - 3);
                await Promise.all(
                  toDelete.map((msg) => messagesRef.child(msg.key).remove())
                );
                console.log(`Trimmed ${toDelete.length} older messages from Firebase in room ${roomId}`);
              }
            }
          }

          // 3. Broadcast real-time to sender and receiver
          const responsePayload = JSON.stringify({
            type: 'message',
            messageId,
            roomId,
            senderId,
            text,
            timestamp,
            isGhost: isGhost || false,
            isTemporary: isTemporary || false,
            ghostMessageId: ghostMessageId || null
          });

          const senderWs = clients.get(senderId);
          if (senderWs && senderWs.readyState === WebSocket.OPEN) {
            senderWs.send(responsePayload);
          }

          const receiverWs = clients.get(receiverId);
          if (receiverWs && receiverWs.readyState === WebSocket.OPEN) {
            receiverWs.send(responsePayload);
          }
        } else if (payload.type === 'use_ghost' && payload.roomId && payload.senderId && payload.receiverId && payload.messageId) {
          const { roomId, senderId, receiverId, messageId } = payload;
          
          await db.ref(`messages/${roomId}/${messageId}`).update({ isUsed: true });
          console.log(`Marked ghost message ${messageId} as used in Firebase`);

          const broadcastPayload = JSON.stringify({
            type: 'use_ghost',
            roomId,
            messageId
          });

          const senderWs = clients.get(senderId);
          if (senderWs && senderWs.readyState === WebSocket.OPEN) {
            senderWs.send(broadcastPayload);
          }

          const receiverWs = clients.get(receiverId);
          if (receiverWs && receiverWs.readyState === WebSocket.OPEN) {
            receiverWs.send(broadcastPayload);
          }
        } else if (payload.type === 'delete_message' && payload.roomId && payload.senderId && payload.receiverId) {
          const { roomId, senderId, receiverId } = payload;
          const msgIds: string[] = (payload as any).messageIds || (payload.messageId ? [payload.messageId] : []);
          
          if (msgIds.length > 0) {
            await Promise.all(msgIds.map(id => db.ref(`messages/${roomId}/${id}`).remove()));
            console.log(`Deleted messages ${msgIds} in Firebase`);

            const broadcastPayload = JSON.stringify({
              type: 'delete_message',
              roomId,
              messageIds: msgIds
            });

            const senderWs = clients.get(senderId);
            if (senderWs && senderWs.readyState === WebSocket.OPEN) {
              senderWs.send(broadcastPayload);
            }

            const receiverWs = clients.get(receiverId);
            if (receiverWs && receiverWs.readyState === WebSocket.OPEN) {
              receiverWs.send(broadcastPayload);
            }
          }
        } else if (payload.type === 'clear_chat' && payload.roomId && payload.senderId && payload.receiverId) {
          const { roomId, senderId, receiverId } = payload;
          
          await db.ref(`messages/${roomId}`).remove();
          console.log(`Cleared Firebase messages for room ${roomId}`);

          const broadcastPayload = JSON.stringify({
            type: 'clear_chat',
            roomId
          });

          const senderWs = clients.get(senderId);
          if (senderWs && senderWs.readyState === WebSocket.OPEN) {
            senderWs.send(broadcastPayload);
          }

          const receiverWs = clients.get(receiverId);
          if (receiverWs && receiverWs.readyState === WebSocket.OPEN) {
            receiverWs.send(broadcastPayload);
          }
        } else if (payload.type === 'remove_friend' && payload.senderId && payload.receiverId) {
          const { senderId, receiverId } = payload;

          await Promise.all([
            db.ref(`friendships/${senderId}/${receiverId}`).remove(),
            db.ref(`friendships/${receiverId}/${senderId}`).remove()
          ]);
          console.log(`Removed friendship in Firebase between ${senderId} and ${receiverId}`);

          const broadcastPayload = JSON.stringify({
            type: 'remove_friend',
            senderId,
            receiverId
          });

          const senderWs = clients.get(senderId);
          if (senderWs && senderWs.readyState === WebSocket.OPEN) {
            senderWs.send(broadcastPayload);
          }

          const receiverWs = clients.get(receiverId);
          if (receiverWs && receiverWs.readyState === WebSocket.OPEN) {
            receiverWs.send(broadcastPayload);
          }
        } else if (payload.type === 'block_user' && payload.senderId && payload.receiverId) {
          const { senderId, receiverId } = payload;

          await db.ref(`blocks/${senderId}/${receiverId}`).set(true);
          console.log(`Blocked user ${receiverId} by ${senderId} in Firebase`);

          const broadcastPayload = JSON.stringify({
            type: 'block_user',
            senderId,
            receiverId
          });

          const senderWs = clients.get(senderId);
          if (senderWs && senderWs.readyState === WebSocket.OPEN) {
            senderWs.send(broadcastPayload);
          }

          const receiverWs = clients.get(receiverId);
          if (receiverWs && receiverWs.readyState === WebSocket.OPEN) {
            receiverWs.send(broadcastPayload);
          }
        }
      } catch (err) {
        console.error('Error handling WebSocket message:', err);
      }
    });

    ws.on('close', () => {
      if (currentUserId) {
        clients.delete(currentUserId);
        console.log(`WebSocket client disconnected: ${currentUserId}`);
      }
    });

    ws.on('error', (err) => {
      console.error(`WebSocket error for user ${currentUserId}:`, err);
    });
  });

  console.log('✅ WebSocket server initialized successfully alongside Hono HTTP server');
}
