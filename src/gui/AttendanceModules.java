package gui;

import model.User;

/**
 * Builds and registers every Attendance Officer screen in the shared dashboard.
 * Kept in its own class so the shared ModuleSetup needs only ONE line for this role
 * (same idea as AdminModules), which keeps merges with teammates simple.
 *
 * Menu: Overview (additional feature), Manage Class Attendance (AO1),
 *       Attendance Records (AO2), Attendance Report (AO3).
 */
public final class AttendanceModules {
    public static final String OVERVIEW = "Overview";
    public static final String MANAGE = "Manage Class Attendance";
    public static final String RECORDS = "Attendance Records";
    public static final String REPORT = "Attendance Report";

    private AttendanceModules() { }

    public static void register(DashboardFrame dashboard, User officer) {
        String id = officer.getId();
        ManageAttendancePanel manage = new ManageAttendancePanel(officer);

        // The Overview opens a class in Manage Class Attendance through this small callback.
        dashboard.registerModule(OVERVIEW, new AttendanceOverviewPanel(id, classId -> {
            manage.showClass(classId);
            dashboard.showModule(MANAGE);
        })); // first = shown after login
        dashboard.registerModule(MANAGE, manage);
        dashboard.registerModule(RECORDS, new AttendanceRecordsPanel(id));
        dashboard.registerModule(REPORT, new AttendanceReportPanel(id));
    }
}
