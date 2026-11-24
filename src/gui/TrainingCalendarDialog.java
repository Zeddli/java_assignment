package gui;

import model.Booking;
import model.ClassSession;
import model.User;
import repository.BookingRepository;
import repository.ClassRepository;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrainingCalendarDialog extends JDialog {

    private final User student;
    private final BookingRepository bookingRepository =
            new BookingRepository();
    private final ClassRepository classRepository =
            new ClassRepository();

    private YearMonth displayedMonth = YearMonth.now();

    private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
    private final JPanel calendarGrid = new JPanel(new GridLayout(0, 7, 5, 5));
    private final JTextArea detailsArea = new JTextArea();

    private final Map<LocalDate, List<ClassSession>> scheduledClasses =
            new HashMap<>();

    public TrainingCalendarDialog(Window owner, User student) {
        super(owner, "My Training Calendar",
                Dialog.ModalityType.APPLICATION_MODAL);

        this.student = student;

        setSize(850, 570);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        buildInterface();
        loadBookings();
        refreshCalendar();
    }

    private void buildInterface() {

        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));

        JLabel title = new JLabel("My Training Calendar");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        topPanel.add(title, BorderLayout.NORTH);

        JPanel navigation = new JPanel(new BorderLayout());

        JButton previousButton = new JButton("< Previous");
        JButton nextButton = new JButton("Next >");

        monthLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        navigation.add(previousButton, BorderLayout.WEST);
        navigation.add(monthLabel, BorderLayout.CENTER);
        navigation.add(nextButton, BorderLayout.EAST);

        topPanel.add(navigation, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        calendarGrid.setBorder(
                BorderFactory.createEmptyBorder(10, 15, 10, 15));

        add(calendarGrid, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(
                BorderFactory.createEmptyBorder(5, 15, 15, 15));

        JLabel detailsLabel = new JLabel("Selected Date - Training Details");
        detailsLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        detailsArea.setEditable(false);
        detailsArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);

        bottomPanel.add(detailsLabel, BorderLayout.NORTH);
        bottomPanel.add(new JScrollPane(detailsArea), BorderLayout.CENTER);
        bottomPanel.setPreferredSize(new Dimension(800, 125));

        add(bottomPanel, BorderLayout.SOUTH);

        previousButton.addActionListener(e -> {
            displayedMonth = displayedMonth.minusMonths(1);
            refreshCalendar();
        });

        nextButton.addActionListener(e -> {
            displayedMonth = displayedMonth.plusMonths(1);
            refreshCalendar();
        });
    }

    private void loadBookings() {

        scheduledClasses.clear();

        for (Booking booking : bookingRepository.findAll()) {

            if (!booking.getStudentId().equals(student.getId())) {
                continue;
            }

            if (!booking.isBooked()) {
                continue;
            }

            ClassSession session =
                    classRepository.findById(booking.getClassId());

            if (session == null || session.isCancelled()) {
                continue;
            }

            try {
                LocalDate date = LocalDate.parse(session.getDate());

                scheduledClasses
                        .computeIfAbsent(date, k -> new ArrayList<>())
                        .add(session);

            } catch (RuntimeException ex) {
                // Skip records containing invalid dates.
            }
        }
    }

    private void refreshCalendar() {

        calendarGrid.removeAll();

        monthLabel.setText(displayedMonth.format(
                DateTimeFormatter.ofPattern("MMMM yyyy")));

        String[] weekdays = {
            "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
        };

        for (String day : weekdays) {
            JLabel header = new JLabel(day, SwingConstants.CENTER);
            header.setFont(new Font("SansSerif", Font.BOLD, 12));
            calendarGrid.add(header);
        }

        int startingDay =
                displayedMonth.atDay(1).getDayOfWeek().getValue();

        for (int i = 1; i < startingDay; i++) {
            calendarGrid.add(new JLabel(""));
        }

        for (int day = 1; day <= displayedMonth.lengthOfMonth(); day++) {

            LocalDate date = displayedMonth.atDay(day);

            List<ClassSession> sessions = scheduledClasses.get(date);
            int count = sessions == null ? 0 : sessions.size();

            String label = String.valueOf(day);

            if (count > 0) {
                label += " (" + count + ")";
            }

            JButton dayButton = new JButton(label);

            if (count > 0) {
                dayButton.setBackground(new Color(210, 235, 220));
                dayButton.setOpaque(true);
                dayButton.setFont(
                        new Font("SansSerif", Font.BOLD, 12));
            }

            if (date.equals(LocalDate.now())) {
                dayButton.setBorder(
                        BorderFactory.createLineBorder(Color.BLUE, 2));
            }

            dayButton.addActionListener(e -> showDayDetails(date));

            calendarGrid.add(dayButton);
        }

        detailsArea.setText("Select a date to view your training sessions.");

        calendarGrid.revalidate();
        calendarGrid.repaint();
    }

    private void showDayDetails(LocalDate date) {

        List<ClassSession> sessions = scheduledClasses.get(date);

        if (sessions == null || sessions.isEmpty()) {
            detailsArea.setText(
                    date + "\nNo training sessions booked for this date.");
            return;
        }

        StringBuilder details = new StringBuilder();
        details.append("Training sessions for ").append(date).append("\n\n");

        for (ClassSession session : sessions) {

            details.append("Class: ")
                    .append(session.getDiscipline()).append("\n");

            details.append("Time: ")
                    .append(session.getStartTime())
                    .append(" - ")
                    .append(session.getEndTime()).append("\n");

            details.append("Room: ")
                    .append(session.getRoom()).append("\n");

            details.append("Instructor ID: ")
                    .append(session.getInstructorId()).append("\n\n");
        }

        detailsArea.setText(details.toString());
        detailsArea.setCaretPosition(0);
    }
}