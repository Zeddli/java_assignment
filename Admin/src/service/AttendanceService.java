package service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.AttendanceRecord;
import model.ClassSession;
import repository.AttendanceRepository;
import repository.ClassRepository;
import repository.UserRepository;
 
/** Joins attendance.txt + classes.txt + users.txt so screens do not repeat this logic. */
public class AttendanceService {
    private final AttendanceRepository attendanceRepo = new AttendanceRepository();
    private final ClassRepository classRepo = new ClassRepository();
    private final UserRepository userRepo = new UserRepository();
 
    public List<AttendanceEntry> loadEntries() {
        Map<String, ClassSession> classes = new HashMap<>();
        for (ClassSession c : classRepo.findAll()) classes.put(c.getId(), c);
 
        List<AttendanceEntry> out = new ArrayList<>();
        for (AttendanceRecord r : attendanceRepo.findAll()) {
            ClassSession c = classes.get(r.getClassId());
            if (c == null) continue;
            out.add(new AttendanceEntry(r.getId(), c.getDate(), c.getId(), c.getDiscipline(),
                    c.getInstructorId(), userRepo.nameOf(c.getInstructorId()),
                    r.getStudentId(), userRepo.nameOf(r.getStudentId()), r.getStatus()));
        }
        return out;
    }
}

