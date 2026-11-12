package service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import model.Role;
import model.User;
import repository.UserRepository;
import util.TextFileHandler;
import util.ValidationException;
import util.Validator;

/**
 * Shared account logic for AD1 (staff) and AD2 (students): search, add, update, delete, activate.
 *
 * ABSTRACTION: this class says WHAT every account manager must do. The parts that differ
 * (which roles, which ID prefix, what "extra" means, what counts as history) are abstract methods.
 * POLYMORPHISM: the GUI holds an AccountService reference; at runtime it is a StaffService or a
 * StudentService, and each one answers the abstract methods its own way.
 * TEMPLATE METHOD: add() and update() fix the steps (validate, build, save) and call the hooks.
 */
public abstract class AccountService {
    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._]{3,20}$");
    private static final Pattern NAME = Pattern.compile("^[A-Za-z][A-Za-z .'/-]{1,49}$");
    private static final Pattern PHONE = Pattern.compile("^0\\d{8,10}$"); // after removing spaces and dashes

    protected final UserRepository userRepo = new UserRepository();

    // ---------- hooks each subclass must answer ----------

    /** Roles this screen may create or edit. */
    public abstract List<Role> manageableRoles();

    /** ID prefix such as INS, FAC, ATT or STU. */
    protected abstract String idPrefix(Role role);

    /** Checks the role-specific "extra" field (discipline or date of birth). */
    protected abstract void validateExtra(AccountForm form) throws ValidationException;

    /**
     * Describes the records that point at this user, e.g. "2 bookings, 1 payment".
     * Returns null when there are none, which means a hard delete is safe.
     */
    public abstract String findHistory(User user);

    /** Optional warning before deactivating (null = nothing to warn about). */
    public String deactivationWarning(User user) { return null; }

    // ---------- shared behaviour ----------

    /** Accounts this screen manages, filtered by text, role (null = all) and status (null = all). */
    public List<User> search(String text, Role role, String status) {
        String q = text == null ? "" : text.trim().toLowerCase();
        List<User> out = new ArrayList<>();
        for (User u : userRepo.findAll()) {
            if (!manageableRoles().contains(u.getRole())) continue;
            if (role != null && u.getRole() != role) continue;
            if (status != null && !status.equalsIgnoreCase(u.getStatus())) continue;
            if (!q.isEmpty() && !matches(u, q)) continue;
            out.add(u);
        }
        return out;
    }

    private static boolean matches(User u, String q) {
        return u.getId().toLowerCase().contains(q)
                || u.getFullName().toLowerCase().contains(q)
                || u.getUsername().toLowerCase().contains(q)
                || u.getEmail().toLowerCase().contains(q);
    }

    /** Finds one account, but only if this screen is allowed to manage it. */
    public User find(String userId) {
        User u = userRepo.findById(userId);
        return (u != null && manageableRoles().contains(u.getRole())) ? u : null;
    }

    public User add(AccountForm form) throws ValidationException {
        validate(form, null);
        if (form.getPassword().isEmpty()) {
            throw new ValidationException(AccountForm.PASSWORD, "Password is required.");
        }
        String id = userRepo.nextId(idPrefix(form.getRole()));
        User user = build(id, form, form.getPassword(), ACTIVE);
        userRepo.add(user);
        return user;
    }

    /** Role cannot change (the ID prefix depends on it). A blank password keeps the old one. */
    public User update(String userId, AccountForm form) throws ValidationException {
        User old = requireManaged(userId);
        if (form.getRole() != old.getRole()) {
            throw new ValidationException(AccountForm.ROLE,
                    "Role cannot be changed. Add a new account for the new role instead.");
        }
        validate(form, userId);
        String password = form.getPassword().isEmpty() ? old.getPassword() : form.getPassword();
        User updated = build(userId, form, password, old.getStatus());
        userRepo.update(updated);
        return updated;
    }

    /** Hard delete. Refused when other records still point at this account (use setStatus instead). */
    public void delete(String userId) throws ValidationException {
        User u = requireManaged(userId);
        String history = findHistory(u);
        if (history != null) {
            throw new ValidationException("Cannot delete " + userId + ": it has " + history
                    + ". Mark it INACTIVE instead.");
        }
        userRepo.delete(userId);
    }

    /** Activate or deactivate. INACTIVE accounts cannot log in, but their history is kept. */
    public User setStatus(String userId, String status) throws ValidationException {
        if (!ACTIVE.equals(status) && !INACTIVE.equals(status)) {
            throw new ValidationException("Status must be ACTIVE or INACTIVE.");
        }
        User old = requireManaged(userId);
        old.setStatus(status);
        userRepo.update(old);
        return old;
    }

    // ---------- helpers ----------

    private User requireManaged(String userId) throws ValidationException {
        User u = find(userId);
        if (u == null) throw new ValidationException("Account " + userId + " was not found.");
        return u;
    }

    /** FACTORY: UserRepository.create builds the object from the role, so this class never calls new User(...). */
    private User build(String id, AccountForm f, String password, String status) {
        return UserRepository.create(f.getRole().name(), id, f.getUsername(), password,
                f.getFullName(), f.getEmail(), normalisePhone(f.getPhone()), f.getExtra(), status);
    }

    /** Common checks for every account, then the subclass-specific check (validateExtra). */
    private void validate(AccountForm f, String ownId) throws ValidationException {
        if (f.getRole() == null || !manageableRoles().contains(f.getRole())) {
            throw new ValidationException(AccountForm.ROLE, "Choose a valid role.");
        }
        noPipe(AccountForm.FULL_NAME, f.getFullName());
        noPipe(AccountForm.USERNAME, f.getUsername());
        noPipe(AccountForm.PASSWORD, f.getPassword());
        noPipe(AccountForm.EMAIL, f.getEmail());
        noPipe(AccountForm.PHONE, f.getPhone());
        noPipe(AccountForm.EXTRA, f.getExtra());

        if (Validator.isBlank(f.getFullName())) {
            throw new ValidationException(AccountForm.FULL_NAME, "Full name is required.");
        }
        if (!NAME.matcher(f.getFullName()).matches()) {
            throw new ValidationException(AccountForm.FULL_NAME,
                    "Full name: 2-50 letters (spaces . ' / - allowed).");
        }
        if (Validator.isBlank(f.getUsername())) {
            throw new ValidationException(AccountForm.USERNAME, "Username is required.");
        }
        if (!USERNAME.matcher(f.getUsername()).matches()) {
            throw new ValidationException(AccountForm.USERNAME,
                    "Username: 3-20 letters, digits, dot or underscore.");
        }
        if (isUsernameTaken(f.getUsername(), ownId)) {
            throw new ValidationException(AccountForm.USERNAME,
                    "Username \"" + f.getUsername() + "\" is already used.");
        }
        if (!f.getPassword().isEmpty() && (f.getPassword().length() < 6 || f.getPassword().contains(" "))) {
            throw new ValidationException(AccountForm.PASSWORD,
                    "Password: at least 6 characters, no spaces.");
        }
        if (Validator.isBlank(f.getEmail())) {
            throw new ValidationException(AccountForm.EMAIL, "Email is required.");
        }
        if (!EMAIL.matcher(f.getEmail()).matches()) {
            throw new ValidationException(AccountForm.EMAIL, "Email format is invalid (e.g. name@sda.my).");
        }
        if (Validator.isBlank(f.getPhone())) {
            throw new ValidationException(AccountForm.PHONE, "Phone is required.");
        }
        if (!PHONE.matcher(normalisePhone(f.getPhone())).matches()) {
            throw new ValidationException(AccountForm.PHONE,
                    "Phone: 9-11 digits starting with 0 (e.g. 012-345 6789).");
        }
        validateExtra(f); // POLYMORPHISM: StaffService or StudentService decides
    }

    private static void noPipe(String field, String value) throws ValidationException {
        if (Validator.hasPipe(value)) throw new ValidationException(field, "The | character is not allowed.");
    }

    /** Usernames are unique across ALL users (every role logs in from the same screen). */
    private boolean isUsernameTaken(String username, String ownId) {
        for (User u : userRepo.findAll()) {
            if (u.getUsername().equalsIgnoreCase(username) && !u.getId().equals(ownId)) return true;
        }
        return false;
    }

    /** Stores phones as digits only, so "012-345 6789" and "0123456789" are the same number. */
    protected static String normalisePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[\\s-]", "");
    }

    /**
     * Counts lines in a data file whose given column equals value.
     * Used for files that have no repository class yet (ratings.txt, problems.txt).
     */
    protected static int countInFile(String fileName, int column, String value) {
        int n = 0;
        for (String[] f : TextFileHandler.readRecords(fileName)) {
            if (f.length > column && f[column].equals(value)) n++;
        }
        return n;
    }

    /** Builds "2 bookings, 1 payment" from pairs of (count, noun). Returns null if every count is 0. */
    protected static String describe(Object... countNounPairs) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i + 1 < countNounPairs.length; i += 2) {
            int count = (Integer) countNounPairs[i];
            String noun = (String) countNounPairs[i + 1];
            String plural = noun.endsWith("s") ? noun + "es" : noun + "s"; // class -> classes
            if (count > 0) parts.add(count + " " + (count == 1 ? noun : plural));
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }
}
