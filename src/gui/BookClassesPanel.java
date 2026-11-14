package gui;

import model.Booking;
import model.ClassSession;
import model.User;
import repository.BookingRepository;
import repository.ClassRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class BookClassesPanel extends JPanel {

    private final User student;
    private final ClassRepository classRepository =
            new ClassRepository();
    private final BookingRepository bookingRepository =
            new BookingRepository();

    private final JTextField searchField = new JTextField(14);
    private final JComboBox<String> disciplineFilter =
            new JComboBox<>(new String[]{
        "All", "Karate", "Judo", "Taekwondo", "MMA"
    });
    private final JTextField dateField = new JTextField(10);

    private final DefaultTableModel classesModel =
            createTableModel("Class ID", "Discipline",
                    "Instructor ID", "Date", "Time",
                    "Fee (RM)", "Free Slots");

    private final DefaultTableModel bookingsModel =
            createTableModel("Booking ID", "Class ID",
                    "Date", "Status", "Payment");

    private final JTable classesTable = new JTable(classesModel);
    private final JTable bookingsTable = new JTable(bookingsModel);

    public BookClassesPanel(User student) {
        this.student = student;

        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        buildInterface();

        // Improve table readability.
        classesTable.setRowHeight(30);
        bookingsTable.setRowHeight(30);

        // Allow only one row to be selected at a time.
        classesTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        bookingsTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        // Fill the remaining table area neatly.
        classesTable.setFillsViewportHeight(true);
        bookingsTable.setFillsViewportHeight(true);

        // Prevent users from dragging table columns into a different order.
        classesTable.getTableHeader().setReorderingAllowed(false);
        bookingsTable.getTableHeader().setReorderingAllowed(false);

        // Improve column sizing.
        classesTable.getColumnModel().getColumn(4)
                .setPreferredWidth(150);

        bookingsTable.getColumnModel().getColumn(0)
                .setPreferredWidth(110);

        // Improve table header appearance.
        Font tableHeaderFont = new Font("SansSerif", Font.BOLD, 13);

        classesTable.getTableHeader().setFont(tableHeaderFont);
        bookingsTable.getTableHeader().setFont(tableHeaderFont);

        classesTable.getTableHeader().setPreferredSize(
                new Dimension(0, 35));

        bookingsTable.getTableHeader().setPreferredSize(
                new Dimension(0, 35));

        refreshTables();

        // Refresh booking records whenever this panel becomes visible.
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                refreshTables();
            }
        });
    }

    private DefaultTableModel createTableModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void buildInterface() {

        // Page heading
        JPanel topPanel = new JPanel(new BorderLayout(0, 10));
        topPanel.setOpaque(false);

        JLabel heading = new JLabel("Book Classes");
        heading.setFont(new Font("SansSerif", Font.BOLD, 20));
        topPanel.add(heading, BorderLayout.NORTH);

        // Search toolbar
        JPanel searchPanel = new JPanel(new FlowLayout(
                FlowLayout.LEFT, 8, 0));
        searchPanel.setOpaque(false);

        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show All");

        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(disciplineFilter);
        searchPanel.add(new JLabel("Date:"));
        searchPanel.add(dateField);
        searchPanel.add(searchButton);
        searchPanel.add(showAllButton);

        topPanel.add(searchPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // Tables
        JPanel tablesPanel = new JPanel(new GridLayout(2, 1, 0, 20));
        tablesPanel.setOpaque(false);

        JPanel availablePanel = new JPanel(new BorderLayout(0, 5));
        availablePanel.setOpaque(false);
        availablePanel.add(
                new JLabel("Available Classes"), BorderLayout.NORTH);
        availablePanel.add(
                new JScrollPane(classesTable), BorderLayout.CENTER);

        JButton bookButton = new JButton("Book Selected Class");
        bookButton.setEnabled(false);

        classesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                bookButton.setEnabled(classesTable.getSelectedRow() != -1);
            }
        });

        bookButton.addActionListener(e -> bookSelectedClass());

        JPanel bookActions = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 10, 8));
        bookActions.setOpaque(false);
        bookActions.add(bookButton);

        // Extra feature: My Training Calendar.
        JButton calendarButton = new JButton("My Training Calendar");

        calendarButton.addActionListener(e -> {
            TrainingCalendarDialog calendar =
            new TrainingCalendarDialog(
                    SwingUtilities.getWindowAncestor(this),
                    student
            );

            calendar.setVisible(true);
        });

        bookActions.add(calendarButton);

        availablePanel.add(bookActions, BorderLayout.SOUTH);
        tablesPanel.add(availablePanel);

        JPanel bookingsPanel = new JPanel(new BorderLayout(0, 5));
        bookingsPanel.setOpaque(false);
        bookingsPanel.add(
                new JLabel("My Bookings"), BorderLayout.NORTH);
        bookingsPanel.add(
                new JScrollPane(bookingsTable), BorderLayout.CENTER);

        JButton rescheduleButton =
                new JButton("Reschedule Selected");
        JButton cancelButton = new JButton("Cancel Selected");

        rescheduleButton.setEnabled(false);
        cancelButton.setEnabled(false);

        // Enable cancellation when a booking is selected.
        bookingsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = bookingsTable.getSelectedRow();

                if (selectedRow == -1) {
                    cancelButton.setEnabled(false);
                    rescheduleButton.setEnabled(false);
                    return;
                }

                int modelRow = bookingsTable.convertRowIndexToModel(selectedRow);

                String status = bookingsModel.getValueAt(modelRow, 3).toString();
                String payment = bookingsModel.getValueAt(modelRow, 4).toString();

                boolean canModify = "BOOKED".equals(status)
                && "UNPAID".equals(payment);

                cancelButton.setEnabled(canModify);
                rescheduleButton.setEnabled(canModify);
            }
        });

        // Handle cancellation when clicked.
        rescheduleButton.addActionListener(e -> rescheduleSelectedBooking());
        cancelButton.addActionListener(e -> cancelSelectedBooking());

        JPanel bookingActions = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 10, 8));
        bookingActions.setOpaque(false);
        bookingActions.add(rescheduleButton);
        bookingActions.add(cancelButton);

        bookingsPanel.add(bookingActions, BorderLayout.SOUTH);
        tablesPanel.add(bookingsPanel);

        add(tablesPanel, BorderLayout.CENTER);

        // Search actions
        searchButton.addActionListener(e -> refreshTables());

        showAllButton.addActionListener(e -> {
            searchField.setText("");
            dateField.setText("");
            disciplineFilter.setSelectedIndex(0);
            refreshTables();
        });
    }

    private void refreshTables() {

        classesModel.setRowCount(0);
        bookingsModel.setRowCount(0);

        String query = searchField.getText()
                .trim().toLowerCase();

        String discipline =
                (String) disciplineFilter.getSelectedItem();

        String date = dateField.getText().trim();

        for (ClassSession session : classRepository.findAll()) {

            if (session.isCancelled()) continue;

            boolean matchesSearch =
                    session.getId().toLowerCase().contains(query)
                    || session.getDiscipline()
                    .toLowerCase().contains(query)
                    || session.getInstructorId()
                    .toLowerCase().contains(query);

            boolean matchesDiscipline =
                    "All".equals(discipline)
                    || session.getDiscipline()
                    .equalsIgnoreCase(discipline);

            boolean matchesDate =
                    date.isEmpty()
                    || session.getDate().equals(date);

            if (!matchesSearch
                    || !matchesDiscipline
                    || !matchesDate) {
                continue;
            }

            int bookedCount = bookingRepository
                    .findBookedByClass(session.getId()).size();

            int freeSlots = Math.max(
                    0, session.getCapacity() - bookedCount);

            classesModel.addRow(new Object[]{
                session.getId(),
                session.getDiscipline(),
                session.getInstructorId(),
                session.getDate(),
                session.getStartTime() + " - "
                + session.getEndTime(),
                String.format("%.2f", session.getFee()),
                freeSlots
            });
        }

        for (Booking booking : bookingRepository.findAll()) {

            if (!booking.getStudentId()
                    .equals(student.getId())) {
                continue;
            }

            ClassSession session = classRepository
                    .findById(booking.getClassId());

            String classDate = session == null
                    ? "-"
                    : session.getDate();

            bookingsModel.addRow(new Object[]{
                booking.getId(),
                booking.getClassId(),
                classDate,
                booking.getStatus(),
                booking.getPaymentStatus()
            });
        }
    }

    private void bookSelectedClass() {

        int selectedRow = classesTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a class first.");
            return;
        }

        int modelRow = classesTable.convertRowIndexToModel(selectedRow);

        String classId = classesModel.getValueAt(
                modelRow, 0).toString();

        // Get the class session from the repository.
        ClassSession session = classRepository.findById(classId);

        if (session == null) {
            JOptionPane.showMessageDialog(this,
                    "This class could not be found.");
            refreshTables();
            return;
        }

        // Check that the class is open.
        if (!"OPEN".equalsIgnoreCase(session.getStatus())) {
            JOptionPane.showMessageDialog(this,
                    "This class is not available for booking.");
            return;
        }

        // Check whether the class has already started.
        try {
            LocalDateTime classStart = LocalDateTime.of(
                    LocalDate.parse(session.getDate()),
                    LocalTime.parse(session.getStartTime())
            );

            if (!classStart.isAfter(LocalDateTime.now())) {
                JOptionPane.showMessageDialog(this,
                        "You cannot book a class that has already started.");
                return;
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this,
                    "The selected class has an invalid date or time.");
            return;
        }

        // Check available capacity.
        int bookedCount = bookingRepository
                .findBookedByClass(classId).size();

        if (bookedCount >= session.getCapacity()) {
            JOptionPane.showMessageDialog(this,
                    "Sorry, this class is already full.");
            refreshTables();
            return;
        }

        // Prevent duplicate bookings.
        for (Booking existing : bookingRepository.findAll()) {
            if (existing.getStudentId().equals(student.getId())
                    && existing.getClassId().equals(classId)
                    && existing.isBooked()) {

                JOptionPane.showMessageDialog(this,
                        "You have already booked this class.");
                return;
            }
        }

        // Prevent overlapping bookings.
        if (hasOverlappingBooking(session, null)) {
            JOptionPane.showMessageDialog(this,
                    "You already have another class booked during this time.");
            return;
        }

        // Ask for confirmation.
        int confirmation = JOptionPane.showConfirmDialog(
                this,
                "Book " + session.getDiscipline()
                + " on " + session.getDate()
                + " at " + session.getStartTime()
                + "\nFee: RM "
                + String.format("%.2f", session.getFee()),
                "Confirm Booking",
                JOptionPane.YES_NO_OPTION
        );

        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }

        // Create and save the booking.
        try {
            String bookingId = bookingRepository.nextId("BKG");

            String bookedAt = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            );

            Booking booking = new Booking(
                    bookingId,
                    student.getId(),
                    classId,
                    bookedAt,
                    "BOOKED",
                    "UNPAID"
            );

            bookingRepository.add(booking);

            refreshTables();

            JOptionPane.showMessageDialog(this,
                    "Class booked successfully!\n"
                    + "Booking ID: " + bookingId);

        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Unable to save booking: " + ex.getMessage(),
                    "Booking Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void cancelSelectedBooking() {

        int selectedRow = bookingsTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a booking first.");
            return;
        }

        int modelRow = bookingsTable.convertRowIndexToModel(selectedRow);
        String bookingId = bookingsModel.getValueAt(modelRow, 0).toString();

        // Retrieve the latest booking record.
        Booking booking = bookingRepository.findById(bookingId);

        if (booking == null || !booking.getStudentId().equals(student.getId())) {
            JOptionPane.showMessageDialog(this,
                    "Booking not found or access denied.");
            refreshTables();
            return;
        }

        if (!booking.isBooked()) {
            JOptionPane.showMessageDialog(this,
                    "This booking has already been cancelled.");
            refreshTables();
            return;
        }

        // Paid bookings require an agreed refund policy.
        if (!"UNPAID".equals(booking.getPaymentStatus())) {
            JOptionPane.showMessageDialog(this,
                    "Paid bookings cannot be cancelled here.\n"
                    + "Please contact the administrator.");
            return;
        }

        ClassSession session = classRepository.findById(booking.getClassId());

        if (session == null) {
            JOptionPane.showMessageDialog(this,
                    "The associated class could not be found.");
            return;
        }

        // Only future classes can be cancelled.
        try {
            LocalDateTime classStart = LocalDateTime.of(
                    LocalDate.parse(session.getDate()),
                    LocalTime.parse(session.getStartTime())
            );

            if (!classStart.isAfter(LocalDateTime.now())) {
                JOptionPane.showMessageDialog(this,
                        "You cannot cancel a class that has already started.");
                return;
            }
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this,
                    "The class date or time is invalid.");
            return;
        }

        int confirmation = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to cancel booking " + bookingId + "?",
                "Confirm Cancellation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }

        // Booking is immutable, so create a replacement record.
        Booking cancelledBooking = new Booking(
                booking.getId(),
                booking.getStudentId(),
                booking.getClassId(),
                booking.getBookedAt(),
                "CANCELLED",
                booking.getPaymentStatus()
        );

        try {
            boolean updated = bookingRepository.update(cancelledBooking);

            if (!updated) {
                JOptionPane.showMessageDialog(this,
                        "Booking could not be updated.");
                return;
            }

            refreshTables();

            JOptionPane.showMessageDialog(this,
                    "Booking " + bookingId + " cancelled successfully!");

        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Unable to cancel booking: " + ex.getMessage(),
                    "Cancellation Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void rescheduleSelectedBooking() {

        int selectedRow = bookingsTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a booking first.");
            return;
        }

        int modelRow = bookingsTable.convertRowIndexToModel(selectedRow);
        String bookingId = bookingsModel.getValueAt(modelRow, 0).toString();

        // Retrieve the booking and verify ownership.
        Booking booking = bookingRepository.findById(bookingId);

        if (booking == null || !booking.getStudentId().equals(student.getId())) {
            JOptionPane.showMessageDialog(this,
                    "Booking not found or access denied.");
            refreshTables();
            return;
        }

        if (!booking.isBooked()) {
            JOptionPane.showMessageDialog(this,
                    "Cancelled bookings cannot be rescheduled.");
            return;
        }

        if (!"UNPAID".equals(booking.getPaymentStatus())) {
            JOptionPane.showMessageDialog(this,
                    "Paid bookings cannot be rescheduled here.\n"
                    + "Please contact the administrator.");
            return;
        }

        ClassSession currentClass = classRepository.findById(
                booking.getClassId());

        if (currentClass == null) {
            JOptionPane.showMessageDialog(this,
                    "The original class could not be found.");
            return;
        }

        // Rescheduling is only allowed before the original class starts.
        try {
            if (currentClass.hasStarted()) {
                JOptionPane.showMessageDialog(this,
                        "You cannot reschedule a class that has already started.");
                return;
            }
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this,
                    "The original class has an invalid date or time.");
            return;
        }

        // Find other eligible classes.
        java.util.List<ClassSession> availableClasses =
                new java.util.ArrayList<>();

        java.util.List<String> classOptions =
                new java.util.ArrayList<>();

        for (ClassSession session : classRepository.findAll()) {

            if (session.getId().equals(booking.getClassId())) {
                continue;
            }

            if (!"OPEN".equalsIgnoreCase(session.getStatus())) {
                continue;
            }

            try {
                if (session.hasStarted()) {
                    continue;
                }
            } catch (RuntimeException ex) {
                continue;
            }

            int bookedCount = bookingRepository
                    .findBookedByClass(session.getId()).size();

            if (bookedCount >= session.getCapacity()) {
                continue;
            }

            // Prevent duplicate bookings for the replacement class.
            boolean alreadyBooked = false;

            for (Booking existing : bookingRepository.findAll()) {
                if (existing.getStudentId().equals(student.getId())
                        && existing.getClassId().equals(session.getId())
                        && existing.isBooked()) {
                    alreadyBooked = true;
                    break;
                }
            }

            if (alreadyBooked) {
                continue;
            }

            // Exclude replacement classes that overlap other bookings
            if (hasOverlappingBooking(session, booking.getId())) {
                continue;
            }

            availableClasses.add(session);

            classOptions.add(
                    session.getId() + " | "
                    + session.getDiscipline() + " | "
                    + session.getDate() + " "
                    + session.getStartTime() + " | RM "
                    + String.format("%.2f", session.getFee())
            );
        }

        if (availableClasses.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No alternative classes are currently available.");
            return;
        }

        // Ask the student to select a replacement class.
        Object selectedOption = JOptionPane.showInputDialog(
                this,
                "Choose a new class for booking " + bookingId + ":",
                "Reschedule Booking",
                JOptionPane.QUESTION_MESSAGE,
                null,
                classOptions.toArray(),
                classOptions.get(0)
        );

        if (selectedOption == null) {
            return;
        }

        int selectedIndex = classOptions.indexOf(selectedOption.toString());

        if (selectedIndex == -1) {
            return;
        }

        ClassSession replacement = availableClasses.get(selectedIndex);

        // Recheck availability using the latest records.
        ClassSession latestClass = classRepository.findById(
                replacement.getId());

        if (latestClass == null
                || !"OPEN".equalsIgnoreCase(latestClass.getStatus())) {
            JOptionPane.showMessageDialog(this,
                    "The selected replacement class is no longer available.");
            refreshTables();
            return;
        }

        try {
            if (latestClass.hasStarted()) {
                JOptionPane.showMessageDialog(this,
                        "The replacement class has already started.");
                refreshTables();
                return;
            }
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this,
                    "The replacement class has an invalid date or time.");
            return;
        }

        int latestBookedCount = bookingRepository
                .findBookedByClass(latestClass.getId()).size();

        if (latestBookedCount >= latestClass.getCapacity()) {
            JOptionPane.showMessageDialog(this,
                    "The replacement class is now full.");
            refreshTables();
            return;
        }

        for (Booking existing : bookingRepository.findAll()) {
            if (existing.getStudentId().equals(student.getId())
                    && existing.getClassId().equals(latestClass.getId())
                    && existing.isBooked()) {
                JOptionPane.showMessageDialog(this,
                        "You have already booked the replacement class.");
                return;
            }
        }

        // Recheck for scheduling conflicts before confirmation.
        if (hasOverlappingBooking(latestClass, booking.getId())) {
            JOptionPane.showMessageDialog(this,
                    "This replacement class overlaps another booking.");
            refreshTables();
            return;
        }

        int confirmation = JOptionPane.showConfirmDialog(
                this,
                "Reschedule booking " + bookingId + "?\n\n"
                + "From: " + currentClass.getDiscipline()
                + " (" + currentClass.getDate() + ")\n"
                + "To: " + latestClass.getDiscipline()
                + " (" + latestClass.getDate() + ")\n\n"
                + "New class fee: RM "
                + String.format("%.2f", latestClass.getFee()),
                "Confirm Rescheduling",
                JOptionPane.YES_NO_OPTION
        );

        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }

        // Recheck the booking after the confirmation dialog.
        Booking latestBooking = bookingRepository.findById(bookingId);

        if (latestBooking == null
                || !latestBooking.getStudentId().equals(student.getId())
                || !latestBooking.isBooked()
                || !"UNPAID".equals(latestBooking.getPaymentStatus())
                || !latestBooking.getClassId().equals(booking.getClassId())) {

            JOptionPane.showMessageDialog(this,
                    "This booking has changed. Please try again.");

            refreshTables();
            return;
        }

        // Recheck the replacement class after confirmation.
        ClassSession confirmedClass =
                classRepository.findById(latestClass.getId());

        if (confirmedClass == null
                || !"OPEN".equalsIgnoreCase(confirmedClass.getStatus())
                || confirmedClass.hasStarted()
                || bookingRepository.findBookedByClass(
                        confirmedClass.getId()).size()
                >= confirmedClass.getCapacity()
                || hasOverlappingBooking(
                        confirmedClass, latestBooking.getId())) {

            JOptionPane.showMessageDialog(this,
                    "The replacement class is no longer available.");

            refreshTables();
            return;
        }

        // Replace the existing booking while keeping its booking ID.
        Booking updatedBooking = new Booking(
                booking.getId(),
                booking.getStudentId(),
                confirmedClass.getId(),
                booking.getBookedAt(),
                "BOOKED",
                "UNPAID"
        );

        try {
            boolean updated = bookingRepository.update(updatedBooking);

            if (!updated) {
                JOptionPane.showMessageDialog(this,
                        "The booking could not be updated.");
                return;
            }

            refreshTables();

            JOptionPane.showMessageDialog(this,
                    "Booking rescheduled successfully!\n"
                    + "Booking ID: " + bookingId + "\n"
                    + "New Class: " + latestClass.getId());

        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Unable to reschedule booking: " + ex.getMessage(),
                    "Rescheduling Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private boolean hasOverlappingBooking(
            ClassSession newClass, String excludeBookingId) {

        LocalTime newStart;
        LocalTime newEnd;

        try {
            newStart = LocalTime.parse(newClass.getStartTime());
            newEnd = LocalTime.parse(newClass.getEndTime());

            if (!newStart.isBefore(newEnd)) {
                return true;
            }

        } catch (RuntimeException ex) {
            return true;
        }

        for (Booking existing : bookingRepository.findAll()) {

            // Only consider active bookings of this student.
            if (!existing.getStudentId().equals(student.getId())
                    || !existing.isBooked()) {
                continue;
            }

            // Ignore the booking currently being rescheduled.
            if (excludeBookingId != null
                    && existing.getId().equals(excludeBookingId)) {
                continue;
            }

            ClassSession existingClass =
                    classRepository.findById(existing.getClassId());

            if (existingClass == null || existingClass.isCancelled()) {
                continue;
            }

            // Classes on different dates cannot overlap.
            if (!existingClass.getDate().equals(newClass.getDate())) {
                continue;
            }

            try {
                LocalTime existingStart =
                        LocalTime.parse(existingClass.getStartTime());

                LocalTime existingEnd =
                        LocalTime.parse(existingClass.getEndTime());

                // Standard time-interval overlap check.
                if (newStart.isBefore(existingEnd)
                        && existingStart.isBefore(newEnd)) {
                    return true;
                }

            } catch (RuntimeException ex) {
                // Reject if an existing same-day time cannot be checked.
                return true;
            }
        }

        return false;
    }
}