import { IUserRepository } from '../repositories/user-repository.interface.js';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { ENV } from '../config/env.js';
import { User, AuthResponse } from '../models/user.js';

export class AuthService {
  constructor(private userRepository: IUserRepository) {}

  async register(username: string, passwordPlain: string): Promise<AuthResponse> {
    if (!username || !passwordPlain) {
      throw new Error('Username and password are required');
    }

    const exists = await this.userRepository.usernameExists(username);
    if (exists) {
      throw new Error('Username already exists');
    }

    const hashedPassword = await bcrypt.hash(passwordPlain, 10);
    const user = await this.userRepository.create(username, hashedPassword);

    const token = jwt.sign({ id: user.id, username: user.username }, ENV.JWT_SECRET, { expiresIn: '7d' });

    return {
      message: 'User registered successfully',
      token,
      user
    };
  }

  async login(username: string, passwordPlain: string): Promise<AuthResponse> {
    if (!username || !passwordPlain) {
      throw new Error('Username and password are required');
    }

    const record = await this.userRepository.findByUsername(username);
    if (!record) {
      throw new Error('Invalid username or password');
    }

    const isMatch = await bcrypt.compare(passwordPlain, record.user.password || '');
    if (!isMatch) {
      throw new Error('Invalid username or password');
    }

    const token = jwt.sign({ id: record.id, username: record.user.username }, ENV.JWT_SECRET, { expiresIn: '7d' });

    return {
      message: 'Login successful',
      token,
      user: {
        id: record.id,
        username: record.user.username,
        avatar: record.user.avatar,
        mood: record.user.mood,
        background: record.user.background
      }
    };
  }

  async checkUsername(username: string): Promise<boolean> {
    if (!username) {
      throw new Error('Username is required');
    }
    const exists = await this.userRepository.usernameExists(username);
    return !exists; // returns true if username is available (does not exist)
  }

  async updateAvatar(userId: string, avatar: string): Promise<void> {
    if (!avatar) {
      throw new Error('Avatar is required');
    }
    await this.userRepository.updateAvatar(userId, avatar);
  }

  async updateMood(userId: string, mood: string | null): Promise<void> {
    await this.userRepository.updateMood(userId, mood);
  }

  async updateFcmToken(userId: string, fcmToken: string | null): Promise<void> {
    await this.userRepository.updateFcmToken(userId, fcmToken);
  }

  async updateBackground(userId: string, background: string | null): Promise<void> {
    await this.userRepository.updateBackground(userId, background);
  }

  async getProfile(userId: string): Promise<User | null> {
    return this.userRepository.findById(userId);
  }
}
