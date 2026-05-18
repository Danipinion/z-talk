import { Context, Next } from 'hono';
import jwt from 'jsonwebtoken';
import { ENV } from './env.js';

export interface AuthUser {
  id: string;
  username: string;
}

export async function authMiddleware(c: Context, next: Next) {
  const authHeader = c.req.header('Authorization');
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return c.json({ error: 'Unauthorized: Missing or invalid token' }, 401);
  }

  const token = authHeader.split(' ')[1];
  try {
    const decoded = jwt.verify(token, ENV.JWT_SECRET) as AuthUser;
    c.set('user', decoded);
    await next();
  } catch (err) {
    return c.json({ error: 'Unauthorized: Invalid token' }, 401);
  }
}
