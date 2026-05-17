import { serve } from '@hono/node-server'
import { Hono } from 'hono'
import { logger } from 'hono/logger'
import { cors } from 'hono/cors'
import { ENV } from './config/env.js'
import { authRouter } from './routes/auth-routes.js'

const app = new Hono()

app.use('*', logger())
app.use('*', cors())

app.get('/', (c) => {
  return c.text('Z-Talk SOLID Backend is running!')
})

// Mount authentication routes under /api/auth
app.route('/api/auth', authRouter)

console.log(`Server is running on port ${ENV.PORT}`)

serve({
  fetch: app.fetch,
  port: ENV.PORT
})
