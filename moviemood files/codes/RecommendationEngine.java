import java.util.*;

public class RecommendationEngine {
    private static List<Movie> allMovies;

    public static void initializeMovies() {
        if (allMovies == null) {
            allMovies = FilmController.getAllMovies();

        }
    }

    // Constructor, başlatma işlemini statik metoda yönlendirir
    public RecommendationEngine() {
        initializeMovies();
    }

    public static List<Movie> recommendMovies(User user) {
        if (allMovies == null) {
            initializeMovies();
        }

        if (allMovies == null || allMovies.isEmpty()) {
            System.out.println("Warning: No movies available for recommendations");
            return new ArrayList<>();
        }

        Map<String, Integer> genreFreq = new HashMap<>();

        if (user.getRecentlyWatched() != null && !user.getRecentlyWatched().isEmpty()) {
            for (Movie m : user.getRecentlyWatched()) {
                for (String g : m.getGenres()) {
                    genreFreq.put(g, genreFreq.getOrDefault(g, 0) + 1);
                }
            }
        } else if (user.getFavoriteMovies() != null && !user.getFavoriteMovies().isEmpty()) {
            for (Movie m : user.getFavoriteMovies()) {
                for (String g : m.getGenres()) {
                    genreFreq.put(g, genreFreq.getOrDefault(g, 0) + 1);
                }
            }
        } else {
            System.out.println("User has no watched or favorite movies. Recommending random selection.");
            List<Movie> randomMovies = new ArrayList<>(allMovies);
            Collections.shuffle(randomMovies);
            return randomMovies.subList(0, Math.min(5, randomMovies.size()));
        }

        List<String> topGenres = genreFreq.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        List<Movie> recommended = new ArrayList<>();
        for (Movie movie : allMovies) {
            if (user.getRecentlyWatched() != null && user.getRecentlyWatched().contains(movie)) {
                continue;
            }
            if (user.getFavoriteMovies() != null && user.getFavoriteMovies().contains(movie)) {
                continue;
            }

            for (String genre : movie.getGenres()) {
                if (topGenres.contains(genre)) {
                    recommended.add(movie);
                    break;
                }
            }
        }

        // Shuffle
        Collections.shuffle(recommended);

        return recommended.subList(0, 20);
    }

}