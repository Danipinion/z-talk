import { IUserRepository } from './user-repository.interface.js';
import { User } from '../models/user.js';
import { db } from '../config/firebase.js';
import admin from 'firebase-admin';

export class FirebaseUserRepository implements IUserRepository {
  private usersRef = db.ref('users');

  async findByUsername(username: string): Promise<{ id: string; user: User } | null> {
    const snapshot = await this.usersRef.orderByChild('username').equalTo(username).once('value');
    if (!snapshot.exists()) return null;

    let foundUser: { id: string; user: User } | null = null;
    snapshot.forEach((child) => {
      const data = child.val();
      foundUser = {
        id: child.key as string,
        user: {
          id: child.key as string,
          username: data.username,
          password: data.password,
          createdAt: data.createdAt,
          avatar: data.avatar,
          mood: data.mood,
          fcmToken: data.fcmToken,
        }
      };
    });
    return foundUser;
  }

  async create(username: string, passwordHash: string): Promise<User> {
    const newUserRef = this.usersRef.push();
    const userData = {
      username,
      password: passwordHash,
      createdAt: admin.database.ServerValue.TIMESTAMP
    };
    await newUserRef.set(userData);
    return {
      id: newUserRef.key as string,
      username
    };
  }

  async usernameExists(username: string): Promise<boolean> {
    const snapshot = await this.usersRef.orderByChild('username').equalTo(username).once('value');
    return snapshot.exists();
  }

  async findById(id: string): Promise<User | null> {
    const snapshot = await this.usersRef.child(id).once('value');
    if (!snapshot.exists()) return null;
    const data = snapshot.val();
    return {
      id,
      username: data.username,
      createdAt: data.createdAt,
      avatar: data.avatar,
      mood: data.mood,
      fcmToken: data.fcmToken
    };
  }

  async updateAvatar(userId: string, avatar: string): Promise<void> {
    await this.usersRef.child(userId).update({ avatar });
  }

  async updateMood(userId: string, mood: string | null): Promise<void> {
    await this.usersRef.child(userId).update({ mood: mood || null });
  }

  async updateFcmToken(userId: string, fcmToken: string | null): Promise<void> {
    await this.usersRef.child(userId).update({ fcmToken: fcmToken || null });
  }
}
