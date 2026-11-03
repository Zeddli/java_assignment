/**
 * Details needed by the UI after a user
 * has successfully logged in.
 */
public final class User {

    private final String userId;
    private final Role role;
    private final String username;
    private final String fullName;

    public User(
            String userId,
            Role role,
            String username,
            String fullName) {

        this.userId = userId;
        this.role = role;
        this.username = username;
        this.fullName = fullName;
    }

    public String getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }
}