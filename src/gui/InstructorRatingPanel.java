package gui;

import model.AttendanceRecord;
import model.Booking;
import model.ClassSession;
import model.Rating;
import model.Role;
import model.User;

import repository.AttendanceRepository;
import repository.BookingRepository;
import repository.ClassRepository;
import repository.RatingRepository;
import repository.UserRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class InstructorRatingPanel extends JPanel {

    private final User student;

    private final AttendanceRepository attendanceRepository =
            new AttendanceRepository();
    private final BookingRepository bookingRepository =
            new BookingRepository();
    private final ClassRepository classRepository =
            new ClassRepository();
    private final RatingRepository ratingRepository =
            new RatingRepository();
    private final UserRepository userRepository =
            new UserRepository();

    private final JComboBox<String> instructorBox =
            new JComboBox<>();

    private final JComboBox<String> bookingBox =
            new JComboBox<>();

    private final JComboBox<Integer> scoreBox =
            new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});

    private final JTextArea commentArea = new JTextArea(4, 18);

    private final JLabel averageLabel =
            new JLabel("Select an instructor");

    private final List<Booking> eligibleBookings =
            new ArrayList<>();

    private final DefaultTableModel ratingsModel =
            new DefaultTableModel(
                    new String[]{
        "Class", "Instructor", "Score", "Comment"
    }, 0) {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public InstructorRatingPanel(User student) {
        this.student = student;

        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        buildInterface();
        refreshData();

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                refreshData();
            }
        });
    }

    private void buildInterface() {

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);

        JLabel heading = new JLabel("Instructor Rating");
        heading.setFont(new Font("SansSerif", Font.BOLD, 20));

        topPanel.add(heading, BorderLayout.NORTH);

        JPanel instructorPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT));

        instructorPanel.setOpaque(false);
        instructorPanel.add(new JLabel("View Instructor:"));
        instructorPanel.add(instructorBox);
        instructorPanel.add(averageLabel);

        topPanel.add(instructorPanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(12, 0));
        contentPanel.setOpaque(false);

        JPanel historyPanel = new JPanel(new BorderLayout(0, 8));
        historyPanel.setOpaque(false);

        historyPanel.add(
                new JLabel("Ratings I Have Given"),
                BorderLayout.NORTH
        );

        historyPanel.add(
                new JScrollPane(new JTable(ratingsModel)),
                BorderLayout.CENTER
        );

        contentPanel.add(historyPanel, BorderLayout.CENTER);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(
                formPanel, BoxLayout.Y_AXIS));

        formPanel.setPreferredSize(new Dimension(300, 350));

        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Rate an Instructor"),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        // Keep dropdowns compact.
        bookingBox.setMaximumSize(new Dimension(
                Integer.MAX_VALUE, 35));

        scoreBox.setMaximumSize(new Dimension(
                Integer.MAX_VALUE, 35));

        bookingBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        scoreBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Centre the form controls vertically.
        formPanel.add(Box.createVerticalGlue());

        formPanel.add(new JLabel("Attended Class"));
        formPanel.add(bookingBox);

        formPanel.add(Box.createVerticalStrut(12));

        formPanel.add(new JLabel("Score (1 - 5)"));
        formPanel.add(scoreBox);

        formPanel.add(Box.createVerticalStrut(12));

        formPanel.add(new JLabel("Comment (Optional)"));

        JScrollPane commentScrollPane = new JScrollPane(commentArea);

        commentScrollPane.setPreferredSize(new Dimension(260, 90));
        commentScrollPane.setMaximumSize(new Dimension(
                Integer.MAX_VALUE, 90));

        commentScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        formPanel.add(commentScrollPane);

        formPanel.add(Box.createVerticalStrut(15));

        JButton submitButton = new JButton("Submit Rating");
        JButton clearButton = new JButton("Clear");

        JPanel actions = new JPanel(
                new FlowLayout(FlowLayout.LEFT));

        actions.add(submitButton);
        actions.add(clearButton);

        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.setMaximumSize(new Dimension(
                Integer.MAX_VALUE, 45));

        formPanel.add(actions);

        formPanel.add(Box.createVerticalGlue());

        // Match the height of the ratings history section.
        contentPanel.add(formPanel, BorderLayout.EAST);

        add(contentPanel, BorderLayout.CENTER);

        instructorBox.addActionListener(e ->
                updateInstructorAverage());

        submitButton.addActionListener(e ->
                submitRating());

        clearButton.addActionListener(e -> {
            scoreBox.setSelectedIndex(0);
            commentArea.setText("");
        });
    }

    private void refreshData() {
        refreshInstructors();
        refreshRatingHistory();
        refreshEligibleBookings();
    }

    private void refreshInstructors() {

        instructorBox.removeAllItems();
        instructorBox.addItem("Select Instructor");

        for (User user : userRepository.findAll()) {

            if (Role.INSTRUCTOR.equals(user.getRole())) {
                instructorBox.addItem(
                        user.getId() + " - " + user.getFullName()
                );
            }
        }

        updateInstructorAverage();
    }

    private void updateInstructorAverage() {

        Object selected = instructorBox.getSelectedItem();

        if (selected == null
                || selected.toString().equals("Select Instructor")) {

            averageLabel.setText("Select an instructor");
            return;
        }

        String instructorId =
                selected.toString().split(" - ", 2)[0];

        int total = 0;
        int count = 0;

        for (Rating rating : ratingRepository.findAll()) {

            if (rating.getInstructorId().equals(instructorId)) {
                total += rating.getScore();
                count++;
            }
        }

        if (count == 0) {
            averageLabel.setText("No ratings yet");
        } else {
            averageLabel.setText(String.format(
                    "Overall: %.1f / 5 (%d ratings)",
                    (double) total / count, count
            ));
        }
    }

    private void refreshRatingHistory() {

        ratingsModel.setRowCount(0);

        for (Rating rating : ratingRepository.findAll()) {

            if (!rating.getStudentId().equals(student.getId())) {
                continue;
            }

            Booking booking = bookingRepository.findById(
                    rating.getBookingId());

            ClassSession session = booking == null
                    ? null
                    : classRepository.findById(booking.getClassId());

            String className = session == null
                    ? "Unavailable"
                    : session.getDiscipline();

            ratingsModel.addRow(new Object[]{
                className,
                userRepository.nameOf(rating.getInstructorId()),
                rating.getScore(),
                rating.getComment()
            });
        }
    }

    private void refreshEligibleBookings() {

        eligibleBookings.clear();
        bookingBox.removeAllItems();

        for (Booking booking : bookingRepository.findAll()) {

            if (!booking.getStudentId().equals(student.getId())
                    || !booking.isBooked()) {
                continue;
            }

            if (ratingRepository.hasRatedBooking(booking.getId())) {
                continue;
            }

            AttendanceRecord attendance =
                    attendanceRepository.findByBooking(booking.getId());

            ClassSession session =
                    classRepository.findById(booking.getClassId());

            if (!isEligibleForRating(booking, attendance, session)) {
                continue;
            }

            eligibleBookings.add(booking);

            bookingBox.addItem(
                    booking.getId() + " - "
                    + session.getDiscipline()
                    + " (" + session.getDate() + ")"
            );

        }
    }

    private void submitRating() {

        int selectedIndex = bookingBox.getSelectedIndex();

        if (selectedIndex < 0
                || selectedIndex >= eligibleBookings.size()) {

            JOptionPane.showMessageDialog(this,
                    "No attended classes available to rate.");
            return;
        }

        Booking selectedBooking = eligibleBookings.get(selectedIndex);

        // Retrieve the latest booking information.
        Booking booking = bookingRepository.findById(
                selectedBooking.getId());

        AttendanceRecord attendance = booking == null
                ? null
                : attendanceRepository.findByBooking(booking.getId());

        ClassSession session = booking == null
                ? null
                : classRepository.findById(booking.getClassId());

        if (!isEligibleForRating(booking, attendance, session)
                || ratingRepository.hasRatedBooking(selectedBooking.getId())) {

            JOptionPane.showMessageDialog(this,
                    "This booking is not eligible for rating.");

            refreshData();
            return;
        }

        int score = (Integer) scoreBox.getSelectedItem();

        String comment = commentArea.getText().trim();

        if (comment.contains("|")
                || comment.contains("\n")
                || comment.contains("\r")) {

            JOptionPane.showMessageDialog(this,
                    "Please use a single-line comment without | characters.");
            return;
        }

        if (comment.length() > 200) {
            JOptionPane.showMessageDialog(this,
                    "Comment must not exceed 200 characters.");
            return;
        }

        int confirmation = JOptionPane.showConfirmDialog(
                this,
                "Submit a rating of " + score
                + "/5 for "
                + userRepository.nameOf(session.getInstructorId())
                + "?",
                "Confirm Rating",
                JOptionPane.YES_NO_OPTION
        );

        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }

        Rating rating = new Rating(
                ratingRepository.nextId("RAT"),
                booking.getId(),
                student.getId(),
                session.getInstructorId(),
                score,
                comment,
                LocalDate.now().toString()
        );

        try {
            ratingRepository.add(rating);

            commentArea.setText("");
            refreshData();

            JOptionPane.showMessageDialog(this,
                    "Instructor rating submitted successfully!");

        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Unable to save rating: " + ex.getMessage(),
                    "Rating Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private boolean isEligibleForRating(
            Booking booking,
            AttendanceRecord attendance,
            ClassSession session) {

        if (booking == null || attendance == null || session == null) {
            return false;
        }

        // The booking must belong to the logged-in student.
        if (!booking.getStudentId().equals(student.getId())
                || !booking.isBooked()) {
            return false;
        }

        // Attendance must match the booking, class and student.
        if (!attendance.getBookingId().equals(booking.getId())
                || !attendance.getStudentId().equals(student.getId())
                || !attendance.getClassId().equals(session.getId())
                || !booking.getClassId().equals(session.getId())) {
            return false;
        }

        if (session.isCancelled()) {
            return false;
        }

        // Only students marked PRESENT or LATE can rate.
        String status = attendance.getStatus();

        if (!"PRESENT".equals(status) && !"LATE".equals(status)) {
            return false;
        }

        // The class must have finished before it can be rated.
        try {
            LocalDateTime classEnd = LocalDateTime.of(
                    LocalDate.parse(session.getDate()),
                    LocalTime.parse(session.getEndTime())
            );

            return !classEnd.isAfter(LocalDateTime.now());

        } catch (RuntimeException ex) {
            return false;
        }
    }
}