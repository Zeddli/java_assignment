package gui;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import model.*;
import repository.*;
import service.AttendanceOverviewService;
import service.AttendanceOverviewService.ClassStatus;

/**
 * AO1: list the BOOKED and PAID students of a class that has started,
 * then record attendance (PRESENT / ABSENT / LATE) or correct it.
 * Choosing a class loads its students straight away; the search box filters the list.
 */
public class ManageAttendancePanel extends JPanel {
    private static final String CHOOSE = "– choose –";
    private static final String[] OPTIONS = {CHOOSE, "PRESENT", "ABSENT", "LATE"};

    private final User officer;
    private final BookingRepository bookingRepo = new BookingRepository();
    private final AttendanceRepository attendanceRepo = new AttendanceRepository();
    private final UserRepository userRepo = new UserRepository();
    private final AttendanceOverviewService overview = new AttendanceOverviewService();

    private final AttendanceHeader header;
    private final JComboBox<ClassItem> cmbClass = new JComboBox<>();
    private final JTextField txtSearch = new JTextField(14);
    private final JLabel lblInfo = new JLabel(" ");
    private final JLabel lblCounts = new JLabel(" ");
    private final JLabel lblError = new JLabel(" ");
    private final RowModel model = new RowModel();
    private final JTable table = new JTable(model);
    private final TableRowSorter<RowModel> sorter = new TableRowSorter<>(model);
    private boolean loadingClasses; // true while the dropdown is being refilled

    public ManageAttendancePanel(User officer) {
        this.officer = officer;
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        setBackground(Color.WHITE);

        header = new AttendanceHeader(officer.getId(), "Manage Class Attendance",
                "Record attendance after a class takes place; correct it if needed");

        // filter bar (two rows so nothing wraps or gets clipped)
        AttendanceButton btnRefresh = new AttendanceButton("Refresh");
        JPanel filters = AttendanceTheme.filterBar(
                AttendanceTheme.row(new JLabel("Class"), cmbClass, btnRefresh),
                AttendanceTheme.row(new JLabel("Search student"), txtSearch));

        lblInfo.setForeground(AttendanceTheme.TEXT);
        lblCounts.setForeground(AttendanceTheme.TEXT_MUTED);
        JPanel info = new JPanel(new GridLayout(0, 1));
        info.setOpaque(false);
        info.add(lblInfo); info.add(lblCounts);

        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        JPanel under = new JPanel(new BorderLayout(0, 6));
        under.setOpaque(false);
        under.add(filters, BorderLayout.NORTH);
        under.add(info, BorderLayout.CENTER);
        north.add(under, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        // table
        table.setRowSorter(sorter);
        AttendanceTheme.styleTable(table);
        AttendanceTheme.useStatusBadges(table, 2);
        AttendanceTheme.useStatusBadges(table, 3);
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JComboBox<>(OPTIONS)));
        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setPreferredWidth(240);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // buttons
        AttendanceButton btnAllPresent = new AttendanceButton("Mark all present");
        AttendanceButton btnSave = new AttendanceButton("Save attendance", AttendanceButton.Style.PRIMARY);
        AttendanceButton btnUpdate = new AttendanceButton("Update corrections");
        AttendanceButton btnClear = new AttendanceButton("Clear unsaved");
        lblError.setForeground(AttendanceTheme.ERROR);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        south.setOpaque(false);
        south.add(btnAllPresent); south.add(btnSave); south.add(btnUpdate); south.add(btnClear);
        south.add(lblError);
        add(south, BorderLayout.SOUTH);

        cmbClass.addActionListener(e -> { if (!loadingClasses) loadStudents(); });
        btnRefresh.addActionListener(e -> { loadClasses(null); });
        btnAllPresent.addActionListener(e -> markAllPresent());
        btnSave.addActionListener(e -> saveNew());
        btnUpdate.addActionListener(e -> saveCorrections());
        btnClear.addActionListener(e -> loadStudents());
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        model.addTableModelListener(e -> updateCounts());
        // pending counts may have changed on another screen, so refill the dropdown when shown
        addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) { header.refresh(); loadClasses(null); }
        });

        loadClasses(null);
    }

    /** Called by the Overview: show this class and load its students. */
    public void showClass(String classId) {
        loadClasses(classId);
    }

    /** Refills the dropdown (classes that are not cancelled and have started, newest first). */
    private void loadClasses(String selectId) {
        ClassItem current = (ClassItem) cmbClass.getSelectedItem();
        String keepId = selectId != null ? selectId : current == null ? null : current.status.getSession().getId();

        List<ClassStatus> classes = overview.classStatuses();
        classes.sort(Comparator.comparing((ClassStatus s) -> s.getSession().getDate())
                .thenComparing(s -> s.getSession().getStartTime()).reversed());

        loadingClasses = true;
        cmbClass.removeAllItems();
        ClassItem toSelect = null;
        for (ClassStatus s : classes) {
            ClassItem item = new ClassItem(s);
            cmbClass.addItem(item);
            if (s.getSession().getId().equals(keepId)) toSelect = item;
        }
        if (toSelect != null) cmbClass.setSelectedItem(toSelect);
        loadingClasses = false;
        loadStudents();
    }

    private void loadStudents() {
        lblError.setText(" ");
        stopEditing();
        ClassItem item = (ClassItem) cmbClass.getSelectedItem();
        model.rows.clear();
        if (item == null) {
            model.fireTableDataChanged();
            lblInfo.setText("No class has taken place yet.");
            return;
        }
        ClassSession c = item.status.getSession();
        for (Booking b : bookingRepo.findBookedByClass(c.getId())) {
            model.rows.add(new Row(b, userRepo.nameOf(b.getStudentId()), attendanceRepo.findByBooking(b.getId())));
        }
        model.fireTableDataChanged();
        applyFilter();
        lblInfo.setText("<html><b>" + c.getId() + "</b> · " + c.getDiscipline() + " · "
                + userRepo.nameOf(c.getInstructorId()) + " · " + c.getDate() + " "
                + c.getStartTime() + "–" + c.getEndTime() + " · " + c.getRoom() + "</html>");
        updateCounts();
    }

    /** Search box: hides rows that do not match, without losing choices that are not saved yet. */
    private void applyFilter() {
        String kw = txtSearch.getText().trim();
        sorter.setRowFilter(kw.isEmpty() ? null
                : RowFilter.regexFilter("(?i)" + Pattern.quote(kw), 0, 1));
    }

    /** "Registered 5 · Recorded 3 · Pending 2" plus how many of each status is chosen. */
    private void updateCounts() {
        int present = 0, late = 0, absent = 0, notChosen = 0, recorded = 0;
        for (Row r : model.rows) {
            if (r.record != null) recorded++;
            if (r.status == null) notChosen++;
            else if (r.status.equals("PRESENT")) present++;
            else if (r.status.equals("LATE")) late++;
            else if (r.status.equals("ABSENT")) absent++;
        }
        int total = model.rows.size();
        lblCounts.setText("<html>Registered <b>" + total + "</b> · Recorded <b>" + recorded
                + "</b> · Pending <b>" + (total - recorded) + "</b> &nbsp;&nbsp;|&nbsp;&nbsp; "
                + "<font color='#1E7A4C'>Present " + present + "</font> · "
                + "<font color='#9A6700'>Late " + late + "</font> · "
                + "<font color='#B00020'>Absent " + absent + "</font> · "
                + "Not chosen " + notChosen + "</html>");
    }

    /** Quick fill: every student who has no record and no choice yet becomes PRESENT. */
    private void markAllPresent() {
        stopEditing();
        int n = 0;
        for (int i = 0; i < model.rows.size(); i++) {
            Row r = model.rows.get(i);
            if (r.record == null && r.status == null) { r.status = "PRESENT"; n++; }
        }
        model.fireTableDataChanged();
        lblError.setText(n == 0 ? "Everyone already has an attendance choice." : " ");
    }

    /** Save attendance: creates records for students who have none yet. */
    private void saveNew() {
        stopEditing();
        lblError.setText(" ");
        ClassItem item = (ClassItem) cmbClass.getSelectedItem();
        if (item == null) { lblError.setText("Choose a class first."); return; }
        ClassSession c = item.status.getSession();
        if (!c.hasStarted()) { lblError.setText("This class has not started yet."); return; }

        List<Row> pending = new ArrayList<>();
        for (Row r : model.rows) if (r.record == null) pending.add(r);
        if (pending.isEmpty()) { lblError.setText("Nothing new to save."); return; }
        for (Row r : pending) {
            if (r.status == null) { lblError.setText("Choose attendance for every student before saving."); return; }
        }
        for (Row r : pending) {
            attendanceRepo.add(new AttendanceRecord(attendanceRepo.nextId("ATD"), r.booking.getId(),
                    r.booking.getClassId(), r.booking.getStudentId(), r.status, officer.getId(), now()));
        }
        JOptionPane.showMessageDialog(this, pending.size() + " attendance record(s) saved.");
        loadClasses(c.getId());
    }

    /** Update corrections: changes records that already exist. */
    private void saveCorrections() {
        stopEditing();
        lblError.setText(" ");
        int changed = 0;
        for (Row r : model.rows) {
            if (r.record == null || r.status != null && r.status.equals(r.record.getStatus())) continue;
            if (r.status == null) { lblError.setText("Choose a status for " + r.studentName + "."); return; }
            r.record.setStatus(r.status);
            r.record.setMarkedBy(officer.getId());
            r.record.setMarkedAt(now());
            attendanceRepo.update(r.record);
            changed++;
        }
        if (changed == 0) { lblError.setText("No corrections to save."); return; }
        JOptionPane.showMessageDialog(this, changed + " record(s) corrected.");
        ClassItem item = (ClassItem) cmbClass.getSelectedItem();
        loadClasses(item == null ? null : item.status.getSession().getId());
    }

    private void stopEditing() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    /** Combo box entry that shows readable text but keeps the class and its pending count. */
    private class ClassItem {
        final ClassStatus status;
        ClassItem(ClassStatus status) { this.status = status; }
        @Override public String toString() {
            ClassSession s = status.getSession();
            String tail = status.getEligible() == 0 ? "no paid students"
                    : status.getPending() == 0 ? "all marked" : status.getPending() + " pending";
            return s.getId() + " · " + s.getDiscipline() + " · "
                    + status.getInstructorName() + " · " + s.getDate() + " " + s.getStartTime()
                    + "  (" + tail + ")";
        }
    }

    /** One table row = one booked and paid student. */
    private static class Row {
        final Booking booking;
        final String studentName;
        final AttendanceRecord record; // null = not recorded yet
        String status;                 // what is currently chosen in the dropdown
        Row(Booking booking, String studentName, AttendanceRecord record) {
            this.booking = booking;
            this.studentName = studentName;
            this.record = record;
            this.status = record == null ? null : record.getStatus();
        }
        String recordedText() {
            if (record == null) return "Not yet";
            return record.getStatus().equals(status) ? "Saved" : "Changed";
        }
    }

    private static class RowModel extends AbstractTableModel {
        final List<Row> rows = new ArrayList<>();
        private final String[] cols = {"Student ID", "Name", "Attendance", "Recorded"};

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public boolean isCellEditable(int r, int c) { return c == 2; }

        @Override public Object getValueAt(int r, int c) {
            Row row = rows.get(r);
            switch (c) {
                case 0: return row.booking.getStudentId();
                case 1: return row.studentName;
                case 2: return row.status == null ? CHOOSE : row.status;
                default: return row.recordedText();
            }
        }

        @Override public void setValueAt(Object value, int r, int c) {
            if (c != 2) return;
            rows.get(r).status = CHOOSE.equals(value) ? null : (String) value;
            fireTableRowsUpdated(r, r);
        }
    }
}
