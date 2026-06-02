import express from 'express';
import multer from 'multer';
import path from 'path';
import db from '../controllers/DatabaseManager.js';

const router = express.Router();

// Use memory storage — no filesystem needed (works on Render)
const upload = multer({
    storage: multer.memoryStorage(),
    limits: { fileSize: 5 * 1024 * 1024 }, // 5MB limit
    fileFilter: (req, file, cb) => {
        const allowedTypes = /jpeg|jpg|png|gif|webp/;
        const extname = allowedTypes.test(path.extname(file.originalname).toLowerCase());
        const mimetype = allowedTypes.test(file.mimetype);
        if (mimetype && extname) {
            return cb(null, true);
        } else {
            cb(new Error('Only image files are allowed!'));
        }
    }
});

// Upload profile picture — stored as base64 data URL in database
router.post('/profile-picture', upload.single('profilePicture'), (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ error: 'No file uploaded' });
        }

        const { userId } = req.body;

        if (!userId) {
            return res.status(400).json({ error: 'User ID is required' });
        }

        // Convert to base64 data URL
        const base64 = req.file.buffer.toString('base64');
        const dataUrl = `data:${req.file.mimetype};base64,${base64}`;

        // Save directly in database
        db.prepare('UPDATE users SET profile_picture_path = ? WHERE user_id = ?').run(dataUrl, userId);

        res.json({
            message: 'Profile picture uploaded successfully',
            profilePicturePath: dataUrl
        });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to upload profile picture' });
    }
});

export default router;
