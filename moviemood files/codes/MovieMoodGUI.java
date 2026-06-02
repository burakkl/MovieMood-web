import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;
import javax.imageio.ImageIO;

public class MovieMoodGUI extends JFrame {
    private FilmController filmController;
    private UserController userController;
    private User currentUser;
    private Movie selectedMovie;

    private JPanel mainPanel, headerPanel, contentPanel;
    private JLabel titleLabel, userRatingLabel;
    private JButton homeButton, exploreButton, myListButton, moviesButton, profileButton, chatButton;

    private Color darkBackground = new Color(25, 25, 25);
    private Color brightRed = new Color(230, 0, 0);

    public MovieMoodGUI(FilmController filmController, UserController userController, User currentUser, Movie movie) {
        this.filmController = filmController;
        this.userController = userController;
        this.currentUser = currentUser;
        this.selectedMovie = movie;

        // Add to recently watched when opening the movie details
        if (currentUser != null && selectedMovie != null) {
            currentUser.watchMovie(selectedMovie);
        }

        setTitle("Movie Mood - " + (movie != null ? movie.getTitle() : "Movie Details"));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setSize(1200, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();

        setVisible(true);
    }

    private void initComponents() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(darkBackground);

        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(darkBackground);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        contentPanel = new JPanel();
        contentPanel.setBackground(darkBackground);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        createHeader();
        createContentPanel();

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        add(mainPanel);
    }

    private void createHeader() {
        titleLabel = new JLabel("Movie Mood");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(brightRed);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        navPanel.setOpaque(false);

        homeButton = createNavButton("Home");
        exploreButton = createNavButton("Explore");
        myListButton = createNavButton("My List");
        moviesButton = createNavButton("Movies");
        profileButton = createNavButton("My Profile");

        navPanel.add(homeButton);
        navPanel.add(exploreButton);
        navPanel.add(myListButton);
        navPanel.add(moviesButton);
        navPanel.add(profileButton);

        headerPanel.add(navPanel, BorderLayout.CENTER);

        homeButton.addActionListener(e -> navigateToHome());
        exploreButton.addActionListener(e -> navigateToExplore());
        myListButton.addActionListener(e -> navigateToMyList());
        moviesButton.addActionListener(e -> navigateToMovies());
        profileButton.addActionListener(e -> navigateToProfile());
    }

    private void createContentPanel() {
        contentPanel.setLayout(new BorderLayout());

        if (selectedMovie != null) {
            JPanel movieDetailsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
            movieDetailsPanel.setBackground(darkBackground);

            JPanel leftPanel = new JPanel();
            leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
            leftPanel.setBackground(darkBackground);

            JLabel movieTitleLabel = new JLabel(selectedMovie.getTitle());
            movieTitleLabel.setFont(new Font("Arial", Font.BOLD, 32));
            movieTitleLabel.setForeground(Color.WHITE);
            movieTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            leftPanel.add(movieTitleLabel);
            leftPanel.add(Box.createRigidArea(new Dimension(0, 20)));

            JLabel yearLabel = new JLabel("Release Year: " + selectedMovie.getReleaseDate());
            yearLabel.setFont(new Font("Arial", Font.PLAIN, 18));
            yearLabel.setForeground(Color.WHITE);
            yearLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            leftPanel.add(yearLabel);
            leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            String genresText = "Genres: " + String.join(", ", selectedMovie.getGenres());
            JLabel genresLabel = new JLabel(genresText);
            genresLabel.setFont(new Font("Arial", Font.PLAIN, 18));
            genresLabel.setForeground(Color.WHITE);
            genresLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            leftPanel.add(genresLabel);
            leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            JLabel imdbRatingLabel = new JLabel("IMDB Rating: " + selectedMovie.getImdbRating());
            imdbRatingLabel.setFont(new Font("Arial", Font.PLAIN, 18));
            imdbRatingLabel.setForeground(brightRed);
            imdbRatingLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            leftPanel.add(imdbRatingLabel);
            leftPanel.add(Box.createRigidArea(new Dimension(0, 20)));

            double userRating = FilmController.getAverageRating(selectedMovie);
            userRatingLabel = new JLabel("Our Users Rate: " + String.format("%.1f", userRating));
            userRatingLabel.setFont(new Font("Arial", Font.BOLD, 18));
            userRatingLabel.setForeground(Color.WHITE);
            userRatingLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            leftPanel.add(userRatingLabel);
            leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            JTextArea overviewArea = new JTextArea(selectedMovie.getOverview());
            overviewArea.setFont(new Font("Arial", Font.PLAIN, 16));
            overviewArea.setForeground(Color.WHITE);
            overviewArea.setBackground(darkBackground);
            overviewArea.setLineWrap(true);
            overviewArea.setWrapStyleWord(true);
            overviewArea.setEditable(false);
            overviewArea.setPreferredSize(new Dimension(500, 200));
            overviewArea.setMaximumSize(new Dimension(500, 200));
            overviewArea.setAlignmentX(Component.LEFT_ALIGNMENT);
            leftPanel.add(overviewArea);
            leftPanel.add(Box.createRigidArea(new Dimension(0, 30)));

            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            actionPanel.setBackground(darkBackground);
            actionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JButton addRatingButton = createActionButton("Add Rating");
            JButton addCommentButton = createActionButton("Add Comment");
            JButton seeCommentsButton = createActionButton("See Comments");
            JButton addToListButton = createActionButton("Add to List");
            JButton imdbButton = createActionButton("Visit IMDb");
            imdbButton.addActionListener(e -> {
                try {
                    String url = selectedMovie.getWebLink();
                    if (url != null && !url.isEmpty()) {
                        Desktop.getDesktop().browse(new java.net.URI(url));
                    } else {
                        JOptionPane.showMessageDialog(null, "IMDb link is not available for this movie.");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Failed to open IMDb link.");
                }
            });
            actionPanel.add(imdbButton);
            JButton youtubeButton = createActionButton("Watch Trailer");
            youtubeButton.addActionListener(e -> {
                try {
                    String url = selectedMovie.getYoutubeLink();
                    if (url != null && !url.isEmpty()) {
                        Desktop.getDesktop().browse(new java.net.URI(url));
                    } else {
                        JOptionPane.showMessageDialog(null, "YouTube link is not available for this movie.");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Failed to open YouTube link.");
                }
            });
            actionPanel.add(youtubeButton);
            addRatingButton.addActionListener(e -> showRatingDialog());
            addCommentButton.addActionListener(e -> showAddCommentDialog());
            seeCommentsButton.addActionListener(e -> showCommentsDialog());
            addToListButton.addActionListener(e -> showAddToListDialog());

            actionPanel.add(addRatingButton);
            actionPanel.add(addCommentButton);
            actionPanel.add(seeCommentsButton);
            actionPanel.add(addToListButton);

            leftPanel.add(actionPanel);

            JPanel rightPanel = new JPanel();
            rightPanel.setBackground(darkBackground);
            rightPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

            JLabel posterLabel = new JLabel();
            posterLabel.setPreferredSize(new Dimension(300, 450));
            posterLabel.setBackground(new Color(40, 40, 40));
            posterLabel.setOpaque(true);
            posterLabel.setHorizontalAlignment(JLabel.CENTER);
            posterLabel.setVerticalAlignment(JLabel.CENTER);

            loadPosterImage(posterLabel);

            rightPanel.add(posterLabel);

            movieDetailsPanel.add(leftPanel);
            movieDetailsPanel.add(rightPanel);

            contentPanel.add(movieDetailsPanel);
        }
    }

    private void loadPosterImage(JLabel posterLabel) {
        posterLabel.setText("Loading...");
        posterLabel.setForeground(Color.LIGHT_GRAY);

        SwingWorker<ImageIcon, Void> worker = new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                try {
                    String posterUrl = selectedMovie.getPosterUrl();
                    if (posterUrl != null && !posterUrl.isEmpty()) {
                        URL url = new URL(posterUrl);
                        BufferedImage img = ImageIO.read(url);
                        Image scaledImg = img.getScaledInstance(300, 450, Image.SCALE_SMOOTH);
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
                        posterLabel.setText("No Image");
                        posterLabel.setFont(new Font("Arial", Font.BOLD, 24));
                    }
                } catch (Exception e) {
                    posterLabel.setText("Error");
                }
            }
        };
        worker.execute();
    }

    private JButton createNavButton(String text) {
        JButton button = new JButton(text);
        button.setForeground(text.equals("Movies") ? Color.WHITE : Color.LIGHT_GRAY);
        button.setBackground(null);
        button.setBorder(null);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setFont(new Font("Arial", Font.PLAIN, 16));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!text.equals("Movies")) {
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

    private JButton createActionButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(brightRed);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(120, 35));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(brightRed.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(brightRed);
            }
        });

        return button;
    }

    private void showRatingDialog() {
        JDialog dialog = new JDialog(this, "Rate Movie", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(darkBackground);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(darkBackground);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel label = new JLabel("Rate this movie (1-10):");
        label.setFont(new Font("Arial", Font.BOLD, 18));
        label.setForeground(Color.WHITE);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSlider slider = new JSlider(1, 10, 5);
        slider.setMajorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setBackground(darkBackground);
        slider.setForeground(Color.WHITE);

        JButton submitButton = new JButton("Submit");
        submitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        submitButton.setBackground(brightRed);
        submitButton.setForeground(Color.WHITE);
        submitButton.addActionListener(e -> {
            filmController.rateMovie(selectedMovie, currentUser, slider.getValue());

            // Update the rating display immediately
            double newUserRating = FilmController.getAverageRating(selectedMovie);
            userRatingLabel.setText("Our Users Rate: " + String.format("%.1f", newUserRating));

            JOptionPane.showMessageDialog(dialog, "Rating submitted!");
            dialog.dispose();
        });

        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(slider);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(submitButton);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void showAddCommentDialog() {
        JDialog dialog = new JDialog(this, "Add Comment", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(darkBackground);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(darkBackground);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel label = new JLabel("Add your comment:");
        label.setFont(new Font("Arial", Font.BOLD, 18));
        label.setForeground(Color.WHITE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea textArea = new JTextArea(5, 30);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton submitButton = new JButton("Submit");
        submitButton.setBackground(brightRed);
        submitButton.setForeground(Color.WHITE);
        submitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        submitButton.addActionListener(e -> {
            String comment = textArea.getText().trim();
            if (!comment.isEmpty()) {
                filmController.addCommentToMovie(selectedMovie, currentUser, comment);
                JOptionPane.showMessageDialog(dialog, "Comment added!");
                dialog.dispose();
            }
        });

        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(scrollPane);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(submitButton);

        // Check if user already commented
        Comment existing = filmController.getLatestUserComment(selectedMovie, currentUser);
        if (existing != null) {
            panel.add(Box.createRigidArea(new Dimension(0, 30)));

            JLabel prevLabel = new JLabel("Previous Comment:");
            prevLabel.setForeground(Color.WHITE);
            prevLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            prevLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel prevText = new JLabel("<html><b>" + existing.getText() + "</b></html>");
            prevText.setForeground(Color.LIGHT_GRAY);
            prevText.setFont(new Font("Arial", Font.PLAIN, 14));
            prevText.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Butonlar için panel oluştur
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            buttonPanel.setBackground(darkBackground);
            buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Edit butonu
            JButton editButton = new JButton("Edit");
            editButton.setBackground(brightRed);
            editButton.setForeground(Color.WHITE);
            editButton.setFocusPainted(false);
            editButton.setFont(new Font("Arial", Font.BOLD, 12));
            editButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

            editButton.addActionListener(ev -> {
                String newText = JOptionPane.showInputDialog(dialog, "Edit your comment:", existing.getText());
                if (newText != null && !newText.trim().isEmpty()) {
                    boolean updated = filmController.editComment(selectedMovie, currentUser, existing.getText(),
                            newText.trim());
                    if (updated) {
                        JOptionPane.showMessageDialog(dialog, "Comment updated!");
                        dialog.dispose(); // optionally reload the dialog
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Failed to update comment.");
                    }
                }
            });

            // Delete butonu
            JButton deleteButton = new JButton("Delete");
            deleteButton.setBackground(brightRed);
            deleteButton.setForeground(Color.WHITE);
            deleteButton.setFocusPainted(false);
            deleteButton.setFont(new Font("Arial", Font.BOLD, 12));
            deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

            deleteButton.addActionListener(ev -> {
                int confirm = JOptionPane.showConfirmDialog(
                        dialog,
                        "Are you sure you want to delete your comment?",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if (confirm == JOptionPane.YES_OPTION) {
                    boolean deleted = filmController.deleteComment(selectedMovie, currentUser, existing.getText());
                    if (deleted) {
                        JOptionPane.showMessageDialog(dialog, "Comment deleted successfully!");
                        dialog.dispose();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Failed to delete comment.");
                    }
                }
            });

            // Butonları panele ekle
            buttonPanel.add(editButton);
            buttonPanel.add(deleteButton);

            panel.add(prevLabel);
            panel.add(Box.createRigidArea(new Dimension(0, 5)));
            panel.add(prevText);
            panel.add(Box.createRigidArea(new Dimension(0, 5)));
            panel.add(buttonPanel);
        }

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void showCommentsDialog() {
        JDialog dialog = new JDialog(this, "Comments", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(darkBackground);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(darkBackground);

        List<Comment> comments = filmController.getCommentsForMovie(selectedMovie);

        if (comments.isEmpty()) {
            JLabel label = new JLabel("No comments yet");
            label.setForeground(Color.WHITE);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(label);
        } else {
            for (Comment comment : comments) {
                JPanel commentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                commentPanel.setBackground(new Color(40, 40, 40));
                commentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                JLabel authorLabel = new JLabel(comment.getAuthor().getUsername() + ": ");
                authorLabel.setForeground(brightRed);
                authorLabel.setFont(new Font("Arial", Font.BOLD, 14));

                JLabel textLabel = new JLabel(comment.getText());
                textLabel.setForeground(Color.WHITE);

                commentPanel.add(authorLabel);
                commentPanel.add(textLabel);
                panel.add(commentPanel);
                panel.add(Box.createRigidArea(new Dimension(0, 5)));
            }
        }

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.getViewport().setBackground(darkBackground);
        dialog.add(scrollPane);
        dialog.setVisible(true);
    }

    private void showAddToListDialog() {
        JDialog dialog = new JDialog(this, "Add to List", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(darkBackground);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(darkBackground);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel label = new JLabel("Select a list:");
        label.setFont(new Font("Arial", Font.BOLD, 18));
        label.setForeground(Color.WHITE);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        List<FilmList> userLists = FilmListController.getAllFilmLists(currentUser);
        String[] listNames = userLists.stream().map(FilmList::getName).toArray(String[]::new);

        JComboBox<String> combo = new JComboBox<>(listNames);
        combo.setMaximumSize(new Dimension(200, 30));

        JButton submitButton = new JButton("Add to List");
        submitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        submitButton.setBackground(brightRed);
        submitButton.setForeground(Color.WHITE);
        submitButton.addActionListener(e -> {
            if (combo.getSelectedItem() != null) {
                String selectedListName = (String) combo.getSelectedItem();
                FilmList selectedList = userLists.stream()
                        .filter(list -> list.getName().equals(selectedListName))
                        .findFirst()
                        .orElse(null);

                if (selectedList != null) {
                    // Film zaten listede mi kontrol et
                    if (selectedList.getMovies().contains(selectedMovie)) {
                        JOptionPane.showMessageDialog(dialog,
                                "This movie is already in " + selectedListName,
                                "Already in List", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        FilmListController.addMovieToList(selectedList, selectedMovie);
                        JOptionPane.showMessageDialog(dialog, "Movie added to " + selectedListName);
                    }
                    dialog.dispose();
                }
            }
        });

        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(combo);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(submitButton);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void navigateToHome() {
        dispose();
        new HomePage(filmController, userController, currentUser);
    }

    private void navigateToExplore() {
        dispose();
        new ExploreFrame(filmController, userController, currentUser);
    }

    private void navigateToMyList() {
        dispose();
        new MyListPanel(currentUser);
    }

    private void navigateToMovies() {
        new MoviesPage(filmController, userController, currentUser);
        dispose();
    }

    private void navigateToProfile() {
        dispose();
        new ProfileFrame(currentUser);
    }
}