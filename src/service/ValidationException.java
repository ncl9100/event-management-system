package service;

import java.util.ArrayList;
import java.util.List;

/**
 * Thrown when an Event or Reservation fails the rules in EventService.
 *
 * Carries every problem found, not just the first one, so a UI can show the
 * user the complete list in one go.
 */
public class ValidationException extends Exception {

    private static final long serialVersionUID = 1L;

    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super(join(errors));
        this.errors = new ArrayList<String>(errors);
    }

    /** A copy, so a caller cannot change the list held by this exception. */
    public List<String> getErrors() {
        return new ArrayList<String>(errors);
    }

    private static String join(List<String> errors) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < errors.size(); i++) {
            if (i > 0) {
                builder.append("; ");
            }
            builder.append(errors.get(i));
        }
        return builder.toString();
    }
}
