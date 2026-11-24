package gui;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.*;
import service.AttendanceEntry;
import service.AttendanceService;
import util.Validator;

/**
 * AO3: attendance summary by student or by class (read-only).
 * Attendance rate = (present + late) / classes. A rate below the limit is shown in red.
 */
public class AttendanceReportPanel extends JPanel {
    private final AttendanceService service = new AttendanceService();
    private final AttendanceHeader header;
    private final JComboBox<String> cmbMode = new JComboBox<>(new String[]{"Student", "Class"});
    private final JTextField txtSearch = new JTextField(10);
    private final JTextField txtFrom = new JTextField(8);
    private final JTextField txtTo = new JTextField(8);
    private final JLabel lblError = new JLabel(" ");
    private final AttendanceTile cardAttended = new AttendanceTile("Total attended", null, null);
    private final AttendanceTile cardAbsent = new AttendanceTile("Total absent", null, null);
    private final AttendanceTile cardLate = new AttendanceTile("Total late", null, null);
    private final AttendanceTile cardRate = new AttendanceTile("Attendance rate", null, null);
    private final TypedTableModel model = new TypedTableModel();
    private final JTable table = new JTable(model);

    public AttendanceReportPanel() { this(null); }

    public AttendanceReportPanel(String userId) {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        setBackground(Color.WHITE);

        header = new AttendanceHeader(userId, "Attendance Report",
                "Summaries of classes attended, absent and late, by student or by class");

        txtFrom.setToolTipText("yyyy-MM-dd");
        txtTo.setToolTipText("yyyy-MM-dd");
        AttendanceButton btnGenerate = new AttendanceButton("Generate", AttendanceButton.Style.PRIMARY);
        AttendanceButton btnReset = new AttendanceButton("Reset");
        JPanel filters = AttendanceTheme.filterBar(
                AttendanceTheme.row(new JLabel("Summarise by"), cmbMode,
                        new JLabel("Search name or ID"), txtSearch),
                AttendanceTheme.row(new JLabel("From (yyyy-MM-dd)"), txtFrom,
                        new JLabel("To (yyyy-MM-dd)"), txtTo, btnGenerate, btnReset));
        lblError.setForeground(AttendanceTheme.ERROR);

        JPanel cards = new JPanel(new GridLayout(1, 4, 10, 0));
        cards.setOpaque(false);
        cards.add(cardAttended); cards.add(cardAbsent); cards.add(cardLate); cards.add(cardRate);

        JPanel under = new JPanel(new BorderLayout(0, 6));
        under.setOpaque(false);
        under.add(filters, BorderLayout.NORTH);
        under.add(lblError, BorderLayout.CENTER);
        under.add(cards, BorderLayout.SOUTH);
        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        north.add(under, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        table.setAutoCreateRowSorter(true);
        AttendanceTheme.styleTable(table);
        add(new JScrollPane(table), BorderLayout.CENTER);

        AttendanceButton btnExport = new AttendanceButton("Export CSV");
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        south.setOpaque(false);
        south.add(btnExport);
        add(south, BorderLayout.SOUTH);

        btnGenerate.addActionListener(e -> generate());
        btnReset.addActionListener(e -> {
            txtSearch.setText(""); txtFrom.setText(""); txtTo.setText("");
            cmbMode.setSelectedIndex(0);
            generate();
        });
        btnExport.addActionListener(e -> AttendanceCsvExporter.export(this, table,
                cmbMode.getSelectedIndex() == 0 ? "attendance_report_by_student.csv"
                                                : "attendance_report_by_class.csv"));
        cmbMode.addActionListener(e -> generate());
        for (JTextField f : new JTextField[]{txtSearch, txtFrom, txtTo}) f.addActionListener(e -> generate());
        // records may have been added in Manage Class Attendance, so reload when shown
        addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) { header.refresh(); generate(); }
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
            String label = byStudent ? e.getStudentName() : e.getDiscipline() + " · " + e.getInstructorName();
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
                : new String[]{"Class ID", "Discipline · Instructor", "Students", "Present", "Absent", "Late", "Attendance rate"});
        model.setRowCount(0);

        int total = 0, attended = 0, absent = 0, late = 0;
        for (Map.Entry<String, int[]> en : counts.entrySet()) {
            int[] c = en.getValue();
            double rate = c[0] == 0 ? 0 : (c[1] + c[3]) * 100.0 / c[0];
            model.addRow(new Object[]{en.getKey(), labels.get(en.getKey()), c[0], c[1], c[2], c[3], rate});
            total += c[0];
            attended += c[1] + c[3];
            absent += c[2];
            late += c[3];
        }
        // setColumnIdentifiers rebuilds the columns, so the rate bars are applied again here
        AttendanceTheme.styleTable(table);
        AttendanceTheme.useRateBars(table, 6);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        table.getColumnModel().getColumn(6).setPreferredWidth(170);

        double overall = total == 0 ? 0 : attended * 100.0 / total;
        cardAttended.setValue(String.valueOf(attended), null, "present + late");
        cardAbsent.setValue(String.valueOf(absent), absent == 0 ? AttendanceTheme.PRESENT : AttendanceTheme.ABSENT, null);
        cardLate.setValue(String.valueOf(late), late == 0 ? AttendanceTheme.PRESENT : AttendanceTheme.LATE, "included in attended");
        cardRate.setValue(total == 0 ? "-" : String.format("%.1f%%", overall),
                total == 0 ? null : AttendanceTheme.rateColor(overall), total + " record(s)");
        if (model.getRowCount() == 0) lblError.setText("No records found.");
    }
}
