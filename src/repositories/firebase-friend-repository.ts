import { IFriendRepository, FriendRequest, Friend } from './friend-repository.interface.js';
import { db } from '../config/firebase.js';

export class FirebaseFriendRepository implements IFriendRepository {
  private friendRequestsRef = db.ref('friend_requests');
  private friendshipsRef = db.ref('friendships');
  private usersRef = db.ref('users');

  async sendFriendRequest(senderId: string, senderUsername: string, receiverId: string): Promise<void> {
    await this.friendRequestsRef.child(receiverId).child(senderId).set({
      senderUsername,
      status: 'pending',
      createdAt: Date.now()
    });
  }

  async getFriendRequests(userId: string): Promise<FriendRequest[]> {
    const snapshot = await this.friendRequestsRef.child(userId).once('value');
    if (!snapshot.exists()) return [];

    const requests: FriendRequest[] = [];
    const promises: Promise<void>[] = [];
    snapshot.forEach((child) => {
      const data = child.val();
      const senderId = child.key as string;
      const promise = this.usersRef.child(senderId).once('value').then((userSnapshot) => {
        const userData = userSnapshot.exists() ? userSnapshot.val() : {};
        requests.push({
          senderId,
          senderUsername: data.senderUsername,
          status: data.status,
          createdAt: data.createdAt,
          senderAvatar: userData.avatar,
          senderMood: userData.mood
        });
      });
      promises.push(promise);
    });
    await Promise.all(promises);
    return requests;
  }

  async respondToFriendRequest(userId: string, senderId: string, accept: boolean): Promise<void> {
    if (accept) {
      // Add to both friendship nodes
      await Promise.all([
        this.friendshipsRef.child(userId).child(senderId).set(true),
        this.friendshipsRef.child(senderId).child(userId).set(true)
      ]);
    }
    // Delete the pending friend request
    await this.friendRequestsRef.child(userId).child(senderId).remove();
  }

  async addFriendDirectly(userId: string, friendId: string): Promise<void> {
    await Promise.all([
      this.friendshipsRef.child(userId).child(friendId).set(true),
      this.friendshipsRef.child(friendId).child(userId).set(true)
    ]);
  }

  async getFriends(userId: string): Promise<Friend[]> {
    const snapshot = await this.friendshipsRef.child(userId).once('value');
    if (!snapshot.exists()) return [];

    const friendIds = Object.keys(snapshot.val());
    const friends: Friend[] = [];

    await Promise.all(
      friendIds.map(async (friendId) => {
        const userSnapshot = await this.usersRef.child(friendId).once('value');
        if (userSnapshot.exists()) {
          const userData = userSnapshot.val();
          friends.push({
            id: friendId,
            username: userData.username,
            avatar: userData.avatar,
            mood: userData.mood
          });
        }
      })
    );

    return friends;
  }

  async searchUsers(query: string, currentUserId: string): Promise<any[]> {
    const usersSnapshot = await this.usersRef.once('value');
    if (!usersSnapshot.exists()) return [];

    const results: any[] = [];
    const allUsers = usersSnapshot.val();

    // Get current user's friendships and sent/received friend requests for relationship status mapping
    const [friendshipsSnapshot, outboundRequestsSnapshot, inboundRequestsSnapshot] = await Promise.all([
      this.friendshipsRef.child(currentUserId).once('value'),
      this.friendRequestsRef.once('value'), // All friend requests to check if currentUserId sent one
      this.friendRequestsRef.child(currentUserId).once('value') // Received by currentUserId
    ]);

    const friendsMap = friendshipsSnapshot.exists() ? friendshipsSnapshot.val() : {};
    const receivedRequests = inboundRequestsSnapshot.exists() ? inboundRequestsSnapshot.val() : {};

    // Build sent requests map
    const sentRequests: Record<string, boolean> = {};
    if (outboundRequestsSnapshot.exists()) {
      const allRequests = outboundRequestsSnapshot.val();
      for (const receiverId in allRequests) {
        if (allRequests[receiverId][currentUserId]) {
          sentRequests[receiverId] = true;
        }
      }
    }

    for (const userId in allUsers) {
      if (userId === currentUserId) continue;

      const user = allUsers[userId];
      if (user.username.toLowerCase().includes(query.toLowerCase())) {
        let relation = 'none';
        if (friendsMap[userId]) {
          relation = 'friend';
        } else if (sentRequests[userId]) {
          relation = 'sent';
        } else if (receivedRequests[userId]) {
          relation = 'received';
        }

        results.push({
          id: userId,
          username: user.username,
          avatar: user.avatar,
          mood: user.mood,
          relation
        });
      }
    }

    return results;
  }
}
