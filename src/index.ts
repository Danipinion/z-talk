import { serve } from '@hono/node-server'
import { Hono } from 'hono'
import { logger } from 'hono/logger'
import { cors } from 'hono/cors'
import admin from 'firebase-admin'
import bcrypt from 'bcryptjs'
import jwt from 'jsonwebtoken'
import 'dotenv/config'

const app = new Hono()

// Initialize Firebase Admin (Update with your actual service account credentials)
// For local development without service account JSON file, 
// you can set GOOGLE_APPLICATION_CREDENTIALS in .env or provide an object here.
try {
  admin.initializeApp({
    credential: admin.credential.applicationDefault(),
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
