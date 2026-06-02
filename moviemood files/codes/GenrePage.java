import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

public class GenrePage extends JFrame {
    private FilmController filmController;
    private UserController userController;
    private User currentUser;

    public GenrePage(FilmController filmController, UserController userController, User currentUser, String label, int startYear, int endYear) {
        this.filmController = filmController;
        this.userController = userController;
        this.currentUser = currentUser;
        
        List<Movie> movies = filmController.searchByReleaseYearInterval(startYear, endYear);
        initializeUI(label + " Movies", movies);
    }
    public GenrePage(FilmController filmController, UserController userController, User currentUser, String genre) {
        this.filmController = filmController;
        this.userController = userController;
        this.currentUser = currentUser;
        
        List<Movie> movies = filmController.searchByGenre(genre);
        initializeUI(genre.toUpperCase() + " Movies", movies);
    }

    private void initializeUI(String titleText, List<Movie> movies) {
        setTitle("Movie Mood - " + titleText);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setSize(1200, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Top panel
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.BLACK);
        header.setPreferredSize(new Dimension(900, 40));

        JButton back = new JButton("BACK");
        back.setForeground(Color.WHITE);
        back.setBackground(Color.RED);
        back.addActionListener(e -> {
            new MoviesPage(filmController, userController, currentUser);
            dispose();
        });

        JLabel title = new JLabel(titleText, SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 16));

        header.add(back, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // Poster panel
        JPanel moviePanel = new JPanel();
        moviePanel.setBackground(Color.BLACK);
        moviePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 20));

        // First 20 movies for speed
        int count = 0;
        for (Movie movie : movies) {
            if (count >= 20) break;
            try {
                String imageUrl = movie.getPosterUrl();
                ImageIcon icon = loadImageFromURL(imageUrl);

                Image scaled = icon.getImage().getScaledInstance(120, 180, Image.SCALE_SMOOTH);
                JLabel poster = new JLabel(new ImageIcon(scaled));
                poster.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                poster.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        new MovieMoodGUI(filmController, userController, currentUser, movie);
                        dispose();
                    }
                });

                moviePanel.add(poster);
                count++;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        JScrollPane scrollPane = new JScrollPane(moviePanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        setVisible(true);
    }

    // URL'den resim çekme metodu
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
}