import express from 'express';
import db from '../controllers/DatabaseManager.js';

const router = express.Router();

router.get('/user/:userId', async (req, res) => {
    try {
        const { userId } = req.params;
        const lists = await db.prepare('SELECT * FROM user_lists WHERE user_id = ? ORDER BY created_at DESC').all(userId);

        const listsWithMovies = await Promise.all(lists.map(async list => {
            const movies = await db.prepare(`
                SELECT m.* FROM movies m
                INNER JOIN list_movies lm ON m.movie_id = lm.movie_id
                WHERE lm.list_id = ?
                ORDER BY lm.added_at DESC
            `).all(list.list_id);
            return { ...list, movies };
        }));

        res.json(listsWithMovies);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to fetch lists' });
    }
});

router.post('/', async (req, res) => {
    try {
        const { userId, listName } = req.body;
        if (!listName || !userId) return res.status(400).json({ error: 'User ID and list name are required' });

        const result = await db.prepare('INSERT INTO user_lists (user_id, list_name) VALUES (?, ?)').run(userId, listName);
        const newList = await db.prepare('SELECT * FROM user_lists WHERE list_id = ?').get(result.lastID);

        res.json({ ...newList, movies: [] });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to create list' });
    }
});

router.post('/:listId/movies', async (req, res) => {
    try {
        const { listId } = req.params;
        const { movieId } = req.body;
        if (!movieId) return res.status(400).json({ error: 'Movie ID is required' });

        const existing = await db.prepare('SELECT * FROM list_movies WHERE list_id = ? AND movie_id = ?').get(listId, movieId);
        if (existing) return res.status(400).json({ error: 'Movie already in list' });

        await db.prepare('INSERT INTO list_movies (list_id, movie_id) VALUES (?, ?)').run(listId, movieId);
        res.json({ message: 'Movie added to list' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to add movie to list' });
    }
});

router.delete('/:listId/movies/:movieId', async (req, res) => {
    try {
        const { listId, movieId } = req.params;
        await db.prepare('DELETE FROM list_movies WHERE list_id = ? AND movie_id = ?').run(listId, movieId);
        res.json({ message: 'Movie removed from list' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to remove movie from list' });
    }
});

router.delete('/:listId', async (req, res) => {
    try {
        const { listId } = req.params;
        await db.prepare('DELETE FROM user_lists WHERE list_id = ?').run(listId);
        res.json({ message: 'List deleted' });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to delete list' });
    }
});

router.put('/:listId', async (req, res) => {
    try {
        const { listId } = req.params;
        const { listName } = req.body;
        if (!listName) return res.status(400).json({ error: 'List name is required' });

        await db.prepare('UPDATE user_lists SET list_name = ? WHERE list_id = ?').run(listName, listId);
        const updatedList = await db.prepare('SELECT * FROM user_lists WHERE list_id = ?').get(listId);
        res.json(updatedList);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to rename list' });
    }
});

export default router;
