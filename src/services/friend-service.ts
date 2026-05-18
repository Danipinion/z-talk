import { IFriendRepository, FriendRequest, Friend } from '../repositories/friend-repository.interface.js';
import { IUserRepository } from '../repositories/user-repository.interface.js';

export class FriendService {
  constructor(
    private friendRepository: IFriendRepository,
    private userRepository: IUserRepository
  ) {}

  async sendFriendRequest(senderId: string, senderUsername: string, receiverUsername: string): Promise<void> {
    if (!receiverUsername) throw new Error('Receiver username is required');
    if (senderUsername === receiverUsername) throw new Error('You cannot add yourself');

    const receiver = await this.userRepository.findByUsername(receiverUsername);
    if (!receiver) {
      throw new Error('User not found');
    }

    await this.friendRepository.sendFriendRequest(senderId, senderUsername, receiver.id);
  }

  async getFriendRequests(userId: string): Promise<FriendRequest[]> {
    return this.friendRepository.getFriendRequests(userId);
  }

  async respondToFriendRequest(userId: string, senderId: string, accept: boolean): Promise<void> {
    await this.friendRepository.respondToFriendRequest(userId, senderId, accept);
  }

  async addFriendDirectly(userId: string, friendId: string): Promise<void> {
    if (userId === friendId) throw new Error('You cannot add yourself');
    await this.friendRepository.addFriendDirectly(userId, friendId);
  }

  async getFriends(userId: string): Promise<Friend[]> {
    return this.friendRepository.getFriends(userId);
  }

  async searchUsers(query: string, currentUserId: string): Promise<any[]> {
    if (!query) return [];
    return this.friendRepository.searchUsers(query, currentUserId);
  }
}
