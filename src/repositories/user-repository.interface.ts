import { User } from "../models/user.js";

export interface IUserRepository {
  findByUsername(username: string): Promise<{ id: string; user: User } | null>;
  create(username: string, passwordHash: string): Promise<User>;
  usernameExists(username: string): Promise<boolean>;
  findById(id: string): Promise<User | null>;
  updateAvatar(userId: string, avatar: string): Promise<void>;
  updateMood(userId: string, mood: string | null): Promise<void>;
  updateFcmToken(userId: string, fcmToken: string | null): Promise<void>;
  updateBackground(userId: string, background: string | null): Promise<void>;
  updateProfile(userId: string, data: Partial<User>): Promise<void>;
}
