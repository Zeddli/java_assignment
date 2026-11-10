package gui;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import service.AttendanceEntry;
import service.AttendanceService;
import util.Validator;
 
/** AO2: view and search attendance records (read-only). Filters combine with AND. */
public class AttendanceRecordsPanel extends JPanel {
    private final AttendanceService service = new AttendanceService();
    private final JTextField txtStudent = new JTextField(9);
    private final JTextField txtClass = new JTextField(7);
    private final JTextField txtInstructor = new JTextField(9);
    private final JComboBox<String> cmbDiscipline =
            new JComboBox<>(new String[]{"All", "Karate", "Judo", "Taekwondo", "MMA"});
    private final JTextField txtDate = new JTextField(9);
    private final JLabel lblMessage = new JLabel(" ");
    private final ReadOnlyTableModel model = new ReadOnlyTableModel(
            "Record", "Date", "Class", "Discipline", "Instructor", "Student", "Status");
 
    public AttendanceRecordsPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
 
        JLabel heading = new JLabel("Attendance Records");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 20f));
        JLabel sub = new JLabel("View and search records by student, class, instructor, discipline and date (read-only)");
 
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Student")); filters.add(txtStudent);
        filters.add(new JLabel("Class")); filters.add(txtClass);
        filters.add(new JLabel("Instructor")); filters.add(txtInstructor);
        filters.add(new JLabel("Discipline")); filters.add(cmbDiscipline);
        filters.add(new JLabel("Date (yyyy-MM-dd)")); filters.add(txtDate);
        JButton btnSearch = new JButton("Search");
        JButton btnReset = new JButton("Reset");
        filters.add(btnSearch); filters.add(btnReset);
        lblMessage.setForeground(new Color(170, 30, 30));
 
        JPanel north = new JPanel(new GridLayout(0, 1));
        north.add(heading); north.add(sub); north.add(filters); north.add(lblMessage);
        add(north, BorderLayout.NORTH);
 
        JTable table = new JTable(model);
        table.setRowHeight(24);
        add(new JScrollPane(table), BorderLayout.CENTER);
 
        btnSearch.addActionListener(e -> search());
        btnReset.addActionListener(e -> reset());
        search();
    }
 
    private void reset() {
        txtStudent.setText(""); txtClass.setText(""); txtInstructor.setText("");
        txtDate.setText(""); cmbDiscipline.setSelectedIndex(0);
        search();
    }
 
    private void search() {
        lblMessage.setText(" ");
        String date = txtDate.getText().trim();
        if (!date.isEmpty() && !Validator.isDate(date)) {
            lblMessage.setText("Date must be a valid yyyy-MM-dd.");
            return;
        }
        model.setRowCount(0);
        String discipline = (String) cmbDiscipline.getSelectedItem();
        List<AttendanceEntry> all = service.loadEntries();
        for (AttendanceEntry e : all) {
            if (!contains(e.getStudentId() + " " + e.getStudentName(), txtStudent.getText())) continue;
            if (!contains(e.getClassId(), txtClass.getText())) continue;
            if (!contains(e.getInstructorId() + " " + e.getInstructorName(), txtInstructor.getText())) continue;
            if (!"All".equals(discipline) && !e.getDiscipline().equals(discipline)) continue;
            if (!date.isEmpty() && !e.getDate().equals(date)) continue;
            model.addRow(new Object[]{e.getRecordId(), e.getDate(), e.getClassId(), e.getDiscipline(),
                    e.getInstructorName(), e.getStudentName(), e.getStatus()});
        }
        if (model.getRowCount() == 0) lblMessage.setText("No records found.");
    }
 
    private static boolean contains(String text, String keyword) {
        return text.toLowerCase().contains(keyword.trim().toLowerCase());
    }
}

