import express from 'express';
import cors from 'cors';
import session from 'express-session';
import { createServer } from 'http';
import { Server } from 'socket.io';
import path from 'path';
import { fileURLToPath } from 'url';
import { dirname } from 'path';

import authRoutes from './routes/auth.js';
import movieRoutes from './routes/movies.js';
import friendRoutes from './routes/friends.js';
import userRoutes from './routes/users.js';
import listRoutes from './routes/lists.js';
import uploadRoutes from './routes/uploads.js';
import chatRoutes from './routes/chat.js';
import recommendationRoutes from './routes/recommendations.js';
import db from './controllers/DatabaseManager.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const app = express();
const httpServer = createServer(app);
const io = new Server(httpServer, {
  cors: {
    origin: 'http://localhost:5173',
    credentials: true
  }
});

const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors({
  origin: function (origin, callback) {
    // Allow requests with no origin (like mobile apps or curl requests)
    if (!origin) return callback(null, true);

    // Allow localhost, local network IPs, and external tunnels
    const allowedOrigins = [
      'http://localhost:5173',
      'http://localhost:5174',
      /^http:\/\/192\.168\.\d{1,3}\.\d{1,3}:\d{4}$/,
      /^http:\/\/139\.179\.\d{1,3}\.\d{1,3}:\d{4}$/,
      /https:\/\/.*\.loca\.lt$/
    ];

    const isAllowed = allowedOrigins.some(o =>
      o instanceof RegExp ? o.test(origin) : o === origin
    ) || true; // Temporarily allow all for troubleshooting

    if (isAllowed) {
      callback(null, true);
    } else {
      callback(new Error('Not allowed by CORS'));
    }
  },
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));
app.use(express.json());
app.use(session({
  secret: 'moviemood-secret-key',
  resave: true,
  saveUninitialized: true,
  cookie: {
    secure: false, // Set to true if using HTTPS
    httpOnly: true,
    maxAge: 24 * 60 * 60 * 1000 // 24 hours
  }
}));

// Debug Middleware
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
  console.log('Session ID:', req.sessionID);
  console.log('Session Data:', req.session);
  next();
});

// Serve static files for uploads
app.use('/user_uploads', express.static(path.join(__dirname, '..', 'user_uploads')));

// Routes
app.use('/api/auth', authRoutes);
app.use('/api/movies', movieRoutes);
app.use('/api/friends', friendRoutes);
app.use('/api/users', userRoutes);
app.use('/api/lists', listRoutes);
app.use('/api/upload', uploadRoutes);
app.use('/api/chat', chatRoutes);
app.use('/api/recommendations', recommendationRoutes);

// Health check
app.get('/api/health', (req, res) => {
  res.json({ status: 'OK', message: 'MovieMood API is running' });
});

// WebSocket for real-time chat
const connectedUsers = new Map(); // userId -> socketId

io.on('connection', (socket) => {
  console.log('User connected:', socket.id);

  // Register user
  socket.on('register', (userId) => {
    connectedUsers.set(userId, socket.id);
    console.log(`User ${userId} registered with socket ${socket.id}`);
  });

  // Send message
  socket.on('send_message', (data) => {
    const { chatId, senderId, receiverId, messageText } = data;

    try {
      // Save message to database
      const result = db.prepare('INSERT INTO messages (chat_id, sender_id, message_text) VALUES (?, ?, ?)').run(chatId, senderId, messageText);

      const message = db.prepare(`
        SELECT m.*, u.firstname, u.lastname
        FROM messages m
        INNER JOIN users u ON m.sender_id = u.user_id
        WHERE m.message_id = ?
      `).get(result.lastID);

      // Send to sender
      socket.emit('message_received', message);

      // Send to receiver if online
      const receiverSocketId = connectedUsers.get(parseInt(receiverId));
      if (receiverSocketId) {
        io.to(receiverSocketId).emit('message_received', message);
      }
    } catch (error) {
      console.error('Error sending message:', error);
      socket.emit('message_error', { error: 'Failed to send message' });
    }
  });

  // Handle disconnect
  socket.on('disconnect', () => {
    // Remove user from connected users
    for (const [userId, socketId] of connectedUsers.entries()) {
      if (socketId === socket.id) {
        connectedUsers.delete(userId);
        console.log(`User ${userId} disconnected`);
        break;
      }
    }
  });
});

httpServer.listen(PORT, '0.0.0.0', () => {
  console.log(`🎬 MovieMood server running on http://0.0.0.0:${PORT}`);
});
