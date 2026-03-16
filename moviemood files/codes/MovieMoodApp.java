import javax.swing.*;

public class MovieMoodApp {
    public static void main(String[] args) {
        try {

            UserController userController = new UserController();
            FilmController filmController = new FilmController();
            FilmListController filmListController = new FilmListController();

            System.out.println("Loading movies...");
            MovieSeederWithYoutube.seedMovies(filmController);
            System.out.println("Total " + filmController.getAllMovies().size() + " movies loaded");
            UserController.register("mert@gmail.com", "Ahmet", "Mehmet", "12345678");
            UserController.getAllUsers().get(0).setId(1);
            UserController.register("Musti@gmail.com", "çapkin", "kamu", "12345678");
            UserController.getAllUsers().get(1).setId(0);
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new MovieMoodLoginUI());
    }
}
