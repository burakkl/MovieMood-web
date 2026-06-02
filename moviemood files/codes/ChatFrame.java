import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ChatFrame extends JFrame {
    private final User currentUser = FrontendStaticUser.frontEndStaticUser;

    private JPanel chatListPanel;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton; 

    private Chat activeChat;

    private final Color darkBg = new Color(30, 30, 30);
    private final Color redAccent = new Color(200, 50, 50);
    private final Color lightText = Color.WHITE;

    public ChatFrame() {
        setTitle("Chat");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); 
        setLayout(new BorderLayout());

        initChatListPanel();
        initChatViewPanel();

        populateChatList();

        getContentPane().setBackground(darkBg);
        setVisible(true);
    }

    private void initChatListPanel() {
        chatListPanel = new JPanel();
        chatListPanel.setLayout(new BoxLayout(chatListPanel, BoxLayout.Y_AXIS));
        chatListPanel.setBackground(darkBg);

        JScrollPane scrollPane = new JScrollPane(chatListPanel);
        scrollPane.setPreferredSize(new Dimension(250, getHeight()));
        scrollPane.getViewport().setBackground(darkBg);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.WEST);
    }

    private void initChatViewPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(darkBg);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBackground(darkBg);
        chatArea.setForeground(lightText);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        chatArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(null);
        chatScroll.getViewport().setBackground(darkBg);
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(darkBg);

        inputField = new JTextField();
        inputField.setBackground(new Color(45, 45, 45));
        inputField.setForeground(lightText);
        inputField.setCaretColor(lightText);
        inputField.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        sendButton = new JButton("Send");
        sendButton.setBackground(redAccent);
        sendButton.setForeground(lightText);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        add(chatPanel, BorderLayout.CENTER);
    }

    private void populateChatList() {
        chatListPanel.removeAll();

        List<User> friends = currentUser.getFriends();

        for (User friend : friends) {
            Chat chat = ChatController.getOrCreateChat(currentUser, friend);
            String lastMessage = "(No messages yet)";
            List<Message> messages = chat.getMessages();
            if (!messages.isEmpty()) {
                Message last = messages.get(messages.size() - 1);
                lastMessage = last.getSender().getUsername() + ": " + truncate(last.getContent(), 30);
            }

            JButton chatButton = new JButton("<html><b style='color:white'>" + friend.getUsername() +
                    "</b><br><span style='color:gray'>" + lastMessage + "</span></html>");
            chatButton.setHorizontalAlignment(SwingConstants.LEFT);
            chatButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            chatButton.setBackground(darkBg);
            chatButton.setForeground(lightText);
            chatButton.setFocusPainted(false);
            chatButton.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 60, 60)));

            chatButton.addActionListener(e -> {
                activeChat = chat;
                refreshChatArea();
            });

            chatListPanel.add(chatButton);
        }

        chatListPanel.revalidate();
        chatListPanel.repaint();
    }

    private void refreshChatArea() {
        if (activeChat == null)
            return;

        StringBuilder sb = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        for (Message message : activeChat.getMessages()) {
            sb.append("[").append(formatter.format(message.getTimestamp())).append("] ")
                    .append(message.getSender().getUsername()).append(": ")
                    .append(message.getContent()).append("\n");
        }

        chatArea.setText(sb.toString());
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void sendMessage() {
        if (activeChat == null)
            return;

        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            User receiver = activeChat.getOtherUser(currentUser);
            ChatController.sendMessage(currentUser, receiver, text);
            inputField.setText("");
            refreshChatArea();
            populateChatList();
        }
    }

    private String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
    

    public JPanel getChatListPanel() {
        return chatListPanel;
    }

    public static void main(String[] args) {
        User alice = new User("Alice", "Smith", "alice@mail.com", "pass", 1001);
        User bob = new User("Bob", "Brown", "bob@mail.com", "pass", 1002);
        User charlie = new User("Charlie", "Black", "charlie@mail.com", "pass", 1003);

        alice.addFriend(bob);
        alice.addFriend(charlie);

        FrontendStaticUser.frontEndStaticUser = alice;

        ChatController.sendMessage(bob, alice, "Hey Alice!");
        ChatController.sendMessage(alice, bob, "Hi Bob, how's it going?");
        ChatController.sendMessage(charlie, alice, "Wanna watch a movie?");

        SwingUtilities.invokeLater(ChatFrame::new);
    }
}