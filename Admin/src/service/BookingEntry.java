package service;

/** One booking joined with its class details (used by the student history dialog). */
public class BookingEntry {
    private final String bookingId, classId, discipline, dateTime, status, paymentStatus;

    public BookingEntry(String bookingId, String classId, String discipline, String dateTime,
                        String status, String paymentStatus) {
        this.bookingId = bookingId;
        this.classId = classId;
        this.discipline = discipline;
        this.dateTime = dateTime;
        this.status = status;
        this.paymentStatus = paymentStatus;
    }

    public String getBookingId() { return bookingId; }
    public String getClassId() { return classId; }
    public String getDiscipline() { return discipline; }
    public String getDateTime() { return dateTime; }
    public String getStatus() { return status; }
    public String getPaymentStatus() { return paymentStatus; }
}
