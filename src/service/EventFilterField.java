package service;

/** The event fields a user is allowed to filter/search on. */
public enum EventFilterField {

    TITLE("title"),
    DESCRIPTION("description"),
    VENUE("venue");

    private final String label;

    EventFilterField(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Case-insensitive lookup used by the CLI. Returns null when unrecognised. */
    public static EventFilterField fromLabel(String text) {
        if (text == null) {
            return null;
        }
        for (EventFilterField field : values()) {
            if (field.label.equalsIgnoreCase(text.trim())) {
                return field;
            }
        }
        return null;
    }
}
