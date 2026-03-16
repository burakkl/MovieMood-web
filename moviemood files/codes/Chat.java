import java.util.ArrayList;
import java.util.List;

public class Chat {
    private Integer chatId;
    private User user1;
    private User user2;
    private List<Message> messages = new ArrayList<>();

    public Chat(User user1, User user2) {
        this.user1 = user1;
        this.user2 = user2;
    }

    public Chat(Integer chatId, User user1, User user2) {
        this.chatId = chatId;
        this.user1 = user1;
        this.user2 = user2;
    }

    public Integer getChatId() {
        return chatId;
    }

    public void setChatId(Integer chatId) {
        this.chatId = chatId;
    }

    public void sendMessage(User from, String content) {
        messages.add(new Message(from, content));
    }

    public List<Message> getMessages() {
        return messages;
    }

    public boolean isBetween(User a, User b) {
        return (a.getUserId() == user1.getUserId() && b.getUserId() == user2.getUserId()) ||
                (a.getUserId() == user2.getUserId() && b.getUserId() == user1.getUserId());
    }

    public User getOtherUser(User current) {
        return current.getUserId() == user1.getUserId() ? user2 : user1;
    }

    public User getUser1() {
        return user1;
    }

    public User getUser2() {
        return user2;
    }
}
