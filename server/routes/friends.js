import express from 'express';
import db from '../controllers/DatabaseManager.js';

const router = express.Router();

// Check friendship status between two users
router.get('/check/:userId/:targetId', (req, res) => {
    try {
        const { userId, targetId } = req.params;

        // Check if friends
        const isFriend = db.prepare('SELECT * FROM user_friends WHERE user_id = ? AND friend_id = ?').get(userId, targetId);
        if (isFriend) {
            return res.json({ status: 'friends' });
        }

        // Check for pending request (Sent)
        const sentRequest = db.prepare('SELECT * FROM friend_requests WHERE sender_id = ? AND receiver_id = ? AND status = ?').get(userId, targetId, 'pending');
        if (sentRequest) {
            return res.json({ status: 'pending_sent', requestId: sentRequest.request_id });
        }

        // Check for pending request (Received)
        const receivedRequest = db.prepare('SELECT * FROM friend_requests WHERE sender_id = ? AND receiver_id = ? AND status = ?').get(targetId, userId, 'pending');
        if (receivedRequest) {
            return res.json({ status: 'pending_received', requestId: receivedRequest.request_id });
        }

        res.json({ status: 'none' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to check friendship status' });
    }
});

// Get user's friends list
router.get('/:userId', (req, res) => {
    try {
        const { userId } = req.params;

        const friends = db.prepare(`
            SELECT u.user_id, u.firstname, u.lastname, u.email, u.profile_picture_path
            FROM users u
            INNER JOIN user_friends uf ON u.user_id = uf.friend_id
            WHERE uf.user_id = ?
        `).all(userId);

        res.json(friends);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch friends' });
    }
});

// Send friend request by user ID
router.post('/request', (req, res) => {
    try {
        const { senderId, receiverId } = req.body;

        if (!senderId || !receiverId) {
            return res.status(400).json({ error: 'Sender ID and Receiver ID are required' });
        }

        if (senderId === receiverId) {
            return res.status(400).json({ error: 'Cannot send friend request to yourself' });
        }

        // Check if receiver exists
        const receiver = db.prepare('SELECT * FROM users WHERE user_id = ?').get(receiverId);
        if (!receiver) {
            return res.status(404).json({ error: 'User not found' });
        }

        // Check if already friends
        const areFriends = db.prepare('SELECT * FROM user_friends WHERE user_id = ? AND friend_id = ?').get(senderId, receiverId);
        if (areFriends) {
            return res.status(400).json({ error: 'Already friends with this user' });
        }

        // Check if request already exists
        const existingRequest = db.prepare(
            'SELECT * FROM friend_requests WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)'
        ).get(senderId, receiverId, receiverId, senderId);

        if (existingRequest) {
            return res.status(400).json({ error: 'Friend request already exists' });
        }

        db.prepare('INSERT INTO friend_requests (sender_id, receiver_id, status) VALUES (?, ?, ?)').run(senderId, receiverId, 'pending');

        res.json({ message: 'Friend request sent' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to send friend request' });
    }
});

// Get pending friend requests for a user
router.get('/requests/:userId', (req, res) => {
    try {
        const { userId } = req.params;

        const requests = db.prepare(`
            SELECT fr.request_id, fr.sender_id, fr.created_at, u.firstname, u.lastname, u.profile_picture_path
            FROM friend_requests fr
            INNER JOIN users u ON fr.sender_id = u.user_id
            WHERE fr.receiver_id = ? AND fr.status = 'pending'
            ORDER BY fr.created_at DESC
        `).all(userId);

        res.json(requests);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch friend requests' });
    }
});

// Accept friend request
router.post('/accept/:requestId', (req, res) => {
    try {
        const { requestId } = req.params;

        const request = db.prepare('SELECT * FROM friend_requests WHERE request_id = ? AND status = ?').get(requestId, 'pending');

        if (!request) {
            return res.status(404).json({ error: 'Friend request not found' });
        }

        // Add to user_friends (both directions)
        db.prepare('INSERT INTO user_friends (user_id, friend_id) VALUES (?, ?)').run(request.receiver_id, request.sender_id);
        db.prepare('INSERT INTO user_friends (user_id, friend_id) VALUES (?, ?)').run(request.sender_id, request.receiver_id);

        // Update request status
        db.prepare('UPDATE friend_requests SET status = ? WHERE request_id = ?').run('accepted', requestId);

        res.json({ message: 'Friend request accepted' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to accept friend request' });
    }
});

// Reject friend request
router.post('/reject/:requestId', (req, res) => {
    try {
        const { requestId } = req.params;

        const request = db.prepare('SELECT * FROM friend_requests WHERE request_id = ? AND status = ?').get(requestId, 'pending');

        if (!request) {
            return res.status(404).json({ error: 'Friend request not found' });
        }

        // Update request status
        db.prepare('UPDATE friend_requests SET status = ? WHERE request_id = ?').run('rejected', requestId);

        res.json({ message: 'Friend request rejected' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to reject friend request' });
    }
});

// Cancel/Delete friend request
router.delete('/request/:requestId', (req, res) => {
    try {
        const { requestId } = req.params;
        db.prepare('DELETE FROM friend_requests WHERE request_id = ?').run(requestId);
        res.json({ message: 'Friend request deleted' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to delete friend request' });
    }
});

// Remove friend
router.delete('/:friendId', (req, res) => {
    try {
        const { friendId } = req.params;
        const { userId } = req.body;

        if (!userId) {
            return res.status(400).json({ error: 'User ID is required' });
        }

        // Remove friendship (both directions)
        db.prepare('DELETE FROM user_friends WHERE user_id = ? AND friend_id = ?').run(userId, friendId);
        db.prepare('DELETE FROM user_friends WHERE user_id = ? AND friend_id = ?').run(friendId, userId);

        res.json({ message: 'Friend removed' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to remove friend' });
    }
});

export default router;
