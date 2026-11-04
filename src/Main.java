import java.nio.file.Paths;
import javax.swing.SwingUtilities;

// start the project in main
public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            AuthService authService = new TextFileAuthService(
                    Paths.get("data", "users.txt"));

            new LoginFrame(authService).setVisible(true);
        });
    }
}