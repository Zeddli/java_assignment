
package gui;

import model.Role;
import model.User;
import service.AdministratorService;

/**
 * MG1 - Manage Administrators GUI.
 *
 * Inherits shared account management UI
 * from AccountPanel.
 *
 * Supports:
 * - Add Administrator
 * - View Administrators
 * - Search Administrators
 * - Update Administrator
 * - Delete Administrator
 * - Activate/Deactivate Administrator
 */
public class ManageAdministratorsPanel extends AccountPanel {

    public ManageAdministratorsPanel() {

        super(
                "Manage Administrators",
                "Add, view, search, update and delete administrator accounts",
                "e.g. ADM001 or Aisha",
                "Administrator details",
                new AdministratorService(),
                "ID",
                "Name",
                "Username",
                "Email",
                "Status"
        );

        // Build the inherited account management interface.
        buildUi();
    }

    /**
     * Convert an Administrator record into a table row.
     */
    @Override
    protected Object[] rowFor(User user) {

        return new Object[] {
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus()
        };
    }

    /**
     * Preferred table column widths.
     */
    @Override
    protected int[] columnWidths() {

        return new int[] {
                70, 120, 100, 160, 75
        };
    }

    /**
     * All accounts managed here have the ADMIN role.
     */
    @Override
    protected Role formRole() {
        return Role.ADMIN;
    }

    /**
     * Administrators have no extra information field.
     */
    @Override
    protected String formExtra() {
        return "";
    }

    /**
     * No additional form fields are needed.
     */
    @Override
    protected void addExtraFields() {
        // No extra Administrator fields.
    }

    /**
     * No additional fields to populate
     * when an Administrator is selected.
     */
    @Override
    protected void fillExtra(User user) {
        // Common fields are populated by AccountPanel.
    }

    /**
     * No additional fields to clear.
     */
    @Override
    protected void clearExtra() {
        // Common fields are cleared by AccountPanel.
    }
}
