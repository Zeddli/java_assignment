package model;

public class AttendanceOfficer extends User {
    public AttendanceOfficer(String userId, String username, String password, String fullName,
              String email, String phone, String extra, String status) {
        super(userId, Role.ATTENDANCE, username, password, fullName, email, phone, extra, status);
    }

    @Override
    public Role getRole() {
        return Role.ATTENDANCE;
    }
}

