package service;
import java.io.IOException;
import java.util.Optional;

import model.User;

// only can login, user creation is by role
public interface AuthService {

    Optional<User> login(
            String username,
            char[] password
    ) throws IOException;
}