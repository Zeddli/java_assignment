package util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
 
/** Shared input checks. */
public final class Validator {
    private Validator() { }
 
    public static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
 
    /** '|' is the file delimiter, so users must never type it. */
    public static boolean hasPipe(String s) { return s != null && s.contains("|"); }
 
    public static boolean isDate(String s) {
        try { LocalDate.parse(s); return true; } catch (DateTimeParseException | NullPointerException e) { return false; }
    }
}

