package service;

import model.Role;

/**
 * What the user typed into a staff or student form, passed from the GUI to the service.
 * Keeping it in one object means the GUI never builds User objects or touches files itself.
 * {@code extra} is the discipline for instructors and the date of birth for students.
 */
public class AccountForm {
    // Field keys, so a ValidationException can say which box is wrong.
    public static final String ROLE = "role";
    public static final String FULL_NAME = "fullName";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String EMAIL = "email";
    public static final String PHONE = "phone";
    public static final String EXTRA = "extra";

    private final Role role;
    private final String fullName;
    private final String username;
    private final String password;
    private final String email;
    private final String phone;
    private final String extra;

    public AccountForm(Role role, String fullName, String username, String password,
                       String email, String phone, String extra) {
        this.role = role;
        this.fullName = trim(fullName);
        this.username = trim(username);
        this.password = password == null ? "" : password;
        this.email = trim(email);
        this.phone = trim(phone);
        this.extra = trim(extra);
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }

    public Role getRole() { return role; }
    public String getFullName() { return fullName; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getExtra() { return extra; }
}
