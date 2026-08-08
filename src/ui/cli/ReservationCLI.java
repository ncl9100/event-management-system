package ui.cli;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import model.Event;
import model.Reservation;
import service.EventService;

/**
 * Boundary object. The form for one attendee: add a new reservation, or edit an
 * existing one.
 *
 * Implements UC-08 (Add Reservation) and UC-09 (Edit Reservation) data entry,
 * and includes UC-16 (Validate Attendee Information) before returning.
 *
 * It works on a copy and returns it only on save, so aborting leaves the
 * caller's reservation untouched.
 */
public class ReservationCLI extends AbstractCLI {

    private Event event;

    /** Index of the reservation being edited, or -1 when creating a new one. */
    private int editingIndex;

    public ReservationCLI(EventService eventService) {
        super(eventService);
        this.editingIndex = -1;
    }

    /**
     * Collects a brand new reservation for this event.
     *
     * @return the reservation, or an empty Optional when the user aborted
     */
    public Optional<Reservation> create(Event event) {
        this.event = event;
        this.editingIndex = -1;
        Reservation reservation = new Reservation();
        reservation.setDateRegistered(LocalDateTime.now());
        return editLoop(reservation, "New reservation");
    }

    /**
     * Edits an existing reservation.
     *
     * @return the changed reservation, or an empty Optional when the user aborted
     */
    public Optional<Reservation> edit(Event event, Reservation r) {
        this.event = event;
        this.editingIndex = indexOf(event, r);
        return editLoop(r.deepCopy(), "Edit reservation");
    }

    // ------------------------------------------------------------------------

    private Optional<Reservation> editLoop(Reservation reservation, String heading) {
        printHeading(heading + " - " + event.getTitle());
        showHelp();
        showReservation(reservation);

        while (true) {
            Command command = promptAndParse("reservation> ");

            if (command.name.isEmpty()) {
                continue;
            } else if (command.name.equals("help")) {
                showHelp();
            } else if (command.name.equals("show")) {
                showReservation(reservation);
            } else if (command.name.equals("set")) {
                handleSet(command, reservation);
            } else if (command.name.equals("save")) {
                if (trySave(reservation)) {
                    return Optional.of(reservation);
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

    private boolean trySave(Reservation reservation) {
        // UC-16: Validate Attendee Information
        List<String> errors = EventService.validateReservation(reservation);
        if (!errors.isEmpty()) {
            printError("This reservation cannot be saved yet:");
            printErrors(errors);
            return false;
        }
        if (isDuplicateEmail(reservation)) {
            printError(reservation.getEmail() + " is already registered for this event.");
            return false;
        }
        printInfo("Reservation accepted.");
        return true;
    }

    /**
     * True when another reservation on the same event already uses this email.
     * The reservation being edited is matched by identity, so re-saving it
     * unchanged is not treated as a duplicate.
     */
    private boolean isDuplicateEmail(Reservation candidate) {
        String email = candidate.getEmail() == null ? "" : candidate.getEmail().trim().toLowerCase();
        if (email.isEmpty()) {
            return false;
        }
        List<Reservation> existing = event.getReservations();
        for (int i = 0; i < existing.size(); i++) {
            if (i == editingIndex) {
                continue; // this is the reservation we are editing
            }
            String other = existing.get(i).getEmail();
            String normalised = other == null ? "" : other.trim().toLowerCase();
            if (normalised.equals(email)) {
                return true;
            }
        }
        return false;
    }

    /** Position of this exact reservation object in the event, or -1. */
    private int indexOf(Event event, Reservation r) {
        List<Reservation> existing = event.getReservations();
        for (int i = 0; i < existing.size(); i++) {
            if (existing.get(i) == r) {
                return i;
            }
        }
        return -1;
    }

    private void handleSet(Command command, Reservation reservation) {
        String field = command.arg(0);
        if (field == null) {
            printError("Usage: set <field> <value>");
            return;
        }
        String value = command.argsFrom(1);
        String key = field.toLowerCase();

        if (key.equals("firstname")) {
            reservation.setFirstName(value);
        } else if (key.equals("lastname")) {
            reservation.setLastName(value);
        } else if (key.equals("email")) {
            reservation.setEmail(value);
        } else if (key.equals("phone")) {
            reservation.setPhone(value);
        } else {
            printError("Unknown field \"" + field + "\". "
                    + "Try firstname, lastname, email or phone.");
            return;
        }
        printInfo(key + " set to \"" + value + "\"");
    }

    private void showReservation(Reservation r) {
        blankLine();
        println("  first name     : " + r.getFirstName());
        println("  last name      : " + r.getLastName());
        println("  email          : " + r.getEmail());
        println("  phone          : " + r.getPhone());
        println("  registered     : " + formatDateTime(r.getDateRegistered()));
        println("  for event      : " + event.getTitle()
                + "  (" + event.getRemainingCapacity() + " of "
                + event.getCapacity() + " seats free)");
        blankLine();
    }

    private void showHelp() {
        println("  set <field> <value>   fields: firstname, lastname, email, phone");
        println("  show                  show the current values");
        println("  save                  validate and return this reservation");
        println("  abort                 discard and go back");
    }
}
