import express from 'express';
import db from '../controllers/DatabaseManager.js';

const router = express.Router();

// Get all movies
router.get('/', (req, res) => {
    try {
        const movies = db.prepare('SELECT * FROM movies LIMIT 100').all();
        res.json(movies);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch movies' });
    }
});


// Get recent movies (top 20 by release date)
router.get('/recent', (req, res) => {
    try {
        const movies = db.prepare('SELECT * FROM movies ORDER BY release_date DESC LIMIT 20').all();
        res.json(movies);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch recent movies' });
    }
});

// Get movie by ID with genres
router.get('/:id', (req, res) => {
    try {
        const { id } = req.params;
        const movie = db.prepare('SELECT * FROM movies WHERE movie_id = ?').get(id);

        if (!movie) {
            return res.status(404).json({ error: 'Movie not found' });
        }

        const genres = db.prepare('SELECT genre FROM movie_genres WHERE movie_id = ?').all(id);
        movie.genres = genres.map(g => g.genre);

        // Get average rating
        const ratingResult = db.prepare('SELECT AVG(rating) as average FROM user_ratings WHERE movie_id = ?').get(id);
        movie.averageRating = ratingResult ? ratingResult.average : 0;

        res.json(movie);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch movie' });
    }
});


// Search movies by title
router.get('/search/query', (req, res) => {
    try {
        const { q } = req.query;

        if (!q) {
            return res.status(400).json({ error: 'Search query is required' });
        }

        const movies = db.prepare(`
            SELECT DISTINCT m.* FROM movies m
            WHERE m.title LIKE ? OR m.original_title LIKE ?
            LIMIT 50
        `).all(`%${q}%`, `%${q}%`);

        res.json(movies);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to search movies' });
    }
});

// Get movies by genre
router.get('/genre/:genre', (req, res) => {
    try {
        const { genre } = req.params;

        const movies = db.prepare(`
            SELECT DISTINCT m.* FROM movies m
            INNER JOIN movie_genres mg ON m.movie_id = mg.movie_id
            WHERE mg.genre = ?
            LIMIT 100
        `).all(genre);

        res.json(movies);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch movies by genre' });
    }
});

// Get movies by year range
router.get('/year/:startYear/:endYear', (req, res) => {
    try {
        const { startYear, endYear } = req.params;

        const movies = db.prepare(`
            SELECT * FROM movies
            WHERE release_date >= ? AND release_date <= ?
            ORDER BY release_date DESC
            LIMIT 100
        `).all(parseInt(startYear), parseInt(endYear));

        res.json(movies);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch movies by year' });
    }
});

// Get random movies
router.get('/random/selection', (req, res) => {
    try {
        const limit = req.query.limit || 20;

        const movies = db.prepare(`
            SELECT * FROM movies
            ORDER BY RANDOM()
            LIMIT ?
        `).all(parseInt(limit));

        res.json(movies);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch random movies' });
    }
});

// Get movie comments
router.get('/:id/comments', (req, res) => {
    try {
        const { id } = req.params;

        const comments = db.prepare(`
            SELECT c.*, u.firstname, u.lastname, u.user_id
            FROM comments c
            INNER JOIN users u ON c.user_id = u.user_id
            WHERE c.movie_id = ?
            ORDER BY c.created_at DESC
        `).all(id);

        res.json(comments);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch comments' });
    }
});

// Add comment to movie
router.post('/:id/comments', (req, res) => {
    try {
        const { id } = req.params;
        const { userId, commentText } = req.body;

        if (!userId || !commentText) {
            return res.status(400).json({ error: 'User ID and comment text are required' });
        }

        db.prepare('INSERT INTO comments (movie_id, user_id, comment_text) VALUES (?, ?, ?)').run(id, userId, commentText);

        res.json({ message: 'Comment added successfully' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to add comment' });
    }
});

// Edit comment
router.put('/:id/comments/:commentId', (req, res) => {
    try {
        const { commentId } = req.params;
        const { userId, commentText } = req.body;

        if (!commentText) {
            return res.status(400).json({ error: 'Comment text is required' });
        }

        // Verify ownership
        const comment = db.prepare('SELECT * FROM comments WHERE comment_id = ? AND user_id = ?').get(commentId, userId);

        if (!comment) {
            return res.status(403).json({ error: 'Not authorized to edit this comment' });
        }

        db.prepare('UPDATE comments SET comment_text = ? WHERE comment_id = ?').run(commentText, commentId);

        res.json({ message: 'Comment updated successfully' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to update comment' });
    }
});

// Delete comment
router.delete('/:id/comments/:commentId', (req, res) => {
    try {
        const { commentId } = req.params;
        const { userId } = req.body;

        // Verify ownership
        const comment = db.prepare('SELECT * FROM comments WHERE comment_id = ? AND user_id = ?').get(commentId, userId);

        if (!comment) {
            return res.status(403).json({ error: 'Not authorized to delete this comment' });
        }

        db.prepare('DELETE FROM comments WHERE comment_id = ?').run(commentId);

        res.json({ message: 'Comment deleted successfully' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to delete comment' });
    }
});

// Rate movie
router.post('/:id/rating', (req, res) => {
    try {
        const { id } = req.params;
        const { userId, rating } = req.body;

        if (!userId || rating === undefined) {
            return res.status(400).json({ error: 'User ID and rating are required' });
        }

        if (rating < 1 || rating > 10) {
            return res.status(400).json({ error: 'Rating must be between 1 and 10' });
        }

        // Upsert rating
        const existing = db.prepare('SELECT * FROM user_ratings WHERE user_id = ? AND movie_id = ?').get(userId, id);

        if (existing) {
            db.prepare('UPDATE user_ratings SET rating = ? WHERE user_id = ? AND movie_id = ?').run(rating, userId, id);
        } else {
            db.prepare('INSERT INTO user_ratings (user_id, movie_id, rating) VALUES (?, ?, ?)').run(userId, id, rating);
        }

        res.json({ message: 'Rating submitted successfully' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to submit rating' });
    }
});

// Get average user rating for movie
router.get('/:id/average-rating', (req, res) => {
    try {
        const { id } = req.params;

        const result = db.prepare('SELECT AVG(rating) as average FROM user_ratings WHERE movie_id = ?').get(id);

        res.json({ average: result.average || 0 });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch average rating' });
    }
});

// Mark movie as watched
router.post('/:id/watch', (req, res) => {
    try {
        const { id } = req.params;
        const { userId } = req.body;

        if (!userId) {
            return res.status(400).json({ error: 'User ID is required' });
        }

        // Upsert watch history
        const existing = db.prepare('SELECT * FROM user_watch_history WHERE user_id = ? AND movie_id = ?').get(userId, id);

        if (existing) {
            db.prepare('UPDATE user_watch_history SET watched_at = CURRENT_TIMESTAMP WHERE user_id = ? AND movie_id = ?').run(userId, id);
        } else {
            db.prepare('INSERT INTO user_watch_history (user_id, movie_id) VALUES (?, ?)').run(userId, id);
        }

        // Enforce 10-movie limit (FIFO)
        const history = db.prepare('SELECT movie_id FROM user_watch_history WHERE user_id = ? ORDER BY watched_at DESC').all(userId);

        if (history.length > 10) {
            const moviesKeep = history.slice(0, 10).map(h => h.movie_id);
            // Delete movies not in the top 10 most recent
            const placeholders = moviesKeep.map(() => '?').join(',');
            db.prepare(`DELETE FROM user_watch_history WHERE user_id = ? AND movie_id NOT IN (${placeholders})`).run(userId, ...moviesKeep);
        }

        res.json({ message: 'Movie marked as watched' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to mark movie as watched' });
    }
});

// Get recently watched movies for user
router.get('/user/:userId/recent', (req, res) => {
    try {
        const { userId } = req.params;
        const limit = req.query.limit || 20;

        const movies = db.prepare(`
            SELECT m.*, wh.watched_at
            FROM movies m
            INNER JOIN user_watch_history wh ON m.movie_id = wh.movie_id
            WHERE wh.user_id = ?
            ORDER BY wh.watched_at DESC
            LIMIT ?
        `).all(userId, parseInt(limit));

        res.json(movies);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch recently watched movies' });
    }
});

// Get all unique genres
router.get('/meta/genres', (req, res) => {
    try {
        const genres = db.prepare('SELECT DISTINCT genre FROM movie_genres ORDER BY genre').all();
        res.json(genres.map(g => g.genre));
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch genres' });
    }
});

export default router;
