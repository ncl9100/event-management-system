package service;

/** The event fields a user is allowed to sort the browsed list by. */
public enum EventSortField {

    TITLE("title"),
    CAPACITY("capacity"),
    REMAINING_CAPACITY("remaining"),
    START_DATE("start"),
    END_DATE("end");

    private final String label;

    EventSortField(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Case-insensitive lookup used by the CLI. Returns null when unrecognised. */
    public static EventSortField fromLabel(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        for (EventSortField field : values()) {
            if (field.label.equalsIgnoreCase(trimmed) || field.name().equalsIgnoreCase(trimmed)) {
                return field;
            }
        }
        return null;
    }
}
