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
 * MG2: read-only Staff Report with name/role filtering and CSV export.
 * Only the currently filtered/sorted rows and their visible columns are exported.
 */
public class StaffReportPanel extends JPanel {
    private final StaffReportService service = new StaffReportService();
    private final JTextField txtSearch = new JTextField(16);
    private final JComboBox<Role> cmbRole = new JComboBox<>(new Role[]{
            null, Role.ADMIN, Role.INSTRUCTOR, Role.FACILITIES
    });
    private final StatCard adminCard = new StatCard("Administrators");
    private final StatCard instructorCard = new StatCard("Instructors");
    private final StatCard facilitiesCard = new StatCard("Facilities Officers");
    private final StatCard totalCard = new StatCard("Total staff in view");
    private final SortableTable table = new SortableTable(
            "ID", "Name", "Role", "Email", "Phone", "Status");
    private final JLabel lblMessage = new JLabel(" ");
    private boolean dataLoaded;

    public StaffReportPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        buildHeader();
        table.setColumnWidths(65, 120, 125, 160, 100, 75);
        add(new JScrollPane(table), BorderLayout.CENTER);
        lblMessage.setForeground(Color.DARK_GRAY);
        add(lblMessage, BorderLayout.SOUTH);
        addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent event) { refresh(); }
        });
        refresh();
    }

    private void buildHeader() {
        JLabel title = new JLabel("Staff Report");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        JLabel subtitle = new JLabel("View and search administrators, instructors and facilities officers");
        subtitle.setForeground(Color.DARK_GRAY);
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitle);

        cmbRole.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focused) {
                String label = value == null ? "All roles" : ((Role) value).getDisplayName();
                return super.getListCellRendererComponent(list, label, index, selected, focused);
            }
        });

        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        txtSearch.setToolTipText("Search by full name");
        JButton apply = new JButton("Apply");
        JButton reset = new JButton("Reset");
        JButton export = new JButton("Export CSV");
        search.add(new JLabel("Role"));
        search.add(cmbRole);
        search.add(new JLabel("Search by name"));
        search.add(txtSearch);
        search.add(apply);
        search.add(reset);
        search.add(export);
        apply.addActionListener(event -> refresh());
        txtSearch.addActionListener(event -> refresh());
        reset.addActionListener(event -> resetFilters());
        export.addActionListener(event -> {
            if (dataLoaded) ReportCsvExporter.export(this, table, "sdams_staff_report.csv");
        });

        JPanel summaries = new JPanel(new GridLayout(1, 4, 8, 0));
        summaries.add(adminCard);
        summaries.add(instructorCard);
        summaries.add(facilitiesCard);
        summaries.add(totalCard);

        JPanel header = new JPanel(new BorderLayout(0, 10));
        header.add(heading, BorderLayout.NORTH);
        header.add(search, BorderLayout.CENTER);
        header.add(summaries, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);
    }

    private void refresh() {
        dataLoaded = false;
        try {
            List<User> staff = service.search(txtSearch.getText(), (Role) cmbRole.getSelectedItem());
            table.clearRows();
            for (User user : staff) {
                table.addRow(user.getId(), user.getFullName(), user.getRole().getDisplayName(),
                        user.getEmail(), user.getPhone(), user.getStatus());
            }
            adminCard.setValue(String.valueOf(service.countByRole(staff, Role.ADMIN)));
            instructorCard.setValue(String.valueOf(service.countByRole(staff, Role.INSTRUCTOR)));
            facilitiesCard.setValue(String.valueOf(service.countByRole(staff, Role.FACILITIES)));
            totalCard.setValue(String.valueOf(staff.size()));
            lblMessage.setForeground(Color.DARK_GRAY);
            lblMessage.setText(staff.isEmpty() ? "No staff records found."
                    : staff.size() + " staff record" + (staff.size() == 1 ? "" : "s") + " shown");
            dataLoaded = true;
        } catch (DataException ex) {
            table.clearRows();
            adminCard.setValue("0");
            instructorCard.setValue("0");
            facilitiesCard.setValue("0");
            totalCard.setValue("0");
            lblMessage.setForeground(new Color(185, 0, 0));
            lblMessage.setText("Unable to read staff records: " + ex.getMessage());
        }
    }

    private void resetFilters() {
        txtSearch.setText("");
        cmbRole.setSelectedIndex(0);
        refresh();
    }
}
