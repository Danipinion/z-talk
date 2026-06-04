export interface FriendRequest {
  senderId: string;
  senderUsername: string;
  status: string;
  createdAt: number;
  senderAvatar?: string;
}

export interface Friend {
  id: string;
  username: string;
  avatar?: string;
}

export interface IFriendRepository {
  sendFriendRequest(senderId: string, senderUsername: string, receiverId: string): Promise<void>;
  getFriendRequests(userId: string): Promise<FriendRequest[]>;
  respondToFriendRequest(userId: string, senderId: string, accept: boolean): Promise<void>;
  addFriendDirectly(userId: string, friendId: string): Promise<void>;
  getFriends(userId: string): Promise<Friend[]>;
  searchUsers(query: string, currentUserId: string): Promise<any[]>;
}
