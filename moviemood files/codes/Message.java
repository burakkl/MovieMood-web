
// Message.java
import java.time.LocalDateTime;

public class Message {
    private Integer messageId;
    private User sender;
    private String content;
    private LocalDateTime timestamp;

    public Message(User sender, String content) {
        this.sender = sender;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public Message(Integer messageId, User sender, String content, LocalDateTime timestamp) {
        this.messageId = messageId;
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public void setMessageId(Integer messageId) {
        this.messageId = messageId;
    }

    public String getContent() {
        return content;
    }

    public User getSender() {
        return sender;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
