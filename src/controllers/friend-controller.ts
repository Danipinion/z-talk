import { Context } from 'hono';
import { FriendService } from '../services/friend-service.js';

export class FriendController {
  constructor(private friendService: FriendService) {}

  sendRequest = async (c: Context) => {
    try {
      const user = c.get('user');
      const { receiverUsername } = await c.req.json();

      await this.friendService.sendFriendRequest(user.id, user.username, receiverUsername);
      return c.json({ message: 'Friend request sent successfully' });
    } catch (error: any) {
      return c.json({ error: error.message || 'Failed to send friend request' }, 400);
    }
  };

  getRequests = async (c: Context) => {
    try {
      const user = c.get('user');
      const requests = await this.friendService.getFriendRequests(user.id);
      return c.json(requests);
    } catch (error: any) {
      return c.json({ error: error.message || 'Failed to get friend requests' }, 400);
    }
  };

  respondRequest = async (c: Context) => {
    try {
      const user = c.get('user');
      const { senderId, accept } = await c.req.json();

      await this.friendService.respondToFriendRequest(user.id, senderId, accept);
      return c.json({ message: accept ? 'Friend request accepted' : 'Friend request declined' });
    } catch (error: any) {
      return c.json({ error: error.message || 'Failed to respond to friend request' }, 400);
    }
  };

  addDirectly = async (c: Context) => {
    try {
      const user = c.get('user');
      const { friendId } = await c.req.json();

      await this.friendService.addFriendDirectly(user.id, friendId);
      return c.json({ message: 'Friend added directly successfully' });
    } catch (error: any) {
      return c.json({ error: error.message || 'Failed to add friend directly' }, 400);
    }
  };

  getFriendsList = async (c: Context) => {
    try {
      const user = c.get('user');
      const friends = await this.friendService.getFriends(user.id);
      return c.json(friends);
    } catch (error: any) {
      return c.json({ error: error.message || 'Failed to get friends' }, 400);
    }
  };

  searchUsersList = async (c: Context) => {
    try {
      const user = c.get('user');
      const query = c.req.query('q') || '';
      const results = await this.friendService.searchUsers(query, user.id);
      return c.json(results);
    } catch (error: any) {
      return c.json({ error: error.message || 'Failed to search users' }, 400);
    }
  };
}
