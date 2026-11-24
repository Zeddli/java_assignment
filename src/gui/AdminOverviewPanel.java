package gui;

import java.awt.*;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import model.Role;
import service.AccountService;
import service.OverviewService;
import service.OverviewService.MonthTotal;
import service.OverviewService.RoleCount;
import util.DataException;

/**
 * Interactive feature: the Administrator's home screen.
 * Six clickable cards summarise accounts and this month's payments; each one opens the matching
 * screen already filtered. Below, a bar chart shows payments for the last six months; clicking a
 * bar opens Payment Records for that month. Numbers reload every time the screen is opened.
 */
public class AdminOverviewPanel extends JPanel {
    private static final int MONTHS = 6;

    private final OverviewService service = new OverviewService();
    private final WelcomeHeader welcome;
    private final MonthlyBarChart chart = new MonthlyBarChart();
    private final JLabel lblError = new JLabel(" ");
    private final StatTile tStudents, tInstructors, tFacilities, tAttendance, tInactive, tPayments;

    public AdminOverviewPanel(String adminId, AdminNavigator nav) {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        welcome = new WelcomeHeader(adminId, "Overview",
                "Your academy at a glance. Click a card or a bar to open the matching records.");
        add(welcome, BorderLayout.NORTH);

        // Each card's action is a lambda: the card does not know which screen it opens.
        tStudents = new StatTile("Active students", "Open active students",
                () -> nav.openStudents(AccountService.ACTIVE));
        tInstructors = new StatTile("Instructors", "Open instructors",
                () -> nav.openStaff(Role.INSTRUCTOR, null));
        tFacilities = new StatTile("Facilities officers", "Open facilities officers",
                () -> nav.openStaff(Role.FACILITIES, null));
        tAttendance = new StatTile("Attendance officers", "Open attendance officers",
                () -> nav.openStaff(Role.ATTENDANCE, null));
        tInactive = new StatTile("Inactive staff", "Open inactive staff",
                () -> nav.openStaff(null, AccountService.INACTIVE));
        tPayments = new StatTile("Payments this month", "Open this month's payments", () -> {
            LocalDate today = LocalDate.now();
            nav.openPayments(today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()));
        });

        JPanel grid = new JPanel(new GridLayout(2, 3, 10, 10));
        for (StatTile t : new StatTile[]{tStudents, tInstructors, tFacilities, tAttendance, tInactive, tPayments}) {
            grid.add(t);
        }

        JPanel chartHeader = new JPanel(new BorderLayout());
        chartHeader.add(AdminTheme.sectionTitle("Payments per month"), BorderLayout.WEST);
        chartHeader.add(AdminTheme.muted("Last " + MONTHS + " months · hover for details, click to open"),
                BorderLayout.EAST);

        chart.setOnBarClicked(m -> nav.openPayments(m.firstDay(), m.lastDay()));
        JPanel chartArea = new JPanel(new BorderLayout(0, 6));
        chartArea.add(chartHeader, BorderLayout.NORTH);
        chartArea.add(chart, BorderLayout.CENTER);
        lblError.setForeground(AdminTheme.ERROR);
        chartArea.add(lblError, BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.add(grid, BorderLayout.NORTH);
        body.add(chartArea, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) { refresh(); }
        });
        refresh();
    }

    /** Reloads every number from the text files. */
    public final void refresh() {
        welcome.refresh();
        lblError.setText(" ");
        try {
            RoleCount students = service.countRole(Role.STUDENT);
            tStudents.setValue(String.valueOf(students.getActive()),
                    students.getInactive() + " inactive · " + students.getTotal() + " in total");
            showRole(tInstructors, service.countRole(Role.INSTRUCTOR));
            showRole(tFacilities, service.countRole(Role.FACILITIES));
            showRole(tAttendance, service.countRole(Role.ATTENDANCE));

            int inactiveStaff = service.countInactiveStaff();
            tInactive.setValue(String.valueOf(inactiveStaff),
                    inactiveStaff == 0 ? "Everyone can log in" : "Cannot log in until activated");

            LocalDate today = LocalDate.now();
            MonthTotal month = service.thisMonth(today);
            String monthName = today.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            tPayments.setValue("RM " + String.format("%,.2f", month.getTotal()),
                    month.getCount() + " payment" + (month.getCount() == 1 ? "" : "s") + " in " + monthName);

            List<MonthTotal> months = service.monthlyTotals(MONTHS, today);
            chart.setData(months);
        } catch (DataException ex) {
            lblError.setText("Could not read the data files: " + ex.getMessage());
        }
    }

    private static void showRole(StatTile tile, RoleCount c) {
        tile.setValue(String.valueOf(c.getActive()),
                c.getInactive() == 0 ? "All active" : c.getInactive() + " inactive");
    }
}
