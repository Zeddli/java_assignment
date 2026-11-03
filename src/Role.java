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
}