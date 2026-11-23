package service;

import model.User;
import repository.UserRepository;
import util.ValidationException;

/**
 * "My Profile": the logged-in administrator views and edits THEIR OWN account.
 * Username, role, ID and status cannot be changed here (only a Manager manages admins, MG1).
 * Uses the same AccountRules as AD1/AD2, so the checks are identical everywhere.
 */
public class ProfileService {
    public static final String CURRENT = "current";
    public static final String NEW = "new";
    public static final String CONFIRM = "confirm";

    private final UserRepository userRepo = new UserRepository();

    public User load(String userId) throws ValidationException {
        User u = userRepo.findById(userId);
        if (u == null) throw new ValidationException("Your account (" + userId + ") was not found.");
        return u;
    }

    /** Updates name, email and phone. Everything else is copied from the stored account. */
    public User updateDetails(String userId, String fullName, String email, String phone)
            throws ValidationException {
        User me = load(userId);
        String name = fullName == null ? "" : fullName.trim();
        String mail = email == null ? "" : email.trim();
        AccountRules.fullName(AccountForm.FULL_NAME, name);
        AccountRules.email(AccountForm.EMAIL, mail);
        AccountRules.phone(AccountForm.PHONE, phone);
        User updated = rebuild(me, me.getPassword(), name, mail, AccountRules.normalisePhone(phone));
        userRepo.update(updated);
        return updated;
    }

    /**
     * Changes the password after checking the current one.
     * The new password must follow the usual rules, differ from the old one, and be typed twice.
     */
    public void changePassword(String userId, String current, String newPassword, String confirm)
            throws ValidationException {
        User me = load(userId);
        if (current == null || current.isEmpty()) {
            throw new ValidationException(CURRENT, "Enter your current password.");
        }
        if (!current.equals(me.getPassword())) {
            throw new ValidationException(CURRENT, "Current password is incorrect.");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            throw new ValidationException(NEW, "Enter a new password.");
        }
        AccountRules.password(NEW, newPassword);
        if (newPassword.equals(current)) {
            throw new ValidationException(NEW, "New password must be different from the current one.");
        }
        if (!newPassword.equals(confirm)) {
            throw new ValidationException(CONFIRM, "The two new passwords do not match.");
        }
        userRepo.update(rebuild(me, newPassword, me.getFullName(), me.getEmail(), me.getPhone()));
    }

    /** FACTORY: a replacement object with the same ID (the shared User class has no setPassword). */
    private static User rebuild(User me, String password, String name, String email, String phone) {
        return UserRepository.create(me.getRole().name(), me.getId(), me.getUsername(), password,
                name, email, phone, me.getExtra(), me.getStatus());
    }
}
