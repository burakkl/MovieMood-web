import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;

public class FriendsPage extends JFrame {

    private Color darkBackground = new Color(25, 25, 25);

    public FriendsPage() {
        setTitle("Movie Mood - Friends");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.BLACK);

        JLabel header = new JLabel("Friends");
        header.setFont(new Font("Arial", Font.BOLD, 20));
        header.setForeground(Color.BLACK);
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBackground(Color.LIGHT_GRAY);
        headerPanel.add(header);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(Color.LIGHT_GRAY);

        String[] imageFiles = {"2.jpg", "3.jpg"};
        String[] friendNames = {"Rafa Silva", "Khontkar"};

        for (int i = 0; i < imageFiles.length; i++) {
            String imagePath = "images/" + imageFiles[i];
            BufferedImage img = tryLoadImage(imagePath);
            CircularPicturePanel previewCircle = new CircularPicturePanel(img, 80, darkBackground);
            addFriend(centerPanel, friendNames[i], previewCircle);
        }

        JScrollPane scrollPane = new JScrollPane(centerPanel);
        scrollPane.setBorder(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.LIGHT_GRAY);
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
        setVisible(true);
    }

    private void addFriend(JPanel parent, String name, CircularPicturePanel picturePanel) {
        JPanel friendPanel = new JPanel();
        friendPanel.setLayout(new BoxLayout(friendPanel, BoxLayout.Y_AXIS));
        friendPanel.setBackground(Color.LIGHT_GRAY);
        friendPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        picturePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nameLabel = new JLabel(name);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameLabel.setForeground(Color.BLACK);

        JButton chatButton = new JButton("CHAT");
        chatButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        chatButton.setBackground(Color.BLACK);
        chatButton.setForeground(Color.WHITE);
        chatButton.addActionListener(e -> openChatWindow(name, picturePanel));

        friendPanel.add(picturePanel);
        friendPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        friendPanel.add(nameLabel);
        friendPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        friendPanel.add(chatButton);

        parent.add(friendPanel);
    }

    private void openChatWindow(String friendName, CircularPicturePanel picturePanel) {
        JFrame chatFrame = new JFrame(friendName);
        chatFrame.setSize(400, 500);
        chatFrame.setLocationRelativeTo(null);
        chatFrame.setLayout(new BorderLayout());
        chatFrame.getContentPane().setBackground(Color.DARK_GRAY);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.DARK_GRAY);
        JButton backButton = new JButton("BACK");
        backButton.setBackground(Color.RED);
        backButton.setForeground(Color.WHITE);
        backButton.addActionListener(e -> chatFrame.dispose());

        JLabel nameLabel = new JLabel(friendName, SwingConstants.CENTER);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 16));

        header.add(backButton, BorderLayout.WEST);
        header.add(nameLabel, BorderLayout.CENTER);

        // Chat area
        JPanel chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(Color.LIGHT_GRAY);
        chatPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Initial messages
        chatPanel.add(createChatBubble(picturePanel, "HEY DID YOU LIKE THE MOVIE", true));
        chatPanel.add(createChatBubble(null, "Yeah it was pretty good", false));
        chatPanel.add(createChatBubble(picturePanel, "Okay than", true));

        JScrollPane chatScroll = new JScrollPane(chatPanel);
        chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Input field
        JPanel inputPanel = new JPanel(new BorderLayout());
        JTextField inputField = new JTextField();
        JButton sendButton = new JButton("SEND");
        sendButton.setBackground(Color.BLACK);
        sendButton.setForeground(Color.WHITE);

        sendButton.addActionListener(e -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty()) {
                chatPanel.add(createChatBubble(null, text, false));
                chatPanel.revalidate();
                chatPanel.repaint();
                inputField.setText("");

                JScrollBar vertical = chatScroll.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            }
        });

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        chatFrame.add(header, BorderLayout.NORTH);
        chatFrame.add(chatScroll, BorderLayout.CENTER);
        chatFrame.add(inputPanel, BorderLayout.SOUTH);

        chatFrame.setVisible(true);
    }

    private JPanel createChatBubble(CircularPicturePanel avatar, String message, boolean isLeft) {
        JPanel bubblePanel = new JPanel();
        bubblePanel.setLayout(new FlowLayout(isLeft ? FlowLayout.LEFT : FlowLayout.RIGHT));
        bubblePanel.setBackground(Color.LIGHT_GRAY);

        if (isLeft && avatar != null) {
            bubblePanel.add(new JLabel(avatar.getImageIcon(40)));
        }

        JLabel messageLabel = new JLabel("<html><div style='padding:6px; background:white; border-radius:6px; max-width:200px;'>" + message + "</div></html>");
        bubblePanel.add(messageLabel);

        return bubblePanel;
    }

    private BufferedImage tryLoadImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (Exception e) {
            System.err.println("Could not load image: " + path);
            return null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FriendsPage());
    }

    // İç sınıf: Profil resmini daire olarak çizer
    static class CircularPicturePanel extends JPanel {
        private BufferedImage image;
        private int diameter;
        private Color backgroundColor;

        public CircularPicturePanel(BufferedImage image, int diameter, Color backgroundColor) {
            this.image = image;
            this.diameter = diameter;
            this.backgroundColor = backgroundColor;
            setPreferredSize(new Dimension(diameter, diameter));
            setMinimumSize(new Dimension(diameter, diameter));
            setMaximumSize(new Dimension(diameter, diameter));
            setOpaque(false);
        }

        public void updateImage(BufferedImage newImage) {
            this.image = newImage;
        }

        public ImageIcon getImageIcon(int size) {
            if (image == null) return null;
            Image scaled = image.getScaledInstance(size, size, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            if (image != null) {
                g2d.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, diameter, diameter));
                g2d.drawImage(image, 0, 0, diameter, diameter, null);
            } else {
                g2d.setColor(Color.GRAY);
                g2d.fillOval(0, 0, diameter, diameter);
            }

            g2d.dispose();
        }
    }
}
