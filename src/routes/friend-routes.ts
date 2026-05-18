import { Hono } from 'hono';
import { authMiddleware } from '../config/auth-middleware.js';
import { FriendController } from '../controllers/friend-controller.js';
import { FriendService } from '../services/friend-service.js';
import { FirebaseFriendRepository } from '../repositories/firebase-friend-repository.js';
import { FirebaseUserRepository } from '../repositories/firebase-user-repository.js';

const friendRouter = new Hono();

// Assemble SOLID dependencies
const friendRepository = new FirebaseFriendRepository();
const userRepository = new FirebaseUserRepository();
const friendService = new FriendService(friendRepository, userRepository);
const friendController = new FriendController(friendService);

// Apply authMiddleware globally to all friendship routes
friendRouter.use('*', authMiddleware);

friendRouter.post('/request', friendController.sendRequest);
friendRouter.get('/requests', friendController.getRequests);
friendRouter.post('/respond', friendController.respondRequest);
friendRouter.post('/add-direct', friendController.addDirectly);
friendRouter.get('/list', friendController.getFriendsList);
friendRouter.get('/search', friendController.searchUsersList);

export { friendRouter };
