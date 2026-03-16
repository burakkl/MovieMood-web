import express from 'express';
import db from '../controllers/DatabaseManager.js';

const router = express.Router();

// Get user by ID
router.get('/:id', (req, res) => {
    const { id } = req.params;

    try {
        const user = db.prepare('SELECT user_id, firstname, lastname, email, profile_picture_path, user_code FROM users WHERE user_id = ?').get(id);

        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        res.json(user);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch user' });
    }
});

// Get user's favorites
router.get('/:id/favorites', (req, res) => {
    const { id } = req.params;

    try {
        const favorites = db.prepare(`
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
router.post('/:id/favorites', (req, res) => {
    const userId = req.session.userId;
    const { movie_id } = req.body;

    console.log('POST /favorites request:', { sessionUserId: userId, bodyMovieId: movie_id, paramId: req.params.id });

    if (!userId) {
        console.log('Authentication failed: No session userId');
        return res.status(401).json({ error: 'Not authenticated' });
    }

    if (!movie_id) {
        console.log('Missing movie_id in body');
        return res.status(400).json({ error: 'Movie ID is required' });
    }

    try {
        db.prepare('INSERT OR IGNORE INTO user_favorites (user_id, movie_id) VALUES (?, ?)').run(userId, movie_id);
        res.json({ message: 'Added to favorites' });
    } catch (error) {
        console.error('Database error adding favorite:', error);
        res.status(500).json({ error: 'Failed to add favorite' });
    }
});

// Remove from favorites
router.delete('/:id/favorites/:movieId', (req, res) => {
    const userId = req.session.userId;
    const { movieId } = req.params;

    if (!userId) {
        return res.status(401).json({ error: 'Not authenticated' });
    }

    try {
        db.prepare('DELETE FROM user_favorites WHERE user_id = ? AND movie_id = ?').run(userId, movieId);
        res.json({ message: 'Removed from favorites' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to remove favorite' });
    }
});

export default router;
