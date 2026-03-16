import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserController {

    // User kayıt
    public static boolean register(String email, String firstname, String lastname, String passwordHash) {
        if (getUserByEmail(email) != null) {
            return false; // Email zaten kayıtlı
        }

        int uniqueId = generateUniqueId();
        String profilePicturePath = getRandomProfilePicture();

        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "INSERT INTO users (user_id, firstname, lastname, email, password_hash, profile_picture_path) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, uniqueId);
            pstmt.setString(2, firstname);
            pstmt.setString(3, lastname);
            pstmt.setString(4, email);
            pstmt.setString(5, passwordHash);
            pstmt.setString(6, profilePicturePath);
            pstmt.executeUpdate();
            pstmt.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Login
    public static User login(String email, String passwordHash) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM users WHERE email = ? AND password_hash = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            pstmt.setString(2, passwordHash);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = createUserFromResultSet(rs);
                loadUserData(user); // Kullanıcının diğer verilerini yükle
                rs.close();
                pstmt.close();
                return user;
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Email ile kullanıcı getir
    public static User getUserByEmail(String email) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM users WHERE email = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = createUserFromResultSet(rs);
                rs.close();
                pstmt.close();
                return user;
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ID ile kullanıcı getir
    public static User getUserById(int id) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM users WHERE user_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = createUserFromResultSet(rs);
                loadUserData(user);
                rs.close();
                pstmt.close();
                return user;
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Tüm kullanıcıları getir
    public static List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM users";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                User user = createUserFromResultSet(rs);
                users.add(user);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // Arkadaş ekle
    public static boolean addFriend(int userId, int friendId) {
        if (userId == friendId)
            return false;

        try {
            Connection conn = DatabaseManager.getConnection();
            // İki yönlü arkadaşlık ekle
            String sql = "INSERT OR IGNORE INTO user_friends (user_id, friend_id) VALUES (?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setInt(1, userId);
            pstmt.setInt(2, friendId);
            pstmt.executeUpdate();

            pstmt.setInt(1, friendId);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();

            pstmt.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Arkadaş çıkar
    public static boolean removeFriend(int userId, int friendId) {
        try {
            Connection conn = DatabaseManager.getConnection();

            // Delete friendship records
            String sql = "DELETE FROM user_friends WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.setInt(2, friendId);
            pstmt.setInt(3, friendId);
            pstmt.setInt(4, userId);
            pstmt.executeUpdate();
            pstmt.close();

            // Also delete any friend request records between these users
            String deleteFriendRequestsSql = "DELETE FROM friend_requests WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)";
            PreparedStatement deleteReqStmt = conn.prepareStatement(deleteFriendRequestsSql);
            deleteReqStmt.setInt(1, userId);
            deleteReqStmt.setInt(2, friendId);
            deleteReqStmt.setInt(3, friendId);
            deleteReqStmt.setInt(4, userId);
            deleteReqStmt.executeUpdate();
            deleteReqStmt.close();

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Kullanıcının arkadaşlarını yükle
    private static void loadUserFriends(User user) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT u.* FROM users u " +
                    "INNER JOIN user_friends uf ON u.user_id = uf.friend_id " +
                    "WHERE uf.user_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, user.getUserId());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                User friend = createUserFromResultSet(rs);
                user.addFriend(friend);
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Reload user's friends list from database
    public static void reloadUserFriends(User user) {
        // Clear existing friends from the user object
        user.getFriends().clear();
        // Reload friends from database
        loadUserFriends(user);
    }

    // Kullanıcının favorilerini yükle
    private static void loadUserFavorites(User user) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT movie_id FROM user_favorites WHERE user_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, user.getUserId());
            ResultSet rs = pstmt.executeQuery();

            FilmController filmController = new FilmController();
            while (rs.next()) {
                int movieId = rs.getInt("movie_id");
                Movie movie = filmController.getMovieById(movieId);
                if (movie != null) {
                    user.addFavorite(movie);
                }
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Kullanıcının son izlediği filmleri yükle
    private static void loadRecentlyWatched(User user) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT movie_id FROM recently_watched WHERE user_id = ? ORDER BY watched_at DESC LIMIT 20";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, user.getUserId());
            ResultSet rs = pstmt.executeQuery();

            FilmController filmController = new FilmController();
            while (rs.next()) {
                int movieId = rs.getInt("movie_id");
                Movie movie = filmController.getMovieById(movieId);
                if (movie != null) {
                    user.watchMovie(movie);
                }
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Kullanıcının tüm verilerini yükle
    private static void loadUserData(User user) {
        loadUserFriends(user);
        loadUserFavorites(user);
        loadRecentlyWatched(user);
        // Film listelerini yükle
        FilmListController.loadUserFilmLists(user);
    }

    // ResultSet'ten User oluştur
    private static User createUserFromResultSet(ResultSet rs) throws SQLException {
        int userId = rs.getInt("user_id");
        String firstname = rs.getString("firstname");
        String lastname = rs.getString("lastname");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");
        String profilePicturePath = rs.getString("profile_picture_path");

        User user = new User(firstname, lastname, email, passwordHash, userId);
        user.setProfilePicturePath(profilePicturePath);
        return user;
    }

    // Benzersiz ID üret
    private static int generateUniqueId() {
        int id;
        do {
            id = 1000 + (int) (Math.random() * 9000);
        } while (getUserById(id) != null);
        return id;
    }

    // Rastgele profil resmi seç
    private static String getRandomProfilePicture() {
        int imageNumber = (int) (Math.random() * 7) + 1;
        return "images/" + imageNumber + ".jpg";
    }

    // Kullanıcı favoriye film ekle
    public static boolean addFavorite(int userId, int movieId) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "INSERT OR IGNORE INTO user_favorites (user_id, movie_id) VALUES (?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.setInt(2, movieId);
            pstmt.executeUpdate();
            pstmt.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Kullanıcı favoriden film çıkar
    public static boolean removeFavorite(int userId, int movieId) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "DELETE FROM user_favorites WHERE user_id = ? AND movie_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.setInt(2, movieId);
            pstmt.executeUpdate();
            pstmt.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Update user's profile picture in database
    public static boolean updateProfilePicture(int userId, String picturePath) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "UPDATE users SET profile_picture_path = ? WHERE user_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, picturePath);
            pstmt.setInt(2, userId);
            int rowsAffected = pstmt.executeUpdate();
            pstmt.close();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Film izle kaydı ekle
    public static boolean addWatchedMovie(int userId, int movieId) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "INSERT INTO recently_watched (user_id, movie_id) VALUES (?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.setInt(2, movieId);
            pstmt.executeUpdate();
            pstmt.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Send friend request
    public static boolean sendFriendRequest(int senderId, int receiverId) {
        // Check if users are already friends
        try {
            Connection conn = DatabaseManager.getConnection();
            String checkFriendsSql = "SELECT * FROM user_friends WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)";
            PreparedStatement checkStmt = conn.prepareStatement(checkFriendsSql);
            checkStmt.setInt(1, senderId);
            checkStmt.setInt(2, receiverId);
            checkStmt.setInt(3, receiverId);
            checkStmt.setInt(4, senderId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                rs.close();
                checkStmt.close();
                return false; // Already friends
            }
            rs.close();
            checkStmt.close();

            // Check if request already exists
            String checkRequestSql = "SELECT * FROM friend_requests WHERE sender_id = ? AND receiver_id = ? AND status = 'pending'";
            PreparedStatement checkReqStmt = conn.prepareStatement(checkRequestSql);
            checkReqStmt.setInt(1, senderId);
            checkReqStmt.setInt(2, receiverId);
            ResultSet reqRs = checkReqStmt.executeQuery();

            if (reqRs.next()) {
                reqRs.close();
                checkReqStmt.close();
                return false; // Request already exists
            }
            reqRs.close();
            checkReqStmt.close();

            // Create new friend request
            String sql = "INSERT INTO friend_requests (sender_id, receiver_id, status) VALUES (?, ?, 'pending')";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, senderId);
            pstmt.setInt(2, receiverId);
            pstmt.executeUpdate();
            pstmt.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Accept friend request
    public static boolean acceptFriendRequest(int requestId, int receiverId) {
        try {
            Connection conn = DatabaseManager.getConnection();

            // Get the request details
            String getRequestSql = "SELECT * FROM friend_requests WHERE request_id = ? AND receiver_id = ? AND status = 'pending'";
            PreparedStatement getStmt = conn.prepareStatement(getRequestSql);
            getStmt.setInt(1, requestId);
            getStmt.setInt(2, receiverId);
            ResultSet rs = getStmt.executeQuery();

            if (rs.next()) {
                int senderId = rs.getInt("sender_id");
                rs.close();
                getStmt.close();

                // Update request status
                String updateSql = "UPDATE friend_requests SET status = 'accepted' WHERE request_id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setInt(1, requestId);
                updateStmt.executeUpdate();
                updateStmt.close();

                // Add as friends
                boolean success = addFriend(receiverId, senderId);
                return success;
            }
            rs.close();
            getStmt.close();
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Reject friend request
    public static boolean rejectFriendRequest(int requestId, int receiverId) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "UPDATE friend_requests SET status = 'rejected' WHERE request_id = ? AND receiver_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, requestId);
            pstmt.setInt(2, receiverId);
            int rowsAffected = pstmt.executeUpdate();
            pstmt.close();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Get pending friend requests received by user
    public static List<FriendRequest> getPendingFriendRequests(int userId) {
        List<FriendRequest> requests = new ArrayList<>();
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM friend_requests WHERE receiver_id = ? AND status = 'pending' ORDER BY created_at DESC";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int requestId = rs.getInt("request_id");
                int senderId = rs.getInt("sender_id");
                int receiverId = rs.getInt("receiver_id");
                String status = rs.getString("status");
                java.sql.Timestamp timestamp = rs.getTimestamp("created_at");
                java.time.LocalDateTime createdAt = timestamp.toLocalDateTime();

                User sender = getUserById(senderId);
                User receiver = getUserById(receiverId);

                if (sender != null && receiver != null) {
                    FriendRequest request = new FriendRequest(requestId, sender, receiver, status, createdAt);
                    requests.add(request);
                }
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return requests;
    }

    // Get count of pending friend requests
    public static int getPendingFriendRequestCount(int userId) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT COUNT(*) as count FROM friend_requests WHERE receiver_id = ? AND status = 'pending'";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int count = rs.getInt("count");
                rs.close();
                pstmt.close();
                return count;
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}