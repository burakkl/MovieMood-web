import express from 'express';
import multer from 'multer';
import path from 'path';
import { fileURLToPath } from 'url';
import { dirname } from 'path';
import fs from 'fs';
import db from '../controllers/DatabaseManager.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const router = express.Router();

// Create user_uploads directory if it doesn't exist
const uploadsDir = path.join(__dirname, '..', '..', 'user_uploads');
if (!fs.existsSync(uploadsDir)) {
    fs.mkdirSync(uploadsDir, { recursive: true });
}

// Configure multer for file upload
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, uploadsDir);
    },
    filename: (req, file, cb) => {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        cb(null, 'profile-' + uniqueSuffix + path.extname(file.originalname));
    }
});

const upload = multer({
    storage: storage,
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

// Upload profile picture
router.post('/profile-picture', upload.single('profilePicture'), (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ error: 'No file uploaded' });
        }

        const { userId } = req.body;

        if (!userId) {
            // Delete uploaded file if userId not provided
            fs.unlinkSync(req.file.path);
            return res.status(400).json({ error: 'User ID is required' });
        }

        const filePath = `user_uploads/${req.file.filename}`;

        // Update user's profile picture path in database
        db.prepare('UPDATE users SET profile_picture_path = ? WHERE user_id = ?').run(filePath, userId);

        res.json({
            message: 'Profile picture uploaded successfully',
            profilePicturePath: filePath
        });
    } catch (error) {
        console.error(error);
        if (req.file) {
            fs.unlinkSync(req.file.path);
        }
        res.status(500).json({ error: 'Failed to upload profile picture' });
    }
});

export default router;
