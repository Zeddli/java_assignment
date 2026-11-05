package repository;

import model.Role;
import model.User;

public class UserRepository extends BaseRepository<User> {
    public UserRepository() {
        super("users.txt");
    }

    /** Factory: builds a User from the role in the file. */
    public static User create(String role, String id, String username, String password, String fullName,
                              String email, String phone, String extra, String status) {
        Role resolved = Role.fromString(role);
        if (resolved == null) {
            throw new IllegalArgumentException("Unknown role " + role);
        }
        return new User(id, resolved, username, password, fullName, email, phone, extra, status);
    }

    @Override
    protected User fromFields(String[] f) {
        return create(f[1], f[0], f[2], f[3], f[4], f[5], f[6], f[7], f[8]);
    }

    /** Returns the user if username + password match and the account is ACTIVE, else null. */
    public User authenticate(String username, String password) {
        for (User u : findAll()) {
            if (u.getUsername().equals(username) && u.getPassword().equals(password) && u.isActive()) return u;
        }
        return null;
    }

    public String nameOf(String userId) {
        User u = findById(userId);
        return u == null ? userId : u.getFullName();
    }
}

