# Stage 1: Build stage
FROM node:20-slim AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Limit memory usage for npm/node in resource-constrained environments
ENV NODE_OPTIONS="--max-old-space-size=512"

# Install all dependencies (including devDependencies for building)
RUN npm install --no-audit --no-fund --loglevel=error

# Copy tsconfig and source files
COPY tsconfig.json ./
COPY src/ ./src/

# Compile TypeScript
RUN npm run build

# Stage 2: Runtime stage (production environment)
FROM node:20-slim AS runner

WORKDIR /app

# Set environment to production
ENV NODE_ENV=production

# Copy package files
COPY package*.json ./

# Limit memory usage for npm/node in resource-constrained environments
ENV NODE_OPTIONS="--max-old-space-size=512"

# Install only production dependencies to minimize image size
RUN npm install --omit=dev --no-audit --no-fund --loglevel=error && npm cache clean --force

# Copy compiled code from builder
COPY --from=builder /app/dist ./dist

# Expose server port (default 3000)
EXPOSE 3000

# Start server directly with Node for proper OS signal propagation (SIGTERM, SIGINT)
CMD ["node", "dist/index.js"]
