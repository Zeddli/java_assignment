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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class InstructorRatingPanel extends JPanel {

    private static final Color BLUE_DARK = new Color(55, 68, 88);
    private static final Color BLUE_PALE = new Color(237, 240, 245);
    private static final Color TEXT = new Color(40, 51, 69);
    private static final Color MUTED = new Color(101, 114, 132);
    private static final Color LINE = new Color(218, 227, 239);
    private static final Font BODY_FONT = new Font("SansSerif", Font.PLAIN, 13);

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

    private final JComboBox<String> instructorBox = new JComboBox<>();
    private final JComboBox<String> bookingBox = new JComboBox<>();
    private final JComboBox<Integer> scoreBox =
            new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});

    private final JTextArea commentArea = new JTextArea(4, 18);
    private final JLabel averageLabel = new JLabel("Select an instructor");
    private final JLabel ratingsGivenLabel = new JLabel("0");
    private final JLabel averageGivenLabel = new JLabel("–");
    private final List<Booking> eligibleBookings = new ArrayList<>();

    private final DefaultTableModel ratingsModel =
            new DefaultTableModel(
                    new String[]{"Class", "Instructor", "Score", "Comment"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    private final JTable ratingsTable = new JTable(ratingsModel);

    public InstructorRatingPanel(User student) {
        this.student = student;

        setLayout(new BorderLayout(12, 12));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        buildInterface();
        styleTable(ratingsTable);
        refreshData();

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                refreshData();
            }
        });
    }

    private void buildInterface() {
        JPanel topPanel = new JPanel(new BorderLayout(0, 16));
        topPanel.setOpaque(false);

        JPanel headingPanel = new JPanel(new GridLayout(2, 1, 0, 3));
        headingPanel.setOpaque(false);

        JLabel heading = new JLabel("Instructor Rating");
        heading.setFont(new Font("SansSerif", Font.BOLD, 21));
        heading.setForeground(TEXT);

        JLabel subheading = new JLabel(
                "Rate your instructors after attending a class and review your feedback");
        subheading.setFont(BODY_FONT);
        subheading.setForeground(MUTED);

        headingPanel.add(heading);
        headingPanel.add(subheading);
        topPanel.add(headingPanel, BorderLayout.NORTH);

        JPanel instructorPanel = new JPanel(new BorderLayout(16, 0));
        instructorPanel.setOpaque(false);

        JPanel selector = new JPanel(new BorderLayout(0, 6));
        selector.setOpaque(false);
        selector.add(fieldLabel("View instructor"), BorderLayout.NORTH);

        instructorBox.setFont(BODY_FONT);
        instructorBox.setBackground(Color.WHITE);
        instructorBox.setPreferredSize(new Dimension(390, 34));
        selector.add(instructorBox, BorderLayout.CENTER);
        instructorPanel.add(selector, BorderLayout.CENTER);

        JPanel averagePanel = new JPanel(new BorderLayout(0, 6));
        averagePanel.setBackground(new Color(248, 250, 254));
        averagePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        averagePanel.setPreferredSize(new Dimension(303, 60));

        JLabel averageTitle = fieldLabel("Instructor average rating");
        averageLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        averageLabel.setForeground(BLUE_DARK);
        averagePanel.add(averageTitle, BorderLayout.NORTH);
        averagePanel.add(averageLabel, BorderLayout.CENTER);
        instructorPanel.add(averagePanel, BorderLayout.EAST);

        topPanel.add(instructorPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(16, 0));
        contentPanel.setOpaque(false);

        JPanel historyPanel = new JPanel(new BorderLayout(0, 8));
        historyPanel.setOpaque(false);
        historyPanel.add(sectionHeading("Ratings I Have Given"), BorderLayout.NORTH);

        ratingsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ratingsTable.setFillsViewportHeight(true);
        ratingsTable.getTableHeader().setReorderingAllowed(false);
        ratingsTable.getColumnModel().getColumn(3).setPreferredWidth(250);
        historyPanel.add(tableScroll(ratingsTable), BorderLayout.CENTER);
        contentPanel.add(historyPanel, BorderLayout.CENTER);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setPreferredSize(new Dimension(303, 420));
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(16, 14, 16, 14)));

        JLabel formTitle = sectionHeading("Rate an Instructor");
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(formTitle);

        formPanel.add(Box.createVerticalStrut(14));
        formPanel.add(createRatingScalePanel());
        formPanel.add(Box.createVerticalStrut(17));

        addFormLabel(formPanel, "Attended Class");
        bookingBox.setFont(BODY_FONT);
        bookingBox.setBackground(Color.WHITE);
        bookingBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        bookingBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        bookingBox.setToolTipText("Choose a completed class with PRESENT or LATE attendance");
        formPanel.add(bookingBox);

        formPanel.add(Box.createVerticalStrut(16));
        addFormLabel(formPanel, "Score (1 - 5)");
        scoreBox.setFont(BODY_FONT);
        scoreBox.setBackground(Color.WHITE);
        scoreBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        scoreBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(scoreBox);

        formPanel.add(Box.createVerticalStrut(16));
        addFormLabel(formPanel, "Comment (Optional)");
        commentArea.setFont(BODY_FONT);
        commentArea.setForeground(TEXT);
        commentArea.setLineWrap(true);
        commentArea.setWrapStyleWord(true);
        commentArea.setMargin(new Insets(7, 9, 7, 9));

        JScrollPane commentScrollPane = new JScrollPane(commentArea);
        commentScrollPane.setBorder(BorderFactory.createLineBorder(LINE));
        commentScrollPane.setPreferredSize(new Dimension(270, 100));
        commentScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        commentScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(commentScrollPane);

        formPanel.add(Box.createVerticalStrut(18));
        JButton submitButton = new JButton("Submit Rating");
        JButton clearButton = new JButton("Clear");

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        actions.add(submitButton);
        actions.add(clearButton);
        formPanel.add(actions);

        formPanel.add(Box.createVerticalGlue());
        formPanel.add(createRatingSummaryPanel());
        contentPanel.add(formPanel, BorderLayout.EAST);
        add(contentPanel, BorderLayout.CENTER);

        instructorBox.addActionListener(e -> updateInstructorAverage());
        submitButton.addActionListener(e -> submitRating());
        clearButton.addActionListener(e -> {
            scoreBox.setSelectedIndex(0);
            commentArea.setText("");
        });
    }

    private JPanel createRatingScalePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBackground(new Color(248, 250, 254));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(9, 10, 9, 10)));

        JLabel heading = sectionHeading("Rating Scale");
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(heading);
        panel.add(Box.createVerticalStrut(7));

        String[] labels = {
            "Excellent", "Good", "Average", "Below Average", "Poor"
        };

        for (int score = 5; score >= 1; score--) {
            JPanel row = new JPanel(new BorderLayout(5, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel stars = new JLabel("★".repeat(score));
            stars.setFont(new Font("SansSerif", Font.PLAIN, 13));
            stars.setForeground(new Color(168, 121, 38));
            row.add(stars, BorderLayout.WEST);

            JLabel meaning = fieldLabel(labels[5 - score]);
            meaning.setHorizontalAlignment(SwingConstants.RIGHT);
            row.add(meaning, BorderLayout.CENTER);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 23));
            panel.add(row);
        }

        return panel;
    }

    private JPanel createRatingSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 9));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setOpaque(false);

        panel.add(sectionHeading("Your Rating Summary"), BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 2, 9, 0));
        cards.setOpaque(false);
        cards.add(createSummaryCard("Ratings Given", ratingsGivenLabel));
        cards.add(createSummaryCard("Average Given", averageGivenLabel));
        panel.add(cards, BorderLayout.CENTER);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 98));
        return panel;
    }

    private JPanel createSummaryCard(String title, JLabel value) {
        JPanel card = new JPanel(new BorderLayout(0, 7));
        card.setBackground(new Color(248, 250, 254));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(10, 9, 10, 9)));
        card.add(fieldLabel(title), BorderLayout.NORTH);
        value.setFont(new Font("SansSerif", Font.BOLD, 15));
        value.setForeground(BLUE_DARK);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private static void addFormLabel(JPanel panel, String text) {
        JLabel label = fieldLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(6));
    }

    private static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(MUTED);
        return label;
    }

    private static JLabel sectionHeading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 15));
        label.setForeground(TEXT);
        return label;
    }

    private static JScrollPane tableScroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(LINE));
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    private static void styleTable(JTable table) {
        table.setFont(BODY_FONT);
        table.setForeground(TEXT);
        table.setRowHeight(30);
        table.setGridColor(LINE);
        table.setShowVerticalLines(false);
        table.setSelectionBackground(BLUE_PALE);
        table.setSelectionForeground(TEXT);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(BLUE_PALE);
        table.getTableHeader().setForeground(BLUE_DARK);
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean selected, boolean focused,
                    int row, int column) {
                super.getTableCellRendererComponent(
                        table, value, selected, focused, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 7));
                setBackground(selected ? BLUE_PALE
                        : (row % 2 == 0 ? Color.WHITE : new Color(248, 250, 254)));
                setForeground(TEXT);
                return this;
            }
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
        int ratingCount = 0;
        int ratingTotal = 0;

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
            ratingCount++;
            ratingTotal += rating.getScore();
        }

        ratingsGivenLabel.setText(String.valueOf(ratingCount));
        averageGivenLabel.setText(ratingCount == 0
                ? "–"
                : String.format("%.1f / 5", (double) ratingTotal / ratingCount));
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