package gui;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import service.AttendanceOverviewService;
import service.AttendanceOverviewService.ClassStatus;
import service.AttendanceOverviewService.StudentRate;

/**
 * ADDITIONAL FEATURE: Attendance Overview + Low-Attendance Alert (first screen after login).
 *
 *  - four summary cards (classes to mark, records, overall rate, low-attendance students)
 *  - a chart of the last classes (present / late / absent)
 *  - the classes that still need attendance (double-click opens Manage Class Attendance)
 *  - students whose attendance rate is below the limit
 *
 * Read-only: all numbers come from AttendanceOverviewService.
 */
public class AttendanceOverviewPanel extends JPanel {
    private final AttendanceOverviewService service = new AttendanceOverviewService();
    private final Consumer<String> openClass; // opens Manage Class Attendance on a class id

    private final AttendanceHeader header;
    private final AttendanceTile tileToMark;
    private final AttendanceTile tileRecords = new AttendanceTile("Records marked", null, null);
    private final AttendanceTile tileRate = new AttendanceTile("Attendance rate", null, null);
    private final AttendanceTile tileLow = new AttendanceTile("Low attendance", null, null);
    private final AttendanceBarChart chart = new AttendanceBarChart();
    private final JLabel lblPendingTitle = AttendanceTheme.sectionTitle("Waiting for attendance");
    private final JLabel lblLowTitle = AttendanceTheme.sectionTitle("Low attendance alert");
    private final ReadOnlyTableModel pendingModel =
            new ReadOnlyTableModel("Class", "Date", "Discipline", "Pending");
    private final TypedTableModel lowModel =
            new TypedTableModel("Student ID", "Name", "Classes", "Absent", "Late", "Attendance rate");
    private final JTable pendingTable = new JTable(pendingModel);
    private final JTable lowTable = new JTable(lowModel);

    public AttendanceOverviewPanel(String userId, Consumer<String> openClass) {
        this.openClass = openClass;
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        setBackground(Color.WHITE);

        header = new AttendanceHeader(userId, "Overview",
                "Classes waiting for attendance and students who need attention");

        tileToMark = new AttendanceTile("Classes to mark", "Open first class", this::openFirstPending);
        JPanel tiles = new JPanel(new GridLayout(1, 4, 10, 0));
        tiles.setOpaque(false);
        tiles.add(tileToMark); tiles.add(tileRecords); tiles.add(tileRate); tiles.add(tileLow);

        JPanel north = new JPanel(new BorderLayout(0, 10));
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        north.add(tiles, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        // middle: chart | waiting classes
        JPanel chartBox = new JPanel(new BorderLayout(0, 4));
        chartBox.setBackground(Color.WHITE);
        chartBox.setBorder(AttendanceTheme.cardBorder());
        chartBox.add(AttendanceTheme.sectionTitle("Recent classes"), BorderLayout.NORTH);
        chartBox.add(chart, BorderLayout.CENTER);

        AttendanceTheme.styleTable(pendingTable);
        AttendanceTheme.styleTable(lowTable);
        AttendanceTheme.useRateBars(lowTable, 5);
        pendingTable.setToolTipText("Double-click a class to mark its attendance");
        pendingTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) openSelected(); }
        });
        AttendanceButton btnOpen = new AttendanceButton("Mark attendance", AttendanceButton.Style.PRIMARY);
        btnOpen.addActionListener(e -> openSelected());

        JPanel pendingTop = new JPanel(new BorderLayout());
        pendingTop.setOpaque(false);
        pendingTop.add(lblPendingTitle, BorderLayout.WEST);
        pendingTop.add(btnOpen, BorderLayout.EAST);
        JPanel pendingBox = new JPanel(new BorderLayout(0, 4));
        pendingBox.setOpaque(false);
        pendingBox.add(pendingTop, BorderLayout.NORTH);
        pendingBox.add(new JScrollPane(pendingTable), BorderLayout.CENTER);

        JPanel middle = new JPanel(new GridLayout(1, 2, 12, 0));
        middle.setOpaque(false);
        middle.add(chartBox);
        middle.add(pendingBox);

        JPanel lowBox = new JPanel(new BorderLayout(0, 4));
        lowBox.setOpaque(false);
        lowBox.add(lblLowTitle, BorderLayout.NORTH);
        lowBox.add(new JScrollPane(lowTable), BorderLayout.CENTER);

        JPanel center = new JPanel(new GridLayout(2, 1, 0, 10));
        center.setOpaque(false);
        center.add(middle);
        center.add(lowBox);
        add(center, BorderLayout.CENTER);

        // numbers may have changed in Manage Class Attendance, so reload whenever this screen is shown
        addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) { refresh(); }
        });
        refresh();
    }

    /** Re-reads the text files and redraws everything. */
    public final void refresh() {
        header.refresh();
        List<ClassStatus> pending = service.pendingClasses();
        List<StudentRate> low = service.lowAttendance();

        int waiting = 0;
        for (ClassStatus s : pending) waiting += s.getPending();
        tileToMark.setValue(String.valueOf(pending.size()),
                pending.isEmpty() ? AttendanceTheme.PRESENT : AttendanceTheme.ACCENT,
                waiting == 0 ? "All caught up" : waiting + " student(s) waiting");

        int records = service.totalRecords();
        tileRecords.setValue(String.valueOf(records), null,
                "in " + service.recentClasses(Integer.MAX_VALUE).size() + " classes");

        double rate = service.overallRate();
        tileRate.setValue(records == 0 ? "-" : String.format("%.1f%%", rate),
                records == 0 ? null : AttendanceTheme.rateColor(rate), "present + late");

        tileLow.setValue(String.valueOf(low.size()),
                low.isEmpty() ? AttendanceTheme.PRESENT : AttendanceTheme.ABSENT,
                "below " + (int) AttendanceOverviewService.LOW_RATE + "% attendance");

        chart.setData(service.recentClasses(6));

        pendingModel.setRowCount(0);
        for (ClassStatus s : pending) {
            pendingModel.addRow(new Object[]{s.getSession().getId(), s.getSession().getDate(),
                    s.getSession().getDiscipline(), s.getPending() + " of " + s.getEligible()});
        }
        lblPendingTitle.setText("Waiting for attendance (" + pending.size() + ")");

        lowModel.setRowCount(0);
        for (StudentRate r : low) {
            lowModel.addRow(new Object[]{r.getStudentId(), r.getName(), r.getClasses(),
                    r.getAbsent(), r.getLate(), r.getRate()});
        }
        lblLowTitle.setText("Low attendance alert (" + low.size() + ")  –  below "
                + (int) AttendanceOverviewService.LOW_RATE + "% with at least "
                + AttendanceOverviewService.MIN_CLASSES + " recorded classes");
    }

    private void openFirstPending() {
        if (pendingModel.getRowCount() > 0) openClass.accept((String) pendingModel.getValueAt(0, 0));
    }

    private void openSelected() {
        int row = pendingTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a class in the list first.",
                    "Mark attendance", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        openClass.accept((String) pendingModel.getValueAt(row, 0));
    }
}
