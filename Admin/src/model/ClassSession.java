package model;

import java.time.LocalDate;
import java.time.LocalTime;
 
/** One line of classes.txt. */
public class ClassSession implements Storable {
    private final String classId;
    private final String instructorId;
    private final String discipline;
    private final String date;
    private final String startTime;
    private final String endTime;
    private final String room;
    private final int capacity;
    private final double fee;
    private final String status; // OPEN, FULL, CANCELLED
 
    public ClassSession(String classId, String instructorId, String discipline, String date,
                        String startTime, String endTime, String room, int capacity, double fee, String status) {
        this.classId = classId;
        this.instructorId = instructorId;
        this.discipline = discipline;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.capacity = capacity;
        this.fee = fee;
        this.status = status;
    }
 
    public static ClassSession fromFields(String[] f) {
        return new ClassSession(f[0], f[1], f[2], f[3], f[4], f[5], f[6],
                Integer.parseInt(f[7]), Double.parseDouble(f[8]), f[9]);
    }
 
    @Override public String getId() { return classId; }
    public String getInstructorId() { return instructorId; }
    public String getDiscipline() { return discipline; }
    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getRoom() { return room; }
    public int getCapacity() { return capacity; }
    public double getFee() { return fee; }
    public String getStatus() { return status; }
    public boolean isCancelled() { return "CANCELLED".equals(status); }
 
    /** True once the class start time has passed (attendance may then be recorded). */
    public boolean hasStarted() {
        LocalDate d = LocalDate.parse(date);
        LocalDate today = LocalDate.now();
        if (d.isBefore(today)) return true;
        if (d.isAfter(today)) return false;
        return !LocalTime.parse(startTime).isAfter(LocalTime.now());
    }
 
    @Override
    public String toLine() {
        return String.join("|", classId, instructorId, discipline, date, startTime, endTime, room,
                String.valueOf(capacity), String.format("%.2f", fee), status);
    }
}

