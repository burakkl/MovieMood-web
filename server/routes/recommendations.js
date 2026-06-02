import express from 'express';
import db from '../controllers/DatabaseManager.js';

const router = express.Router();

// Generate personalized movie recommendations for a user
router.post('/generate/:userId', (req, res) => {
    const { userId } = req.params;

    try {
        // Step 1: Get user's recently watched movies (limited to last 20)
        const recentMovies = db.prepare(`
            SELECT m.*, GROUP_CONCAT(mg.genre) as genres
            FROM user_watch_history uwh
            JOIN movies m ON uwh.movie_id = m.movie_id
            LEFT JOIN movie_genres mg ON m.movie_id = mg.movie_id
            WHERE uwh.user_id = ?
            GROUP BY m.movie_id
            ORDER BY uwh.watched_at DESC
            LIMIT 20
        `).all(userId);

        if (recentMovies.length === 0) {
            return res.status(400).json({ error: 'No watch history found. Watch some movies first!' });
        }

        // Step 2: Get user's ratings to understand preferences
        const userRatings = db.prepare(`
            SELECT movie_id, rating
            FROM user_ratings
            WHERE user_id = ?
            ORDER BY rating DESC
        `).all(userId);

        // Step 3: Extract preferred genres and directors
        const genreCount = {};
        const directorCount = {};

        recentMovies.forEach(movie => {
            // Count genres
            if (movie.genres) {
                movie.genres.split(',').forEach(genre => {
                    genreCount[genre] = (genreCount[genre] || 0) + 1;
                });
            }

            // Count directors
            if (movie.director) {
                directorCount[movie.director] = (directorCount[movie.director] || 0) + 1;
            }
        });

        // Get top 3 genres and top 2 directors
        const topGenres = Object.entries(genreCount)
            .sort((a, b) => b[1] - a[1])
            .slice(0, 3)
            .map(([genre]) => genre);

        const topDirectors = Object.entries(directorCount)
            .sort((a, b) => b[1] - a[1])
            .slice(0, 2)
            .map(([director]) => director);

        // Step 4: Get watched movie IDs to exclude
        const watchedIds = recentMovies.map(m => m.movie_id);

        // Step 5: Build recommendation query
        // Prioritize: highly rated movies in preferred genres or by preferred directors
        let recommendations = [];

        // Query movies by top genres
        if (topGenres.length > 0) {
            const genrePlaceholders = topGenres.map(() => '?').join(',');
            const genreMovies = db.prepare(`
                SELECT DISTINCT m.*, 
                    (SELECT AVG(rating) FROM user_ratings WHERE movie_id = m.movie_id) as avg_rating,
                    2 as score_type
                FROM movies m
                JOIN movie_genres mg ON m.movie_id = mg.movie_id
                WHERE mg.genre IN (${genrePlaceholders})
                    AND m.movie_id NOT IN (${watchedIds.map(() => '?').join(',')})
                    AND m.rating >= 6.0
                ORDER BY m.rating DESC, avg_rating DESC
                LIMIT 15
            `).all(...topGenres, ...watchedIds);

            recommendations.push(...genreMovies);
        }

        // Query movies by top directors
        if (topDirectors.length > 0) {
            const directorPlaceholders = topDirectors.map(() => '?').join(',');
            const directorMovies = db.prepare(`
                SELECT DISTINCT m.*, 
                    (SELECT AVG(rating) FROM user_ratings WHERE movie_id = m.movie_id) as avg_rating,
                    3 as score_type
                FROM movies m
                WHERE m.director IN (${directorPlaceholders})
                    AND m.movie_id NOT IN (${watchedIds.map(() => '?').join(',')})
                    AND m.rating >= 6.5
                ORDER BY m.rating DESC, avg_rating DESC
                LIMIT 10
            `).all(...topDirectors, ...watchedIds);

            recommendations.push(...directorMovies);
        }

        // Remove duplicates and limit to 10
        const uniqueRecommendations = Array.from(
            new Map(recommendations.map(m => [m.movie_id, m])).values()
        ).slice(0, 10);

        // If we don't have enough, fill with highly rated movies
        if (uniqueRecommendations.length < 10) {
            const fillCount = 10 - uniqueRecommendations.length;
            const fillerMovies = db.prepare(`
                SELECT m.*, 
                    (SELECT AVG(rating) FROM user_ratings WHERE movie_id = m.movie_id) as avg_rating
                FROM movies m
                WHERE m.movie_id NOT IN (${watchedIds.map(() => '?').join(',')})
                    AND m.rating >= 7.0
                ORDER BY m.rating DESC
                LIMIT ?
            `).all(...watchedIds, fillCount);

            uniqueRecommendations.push(...fillerMovies);
        }

        res.json({
            recommendations: uniqueRecommendations.slice(0, 10),
            metadata: {
                topGenres,
                topDirectors,
                basedOn: recentMovies.length
            }
        });

    } catch (error) {
        console.error('Recommendation error:', error);
        res.status(500).json({ error: 'Failed to generate recommendations' });
    }
});

export default router;
