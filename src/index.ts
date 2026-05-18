import { serve } from '@hono/node-server'
import { Hono } from 'hono'
import { logger } from 'hono/logger'
import { cors } from 'hono/cors'
import { ENV } from './config/env.js'
import { authRouter } from './routes/auth-routes.js'
import { friendRouter } from './routes/friend-routes.js'
import { initWebSocketServer } from './services/websocket-service.js'

const app = new Hono()

app.use('*', logger())
app.use('*', cors())

app.get('/', (c) => {
  return c.text('Z-Talk SOLID Backend is running!')
})

// Mount routes
app.route('/api/auth', authRouter)
app.route('/api/friends', friendRouter)

console.log(`Server is running on port ${ENV.PORT}`)

const server = serve({
  fetch: app.fetch,
  port: ENV.PORT
})

initWebSocketServer(server)
