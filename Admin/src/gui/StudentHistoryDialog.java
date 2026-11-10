package gui;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import model.User;
import service.BookingEntry;
import service.PaymentEntry;
import service.PaymentService;
import service.StudentService;

/**
 * Extra feature for AD2: read-only history of one student (bookings, payments, attendance totals).
 * Lets the administrator see WHY a student cannot be deleted.
 */
public class StudentHistoryDialog extends JDialog {

    public StudentHistoryDialog(Window owner, User student, StudentService students) {
        super(owner, "Student history · " + student.getId() + " " + student.getFullName(),
              ModalityType.APPLICATION_MODAL);
        setSize(700, 420);
        setLocationRelativeTo(owner);

        List<BookingEntry> bookings = students.bookingsOf(student.getId());
        List<PaymentEntry> payments = new PaymentService().forStudent(student.getId());
        int[] att = students.attendanceTotals(student.getId());

        StatCard cBookings = new StatCard("Bookings");
        cBookings.setValue(String.valueOf(bookings.size()));
        StatCard cPaid = new StatCard("Total paid (RM)");
        cPaid.setValue(String.format("%,.2f", PaymentService.total(payments)));
        StatCard cAttended = new StatCard("Present + late");
        cAttended.setValue(String.valueOf(att[0] + att[2]));
        StatCard cAbsent = new StatCard("Absent");
        cAbsent.setValue(String.valueOf(att[1]));
        JPanel cards = new JPanel(new GridLayout(1, 4, 8, 0));
        cards.add(cBookings); cards.add(cPaid); cards.add(cAttended); cards.add(cAbsent);

        SortableTable tBookings = new SortableTable("Booking", "Class", "Date and time", "Booking status", "Payment");
        for (BookingEntry b : bookings) {
            tBookings.addRow(b.getBookingId(), b.getClassId() + " · " + b.getDiscipline(),
                    b.getDateTime(), b.getStatus(), b.getPaymentStatus());
        }
        SortableTable tPayments = new SortableTable("Payment", "Class", "Method", "Paid on", "Amount (RM)");
        for (PaymentEntry p : payments) {
            tPayments.addRow(p.getPaymentId(), p.getClassLabel(), p.getMethod(), p.getPaidDate(), p.getAmount());
        }
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Bookings (" + bookings.size() + ")", new JScrollPane(tBookings));
        tabs.addTab("Payments (" + payments.size() + ")", new JScrollPane(tPayments));

        JLabel info = new JLabel(student.getEmail() + "  ·  " + student.getPhone()
                + "  ·  born " + student.getExtra() + "  ·  " + student.getStatus());
        info.setForeground(Color.DARK_GRAY);

        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(close);

        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.add(info, BorderLayout.NORTH);
        north.add(cards, BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 14, 8, 14));
        root.add(north, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(close);
    }
}
