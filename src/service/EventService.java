package service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import model.Event;
import model.Reservation;
import repository.EventRepository;

/**
 * Control object sitting between the UIs and the store.
 *
 * It owns two things: the rules that decide whether an event or a reservation
 * is acceptable, and the translation of an EventQuery into a filtered, sorted
 * list. Every UI in the program goes through here, so those rules cannot be
 * bypassed by adding another screen.
 *
 * It talks to EventRepository (the interface), never to a concrete store.
 */
public class EventService {

    private EventRepository repository;
    private List<Event> events;

    public EventService(EventRepository repository) {
        this.repository = repository;
        this.events = repository.loadAll();
    }

    /**
     * Returns the events to display. An empty query (or none at all) means
     * "everything, in stored order".
     */
    public List<Event> getEvents(Optional<EventQuery> query) {
        this.events = repository.loadAll();
        List<Event> result = new ArrayList<Event>(this.events);
        if (query == null || !query.isPresent()) {
            return result;
        }
        EventQuery q = query.get();
        result = applyFilter(result, q);
        result = applySort(result, q);
        return result;
    }

    /** Convenience for callers that want the unfiltered list. */
    public List<Event> getEvents() {
        return getEvents(Optional.<EventQuery>empty());
    }

    /** Returns the stored event with this id, or an empty Optional. */
    public Optional<Event> getEventById(int id) {
        for (Event e : repository.loadAll()) {
            if (e.getId() == id) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    /**
     * Validates and stores a brand new event.
     *
     * @return the id assigned to it
     * @throws ValidationException when the event breaks a rule
     */
    public int insertEvent(Event e) throws ValidationException {
        List<String> errors = validateEvent(e);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        int id = repository.insertEvent(e);
        this.events = repository.loadAll();
        return id;
    }

    /**
     * Validates and saves changes to an existing event.
     *
     * @return false when no stored event has that id
     */
    public boolean updateEvent(Event e) throws ValidationException {
        List<String> errors = validateEvent(e);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        boolean updated = repository.updateEvent(e);
        this.events = repository.loadAll();
        return updated;
    }

    /** Validates and saves changes to several events at once. */
    public boolean updateEvents(List<Event> es) throws ValidationException {
        List<String> errors = new ArrayList<String>();
        for (Event e : es) {
            errors.addAll(validateEvent(e));
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        boolean allUpdated = true;
        for (Event e : es) {
            if (!repository.updateEvent(e)) {
                allUpdated = false;
            }
        }
        this.events = repository.loadAll();
        return allUpdated;
    }

    /** Removes an event and everything registered to it. */
    public boolean deleteEvent(int id) {
        boolean deleted = repository.deleteEvent(id);
        this.events = repository.loadAll();
        return deleted;
    }

    // ------------------------------------------------------------ validation

    /** True when this event breaks none of the rules. */
    public static boolean isValidEvent(Event e) {
        return validateEvent(e).isEmpty();
    }

    /** True when this reservation breaks none of the rules. */
    public static boolean isValidReservation(Reservation r) {
        return validateReservation(r).isEmpty();
    }

    /** True when the event still has room for one more attendee. */
    public static boolean canAddReservation(Event e) {
        if (e == null) {
            return false;
        }
        if (e.getCapacity() <= 0) {
            return false;
        }
        return !e.isFull();
    }

    /**
     * Every rule this event breaks, in the order a user would want to read them.
     * An empty list means the event is valid.
     */
    public static List<String> validateEvent(Event e) {
        List<String> errors = new ArrayList<String>();
        if (e == null) {
            errors.add("No event was supplied.");
            return errors;
        }
        if (isBlank(e.getTitle())) {
            errors.add("Title is required.");
        } else if (e.getTitle().length() > 120) {
            errors.add("Title must be 120 characters or fewer.");
        }
        if (isBlank(e.getVenueName())) {
            errors.add("Venue name is required.");
        }
        if (e.getStartDateTime() == null) {
            errors.add("Start date and time is required.");
        }
        if (e.getEndDateTime() == null) {
            errors.add("End date and time is required.");
        }
        if (e.getStartDateTime() != null && e.getEndDateTime() != null
                && !e.getEndDateTime().isAfter(e.getStartDateTime())) {
            errors.add("End date and time must be after the start date and time.");
        }
        if (e.getCapacity() < 1) {
            errors.add("Capacity must be at least 1.");
        }
        if (e.getCapacity() > 0 && e.getReservations().size() > e.getCapacity()) {
            errors.add("Capacity (" + e.getCapacity() + ") is lower than the "
                    + e.getReservations().size() + " reservations already registered.");
        }
        List<Reservation> reservations = e.getReservations();
        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);
            for (String reservationError : validateReservation(r)) {
                errors.add("Reservation " + (i + 1) + ": " + reservationError);
            }
            // Compare against the reservations already checked, so each clash
            // is reported once, against the later of the two.
            String email = normalisedEmail(r);
            if (!email.isEmpty()) {
                for (int j = 0; j < i; j++) {
                    if (normalisedEmail(reservations.get(j)).equals(email)) {
                        errors.add("Reservation " + (i + 1) + ": " + r.getEmail()
                                + " is already registered for this event.");
                        break;
                    }
                }
            }
        }
        return errors;
    }

    /**
     * Every rule this reservation breaks. An empty list means it is valid.
     */
    public static List<String> validateReservation(Reservation r) {
        List<String> errors = new ArrayList<String>();
        if (r == null) {
            errors.add("No reservation was supplied.");
            return errors;
        }
        if (isBlank(r.getFirstName())) {
            errors.add("First name is required.");
        }
        if (isBlank(r.getLastName())) {
            errors.add("Last name is required.");
        }
        if (isBlank(r.getEmail())) {
            errors.add("Email is required.");
        } else if (!isPlausibleEmail(r.getEmail())) {
            errors.add("\"" + r.getEmail() + "\" is not a valid email address.");
        }
        if (!isBlank(r.getPhone()) && countDigits(r.getPhone()) < 7) {
            errors.add("Phone number must contain at least 7 digits.");
        }
        if (r.getDateRegistered() == null) {
            errors.add("Registration date is required.");
        }
        return errors;
    }

    // --------------------------------------------------------- query support

    private List<Event> applyFilter(List<Event> source, EventQuery query) {
        if (!query.hasFilter()) {
            return source;
        }
        String needle = query.filterValue.trim().toLowerCase();
        List<Event> matches = new ArrayList<Event>();
        for (Event e : source) {
            if (fieldValueOf(e, query.filterField).toLowerCase().contains(needle)) {
                matches.add(e);
            }
        }
        return matches;
    }

    private String fieldValueOf(Event e, EventFilterField field) {
        if (field == EventFilterField.TITLE) {
            return nullToEmpty(e.getTitle());
        }
        if (field == EventFilterField.DESCRIPTION) {
            return nullToEmpty(e.getDescription());
        }
        // VENUE searches the whole venue, not just its name.
        return nullToEmpty(e.getVenueName()) + " "
                + nullToEmpty(e.getVenueAddress()) + " "
                + nullToEmpty(e.getVenueDetails());
    }

    /**
     * Returns a sorted copy of the list, using an insertion sort.
     *
     * The algorithm is written out rather than handed to a library call for
     * three reasons: the list is small (tens of events), it is usually already
     * close to sorted, which is the case insertion sort handles best, and it is
     * stable -- events that tie on the sort field keep their stored order
     * instead of being shuffled.
     */
    private List<Event> applySort(List<Event> source, EventQuery query) {
        if (!query.hasSort()) {
            return source;
        }
        List<Event> sorted = new ArrayList<Event>(source);
        for (int i = 1; i < sorted.size(); i++) {
            Event current = sorted.get(i);
            int j = i - 1;
            // Slide everything that belongs after "current" one place right.
            while (j >= 0 && belongsAfter(sorted.get(j), current, query)) {
                sorted.set(j + 1, sorted.get(j));
                j--;
            }
            sorted.set(j + 1, current);
        }
        return sorted;
    }

    /** True when event a should appear below event b under this query's sort. */
    private boolean belongsAfter(Event a, Event b, EventQuery query) {
        int comparison = compareByField(a, b, query.sortField);
        if (query.sortDirection == EventQuery.DESCENDING) {
            comparison = -comparison;
        }
        return comparison > 0;
    }

    private int compareByField(Event a, Event b, EventSortField field) {
        if (field == EventSortField.TITLE) {
            return nullToEmpty(a.getTitle()).compareToIgnoreCase(nullToEmpty(b.getTitle()));
        }
        if (field == EventSortField.CAPACITY) {
            return compareNumbers(a.getCapacity(), b.getCapacity());
        }
        if (field == EventSortField.REMAINING_CAPACITY) {
            return compareNumbers(a.getRemainingCapacity(), b.getRemainingCapacity());
        }
        if (field == EventSortField.START_DATE) {
            return compareDates(a.getStartDateTime(), b.getStartDateTime());
        }
        return compareDates(a.getEndDateTime(), b.getEndDateTime());
    }

    private int compareNumbers(int a, int b) {
        if (a < b) {
            return -1;
        }
        if (a > b) {
            return 1;
        }
        return 0;
    }

    private int compareDates(LocalDateTime a, LocalDateTime b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1; // events with no date sort to the bottom
        }
        if (b == null) {
            return -1;
        }
        return a.compareTo(b);
    }

    // ---------------------------------------------------------------- helpers

    /** An email in the form used for comparing: trimmed and lower case. */
    private static String normalisedEmail(Reservation r) {
        if (r.getEmail() == null) {
            return "";
        }
        return r.getEmail().trim().toLowerCase();
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private static String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    private static int countDigits(String text) {
        int digits = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                digits++;
            }
        }
        return digits;
    }

    private static boolean isPlausibleEmail(String email) {
        String trimmed = email.trim();
        int at = trimmed.indexOf('@');
        if (at <= 0 || at != trimmed.lastIndexOf('@')) {
            return false;
        }
        String domain = trimmed.substring(at + 1);
        if (domain.length() < 3 || trimmed.contains(" ")) {
            return false;
        }
        int dot = domain.indexOf('.');
        return dot > 0 && dot < domain.length() - 1;
    }
}
