package service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Booking;
import model.ClassSession;
import model.Payment;
import model.User;
import repository.BookingRepository;
import repository.ClassRepository;
import repository.PaymentRepository;
import repository.UserRepository;
import util.ValidationException;
import util.Validator;

/**
 * AD3 Payment Records (read-only). Joins payments.txt + bookings.txt + classes.txt + users.txt
 * so the screen only receives ready-made rows and never opens a file.
 */
public class PaymentService {
    public static final String[] METHODS = {"Card", "Cash", "Online banking"};

    private final PaymentRepository paymentRepo = new PaymentRepository();
    private final BookingRepository bookingRepo = new BookingRepository();
    private final ClassRepository classRepo = new ClassRepository();
    private final UserRepository userRepo = new UserRepository();

    /**
     * Filters combine with AND; blank filters are ignored. method null or "All" = any method.
     * @throws ValidationException when a date is not yyyy-MM-dd or from-date is after to-date
     */
    public List<PaymentEntry> filter(String student, String from, String to, String method)
            throws ValidationException {
        String q = student == null ? "" : student.trim().toLowerCase();
        LocalDate fromDate = parseOptional("from", from, "From date");
        LocalDate toDate = parseOptional("to", to, "To date");
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ValidationException("from", "From date cannot be after To date.");
        }
        boolean anyMethod = method == null || method.isEmpty() || "All".equals(method);

        List<PaymentEntry> out = new ArrayList<>();
        for (PaymentEntry e : loadAll()) {
            if (!q.isEmpty() && !e.getStudentId().toLowerCase().contains(q)
                    && !e.getStudentName().toLowerCase().contains(q)) continue;
            if (!anyMethod && !e.getMethod().equalsIgnoreCase(method)) continue;
            if (fromDate != null || toDate != null) {
                if (!Validator.isDate(e.getPaidDate())) continue;
                LocalDate d = LocalDate.parse(e.getPaidDate());
                if (fromDate != null && d.isBefore(fromDate)) continue;
                if (toDate != null && d.isAfter(toDate)) continue;
            }
            out.add(e);
        }
        return out;
    }

    /** Every payment of one student (student history dialog). */
    public List<PaymentEntry> forStudent(String studentId) {
        List<PaymentEntry> out = new ArrayList<>();
        for (PaymentEntry e : loadAll()) if (e.getStudentId().equals(studentId)) out.add(e);
        return out;
    }

    public static double total(List<PaymentEntry> rows) {
        double sum = 0;
        for (PaymentEntry e : rows) sum += e.getAmount();
        return sum;
    }

    private List<PaymentEntry> loadAll() {
        Map<String, Booking> bookings = new HashMap<>();
        for (Booking b : bookingRepo.findAll()) bookings.put(b.getId(), b);
        Map<String, ClassSession> classes = new HashMap<>();
        for (ClassSession c : classRepo.findAll()) classes.put(c.getId(), c);
        Map<String, String> names = new HashMap<>();
        for (User u : userRepo.findAll()) names.put(u.getId(), u.getFullName());

        List<PaymentEntry> out = new ArrayList<>();
        for (Payment p : paymentRepo.findAll()) {
            Booking b = bookings.get(p.getBookingId());
            ClassSession c = b == null ? null : classes.get(b.getClassId());
            out.add(new PaymentEntry(p.getId(), p.getStudentId(),
                    names.getOrDefault(p.getStudentId(), "(unknown)"),
                    b == null ? p.getBookingId() : b.getClassId(),
                    c == null ? "?" : c.getDiscipline(),
                    p.getMethod(), p.getPaidDate(), p.getAmount()));
        }
        return out;
    }

    private static LocalDate parseOptional(String field, String text, String label) throws ValidationException {
        if (Validator.isBlank(text)) return null;
        if (!Validator.isDate(text.trim())) {
            throw new ValidationException(field, label + " must be a valid yyyy-MM-dd date.");
        }
        return LocalDate.parse(text.trim());
    }
}
