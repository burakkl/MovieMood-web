import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

public class MoviesPage extends JFrame {
    private FilmController filmController;
    private UserController userController;
    private User currentUser;

    private static final Color DARK_BG = new Color(20, 20, 20);

    public MoviesPage(FilmController filmController, UserController userController, User currentUser) {
        this.filmController = filmController;
        this.userController = userController;
        this.currentUser = currentUser;

        setTitle("Movie Mood - Movies");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setSize(1200, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Ensure no border on the JFrame
        getRootPane().setBorder(null);
        getContentPane().setBackground(DARK_BG);

        add(createHeader(), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(DARK_BG);
        centerPanel.setBorder(null);

        JLabel genreLabel = new JLabel("Movies by Genre");
        genreLabel.setForeground(Color.WHITE);
        genreLabel.setFont(new Font("Arial", Font.BOLD, 16));
        genreLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        genreLabel.setBorder(BorderFactory.createEmptyBorder(10, 70, 10, 0));
        centerPanel.add(genreLabel);

        JPanel genreRow = new JPanel(new GridLayout(1, 3));
        genreRow.setBackground(DARK_BG);
        genreRow.setBorder(BorderFactory.createEmptyBorder(0, 70, 0, 70));

        genreRow.add(createCategoryPanel("Science Fiction"));
        genreRow.add(createCategoryPanel("Action"));
        genreRow.add(createCategoryPanel("Romance"));
        centerPanel.add(genreRow);

        JLabel yearLabel = new JLabel("Movies by Release Year");
        yearLabel.setForeground(Color.WHITE);
        yearLabel.setFont(new Font("Arial", Font.BOLD, 16));
        yearLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        yearLabel.setBorder(BorderFactory.createEmptyBorder(20, 70, 10, 0));
        centerPanel.add(yearLabel);

        JPanel yearRow = new JPanel(new GridLayout(1, 3));
        yearRow.setBackground(DARK_BG);
        yearRow.setBorder(BorderFactory.createEmptyBorder(0, 70, 10, 70));

        yearRow.add(createYearPanel("2000-", 2000, 3000));
        yearRow.add(createYearPanel("1990-2000", 1990, 1999));
        yearRow.add(createYearPanel("1900-1990", 1900, 1989));
        centerPanel.add(yearRow);

        JScrollPane scrollPane = new JScrollPane(centerPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(DARK_BG);
        scrollPane.setBackground(DARK_BG);
        add(scrollPane, BorderLayout.CENTER);

        setVisible(true);
    }

    private JPanel createHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(DARK_BG);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Logo
        JLabel logoLabel = new JLabel("Movie Mood");
        logoLabel.setFont(new Font("Arial", Font.BOLD, 24));
        logoLabel.setForeground(Color.RED);
        headerPanel.add(logoLabel, BorderLayout.WEST);

        // Navigation buttons
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        navPanel.setOpaque(false);

        String[] navItems = { "Home", "Explore", "My List", "Movies", "My Profile" };
        for (String item : navItems) {
            JButton navButton = new JButton(item);
            styleButton(navButton, item.equals("Movies"));

            if (item.equals("Home")) {
                navButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {

                        HomePage homePage = new HomePage(filmController, userController, currentUser);

                        dispose();
                    }
                });
            } else if (item.equals("Explore")) {
                navButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // Create a new ExploreFrame with the current user
                        ExploreFrame exploreFrame = new ExploreFrame(filmController, userController, currentUser);
                        // Dispose this MoviesPage
                        dispose();
                    }
                });
            } else if (item.equals("My List")) {
                navButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // Create a new MyListPanel with the current user
                        MyListPanel myListPanel = new MyListPanel(currentUser);
                        // Dispose this MoviesPage
                        dispose();
                    }
                });
            } else if (item.equals("Movies")) {
                navButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // Already on Movies page - do nothing
                    }
                });
            } else if (item.equals("My Profile")) {
                navButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // Create a new ProfileFrame with the current user
                        ProfileFrame profileFrame = new ProfileFrame(currentUser);
                        // Dispose this MoviesPage
                        dispose();
                    }
                });
            }

            navPanel.add(navButton);
        }
        headerPanel.add(navPanel, BorderLayout.CENTER);

        return headerPanel;
    }

    private void styleButton(JButton button, boolean selected) {
        button.setForeground(selected ? Color.WHITE : Color.LIGHT_GRAY);
        button.setBackground(null);
        button.setBorder(null);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setFont(new Font("Arial", Font.PLAIN, 16));
    }

    private JPanel createYearPanel(String label, int start, int end) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(DARK_BG);
        panel.setBorder(null);

        JButton title = new JButton(label);
        title.setForeground(Color.WHITE);
        title.setBackground(DARK_BG);
        title.setFocusPainted(false);
        title.setBorderPainted(false);
        title.setFont(new Font("Arial", Font.BOLD, 14));
        title.addActionListener(e -> {
            new GenrePage(filmController, userController, currentUser, label, start, end);
            dispose();
        });

        JPanel posters = createPostersPanel(filmController.searchByReleaseYearInterval(start, end));
        panel.add(title, BorderLayout.NORTH);
        panel.add(posters, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createPostersPanel(List<Movie> movies) {
        JPanel panel = new JPanel();
        panel.setLayout(new OverlayLayout(panel));
        panel.setBackground(DARK_BG);
        panel.setBorder(null);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        int offset = 45;
        int count = 0;

        for (Movie movie : movies) {
            if (count >= 4)
                break;

            // Create placeholder label
            JLabel poster = new JLabel();
            poster.setPreferredSize(new Dimension(150, 225));
            poster.setBackground(Color.DARK_GRAY);
            poster.setOpaque(true);
            poster.setAlignmentX(0.0f);
            poster.setAlignmentY(0.0f);
            poster.setBorder(BorderFactory.createEmptyBorder(0, count * offset, 0, 0));
            poster.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            poster.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    new MovieMoodGUI(filmController, userController, currentUser, movie);
                    dispose();
                }
            });

            panel.add(poster);

            loadImageAsync(movie.getPosterUrl(), poster);

            count++;
        }

        return panel;
    }

    private JPanel createCategoryPanel(String categoryName) {
        JPanel categoryPanel = new JPanel();
        categoryPanel.setLayout(new BorderLayout());
        categoryPanel.setBackground(DARK_BG);
        categoryPanel.setBorder(null);

        JButton titleButton = new JButton(categoryName);
        titleButton.setFont(new Font("Arial", Font.BOLD, 14));
        titleButton.setForeground(Color.WHITE);
        titleButton.setBackground(DARK_BG);
        titleButton.setFocusPainted(false);
        titleButton.setBorderPainted(false);
        titleButton.addActionListener(e -> {
            new GenrePage(filmController, userController, currentUser, categoryName);
            dispose();
        });

        JPanel posters = new JPanel();
        posters.setLayout(new OverlayLayout(posters));
        posters.setBackground(DARK_BG);
        posters.setBorder(null);
        posters.setAlignmentX(Component.LEFT_ALIGNMENT);
        int offset = 45;

        List<Movie> movies = filmController.searchByGenre(categoryName);

        int count = 0;
        for (Movie movie : movies) {
            if (count >= 4)
                break;

            // Create placeholder label
            JLabel poster = new JLabel();
            poster.setPreferredSize(new Dimension(150, 225));
            poster.setBackground(Color.DARK_GRAY);
            poster.setOpaque(true);
            poster.setAlignmentX(0.0f);
            poster.setAlignmentY(0.0f);
            poster.setBorder(BorderFactory.createEmptyBorder(0, count * offset, 0, 0));
            poster.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            poster.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    new MovieMoodGUI(filmController, userController, currentUser, movie);
                    dispose();
                }
            });

            posters.add(poster);

            // Load image asynchronously
            loadImageAsync(movie.getPosterUrl(), poster);

            count++;
        }

        categoryPanel.add(titleButton, BorderLayout.NORTH);
        categoryPanel.add(posters, BorderLayout.CENTER);
        return categoryPanel;
    }

    private void loadImageAsync(String imageUrl, JLabel label) {
        SwingWorker<ImageIcon, Void> worker = new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                try {
                    URL url = new URL(imageUrl);
                    BufferedImage image = ImageIO.read(url);
                    Image scaled = image.getScaledInstance(150, 225, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaled);
                } catch (Exception e) {
                    System.err.println("Image could not be loaded: " + e.getMessage());
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        label.setIcon(icon);
                        label.setOpaque(false);
                        label.repaint();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        worker.execute();
    }

    public static ImageIcon loadImageFromURL(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            BufferedImage image = ImageIO.read(url);
            return new ImageIcon(image);
        } catch (Exception e) {
            System.err.println("Image could not be loaded: " + e.getMessage());
            return new ImageIcon();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FilmController filmController = new FilmController();
            UserController userController = new UserController();
            MovieSeeder.seedMovies(filmController);

            userController.register("test@example.com", "Test", "User", "password");
            User testUser = userController.login("test@example.com", "password");

            new MoviesPage(filmController, userController, testUser);
        });
    }
}