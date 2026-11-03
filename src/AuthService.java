import java.io.IOException;
import java.util.Optional;

/**
 * Authentication contract:
 * login only, no signup or account creation.
 */
public interface AuthService {

    Optional<User> login(
            String username,
            char[] password
    ) throws IOException;
}