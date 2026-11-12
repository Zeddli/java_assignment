package util;

/**
 * Custom CHECKED exception for bad user input.
 * The service throws it, and the GUI catches it and shows the message inline in red.
 * {@code field} tells the screen which input box to highlight (may be null).
 */
public class ValidationException extends Exception {
    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public ValidationException(String message) {
        this(null, message);
    }

    public String getField() { return field; }
}
