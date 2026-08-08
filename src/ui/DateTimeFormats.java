package ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * One definition of how a date and time is typed in and shown, shared by the
 * command line and the windowed front end.
 *
 * Without this, the two UIs would drift apart and users would have to remember
 * two formats.
 */
public final class DateTimeFormats {

    /** The single format the whole application reads and writes: 2026-09-14 18:30 */
    public static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** Shown next to date inputs so the user knows what to type. */
    public static final String HINT = "yyyy-MM-dd HH:mm";

    private DateTimeFormats() {
        // utility class, never instantiated
    }

    /** Formats a date for display. Null becomes "(not set)". */
    public static String format(LocalDateTime value) {
        if (value == null) {
            return "(not set)";
        }
        return value.format(DISPLAY);
    }

    /** Formats a date for a text box. Null becomes an empty string. */
    public static String formatForInput(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value.format(DISPLAY);
    }

    /** Reads a typed date. Returns null when it is blank or unreadable. */
    public static LocalDateTime parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.trim(), DISPLAY);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
