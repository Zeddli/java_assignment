package gui;

import model.Booking;
import model.ClassSession;
import model.Payment;
import model.User;
import repository.BookingRepository;
import repository.ClassRepository;
import repository.PaymentRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class ManagePaymentPanel extends JPanel {

    private static final Color BLUE_DARK = new Color(55, 68, 88);
    private static final Color BLUE_PALE = new Color(237, 240, 245);
    private static final Color TEXT = new Color(40, 51, 69);
    private static final Color MUTED = new Color(101, 114, 132);
    private static final Color LINE = new Color(218, 227, 239);
    private static final Font BODY_FONT = new Font("SansSerif", Font.PLAIN, 13);

    private final User student;

    private final BookingRepository bookingRepository =
            new BookingRepository();
    private final ClassRepository classRepository =
            new ClassRepository();
    private final PaymentRepository paymentRepository =
            new PaymentRepository();

    private final JTextField searchField =
            new HintTextField("e.g. PAY001 or Karate", 18);
    private final JTextField fromDateField =
            new HintTextField("yyyy-MM-dd", 11);
    private final JTextField toDateField =
            new HintTextField("yyyy-MM-dd", 11);

    private final DefaultTableModel paymentModel =
            new DefaultTableModel(
                    new String[]{
                        "Payment ID", "Class", "Paid On",
                        "Method", "Amount (RM)"
                    }, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    private final JTable paymentTable = new JTable(paymentModel);

    private final JComboBox<String> unpaidBookingBox = new JComboBox<>();
    private final JTextField amountField = new JTextField();
    private final JComboBox<String> paymentMethodBox =
            new JComboBox<>(new String[]{
                "Card", "Cash", "Online banking"
            });

    private final JButton payButton = new JButton("Pay Now");
    private final JLabel totalPaidValue = summaryValueLabel();
    private final JLabel outstandingValue = summaryValueLabel();
    private final JLabel paymentsMadeValue = summaryValueLabel();
    private final JLabel unpaidBookingsValue = summaryValueLabel();
    private final JLabel totalBookingsValue = summaryValueLabel();
    private final JLabel paymentCompletionValue = summaryValueLabel();

    public ManagePaymentPanel(User student) {
        this.student = student;

        setLayout(new BorderLayout(12, 12));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        buildInterface();
        styleTable(paymentTable);
        refreshData();

        // Reload payments when returning to the screen after a booking or payment change.
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                refreshData();
            }
        });
    }

    private void buildInterface() {
        JPanel topPanel = new JPanel(new BorderLayout(0, 14));
        topPanel.setOpaque(false);

        JPanel headingPanel = new JPanel(new GridLayout(2, 1, 0, 3));
        headingPanel.setOpaque(false);

        JLabel heading = new JLabel("Manage Payment");
        heading.setFont(new Font("SansSerif", Font.BOLD, 21));
        heading.setForeground(TEXT);

        JLabel subheading = new JLabel("Pay for your classes and review payment history");
        subheading.setFont(BODY_FONT);
        subheading.setForeground(MUTED);

        headingPanel.add(heading);
        headingPanel.add(subheading);
        topPanel.add(headingPanel, BorderLayout.NORTH);

        styleField(searchField);
        styleField(fromDateField);
        styleField(toDateField);

        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(0, 0, 5, 10);

        gc.gridx = 0;
        filterPanel.add(fieldLabel("Search payment or class"), gc);
        gc.gridx = 1;
        filterPanel.add(fieldLabel("From date"), gc);
        gc.gridx = 2;
        filterPanel.add(fieldLabel("To date"), gc);

        gc.gridy = 1;
        gc.insets = new Insets(0, 0, 0, 10);
        gc.gridx = 0;
        gc.weightx = 1.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        filterPanel.add(searchField, gc);

        gc.gridx = 1;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        filterPanel.add(fromDateField, gc);
        gc.gridx = 2;
        filterPanel.add(toDateField, gc);

        JButton filterButton = new JButton("Filter");
        JButton resetButton = new JButton("Reset");
        JPanel filterActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filterActions.setOpaque(false);
        filterActions.add(filterButton);
        filterActions.add(resetButton);

        gc.gridx = 3;
        gc.insets = new Insets(0, 0, 0, 0);
        filterPanel.add(filterActions, gc);

        topPanel.add(filterPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(16, 0));
        contentPanel.setOpaque(false);

        JPanel historyPanel = new JPanel(new BorderLayout(0, 8));
        historyPanel.setOpaque(false);
        historyPanel.add(sectionHeading("My Payment History"), BorderLayout.NORTH);

        paymentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        paymentTable.setFillsViewportHeight(true);
        paymentTable.getTableHeader().setReorderingAllowed(false);
        paymentTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        historyPanel.add(tableScroll(paymentTable), BorderLayout.CENTER);

        JButton receiptButton = new JButton("View Receipt");
        receiptButton.setEnabled(false);

        JPanel receiptActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        receiptActions.setOpaque(false);
        receiptActions.add(receiptButton);
        historyPanel.add(receiptActions, BorderLayout.SOUTH);

        paymentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                receiptButton.setEnabled(paymentTable.getSelectedRow() != -1);
            }
        });
        receiptButton.addActionListener(e -> viewSelectedReceipt());
        contentPanel.add(historyPanel, BorderLayout.CENTER);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        GridBagConstraints fg = new GridBagConstraints();
        fg.gridx = 0;
        fg.weightx = 1.0;
        fg.fill = GridBagConstraints.HORIZONTAL;
        fg.anchor = GridBagConstraints.NORTHWEST;
        fg.insets = new Insets(0, 0, 7, 0);

        fg.gridy = 0;
        formPanel.add(sectionHeading("Pay for a Booking"), fg);
        fg.gridy++;
        fg.insets = new Insets(7, 0, 5, 0);
        formPanel.add(fieldLabel("Unpaid Booking"), fg);

        unpaidBookingBox.setFont(BODY_FONT);
        unpaidBookingBox.setBackground(Color.WHITE);
        unpaidBookingBox.setPreferredSize(new Dimension(245, 32));
        unpaidBookingBox.setToolTipText("Select a booking ID, e.g. BKG001");
        fg.gridy++;
        fg.insets = new Insets(0, 0, 6, 0);
        formPanel.add(unpaidBookingBox, fg);

        fg.gridy++;
        fg.insets = new Insets(9, 0, 5, 0);
        formPanel.add(fieldLabel("Amount (RM)"), fg);
        amountField.setEditable(false);
        styleField(amountField);
        fg.gridy++;
        fg.insets = new Insets(0, 0, 6, 0);
        formPanel.add(amountField, fg);

        fg.gridy++;
        fg.insets = new Insets(9, 0, 5, 0);
        formPanel.add(fieldLabel("Payment Method"), fg);
        paymentMethodBox.setFont(BODY_FONT);
        paymentMethodBox.setBackground(Color.WHITE);
        paymentMethodBox.setPreferredSize(new Dimension(245, 32));
        fg.gridy++;
        fg.insets = new Insets(0, 0, 12, 0);
        formPanel.add(paymentMethodBox, fg);

        JButton clearButton = new JButton("Clear");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttonPanel.setOpaque(false);
        payButton.setEnabled(false);
        payButton.addActionListener(e -> paySelectedBooking());
        buttonPanel.add(payButton);
        buttonPanel.add(clearButton);
        fg.gridy++;
        fg.insets = new Insets(0, 0, 0, 0);
        formPanel.add(buttonPanel, fg);

        JPanel formColumn = new JPanel(new BorderLayout());
        formColumn.setOpaque(false);
        formColumn.setPreferredSize(new Dimension(285, 0));
        JPanel rightPanels = new JPanel();
        rightPanels.setOpaque(false);
        rightPanels.setLayout(new BoxLayout(rightPanels, BoxLayout.Y_AXIS));
        formPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanels.add(formPanel);
        rightPanels.add(Box.createVerticalStrut(14));

        JPanel summaryPanel = buildPaymentSummary();
        summaryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanels.add(summaryPanel);
        formColumn.add(rightPanels, BorderLayout.NORTH);
        contentPanel.add(formColumn, BorderLayout.EAST);
        add(contentPanel, BorderLayout.CENTER);

        filterButton.addActionListener(e -> refreshPaymentHistory());
        resetButton.addActionListener(e -> {
            searchField.setText("");
            fromDateField.setText("");
            toDateField.setText("");
            refreshPaymentHistory();
        });

        unpaidBookingBox.addActionListener(e -> updateSelectedAmount());
        clearButton.addActionListener(e -> {
            unpaidBookingBox.setSelectedIndex(
                    unpaidBookingBox.getItemCount() > 0 ? 0 : -1);
            paymentMethodBox.setSelectedIndex(0);
            updateSelectedAmount();
        });
    }

    private JPanel buildPaymentSummary() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        panel.add(sectionHeading("Payment Summary"), BorderLayout.NORTH);

        JPanel values = new JPanel(new GridLayout(3, 2, 9, 9));
        values.setOpaque(false);
        values.add(summaryCard("Total Paid", totalPaidValue));
        values.add(summaryCard("Outstanding", outstandingValue));
        values.add(summaryCard("Payments Made", paymentsMadeValue));
        values.add(summaryCard("Unpaid Bookings", unpaidBookingsValue));
        values.add(summaryCard("Total Bookings", totalBookingsValue));
        values.add(summaryCard("Payment Completion", paymentCompletionValue));
        panel.add(values, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel summaryCard(String title, JLabel value) {
        JPanel card = new JPanel(new BorderLayout(0, 7));
        card.setBackground(new Color(248, 250, 254));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(12, 9, 12, 9)));
        JLabel name = fieldLabel(title);
        card.add(name, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private static JLabel summaryValueLabel() {
        JLabel value = new JLabel("—");
        value.setFont(new Font("SansSerif", Font.BOLD, 16));
        value.setForeground(BLUE_DARK);
        return value;
    }

    private void refreshPaymentSummary() {
        double totalPaid = 0;
        int paymentsMade = 0;
        for (Payment payment : paymentRepository.findByStudent(student.getId())) {
            if ("PAID".equals(payment.getStatus())) {
                totalPaid += payment.getAmount();
                paymentsMade++;
            }
        }

        double outstanding = 0;
        int unpaidBookings = 0;
        int totalBookings = 0;
        int paidBookings = 0;

        for (Booking booking : bookingRepository.findAll()) {
            if (!booking.getStudentId().equals(student.getId())
                    || !booking.isBooked()) {
                continue;
            }

            ClassSession session = classRepository.findById(booking.getClassId());
            if (session == null || session.isCancelled()) {
                continue;
            }

            totalBookings++;
            if ("PAID".equals(booking.getPaymentStatus())) {
                paidBookings++;
            } else if ("UNPAID".equals(booking.getPaymentStatus())) {
                outstanding += session.getFee();
                unpaidBookings++;
            }
        }

        double paymentCompletion = totalBookings == 0
                ? 0.0 : (100.0 * paidBookings / totalBookings);

        // Summary values include all records, regardless of payment-history filters.
        totalPaidValue.setText(String.format("RM %.2f", totalPaid));
        outstandingValue.setText(String.format("RM %.2f", outstanding));
        paymentsMadeValue.setText(Integer.toString(paymentsMade));
        unpaidBookingsValue.setText(Integer.toString(unpaidBookings));
        totalBookingsValue.setText(Integer.toString(totalBookings));
        paymentCompletionValue.setText(String.format("%.0f%%", paymentCompletion));
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

    // Draw the hint without storing it as input text.
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

    private void refreshData() {
        refreshPaymentHistory();
        refreshUnpaidBookings();
        refreshPaymentSummary();
    }

    private void refreshPaymentHistory() {

        LocalDate fromDate = null;
        LocalDate toDate = null;

        try {
            if (!fromDateField.getText().trim().isEmpty()) {
                fromDate = LocalDate.parse(
                        fromDateField.getText().trim());
            }

            if (!toDateField.getText().trim().isEmpty()) {
                toDate = LocalDate.parse(
                        toDateField.getText().trim());
            }

        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                    "Please enter dates in yyyy-MM-dd format.");
            return;
        }

        if (fromDate != null && toDate != null
                && fromDate.isAfter(toDate)) {
            JOptionPane.showMessageDialog(this,
                    "From date cannot be after To date.");
            return;
        }

        paymentModel.setRowCount(0);

        String query = searchField.getText()
                .trim().toLowerCase();

        for (Payment payment :
                paymentRepository.findByStudent(student.getId())) {

            Booking booking = bookingRepository.findById(
                    payment.getBookingId());

            ClassSession session = booking == null
                    ? null
                    : classRepository.findById(booking.getClassId());

            String classLabel = session == null
                    ? payment.getBookingId()
                    : session.getId() + " - "
            + session.getDiscipline();

            String searchableText =
                    (payment.getId() + " " + classLabel)
                    .toLowerCase();

            if (!searchableText.contains(query)) {
                continue;
            }

            LocalDate paidDate;

            try {
                paidDate = LocalDate.parse(payment.getPaidDate());
            } catch (DateTimeParseException ex) {
                continue;
            }

            if (fromDate != null && paidDate.isBefore(fromDate)) {
                continue;
            }

            if (toDate != null && paidDate.isAfter(toDate)) {
                continue;
            }

            paymentModel.addRow(new Object[]{
                payment.getId(),
                classLabel,
                payment.getPaidDate(),
                payment.getMethod(),
                String.format("%.2f", payment.getAmount())
            });
        }
    }

    private void refreshUnpaidBookings() {

        unpaidBookingBox.removeAllItems();

        List<String> bookingIds = new ArrayList<>();

        for (Booking booking : bookingRepository.findAll()) {

            if (!booking.getStudentId().equals(student.getId())) {
                continue;
            }

            if (!booking.isBooked()
                    || !"UNPAID".equals(booking.getPaymentStatus())) {
                continue;
            }

            ClassSession session = classRepository.findById(
                    booking.getClassId());

            if (session == null || session.isCancelled()) {
                continue;
            }

            bookingIds.add(booking.getId());
        }

        for (String bookingId : bookingIds) {
            unpaidBookingBox.addItem(bookingId);
        }

        updateSelectedAmount();
    }

    private void updateSelectedAmount() {

        payButton.setEnabled(false);

        Object selected = unpaidBookingBox.getSelectedItem();

        if (selected == null) {
            amountField.setText("");
            return;
        }

        Booking booking = bookingRepository.findById(
                selected.toString());

        if (booking == null
                || !booking.getStudentId().equals(student.getId())
                || !booking.isBooked()
                || !"UNPAID".equals(booking.getPaymentStatus())) {

            amountField.setText("");
            return;
        }

        ClassSession session = classRepository.findById(
                booking.getClassId());

        if (session == null || session.isCancelled()) {
            amountField.setText("");
            return;
        }

        amountField.setText(
                String.format("%.2f", session.getFee()));

        payButton.setEnabled(true);
    }

    private void paySelectedBooking() {

        Object selected = unpaidBookingBox.getSelectedItem();

        if (selected == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select an unpaid booking.");
            return;
        }

        String bookingId = selected.toString();

        // Retrieve the latest booking information.
        Booking booking = bookingRepository.findById(bookingId);

        if (booking == null
                || !booking.getStudentId().equals(student.getId())
                || !booking.isBooked()
                || !"UNPAID".equals(booking.getPaymentStatus())) {

            JOptionPane.showMessageDialog(this,
                    "This booking is no longer eligible for payment.");

            refreshData();
            return;
        }

        ClassSession session = classRepository.findById(
                booking.getClassId());

        if (session == null || session.isCancelled()) {
            JOptionPane.showMessageDialog(this,
                    "The associated class is unavailable.");

            refreshData();
            return;
        }

        // Prevent duplicate payments.
        for (Payment existing : paymentRepository.findAll()) {

            if (existing.getBookingId().equals(bookingId)
                    && "PAID".equals(existing.getStatus())) {

                JOptionPane.showMessageDialog(this,
                        "A payment already exists for this booking.");

                refreshData();
                return;
            }
        }

        String paymentMethod =
                (String) paymentMethodBox.getSelectedItem();

        if (paymentMethod == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a payment method.");
            return;
        }

        double amount = session.getFee();

        if (!Double.isFinite(amount) || amount < 0) {
            JOptionPane.showMessageDialog(this,
                    "The class fee is invalid.");
            return;
        }

        int confirmation = JOptionPane.showConfirmDialog(
                this,
                "Confirm payment for booking " + bookingId + "?\n\n"
                + "Class: " + session.getDiscipline() + "\n"
                + "Amount: RM " + String.format("%.2f", amount) + "\n"
                + "Method: " + paymentMethod,
                "Confirm Payment",
                JOptionPane.YES_NO_OPTION
        );

        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }

        // Prepare the updated booking and payment record.
        String paymentId = paymentRepository.nextId("PAY");

        Payment payment = new Payment(
                paymentId,
                bookingId,
                student.getId(),
                amount,
                paymentMethod,
                java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter
                        .ofPattern("yyyy-MM-dd HH:mm")),
                "PAID"
        );

        Booking paidBooking = new Booking(
                booking.getId(),
                booking.getStudentId(),
                booking.getClassId(),
                booking.getBookedAt(),
                booking.getStatus(),
                "PAID"
        );

        // Save the payment, then update its booking.
        String errorMessage = null;

        try {
            paymentRepository.add(payment);

            boolean updated = bookingRepository.update(paidBooking);

            if (!updated) {
                errorMessage = "The booking could not be updated.";
            }

        } catch (RuntimeException ex) {
            errorMessage = "A file operation failed: " + ex.getMessage();
        }

        // Verify the actual records after the save attempt.
        try {
            Booking savedBooking = bookingRepository.findById(bookingId);
            Payment savedPayment = paymentRepository.findById(paymentId);

            boolean bookingPaid = savedBooking != null
                    && "PAID".equals(savedBooking.getPaymentStatus());

            boolean paymentSaved = savedPayment != null
                    && savedPayment.getBookingId().equals(bookingId)
                    && savedPayment.getStudentId().equals(student.getId())
                    && "PAID".equals(savedPayment.getStatus());

            if (bookingPaid && paymentSaved) {

                refreshData();

                JOptionPane.showMessageDialog(this,
                        "Payment recorded successfully!\n"
                        + "Payment ID: " + paymentId + "\n"
                        + "Amount: RM "
                        + String.format("%.2f", amount));

                return;
            }

            // Only remove the new payment if the booking is still unpaid.
            if (!bookingPaid && paymentSaved) {

                boolean rolledBack =
                        paymentRepository.delete(paymentId);

                if (!rolledBack
                        || paymentRepository.findById(paymentId) != null) {

                    errorMessage = "Payment and booking records are inconsistent."
                    + "\nPlease contact the administrator before retrying.";

                } else {
                    errorMessage = "The booking could not be updated."
                    + "\nThe payment record was rolled back.";
                }
            }

            // A paid booking with no matching payment needs manual review.
            if (bookingPaid && !paymentSaved) {
                errorMessage = "The booking is marked PAID, but its payment"
                + " record could not be verified."
                + "\nPlease contact the administrator.";
            }

            if (errorMessage == null) {
                errorMessage = "Payment could not be completed.";
            }

        } catch (RuntimeException ex) {
            errorMessage = "Unable to verify or recover payment records."
            + "\nPlease check both data files before retrying."
            + "\nDetails: " + ex.getMessage();
        }

        JOptionPane.showMessageDialog(
                this,
                errorMessage,
                "Payment Error",
                JOptionPane.ERROR_MESSAGE
        );

        refreshData();
    }

    private void viewSelectedReceipt() {

        int selectedRow = paymentTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a payment first.");
            return;
        }

        int modelRow = paymentTable.convertRowIndexToModel(selectedRow);

        String paymentId = paymentModel.getValueAt(
                modelRow, 0).toString();

        // Retrieve the latest payment record.
        Payment payment = paymentRepository.findById(paymentId);

        if (payment == null
                || !payment.getStudentId().equals(student.getId())) {

            JOptionPane.showMessageDialog(this,
                    "Payment not found or access denied.");

            return;
        }

        PaymentReceiptDialog receipt =
                new PaymentReceiptDialog(
                        SwingUtilities.getWindowAncestor(this),
                        student,
                        payment
                );

        receipt.setVisible(true);
    }
}