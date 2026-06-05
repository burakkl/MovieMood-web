import 'dotenv/config';
import pg from 'pg';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import fs from 'fs';

const { Pool } = pg;

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: { rejectUnauthorized: false },
  max: 10,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 5000,
});

pool.on('error', (err) => {
  console.error('Unexpected error on idle client', err);
});

// Convert SQLite ? placeholders to PostgreSQL $1, $2, ...
const convertPlaceholders = (sql) => {
  let i = 0;
  return sql.replace(/\?/g, () => `$${++i}`);
};

const isInsertOrIgnore = (sql) => /INSERT\s+OR\s+IGNORE/i.test(sql);
const isInsert = (sql) => sql.trim().toUpperCase().startsWith('INSERT');

const preparePgSQL = (sql) => {
  const wasInsertOrIgnore = isInsertOrIgnore(sql);
  let pgSQL = sql.replace(/INSERT\s+OR\s+IGNORE\s+INTO/gi, 'INSERT INTO');
  pgSQL = convertPlaceholders(pgSQL);
  return { pgSQL, wasInsertOrIgnore };
};

const flatParams = (params) => params.flat();

// ─── Schema ────────────────────────────────────────────────────────────────
const initDatabase = async () => {
  const client = await pool.connect();
  try {
    await client.query(`
      CREATE TABLE IF NOT EXISTS users (
        user_id INTEGER PRIMARY KEY,
        firstname TEXT NOT NULL,
        lastname TEXT NOT NULL,
        email TEXT UNIQUE NOT NULL,
        password_hash TEXT NOT NULL,
        profile_picture_path TEXT,
        user_code TEXT
      )
    `);

    await client.query(`
      CREATE TABLE IF NOT EXISTS movies (
        movie_id INTEGER PRIMARY KEY,
        title TEXT NOT NULL,
        original_title TEXT,
        director TEXT,
        overview TEXT,
        language TEXT,
        release_date INTEGER,
        rating FLOAT,
        vote_count INTEGER,
        poster_path TEXT,
        backdrop_path TEXT,
        popularity FLOAT,
        web_link TEXT,
        youtube_link TEXT
      )
    `);

    await client.query(`
      CREATE TABLE IF NOT EXISTS movie_genres (
        movie_id INTEGER REFERENCES movies(movie_id),
        genre TEXT,
        PRIMARY KEY(movie_id, genre)
      )
    `);

    await client.query(`
      CREATE TABLE IF NOT EXISTS comments (
        comment_id SERIAL PRIMARY KEY,
        movie_id INTEGER REFERENCES movies(movie_id),
        user_id INTEGER REFERENCES users(user_id),
        comment_text TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    await client.query(`
      CREATE TABLE IF NOT EXISTS user_ratings (
        user_id INTEGER REFERENCES users(user_id),
        movie_id INTEGER REFERENCES movies(movie_id),
        rating FLOAT,
        PRIMARY KEY(user_id, movie_id)
      )
    `);

    await client.query(`
      CREATE TABLE IF NOT EXISTS user_favorites (
        user_id INTEGER REFERENCES users(user_id),
        movie_id INTEGER REFERENCES movies(movie_id),
        PRIMARY KEY(user_id, movie_id)
      )
    `);

    await client.query(`
      CREATE TABLE IF NOT EXISTS user_friends (
        user_id INTEGER REFERENCES users(user_id),
        friend_id INTEGER REFERENCES users(user_id),
        PRIMARY KEY(user_id, friend_id)
      )
    `);

    await client.query(`
      CREATE TABLE IF NOT EXISTS friend_requests (
        request_id SERIAL PRIMARY KEY,
        sender_id INTEGER REFERENCES users(user_id),
        receiver_id INTEGER REFERENCES users(user_id),
        status TEXT DEFAULT 'pending',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE(sender_id, receiver_id)
      )
    `);

    await client.query(`
      CREATE TABLE IF NOT EXISTS user_lists (
        list_id SERIAL PRIMARY KEY,
        user_id INTEGER REFERENCES users(user_id),
        list_name TEXT NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    await client.query(`
      CREATE TABLE IF NOT EXISTS list_movies (
        list_id INTEGER REFERENCES user_lists(list_id) ON DELETE CASCADE,
        movie_id INTEGER REFERENCES movies(movie_id),
        added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY(list_id, movie_id)
      )
    `);

    await client.query(`
      CREATE TABLE IF NOT EXISTS chats (
        chat_id SERIAL PRIMARY KEY,
        user1_id INTEGER REFERENCES users(user_id),
        user2_id INTEGER REFERENCES users(user_id),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE(user1_id, user2_id)
      )
    `);

    await client.query(`
      CREATE TABLE IF NOT EXISTS messages (
        message_id SERIAL PRIMARY KEY,
        chat_id INTEGER REFERENCES chats(chat_id) ON DELETE CASCADE,
        sender_id INTEGER REFERENCES users(user_id),
        message_text TEXT NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    await client.query(`
      CREATE TABLE IF NOT EXISTS user_watch_history (
        user_id INTEGER REFERENCES users(user_id),
        movie_id INTEGER REFERENCES movies(movie_id),
        watched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY(user_id, movie_id)
      )
    `);

    console.log('✓ Database tables created successfully');
  } finally {
    client.release();
  }

  await seedMovies();
};

// ─── Seeder ────────────────────────────────────────────────────────────────
const seedMovies = async () => {
  const result = await pool.query('SELECT COUNT(*) FROM movies');
  const count = parseInt(result.rows[0].count);
  if (count > 0) {
    console.log(`✓ Movies already in database (${count} movies)`);
    return;
  }

  const moviesPath = join(__dirname, '..', '..', 'moviemood files', 'codes', 'movies_with_youtube_links.json');
  if (!fs.existsSync(moviesPath)) {
    console.log('⚠ Movies JSON not found, skipping seed');
    return;
  }

  const GENRE_MAP = {
    28: 'Action', 12: 'Adventure', 16: 'Animation', 35: 'Comedy',
    80: 'Crime', 99: 'Documentary', 18: 'Drama', 10751: 'Family',
    14: 'Fantasy', 36: 'History', 27: 'Horror', 10402: 'Music',
    9648: 'Mystery', 10749: 'Romance', 878: 'Science Fiction',
    10770: 'TV Movie', 53: 'Thriller', 10752: 'War', 37: 'Western'
  };

  const pages = JSON.parse(fs.readFileSync(moviesPath, 'utf8'));
  const client = await pool.connect();
  let inserted = 0;

  try {
    for (const page of pages) {
      for (const movie of (page.results || [])) {
        try {
          const year = movie.release_date ? parseInt(movie.release_date.substring(0, 4)) : null;
          await client.query(
            `INSERT INTO movies (movie_id, title, original_title, overview, language, release_date, rating, vote_count, poster_path, backdrop_path, popularity, web_link, youtube_link)
             VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13)
             ON CONFLICT (movie_id) DO NOTHING`,
            [movie.id, movie.title, movie.original_title, movie.overview, movie.original_language,
             year, movie.vote_average, movie.vote_count, movie.poster_path, movie.backdrop_path,
             movie.popularity, movie.imdb_link || null, movie.youtube_link || null]
          );
          for (const genreId of (movie.genre_ids || [])) {
            const genreName = GENRE_MAP[genreId];
            if (genreName) {
              await client.query(
                'INSERT INTO movie_genres (movie_id, genre) VALUES ($1, $2) ON CONFLICT DO NOTHING',
                [movie.id, genreName]
              );
            }
          }
          inserted++;
        } catch (_) { /* skip */ }
      }
    }
  } finally {
    client.release();
  }

  console.log(`✓ Seeded ${inserted} movies`);
};

// ─── DB Wrapper (same API as before, but async) ───────────────────────────
const db = {
  prepare: (sql) => {
    const { pgSQL, wasInsertOrIgnore } = preparePgSQL(sql);
    const insert = isInsert(sql);

    return {
      get: async (...params) => {
        try {
          const result = await pool.query(pgSQL, flatParams(params));
          return result.rows[0] || null;
        } catch (error) {
          console.error('Query error (get):', error.message, '\nSQL:', pgSQL);
          return null;
        }
      },
      all: async (...params) => {
        try {
          const result = await pool.query(pgSQL, flatParams(params));
          return result.rows;
        } catch (error) {
          console.error('Query error (all):', error.message, '\nSQL:', pgSQL);
          return [];
        }
      },
      run: async (...params) => {
        try {
          let finalSQL = pgSQL;
          if (wasInsertOrIgnore) finalSQL += ' ON CONFLICT DO NOTHING';
          if (insert && !wasInsertOrIgnore) finalSQL += ' RETURNING *';

          const result = await pool.query(finalSQL, flatParams(params));
          const row = result.rows[0] || {};
          const lastID = row.message_id ?? row.comment_id ?? row.list_id ??
                         row.chat_id ?? row.request_id ?? row.user_id ?? 0;
          return { changes: result.rowCount, lastID, lastRow: row };
        } catch (error) {
          console.error('Query error (run):', error.message, '\nSQL:', pgSQL);
          throw error;
        }
      }
    };
  },

  // Raw PostgreSQL query (for complex dynamic queries)
  rawQuery: async (sql, params = []) => {
    const result = await pool.query(sql, params);
    return result.rows;
  }
};

await initDatabase();

export default db;
