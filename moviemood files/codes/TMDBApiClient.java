import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class TMDBApiClient {
    private static final String API_KEY = "b94045aa0416dc579183b96ac34cfb16";
    private static final String BASE_URL = "https://api.themoviedb.org/3";

    /**
     * TMDB API'den filmler yükle ve database'e kaydet
     * 
     * @param filmController Film controller
     * @param pageCount      Kaç sayfa yüklenecek (her sayfa 20 film)
     */
    public static void loadMovies(FilmController filmController, int pageCount) {
        System.out.println("Loading movies from TMDB API...");

        int totalMovies = 0;

        // Popular filmler
        System.out.println("Loading Popular movies...");
        totalMovies += loadPopularMovies(filmController, pageCount / 3);

        // Top rated filmler
        System.out.println("Loading Top Rated movies...");
        totalMovies += loadTopRatedMovies(filmController, pageCount / 3);

        // Now playing filmler
        System.out.println("Loading Now Playing movies...");
        totalMovies += loadNowPlayingMovies(filmController, pageCount / 3);

        System.out.println("Total " + totalMovies + " movies loaded from TMDB API!");
    }

    /**
     * Popular filmleri yükle
     */
    public static int loadPopularMovies(FilmController filmController, int pageCount) {
        return loadMoviesFromEndpoint(filmController, "/movie/popular", pageCount);
    }

    /**
     * Top rated filmleri yükle
     */
    public static int loadTopRatedMovies(FilmController filmController, int pageCount) {
        return loadMoviesFromEndpoint(filmController, "/movie/top_rated", pageCount);
    }

    /**
     * Now playing filmleri yükle
     */
    public static int loadNowPlayingMovies(FilmController filmController, int pageCount) {
        return loadMoviesFromEndpoint(filmController, "/movie/now_playing", pageCount);
    }

    /**
     * Belirtilen endpoint'ten filmleri yükle
     */
    private static int loadMoviesFromEndpoint(FilmController filmController, String endpoint, int pageCount) {
        int loadedCount = 0;

        // Sistem proxy ayarlarını kullan (VPN için gerekli)
        System.setProperty("java.net.useSystemProxies", "true");

        for (int page = 1; page <= pageCount; page++) {
            try {
                String urlString = BASE_URL + endpoint + "?api_key=" + API_KEY + "&page=" + page + "&language=en-US";
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000); // 15 saniye timeout (VPN için biraz daha uzun)
                conn.setReadTimeout(15000);

                // User-Agent ekle (bazı API'ler gerektirir)
                conn.setRequestProperty("User-Agent", "MovieMood/1.0");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    // JSON parse et
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray results = jsonResponse.getJSONArray("results");

                    // Her filmi işle
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject movieJson = results.getJSONObject(i);
                        Movie movie = parseMovieFromJson(movieJson);

                        if (movie != null) {
                            filmController.addMovie(movie);
                            loadedCount++;
                        }
                    }

                    System.out.println("Page " + page + "/" + pageCount + " loaded (" + results.length() + " movies)");

                    // Rate limit için kısa bekleme
                    Thread.sleep(250); // 250ms = saniyede 4 istek (TMDB limiti)

                } else {
                    System.err.println("TMDB API error: " + responseCode);
                    break;
                }

            } catch (Exception e) {
                System.err.println("Movie loading error (page " + page + "): " + e.getMessage());
                e.printStackTrace();
                break;
            }
        }

        return loadedCount;
    }

    /**
     * JSON'dan Movie nesnesi oluştur
     */
    private static Movie parseMovieFromJson(JSONObject json) {
        try {
            String title = json.optString("title", "Unknown");
            String originalTitle = json.optString("original_title", title);
            String overview = json.optString("overview", "");
            String language = json.optString("original_language", "en");
            String releaseDate = json.optString("release_date", "");
            String posterPath = json.optString("poster_path", "");
            String backdropPath = json.optString("backdrop_path", "");
            double rating = json.optDouble("vote_average", 0.0);
            int voteCount = json.optInt("vote_count", 0);
            double popularity = json.optDouble("popularity", 0.0);

            // Release year
            int releaseYear = 0;
            if (!releaseDate.isEmpty() && releaseDate.length() >= 4) {
                try {
                    releaseYear = Integer.parseInt(releaseDate.substring(0, 4));
                } catch (Exception e) {
                    releaseYear = 0;
                }
            }

            // Genres
            List<String> genres = new ArrayList<>();
            JSONArray genreIds = json.optJSONArray("genre_ids");
            if (genreIds != null) {
                for (int i = 0; i < genreIds.length(); i++) {
                    int genreId = genreIds.getInt(i);
                    String genreName = getGenreName(genreId);
                    if (genreName != null) {
                        genres.add(genreName);
                    }
                }
            }

            // IMDB link (film ID'den oluştur)
            int tmdbId = json.optInt("id", 0);
            String webLink = "https://www.imdb.com/title/tt" + tmdbId; // Yaklaşık

            // Movie nesnesi oluştur
            Movie movie = new Movie(
                    title,
                    genres,
                    "Unknown", // Director - API'den ayrıca çekilmeli
                    new ArrayList<>(), // Actors - API'den ayrıca çekilmeli
                    releaseYear,
                    originalTitle,
                    overview,
                    language,
                    releaseDate,
                    rating,
                    voteCount,
                    posterPath,
                    backdropPath,
                    popularity,
                    webLink,
                    "" // YouTube link - ayrıca çekilmeli
            );

            return movie;

        } catch (Exception e) {
            System.err.println("Movie parsing error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Genre ID'den genre adını al
     */
    private static String getGenreName(int genreId) {
        switch (genreId) {
            case 28:
                return "Action";
            case 12:
                return "Adventure";
            case 16:
                return "Animation";
            case 35:
                return "Comedy";
            case 80:
                return "Crime";
            case 99:
                return "Documentary";
            case 18:
                return "Drama";
            case 10751:
                return "Family";
            case 14:
                return "Fantasy";
            case 36:
                return "History";
            case 27:
                return "Horror";
            case 10402:
                return "Music";
            case 9648:
                return "Mystery";
            case 10749:
                return "Romance";
            case 878:
                return "Science Fiction";
            case 10770:
                return "TV Movie";
            case 53:
                return "Thriller";
            case 10752:
                return "War";
            case 37:
                return "Western";
            default:
                return null;
        }
    }

    /**
     * Yeni çıkan filmleri güncelle (arka planda)
     */
    public static void updateNewMovies(FilmController filmController) {
        System.out.println("Checking for new movies...");
        // Sadece 1-2 sayfa yeni film ekle
        loadNowPlayingMovies(filmController, 2);
    }
}