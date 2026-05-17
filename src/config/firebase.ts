import admin from 'firebase-admin';
import fs from 'fs';
import path from 'path';
import { ENV } from './env.js';

let credential;

if (ENV.FIREBASE_SERVICE_ACCOUNT_JSON) {
  try {
    credential = admin.credential.cert(JSON.parse(ENV.FIREBASE_SERVICE_ACCOUNT_JSON));
    console.log('Firebase initialized using FIREBASE_SERVICE_ACCOUNT_JSON env variable.');
  } catch (e) {
    console.error('Failed to parse Firebase Env JSON:', e);
  }
} else {
  const keyPath = path.join(process.cwd(), 'serviceAccountKey.json');
  if (fs.existsSync(keyPath)) {
    try {
      const key = JSON.parse(fs.readFileSync(keyPath, 'utf-8'));
      credential = admin.credential.cert(key);
      console.log('Firebase initialized using serviceAccountKey.json file.');
    } catch (e) {
      console.error('Failed to read/parse serviceAccountKey.json:', e);
    }
  }
}

if (!credential) {
  console.warn('⚠️ WARNING: No serviceAccountKey.json found or FIREBASE_SERVICE_ACCOUNT_JSON env variable provided. Falling back to applicationDefault() which might fail in local environments.');
  credential = admin.credential.applicationDefault();
}

try {
  admin.initializeApp({
    credential,
    databaseURL: ENV.FIREBASE_DATABASE_URL
  });
} catch (e) {
  console.log('Firebase initialization warning or already initialized:', e);
}

export const db = admin.database();
export default admin;
