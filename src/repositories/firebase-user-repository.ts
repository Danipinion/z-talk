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
}
