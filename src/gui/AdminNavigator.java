package gui;

import java.time.LocalDate;
import model.Role;

/**
 * What the Overview dashboard can ask for when a card or chart bar is clicked.
 * ABSTRACTION: the Overview only knows this interface, not the other panels or the window,
 * so it can be built and tested on its own. AdminModules supplies the real implementation.
 */
public interface AdminNavigator {
    /** Opens Manage Staff filtered by role (null = all) and status (ACTIVE / INACTIVE / null = all). */
    void openStaff(Role role, String status);

    /** Opens Manage Students filtered by status (ACTIVE / INACTIVE / null = all). */
    void openStudents(String status);

    /** Opens Payment Records showing payments between two dates (inclusive). */
    void openPayments(LocalDate from, LocalDate to);
}
