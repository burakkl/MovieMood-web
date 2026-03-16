import java.time.LocalDateTime;

public class Comment {
    private User author;
    private String text;
    private LocalDateTime timestamp;

    public Comment(User author, String text) {
        this.author = author;
        this.text = text;
        this.timestamp = LocalDateTime.now();
    }

    public User getAuthor() {
        return author;
    }

    public String getText() {
        return text;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setText(String text) {
        this.text = text;
        this.timestamp = LocalDateTime.now(); 
    }

    @Override
    public String toString() {
        return "Comment by " + author.getUsername() + " at " + timestamp + ": \"" + text + "\" ";
    }
}
