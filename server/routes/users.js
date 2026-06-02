import express from 'express';
import db from '../controllers/DatabaseManager.js';

const router = express.Router();

// Find user by user_code (the visible ID shown on profile)
router.get('/find/:code', async (req, res) => {
    const { code } = req.params;
    try {
        const user = await db.prepare(
            'SELECT user_id, firstname, lastname, email, profile_picture_path, user_code FROM users WHERE user_code = ?'
        ).get(code);
        if (!user) return res.status(404).json({ error: 'User not found' });
        res.json(user);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to find user' });
    }
});

// Get user by ID
router.get('/:id', async (req, res) => {
    const { id } = req.params;
    try {
        const user = await db.prepare('SELECT user_id, firstname, lastname, email, profile_picture_path, user_code FROM users WHERE user_id = ?').get(id);
        if (!user) return res.status(404).json({ error: 'User not found' });
        res.json(user);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch user' });
    }
});

// Get user's favorites
router.get('/:id/favorites', async (req, res) => {
    const { id } = req.params;
    try {
        const favorites = await db.prepare(`
            SELECT m.* FROM movies m
            INNER JOIN user_favorites uf ON m.movie_id = uf.movie_id
            WHERE uf.user_id = ?
        `).all(id);
        res.json(favorites);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch favorites' });
    }
});

// Add to favorites
router.post('/:id/favorites', async (req, res) => {
    const userId = req.session.userId;
    const { movie_id } = req.body;

    if (!userId) return res.status(401).json({ error: 'Not authenticated' });
    if (!movie_id) return res.status(400).json({ error: 'Movie ID is required' });

    try {
        await db.prepare('INSERT OR IGNORE INTO user_favorites (user_id, movie_id) VALUES (?, ?)').run(userId, movie_id);
        res.json({ message: 'Added to favorites' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to add favorite' });
    }
});

// Remove from favorites
router.delete('/:id/favorites/:movieId', async (req, res) => {
    const userId = req.session.userId;
    const { movieId } = req.params;

    if (!userId) return res.status(401).json({ error: 'Not authenticated' });

    try {
        await db.prepare('DELETE FROM user_favorites WHERE user_id = ? AND movie_id = ?').run(userId, movieId);
        res.json({ message: 'Removed from favorites' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to remove favorite' });
    }
});

export default router;
