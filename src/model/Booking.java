package model;

/**
 * One line of bookings.txt:
 * bookingId|studentId|classId|bookedAt|status|paymentStatus
 * ENCAPSULATION: fields are private and exposed only through getters.
 */
public class Booking implements Storable {
    private final String bookingId;
    private final String studentId;
    private final String classId;
    private final String bookedAt;
    private final String status;        // BOOKED, CANCELLED
    private final String paymentStatus; // UNPAID, PAID

    public Booking(String bookingId, String studentId, String classId, String bookedAt,
                   String status, String paymentStatus) {
        this.bookingId = bookingId;
        this.studentId = studentId;
        this.classId = classId;
        this.bookedAt = bookedAt;
        this.status = status;
        this.paymentStatus = paymentStatus;
    }

    public static Booking fromFields(String[] f) {
        return new Booking(f[0], f[1], f[2], f[3], f[4], f[5]);
    }

    @Override public String getId() { return bookingId; }
    public String getStudentId() { return studentId; }
    public String getClassId() { return classId; }
    public String getBookedAt() { return bookedAt; }
    public String getStatus() { return status; }
    public String getPaymentStatus() { return paymentStatus; }

    public boolean isBooked() { return "BOOKED".equals(status); }

    /** True when the student has paid for this booking. */
    public boolean isPaid() { return "PAID".equals(paymentStatus); }

    /**
     * Business rule: a student may attend (and be marked present, absent or late)
     * only when the booking is still BOOKED and has been PAID.
     */
    public boolean canAttend() { return isBooked() && isPaid(); }

    @Override
    public String toLine() {
        return String.join("|", bookingId, studentId, classId, bookedAt, status, paymentStatus);
    }
}