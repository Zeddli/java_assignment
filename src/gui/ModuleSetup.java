package gui;

import model.Role;
import model.User;

/**
 * Registers completed role modules with the dashboard.
 *
 * Each team contributes its own JPanel.
 * No modules are registered by default.
 */
public final class ModuleSetup {

    private ModuleSetup() {
        // Prevent object creation.
    }

    public static void register(DashboardFrame dashboard, User user) {

        // Everyone registers their modules here.

        // Manager Modules
        if (Role.MANAGER.equals(user.getRole())) {
            dashboard.registerModule(
                    "Revenue Report",
                    new RevenueReportPanel()
            );
        }

        // Attendance Officer Modules
        if (Role.ATTENDANCE.equals(user.getRole())) {
            dashboard.registerModule(
                    "Manage Class Attendance",
                    new ManageAttendancePanel(user)
            );
            dashboard.registerModule(
                    "Attendance Records",
                    new AttendanceRecordsPanel()
            );
            dashboard.registerModule(
                    "Attendance Report",
                    new AttendanceReportPanel()
            );
        }

        // Uncomment and update as team members finish their respective role screens:

        // if (Role.ADMIN.equals(user.getRole())) {
        //     dashboard.registerModule("Manage Staff", new ManageStaffPanel());
        // }
        // if (Role.STUDENT.equals(user.getRole())) {
        //     dashboard.registerModule("Book Class", new BookClassPanel());
        // }
        // if (Role.INSTRUCTOR.equals(user.getRole())) {
        //     dashboard.registerModule("View Schedule", new InstructorSchedulePanel());
        // }
        // if (Role.FACILITIES.equals(user.getRole())) {
        //     dashboard.registerModule("Facility Slots", new FacilitiesPanel());
        // }
    }
}
