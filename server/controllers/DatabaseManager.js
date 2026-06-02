import initSqlJs from 'sql.js';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import fs from 'fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// Database path
const dbPath = join(__dirname, '..', '..', 'moviemood.db');

// Initialize database
let db;
let SQL;

const initDatabase = async () => {
  SQL = await initSqlJs();

  // Check if database file exists
  if (fs.existsSync(dbPath)) {
    const buffer = fs.readFileSync(dbPath);
    db = new SQL.Database(buffer);
    console.log('✓ Database loaded from existing file');
  } else {
    db = new SQL.Database();
    console.log('✓ New database created');
  }

  createTables();
};

// Create tables
const createTables = () => {
  // Users table
  db.run(`
    CREATE TABLE IF NOT EXISTS users (
      user_id INTEGER PRIMARY KEY AUTOINCREMENT,
      firstname TEXT NOT NULL,
      lastname TEXT NOT NULL,
      email TEXT UNIQUE NOT NULL,
      password_hash TEXT NOT NULL,
      profile_picture_path TEXT,
      user_code TEXT
    )
  `);

  // Migration: Add user_code column if it doesn't exist
  try {
    const result = db.exec("PRAGMA table_info(users)");
    // result[0].values is array of arrays [cid, name, type, notnull, dflt_value, pk]
    // name is the second element (index 1)
    const columns = result.length > 0 ? result[0].values.map(row => row[1]) : [];

    const hasUserCode = columns.includes('user_code');
    if (!hasUserCode) {
      console.log('Adding user_code column to users table...');
      db.run('ALTER TABLE users ADD COLUMN user_code TEXT');

      // Generate codes for existing users
      const usersResult = db.exec('SELECT user_id FROM users');
      if (usersResult.length > 0) {
        const userIds = usersResult[0].values.map(row => row[0]);

        userIds.forEach(userId => {
          const code = Math.floor(1000 + Math.random() * 9000).toString();
          db.run('UPDATE users SET user_code = ? WHERE user_id = ?', [code, userId]);
        });
        console.log(`Updated ${userIds.length} existing users with user codes`);
      }
    }
  } catch (error) {
    console.error('Migration error:', error);
  }

  // Movies table
  db.run(`
    CREATE TABLE IF NOT EXISTS movies (
      movie_id INTEGER PRIMARY KEY AUTOINCREMENT,
      title TEXT NOT NULL,
      original_title TEXT,
      director TEXT,
      overview TEXT,
      language TEXT,
      release_date INTEGER,
      rating REAL,
      vote_count INTEGER,
      poster_path TEXT,
      backdrop_path TEXT,
      popularity REAL,
      web_link TEXT,
      youtube_link TEXT
    )
  `);

  // Movie genres
  db.run(`
    CREATE TABLE IF NOT EXISTS movie_genres (
      movie_id INTEGER,
      genre TEXT,
      FOREIGN KEY(movie_id) REFERENCES movies(movie_id),
      PRIMARY KEY(movie_id, genre)
    )
  `);

  // Comments
  db.run(`
    CREATE TABLE IF NOT EXISTS comments (
      comment_id INTEGER PRIMARY KEY AUTOINCREMENT,
      movie_id INTEGER,
      user_id INTEGER,
      comment_text TEXT,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY(movie_id) REFERENCES movies(movie_id),
      FOREIGN KEY(user_id) REFERENCES users(user_id)
    )
  `);

  // User ratings
  db.run(`
    CREATE TABLE IF NOT EXISTS user_ratings (
      user_id INTEGER,
      movie_id INTEGER,
      rating REAL,
      FOREIGN KEY(user_id) REFERENCES users(user_id),
      FOREIGN KEY(movie_id) REFERENCES movies(movie_id),
      PRIMARY KEY(user_id, movie_id)
    )
  `);

  // User favorites
  db.run(`
    CREATE TABLE IF NOT EXISTS user_favorites (
      user_id INTEGER,
      movie_id INTEGER,
      FOREIGN KEY(user_id) REFERENCES users(user_id),
      FOREIGN KEY(movie_id) REFERENCES movies(movie_id),
      PRIMARY KEY(user_id, movie_id)
    )
  `);

  // User friends
  db.run(`
    CREATE TABLE IF NOT EXISTS user_friends (
      user_id INTEGER,
      friend_id INTEGER,
      FOREIGN KEY(user_id) REFERENCES users(user_id),
      FOREIGN KEY(friend_id) REFERENCES users(user_id),
      PRIMARY KEY(user_id, friend_id)
    )
  `);

  // Friend requests
  db.run(`
    CREATE TABLE IF NOT EXISTS friend_requests (
      request_id INTEGER PRIMARY KEY AUTOINCREMENT,
      sender_id INTEGER,
      receiver_id INTEGER,
      status TEXT DEFAULT 'pending',
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY(sender_id) REFERENCES users(user_id),
      FOREIGN KEY(receiver_id) REFERENCES users(user_id),
      UNIQUE(sender_id, receiver_id)
    )
  `);

  // User lists (custom movie lists)
  db.run(`
    CREATE TABLE IF NOT EXISTS user_lists (
      list_id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id INTEGER,
      list_name TEXT NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY(user_id) REFERENCES users(user_id)
    )
  `);

  // List movies (many-to-many relationship)
  db.run(`
    CREATE TABLE IF NOT EXISTS list_movies (
      list_id INTEGER,
      movie_id INTEGER,
      added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY(list_id) REFERENCES user_lists(list_id) ON DELETE CASCADE,
      FOREIGN KEY(movie_id) REFERENCES movies(movie_id),
      PRIMARY KEY(list_id, movie_id)
    )
  `);

  // Chats
  db.run(`
    CREATE TABLE IF NOT EXISTS chats (
      chat_id INTEGER PRIMARY KEY AUTOINCREMENT,
      user1_id INTEGER,
      user2_id INTEGER,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY(user1_id) REFERENCES users(user_id),
      FOREIGN KEY(user2_id) REFERENCES users(user_id),
      UNIQUE(user1_id, user2_id)
    )
  `);

  // Messages
  db.run(`
    CREATE TABLE IF NOT EXISTS messages (
      message_id INTEGER PRIMARY KEY AUTOINCREMENT,
      chat_id INTEGER,
      sender_id INTEGER,
      message_text TEXT NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY(chat_id) REFERENCES chats(chat_id) ON DELETE CASCADE,
      FOREIGN KEY(sender_id) REFERENCES users(user_id)
    )
  `);

  // User watch history (recently watched movies)
  db.run(`
    CREATE TABLE IF NOT EXISTS user_watch_history (
      user_id INTEGER,
      movie_id INTEGER,
      watched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY(user_id) REFERENCES users(user_id),
      FOREIGN KEY(movie_id) REFERENCES movies(movie_id),
      PRIMARY KEY(user_id, movie_id)
    )
  `);

  console.log('✓ Database tables created successfully');

  // Save database to file
  saveDatabase();
};

// Save database to file
const saveDatabase = () => {
  if (db) {
    const data = db.export();
    const buffer = Buffer.from(data);
    fs.writeFileSync(dbPath, buffer);
  }
};

// Database wrapper to match better-sqlite3 API
const dbWrapper = {
  prepare: (sql) => {
    return {
      get: (...params) => {
        try {
          const stmt = db.prepare(sql);
          if (params.length > 0) {
            stmt.bind(params);
          }
          const result = stmt.step() ? stmt.getAsObject() : null;
          stmt.free();
          return result;
        } catch (error) {
          console.error('Query error (get):', error, 'SQL:', sql);
          return null;
        }
      },
      all: (...params) => {
        try {
          const stmt = db.prepare(sql);
          if (params.length > 0) {
            stmt.bind(params);
          }
          const rows = [];
          while (stmt.step()) {
            rows.push(stmt.getAsObject());
          }
          stmt.free();
          return rows;
        } catch (error) {
          console.error('Query error (all):', error, 'SQL:', sql);
          return [];
        }
      },
      run: (...params) => {
        try {
          const stmt = db.prepare(sql);
          if (params.length > 0) {
            stmt.bind(params);
          }
          stmt.step();
          const lastID = db.exec("SELECT last_insert_rowid() as id")[0]?.values?.[0]?.[0] || 0;
          stmt.free();
          saveDatabase();
          return {
            changes: db.getRowsModified(),
            lastID: lastID
          };
        } catch (error) {
          console.error('Query error (run):', error, 'SQL:', sql);
          throw error;
        }
      }
    };
  },
  exec: (sql) => {
    try {
      db.exec(sql);
      saveDatabase();
    } catch (error) {
      console.error('Exec error:', error);
      throw error;
    }
  }
};

// Initialize on import
await initDatabase();

export default dbWrapper;
