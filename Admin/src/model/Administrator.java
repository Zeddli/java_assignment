package model;

/**
 * Administrator account (AD1, AD2, AD3).
 * INHERITANCE: reuses every field and getter of User.
 * POLYMORPHISM: overrides getRole() so code holding a User reference gets ADMIN.
 */
public class Administrator extends User {
    public Administrator(String userId, String username, String password, String fullName,
                         String email, String phone, String status) {
        super(userId, Role.ADMIN, username, password, fullName, email, phone, "", status);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }
}
