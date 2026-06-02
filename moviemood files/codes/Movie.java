/*import java.util.*;

public class Movie {
    private String title;

    private List<String> genres;

    private String director;
    private List<String> actors;
    private List<Comment> comments = new ArrayList<>();

    public List<Comment> getComments() {
        return comments;
    }

    public String getDirector() {
        return director;
    }

    private double averageRating;

    public double getAverageRating() {
        if (userRatings.isEmpty())
            return 0.0;
        double total = 0;
        for (double r : userRatings.values()) {
            total += r;
        }
        return total / userRatings.size();
    }

    private String originalTitle;
    private String overview;
    private String language;
    private int releaseDate;
    private String webLink;
    private String youtubeLink;

    public String getYoutubeLink() {
        return youtubeLink;
    }

    public void setYoutubeLink(String youtubeLink) {
        this.youtubeLink = youtubeLink;
    }

    public String getWebLink() {
        return webLink;
    }

    public void setWebLink(String webLink) {
        this.webLink = webLink;
    }

    public int getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(int releaseDate) {
        this.releaseDate = releaseDate;
    }

    private double rating;
    private int voteCount;
    private String posterPath;
    private String backdropPath;
    private double popularity;
    private Map<User, Double> userRatings = new HashMap<>();

    // Constructor with 14 parameters (for MovieSeeder without links)
    public Movie(String title, List<String> genres, String director, List<String> actors, int releaseYear,
            String originalTitle, String overview, String language,
            String releaseDate, double rating, int voteCount,
            String posterPath, String backdropPath, double popularity) {
        this(title, genres, director, actors, releaseYear, originalTitle, overview, language,
             releaseDate, rating, voteCount, posterPath, backdropPath, popularity, null, null);
    }

    // Constructor with 16 parameters (full constructor with links)
    public Movie(String title, List<String> genres, String director, List<String> actors, int releaseYear,
            String originalTitle, String overview, String language,
            String releaseDate, double rating, int voteCount,
            String posterPath, String backdropPath, double popularity, String webLink, String ytLink) {
        this.title = title;
        this.genres = genres;
        this.director = director;
        this.actors = actors;
        this.originalTitle = originalTitle;
        this.overview = overview;
        this.language = language;
        this.releaseDate = releaseYear;
        this.rating = rating;
        this.voteCount = voteCount;
        this.posterPath = posterPath;
        this.backdropPath = backdropPath;
        this.popularity = popularity;
        this.webLink = webLink != null ? webLink : "";
        this.youtubeLink = ytLink != null ? ytLink : "";
    }

    public double getRating() {
        return rating;
    }

    public String getOverview() {
        return overview;
    }

public String getPosterUrl() {
    if (posterPath == null || posterPath.isEmpty()) {
        return null;
    }
    // TMDB API Key ile poster URL
    String apiKey = "b94045aa0416dc579183b96ac34cfb16";
    return "https://image.tmdb.org/t/p/w500" + posterPath + "?api_key=" + apiKey;
}

    public String getBackdropUrl() {
        if (backdropPath == null || backdropPath.isEmpty()) {
            return null;
        }
        // TMDB API Key ile backdrop URL
        String apiKey = "b94045aa0416dc579183b96ac34cfb16";
        return "https://image.tmdb.org/t/p/w780" + backdropPath + "?api_key=" + apiKey;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void addComment(Comment comment) {
        comments.add(comment);
    }
    // public String getRelaseYear(){
    // return releaseDate;
    // }

    public Map<String, Object> getMovie() {
        Map<String, Object> movieMap = new HashMap<>();
        movieMap.put("title", title);
        movieMap.put("originalTitle", originalTitle);
        movieMap.put("overview", overview);
        movieMap.put("language", language);
        movieMap.put("releaseDate", releaseDate);
        movieMap.put("genres", new ArrayList<>(genres)); // preserve as a list
        movieMap.put("rating", rating);
        movieMap.put("actors", actors);
        movieMap.put("director", director);
        movieMap.put("averageRating", averageRating);
        movieMap.put("comments", comments);
        movieMap.put("voteCount", voteCount);
        movieMap.put("posterPath", posterPath);
        movieMap.put("backdropPath", backdropPath);
        movieMap.put("popularity", popularity);
        movieMap.put("posterUrl", getPosterUrl());
        movieMap.put("backdropUrl", getBackdropUrl());

        return movieMap;
    }

    public void rateMovie(User user, double rating) {
        userRatings.put(user, rating); // overrides if user already rated
    }

    public Double getRatingByUser(User user) {
        return userRatings.get(user);
    }

    public double getImdbRating() {
        return rating;
    }

}
*/

import java.util.*;

public class Movie {
    private String title;

    private List<String> genres;

    private String director;
    private List<String> actors;
    private List<Comment> comments = new ArrayList<>();

    public List<Comment> getComments() {
        return comments;
    }

    public String getDirector() {
        return director;
    }

    private double averageRating;

    public double getAverageRating() {
        if (userRatings.isEmpty())
            return 0.0;
        double total = 0;
        for (double r : userRatings.values()) {
            total += r;
        }
        return total / userRatings.size();
    }

    private String originalTitle;
    private String overview;
    private String language;
    private int releaseDate;
    private String webLink;
    private String youtubeLink;

    public String getYoutubeLink() {
        return youtubeLink;
    }

    public void setYoutubeLink(String youtubeLink) {
        this.youtubeLink = youtubeLink;
    }

    public String getWebLink() {
        return webLink;
    }

    public void setWebLink(String webLink) {
        this.webLink = webLink;
    }

    public int getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(int releaseDate) {
        this.releaseDate = releaseDate;
    }

    private double rating;
    private int voteCount;
    private String posterPath;
    private String backdropPath;
    private double popularity;
    private Map<User, Double> userRatings = new HashMap<>();

    // Constructor with 14 parameters (for MovieSeeder without links)
    public Movie(String title, List<String> genres, String director, List<String> actors, int releaseYear,
            String originalTitle, String overview, String language,
            String releaseDate, double rating, int voteCount,
            String posterPath, String backdropPath, double popularity) {
        this(title, genres, director, actors, releaseYear, originalTitle, overview, language,
             releaseDate, rating, voteCount, posterPath, backdropPath, popularity, null, null);
    }

    // Constructor with 16 parameters (full constructor with links)
    public Movie(String title, List<String> genres, String director, List<String> actors, int releaseYear,
            String originalTitle, String overview, String language,
            String releaseDate, double rating, int voteCount,
            String posterPath, String backdropPath, double popularity, String webLink, String ytLink) {
        this.title = title;
        this.genres = genres;
        this.director = director;
        this.actors = actors;
        this.originalTitle = originalTitle;
        this.overview = overview;
        this.language = language;
        this.releaseDate = releaseYear;
        this.rating = rating;
        this.voteCount = voteCount;
        this.posterPath = posterPath;
        this.backdropPath = backdropPath;
        this.popularity = popularity;
        this.webLink = webLink != null ? webLink : "";
        this.youtubeLink = ytLink != null ? ytLink : "";
    }

    public double getRating() {
        return rating;
    }

    public String getOverview() {
        return overview;
    }

    public String getPosterUrl() {
        if (posterPath == null || posterPath.isEmpty()) {
            return null;
        }
        try {
            // TMDB API Key ile poster URL
            String apiKey = "b94045aa0416dc579183b96ac34cfb16";
            String tmdbUrl = "https://image.tmdb.org/t/p/w500" + posterPath + "?api_key=" + apiKey;
            // weserv.nl proxy kullanarak rate limit ve bağlantı sorunlarını çöz
            return "https://images.weserv.nl/?url=" + java.net.URLEncoder.encode(tmdbUrl, "UTF-8");
        } catch (Exception e) {
            // Fallback: direkt TMDB URL
            String apiKey = "b94045aa0416dc579183b96ac34cfb16";
            return "https://image.tmdb.org/t/p/w500" + posterPath + "?api_key=" + apiKey;
        }
    }

    public String getBackdropUrl() {
        if (backdropPath == null || backdropPath.isEmpty()) {
            return null;
        }
        try {
            // TMDB API Key ile backdrop URL
            String apiKey = "b94045aa0416dc579183b96ac34cfb16";
            String tmdbUrl = "https://image.tmdb.org/t/p/w780" + backdropPath + "?api_key=" + apiKey;
            // weserv.nl proxy kullanarak rate limit ve bağlantı sorunlarını çöz
            return "https://images.weserv.nl/?url=" + java.net.URLEncoder.encode(tmdbUrl, "UTF-8");
        } catch (Exception e) {
            // Fallback: direkt TMDB URL
            String apiKey = "b94045aa0416dc579183b96ac34cfb16";
            return "https://image.tmdb.org/t/p/w780" + backdropPath + "?api_key=" + apiKey;
        }
    }

    public String getTitle() {
        return title;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void addComment(Comment comment) {
        comments.add(comment);
    }
    // public String getRelaseYear(){
    // return releaseDate;
    // }

    public Map<String, Object> getMovie() {
        Map<String, Object> movieMap = new HashMap<>();
        movieMap.put("title", title);
        movieMap.put("originalTitle", originalTitle);
        movieMap.put("overview", overview);
        movieMap.put("language", language);
        movieMap.put("releaseDate", releaseDate);
        movieMap.put("genres", new ArrayList<>(genres)); // preserve as a list
        movieMap.put("rating", rating);
        movieMap.put("actors", actors);
        movieMap.put("director", director);
        movieMap.put("averageRating", averageRating);
        movieMap.put("comments", comments);
        movieMap.put("voteCount", voteCount);
        movieMap.put("posterPath", posterPath);
        movieMap.put("backdropPath", backdropPath);
        movieMap.put("popularity", popularity);
        movieMap.put("posterUrl", getPosterUrl());
        movieMap.put("backdropUrl", getBackdropUrl());

        return movieMap;
    }

    public void rateMovie(User user, double rating) {
        userRatings.put(user, rating); // overrides if user already rated
    }

    public Double getRatingByUser(User user) {
        return userRatings.get(user);
    }

    public double getImdbRating() {
        return rating;
    }

}