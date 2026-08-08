package ui.cli;

import java.util.List;
import java.util.Optional;

import model.Event;
import model.Reservation;
import service.EventService;

/**
 * Boundary object. The attendee list for one event.
 *
 * Implements UC-07 (View Event Reservations) and hosts UC-08 / UC-09 / UC-10
 * (add, edit, cancel a reservation), including UC-15 (Verify Remaining
 * Capacity) and UC-17 (Validate Attendee Deletion).
 *
 * It edits a deep copy of the event it is given and returns that copy only when
 * the user keeps the changes, so backing out here changes nothing upstream.
 */
public class ReservationListCLI extends AbstractCLI {

    private Event event;
    private ReservationCLI reservationCLI;

    public ReservationListCLI(EventService eventService) {
        super(eventService);
        // Composition: this list owns the attendee form it opens.
        this.reservationCLI = new ReservationCLI(eventService);
    }

    /**
     * Shows the attendee list for this event.
     *
     * @return the event with its updated reservations, or an empty Optional
     *         when the user discarded the changes
     */
    public Optional<Event> run(Event event) {
        this.event = event.deepCopy();

        printHeading("Reservations - " + this.event.getTitle());
        showHelp();
        showReservations();

        while (true) {
            Command command = promptAndParse("reservations> ");

            if (command.name.isEmpty()) {
                continue;
            } else if (command.name.equals("help")) {
                showHelp();
            } else if (command.name.equals("list")) {
                showReservations();
            } else if (command.name.equals("add")) {
                handleAdd();
            } else if (command.name.equals("edit")) {
                handleEdit(command);
            } else if (command.name.equals("delete") || command.name.equals("cancel")) {
                handleDelete(command);
            } else if (command.name.equals("done") || command.name.equals("save")) {
                printInfo("Reservation changes kept. Save the event to make them permanent.");
                return Optional.of(this.event);
            } else if (command.name.equals("abort") || command.name.equals("quit")) {
                printInfo("Discarded the reservation changes.");
                return Optional.empty();
            } else {
                printUnknownCommand(command.name);
            }
        }
    }

    // ------------------------------------------------------- UC-08 Add

    private void handleAdd() {
        // UC-15: Verify Remaining Capacity, before we even open the form.
        if (!EventService.canAddReservation(event)) {
            if (event.getCapacity() <= 0) {
                printError("This event has no capacity set, so nobody can be registered yet.");
            } else {
                printError("This event is full (" + event.getReservations().size()
                        + " of " + event.getCapacity() + " seats taken).");
            }
            return;
        }
        Optional<Reservation> created = reservationCLI.create(event);
        if (created.isPresent()) {
            event.getReservations().add(created.get());
            printInfo("Added " + created.get().getFullName() + ".");
        }
        showReservations();
    }

    // ------------------------------------------------------ UC-09 Edit

    private void handleEdit(Command command) {
        int index = readIndex(command);
        if (index < 0) {
            return;
        }
        Reservation original = event.getReservations().get(index);
        Optional<Reservation> edited = reservationCLI.edit(event, original);
        if (edited.isPresent()) {
            event.getReservations().set(index, edited.get());
            printInfo("Updated " + edited.get().getFullName() + ".");
        }
        showReservations();
    }

    // ---------------------------------------------------- UC-10 Delete

    private void handleDelete(Command command) {
        int index = readIndex(command);
        if (index < 0) {
            return;
        }
        Reservation target = event.getReservations().get(index);
        // UC-17: Validate Attendee Deletion
        if (!confirm("Cancel the reservation for " + target.getFullName() + "?")) {
            printInfo("Nothing was cancelled.");
            return;
        }
        event.getReservations().remove(index);
        printInfo("Cancelled the reservation for " + target.getFullName() + ".");
        showReservations();
    }

    // ---------------------------------------------------------- display

    /**
     * Reads the row number argument and converts it to a list index.
     * Returns -1 (after printing why) when it is missing or out of range.
     */
    private int readIndex(Command command) {
        if (command.argCount() < 1) {
            printError("Usage: " + command.name + " <row>");
            return -1;
        }
        int row = parseInt(command.arg(0), -1);
        if (row < 1 || row > event.getReservations().size()) {
            printError("There is no reservation on row " + command.arg(0) + ".");
            return -1;
        }
        return row - 1;
    }

    private void showReservations() {
        List<Reservation> reservations = event.getReservations();
        blankLine();
        println("  " + event.getTitle()
                + "   |   " + reservations.size() + " of " + event.getCapacity()
                + " seats taken   |   " + event.getRemainingCapacity() + " free");
        blankLine();
        if (reservations.isEmpty()) {
            printInfo("No attendees are registered yet. Type add to register one.");
            blankLine();
            return;
        }
        println("  " + pad("ROW", 5) + pad("NAME", 26) + pad("EMAIL", 30)
                + pad("PHONE", 18) + "REGISTERED");
        println("  " + repeat('-', 5 + 26 + 30 + 18 + 16));
        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);
            println("  " + pad(String.valueOf(i + 1), 5)
                    + pad(r.getFullName(), 26)
                    + pad(r.getEmail(), 30)
                    + pad(r.getPhone(), 18)
                    + formatDateTime(r.getDateRegistered()));
        }
        blankLine();
    }

    private void showHelp() {
        println("  list                  show the attendee list");
        println("  add                   register a new attendee");
        println("  edit <row>            change an attendee's details");
        println("  delete <row>          cancel an attendee's registration");
        println("  done                  keep these changes and go back to the event");
        println("  abort                 discard these changes and go back");
    }
}
