package gui;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import javax.swing.*;
import service.AttendanceEntry;
import service.AttendanceService;
import util.Validator;

/**
 * AO2: view and search attendance records (read-only).
 * Filters (student, class, instructor, discipline, date, status) combine with AND.
 * Click a column header to sort; Export CSV saves what is shown.
 */
public class AttendanceRecordsPanel extends JPanel {
    private final AttendanceService service = new AttendanceService();
    private final AttendanceHeader header;
    private final JTextField txtStudent = new JTextField(9);
    private final JTextField txtClass = new JTextField(6);
    private final JTextField txtInstructor = new JTextField(9);
    private final JComboBox<String> cmbDiscipline =
            new JComboBox<>(new String[]{"All", "Karate", "Judo", "Taekwondo", "MMA"});
    private final JComboBox<String> cmbStatus =
            new JComboBox<>(new String[]{"All", "PRESENT", "LATE", "ABSENT"});
    private final JTextField txtDate = new JTextField(8);
    private final JLabel lblMessage = new JLabel(" ");
    private final JLabel lblCount = new JLabel(" ");
    private final TypedTableModel model = new TypedTableModel(
            "Record", "Date", "Class", "Discipline", "Instructor", "Student", "Status");
    private final JTable table = new JTable(model);

    public AttendanceRecordsPanel() { this(null); }

    public AttendanceRecordsPanel(String userId) {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        setBackground(Color.WHITE);

        header = new AttendanceHeader(userId, "Attendance Records",
                "View and search records by student, class, instructor, discipline and date");

        txtDate.setToolTipText("yyyy-MM-dd");
        AttendanceButton btnSearch = new AttendanceButton("Search", AttendanceButton.Style.PRIMARY);
        AttendanceButton btnReset = new AttendanceButton("Reset");
        JPanel filters = AttendanceTheme.filterBar(
                AttendanceTheme.row(new JLabel("Student"), txtStudent, new JLabel("Class"), txtClass,
                        new JLabel("Instructor"), txtInstructor, new JLabel("Discipline"), cmbDiscipline),
                AttendanceTheme.row(new JLabel("Status"), cmbStatus, new JLabel("Date (yyyy-MM-dd)"), txtDate,
                        btnSearch, btnReset));

        lblMessage.setForeground(AttendanceTheme.ERROR);
        lblCount.setForeground(AttendanceTheme.TEXT_MUTED);
        JPanel messages = new JPanel(new BorderLayout());
        messages.setOpaque(false);
        messages.add(lblCount, BorderLayout.WEST);
        messages.add(lblMessage, BorderLayout.CENTER);

        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        JPanel under = new JPanel(new BorderLayout(0, 4));
        under.setOpaque(false);
        under.add(filters, BorderLayout.NORTH);
        under.add(messages, BorderLayout.CENTER);
        north.add(under, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        table.setAutoCreateRowSorter(true);
        AttendanceTheme.styleTable(table);
        AttendanceTheme.useStatusBadges(table, 6);
        add(new JScrollPane(table), BorderLayout.CENTER);

        AttendanceButton btnExport = new AttendanceButton("Export CSV");
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        south.setOpaque(false);
        south.add(btnExport);
        add(south, BorderLayout.SOUTH);

        btnSearch.addActionListener(e -> search());
        btnReset.addActionListener(e -> reset());
        btnExport.addActionListener(e -> AttendanceCsvExporter.export(this, table, "attendance_records.csv"));
        for (JTextField f : new JTextField[]{txtStudent, txtClass, txtInstructor, txtDate}) {
            f.addActionListener(e -> search()); // Enter key searches
        }
        // records may have been added in Manage Class Attendance, so reload when shown
        addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) { header.refresh(); search(); }
        });
        search();
    }

    private void reset() {
        txtStudent.setText(""); txtClass.setText(""); txtInstructor.setText("");
        txtDate.setText(""); cmbDiscipline.setSelectedIndex(0); cmbStatus.setSelectedIndex(0);
        search();
    }

    private void search() {
        lblMessage.setText(" ");
        String date = txtDate.getText().trim();
        if (!date.isEmpty() && !Validator.isDate(date)) {
            lblMessage.setText("   Date must be a valid yyyy-MM-dd.");
            return;
        }
        model.setRowCount(0);
        String discipline = (String) cmbDiscipline.getSelectedItem();
        String status = (String) cmbStatus.getSelectedItem();
        List<AttendanceEntry> all = service.loadEntries();
        for (AttendanceEntry e : all) {
            if (!contains(e.getStudentId() + " " + e.getStudentName(), txtStudent.getText())) continue;
            if (!contains(e.getClassId(), txtClass.getText())) continue;
            if (!contains(e.getInstructorId() + " " + e.getInstructorName(), txtInstructor.getText())) continue;
            if (!"All".equals(discipline) && !e.getDiscipline().equals(discipline)) continue;
            if (!"All".equals(status) && !e.getStatus().equals(status)) continue;
            if (!date.isEmpty() && !e.getDate().equals(date)) continue;
            model.addRow(new Object[]{e.getRecordId(), e.getDate(), e.getClassId(), e.getDiscipline(),
                    e.getInstructorName(), e.getStudentName(), e.getStatus()});
        }
        lblCount.setText("Showing " + model.getRowCount() + " of " + all.size() + " records");
        if (model.getRowCount() == 0) lblMessage.setText("   No records found.");
    }

    private static boolean contains(String text, String keyword) {
        return text.toLowerCase().contains(keyword.trim().toLowerCase());
    }
}
