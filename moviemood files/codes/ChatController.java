import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatController {
    private static List<Chat> chats = new ArrayList<>();

    public static Chat getOrCreateChat(User a, User b) {
        // First check in memory
        for (Chat chat : chats) {
            if (chat.isBetween(a, b))
                return chat;
        }

        // Check in database
        Chat dbChat = getChatFromDatabase(a, b);
        if (dbChat != null) {
            loadChatMessages(dbChat);
            chats.add(dbChat);
            return dbChat;
        }

        // Create new chat and save to database
        Chat newChat = new Chat(a, b);
        saveChatToDatabase(newChat);
        chats.add(newChat);
        return newChat;
    }

    public static void sendMessage(User from, User to, String message) {
        Chat chat = getOrCreateChat(from, to);
        chat.sendMessage(from, message);

        // Save message to database
        if (chat.getChatId() != null) {
            saveMessageToDatabase(chat.getChatId(), from.getUserId(), message);
        }
    }

    private static Chat getChatFromDatabase(User a, User b) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM chats WHERE " +
                    "(user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, a.getUserId());
            pstmt.setInt(2, b.getUserId());
            pstmt.setInt(3, b.getUserId());
            pstmt.setInt(4, a.getUserId());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int chatId = rs.getInt("chat_id");
                int user1Id = rs.getInt("user1_id");
                int user2Id = rs.getInt("user2_id");

                User user1 = UserController.getUserById(user1Id);
                User user2 = UserController.getUserById(user2Id);

                Chat chat = new Chat(chatId, user1, user2);
                rs.close();
                pstmt.close();
                return chat;
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void saveChatToDatabase(Chat chat) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "INSERT INTO chats (user1_id, user2_id) VALUES (?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setInt(1, chat.getUser1().getUserId());
            pstmt.setInt(2, chat.getUser2().getUserId());
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                chat.setChatId(rs.getInt(1));
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void saveMessageToDatabase(int chatId, int senderId, String messageText) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "INSERT INTO messages (chat_id, sender_id, message_text) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, chatId);
            pstmt.setInt(2, senderId);
            pstmt.setString(3, messageText);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void loadChatMessages(Chat chat) {
        if (chat.getChatId() == null)
            return;

        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM messages WHERE chat_id = ? ORDER BY sent_at ASC";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, chat.getChatId());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int messageId = rs.getInt("message_id");
                int senderId = rs.getInt("sender_id");
                String messageText = rs.getString("message_text");
                Timestamp sentAt = rs.getTimestamp("sent_at");
                LocalDateTime timestamp = sentAt.toLocalDateTime();

                User sender = UserController.getUserById(senderId);
                Message message = new Message(messageId, sender, messageText, timestamp);
                chat.getMessages().add(message);
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
