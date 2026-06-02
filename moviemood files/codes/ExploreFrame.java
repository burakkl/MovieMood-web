import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;
import java.net.URL;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.stream.Collectors;

public class ExploreFrame extends JFrame {

    private JPanel mainPanel, headerPanel, searchPanel, resultsPanel;
    private JTextField searchField;
    private JButton searchButton, filterButton;
    private JLabel titleLabel;
    private JButton homeButton, exploreButton, myListButton, moviesButton, profileButton, chatButton;

    private FilmController filmController;
    private UserController userController;
    private User currentUser;

    private Color darkBackground = new Color(25, 25, 25);
    private Color brightRed = new Color(230, 0, 0);

    private Set<String> selectedGenres = new HashSet<>();

    public ExploreFrame(FilmController filmController, UserController userController, User currentUser) {
        this.filmController = filmController;
        this.userController = userController;
        this.currentUser = currentUser;

        setTitle("Movie Mood - Explore");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setSize(1200, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        layoutComponents();

        setVisible(true);
    }

    private void initComponents() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(darkBackground);

        createHeader();
        createSearchPanel();
        createResultsPanel();
    }

    private void createHeader() {
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(darkBackground);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        titleLabel = new JLabel("Movie Mood");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.RED);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        navPanel.setOpaque(false);

        homeButton = createNavButton("Home");
        exploreButton = createNavButton("Explore");
        exploreButton.setForeground(Color.WHITE);
        myListButton = createNavButton("My List");
        moviesButton = createNavButton("Movies");
        profileButton = createNavButton("My Profile");

        homeButton.addActionListener(e -> {
            if (currentUser == null) {
                JOptionPane.showMessageDialog(this, "Please log in first", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            dispose();
            new HomePage(filmController, userController, currentUser);
        });

        myListButton.addActionListener(e -> navigateToMyList());

        moviesButton.addActionListener(e -> {
            if (currentUser == null) {
                JOptionPane.showMessageDialog(this, "Please log in first", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            dispose();
            new MoviesPage(filmController, userController, currentUser);
        });

        profileButton.addActionListener(e -> {
            if (currentUser == null) {
                JOptionPane.showMessageDialog(this, "Please log in first", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            dispose();
            new ProfileFrame(currentUser);
        });

        navPanel.add(homeButton);
        navPanel.add(exploreButton);
        navPanel.add(myListButton);
        navPanel.add(moviesButton);
        navPanel.add(profileButton);

        headerPanel.add(navPanel, BorderLayout.CENTER);

    }

    private void createSearchPanel() {
        searchPanel = new JPanel();
        searchPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));
        searchPanel.setBackground(darkBackground);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(600, 45));
        searchField.setFont(new Font("Arial", Font.PLAIN, 16));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        searchField.setBackground(new Color(40, 40, 40));
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(Color.WHITE);

        searchField.setText("🔍 Search movies...");
        searchField.setForeground(Color.GRAY);

        searchField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals("🔍 Search movies...")) {
                    searchField.setText("");
                    searchField.setForeground(Color.WHITE);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("🔍 Search movies...");
                    searchField.setForeground(Color.GRAY);
                }
            }
        });

        searchButton = new JButton("Search");
        searchButton.setFont(new Font("Arial", Font.BOLD, 16));
        searchButton.setPreferredSize(new Dimension(100, 45));
        searchButton.setBackground(brightRed);
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.setBorderPainted(false);
        searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        searchButton.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch());

        // Create filter button
        filterButton = new JButton("Filter by Genre");
        filterButton.setFont(new Font("Arial", Font.BOLD, 16));
        filterButton.setPreferredSize(new Dimension(150, 45));
        filterButton.setBackground(new Color(60, 60, 60));
        filterButton.setForeground(Color.WHITE);
        filterButton.setFocusPainted(false);
        filterButton.setBorderPainted(false);
        filterButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        filterButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                filterButton.setBackground(new Color(80, 80, 80));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                filterButton.setBackground(new Color(60, 60, 60));
            }
        });

        filterButton.addActionListener(e -> showGenreFilterDialog());

        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(filterButton);
    }

    private void showGenreFilterDialog() {

        Set<String> allGenres = getAllGenres();

        JDialog filterDialog = new JDialog(this, "Filter by Genre", true);
        filterDialog.setLayout(new BorderLayout());
        filterDialog.setSize(400, 500);
        filterDialog.setLocationRelativeTo(this);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(darkBackground);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Select Genres to Filter");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        Map<String, JCheckBox> genreCheckboxes = new HashMap<>();

        for (String genre : allGenres) {
            JCheckBox checkbox = new JCheckBox(genre);
            checkbox.setFont(new Font("Arial", Font.PLAIN, 14));
            checkbox.setForeground(Color.WHITE);
            checkbox.setBackground(darkBackground);
            checkbox.setSelected(selectedGenres.contains(genre));

            genreCheckboxes.put(genre, checkbox);
            contentPanel.add(checkbox);
            contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBackground(darkBackground);
        scrollPane.getViewport().setBackground(darkBackground);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        filterDialog.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(darkBackground);

        JButton applyButton = new JButton("Apply Filter");
        applyButton.setFont(new Font("Arial", Font.BOLD, 14));
        applyButton.setBackground(brightRed);
        applyButton.setForeground(Color.WHITE);
        applyButton.setFocusPainted(false);
        applyButton.setBorderPainted(false);
        applyButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton clearButton = new JButton("Clear All");
        clearButton.setFont(new Font("Arial", Font.BOLD, 14));
        clearButton.setBackground(new Color(60, 60, 60));
        clearButton.setForeground(Color.WHITE);
        clearButton.setFocusPainted(false);
        clearButton.setBorderPainted(false);
        clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        applyButton.addActionListener(e -> {
            selectedGenres.clear();
            for (Map.Entry<String, JCheckBox> entry : genreCheckboxes.entrySet()) {
                if (entry.getValue().isSelected()) {
                    selectedGenres.add(entry.getKey());
                }
            }
            applyGenreFilter();
            filterDialog.dispose();
        });

        clearButton.addActionListener(e -> {
            selectedGenres.clear();
            for (JCheckBox checkbox : genreCheckboxes.values()) {
                checkbox.setSelected(false);
            }
        });

        buttonPanel.add(applyButton);
        buttonPanel.add(clearButton);

        filterDialog.add(buttonPanel, BorderLayout.SOUTH);
        filterDialog.setVisible(true);
    }

    private Set<String> getAllGenres() {
        Set<String> genres = new TreeSet<>();
        List<Movie> allMovies = filmController.getAllMovies();

        for (Movie movie : allMovies) {
            List<String> movieGenres = movie.getGenres();
            if (movieGenres != null) {
                genres.addAll(movieGenres);
            }
        }

        return genres;
    }

    private void applyGenreFilter() {
        if (selectedGenres.isEmpty()) {
            displayRandomMovies();
            return;
        }

        List<Movie> allMovies = filmController.getAllMovies();
        List<Movie> filteredMovies = allMovies.stream()
                .filter(movie -> {
                    List<String> movieGenres = movie.getGenres();
                    if (movieGenres == null || movieGenres.isEmpty()) {
                        return false;
                    }

                    for (String movieGenre : movieGenres) {
                        if (selectedGenres.contains(movieGenre)) {
                            return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());

        displaySearchResults(filteredMovies);
    }

    private void createResultsPanel() {
        resultsPanel = new JPanel();
        resultsPanel.setLayout(new GridLayout(0, 5, 20, 20));
        resultsPanel.setBackground(darkBackground);
        resultsPanel.setBorder(BorderFactory.createEmptyBorder(20, 70, 20, 70));

        displayRandomMovies();
    }

    private void displayRandomMovies() {
        resultsPanel.removeAll();

        List<Movie> allMovies = filmController.getAllMovies();
        Collections.shuffle(allMovies);

        int numberOfMovies = Math.min(15, allMovies.size());

        for (int i = 0; i < numberOfMovies; i++) {
            resultsPanel.add(createMovieCard(allMovies.get(i)));
        }

        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private void performSearch() {
        String query = searchField.getText().trim();

        if (query.isEmpty() || query.equals("🔍 Search movies...")) {
            if (selectedGenres.isEmpty()) {
                displayRandomMovies();
            } else {
                applyGenreFilter();
            }
            return;
        }

        List<Movie> searchResults = filmController.searchByTitle(query);

        if (!selectedGenres.isEmpty()) {
            searchResults = searchResults.stream()
                    .filter(movie -> {
                        List<String> movieGenres = movie.getGenres();
                        if (movieGenres == null || movieGenres.isEmpty()) {
                            return false;
                        }
                        for (String movieGenre : movieGenres) {
                            if (selectedGenres.contains(movieGenre)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        }

        displaySearchResults(searchResults);
    }

    private void displaySearchResults(List<Movie> movies) {
        resultsPanel.removeAll();
        resultsPanel.setLayout(new GridLayout(0, 5, 20, 20));
        resultsPanel.setBorder(BorderFactory.createEmptyBorder(20, 70, 20, 70));

        if (movies.isEmpty()) {
            resultsPanel.setLayout(new GridBagLayout());
            JLabel noResultsLabel = new JLabel("No movies found for your search.");
            noResultsLabel.setFont(new Font("Arial", Font.PLAIN, 20));
            noResultsLabel.setForeground(Color.GRAY);
            resultsPanel.add(noResultsLabel);
        } else {
            for (Movie movie : movies) {
                resultsPanel.add(createMovieCard(movie));
            }
        }

        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private JButton createNavButton(String text) {
        JButton button = new JButton(text);
        button.setForeground(text.equals("Explore") ? Color.WHITE : Color.LIGHT_GRAY);
        button.setBackground(null);
        button.setBorder(null);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setFont(new Font("Arial", Font.PLAIN, 16));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!text.equals("Explore")) {
                    button.setForeground(Color.WHITE);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!text.equals("Explore")) {
                    button.setForeground(Color.LIGHT_GRAY);
                }
            }
        });

        return button;
    }

    private void layoutComponents() {
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(searchPanel, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(resultsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(darkBackground);

        mainPanel.add(scrollPane, BorderLayout.SOUTH);
        scrollPane.setPreferredSize(new Dimension(getWidth(), 600));

        add(mainPanel);
    }

    private JPanel createMovieCard(Movie movie) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(new Color(30, 30, 30));
        card.setPreferredSize(new Dimension(160, 240));
        card.setBorder(BorderFactory.createEmptyBorder());
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel posterLabel = new JLabel();
        posterLabel.setPreferredSize(new Dimension(160, 200));
        posterLabel.setHorizontalAlignment(JLabel.CENTER);
        posterLabel.setVerticalAlignment(JLabel.CENTER);
        posterLabel.setBackground(new Color(20, 20, 20));
        posterLabel.setOpaque(true);

        SwingWorker<ImageIcon, Void> imageLoader = new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                try {
                    String posterUrl = movie.getPosterUrl();
                    if (posterUrl != null && !posterUrl.isEmpty()) {
                        URL url = new URL(posterUrl);
                        BufferedImage img = ImageIO.read(url);
                        Image scaledImg = img.getScaledInstance(160, 200, Image.SCALE_SMOOTH);
                        return new ImageIcon(scaledImg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        posterLabel.setIcon(icon);
                        posterLabel.setText("");
                    } else {
                        showPlaceholder(posterLabel);
                    }
                } catch (Exception e) {
                    showPlaceholder(posterLabel);
                }
            }
        };

        showPlaceholder(posterLabel);

        imageLoader.execute();

        JLabel titleLabel = new JLabel(movie.getTitle(), SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        card.add(posterLabel, BorderLayout.CENTER);
        card.add(titleLabel, BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
                new MovieMoodGUI(filmController, userController, currentUser, movie);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(new Color(50, 50, 50));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(new Color(30, 30, 30));
            }
        });

        return card;
    }

    private void showPlaceholder(JLabel label) {
        label.setText("🎬");
        label.setFont(new Font("Arial", Font.BOLD, 40));
        label.setForeground(new Color(60, 60, 60));
        label.setBackground(new Color(20, 20, 20));
        label.setOpaque(true);
    }

    private void navigateToMyList() {
        try {
            if (currentUser == null) {
                JOptionPane.showMessageDialog(this, "Please log in first", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            dispose();
            new MyListPanel(currentUser);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error opening My List: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FilmController filmController = new FilmController();
            UserController userController = new UserController();

            MovieSeeder.seedMovies(filmController);

            userController.register("test@example.com", "Test", "User", "password123");
            User testUser = userController.login("test@example.com", "password123");

            if (testUser != null) {
                System.out.println("Logged in as: " + testUser.getUsername());
                new ExploreFrame(filmController, userController, testUser);
            } else {
                System.out.println("Login failed!");
            }
        });
    }
}