package gui;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.*;
import service.AttendanceEntry;
import service.AttendanceService;
import util.Validator;
 
/** AO3: attendance summary by student or by class (read-only). */
public class AttendanceReportPanel extends JPanel {
    private final AttendanceService service = new AttendanceService();
    private final JComboBox<String> cmbMode = new JComboBox<>(new String[]{"Student", "Class"});
    private final JTextField txtSearch = new JTextField(10);
    private final JTextField txtFrom = new JTextField(9);
    private final JTextField txtTo = new JTextField(9);
    private final JLabel lblError = new JLabel(" ");
    private final StatCard cardAttended = new StatCard("Total attended (present + late)");
    private final StatCard cardAbsent = new StatCard("Total absent");
    private final StatCard cardLate = new StatCard("Total late");
    private final ReadOnlyTableModel model = new ReadOnlyTableModel();
 
    public AttendanceReportPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
 
        JLabel heading = new JLabel("Attendance Report");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 20f));
        JLabel sub = new JLabel("Generate and view attendance summaries by student or by class (read-only)");
 
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Summarise by")); filters.add(cmbMode);
        filters.add(new JLabel("Search name or ID")); filters.add(txtSearch);
        filters.add(new JLabel("From")); filters.add(txtFrom);
        filters.add(new JLabel("To")); filters.add(txtTo);
        JButton btnGenerate = new JButton("Generate");
        JButton btnReset = new JButton("Reset");
        filters.add(btnGenerate); filters.add(btnReset);
        lblError.setForeground(new Color(170, 30, 30));
 
        JPanel cards = new JPanel(new GridLayout(1, 3, 10, 0));
        cards.add(cardAttended); cards.add(cardAbsent); cards.add(cardLate);
 
        JPanel north = new JPanel(new GridLayout(0, 1, 0, 4));
        north.add(heading); north.add(sub); north.add(filters); north.add(lblError); north.add(cards);
        add(north, BorderLayout.NORTH);
 
        JTable table = new JTable(model);
        table.setRowHeight(24);
        add(new JScrollPane(table), BorderLayout.CENTER);
 
        btnGenerate.addActionListener(e -> generate());
        btnReset.addActionListener(e -> {
            txtSearch.setText(""); txtFrom.setText(""); txtTo.setText("");
            cmbMode.setSelectedIndex(0);
            generate();
        });
        generate();
    }
 
    private void generate() {
        lblError.setText(" ");
        String from = txtFrom.getText().trim();
        String to = txtTo.getText().trim();
        if (!from.isEmpty() && !Validator.isDate(from)) { lblError.setText("From-date must be yyyy-MM-dd."); return; }
        if (!to.isEmpty() && !Validator.isDate(to)) { lblError.setText("To-date must be yyyy-MM-dd."); return; }
        if (!from.isEmpty() && !to.isEmpty() && from.compareTo(to) > 0) {
            lblError.setText("From-date must not be after to-date.");
            return;
        }
 
        boolean byStudent = cmbMode.getSelectedIndex() == 0;
        String kw = txtSearch.getText().trim().toLowerCase();
        Map<String, int[]> counts = new TreeMap<>();   // key -> {total, present, absent, late}
        Map<String, String> labels = new HashMap<>();
 
        for (AttendanceEntry e : service.loadEntries()) {
            if (!from.isEmpty() && e.getDate().compareTo(from) < 0) continue;
            if (!to.isEmpty() && e.getDate().compareTo(to) > 0) continue;
            String key = byStudent ? e.getStudentId() : e.getClassId();
            String label = byStudent ? e.getStudentName() : e.getDiscipline() + " \u00b7 " + e.getInstructorName();
            if (!kw.isEmpty() && !(key + " " + label).toLowerCase().contains(kw)) continue;
 
            int[] c = counts.computeIfAbsent(key, k -> new int[4]);
            labels.putIfAbsent(key, label);
            c[0]++;
            switch (e.getStatus()) {
                case "PRESENT": c[1]++; break;
                case "ABSENT":  c[2]++; break;
                case "LATE":    c[3]++; break;
                default: break;
            }
        }
 
        model.setColumnIdentifiers(byStudent
                ? new String[]{"Student ID", "Name", "Classes", "Present", "Absent", "Late", "Attendance rate"}
                : new String[]{"Class ID", "Discipline \u00b7 Instructor", "Students", "Present", "Absent", "Late", "Attendance rate"});
        model.setRowCount(0);
 
        int attended = 0, absent = 0, late = 0;
        for (Map.Entry<String, int[]> en : counts.entrySet()) {
            int[] c = en.getValue();
            double rate = c[0] == 0 ? 0 : (c[1] + c[3]) * 100.0 / c[0];
            model.addRow(new Object[]{en.getKey(), labels.get(en.getKey()), c[0], c[1], c[2], c[3],
                    String.format("%.1f%%", rate)});
            attended += c[1] + c[3];
            absent += c[2];
            late += c[3];
        }
        cardAttended.setValue(String.valueOf(attended));
        cardAbsent.setValue(String.valueOf(absent));
        cardLate.setValue(String.valueOf(late));
        if (model.getRowCount() == 0) lblError.setText("No records found.");
    }
}

