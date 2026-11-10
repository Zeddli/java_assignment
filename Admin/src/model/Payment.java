package model;

/**
 * One line of payments.txt:
 * paymentId|bookingId|studentId|amount|method|paidAt|status
 * ENCAPSULATION: fields are private and only exposed through getters.
 */
public class Payment implements Storable {
    private final String paymentId;
    private final String bookingId;
    private final String studentId;
    private final double amount;
    private final String method;  // Card, Cash, Online banking
    private final String paidAt;  // yyyy-MM-dd (a time part after it is ignored)
    private final String status;  // PAID

    public Payment(String paymentId, String bookingId, String studentId, double amount,
                   String method, String paidAt, String status) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.studentId = studentId;
        this.amount = amount;
        this.method = method;
        this.paidAt = paidAt;
        this.status = status;
    }

    public static Payment fromFields(String[] f) {
        return new Payment(f[0], f[1], f[2], Double.parseDouble(f[3]), f[4], f[5], f[6]);
    }

    @Override public String getId() { return paymentId; }
    public String getBookingId() { return bookingId; }
    public String getStudentId() { return studentId; }
    public double getAmount() { return amount; }
    public String getMethod() { return method; }
    public String getPaidAt() { return paidAt; }
    public String getStatus() { return status; }

    /** Date part only (yyyy-MM-dd), so "2026-09-02 10:15" and "2026-09-02" both work. */
    public String getPaidDate() {
        return paidAt.length() >= 10 ? paidAt.substring(0, 10) : paidAt;
    }

    @Override
    public String toLine() {
        return String.join("|", paymentId, bookingId, studentId, String.format("%.2f", amount),
                method, paidAt, status);
    }
}
