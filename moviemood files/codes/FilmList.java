import java.util.*;

public class FilmList {
    private String name;
    private List<Movie> movies = new ArrayList<>();

    public FilmList(String name) {
        this.name = name;
    }

    public void addMovie(Movie movie) {
        movies.add(movie);
    }

    public List<Movie> getMovies() {
        return movies;
    }

    public void removeMovie(Movie movie) {
        movies.remove(movie);
    }

    public String getName() {
        return name;
    }
}
