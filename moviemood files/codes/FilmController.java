import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FilmController {
    
    // Film ekle
    public static int addMovie(Movie movie) {
        try {
            Connection conn = DatabaseManager.getConnection();
            
            // Önce filmin zaten var olup olmadığını kontrol et
            String checkSql = "SELECT movie_id FROM movies WHERE title = ? AND release_date = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, movie.getTitle());
            checkStmt.setInt(2, movie.getReleaseDate());
            ResultSet checkRs = checkStmt.executeQuery();
            
            if (checkRs.next()) {
                int existingId = checkRs.getInt("movie_id");
                checkRs.close();
                checkStmt.close();
                return existingId; // Film zaten var, ID'yi döndür
            }
            checkRs.close();
            checkStmt.close();
            
            // Film yoksa ekle
            String sql = "INSERT INTO movies (title, original_title, director, overview, language, " +
                        "release_date, rating, vote_count, poster_path, backdrop_path, popularity, " +
                        "web_link, youtube_link) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            Map<String, Object> movieData = movie.getMovie();
            
            pstmt.setString(1, movie.getTitle());
            pstmt.setString(2, (String) movieData.get("originalTitle"));
            pstmt.setString(3, movie.getDirector());
            pstmt.setString(4, movie.getOverview());
            pstmt.setString(5, (String) movieData.get("language"));
            pstmt.setInt(6, movie.getReleaseDate());
            pstmt.setDouble(7, movie.getImdbRating());
            pstmt.setInt(8, (int) movieData.get("voteCount"));
            pstmt.setString(9, (String) movieData.get("posterPath"));
            pstmt.setString(10, (String) movieData.get("backdropPath"));
            pstmt.setDouble(11, (double) movieData.get("popularity"));
            pstmt.setString(12, movie.getWebLink());
            pstmt.setString(13, movie.getYoutubeLink());
            
            pstmt.executeUpdate();
            
            // Oluşturulan filmin ID'sini al
            ResultSet rs = pstmt.getGeneratedKeys();
            int movieId = -1;
            if (rs.next()) {
                movieId = rs.getInt(1);
            }
            rs.close();
            pstmt.close();
            
            if (movieId != -1) {
                // Genres ekle
                addMovieGenres(movieId, movie.getGenres());
                
                // Actors ekle
                @SuppressWarnings("unchecked")
                List<String> actors = (List<String>) movieData.get("actors");
                if (actors != null) {
                    addMovieActors(movieId, actors);
                }
            }
            
            return movieId;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    // Film türlerini ekle
    private static void addMovieGenres(int movieId, List<String> genres) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "INSERT OR IGNORE INTO movie_genres (movie_id, genre) VALUES (?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            
            for (String genre : genres) {
                pstmt.setInt(1, movieId);
                pstmt.setString(2, genre);
                pstmt.executeUpdate();
            }
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Film oyuncularını ekle
    private static void addMovieActors(int movieId, List<String> actors) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "INSERT OR IGNORE INTO movie_actors (movie_id, actor) VALUES (?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            
            for (String actor : actors) {
                pstmt.setInt(1, movieId);
                pstmt.setString(2, actor);
                pstmt.executeUpdate();
            }
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Tüm filmleri getir
    public static List<Movie> getAllMovies() {
        List<Movie> movies = new ArrayList<>();
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM movies";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                Movie movie = createMovieFromResultSet(rs);
                movies.add(movie);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return movies;
    }

    // ID ile film getir
    public static Movie getMovieById(int movieId) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM movies WHERE movie_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, movieId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Movie movie = createMovieFromResultSet(rs);
                rs.close();
                pstmt.close();
                return movie;
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Türe göre ara
    public static List<Movie> searchByGenre(String genre) {
        List<Movie> movies = new ArrayList<>();
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT DISTINCT m.* FROM movies m " +
                        "INNER JOIN movie_genres mg ON m.movie_id = mg.movie_id " +
                        "WHERE mg.genre = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, genre);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Movie movie = createMovieFromResultSet(rs);
                movies.add(movie);
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return movies;
    }

    // Başlığa göre ara
    public static List<Movie> searchByTitle(String query) {
        List<Movie> movies = new ArrayList<>();
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM movies WHERE LOWER(title) LIKE ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, "%" + query.toLowerCase() + "%");
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Movie movie = createMovieFromResultSet(rs);
                movies.add(movie);
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return movies;
    }

    // Yıl aralığına göre ara
    public static List<Movie> searchByReleaseYearInterval(int startYear, int endYear) {
        List<Movie> movies = new ArrayList<>();
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM movies WHERE release_date BETWEEN ? AND ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, startYear);
            pstmt.setInt(2, endYear);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Movie movie = createMovieFromResultSet(rs);
                movies.add(movie);
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return movies;
    }

    // Kullanıcının favorilerini getir
    public static List<Movie> getFavorites(User user) {
        return new ArrayList<>(user.getFavoriteMovies());
    }

    // Başlığa göre film bilgisi getir
    public static Map<String, Object> getMovieByTitle(String title) {
        List<Movie> movies = searchByTitle(title);
        if (!movies.isEmpty()) {
            return movies.get(0).getMovie();
        }
        return null;
    }

    // Filme yorum ekle
    public static void addCommentToMovie(Movie movie, User user, String text) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "INSERT INTO comments (movie_id, user_id, comment_text) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            
            // Movie ID'yi al (Movie nesnesine ID eklememiz gerekecek)
            int movieId = getMovieIdByTitle(movie.getTitle());
            
            pstmt.setInt(1, movieId);
            pstmt.setInt(2, user.getUserId());
            pstmt.setString(3, text);
            pstmt.executeUpdate();
            pstmt.close();
            
            // Memory'deki movie nesnesine de ekle
            Comment comment = new Comment(user, text);
            movie.addComment(comment);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Film başlığından ID getir
    private static int getMovieIdByTitle(String title) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT movie_id FROM movies WHERE title = ? LIMIT 1";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, title);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int id = rs.getInt("movie_id");
                rs.close();
                pstmt.close();
                return id;
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Yorum sil
    public static boolean deleteComment(Movie movie, User user, String text) {
        try {
            Connection conn = DatabaseManager.getConnection();
            int movieId = getMovieIdByTitle(movie.getTitle());
            
            String sql = "DELETE FROM comments WHERE movie_id = ? AND user_id = ? AND comment_text = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, movieId);
            pstmt.setInt(2, user.getUserId());
            pstmt.setString(3, text);
            
            int deleted = pstmt.executeUpdate();
            pstmt.close();
            
            if (deleted > 0) {
                // Memory'den de sil
                List<Comment> comments = movie.getComments();
                for (int i = 0; i < comments.size(); i++) {
                    Comment c = comments.get(i);
                    if (c.getAuthor().equals(user) && c.getText().equals(text)) {
                        comments.remove(i);
                        return true;
                    }
                }
            }
            return deleted > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Kullanıcının tüm yorumlarını getir
    public static List<Comment> getUserComments(User user) {
        List<Comment> comments = new ArrayList<>();
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM comments WHERE user_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, user.getUserId());
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String text = rs.getString("comment_text");
                Comment comment = new Comment(user, text);
                comments.add(comment);
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return comments;
    }

    // Filme puan ver
    public static void rateMovie(Movie movie, User user, double rating) {
        try {
            Connection conn = DatabaseManager.getConnection();
            int movieId = getMovieIdByTitle(movie.getTitle());
            
            String sql = "INSERT OR REPLACE INTO user_ratings (user_id, movie_id, rating) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, user.getUserId());
            pstmt.setInt(2, movieId);
            pstmt.setDouble(3, rating);
            pstmt.executeUpdate();
            pstmt.close();
            
            // Memory'de de güncelle
            movie.rateMovie(user, rating);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Filmin yorumlarını getir
    public static List<Comment> getCommentsForMovie(Movie movie) {
        return movie.getComments();
    }

    // Ortalama puanı getir
    public static double getAverageRating(Movie movie) {
        return movie.getAverageRating();
    }

    // Kullanıcının verdiği puanı getir
    public static Double getUserRating(Movie movie, User user) {
        return movie.getRatingByUser(user);
    }

    // Yorum düzenle
    public static boolean editComment(Movie movie, User user, String oldText, String newText) {
        try {
            Connection conn = DatabaseManager.getConnection();
            int movieId = getMovieIdByTitle(movie.getTitle());
            
            String sql = "UPDATE comments SET comment_text = ? WHERE movie_id = ? AND user_id = ? AND comment_text = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newText);
            pstmt.setInt(2, movieId);
            pstmt.setInt(3, user.getUserId());
            pstmt.setString(4, oldText);
            
            int updated = pstmt.executeUpdate();
            pstmt.close();
            
            if (updated > 0) {
                // Memory'de de güncelle
                for (Comment c : movie.getComments()) {
                    if (c.getAuthor().equals(user) && c.getText().equals(oldText)) {
                        c.setText(newText);
                        return true;
                    }
                }
            }
            return updated > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Kullanıcı için önerilen filmler
    public static List<Movie> getRecommendedMovies(User user) {
        user.setRecommendedMovies();
        return user.getRecommendedMovies();
    }

    // Önerilen film listesi oluştur
    public FilmList createRecommendedMovieList(User user, String listName) {
        try {
            RecommendationEngine.initializeMovies();
            List<Movie> recommendedMovies = RecommendationEngine.recommendMovies(user);
            
            FilmListController.createList(user, listName);
            FilmList recommendedList = FilmListController.getFilmListByName(user, listName);
            
            if (recommendedList != null) {
                for (Movie movie : recommendedMovies) {
                    FilmListController.addMovieToList(recommendedList, movie);
                }
                return FilmListController.getFilmListByName(user, listName);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ResultSet'ten Movie oluştur
    private static Movie createMovieFromResultSet(ResultSet rs) throws SQLException {
        String title = rs.getString("title");
        String originalTitle = rs.getString("original_title");
        String director = rs.getString("director");
        String overview = rs.getString("overview");
        String language = rs.getString("language");
        int releaseDate = rs.getInt("release_date");
        double rating = rs.getDouble("rating");
        int voteCount = rs.getInt("vote_count");
        String posterPath = rs.getString("poster_path");
        String backdropPath = rs.getString("backdrop_path");
        double popularity = rs.getDouble("popularity");
        String webLink = rs.getString("web_link");
        String youtubeLink = rs.getString("youtube_link");
        
        // Genres ve actors'ı yükle
        int movieId = rs.getInt("movie_id");
        List<String> genres = loadMovieGenres(movieId);
        List<String> actors = loadMovieActors(movieId);
        
        Movie movie = new Movie(title, genres, director, actors, releaseDate,
                               originalTitle, overview, language, String.valueOf(releaseDate),
                               rating, voteCount, posterPath, backdropPath, popularity,
                               webLink, youtubeLink);
        
        // Yorumları ve puanlamaları yükle
        loadMovieComments(movieId, movie);
        loadMovieRatings(movieId, movie);
        
        return movie;
    }

    // Film türlerini yükle
    private static List<String> loadMovieGenres(int movieId) {
        List<String> genres = new ArrayList<>();
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT genre FROM movie_genres WHERE movie_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, movieId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                genres.add(rs.getString("genre"));
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return genres;
    }

    // Film oyuncularını yükle
    private static List<String> loadMovieActors(int movieId) {
        List<String> actors = new ArrayList<>();
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT actor FROM movie_actors WHERE movie_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, movieId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                actors.add(rs.getString("actor"));
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return actors;
    }

    public static Comment getLatestUserComment(Movie movie, User user) {
        Comment latest = null;
        for (Comment comment : movie.getComments()) {
            if (comment.getAuthor().equals(user)) {
                if (latest == null || comment.getTimestamp().isAfter(latest.getTimestamp())) {
                    latest = comment;
                }
            }
        }
        return latest;
    }

    // Film yorumlarını database'den yükle
    private static void loadMovieComments(int movieId, Movie movie) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT c.comment_text, c.created_at, u.* FROM comments c " +
                        "INNER JOIN users u ON c.user_id = u.user_id " +
                        "WHERE c.movie_id = ? ORDER BY c.created_at DESC";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, movieId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String commentText = rs.getString("comment_text");
                int userId = rs.getInt("user_id");
                String firstname = rs.getString("firstname");
                String lastname = rs.getString("lastname");
                String email = rs.getString("email");
                String passwordHash = rs.getString("password_hash");
                String profilePicturePath = rs.getString("profile_picture_path");
                
                // User nesnesini oluştur (basit versiyon, tam yükleme yapmıyoruz)
                User user = new User(firstname, lastname, email, passwordHash, userId);
                user.setProfilePicturePath(profilePicturePath);
                
                // Comment oluştur ve filme ekle
                Comment comment = new Comment(user, commentText);
                movie.addComment(comment);
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Film puanlamalarını database'den yükle
    private static void loadMovieRatings(int movieId, Movie movie) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT r.rating, u.* FROM user_ratings r " +
                        "INNER JOIN users u ON r.user_id = u.user_id " +
                        "WHERE r.movie_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, movieId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                double rating = rs.getDouble("rating");
                int userId = rs.getInt("user_id");
                String firstname = rs.getString("firstname");
                String lastname = rs.getString("lastname");
                String email = rs.getString("email");
                String passwordHash = rs.getString("password_hash");
                String profilePicturePath = rs.getString("profile_picture_path");
                
                // User nesnesini oluştur
                User user = new User(firstname, lastname, email, passwordHash, userId);
                user.setProfilePicturePath(profilePicturePath);
                
                // Puanı filme ekle
                movie.rateMovie(user, rating);
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}