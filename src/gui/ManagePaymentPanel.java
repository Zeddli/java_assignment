package gui;

import model.Booking;
import model.ClassSession;
import model.Payment;
import model.User;
import repository.BookingRepository;
import repository.ClassRepository;
import repository.PaymentRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class ManagePaymentPanel extends JPanel {

    private final User student;

    private final BookingRepository bookingRepository =
            new BookingRepository();
    private final ClassRepository classRepository =
            new ClassRepository();
    private final PaymentRepository paymentRepository =
            new PaymentRepository();

    private final JTextField searchField = new JTextField(12);
    private final JTextField fromDateField = new JTextField(9);
    private final JTextField toDateField = new JTextField(9);

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

    private final JComboBox<String> unpaidBookingBox =
            new JComboBox<>();

    private final JTextField amountField = new JTextField();
    private final JComboBox<String> paymentMethodBox =
            new JComboBox<>(new String[]{
        "Card", "Cash", "Online banking"
    });

    private final JButton payButton = new JButton("Pay Now");

    public ManagePaymentPanel(User student) {
        this.student = student;

        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        buildInterface();
        refreshData();

        // Refresh payment records whenever this panel becomes visible.
        addComponentListener(new java.awt.event.ComponentAdapter(){
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                refreshData();
            }
        });
    }

    private void buildInterface() {

        JPanel topPanel = new JPanel(new BorderLayout(0, 10));
        topPanel.setOpaque(false);

        JLabel heading = new JLabel("Manage Payment");
        heading.setFont(new Font("SansSerif", Font.BOLD, 20));
        topPanel.add(heading, BorderLayout.NORTH);

        JPanel filterPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterPanel.setOpaque(false);

        JButton filterButton = new JButton("Filter");
        JButton resetButton = new JButton("Reset");

        filterPanel.add(new JLabel("Search:"));
        filterPanel.add(searchField);
        filterPanel.add(new JLabel("From:"));
        filterPanel.add(fromDateField);
        filterPanel.add(new JLabel("To:"));
        filterPanel.add(toDateField);
        filterPanel.add(filterButton);
        filterPanel.add(resetButton);

        topPanel.add(filterPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(12, 0));
        contentPanel.setOpaque(false);

        // Payment history
        JPanel historyPanel = new JPanel(new BorderLayout(0, 8));
        historyPanel.setOpaque(false);

        historyPanel.add(
                new JLabel("My Payment History"), BorderLayout.NORTH);

        paymentTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        historyPanel.add(
                new JScrollPane(paymentTable), BorderLayout.CENTER);

        // Extra feature: View Payment Receipt.
        JButton receiptButton = new JButton("View Receipt");
        receiptButton.setEnabled(false);

        JPanel receiptActions = new JPanel(
                new FlowLayout(FlowLayout.LEFT));

        receiptActions.setOpaque(false);
        receiptActions.add(receiptButton);

        historyPanel.add(receiptActions, BorderLayout.SOUTH);

        // Enable the button when a payment is selected.
        paymentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                receiptButton.setEnabled(
                        paymentTable.getSelectedRow() != -1);
            }
        });

        receiptButton.addActionListener(e -> viewSelectedReceipt());

        contentPanel.add(historyPanel, BorderLayout.CENTER);

        // Payment form
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(
                formPanel, BoxLayout.Y_AXIS));
        formPanel.setPreferredSize(new Dimension(225, 350));
        formPanel.setBorder(BorderFactory.createTitledBorder(
                "Pay for a Booking"));

        amountField.setEditable(false);

        formPanel.add(new JLabel("Unpaid Booking"));
        formPanel.add(unpaidBookingBox);

        formPanel.add(Box.createVerticalStrut(12));

        formPanel.add(new JLabel("Amount (RM)"));
        formPanel.add(amountField);

        formPanel.add(Box.createVerticalStrut(12));

        formPanel.add(new JLabel("Payment Method"));
        formPanel.add(paymentMethodBox);

        formPanel.add(Box.createVerticalStrut(15));

        JButton clearButton = new JButton("Clear");

        JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 5, 0));

        payButton.setEnabled(false);

        payButton.addActionListener(e -> paySelectedBooking());

        buttonPanel.add(payButton);
        buttonPanel.add(clearButton);
        formPanel.add(buttonPanel);

        contentPanel.add(formPanel, BorderLayout.EAST);
        add(contentPanel, BorderLayout.CENTER);

        // Search and filter actions
        filterButton.addActionListener(e ->
                refreshPaymentHistory());

        resetButton.addActionListener(e -> {
            searchField.setText("");
            fromDateField.setText("");
            toDateField.setText("");
            refreshPaymentHistory();
        });

        // Booking selection updates the fee.
        unpaidBookingBox.addActionListener(e ->
                updateSelectedAmount());

        clearButton.addActionListener(e -> {
            unpaidBookingBox.setSelectedIndex(
                    unpaidBookingBox.getItemCount() > 0 ? 0 : -1);
            paymentMethodBox.setSelectedIndex(0);
            updateSelectedAmount();
        });
    }

    private void refreshData() {
        refreshPaymentHistory();
        refreshUnpaidBookings();
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