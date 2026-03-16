import express from 'express';
import db from '../controllers/DatabaseManager.js';

const router = express.Router();

// Get or create chat between two users
router.post('/get-or-create', (req, res) => {
    try {
        const { user1Id, user2Id } = req.body;

        if (!user1Id || !user2Id) {
            return res.status(400).json({ error: 'Both user IDs are required' });
        }

        // Check if chat already exists (order doesn't matter)
        let chat = db.prepare(`
            SELECT * FROM chats 
            WHERE (user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)
        `).get(user1Id, user2Id, user2Id, user1Id);

        if (!chat) {
            // Create new chat
            const result = db.prepare('INSERT INTO chats (user1_id, user2_id) VALUES (?, ?)').run(user1Id, user2Id);
            chat = db.prepare('SELECT * FROM chats WHERE chat_id = ?').get(result.lastID);
        }

        res.json(chat);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to get or create chat' });
    }
});

// Get all chats for a user
router.get('/user/:userId', (req, res) => {
    try {
        const { userId } = req.params;

        const chats = db.prepare(`
            SELECT c.*, 
                   u.user_id as friend_id, u.firstname, u.lastname, u.profile_picture_path,
                   (SELECT message_text FROM messages WHERE chat_id = c.chat_id ORDER BY created_at DESC LIMIT 1) as last_message,
                   (SELECT created_at FROM messages WHERE chat_id = c.chat_id ORDER BY created_at DESC LIMIT 1) as last_message_time
            FROM chats c
            INNER JOIN users u ON (CASE WHEN c.user1_id = ? THEN c.user2_id ELSE c.user1_id END = u.user_id)
            WHERE c.user1_id = ? OR c.user2_id = ?
            ORDER BY last_message_time DESC
        `).all(userId, userId, userId);

        res.json(chats);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch chats' });
    }
});

// Get messages for a chat
router.get('/:chatId/messages', (req, res) => {
    try {
        const { chatId } = req.params;
        const limit = req.query.limit || 100;

        const messages = db.prepare(`
            SELECT m.*, u.firstname, u.lastname
            FROM messages m
            INNER JOIN users u ON m.sender_id = u.user_id
            WHERE m.chat_id = ?
            ORDER BY m.created_at ASC
            LIMIT ?
        `).all(chatId, parseInt(limit));

        res.json(messages);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch messages' });
    }
});

// Send message (also handled via WebSocket, but this is for fallback)
router.post('/:chatId/messages', (req, res) => {
    try {
        const { chatId } = req.params;
        const { senderId, messageText } = req.body;

        if (!senderId || !messageText) {
            return res.status(400).json({ error: 'Sender ID and message text are required' });
        }

        const result = db.prepare('INSERT INTO messages (chat_id, sender_id, message_text) VALUES (?, ?, ?)').run(chatId, senderId, messageText);

        const message = db.prepare(`
            SELECT m.*, u.firstname, u.lastname
            FROM messages m
            INNER JOIN users u ON m.sender_id = u.user_id
            WHERE m.message_id = ?
        `).get(result.lastID);

        res.json(message);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to send message' });
    }
});

export default router;
