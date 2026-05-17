import { serve } from '@hono/node-server'
import { Hono } from 'hono'
import { logger } from 'hono/logger'
import { cors } from 'hono/cors'
import admin from 'firebase-admin'
import bcrypt from 'bcryptjs'
import jwt from 'jsonwebtoken'
import 'dotenv/config'

import fs from 'fs'
import path from 'path'

const app = new Hono()

let credential;

// Try to load from env variable as JSON string
if (process.env.FIREBASE_SERVICE_ACCOUNT_JSON) {
  try {
    const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON)
    credential = admin.credential.cert(serviceAccount)
    console.log('Firebase Admin initialized using FIREBASE_SERVICE_ACCOUNT_JSON env variable.')
  } catch (err) {
    console.error('Error parsing FIREBASE_SERVICE_ACCOUNT_JSON env:', err)
  }
} 
// Try to load from a local serviceAccountKey.json file
else {
  const serviceAccountPath = path.join(process.cwd(), 'serviceAccountKey.json')
  if (fs.existsSync(serviceAccountPath)) {
    try {
      const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, 'utf8'))
      credential = admin.credential.cert(serviceAccount)
      console.log('Firebase Admin initialized using local serviceAccountKey.json file.')
    } catch (err) {
      console.error('Error reading/parsing local serviceAccountKey.json:', err)
    }
  }
}

if (!credential) {
  console.warn('⚠️ WARNING: No serviceAccountKey.json found or FIREBASE_SERVICE_ACCOUNT_JSON env variable provided. Falling back to applicationDefault() which might fail in local environments.')
  credential = admin.credential.applicationDefault()
}

try {
  admin.initializeApp({
    credential,
    databaseURL: process.env.FIREBASE_DATABASE_URL || 'https://your-database-name.firebaseio.com'
  })
} catch (e) {
  console.log('Firebase init error (might be already initialized):', e)
}

const db = admin.database()
const JWT_SECRET = process.env.JWT_SECRET || 'super-secret-jwt-key'

app.use('*', logger())
app.use('*', cors())

app.get('/', (c) => {
  return c.text('Z-Talk Backend is running!')
})

// Check Username API
app.get('/api/auth/check-username/:username', async (c) => {
  try {
    const username = c.req.param('username')
    if (!username) {
      return c.json({ error: 'Username is required' }, 400)
    }
    const usersRef = db.ref('users')
    const snapshot = await usersRef.orderByChild('username').equalTo(username).once('value')
    if (snapshot.exists()) {
      return c.json({ available: false })
    }
    return c.json({ available: true })
  } catch (error) {
    console.error('Check username error:', error)
    return c.json({ error: 'Internal server error' }, 500)
  }
})

// Register API
app.post('/api/auth/register', async (c) => {
  try {
    const { username, password } = await c.req.json()

    if (!username || !password) {
      return c.json({ error: 'Username and password are required' }, 400)
    }

    const usersRef = db.ref('users')
    
    // Check if user exists
    const snapshot = await usersRef.orderByChild('username').equalTo(username).once('value')
    if (snapshot.exists()) {
      return c.json({ error: 'Username already exists' }, 400)
    }

    const hashedPassword = await bcrypt.hash(password, 10)
    
    const newUserRef = usersRef.push()
    await newUserRef.set({
      username,
      password: hashedPassword,
      createdAt: admin.database.ServerValue.TIMESTAMP
    })

    const token = jwt.sign({ id: newUserRef.key, username }, JWT_SECRET, { expiresIn: '7d' })

    return c.json({
      message: 'User registered successfully',
      token,
      user: { id: newUserRef.key, username }
    })
  } catch (error) {
    console.error('Register error:', error)
    return c.json({ error: 'Internal server error' }, 500)
  }
})

// Login API
app.post('/api/auth/login', async (c) => {
  try {
    const { username, password } = await c.req.json()

    if (!username || !password) {
      return c.json({ error: 'Username and password are required' }, 400)
    }

    const usersRef = db.ref('users')
    const snapshot = await usersRef.orderByChild('username').equalTo(username).once('value')
    
    if (!snapshot.exists()) {
      return c.json({ error: 'Invalid username or password' }, 401)
    }

    let userData: any = null
    let userId: string = ''
    
    snapshot.forEach((child) => {
      userData = child.val()
      userId = child.key as string
    })

    const isMatch = await bcrypt.compare(password, userData.password)
    if (!isMatch) {
      return c.json({ error: 'Invalid username or password' }, 401)
    }

    const token = jwt.sign({ id: userId, username }, JWT_SECRET, { expiresIn: '7d' })

    return c.json({
      message: 'Login successful',
      token,
      user: { id: userId, username }
    })
  } catch (error) {
    console.error('Login error:', error)
    return c.json({ error: 'Internal server error' }, 500)
  }
})

const port = process.env.PORT ? parseInt(process.env.PORT) : 3000
console.log(`Server is running on port ${port}`)

serve({
  fetch: app.fetch,
  port
})
