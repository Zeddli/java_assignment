package gui;

import model.Booking;
import model.ClassSession;
import model.Payment;
import model.User;
import repository.BookingRepository;
import repository.ClassRepository;

import javax.swing.*;
import java.awt.*;
import java.awt.print.PrinterException;

public class PaymentReceiptDialog extends JDialog {

    private final BookingRepository bookingRepository =
            new BookingRepository();

    private final ClassRepository classRepository =
            new ClassRepository();

    private final JTextArea receiptArea = new JTextArea();

    public PaymentReceiptDialog(Window owner, User student,
            Payment payment) {

        super(owner, "Payment Receipt",
                Dialog.ModalityType.APPLICATION_MODAL);

        setSize(510, 560);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        buildInterface(student, payment);
    }

    private void buildInterface(User student, Payment payment) {

        setLayout(new BorderLayout(12, 12));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(
                BorderFactory.createEmptyBorder(15, 20, 5, 20));

        JLabel heading = new JLabel("SDAMS - Payment Receipt");
        heading.setFont(new Font("SansSerif", Font.BOLD, 20));

        topPanel.add(heading, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        receiptArea.setEditable(false);
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        receiptArea.setMargin(new Insets(20, 20, 20, 20));

        add(new JScrollPane(receiptArea), BorderLayout.CENTER);

        JButton printButton = new JButton("Print Receipt");
        JButton closeButton = new JButton("Close");

        JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 10, 10));

        buttonPanel.add(printButton);
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // Display only the logged-in student's receipt.
        if (!payment.getStudentId().equals(student.getId())) {

            receiptArea.setText("Access denied.");
            printButton.setEnabled(false);

            return;
        }

        Booking booking = bookingRepository.findById(
                payment.getBookingId());

        ClassSession session = booking == null
                ? null
                : classRepository.findById(booking.getClassId());

        String className = session == null
                ? "Unavailable"
                : session.getDiscipline();

        String classId = session == null
                ? "Unavailable"
                : session.getId();

        StringBuilder receipt = new StringBuilder();

        receipt.append("SELF DEFENCE ACADEMY\n");
        receipt.append("PAYMENT RECEIPT\n");
        receipt.append("----------------------------------------\n\n");

        receipt.append("Payment ID   : ")
                .append(payment.getId()).append("\n");

        receipt.append("Booking ID   : ")
                .append(payment.getBookingId()).append("\n");

        receipt.append("Student      : ")
                .append(student.getFullName()).append("\n");

        receipt.append("Student ID   : ")
                .append(student.getId()).append("\n");

        receipt.append("Class ID     : ")
                .append(classId).append("\n");

        receipt.append("Discipline   : ")
                .append(className).append("\n");

        receipt.append("Payment Date : ")
                .append(payment.getPaidAt()).append("\n");

        receipt.append("Method       : ")
                .append(payment.getMethod()).append("\n");

        receipt.append("Status       : ")
                .append(payment.getStatus()).append("\n\n");

        receipt.append("----------------------------------------\n");

        receipt.append(String.format(
                "TOTAL PAID   : RM %.2f%n",
                payment.getAmount()));

        receipt.append("----------------------------------------\n\n");

        receipt.append("Thank you for your payment.\n");
        receipt.append("Self Defence Academy Management System");

        receiptArea.setText(receipt.toString());
        receiptArea.setCaretPosition(0);

        printButton.addActionListener(e -> printReceipt());

        closeButton.addActionListener(e -> dispose());
    }

    private void printReceipt() {

        try {
            boolean printed = receiptArea.print();

            if (printed) {
                JOptionPane.showMessageDialog(this,
                        "Receipt sent to the printer.");
            }

        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Unable to print receipt: " + ex.getMessage(),
                    "Printing Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}