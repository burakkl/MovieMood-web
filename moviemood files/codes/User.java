import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class User {
    private String firstname;
    private String lastname;
    private String email;

    public String getEmail() {
        return email;
    }

    private String passwordHash;
    private String profilePicturePath; // başlangıç değeri constructor'da atanacak

    public void setProfilePicturePath(String profilePicturePath) {
        this.profilePicturePath = profilePicturePath;
    }

    private int userId; // unique 4-digit ID

    public String getPasswordHash() {
        return passwordHash;
    }

    private List<Movie> recentlyWatched = new ArrayList<>();
    private List<FilmList> filmLists = new ArrayList<>();
    private List<User> friends = new ArrayList<>();
    private List<Movie> recommendedMovies = new ArrayList<>();
    private Set<Movie> favoriteMovies = new HashSet<>();

    public User(String firstname, String lastname, String email, String passwordHash, int userId) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.passwordHash = passwordHash;
        this.userId = userId;

        // Rastgele profil fotoğrafı seçimi
        this.profilePicturePath = getRandomProfilePicture();
    }

    // Rastgele profil fotoğrafı seçen yardımcı metod
    private String getRandomProfilePicture() {
        Random random = new Random();
        int imageNumber = random.nextInt(7) + 1; // 1 ile 7 arasında (7 dahil) rastgele bir sayı
        return "images/" + imageNumber + ".jpg"; // JPG uzantısı kullanıldı
    }

    public void watchMovie(Movie movie) {
        if (!recentlyWatched.contains(movie)) {
            recentlyWatched.add(movie);
            if (recentlyWatched.size() > 20)
                recentlyWatched.remove(0); // keep recent 20
        }
    }

    public void addFriend(User friend) {
        if (!friends.contains(friend))
            friends.add(friend);
    }

    public int getUserId() {
        return userId;
    }

    public String getProfilePicturePath() {
        return profilePicturePath;
    }

    public void addFilmList(FilmList list) {
        filmLists.add(list);
    }

    public List<Movie> getRecentlyWatched() {
        return recentlyWatched;
    }

    public List<User> getFriends() {
        return friends;
    }

    public String getUsername() {
        return firstname;
    }

    public List<FilmList> getFilmLists() {
        return filmLists;
    }

    public List<Movie> getRecommendedMovies() {
        return recommendedMovies;
    }

    public void setRecommendedMovies() {
        RecommendationEngine recommendationEngine = new RecommendationEngine();
        this.recommendedMovies = recommendationEngine.recommendMovies(this);
    }

    public void addFavorite(Movie movie) {
        favoriteMovies.add(movie);
    }

    public void removeFavorite(Movie movie) {
        favoriteMovies.remove(movie);
    }

    public boolean isFavorite(Movie movie) {
        return favoriteMovies.contains(movie);
    }

    public Set<Movie> getFavoriteMovies() {
        return favoriteMovies;
    }

    public void setId(int id) {
        this.userId = id;
    }
}