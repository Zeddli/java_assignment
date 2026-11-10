package model;

/**
 * User roles matching the data contract and UI labels.
 */
public enum Role {

    MANAGER("Manager"),
    ADMIN("Administrator"),
    STUDENT("Student"),
    INSTRUCTOR("Instructor"),
    FACILITIES("Facilities Officer"),
    ATTENDANCE("Attendance Officer");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Converts a string from users.txt (e.g. "ATTENDANCE" or "ADMIN") to a Role enum.
     * Returns null if no matching role is found.
     */
    public static Role fromString(String roleStr) {
        if (roleStr == null) return null;
        for (Role r : Role.values()) {
            if (r.name().equalsIgnoreCase(roleStr.trim())) {
                return r;
            }
        }
        return null;
    }
}