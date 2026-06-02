import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:moviemood.db";
    private static Connection connection = null;

    // Database bağlantısını başlat
    public static void initialize() {
        try {
            // SQLite JDBC driver'ı yükle
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("Database connection successful!");
            createTables();
        } catch (Exception e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Bağlantıyı al
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    // Tabloları oluştur
    private static void createTables() {
        try {
            Statement stmt = connection.createStatement();

            // Users tablosu
            String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INTEGER PRIMARY KEY," +
                    "firstname TEXT NOT NULL," +
                    "lastname TEXT NOT NULL," +
                    "email TEXT UNIQUE NOT NULL," +
                    "password_hash TEXT NOT NULL," +
                    "profile_picture_path TEXT" +
                    ")";
            stmt.execute(createUsersTable);

            // Movies tablosu
            String createMoviesTable = "CREATE TABLE IF NOT EXISTS movies (" +
                    "movie_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT NOT NULL," +
                    "original_title TEXT," +
                    "director TEXT," +
                    "overview TEXT," +
                    "language TEXT," +
                    "release_date INTEGER," +
                    "rating REAL," +
                    "vote_count INTEGER," +
                    "poster_path TEXT," +
                    "backdrop_path TEXT," +
                    "popularity REAL," +
                    "web_link TEXT," +
                    "youtube_link TEXT" +
                    ")";
            stmt.execute(createMoviesTable);

            // Movie Genres (many-to-many ilişki)
            String createGenresTable = "CREATE TABLE IF NOT EXISTS movie_genres (" +
                    "movie_id INTEGER," +
                    "genre TEXT," +
                    "FOREIGN KEY(movie_id) REFERENCES movies(movie_id)," +
                    "PRIMARY KEY(movie_id, genre)" +
                    ")";
            stmt.execute(createGenresTable);

            // Movie Actors (many-to-many ilişki)
            String createActorsTable = "CREATE TABLE IF NOT EXISTS movie_actors (" +
                    "movie_id INTEGER," +
                    "actor TEXT," +
                    "FOREIGN KEY(movie_id) REFERENCES movies(movie_id)," +
                    "PRIMARY KEY(movie_id, actor)" +
                    ")";
            stmt.execute(createActorsTable);

            // Film Lists
            String createFilmListsTable = "CREATE TABLE IF NOT EXISTS film_lists (" +
                    "list_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "list_name TEXT NOT NULL," +
                    "FOREIGN KEY(user_id) REFERENCES users(user_id)" +
                    ")";
            stmt.execute(createFilmListsTable);

            // Film List Movies (many-to-many)
            String createListMoviesTable = "CREATE TABLE IF NOT EXISTS film_list_movies (" +
                    "list_id INTEGER," +
                    "movie_id INTEGER," +
                    "FOREIGN KEY(list_id) REFERENCES film_lists(list_id)," +
                    "FOREIGN KEY(movie_id) REFERENCES movies(movie_id)," +
                    "PRIMARY KEY(list_id, movie_id)" +
                    ")";
            stmt.execute(createListMoviesTable);

            // User Friends (many-to-many)
            String createFriendsTable = "CREATE TABLE IF NOT EXISTS user_friends (" +
                    "user_id INTEGER," +
                    "friend_id INTEGER," +
                    "FOREIGN KEY(user_id) REFERENCES users(user_id)," +
                    "FOREIGN KEY(friend_id) REFERENCES users(user_id)," +
                    "PRIMARY KEY(user_id, friend_id)" +
                    ")";
            stmt.execute(createFriendsTable);

            // Recently Watched
            String createRecentlyWatchedTable = "CREATE TABLE IF NOT EXISTS recently_watched (" +
                    "user_id INTEGER," +
                    "movie_id INTEGER," +
                    "watched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY(user_id) REFERENCES users(user_id)," +
                    "FOREIGN KEY(movie_id) REFERENCES movies(movie_id)," +
                    "PRIMARY KEY(user_id, movie_id, watched_at)" +
                    ")";
            stmt.execute(createRecentlyWatchedTable);

            // User Favorites
            String createFavoritesTable = "CREATE TABLE IF NOT EXISTS user_favorites (" +
                    "user_id INTEGER," +
                    "movie_id INTEGER," +
                    "FOREIGN KEY(user_id) REFERENCES users(user_id)," +
                    "FOREIGN KEY(movie_id) REFERENCES movies(movie_id)," +
                    "PRIMARY KEY(user_id, movie_id)" +
                    ")";
            stmt.execute(createFavoritesTable);

            // User Ratings
            String createRatingsTable = "CREATE TABLE IF NOT EXISTS user_ratings (" +
                    "user_id INTEGER," +
                    "movie_id INTEGER," +
                    "rating REAL," +
                    "FOREIGN KEY(user_id) REFERENCES users(user_id)," +
                    "FOREIGN KEY(movie_id) REFERENCES movies(movie_id)," +
                    "PRIMARY KEY(user_id, movie_id)" +
                    ")";
            stmt.execute(createRatingsTable);

            // Comments
            String createCommentsTable = "CREATE TABLE IF NOT EXISTS comments (" +
                    "comment_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "movie_id INTEGER," +
                    "user_id INTEGER," +
                    "comment_text TEXT," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY(movie_id) REFERENCES movies(movie_id)," +
                    "FOREIGN KEY(user_id) REFERENCES users(user_id)" +
                    ")";
            stmt.execute(createCommentsTable);

            // Chats
            String createChatsTable = "CREATE TABLE IF NOT EXISTS chats (" +
                    "chat_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user1_id INTEGER," +
                    "user2_id INTEGER," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY(user1_id) REFERENCES users(user_id)," +
                    "FOREIGN KEY(user2_id) REFERENCES users(user_id)" +
                    ")";
            stmt.execute(createChatsTable);

            // Messages
            String createMessagesTable = "CREATE TABLE IF NOT EXISTS messages (" +
                    "message_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "chat_id INTEGER," +
                    "sender_id INTEGER," +
                    "message_text TEXT," +
                    "sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY(chat_id) REFERENCES chats(chat_id)," +
                    "FOREIGN KEY(sender_id) REFERENCES users(user_id)" +
                    ")";
            stmt.execute(createMessagesTable);

            // Friend Requests
            String createFriendRequestsTable = "CREATE TABLE IF NOT EXISTS friend_requests (" +
                    "request_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "sender_id INTEGER," +
                    "receiver_id INTEGER," +
                    "status TEXT DEFAULT 'pending'," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY(sender_id) REFERENCES users(user_id)," +
                    "FOREIGN KEY(receiver_id) REFERENCES users(user_id)," +
                    "UNIQUE(sender_id, receiver_id)" +
                    ")";
            stmt.execute(createFriendRequestsTable);

            stmt.close();
            System.out.println("Tables created successfully!");
        } catch (SQLException e) {
            System.err.println("Table creation error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Bağlantıyı kapat
    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Database'i tamamen temizle (test için)
    public static void clearDatabase() {
        try {
            Statement stmt = connection.createStatement();
            stmt.execute("DELETE FROM messages");
            stmt.execute("DELETE FROM chats");
            stmt.execute("DELETE FROM comments");
            stmt.execute("DELETE FROM user_ratings");
            stmt.execute("DELETE FROM user_favorites");
            stmt.execute("DELETE FROM recently_watched");
            stmt.execute("DELETE FROM user_friends");
            stmt.execute("DELETE FROM film_list_movies");
            stmt.execute("DELETE FROM film_lists");
            stmt.execute("DELETE FROM movie_actors");
            stmt.execute("DELETE FROM movie_genres");
            stmt.execute("DELETE FROM movies");
            stmt.execute("DELETE FROM users");
            stmt.close();
            System.out.println("Database cleared!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
