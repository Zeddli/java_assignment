package model;

/**
 * User domain and session model combining system properties and role getters.
 */
public class User implements Storable {

    private final String userId;
    private final Role role;
    private final String username;
    private String password;
    private String fullName;
    private String email;
    private String phone;
    private String extra;
    private String status;

    // Friend's 4-parameter constructor (used during active session after login)
    public User(String userId, Role role, String username, String fullName) {
        this(userId, role, username, "", fullName, "", "", "", "ACTIVE");
    }

    // Full constructor (used when loading complete records from users.txt)
    public User(String userId, Role role, String username, String password,
                String fullName, String email, String phone, String extra, String status) {
        this.userId = userId;
        this.role = role;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.extra = extra;
        this.status = status;
    }

    @Override
    public String getId() {
        return userId;
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

    public String getPassword() {
        return password;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getExtra() {
        return extra;
    }

    public String getStatus() {
        return status;
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toLine() {
        String roleStr = (role != null) ? role.toString() : "";
        return String.join("|", userId, roleStr, username, password, fullName, email, phone, extra, status);
    }
}