package service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.AttendanceRecord;
import model.Booking;
import model.ClassSession;
import repository.AttendanceRepository;
import repository.BookingRepository;
import repository.ClassRepository;
import repository.UserRepository;

/**
 * Numbers for the Attendance Overview screen: which classes still need attendance,
 * how each recent class went, and which students have low attendance.
 * The screen only draws what this class returns (same idea as the Admin OverviewService).
 */
public class AttendanceOverviewService {
    /** A student below this attendance rate (%) is flagged as low attendance. */
    public static final double LOW_RATE = 75.0;
    /** A student needs at least this many recorded classes before being flagged. */
    public static final int MIN_CLASSES = 2;

    private final ClassRepository classRepo = new ClassRepository();
    private final BookingRepository bookingRepo = new BookingRepository();
    private final AttendanceRepository attendanceRepo = new AttendanceRepository();
    private final UserRepository userRepo = new UserRepository();
    private final AttendanceService attendanceService = new AttendanceService();

    /** One class that has started: how many paid students, and how many are already recorded. */
    public static class ClassStatus {
        private final ClassSession session;
        private final String instructorName;
        private int eligible, present, late, absent;

        ClassStatus(ClassSession session, String instructorName) {
            this.session = session;
            this.instructorName = instructorName;
        }
        public ClassSession getSession() { return session; }
        public String getInstructorName() { return instructorName; }
        /** Students who are BOOKED and PAID (the only ones who can attend). */
        public int getEligible() { return eligible; }
        public int getRecorded() { return present + late + absent; }
        public int getPending() { return eligible - getRecorded(); }
        public int getPresent() { return present; }
        public int getLate() { return late; }
        public int getAbsent() { return absent; }
    }

    /** One student's attendance totals. */
    public static class StudentRate {
        private final String studentId, name;
        private int classes, present, late, absent;

        StudentRate(String studentId, String name) { this.studentId = studentId; this.name = name; }
        public String getStudentId() { return studentId; }
        public String getName() { return name; }
        public int getClasses() { return classes; }
        public int getAbsent() { return absent; }
        public int getLate() { return late; }
        /** Present + late counts as attended (same rule as the Attendance Report). */
        public double getRate() { return classes == 0 ? 0 : (present + late) * 100.0 / classes; }
    }

    /** Every class that is not cancelled and has started, oldest first. */
    public List<ClassStatus> classStatuses() {
        Map<String, AttendanceRecord> byBooking = new HashMap<>();
        for (AttendanceRecord r : attendanceRepo.findAll()) byBooking.put(r.getBookingId(), r);

        List<ClassStatus> out = new ArrayList<>();
        for (ClassSession c : classRepo.findAll()) {
            if (c.isCancelled() || !c.hasStarted()) continue;
            ClassStatus st = new ClassStatus(c, userRepo.nameOf(c.getInstructorId()));
            for (Booking b : bookingRepo.findBookedByClass(c.getId())) {
                st.eligible++;
                AttendanceRecord r = byBooking.get(b.getId());
                if (r == null) continue;
                switch (r.getStatus()) {
                    case "PRESENT": st.present++; break;
                    case "LATE":    st.late++; break;
                    case "ABSENT":  st.absent++; break;
                    default: break;
                }
            }
            out.add(st);
        }
        out.sort(Comparator.comparing((ClassStatus s) -> s.getSession().getDate())
                .thenComparing(s -> s.getSession().getStartTime()));
        return out;
    }

    /** Classes that still have students without attendance, oldest first. */
    public List<ClassStatus> pendingClasses() {
        List<ClassStatus> out = new ArrayList<>();
        for (ClassStatus s : classStatuses()) if (s.getPending() > 0) out.add(s);
        return out;
    }

    /** The last {@code n} classes that have at least one record (for the chart), oldest first. */
    public List<ClassStatus> recentClasses(int n) {
        List<ClassStatus> withRecords = new ArrayList<>();
        for (ClassStatus s : classStatuses()) if (s.getRecorded() > 0) withRecords.add(s);
        int from = Math.max(0, withRecords.size() - n);
        return new ArrayList<>(withRecords.subList(from, withRecords.size()));
    }

    /** Totals for every student who has at least one record. */
    public List<StudentRate> studentRates() {
        Map<String, StudentRate> byStudent = new LinkedHashMap<>();
        for (AttendanceEntry e : attendanceService.loadEntries()) {
            StudentRate r = byStudent.computeIfAbsent(e.getStudentId(),
                    id -> new StudentRate(id, e.getStudentName()));
            r.classes++;
            switch (e.getStatus()) {
                case "PRESENT": r.present++; break;
                case "LATE":    r.late++; break;
                case "ABSENT":  r.absent++; break;
                default: break;
            }
        }
        return new ArrayList<>(byStudent.values());
    }

    /** Students below LOW_RATE with at least MIN_CLASSES records, lowest rate first. */
    public List<StudentRate> lowAttendance() {
        List<StudentRate> out = new ArrayList<>();
        for (StudentRate r : studentRates()) {
            if (r.getClasses() >= MIN_CLASSES && r.getRate() < LOW_RATE) out.add(r);
        }
        out.sort(Comparator.comparingDouble(StudentRate::getRate));
        return out;
    }

    /** Overall attendance rate (%) over all records; 0 when there are none. */
    public double overallRate() {
        int total = 0, attended = 0;
        for (StudentRate r : studentRates()) {
            total += r.classes;
            attended += r.present + r.late;
        }
        return total == 0 ? 0 : attended * 100.0 / total;
    }

    /** Total number of attendance records. */
    public int totalRecords() {
        int n = 0;
        for (StudentRate r : studentRates()) n += r.classes;
        return n;
    }
}
