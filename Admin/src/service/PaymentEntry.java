package service;

/** One payment joined with student name and class discipline (one row of AD3). */
public class PaymentEntry {
    private final String paymentId, studentId, studentName, classId, discipline, method, paidDate;
    private final double amount;

    public PaymentEntry(String paymentId, String studentId, String studentName, String classId,
                        String discipline, String method, String paidDate, double amount) {
        this.paymentId = paymentId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.classId = classId;
        this.discipline = discipline;
        this.method = method;
        this.paidDate = paidDate;
        this.amount = amount;
    }

    public String getPaymentId() { return paymentId; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getClassId() { return classId; }
    public String getDiscipline() { return discipline; }
    public String getMethod() { return method; }
    public String getPaidDate() { return paidDate; }
    public double getAmount() { return amount; }

    /** "STU001 · Nurul Huda" as on the wireframe. */
    public String getStudentLabel() { return studentId + " · " + studentName; }

    /** "CLS003 · Karate" as on the wireframe. */
    public String getClassLabel() { return classId + " · " + discipline; }
}
