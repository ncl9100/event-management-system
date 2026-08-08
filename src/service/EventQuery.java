package service;

/**
 * The user's current view of the event list, expressed as an object: what to
 * filter on and how to sort.
 *
 * Keeping this separate from EventService means new filter or sort options can
 * be added here without touching the service or any UI.
 */
public class EventQuery {

    /** Value for sortDirection meaning A to Z / earliest first / smallest first. */
    public static final int ASCENDING = 1;

    /** Value for sortDirection meaning Z to A / latest first / largest first. */
    public static final int DESCENDING = -1;

    public EventFilterField filterField;
    public String filterValue;
    public EventSortField sortField;
    public int sortDirection;

    public EventQuery() {
        clearFilter();
        clearSort();
    }

    /** Drops the current filter so every event is shown again. */
    public void clearFilter() {
        this.filterField = null;
        this.filterValue = null;
    }

    /** Drops the current sort so events appear in stored order. */
    public void clearSort() {
        this.sortField = null;
        this.sortDirection = ASCENDING;
    }

    public boolean hasFilter() {
        return filterField != null && filterValue != null && !filterValue.trim().isEmpty();
    }

    public boolean hasSort() {
        return sortField != null;
    }

    @Override
    public String toString() {
        String filterPart = hasFilter()
                ? filterField.getLabel() + " contains \"" + filterValue + "\""
                : "none";
        String sortPart = hasSort()
                ? sortField.getLabel() + " " + (sortDirection == DESCENDING ? "desc" : "asc")
                : "none";
        return "filter: " + filterPart + "   |   sort: " + sortPart;
    }
}
