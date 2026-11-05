package model;

/** One line of attendance.txt. */
public class AttendanceRecord implements Storable {
    private final String attendanceId;
    private final String bookingId;
    private final String classId;
    private final String studentId;
    private String status;   // PRESENT, ABSENT, LATE
    private String markedBy; // attendance officer userId
    private String markedAt; // yyyy-MM-dd HH:mm
 
    public AttendanceRecord(String attendanceId, String bookingId, String classId, String studentId,
                            String status, String markedBy, String markedAt) {
        this.attendanceId = attendanceId;
        this.bookingId = bookingId;
        this.classId = classId;
        this.studentId = studentId;
        this.status = status;
        this.markedBy = markedBy;
        this.markedAt = markedAt;
    }
 
    public static AttendanceRecord fromFields(String[] f) {
        return new AttendanceRecord(f[0], f[1], f[2], f[3], f[4], f[5], f[6]);
    }
 
    @Override public String getId() { return attendanceId; }
    public String getBookingId() { return bookingId; }
    public String getClassId() { return classId; }
    public String getStudentId() { return studentId; }
    public String getStatus() { return status; }
    public String getMarkedBy() { return markedBy; }
    public String getMarkedAt() { return markedAt; }
 
    public void setStatus(String status) { this.status = status; }
    public void setMarkedBy(String markedBy) { this.markedBy = markedBy; }
    public void setMarkedAt(String markedAt) { this.markedAt = markedAt; }
 
    @Override
    public String toLine() {
        return String.join("|", attendanceId, bookingId, classId, studentId, status, markedBy, markedAt);
    }
}

