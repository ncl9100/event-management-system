package ui.cli;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import model.Event;
import service.EventService;
import service.EventStatistics;

/**
 * Boundary object. The detail form for one event.
 *
 * Implements UC-01 (Create Event), UC-04 (View Event Details and Statistics)
 * and UC-05 (Edit Event), and includes UC-13 (Verify Event Information) on save.
 * It is also the way into UC-07, the reservation list.
 *
 * Everything happens on a copy. The caller only sees an Event come back when
 * the user saves, which is what makes abort safe.
 */
public class EventCLI extends AbstractCLI {

    private Event event;
    private ReservationListCLI reservationListCLI;

    public EventCLI(EventService eventService) {
        super(eventService);
        // Composition: this form owns the reservation list it opens.
        this.reservationListCLI = new ReservationListCLI(eventService);
    }

    /**
     * Collects a brand new event.
     *
     * @return the event to insert, or an empty Optional when the user aborted
     */
    public Optional<Event> create() {
        this.event = new Event();
        return editLoop("New event");
    }

    /**
     * Views and edits an existing event.
     *
     * @return the changed event to save, or an empty Optional when the user aborted
     */
    public Optional<Event> edit(Event event) {
        this.event = event.deepCopy();
        return editLoop("Event " + event.getId());
    }

    // ------------------------------------------------------------------------

    private Optional<Event> editLoop(String heading) {
        printHeading(heading);
        showHelp();
        showEvent();

        while (true) {
            Command command = promptAndParse("event> ");

            if (command.name.isEmpty()) {
                continue;
            } else if (command.name.equals("help")) {
                showHelp();
            } else if (command.name.equals("show")) {
                showEvent();
            } else if (command.name.equals("set")) {
                handleSet(command);
            } else if (command.name.equals("reservations")) {
                handleReservations();
            } else if (command.name.equals("stats")) {
                showStatistics();
            } else if (command.name.equals("save")) {
                if (trySave()) {
                    return Optional.of(event);
                }
            } else if (command.name.equals("abort") || command.name.equals("cancel")
                    || command.name.equals("quit")) {
                printInfo("Aborted. No changes were kept.");
                return Optional.empty();
            } else {
                printUnknownCommand(command.name);
            }
        }
    }

    private boolean trySave() {
        // UC-13: Verify Event Information
        List<String> errors = EventService.validateEvent(event);
        if (!errors.isEmpty()) {
            printError("This event cannot be saved yet:");
            printErrors(errors);
            return false;
        }
        printInfo("Event accepted.");
        return true;
    }

    // -------------------------------------------------------- UC-07 entry

    private void handleReservations() {
        Optional<Event> updated = reservationListCLI.run(event);
        if (updated.isPresent()) {
            // Adopt the reservations the user built up on the copy.
            event.setReservations(updated.get().getReservations());
        }
        showEvent();
    }

    // ------------------------------------------------------------ editing

    private void handleSet(Command command) {
        String field = command.arg(0);
        if (field == null) {
            printError("Usage: set <field> <value>");
            return;
        }
        String value = command.argsFrom(1);
        String key = field.toLowerCase();

        if (key.equals("title")) {
            event.setTitle(value);
        } else if (key.equals("description")) {
            event.setDescription(value);
        } else if (key.equals("venuename")) {
            event.setVenueName(value);
        } else if (key.equals("venueaddress")) {
            event.setVenueAddress(value);
        } else if (key.equals("venuedetails")) {
            event.setVenueDetails(value);
        } else if (key.equals("start")) {
            if (!setStart(value)) {
                return;
            }
        } else if (key.equals("end")) {
            if (!setEnd(value)) {
                return;
            }
        } else if (key.equals("capacity")) {
            if (!setCapacity(value)) {
                return;
            }
        } else {
            printError("Unknown field \"" + field + "\". Fields are: "
                    + listFieldNames() + ".");
            return;
        }
        printInfo(key + " set to \"" + value + "\"");
    }

    private boolean setStart(String value) {
        LocalDateTime parsed = parseDateTime(value);
        if (parsed == null) {
            printError("Could not read \"" + value + "\". Use the format 2026-09-14 18:30");
            return false;
        }
        event.setStartDateTime(parsed);
        return true;
    }

    private boolean setEnd(String value) {
        LocalDateTime parsed = parseDateTime(value);
        if (parsed == null) {
            printError("Could not read \"" + value + "\". Use the format 2026-09-14 21:00");
            return false;
        }
        event.setEndDateTime(parsed);
        return true;
    }

    private boolean setCapacity(String value) {
        int parsed = parseInt(value, -1);
        if (parsed < 0) {
            printError("Capacity must be a whole number, for example: set capacity 120");
            return false;
        }
        if (parsed < event.getReservations().size()) {
            printError("There are already " + event.getReservations().size()
                    + " reservations, so capacity cannot be lower than that.");
            return false;
        }
        event.setCapacity(parsed);
        return true;
    }

    private List<String> fieldNames() {
        return Arrays.asList("title", "description", "venuename", "venueaddress",
                "venuedetails", "start", "end", "capacity");
    }

    /** The field names as one comma separated line, for the error message. */
    private String listFieldNames() {
        StringBuilder builder = new StringBuilder();
        for (String name : fieldNames()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(name);
        }
        return builder.toString();
    }

    // ------------------------------------------------------------ display

    private void showEvent() {
        blankLine();
        if (event.getId() > 0) {
            println("  id             : " + event.getId());
        }
        println("  title          : " + event.getTitle());
        println("  description    : " + event.getDescription());
        println("  venue name     : " + event.getVenueName());
        println("  venue address  : " + event.getVenueAddress());
        println("  venue details  : " + event.getVenueDetails());
        println("  start          : " + formatDateTime(event.getStartDateTime()));
        println("  end            : " + formatDateTime(event.getEndDateTime()));
        println("  capacity       : " + event.getCapacity());
        println("  reservations   : " + event.getReservations().size()
                + "   (" + event.getRemainingCapacity() + " seats free"
                + (event.isFull() ? ", FULL" : "") + ")");
        blankLine();
    }

    /** UC-04: the statistics half of "View Event Details and Statistics". */
    private void showStatistics() {
        blankLine();
        println("  Statistics for this event");
        println("  " + repeat('-', 40));
        println("  reservations         : " + event.getReservations().size());
        println("  capacity             : " + event.getCapacity());
        println("  seats free           : " + event.getRemainingCapacity());
        println("  occupancy            : "
                + String.format("%.1f%%", EventStatistics.occupancyRateOf(event)));
        long minutes = EventStatistics.durationInMinutesOf(event);
        println("  runs for             : " + (minutes / 60) + "h " + (minutes % 60) + "m");
        println("  timing               : " + EventStatistics.describeTimeUntilStart(event));
        blankLine();
    }

    private void showHelp() {
        println("  set <field> <value>   fields: title, description, venuename,");
        println("                                venueaddress, venuedetails,");
        println("                                start, end, capacity");
        println("                        dates look like: 2026-09-14 18:30");
        println("  show                  show the current values");
        println("  stats                 show this event's statistics");
        println("  reservations          manage the attendee list");
        println("  save                  validate and return this event");
        println("  abort                 discard and go back");
    }
}
