
package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import model.Role;
import model.User;
import service.StaffReportService;
import util.DataException;

/**
 * MG2 - Staff Report.
 *
 * Read-only report displaying:
 * - Administrators
 * - Instructors
 * - Facilities Officers
 *
 * Supports filtering by name and role.
 * Summary cards update with filtered results.
 */
public class StaffReportPanel extends JPanel {

    private final StaffReportService service =
            new StaffReportService();

    // Filter components.
    private final JTextField txtSearch =
            new JTextField(16);

    private final JComboBox<Role> cmbRole =
            new JComboBox<>(new Role[] {
                    null,
                    Role.ADMIN,
                    Role.INSTRUCTOR,
                    Role.FACILITIES
            });

    // Summary cards.
    private final StatCard adminCard =
            new StatCard("Administrators");

    private final StatCard instructorCard =
            new StatCard("Instructors");

    private final StatCard facilitiesCard =
            new StatCard("Facilities Officers");

    private final StatCard totalCard =
            new StatCard("Total staff in view");

    // Staff table.
    private final SortableTable table =
            new SortableTable(
                    "ID",
                    "Name",
                    "Role",
                    "Email",
                    "Phone",
                    "Status"
            );

    private final JLabel lblMessage = new JLabel(" ");

    public StaffReportPanel() {

        setLayout(new BorderLayout(10, 10));

        setBorder(BorderFactory.createEmptyBorder(
                12, 14, 12, 14));

        // Build the interface.
        buildHeader();
        buildTable();

        // Reload records when this module is opened.
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentShown(ComponentEvent event) {
                refresh();
            }
        });

        // Initial data load.
        refresh();
    }

    /**
     * Build heading, search controls and summary cards.
     */
    private void buildHeader() {

        // Report heading.
        JLabel title = new JLabel("Staff Report");

        title.setFont(
                title.getFont().deriveFont(Font.BOLD, 20f));

        JLabel subtitle = new JLabel(
                "View and search administrators, "
                + "instructors and facilities officers");

        subtitle.setForeground(Color.DARK_GRAY);

        JPanel heading = new JPanel();
        heading.setLayout(
                new BoxLayout(heading, BoxLayout.Y_AXIS));

        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitle);

        // Display readable names in the role dropdown.
        cmbRole.setRenderer(new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean selected,
                    boolean focused) {

                String label = value == null
                        ? "All roles"
                        : ((Role) value).getDisplayName();

                return super.getListCellRendererComponent(
                        list, label, index, selected, focused);
            }
        });

        // Search controls.
        JPanel filters = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 8, 0));

        txtSearch.setToolTipText(
                "Search by full name");

        JButton btnApply = new JButton("Apply");
        JButton btnReset = new JButton("Reset");

        filters.add(new JLabel("Filter by role"));
        filters.add(cmbRole);

        filters.add(new JLabel("Search by name"));
        filters.add(txtSearch);

        filters.add(btnApply);
        filters.add(btnReset);

        // Apply selected filters.
        btnApply.addActionListener(event -> refresh());

        // Pressing Enter in search field applies filters.
        txtSearch.addActionListener(event -> refresh());

        // Reset all filters.
        btnReset.addActionListener(event -> resetFilters());

        // Summary cards, side by side.
        JPanel summaries =
                new JPanel(new GridLayout(1, 4, 8, 0));

        summaries.add(adminCard);
        summaries.add(instructorCard);
        summaries.add(facilitiesCard);
        summaries.add(totalCard);

        // Combine header components.
        JPanel header = new JPanel(new BorderLayout(0, 10));

        header.add(heading, BorderLayout.NORTH);
        header.add(filters, BorderLayout.CENTER);
        header.add(summaries, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);
    }

    /**
     * Build the read-only staff table.
     */
    private void buildTable() {

        table.setColumnWidths(
                65, 120, 125, 160, 100, 75);

        // Table scrolling.
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Display the current result count or an error.
        lblMessage.setForeground(Color.DARK_GRAY);
        add(lblMessage, BorderLayout.SOUTH);
    }

    /**
     * Reload staff records with the selected filters.
     * Update the table and summary totals together.
     */
    private void refresh() {

        String name = txtSearch.getText();
        Role selectedRole =
                (Role) cmbRole.getSelectedItem();

        try {

            // Retrieve filtered staff records.
            List<User> staff =
                    service.search(name, selectedRole);

            // Clear previous table rows.
            table.clearRows();

            // Populate the table.
            for (User user : staff) {

                table.addRow(
                        user.getId(),
                        user.getFullName(),
                        user.getRole().getDisplayName(),
                        user.getEmail(),
                        user.getPhone(),
                        user.getStatus()
                );
            }

            // Update totals using filtered records.
            adminCard.setValue(String.valueOf(
                    service.countByRole(staff, Role.ADMIN)));

            instructorCard.setValue(String.valueOf(
                    service.countByRole(staff, Role.INSTRUCTOR)));

            facilitiesCard.setValue(String.valueOf(
                    service.countByRole(staff, Role.FACILITIES)));

            totalCard.setValue(
                    String.valueOf(staff.size()));

            // Show result information.
            if (staff.isEmpty()) {

                lblMessage.setText("No staff records found.");

            } else {

                lblMessage.setText(
                        staff.size() + " staff record"
                        + (staff.size() == 1 ? "" : "s")
                        + " shown");
            }

            lblMessage.setForeground(Color.DARK_GRAY);

        } catch (DataException ex) {

            // Do not leave stale report data visible.
            table.clearRows();

            adminCard.setValue("0");
            instructorCard.setValue("0");
            facilitiesCard.setValue("0");
            totalCard.setValue("0");

            lblMessage.setForeground(
                    new Color(185, 0, 0));

            lblMessage.setText(
                    "Unable to read staff records: "
                    + ex.getMessage());
        }
    }

    /**
     * Restore the original unfiltered report.
     */
    private void resetFilters() {

        txtSearch.setText("");
        cmbRole.setSelectedIndex(0);

        refresh();
    }
}
