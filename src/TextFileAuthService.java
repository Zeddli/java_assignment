import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

// validate user data with the text file
public class TextFileAuthService implements AuthService {

    private final Path usersFile;
    private final TextFileHandler fileHandler;

    public TextFileAuthService(Path usersFile) {

        this.usersFile = usersFile;
        this.fileHandler = new TextFileHandler();
    }

    @Override
    public Optional<User> login(
            String username,
            char[] password) throws IOException {

        List<String[]> records =
                fileHandler.readRecords(usersFile);

        for (String[] fields : records) {

            
            //  userId|role|username|password|fullName|email|phone|extra|status
             
            if (fields.length != 9) {
                throw new IOException(
                        "Invalid users.txt record: expected 9 fields.");
            }

            // look matching username
            if (!fields[2].trim().equals(username)) {
                continue;
            }

            // only activ accounts can log in.
            if (!"ACTIVE".equals(fields[8].trim())) {
                return Optional.empty();
            }

            // Compare plain-text passwords directly.
            if (!passwordMatches(password, fields[3])) {
                return Optional.empty();
            }

            try {

                // Convert the role code into a Role enum.
                Role role = Role.valueOf(
                        fields[1].trim());

                User user = new User(
                        fields[0].trim(),  // userId
                        role,              // role
                        fields[2].trim(),  // username
                        fields[4].trim()   // fullName
                );

                return Optional.of(user);

            } catch (IllegalArgumentException ex) {

                throw new IOException(
                        "Unknown role in users.txt.", ex);
            }
        }

        // No matching user found.
        return Optional.empty();
    }

    /**
     * Compares the entered password with
     * the stored plain-text password.
     */
    private boolean passwordMatches(
            char[] entered, String stored) {

        if (entered.length != stored.length()) {
            return false;
        }

        for (int i = 0; i < entered.length; i++) {

            if (entered[i] != stored.charAt(i)) {
                return false;
            }
        }

        return true;
    }
}