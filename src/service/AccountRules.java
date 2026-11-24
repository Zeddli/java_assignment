package service;

import java.util.regex.Pattern;
import util.ValidationException;
import util.Validator;

/**
 * Input rules shared by every screen that edits an account (AD1, AD2 and My Profile),
 * so a name, email, phone or password is checked the same way everywhere.
 * Each method throws a ValidationException naming the field that failed.
 */
public final class AccountRules {
    private AccountRules() { }

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._]{3,20}$");
    private static final Pattern NAME = Pattern.compile("^[A-Za-z][A-Za-z .'/-]{1,49}$");
    private static final Pattern PHONE = Pattern.compile("^0\\d{8,10}$"); // after removing spaces and dashes

    public static final int MIN_PASSWORD = 6;

    /** '|' separates the fields in every .txt file, so it can never be typed into one. */
    public static void noPipe(String field, String value) throws ValidationException {
        if (Validator.hasPipe(value)) throw new ValidationException(field, "The | character is not allowed.");
    }

    public static void fullName(String field, String value) throws ValidationException {
        noPipe(field, value);
        if (Validator.isBlank(value)) throw new ValidationException(field, "Full name is required.");
        if (!NAME.matcher(value.trim()).matches()) {
            throw new ValidationException(field, "Full name: 2-50 letters (spaces . ' / - allowed).");
        }
    }

    public static void username(String field, String value) throws ValidationException {
        noPipe(field, value);
        if (Validator.isBlank(value)) throw new ValidationException(field, "Username is required.");
        if (!USERNAME.matcher(value.trim()).matches()) {
            throw new ValidationException(field, "Username: 3-20 letters, digits, dot or underscore.");
        }
    }

    /** Checks a NEW password (blank passwords are handled by the caller). */
    public static void password(String field, String value) throws ValidationException {
        noPipe(field, value);
        if (value.length() < MIN_PASSWORD || value.contains(" ")) {
            throw new ValidationException(field, "Password: at least " + MIN_PASSWORD + " characters, no spaces.");
        }
    }

    public static void email(String field, String value) throws ValidationException {
        noPipe(field, value);
        if (Validator.isBlank(value)) throw new ValidationException(field, "Email is required.");
        if (!EMAIL.matcher(value.trim()).matches()) {
            throw new ValidationException(field, "Email format is invalid (e.g. name@sda.my).");
        }
    }

    public static void phone(String field, String value) throws ValidationException {
        noPipe(field, value);
        if (Validator.isBlank(value)) throw new ValidationException(field, "Phone is required.");
        if (!PHONE.matcher(normalisePhone(value)).matches()) {
            throw new ValidationException(field, "Phone: 9-11 digits starting with 0 (e.g. 012-345 6789).");
        }
    }

    /** Stores phones as digits only, so "012-345 6789" and "0123456789" are the same number. */
    public static String normalisePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[\\s-]", "");
    }
}
