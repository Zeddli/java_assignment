
package service;

import java.util.List;

import model.Role;
import model.User;
import util.ValidationException;

/**
 * MG1 - Administrator account management.
 *
 * Reuses the common CRUD and validation logic
 * provided by AccountService.
 *
 * Only ADMIN accounts can be managed here.
 */
public class AdministratorService extends AccountService {

    /**
     * Restrict management operations to Administrators.
     */
    @Override
    public List<Role> manageableRoles() {
        return List.of(Role.ADMIN);
    }

    /**
     * Administrator IDs use the ADM prefix.
     * Example: ADM001, ADM002, ADM003.
     */
    @Override
    protected String idPrefix(Role role) {

        if (role != Role.ADMIN) {
            throw new IllegalArgumentException(
                    "Only Administrator accounts are allowed.");
        }

        return "ADM";
    }

    /**
     * Administrators do not have a role-specific
     * extra field in users.txt.
     */
    @Override
    protected void validateExtra(AccountForm form)
            throws ValidationException {

        if (!form.getExtra().isEmpty()) {
            throw new ValidationException(
                    AccountForm.EXTRA,
                    "Administrator extra field must be empty.");
        }
    }

    /**
     * Check whether historical records prevent deletion.
     *
     * Under the current shared data contract,
     * no other records reference Administrator IDs.
     */
    @Override
    public String findHistory(User user) {
        return null;
    }
}
