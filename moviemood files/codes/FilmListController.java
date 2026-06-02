import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FilmListController {
    
    // Liste oluştur
    public static boolean createList(User user, String listName) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "INSERT INTO film_lists (user_id, list_name) VALUES (?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, user.getUserId());
            pstmt.setString(2, listName);
            pstmt.executeUpdate();
            pstmt.close();
            
            // Memory'ye de ekle
            user.addFilmList(new FilmList(listName));
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Listeye film ekle
    public static boolean addMovieToList(FilmList list, Movie movie) {
        try {
            // Liste ID'sini al
            int listId = getListId(list);
            if (listId == -1) return false;
            
            // Film ID'sini al
            int movieId = getMovieIdByTitle(movie.getTitle());
            if (movieId == -1) return false;
            
            Connection conn = DatabaseManager.getConnection();
            String sql = "INSERT OR IGNORE INTO film_list_movies (list_id, movie_id) VALUES (?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, listId);
            pstmt.setInt(2, movieId);
            pstmt.executeUpdate();
            pstmt.close();
            
            // Memory'ye de ekle
            if (!list.getMovies().contains(movie)) {
                list.addMovie(movie);
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Listeden film çıkar
    public static boolean removeMovieFromList(FilmList list, Movie movie) {
        try {
            int listId = getListId(list);
            if (listId == -1) return false;
            
            int movieId = getMovieIdByTitle(movie.getTitle());
            if (movieId == -1) return false;
            
            Connection conn = DatabaseManager.getConnection();
            String sql = "DELETE FROM film_list_movies WHERE list_id = ? AND movie_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, listId);
            pstmt.setInt(2, movieId);
            pstmt.executeUpdate();
            pstmt.close();
            
            // Memory'den de çıkar
            list.removeMovie(movie);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Listeyi sil
    public static boolean removeFilmList(User user, String listName) {
        try {
            Connection conn = DatabaseManager.getConnection();
            
            // Önce liste ID'sini al
            String getIdSql = "SELECT list_id FROM film_lists WHERE user_id = ? AND list_name = ?";
            PreparedStatement getIdStmt = conn.prepareStatement(getIdSql);
            getIdStmt.setInt(1, user.getUserId());
            getIdStmt.setString(2, listName);
            ResultSet rs = getIdStmt.executeQuery();
            
            if (rs.next()) {
                int listId = rs.getInt("list_id");
                rs.close();
                getIdStmt.close();
                
                // Listeden filmleri sil
                String deleteMoviesSql = "DELETE FROM film_list_movies WHERE list_id = ?";
                PreparedStatement deleteMoviesStmt = conn.prepareStatement(deleteMoviesSql);
                deleteMoviesStmt.setInt(1, listId);
                deleteMoviesStmt.executeUpdate();
                deleteMoviesStmt.close();
                
                // Listeyi sil
                String deleteListSql = "DELETE FROM film_lists WHERE list_id = ?";
                PreparedStatement deleteListStmt = conn.prepareStatement(deleteListSql);
                deleteListStmt.setInt(1, listId);
                deleteListStmt.executeUpdate();
                deleteListStmt.close();
                
                // Memory'den de sil
                FilmList target = null;
                for (FilmList list : user.getFilmLists()) {
                    if (list.getName().equalsIgnoreCase(listName)) {
                        target = list;
                        break;
                    }
                }
                if (target != null) {
                    user.getFilmLists().remove(target);
                }
                
                return true;
            }
            rs.close();
            getIdStmt.close();
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Filmi içeren listeleri getir
    public static List<FilmList> getListsWithMovie(User user, Movie movie) {
        List<FilmList> result = new ArrayList<>();
        for (FilmList list : user.getFilmLists()) {
            if (list.getMovies().contains(movie)) {
                result.add(list);
            }
        }
        return result;
    }

    // Kullanıcının tüm listelerini getir
    public static List<FilmList> getAllFilmLists(User user) {
        return user.getFilmLists();
    }

    // İsme göre liste getir
    public static FilmList getFilmListByName(User user, String listName) {
        for (FilmList list : user.getFilmLists()) {
            if (list.getName().equalsIgnoreCase(listName)) {
                return list;
            }
        }
        return null;
    }

    // Kullanıcının listelerini database'den yükle
    public static void loadUserFilmLists(User user) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT list_id, list_name FROM film_lists WHERE user_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, user.getUserId());
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                int listId = rs.getInt("list_id");
                String listName = rs.getString("list_name");
                
                FilmList filmList = new FilmList(listName);
                
                // Bu listedeki filmleri yükle
                loadMoviesForList(listId, filmList);
                
                user.addFilmList(filmList);
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Liste için filmleri yükle
    private static void loadMoviesForList(int listId, FilmList filmList) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT m.* FROM movies m " +
                        "INNER JOIN film_list_movies flm ON m.movie_id = flm.movie_id " +
                        "WHERE flm.list_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, listId);
            ResultSet rs = pstmt.executeQuery();
            
            FilmController filmController = new FilmController();
            while (rs.next()) {
                int movieId = rs.getInt("movie_id");
                Movie movie = filmController.getMovieById(movieId);
                if (movie != null) {
                    filmList.addMovie(movie);
                }
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Liste ID'sini al (liste adından)
    private static int getListId(FilmList list) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT list_id FROM film_lists WHERE list_name = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, list.getName());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int id = rs.getInt("list_id");
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

    // Film ID'sini al (başlıktan)
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
}
