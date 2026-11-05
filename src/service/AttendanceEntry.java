package service;

/** One attendance record joined with its class and people names (used by AO2 and AO3). */
public class AttendanceEntry {
    private final String recordId, date, classId, discipline;
    private final String instructorId, instructorName, studentId, studentName, status;
 
    public AttendanceEntry(String recordId, String date, String classId, String discipline,
                           String instructorId, String instructorName,
                           String studentId, String studentName, String status) {
        this.recordId = recordId;
        this.date = date;
        this.classId = classId;
        this.discipline = discipline;
        this.instructorId = instructorId;
        this.instructorName = instructorName;
        this.studentId = studentId;
        this.studentName = studentName;
        this.status = status;
    }
 
    public String getRecordId() { return recordId; }
    public String getDate() { return date; }
    public String getClassId() { return classId; }
    public String getDiscipline() { return discipline; }
    public String getInstructorId() { return instructorId; }
    public String getInstructorName() { return instructorName; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getStatus() { return status; }
}

