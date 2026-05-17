import 'dotenv/config';

export const ENV = {
  PORT: process.env.PORT ? parseInt(process.env.PORT) : 3000,
  JWT_SECRET: process.env.JWT_SECRET || 'super-secret-jwt-key',
  FIREBASE_DATABASE_URL: process.env.FIREBASE_DATABASE_URL || 'https://your-database-name.firebaseio.com',
  FIREBASE_SERVICE_ACCOUNT_JSON: process.env.FIREBASE_SERVICE_ACCOUNT_JSON || '',
};
