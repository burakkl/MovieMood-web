import java.time.LocalDateTime;

public class FriendRequest {
    private int requestId;
    private User sender;
    private User receiver;
    private String status;
    private LocalDateTime createdAt;

    public FriendRequest(int requestId, User sender, User receiver, String status, LocalDateTime createdAt) {
        this.requestId = requestId;
        this.sender = sender;
        this.receiver = receiver;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getRequestId() {
        return requestId;
    }

    public User getSender() {
        return sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
