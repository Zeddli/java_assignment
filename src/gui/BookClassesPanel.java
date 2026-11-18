package gui;

import model.Booking;
import model.ClassSession;
import model.User;
import repository.BookingRepository;
import repository.ClassRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class BookClassesPanel extends JPanel {

    private static final Color BLUE = new Color(69, 84, 109);
    private static final Color BLUE_DARK = new Color(55, 68, 88);
    private static final Color BLUE_PALE = new Color(237, 240, 245);
    private static final Color TEXT = new Color(40, 51, 69);
    private static final Color MUTED = new Color(101, 114, 132);
    private static final Color LINE = new Color(218, 227, 239);
    private static final Font BODY_FONT = new Font("SansSerif", Font.PLAIN, 13);

    private final User student;
    private final ClassRepository classRepository =
            new ClassRepository();
    private final BookingRepository bookingRepository =
            new BookingRepository();

    private final JTextField searchField = new HintTextField("e.g. CLS003 or Karate", 17);
    private final JComboBox<String> disciplineFilter =
            new JComboBox<>(new String[]{
        "All", "Karate", "Judo", "Taekwondo", "MMA"
    });
    private final JTextField dateField = new HintTextField("yyyy-MM-dd", 11);

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

        setLayout(new BorderLayout(12, 12));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        buildInterface();

        styleTable(classesTable);
        styleTable(bookingsTable);

        classesTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        bookingsTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        classesTable.setFillsViewportHeight(true);
        bookingsTable.setFillsViewportHeight(true);

        classesTable.getTableHeader().setReorderingAllowed(false);
        bookingsTable.getTableHeader().setReorderingAllowed(false);

        classesTable.getColumnModel().getColumn(4)
                .setPreferredWidth(150);

        bookingsTable.getColumnModel().getColumn(0)
                .setPreferredWidth(110);


        refreshTables();

        // Reload bookings when returning from another Student screen.
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

        JPanel topPanel = new JPanel(new BorderLayout(0, 14));
        topPanel.setOpaque(false);

        JPanel headingPanel = new JPanel(new GridLayout(2, 1, 0, 3));
        headingPanel.setOpaque(false);
        JLabel heading = new JLabel("Book Classes");
        heading.setFont(new Font("SansSerif", Font.BOLD, 21));
        heading.setForeground(TEXT);
        JLabel subheading = new JLabel("Find training sessions and manage your bookings");
        subheading.setFont(BODY_FONT);
        subheading.setForeground(MUTED);
        headingPanel.add(heading);
        headingPanel.add(subheading);
        topPanel.add(headingPanel, BorderLayout.NORTH);

        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(0, 0, 5, 10);

        gc.gridx = 0;
        searchPanel.add(fieldLabel("Search class or instructor"), gc);
        gc.gridx = 1;
        searchPanel.add(fieldLabel("Discipline"), gc);
        gc.gridx = 2;
        searchPanel.add(fieldLabel("Date"), gc);

        styleField(searchField);
        styleField(dateField);
        disciplineFilter.setFont(BODY_FONT);
        disciplineFilter.setBackground(Color.WHITE);
        disciplineFilter.setPreferredSize(new Dimension(122, 32));

        gc.gridy = 1;
        gc.insets = new Insets(0, 0, 0, 10);
        gc.gridx = 0;
        gc.weightx = 1.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        searchPanel.add(searchField, gc);
        gc.gridx = 1;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        searchPanel.add(disciplineFilter, gc);
        gc.gridx = 2;
        searchPanel.add(dateField, gc);

        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show All");
        JPanel filterActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filterActions.setOpaque(false);
        filterActions.add(searchButton);
        filterActions.add(showAllButton);
        gc.gridx = 3;
        gc.insets = new Insets(0, 0, 0, 0);
        searchPanel.add(filterActions, gc);

        topPanel.add(searchPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        JPanel tablesPanel = new JPanel(new GridLayout(2, 1, 0, 17));
        tablesPanel.setOpaque(false);

        JPanel availablePanel = new JPanel(new BorderLayout(0, 8));
        availablePanel.setOpaque(false);
        availablePanel.add(sectionHeading("Available Classes"), BorderLayout.NORTH);
        availablePanel.add(tableScroll(classesTable), BorderLayout.CENTER);

        JButton bookButton = new JButton("Book Selected Class");
        bookButton.setEnabled(false);

        classesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                bookButton.setEnabled(classesTable.getSelectedRow() != -1);
            }
        });

        bookButton.addActionListener(e -> bookSelectedClass());

        JPanel bookActions = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 8, 5));
        bookActions.setOpaque(false);
        bookActions.add(bookButton);

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

        JPanel bookingsPanel = new JPanel(new BorderLayout(0, 8));
        bookingsPanel.setOpaque(false);
        bookingsPanel.add(sectionHeading("My Bookings"), BorderLayout.NORTH);
        bookingsPanel.add(tableScroll(bookingsTable), BorderLayout.CENTER);

        JButton rescheduleButton =
                new JButton("Reschedule Selected");
        JButton cancelButton = new JButton("Cancel Selected");

        rescheduleButton.setEnabled(false);
        cancelButton.setEnabled(false);

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

        rescheduleButton.addActionListener(e -> rescheduleSelectedBooking());
        cancelButton.addActionListener(e -> cancelSelectedBooking());

        JPanel bookingActions = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 8, 5));
        bookingActions.setOpaque(false);
        bookingActions.add(rescheduleButton);
        bookingActions.add(cancelButton);

        bookingsPanel.add(bookingActions, BorderLayout.SOUTH);
        tablesPanel.add(bookingsPanel);

        add(tablesPanel, BorderLayout.CENTER);

        searchButton.addActionListener(e -> refreshTables());

        showAllButton.addActionListener(e -> {
            searchField.setText("");
            dateField.setText("");
            disciplineFilter.setSelectedIndex(0);
            refreshTables();
        });
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

    private static void styleField(JTextField field) {
        field.setFont(BODY_FONT);
        field.setForeground(TEXT);
        field.setBackground(Color.WHITE);
        field.setPreferredSize(new Dimension(165, 32));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(4, 9, 4, 9)));
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
                    JTable t, Object value, boolean selected, boolean focused,
                    int row, int column) {
                super.getTableCellRendererComponent(
                        t, value, selected, focused, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 7));
                setBackground(selected ? BLUE_PALE
                        : (row % 2 == 0 ? Color.WHITE : new Color(248, 250, 254)));
                setForeground(TEXT);
                return this;
            }
        });
    }

    // Hints are drawn separately so empty search fields remain empty values.
    private static class HintTextField extends JTextField {
        private final String hint;

        HintTextField(String hint, int columns) {
            super(columns);
            this.hint = hint;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!getText().isEmpty()) {
                return;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setColor(new Color(145, 154, 167));
                g.setFont(getFont().deriveFont(Font.ITALIC));
                Insets insets = getInsets();
                FontMetrics fm = g.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g.drawString(hint, insets.left, y);
            } finally {
                g.dispose();
            }
        }
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

        ClassSession session = classRepository.findById(classId);

        if (session == null) {
            JOptionPane.showMessageDialog(this,
                    "This class could not be found.");
            refreshTables();
            return;
        }

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

        // Paid bookings are handled by the administrator to avoid unsupported refunds.
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

        // Keep the booking ID so existing references remain valid.
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

            // Ignore the original booking when checking the new time slot.
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

        // The booking may have changed while the confirmation dialog was open.
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

        // Capacity and scheduling may also have changed during confirmation.
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

        // Rescheduling retains the booking ID for linked records.
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

                if (newStart.isBefore(existingEnd)
                        && existingStart.isBefore(newEnd)) {
                    return true;
                }

            } catch (RuntimeException ex) {
                // Avoid accepting a booking when an existing time is invalid.
                return true;
            }
        }

        return false;
    }
}