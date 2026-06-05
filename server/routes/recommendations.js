import express from 'express';
import db from '../controllers/DatabaseManager.js';

const router = express.Router();

// ─── Helpers ────────────────────────────────────────────────────────────────

/**
 * Build genre frequency map from favorites.
 * Simply counts how many favorites contain each genre.
 * Multi-genre films add 1 to EACH of their genres.
 */
function buildGenreFrequency(favorites) {
    const freq = {};
    favorites.forEach(movie => {
        if (movie.genres) {
            movie.genres.split(',').forEach(g => {
                const genre = g.trim();
                if (genre) freq[genre] = (freq[genre] || 0) + 1;
            });
        }
    });
    return freq;
}

/**
 * Score a candidate movie against the user's genre frequency map.
 * genre_score = sum of frequencies for each matching genre
 * quality_bonus = tmdb rating * 0.1
 */
function scoreMovie(movie, genreFreq) {
    let genreScore = 0;
    if (movie.genres) {
        movie.genres.split(',').forEach(g => {
            const genre = g.trim();
            if (genreFreq[genre]) {
                genreScore += genreFreq[genre];
            }
        });
    }
    const qualityBonus = (movie.rating || 0) * 0.1;
    return genreScore + qualityBonus;
}

/**
 * Apply diversity filter: max N movies per genre in the final list.
 */
function applyDiversity(scoredMovies, maxPerGenre = 5, targetCount = 15) {
    const result = [];
    const genreCounts = {};

    for (const movie of scoredMovies) {
        if (result.length >= targetCount) break;

        const genres = movie.genres ? movie.genres.split(',').map(g => g.trim()) : [];
        const blocked = genres.some(g => (genreCounts[g] || 0) >= maxPerGenre);

        if (!blocked) {
            result.push(movie);
            genres.forEach(g => {
                genreCounts[g] = (genreCounts[g] || 0) + 1;
            });
        }
    }

    // If diversity was too strict, fill from remaining
    if (result.length < targetCount) {
        const resultIds = new Set(result.map(m => m.movie_id));
        for (const movie of scoredMovies) {
            if (result.length >= targetCount) break;
            if (!resultIds.has(movie.movie_id)) {
                result.push(movie);
            }
        }
    }

    return result;
}

/**
 * Extract franchise/series keywords from favorite titles.
 * Strips numbers, colons, "part", "chapter" etc. to find the base title.
 * E.g. "Deadpool 2" → "Deadpool", "The Dark Knight Rises" → "The Dark Knight"
 */
function extractFranchiseKeywords(favorites) {
    const keywords = [];

    for (const movie of favorites) {
        if (!movie.title) continue;
        let base = movie.title
            .replace(/\s*[:–—-]\s*.+$/, '')          // "Spider-Man: No Way Home" → "Spider-Man"
            .replace(/\s+(Part|Chapter|Vol\.?|Volume)\s*\d*$/i, '') // "John Wick: Chapter 4" → "John Wick"
            .replace(/\s+\d+\s*$/, '')                // "Deadpool 2" → "Deadpool"
            .replace(/\s+(II|III|IV|V|VI|VII|VIII|IX|X)$/i, '') // "Rocky II" → "Rocky"
            .trim();

        // Only use if the base is different from the full title (meaning it was part of a series)
        // OR always include it to find sequels the user hasn't seen
        if (base.length >= 3) {
            keywords.push(base);
        }
    }

    // Deduplicate
    return [...new Set(keywords)];
}

// ─── Main Recommendation Endpoint ───────────────────────────────────────────

router.post('/generate/:userId', async (req, res) => {
    const { userId } = req.params;

    try {
        // ── Step 1: Check cache (24h) ──────────────────────────────────
        const cached = await db.rawQuery(`
            SELECT recommendations, generated_at
            FROM user_recommendations
            WHERE user_id = $1
              AND generated_at > NOW() - INTERVAL '24 hours'
        `, [userId]);

        if (cached.length > 0 && !req.query.force) {
            return res.json(JSON.parse(cached[0].recommendations));
        }

        // ── Step 2: Gather signals ─────────────────────────────────────

        // Primary signal: Last 15 favorites
        const favorites = await db.rawQuery(`
            SELECT m.*, STRING_AGG(DISTINCT mg.genre, ',') as genres
            FROM user_favorites uf
            JOIN movies m ON uf.movie_id = m.movie_id
            LEFT JOIN movie_genres mg ON m.movie_id = mg.movie_id
            WHERE uf.user_id = $1
            GROUP BY m.movie_id
            ORDER BY m.movie_id DESC
            LIMIT 15
        `, [userId]);

        const hasSignals = favorites.length > 0;

        // ── Step 3: Collect IDs to exclude ─────────────────────────────
        const watchedRows = await db.rawQuery(
            'SELECT movie_id FROM user_watch_history WHERE user_id = $1', [userId]
        );
        const favoriteIds = favorites.map(m => m.movie_id);
        const watchedIds = watchedRows.map(r => r.movie_id);
        const excludeIds = [...new Set([...favoriteIds, ...watchedIds])];
        const safeExclude = excludeIds.length > 0 ? excludeIds : [0];

        let result;

        if (hasSignals) {
            // ── Step 4a: Genre frequency from favorites ────────────────
            const genreFreq = buildGenreFrequency(favorites);

            const sortedGenres = Object.entries(genreFreq)
                .sort((a, b) => b[1] - a[1])
                .map(([genre]) => genre);

            const topGenres = sortedGenres.slice(0, 8);

            if (topGenres.length === 0) {
                return res.json(await fallbackRecommendations(safeExclude));
            }

            // ── Step 4b: Franchise / sequel detection ──────────────────
            const franchiseKeywords = extractFranchiseKeywords(favorites);
            let franchiseMovies = [];

            if (franchiseKeywords.length > 0) {
                // Build ILIKE conditions for each keyword
                const conditions = franchiseKeywords.map((_, i) => `m.title ILIKE $${i + 2}`);
                const params = [safeExclude, ...franchiseKeywords.map(kw => `%${kw}%`)];

                franchiseMovies = await db.rawQuery(`
                    SELECT DISTINCT m.*, STRING_AGG(DISTINCT mg.genre, ',') as genres
                    FROM movies m
                    LEFT JOIN movie_genres mg ON m.movie_id = mg.movie_id
                    WHERE (${conditions.join(' OR ')})
                      AND m.movie_id <> ALL($1)
                      AND m.rating >= 4.0
                    GROUP BY m.movie_id
                    ORDER BY m.rating DESC
                    LIMIT 20
                `, params);
            }

            // ── Step 4c: Genre-based candidates ────────────────────────
            const candidates = await db.rawQuery(`
                SELECT DISTINCT m.*, STRING_AGG(DISTINCT mg.genre, ',') as genres
                FROM movies m
                JOIN movie_genres mg ON m.movie_id = mg.movie_id
                WHERE mg.genre = ANY($1)
                  AND m.movie_id <> ALL($2)
                  AND m.rating >= 5.0
                  AND m.vote_count >= 500
                GROUP BY m.movie_id
                ORDER BY m.rating DESC
                LIMIT 200
            `, [topGenres, safeExclude]);

            // ── Step 4d: Score and merge ────────────────────────────────

            // Franchise movies get a bonus of +3 to their score
            const franchiseIds = new Set(franchiseMovies.map(m => m.movie_id));

            const allCandidates = new Map();

            // Add genre candidates
            for (const movie of candidates) {
                allCandidates.set(movie.movie_id, {
                    ...movie,
                    _score: scoreMovie(movie, genreFreq),
                    _isFranchise: false
                });
            }

            // Add/boost franchise movies
            for (const movie of franchiseMovies) {
                if (allCandidates.has(movie.movie_id)) {
                    // Already in candidates - boost score
                    allCandidates.get(movie.movie_id)._score += 3;
                    allCandidates.get(movie.movie_id)._isFranchise = true;
                } else {
                    // New candidate from franchise search
                    allCandidates.set(movie.movie_id, {
                        ...movie,
                        _score: scoreMovie(movie, genreFreq) + 3,
                        _isFranchise: true
                    });
                }
            }

            // Sort by score
            const scored = Array.from(allCandidates.values());
            scored.sort((a, b) => b._score - a._score);

            // Apply diversity filter
            const diverse = applyDiversity(scored, 5, 15);

            // Clean up internal fields
            const recommendations = diverse.map(({ _score, _isFranchise, ...movie }) => movie);

            const topGenresMeta = sortedGenres.slice(0, 5);

            result = {
                recommendations: recommendations.slice(0, 15),
                metadata: {
                    topGenres: topGenresMeta,
                    basedOn: favorites.length,
                    source: 'personalized'
                }
            };

        } else {
            // ── Step 4b: Fallback (no favorites) ───────────────────────
            result = await fallbackRecommendations(safeExclude);
        }

        // ── Step 5: Cache the result ───────────────────────────────────
        await db.rawQuery(`
            INSERT INTO user_recommendations (user_id, recommendations, generated_at)
            VALUES ($1, $2, NOW())
            ON CONFLICT (user_id)
            DO UPDATE SET recommendations = $2, generated_at = NOW()
        `, [userId, JSON.stringify(result)]);

        res.json(result);

    } catch (error) {
        console.error('Recommendation error:', error);
        res.status(500).json({ error: 'Failed to generate recommendations' });
    }
});

// ─── GET cached recommendations (for Home "For You" section) ────────────────

router.get('/for-you/:userId', async (req, res) => {
    const { userId } = req.params;

    try {
        const cached = await db.rawQuery(`
            SELECT recommendations, generated_at
            FROM user_recommendations
            WHERE user_id = $1
              AND generated_at > NOW() - INTERVAL '24 hours'
        `, [userId]);

        if (cached.length > 0) {
            return res.json(JSON.parse(cached[0].recommendations));
        }

        return res.json({ recommendations: [], metadata: { source: 'empty' } });

    } catch (error) {
        console.error('For-You fetch error:', error);
        res.status(500).json({ error: 'Failed to fetch recommendations' });
    }
});

// ─── Fallback: popular, well-voted movies ───────────────────────────────────

async function fallbackRecommendations(excludeIds) {
    const fillers = await db.rawQuery(`
        SELECT m.*, STRING_AGG(DISTINCT mg.genre, ',') as genres
        FROM movies m
        LEFT JOIN movie_genres mg ON m.movie_id = mg.movie_id
        WHERE m.movie_id <> ALL($1)
          AND m.rating >= 7.5
          AND m.vote_count >= 100000
        GROUP BY m.movie_id
        ORDER BY m.rating DESC, m.vote_count DESC
        LIMIT 15
    `, [excludeIds]);

    return {
        recommendations: fillers,
        metadata: {
            topGenres: [],
            basedOn: 0,
            source: 'popular'
        }
    };
}

export default router;
