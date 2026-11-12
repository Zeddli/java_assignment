package service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.AttendanceRecord;
import model.Booking;
import model.ClassSession;
import model.Role;
import model.User;
import repository.AttendanceRepository;
import repository.BookingRepository;
import repository.ClassRepository;
import repository.PaymentRepository;
import util.ValidationException;
import util.Validator;

/**
 * AD2 Manage Students. The "extra" column of users.txt holds the date of birth.
 * INHERITANCE + POLYMORPHISM: same AccountService steps, student rules.
 */
public class StudentService extends AccountService {
    public static final int MIN_AGE = 4;
    public static final int MAX_AGE = 100;

    private final BookingRepository bookingRepo = new BookingRepository();
    private final PaymentRepository paymentRepo = new PaymentRepository();
    private final AttendanceRepository attendanceRepo = new AttendanceRepository();
    private final ClassRepository classRepo = new ClassRepository();

    @Override
    public List<Role> manageableRoles() { return List.of(Role.STUDENT); }

    @Override
    protected String idPrefix(Role role) { return "STU"; }

    /** Date of birth must be a real yyyy-MM-dd date in the past, age 4 to 100. */
    @Override
    protected void validateExtra(AccountForm form) throws ValidationException {
        String dob = form.getExtra();
        if (Validator.isBlank(dob)) {
            throw new ValidationException(AccountForm.EXTRA, "Date of birth is required.");
        }
        if (!Validator.isDate(dob)) {
            throw new ValidationException(AccountForm.EXTRA, "Date of birth must be a valid yyyy-MM-dd date.");
        }
        LocalDate born = LocalDate.parse(dob);
        if (!born.isBefore(LocalDate.now())) {
            throw new ValidationException(AccountForm.EXTRA, "Date of birth must be in the past.");
        }
        int age = Period.between(born, LocalDate.now()).getYears();
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new ValidationException(AccountForm.EXTRA,
                    "Student age must be between " + MIN_AGE + " and " + MAX_AGE + ".");
        }
    }

    @Override
    public String findHistory(User user) {
        String id = user.getId();
        int bookings = 0;
        for (Booking b : bookingRepo.findAll()) if (b.getStudentId().equals(id)) bookings++;
        int attendance = 0;
        for (AttendanceRecord r : attendanceRepo.findAll()) if (r.getStudentId().equals(id)) attendance++;
        return describe(bookings, "booking",
                paymentRepo.findByStudent(id).size(), "payment",
                attendance, "attendance record",
                countInFile("ratings.txt", 2, id), "rating"); // studentId
    }

    /** Extra feature: every booking of one student, joined with its class (for the history dialog). */
    public List<BookingEntry> bookingsOf(String studentId) {
        Map<String, ClassSession> classes = new HashMap<>();
        for (ClassSession c : classRepo.findAll()) classes.put(c.getId(), c);
        List<BookingEntry> out = new ArrayList<>();
        for (Booking b : bookingRepo.findAll()) {
            if (!b.getStudentId().equals(studentId)) continue;
            ClassSession c = classes.get(b.getClassId());
            out.add(new BookingEntry(b.getId(), b.getClassId(),
                    c == null ? "?" : c.getDiscipline(),
                    c == null ? "" : c.getDate() + " " + c.getStartTime(),
                    b.getStatus(), b.getPaymentStatus()));
        }
        return out;
    }

    /** Extra feature: attendance totals for one student as {present, absent, late}. */
    public int[] attendanceTotals(String studentId) {
        int[] t = new int[3];
        for (AttendanceRecord r : attendanceRepo.findAll()) {
            if (!r.getStudentId().equals(studentId)) continue;
            switch (r.getStatus()) {
                case "PRESENT": t[0]++; break;
                case "ABSENT":  t[1]++; break;
                case "LATE":    t[2]++; break;
                default: break;
            }
        }
        return t;
    }
}
