import { WebSocketServer, WebSocket } from 'ws';
import { db } from '../config/firebase.js';

interface ClientMessage {
  type: 'register' | 'message';
  userId?: string;
  roomId?: string;
  senderId?: string;
  receiverId?: string;
  text?: string;
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
          const { roomId, senderId, receiverId, text } = payload;
          const timestamp = Date.now();

          // 1. Save to Firebase Realtime Database
          const messagesRef = db.ref(`messages/${roomId}`);
          const newMessageRef = messagesRef.push();
          const messageId = newMessageRef.key as string;

          const msgData = {
            messageId,
            roomId,
            senderId,
            text,
            timestamp
          };

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

          // 3. Broadcast real-time to sender and receiver
          const responsePayload = JSON.stringify({
            type: 'message',
            messageId,
            roomId,
            senderId,
            text,
            timestamp
          });

          const senderWs = clients.get(senderId);
          if (senderWs && senderWs.readyState === WebSocket.OPEN) {
            senderWs.send(responsePayload);
          }

          const receiverWs = clients.get(receiverId);
          if (receiverWs && receiverWs.readyState === WebSocket.OPEN) {
            receiverWs.send(responsePayload);
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
