import express from 'express';
import db from '../controllers/DatabaseManager.js';

const router = express.Router();

// Register
router.post('/register', (req, res) => {
    const { email, firstname, lastname, password } = req.body;

    try {
        // Check if user exists
        const existingUser = db.prepare('SELECT * FROM users WHERE email = ?').get(email);
        if (existingUser) {
            return res.status(400).json({ error: 'Email already registered' });
        }

        // Generate random user ID
        const userId = Math.floor(1000 + Math.random() * 9000);

        // Generate 4-digit unique user code
        const userCode = Math.floor(1000 + Math.random() * 9000).toString();

        // Random profile picture (1-7)
        const profilePicture = `images/${Math.floor(Math.random() * 7) + 1}.jpg`;

        // Insert user (in production, hash the password!)
        const stmt = db.prepare(
            'INSERT INTO users (user_id, firstname, lastname, email, password_hash, profile_picture_path, user_code) VALUES (?, ?, ?, ?, ?, ?, ?)'
        );
        stmt.run(userId, firstname, lastname, email, password, profilePicture, userCode);

        const user = { userId, firstname, lastname, email, profilePicture, userCode };
        req.session.userId = userId;

        res.json({ message: 'Registration successful', user });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Registration failed' });
    }
});

// Login
router.post('/login', (req, res) => {
    const { email, password } = req.body;

    try {
        const user = db.prepare('SELECT * FROM users WHERE email = ? AND password_hash = ?').get(email, password);

        if (!user) {
            return res.status(401).json({ error: 'Invalid credentials' });
        }

        // Ensure user has a user_code (backward compatibility)
        if (!user.user_code) {
            user.user_code = Math.floor(1000 + Math.random() * 9000).toString();
            db.prepare('UPDATE users SET user_code = ? WHERE user_id = ?').run(user.user_code, user.user_id);
        }

        // Set session user ID
        req.session.userId = user.user_id;
        console.log('Login successful, session userId set:', req.session.userId);

        req.session.save(err => {
            if (err) {
                console.error('Session save error:', err);
                return res.status(500).json({ error: 'Session save failed' });
            }
            res.json({
                message: 'Login successful',
                user: {
                    userId: user.user_id,
                    firstname: user.firstname,
                    lastname: user.lastname,
                    email: user.email,
                    profilePicture: user.profile_picture_path,
                    userCode: user.user_code
                }
            });
        });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Login failed' });
    }
});

// Logout
router.post('/logout', (req, res) => {
    req.session.destroy();
    res.json({ message: 'Logged out successfully' });
});

// Get current user
router.get('/me', (req, res) => {
    res.set('Cache-Control', 'no-store');
    if (!req.session.userId) {
        return res.status(401).json({ error: 'Not authenticated' });
    }

    const user = db.prepare('SELECT * FROM users WHERE user_id = ?').get(req.session.userId);

    if (!user) {
        return res.status(404).json({ error: 'User not found' });
    }

    res.json({
        userId: user.user_id,
        firstname: user.firstname,
        lastname: user.lastname,
        email: user.email,
        profilePicture: user.profile_picture_path,
        userCode: user.user_code
    });
});

export default router;
