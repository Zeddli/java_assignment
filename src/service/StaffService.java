package service;

import java.util.List;
import model.AttendanceRecord;
import model.ClassSession;
import model.Role;
import model.User;
import repository.AttendanceRepository;
import repository.ClassRepository;
import util.ValidationException;

/**
 * AD1 Manage Staff: Instructors, Facilities Officers and Attendance Officers.
 * Never Manager, Administrator or Student (those belong to MG1 and AD2).
 * INHERITANCE: reuses search/add/update/delete from AccountService.
 * POLYMORPHISM: overrides the hooks with staff rules.
 */
public class StaffService extends AccountService {
    /** Disciplines offered by the academy (same list as the AO2 filter). */
    public static final String[] DISCIPLINES = {"Karate", "Judo", "Taekwondo", "MMA"};

    private static final List<Role> ROLES = List.of(Role.INSTRUCTOR, Role.FACILITIES, Role.ATTENDANCE);

    private final ClassRepository classRepo = new ClassRepository();
    private final AttendanceRepository attendanceRepo = new AttendanceRepository();

    @Override
    public List<Role> manageableRoles() { return ROLES; }

    @Override
    protected String idPrefix(Role role) {
        switch (role) {
            case INSTRUCTOR: return "INS";
            case FACILITIES: return "FAC";
            case ATTENDANCE: return "ATT";
            default: throw new IllegalArgumentException("Not a staff role: " + role);
        }
    }

    /** Discipline is required for instructors and must be empty for the other staff roles. */
    @Override
    protected void validateExtra(AccountForm form) throws ValidationException {
        if (form.getRole() == Role.INSTRUCTOR) {
            if (!List.of(DISCIPLINES).contains(form.getExtra())) {
                throw new ValidationException(AccountForm.EXTRA, "Choose the instructor's discipline.");
            }
        } else if (!form.getExtra().isEmpty()) {
            throw new ValidationException(AccountForm.EXTRA, "Only instructors have a discipline.");
        }
    }

    @Override
    public String findHistory(User user) {
        String id = user.getId();
        switch (user.getRole()) {
            case INSTRUCTOR:
                int classes = 0;
                for (ClassSession c : classRepo.findAll()) if (c.getInstructorId().equals(id)) classes++;
                return describe(classes, "class",
                        countInFile("problems.txt", 1, id), "facility problem",   // reportedBy
                        countInFile("ratings.txt", 3, id), "rating");             // instructorId
            case ATTENDANCE:
                int marked = 0;
                for (AttendanceRecord r : attendanceRepo.findAll()) if (r.getMarkedBy().equals(id)) marked++;
                return describe(marked, "attendance record");
            default:
                return null; // problems.txt does not record which facilities officer updated it
        }
    }

    /** Warn before deactivating an instructor who still has upcoming classes students can book. */
    @Override
    public String deactivationWarning(User user) {
        if (user.getRole() != Role.INSTRUCTOR) return null;
        int upcoming = 0;
        for (ClassSession c : classRepo.findAll()) {
            if (c.getInstructorId().equals(user.getId()) && !c.isCancelled() && !c.hasStarted()) upcoming++;
        }
        return upcoming == 0 ? null
                : user.getFullName() + " still teaches " + upcoming + " upcoming class"
                  + (upcoming == 1 ? "" : "es") + ". Those classes will not be cancelled automatically.";
    }
}
