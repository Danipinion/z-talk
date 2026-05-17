import { User } from '../models/user.js';

export interface IUserRepository {
  findByUsername(username: string): Promise<{ id: string; user: User } | null>;
  create(username: string, passwordHash: string): Promise<User>;
  usernameExists(username: string): Promise<boolean>;
}
