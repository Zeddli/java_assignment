package gui;

import java.time.LocalDate;
import model.Role;
import model.User;

/**
 * Builds and registers every Administrator screen in the shared dashboard.
 * Kept in its own class so the shared ModuleSetup needs only ONE line for the Admin role,
 * which keeps merges with teammates simple.
 *
 * Menu: Overview, Manage Staff (AD1), Manage Students (AD2), Payment Records (AD3), My Profile.
 */
public final class AdminModules {
    public static final String OVERVIEW = "Overview";
    public static final String STAFF = "Manage Staff";
    public static final String STUDENTS = "Manage Students";
    public static final String PAYMENTS = "Payment Records";
    public static final String PROFILE = "My Profile";

    private AdminModules() { }

    public static void register(DashboardFrame dashboard, User admin) {
        String id = admin.getId();
        ManageStaffPanel staff = new ManageStaffPanel(id);
        ManageStudentsPanel students = new ManageStudentsPanel(id);
        PaymentRecordsPanel payments = new PaymentRecordsPanel(id);

        // The Overview only sees this interface; here it is connected to the real screens.
        AdminNavigator nav = new AdminNavigator() {
            @Override
            public void openStaff(Role role, String status) {
                staff.applyFilters(role, status);
                dashboard.showModule(STAFF);
            }

            @Override
            public void openStudents(String status) {
                students.applyFilters(null, status);
                dashboard.showModule(STUDENTS);
            }

            @Override
            public void openPayments(LocalDate from, LocalDate to) {
                payments.showRange(from, to);
                dashboard.showModule(PAYMENTS);
            }
        };

        dashboard.registerModule(OVERVIEW, new AdminOverviewPanel(id, nav)); // first = shown after login
        dashboard.registerModule(STAFF, staff);
        dashboard.registerModule(STUDENTS, students);
        dashboard.registerModule(PAYMENTS, payments);
        dashboard.registerModule(PROFILE, new AdminProfilePanel(id));
    }
}
