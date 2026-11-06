package repository;

import model.AttendanceRecord;
 
public class AttendanceRepository extends BaseRepository<AttendanceRecord> {
    public AttendanceRepository() { super("attendance.txt"); }
 
    @Override
    protected AttendanceRecord fromFields(String[] f) { return AttendanceRecord.fromFields(f); }
 
    /** One booking has at most one attendance record. */
    public AttendanceRecord findByBooking(String bookingId) {
        for (AttendanceRecord r : findAll()) if (r.getBookingId().equals(bookingId)) return r;
        return null;
    }
}

