package model;

/** One line of bookings.txt. */
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
 
    @Override
    public String toLine() {
        return String.join("|", bookingId, studentId, classId, bookedAt, status, paymentStatus);
    }
}
