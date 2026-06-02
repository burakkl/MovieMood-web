import express from 'express';
import db from '../controllers/DatabaseManager.js';

const router = express.Router();

router.post('/generate/:userId', async (req, res) => {
    const { userId } = req.params;

    try {
        const recentMovies = await db.rawQuery(`
            SELECT m.*, STRING_AGG(mg.genre, ',') as genres
            FROM user_watch_history uwh
            JOIN movies m ON uwh.movie_id = m.movie_id
            LEFT JOIN movie_genres mg ON m.movie_id = mg.movie_id
            WHERE uwh.user_id = $1
            GROUP BY m.movie_id
            ORDER BY MAX(uwh.watched_at) DESC
            LIMIT 20
        `, [userId]);

        if (recentMovies.length === 0) {
            return res.status(400).json({ error: 'No watch history found. Watch some movies first!' });
        }

        const userRatings = await db.rawQuery(
            'SELECT movie_id, rating FROM user_ratings WHERE user_id = $1 ORDER BY rating DESC',
            [userId]
        );

        const genreCount = {};
        const directorCount = {};

        recentMovies.forEach(movie => {
            if (movie.genres) {
                movie.genres.split(',').forEach(genre => {
                    genreCount[genre] = (genreCount[genre] || 0) + 1;
                });
            }
            if (movie.director) {
                directorCount[movie.director] = (directorCount[movie.director] || 0) + 1;
            }
        });

        const topGenres = Object.entries(genreCount)
            .sort((a, b) => b[1] - a[1]).slice(0, 3).map(([genre]) => genre);
        const topDirectors = Object.entries(directorCount)
            .sort((a, b) => b[1] - a[1]).slice(0, 2).map(([director]) => director);

        const watchedIds = recentMovies.map(m => m.movie_id);
        let recommendations = [];

        if (topGenres.length > 0) {
            const genreMovies = await db.rawQuery(`
                SELECT DISTINCT m.*,
                    (SELECT AVG(rating) FROM user_ratings WHERE movie_id = m.movie_id) as avg_rating
                FROM movies m
                JOIN movie_genres mg ON m.movie_id = mg.movie_id
                WHERE mg.genre = ANY($1)
                    AND m.movie_id <> ALL($2)
                    AND m.rating >= 6.0
                ORDER BY m.rating DESC
                LIMIT 15
            `, [topGenres, watchedIds.length > 0 ? watchedIds : [0]]);
            recommendations.push(...genreMovies);
        }

        if (topDirectors.length > 0) {
            const directorMovies = await db.rawQuery(`
                SELECT DISTINCT m.*,
                    (SELECT AVG(rating) FROM user_ratings WHERE movie_id = m.movie_id) as avg_rating
                FROM movies m
                WHERE m.director = ANY($1)
                    AND m.movie_id <> ALL($2)
                    AND m.rating >= 6.5
                ORDER BY m.rating DESC
                LIMIT 10
            `, [topDirectors, watchedIds.length > 0 ? watchedIds : [0]]);
            recommendations.push(...directorMovies);
        }

        const uniqueRecommendations = Array.from(
            new Map(recommendations.map(m => [m.movie_id, m])).values()
        ).slice(0, 10);

        if (uniqueRecommendations.length < 10) {
            const fillCount = 10 - uniqueRecommendations.length;
            const fillerMovies = await db.rawQuery(`
                SELECT m.*,
                    (SELECT AVG(rating) FROM user_ratings WHERE movie_id = m.movie_id) as avg_rating
                FROM movies m
                WHERE m.movie_id <> ALL($1) AND m.rating >= 7.0
                ORDER BY m.rating DESC
                LIMIT $2
            `, [watchedIds.length > 0 ? watchedIds : [0], fillCount]);
            uniqueRecommendations.push(...fillerMovies);
        }

        res.json({
            recommendations: uniqueRecommendations.slice(0, 10),
            metadata: { topGenres, topDirectors, basedOn: recentMovies.length }
        });

    } catch (error) {
        console.error('Recommendation error:', error);
        res.status(500).json({ error: 'Failed to generate recommendations' });
    }
});

export default router;
