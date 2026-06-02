import javax.swing.*;

public class MainDB {
    public static void main(String[] args) {
        // Start Database
        System.out.println("Starting MovieMood...");
        DatabaseManager.initialize();

        // Film verilerini yükle veya zaten yüklüyse atla
        loadMoviesIfNeeded();

        // GUI'yi başlat
        SwingUtilities.invokeLater(() -> {
            try {
                // Login ekranını göster
                MovieMoodLoginUI loginUI = new MovieMoodLoginUI();
                loginUI.setVisible(true);

                System.out.println("MovieMood started successfully!");
            } catch (Exception e) {
                System.err.println("GUI initialization error:");
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "An error occurred while starting the application: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        // Uygulama kapandığında database bağlantısını kapat
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down application...");
            DatabaseManager.close();
        }));
    }

    // Filmler yüklü değilse yükle
    private static void loadMoviesIfNeeded() {
        // Database'de film var mı kontrol et
        int currentMovieCount = FilmController.getAllMovies().size();

        if (currentMovieCount < 100) {
            System.out.println("Starting initial movie load...");
            System.out.println("This process may take 2-3 minutes, please wait...");

            System.out.println("\n[1/2] Loading manual movies...");
            MovieSeederWithYoutube.seedMovies(new FilmController());
            System.out.println("✓ " + FilmController.getAllMovies().size() + " manual movies loaded");

            System.out.println("\n[2/2] Loading movies from TMDB API...");
            System.out.println("This step might take a while...");

            TMDBApiClient.loadMovies(new FilmController(), 45);

            System.out.println("\n✓ Total " + FilmController.getAllMovies().size() + " movies loaded!");
            System.out.println("Database ready! Next startups will be much faster.\n");

        } else if (currentMovieCount < 1000) {
            System.out.println("Completing missing movies...");
            int needed = (1000 - currentMovieCount) / 20 + 1;
            TMDBApiClient.loadMovies(new FilmController(), needed);
            System.out.println("✓ Total " + FilmController.getAllMovies().size() + " movies loaded");
        } else {
            System.out.println("✓ Movies loaded from database (" + currentMovieCount + " movies)");
            System.out.println("Load time: ~2 seconds");
        }
    }

    public static void resetDatabase() {
        DatabaseManager.clearDatabase();
        System.out.println("Database reset. Restarting application...");
        loadMoviesIfNeeded();
    }
}