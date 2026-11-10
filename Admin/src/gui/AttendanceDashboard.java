package gui;

import java.awt.Color;
import model.User;
 
/** Attendance Officer window: AO1, AO2, AO3. */
public class AttendanceDashboard extends BaseDashboard {
    public AttendanceDashboard(User user) {
        super(user, "Attendance Officer", new Color(166, 45, 74));
        addModule("Manage Class Attendance", new ManageAttendancePanel(user));
        addModule("Attendance Records", new AttendanceRecordsPanel());
        addModule("Attendance Report", new AttendanceReportPanel());
    }
}

