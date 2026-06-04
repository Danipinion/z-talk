export interface User {
  id: string;
  username: string;
  password?: string;
  createdAt?: number;
  avatar?: string;
  mood?: string;
}

export interface AuthResponse {
  message: string;
  token?: string;
  user?: Omit<User, 'password'>;
  error?: string;
}
