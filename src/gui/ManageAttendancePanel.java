package gui;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import model.*;
import repository.*;
 
/** AO1: list BOOKED students of a class that has started, record or correct attendance. */
public class ManageAttendancePanel extends JPanel {
    private static final String CHOOSE = "\u2013 choose \u2013";
    private static final String[] OPTIONS = {CHOOSE, "PRESENT", "ABSENT", "LATE"};
 
    private final User officer;
    private final ClassRepository classRepo = new ClassRepository();
    private final BookingRepository bookingRepo = new BookingRepository();
    private final AttendanceRepository attendanceRepo = new AttendanceRepository();
    private final UserRepository userRepo = new UserRepository();
 
    private final JComboBox<ClassItem> cmbClass = new JComboBox<>();
    private final JTextField txtSearch = new JTextField(14);
    private final JLabel lblInfo = new JLabel(" ");
    private final JLabel lblError = new JLabel(" ");
    private final RowModel model = new RowModel();
    private final JTable table = new JTable(model);
 
    public ManageAttendancePanel(User officer) {
        this.officer = officer;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
 
        JLabel heading = new JLabel("Manage Class Attendance");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 20f));
        JLabel sub = new JLabel("Record attendance after a class takes place; correct it if needed");
        JPanel head = new JPanel(new GridLayout(0, 1));
        head.add(heading); head.add(sub);
 
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnLoad = new JButton("Load students");
        filters.add(new JLabel("Class"));
        filters.add(cmbClass);
        filters.add(new JLabel("Search student"));
        filters.add(txtSearch);
        filters.add(btnLoad);
 
        JPanel north = new JPanel(new BorderLayout());
        north.add(head, BorderLayout.NORTH);
        north.add(filters, BorderLayout.CENTER);
        north.add(lblInfo, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
 
        table.setRowHeight(26);
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JComboBox<>(OPTIONS)));
        add(new JScrollPane(table), BorderLayout.CENTER);
 
        JButton btnSave = new JButton("Save attendance");
        JButton btnUpdate = new JButton("Update corrections");
        JButton btnClear = new JButton("Clear unsaved");
        lblError.setForeground(new Color(170, 30, 30));
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.add(btnSave); south.add(btnUpdate); south.add(btnClear); south.add(lblError);
        add(south, BorderLayout.SOUTH);
 
        btnLoad.addActionListener(e -> loadStudents());
        btnSave.addActionListener(e -> saveNew());
        btnUpdate.addActionListener(e -> saveCorrections());
        btnClear.addActionListener(e -> loadStudents());
 
        loadClasses();
    }
 
    /** Only classes that are not cancelled and have already started. */
    private void loadClasses() {
        cmbClass.removeAllItems();
        for (ClassSession c : classRepo.findAll()) {
            if (!c.isCancelled() && c.hasStarted()) cmbClass.addItem(new ClassItem(c));
        }
        lblInfo.setText(cmbClass.getItemCount() == 0
                ? "No class has taken place yet." : "Choose a class, then press Load students.");
    }
 
    private void loadStudents() {
        lblError.setText(" ");
        stopEditing();
        ClassItem item = (ClassItem) cmbClass.getSelectedItem();
        model.rows.clear();
        if (item == null) { model.fireTableDataChanged(); return; }
 
        String kw = txtSearch.getText().trim().toLowerCase();
        List<Booking> bookings = bookingRepo.findBookedByClass(item.session.getId());
        int recorded = 0;
        for (Booking b : bookings) {
            AttendanceRecord rec = attendanceRepo.findByBooking(b.getId());
            if (rec != null) recorded++;
            String name = userRepo.nameOf(b.getStudentId());
            if (!kw.isEmpty() && !(b.getStudentId() + " " + name).toLowerCase().contains(kw)) continue;
            model.rows.add(new Row(b, name, rec));
        }
        model.fireTableDataChanged();
        ClassSession c = item.session;
        lblInfo.setText("Class " + c.getId() + "   Discipline " + c.getDiscipline()
                + "   Instructor " + userRepo.nameOf(c.getInstructorId()) + "   Date " + c.getDate()
                + "   Registered " + bookings.size() + "   Recorded " + recorded + " of " + bookings.size());
    }
 
    /** Save attendance: creates records for students who have none yet. */
    private void saveNew() {
        stopEditing();
        ClassItem item = (ClassItem) cmbClass.getSelectedItem();
        if (item == null) { lblError.setText("Choose a class first."); return; }
        if (!item.session.hasStarted()) { lblError.setText("This class has not started yet."); return; }
 
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
        loadStudents();
    }
 
    /** Update corrections: changes records that already exist. */
    private void saveCorrections() {
        stopEditing();
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
        loadStudents();
    }
 
    private void stopEditing() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
    }
 
    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
 
    /** Combo box entry that shows readable text but keeps the ClassSession. */
    private class ClassItem {
        final ClassSession session;
        ClassItem(ClassSession session) { this.session = session; }
        @Override public String toString() {
            return session.getId() + " \u00b7 " + session.getDiscipline() + " \u00b7 "
                    + userRepo.nameOf(session.getInstructorId()) + " \u00b7 "
                    + session.getDate() + " " + session.getStartTime();
        }
    }
 
    /** One table row = one booked student. */
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

