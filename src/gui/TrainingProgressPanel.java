package gui;

import model.AttendanceRecord;
import model.Booking;
import model.ClassSession;
import model.User;
import repository.AttendanceRepository;
import repository.BookingRepository;
import repository.ClassRepository;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TrainingProgressPanel extends JPanel {

    private static final Color TEXT = new Color(40, 51, 69);
    private static final Color MUTED = new Color(101, 114, 132);
    private static final Color BLUE = new Color(69, 84, 109);
    private static final Color BORDER = new Color(218, 227, 239);
    private static final Color PALE = new Color(246, 248, 251);
    private static final Color[] CHART_COLORS = {
        new Color(69, 84, 109), new Color(111, 137, 165),
        new Color(162, 186, 207), new Color(199, 211, 224),
        new Color(143, 157, 171), new Color(94, 111, 130)
    };
    private static final Font BODY = new Font("SansSerif", Font.PLAIN, 13);
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final User student;
    private final BookingRepository bookingRepository = new BookingRepository();
    private final ClassRepository classRepository = new ClassRepository();
    private final AttendanceRepository attendanceRepository = new AttendanceRepository();

    private final JLabel attendedValue = valueLabel();
    private final JLabel rateValue = valueLabel();
    private final JLabel upcomingValue = valueLabel();
    private final JLabel hoursValue = valueLabel();
    private final BarChart barChart = new BarChart();
    private final AttendancePieChart pieChart = new AttendancePieChart();

    public TrainingProgressPanel(User student) {
        this.student = student;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        buildInterface();
        refreshProgress();
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                refreshProgress();
            }
        });
    }

    private void buildInterface() {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setBackground(Color.WHITE);
        page.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        JPanel heading = new JPanel(new BorderLayout(0, 5));
        heading.setOpaque(false);
        JLabel title = new JLabel("My Training Progress");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(TEXT);
        JLabel subtitle = new JLabel("Your attendance and training activity at a glance");
        subtitle.setFont(BODY);
        subtitle.setForeground(MUTED);
        heading.add(title, BorderLayout.NORTH);
        heading.add(subtitle, BorderLayout.SOUTH);
        page.add(heading, BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout(0, 18));
        main.setOpaque(false);

        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
        cards.setOpaque(false);
        cards.add(statCard("Classes Attended", attendedValue));
        cards.add(statCard("Attendance Rate", rateValue));
        cards.add(statCard("Upcoming Classes", upcomingValue));
        cards.add(statCard("Training Hours", hoursValue));
        main.add(cards, BorderLayout.NORTH);

        JPanel charts = new JPanel(new GridLayout(1, 2, 16, 0));
        charts.setOpaque(false);
        charts.add(chartCard("Classes Attended by Discipline", barChart));
        charts.add(chartCard("Attendance Breakdown", pieChart));
        main.add(charts, BorderLayout.CENTER);
        main.setPreferredSize(new Dimension(880, 460));

        JScrollPane scroll = new JScrollPane(main);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(Color.WHITE);
        page.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JLabel note = new JLabel("Upcoming classes are paid future bookings. Attendance uses completed classes only.");
        note.setForeground(MUTED);
        note.setFont(new Font("SansSerif", Font.PLAIN, 12));
        footer.add(note, BorderLayout.WEST);
        page.add(footer, BorderLayout.SOUTH);
        add(page, BorderLayout.CENTER);
    }

    private static JLabel valueLabel() {
        JLabel label = new JLabel("—");
        label.setFont(new Font("SansSerif", Font.BOLD, 23));
        label.setForeground(TEXT);
        return label;
    }

    private static JPanel statCard(String title, JLabel value) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(PALE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(15, 14, 15, 14)));
        JLabel name = new JLabel(title);
        name.setFont(new Font("SansSerif", Font.PLAIN, 12));
        name.setForeground(MUTED);
        card.add(name, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private static JPanel chartCard(String title, JComponent chart) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(15, 14, 15, 14)));
        JLabel name = new JLabel(title);
        name.setFont(new Font("SansSerif", Font.BOLD, 15));
        name.setForeground(TEXT);
        chart.setPreferredSize(new Dimension(385, 305));
        card.add(name, BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    private void refreshProgress() {
        Map<String, Booking> studentBookings = new HashMap<>();
        Map<String, ClassSession> sessions = new HashMap<>();
        for (ClassSession session : classRepository.findAll()) {
            sessions.put(session.getId(), session);
        }
        int upcoming = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Booking booking : bookingRepository.findAll()) {
            if (!student.getId().equals(booking.getStudentId())) continue;
            studentBookings.put(booking.getId(), booking);
            if (!booking.isBooked()) continue;
            ClassSession session = sessions.get(booking.getClassId());
            if (session == null || session.isCancelled()) continue;
            try {
                LocalDateTime start = LocalDateTime.parse(
                        session.getDate() + " " + session.getStartTime(), DATE_TIME);
                if (start.isAfter(now) && "PAID".equalsIgnoreCase(booking.getPaymentStatus())) upcoming++;
            } catch (RuntimeException ignored) {
                // Invalid class dates are excluded from the upcoming count.
            }
        }

        Map<String, Integer> disciplines = new LinkedHashMap<>();
        disciplines.put("Karate", 0);
        disciplines.put("Judo", 0);
        disciplines.put("Taekwondo", 0);
        disciplines.put("MMA", 0);
        int present = 0;
        int late = 0;
        int absent = 0;
        long minutesAttended = 0;
        Set<String> countedBookings = new HashSet<>();

        for (AttendanceRecord record : attendanceRepository.findAll()) {
            if (!student.getId().equals(record.getStudentId())) continue;
            Booking booking = studentBookings.get(record.getBookingId());
            if (booking == null || !booking.isBooked()
                    || !booking.getClassId().equals(record.getClassId())
                    || !countedBookings.add(booking.getId())) continue;
            ClassSession session = sessions.get(booking.getClassId());
            if (session == null || session.isCancelled()
                    || !"PAID".equalsIgnoreCase(booking.getPaymentStatus())) continue;

            try {
                LocalDateTime end = LocalDateTime.parse(
                        session.getDate() + " " + session.getEndTime(), DATE_TIME);
                if (end.isAfter(now)) continue;
            } catch (RuntimeException ex) {
                continue;
            }

            String status = record.getStatus();
            if ("ABSENT".equalsIgnoreCase(status)) {
                absent++;
                continue;
            }
            if ("PRESENT".equalsIgnoreCase(status)) present++;
            else if ("LATE".equalsIgnoreCase(status)) late++;
            else continue;

            String discipline = session.getDiscipline();
            disciplines.put(discipline, disciplines.getOrDefault(discipline, 0) + 1);
            try {
                LocalDateTime start = LocalDateTime.parse(
                        session.getDate() + " " + session.getStartTime(), DATE_TIME);
                LocalDateTime end = LocalDateTime.parse(
                        session.getDate() + " " + session.getEndTime(), DATE_TIME);
                long minutes = Duration.between(start, end).toMinutes();
                if (minutes > 0) minutesAttended += minutes;
            } catch (RuntimeException ignored) {
                // A malformed session duration must not prevent the summary loading.
            }
        }

        int recorded = present + late + absent;
        attendedValue.setText(String.valueOf(present + late));
        rateValue.setText(recorded == 0 ? "N/A"
                : String.format("%.1f%%", (present + late) * 100.0 / recorded));
        upcomingValue.setText(String.valueOf(upcoming));
        hoursValue.setText(String.format("%.1f h", minutesAttended / 60.0));
        barChart.setData(disciplines);
        pieChart.setData(present, late, absent);
    }

    private static class BarChart extends JPanel {
        private Map<String, Integer> values = new LinkedHashMap<>();

        BarChart() { setOpaque(false); }

        void setData(Map<String, Integer> data) {
            values = new LinkedHashMap<>(data);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int left = 42, right = 16, top = 20, bottom = 55;
                int usableWidth = Math.max(1, getWidth() - left - right);
                int usableHeight = Math.max(1, getHeight() - top - bottom);
                int maximum = 0;
                for (int count : values.values()) maximum = Math.max(maximum, count);
                if (maximum == 0) {
                    emptyMessage(g, getWidth(), getHeight(), "No attended classes recorded");
                    return;
                }
                int scale = Math.max(4, maximum);
                g.setFont(new Font("SansSerif", Font.PLAIN, 11));
                for (int tick = 0; tick <= 4; tick++) {
                    int y = top + usableHeight - tick * usableHeight / 4;
                    g.setColor(BORDER);
                    g.drawLine(left, y, left + usableWidth, y);
                    g.setColor(MUTED);
                    g.drawString(String.valueOf(tick * scale / 4), 7, y + 4);
                }
                int slot = usableWidth / Math.max(1, values.size());
                int i = 0;
                for (Map.Entry<String, Integer> entry : values.entrySet()) {
                    int barWidth = Math.max(12, Math.min(48, slot - 22));
                    int x = left + i * slot + (slot - barWidth) / 2;
                    int barHeight = entry.getValue() * usableHeight / scale;
                    int y = top + usableHeight - barHeight;
                    g.setColor(CHART_COLORS[i % CHART_COLORS.length]);
                    g.fillRoundRect(x, y, barWidth, barHeight, 6, 6);
                    g.setColor(TEXT);
                    String count = String.valueOf(entry.getValue());
                    g.drawString(count, x + (barWidth - g.getFontMetrics().stringWidth(count)) / 2,
                            Math.max(top + 10, y - 5));
                    String name = entry.getKey();
                    if (name.length() > 10) name = name.substring(0, 9) + "…";
                    int tx = left + i * slot + (slot - g.getFontMetrics().stringWidth(name)) / 2;
                    g.drawString(name, tx, top + usableHeight + 19);
                    i++;
                }
            } finally {
                g.dispose();
            }
        }
    }

    private static class AttendancePieChart extends JPanel {
        private int[] values = new int[3];
        private final String[] labels = {"Present", "Late", "Absent"};
        private final Color[] colors = {BLUE, new Color(127, 151, 176),
            new Color(210, 220, 231)};

        AttendancePieChart() { setOpaque(false); }

        void setData(int present, int late, int absent) {
            values = new int[]{present, late, absent};
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int total = values[0] + values[1] + values[2];
                if (total == 0) {
                    emptyMessage(g, getWidth(), getHeight(), "No attendance records yet");
                    return;
                }
                int diameter = Math.max(80, Math.min(getWidth() - 60, getHeight() - 100));
                int x = (getWidth() - diameter) / 2;
                int y = 12;
                double start = 90;
                for (int i = 0; i < values.length; i++) {
                    if (values[i] == 0) continue;
                    double angle = values[i] * 360.0 / total;
                    g.setColor(colors[i]);
                    g.fill(new Arc2D.Double(x, y, diameter, diameter,
                            start, -angle, Arc2D.PIE));
                    start -= angle;
                }
                g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                int lineY = y + diameter + 23;
                int legendWidth = 0;
                for (String label : labels) legendWidth += g.getFontMetrics().stringWidth(label) + 45;
                int legendX = Math.max(4, (getWidth() - legendWidth) / 2);
                for (int i = 0; i < 3; i++) {
                    g.setColor(colors[i]);
                    g.fillRoundRect(legendX, lineY - 10, 12, 12, 3, 3);
                    g.setColor(TEXT);
                    g.drawString(labels[i], legendX + 17, lineY);
                    legendX += g.getFontMetrics().stringWidth(labels[i]) + 45;
                }
            } finally {
                g.dispose();
            }
        }
    }

    private static void emptyMessage(Graphics2D g, int width, int height, String message) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(MUTED);
        int textWidth = g.getFontMetrics().stringWidth(message);
        g.drawString(message, Math.max(5, (width - textWidth) / 2), height / 2);
    }
}
