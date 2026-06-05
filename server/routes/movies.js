import express from 'express';
import db from '../controllers/DatabaseManager.js';

const router = express.Router();

router.get('/', async (req, res) => {
    try {
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const offset = (page - 1) * limit;
        const sortBy = req.query.sortBy || 'popularity';
        const order = (req.query.order || 'DESC').toUpperCase();

        let orderBy = 'ORDER BY popularity DESC';
        if (sortBy === 'title') {
            orderBy = `ORDER BY title ${order === 'DESC' ? 'DESC' : 'ASC'}`;
        } else if (sortBy === 'rating') {
            orderBy = `ORDER BY rating ${order === 'ASC' ? 'ASC' : 'DESC'}`;
        }

        const movies = await db.prepare(`SELECT * FROM movies ${orderBy} LIMIT ? OFFSET ?`).all(limit, offset);
        res.json(movies);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch movies' });
    }
});

router.get('/recent', async (req, res) => {
    try {
        const movies = await db.prepare('SELECT * FROM movies ORDER BY release_date DESC LIMIT 20').all();
        res.json(movies);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch recent movies' });
    }
});

router.get('/random/selection', async (req, res) => {
    try {
        const limit = req.query.limit || 20;
        const movies = await db.prepare('SELECT * FROM movies ORDER BY RANDOM() LIMIT ?').all(parseInt(limit));
        res.json(movies);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch random movies' });
    }
});

router.get('/meta/genres', async (req, res) => {
    try {
        const genres = await db.prepare('SELECT DISTINCT genre FROM movie_genres ORDER BY genre').all();
        res.json(genres.map(g => g.genre));
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch genres' });
    }
});

router.get('/search/query', async (req, res) => {
    try {
        const { q } = req.query;
        if (!q) return res.status(400).json({ error: 'Search query is required' });

        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const offset = (page - 1) * limit;
        const sortBy = req.query.sortBy || 'popularity';
        const order = (req.query.order || 'DESC').toUpperCase();

        let orderBy = 'ORDER BY m.popularity DESC';
        if (sortBy === 'title') {
            orderBy = `ORDER BY m.title ${order === 'DESC' ? 'DESC' : 'ASC'}`;
        } else if (sortBy === 'rating') {
            orderBy = `ORDER BY m.rating ${order === 'ASC' ? 'ASC' : 'DESC'}`;
        }

        const movies = await db.prepare(`
            SELECT DISTINCT m.* FROM movies m
            WHERE m.title ILIKE ? OR m.original_title ILIKE ?
            ${orderBy}
            LIMIT ? OFFSET ?
        `).all(`%${q}%`, `%${q}%`, limit, offset);

        res.json(movies);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to search movies' });
    }
});

router.get('/genre/:genre', async (req, res) => {
    try {
        const { genre } = req.params;
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const offset = (page - 1) * limit;
        const sortBy = req.query.sortBy || 'popularity';
        const order = (req.query.order || 'DESC').toUpperCase();

        let orderBy = 'ORDER BY m.popularity DESC';
        if (sortBy === 'title') {
            orderBy = `ORDER BY m.title ${order === 'DESC' ? 'DESC' : 'ASC'}`;
        } else if (sortBy === 'rating') {
            orderBy = `ORDER BY m.rating ${order === 'ASC' ? 'ASC' : 'DESC'}`;
        }

        const movies = await db.prepare(`
            SELECT DISTINCT m.* FROM movies m
            INNER JOIN movie_genres mg ON m.movie_id = mg.movie_id
            WHERE mg.genre = ?
            ${orderBy}
            LIMIT ? OFFSET ?
        `).all(genre, limit, offset);
        res.json(movies);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch movies by genre' });
    }
});

router.get('/genres', async (req, res) => {
    try {
        const { genres } = req.query; 
        if (!genres) return res.json([]);
        
        const genreList = genres.split(',');
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const offset = (page - 1) * limit;
        const sortBy = req.query.sortBy || 'popularity';
        const order = (req.query.order || 'DESC').toUpperCase();

        let orderBy = 'ORDER BY m.popularity DESC';
        if (sortBy === 'title') {
            orderBy = `ORDER BY m.title ${order === 'DESC' ? 'DESC' : 'ASC'}`;
        } else if (sortBy === 'rating') {
            orderBy = `ORDER BY m.rating ${order === 'ASC' ? 'ASC' : 'DESC'}`;
        }

        const placeholders = genreList.map(() => '?').join(',');
        
        const movies = await db.prepare(`
            SELECT m.* FROM movies m
            WHERE m.movie_id IN (
                SELECT movie_id FROM movie_genres
                WHERE genre IN (${placeholders})
                GROUP BY movie_id
                HAVING COUNT(DISTINCT genre) = ?
            )
            ${orderBy}
            LIMIT ? OFFSET ?
        `).all(...genreList, genreList.length, limit, offset);
        res.json(movies);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch movies by multiple genres' });
    }
});

router.get('/year/:startYear/:endYear', async (req, res) => {
    try {
        const { startYear, endYear } = req.params;
        const movies = await db.prepare(`
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

router.get('/user/:userId/recent', async (req, res) => {
    try {
        const { userId } = req.params;
        const limit = req.query.limit || 20;
        const movies = await db.prepare(`
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

router.get('/:id', async (req, res) => {
    try {
        const { id } = req.params;
        const movie = await db.prepare('SELECT * FROM movies WHERE movie_id = ?').get(id);
        if (!movie) return res.status(404).json({ error: 'Movie not found' });

        const genres = await db.prepare('SELECT genre FROM movie_genres WHERE movie_id = ?').all(id);
        movie.genres = genres.map(g => g.genre);

        const ratingResult = await db.prepare('SELECT AVG(rating) as average FROM user_ratings WHERE movie_id = ?').get(id);
        movie.averageRating = ratingResult ? ratingResult.average : 0;

        res.json(movie);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch movie' });
    }
});

router.get('/:id/comments', async (req, res) => {
    try {
        const { id } = req.params;
        const comments = await db.prepare(`
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

router.post('/:id/comments', async (req, res) => {
    try {
        const { id } = req.params;
        const { userId, commentText } = req.body;
        if (!userId || !commentText) return res.status(400).json({ error: 'User ID and comment text are required' });

        await db.prepare('INSERT INTO comments (movie_id, user_id, comment_text) VALUES (?, ?, ?)').run(id, userId, commentText);
        res.json({ message: 'Comment added successfully' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to add comment' });
    }
});

router.put('/:id/comments/:commentId', async (req, res) => {
    try {
        const { commentId } = req.params;
        const { userId, commentText } = req.body;
        if (!commentText) return res.status(400).json({ error: 'Comment text is required' });

        const comment = await db.prepare('SELECT * FROM comments WHERE comment_id = ? AND user_id = ?').get(commentId, userId);
        if (!comment) return res.status(403).json({ error: 'Not authorized to edit this comment' });

        await db.prepare('UPDATE comments SET comment_text = ? WHERE comment_id = ?').run(commentText, commentId);
        res.json({ message: 'Comment updated successfully' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to update comment' });
    }
});

router.delete('/:id/comments/:commentId', async (req, res) => {
    try {
        const { commentId } = req.params;
        const { userId } = req.body;

        const comment = await db.prepare('SELECT * FROM comments WHERE comment_id = ? AND user_id = ?').get(commentId, userId);
        if (!comment) return res.status(403).json({ error: 'Not authorized to delete this comment' });

        await db.prepare('DELETE FROM comments WHERE comment_id = ?').run(commentId);
        res.json({ message: 'Comment deleted successfully' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to delete comment' });
    }
});

router.post('/:id/rating', async (req, res) => {
    try {
        const { id } = req.params;
        const { userId, rating } = req.body;
        if (!userId || rating === undefined) return res.status(400).json({ error: 'User ID and rating are required' });
        if (rating < 1 || rating > 10) return res.status(400).json({ error: 'Rating must be between 1 and 10' });

        const existing = await db.prepare('SELECT * FROM user_ratings WHERE user_id = ? AND movie_id = ?').get(userId, id);
        if (existing) {
            await db.prepare('UPDATE user_ratings SET rating = ? WHERE user_id = ? AND movie_id = ?').run(rating, userId, id);
        } else {
            await db.prepare('INSERT INTO user_ratings (user_id, movie_id, rating) VALUES (?, ?, ?)').run(userId, id, rating);
        }

        res.json({ message: 'Rating submitted successfully' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to submit rating' });
    }
});

router.get('/:id/average-rating', async (req, res) => {
    try {
        const { id } = req.params;
        const result = await db.prepare('SELECT AVG(rating) as average FROM user_ratings WHERE movie_id = ?').get(id);
        res.json({ average: result?.average || 0 });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch average rating' });
    }
});

router.post('/:id/watch', async (req, res) => {
    try {
        const { id } = req.params;
        const { userId } = req.body;
        if (!userId) return res.status(400).json({ error: 'User ID is required' });

        const existing = await db.prepare('SELECT * FROM user_watch_history WHERE user_id = ? AND movie_id = ?').get(userId, id);
        if (existing) {
            await db.prepare('UPDATE user_watch_history SET watched_at = CURRENT_TIMESTAMP WHERE user_id = ? AND movie_id = ?').run(userId, id);
        } else {
            await db.prepare('INSERT INTO user_watch_history (user_id, movie_id) VALUES (?, ?)').run(userId, id);
        }

        const history = await db.prepare('SELECT movie_id FROM user_watch_history WHERE user_id = ? ORDER BY watched_at DESC').all(userId);
        if (history.length > 10) {
            const keepIds = history.slice(0, 10).map(h => h.movie_id);
            await db.rawQuery(
                `DELETE FROM user_watch_history WHERE user_id = $1 AND movie_id <> ALL($2)`,
                [userId, keepIds]
            );
        }

        res.json({ message: 'Movie marked as watched' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to mark movie as watched' });
    }
});

export default router;
