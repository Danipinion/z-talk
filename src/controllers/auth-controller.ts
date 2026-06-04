import { Context } from 'hono';
import { AuthService } from '../services/auth-service.js';

export class AuthController {
  constructor(private authService: AuthService) {}

  register = async (c: Context) => {
    try {
      const { username, password } = await c.req.json();
      const result = await this.authService.register(username, password);
      return c.json(result);
    } catch (error: any) {
      console.error('Register controller error:', error.message);
      const status = error.message.includes('required') || error.message.includes('exists') ? 400 : 500;
      return c.json({ error: error.message }, status);
    }
  };

  login = async (c: Context) => {
    try {
      const { username, password } = await c.req.json();
      const result = await this.authService.login(username, password);
      return c.json(result);
    } catch (error: any) {
      console.error('Login controller error:', error.message);
      const status = error.message.includes('required') ? 400 : error.message.includes('Invalid') ? 401 : 500;
      return c.json({ error: error.message }, status);
    }
  };

  checkUsername = async (c: Context) => {
    try {
      const username = c.req.param('username') || '';
      const available = await this.authService.checkUsername(username);
      return c.json({ available });
    } catch (error: any) {
      console.error('Check username controller error:', error.message);
      const status = error.message.includes('required') ? 400 : 500;
      return c.json({ error: error.message }, status);
    }
  };

  updateAvatar = async (c: Context) => {
    try {
      const user = c.get('user');
      const { avatar } = await c.req.json();
      await this.authService.updateAvatar(user.id, avatar);
      return c.json({ message: 'Avatar updated successfully' });
    } catch (error: any) {
      console.error('Update avatar controller error:', error.message);
      return c.json({ error: error.message }, 400);
    }
  };

  getProfile = async (c: Context) => {
    try {
      const user = c.get('user');
      const profile = await this.authService.getProfile(user.id);
      return c.json(profile);
    } catch (error: any) {
      console.error('Get profile controller error:', error.message);
      return c.json({ error: error.message }, 400);
    }
  };
}
