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

    public static void register(
            DashboardFrame dashboard,
            User user) {

        // Team members register their modules here.
        // Keep empty until modules are completed.

        if (user.getRole() == Role.MANAGER) {

            dashboard.registerModule(
                    "Revenue Report",
                    new RevenueReportPanel()); 
        }
        // if (user.getRole() == Role.ADMINISTRATOR) {

        //     dashboard.registerModule(
        //             "Revenue Report",
        //             new RevenueReportPanel()); Your own module
        // }
        // if (user.getRole() == Role.STUDENT) {

        //     dashboard.registerModule(
        //             "Revenue Report",
        //             new RevenueReportPanel()); Your own module
        // }
        // if (user.getRole() == Role.MANAGER) {

        //     dashboard.registerModule(
        //             "Revenue Report",
        //             new RevenueReportPanel());
        // }
        // if (user.getRole() == Role.MANAGER) {

        //     dashboard.registerModule(
        //             "Revenue Report",
        //             new RevenueReportPanel());
        // }
    }
}