package ui.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import model.Event;
import service.EventFilterField;
import service.EventQuery;
import service.EventService;
import service.EventSortField;
import service.ValidationException;
import ui.EventListUI;

/**
 * Boundary object. The command-line home screen: the list of events and
 * everything you can start from it.
 *
 * Implements UC-02 (Browse Events), UC-03 (Search for Event), UC-11 (Filter
 * Events), UC-12 (Sort Events), and launches UC-01 / UC-04 / UC-05 through
 * EventCLI. It also handles UC-06 (Delete Event) including UC-14 (Confirm
 * Event Deletion).
 *
 * It holds no business rules of its own: everything it changes goes through
 * EventService, and everything it displays comes back from EventService.
 */
public class EventListCLI extends AbstractCLI implements EventListUI {

    private EventQuery query;
    private List<Event> displayedEvents;
    private EventCLI eventCLI;

    public EventListCLI(EventService eventService) {
        super(eventService);
        this.query = new EventQuery();
        this.displayedEvents = new ArrayList<Event>();
        // Composition: this screen owns the event form it opens.
        this.eventCLI = new EventCLI(eventService);
    }

    @Override
    public void run() {
        printHeading("Event Management System");
        showHelp();
        refresh();

        while (true) {
            Command command = promptAndParse("events> ");

            if (command.name.isEmpty()) {
                continue;
            } else if (command.name.equals("help")) {
                showHelp();
            } else if (command.name.equals("list")) {
                refresh();
            } else if (command.name.equals("add")) {
                handleAdd();
            } else if (command.name.equals("view") || command.name.equals("edit")) {
                handleViewOrEdit(command);
            } else if (command.name.equals("delete")) {
                handleDelete(command);
            } else if (command.name.equals("search")) {
                handleSearch(command);
            } else if (command.name.equals("filterby")) {
                handleFilterBy(command);
            } else if (command.name.equals("clearfilter")) {
                query.clearFilter();
                printInfo("Filter cleared.");
                refresh();
            } else if (command.name.equals("sortby")) {
                handleSortBy(command);
            } else if (command.name.equals("clearsort")) {
                query.clearSort();
                printInfo("Sort cleared.");
                refresh();
            } else if (command.name.equals("stats")) {
                showStatistics();
            } else if (command.name.equals("quit") || command.name.equals("exit")) {
                println("Goodbye.");
                return;
            } else {
                printUnknownCommand(command.name);
            }
        }
    }

    // ------------------------------------------------------- UC-01 Create

    private void handleAdd() {
        Optional<Event> created = eventCLI.create();
        if (created.isPresent()) {
            try {
                int id = eventService.insertEvent(created.get());
                printInfo("Created event " + id + ".");
            } catch (ValidationException e) {
                printError("The event could not be saved:");
                printErrors(e.getErrors());
            }
        }
        refresh();
    }

    // ------------------------------------------------- UC-04 / UC-05 View

    private void handleViewOrEdit(Command command) {
        Optional<Event> found = readEvent(command);
        if (!found.isPresent()) {
            return;
        }
        Optional<Event> edited = eventCLI.edit(found.get());
        if (edited.isPresent()) {
            try {
                if (eventService.updateEvent(edited.get())) {
                    printInfo("Saved event " + edited.get().getId() + ".");
                } else {
                    printError("Event " + edited.get().getId() + " no longer exists.");
                }
            } catch (ValidationException e) {
                printError("The changes could not be saved:");
                printErrors(e.getErrors());
            }
        }
        refresh();
    }

    // ------------------------------------------------------- UC-06 Delete

    private void handleDelete(Command command) {
        Optional<Event> found = readEvent(command);
        if (!found.isPresent()) {
            return;
        }
        Event target = found.get();
        // UC-14: Confirm Event Deletion
        String warning = target.getReservations().isEmpty()
                ? ""
                : " This also cancels " + target.getReservations().size() + " reservation(s).";
        if (!confirm("Delete \"" + target.getTitle() + "\"?" + warning)) {
            printInfo("Nothing was deleted.");
            return;
        }
        if (eventService.deleteEvent(target.getId())) {
            printInfo("Deleted event " + target.getId() + ".");
        } else {
            printError("Event " + target.getId() + " could not be deleted.");
        }
        refresh();
    }

    // ---------------------------------------- UC-03 Search / UC-11 Filter

    private void handleSearch(Command command) {
        if (command.argCount() < 1) {
            printError("Usage: search <text>");
            return;
        }
        query.filterField = EventFilterField.TITLE;
        query.filterValue = command.argsFrom(0);
        printInfo("Searching titles for \"" + query.filterValue + "\".");
        refresh();
    }

    private void handleFilterBy(Command command) {
        if (command.argCount() < 2) {
            printError("Usage: filterby <title|description|venue> <text>");
            return;
        }
        EventFilterField field = EventFilterField.fromLabel(command.arg(0));
        if (field == null) {
            printError("Cannot filter by \"" + command.arg(0)
                    + "\". Choose title, description or venue.");
            return;
        }
        query.filterField = field;
        query.filterValue = command.argsFrom(1);
        refresh();
    }

    // ---------------------------------------------------------- UC-12 Sort

    private void handleSortBy(Command command) {
        if (command.argCount() < 1) {
            printError("Usage: sortby <title|capacity|remaining|start|end> [asc|desc]");
            return;
        }
        EventSortField field = EventSortField.fromLabel(command.arg(0));
        if (field == null) {
            printError("Cannot sort by \"" + command.arg(0)
                    + "\". Choose title, capacity, remaining, start or end.");
            return;
        }
        query.sortField = field;
        String direction = command.arg(1);
        if (direction != null && direction.equalsIgnoreCase("desc")) {
            query.sortDirection = EventQuery.DESCENDING;
        } else {
            query.sortDirection = EventQuery.ASCENDING;
        }
        refresh();
    }

    // ------------------------------------------------------------ display

    /** UC-02: reload through the service and redraw the table. */
    private void refresh() {
        displayedEvents = eventService.getEvents(Optional.of(query));
        showEvents();
    }

    private void showEvents() {
        blankLine();
        println("  " + query.toString());
        blankLine();
        if (displayedEvents.isEmpty()) {
            if (query.hasFilter()) {
                printInfo("No events match that filter. Type clearfilter to see them all.");
            } else {
                printInfo("There are no events yet. Type add to create one.");
            }
            blankLine();
            return;
        }
        println("  " + pad("ID", 5) + pad("TITLE", 28) + pad("VENUE", 22)
                + pad("START", 18) + pad("END", 18)
                + pad("CAP", 6) + pad("RESVD", 7) + "FREE");
        println("  " + repeat('-', 5 + 28 + 22 + 18 + 18 + 6 + 7 + 5));
        for (Event e : displayedEvents) {
            println("  " + pad(String.valueOf(e.getId()), 5)
                    + pad(e.getTitle(), 28)
                    + pad(e.getVenueName(), 22)
                    + pad(formatDateTime(e.getStartDateTime()), 18)
                    + pad(formatDateTime(e.getEndDateTime()), 18)
                    + pad(String.valueOf(e.getCapacity()), 6)
                    + pad(String.valueOf(e.getReservations().size()), 7)
                    + (e.isFull() ? "FULL" : String.valueOf(e.getRemainingCapacity())));
        }
        blankLine();
        printInfo(displayedEvents.size() + " event(s) shown.");
        blankLine();
    }

    /** Display-wide statistics over whatever is currently listed. */
    private void showStatistics() {
        service.EventStatistics stats = new service.EventStatistics(displayedEvents);
        blankLine();
        println("  Statistics for the " + stats.getTotalEvents() + " event(s) listed");
        println("  " + repeat('-', 46));
        println("  total reservations   : " + stats.getTotalReservations());
        println("  total capacity       : " + stats.getTotalCapacity());
        println("  seats still free     : " + stats.getTotalRemainingCapacity());
        println("  overall occupancy    : " + String.format("%.1f%%", stats.getOverallOccupancyRate()));
        println("  average occupancy    : " + String.format("%.1f%%", stats.getAverageOccupancyRate()));
        println("  full events          : " + stats.getFullEventCount());
        println("  events with nobody   : " + stats.getEmptyEventCount());
        println("  upcoming / now / past: " + stats.getUpcomingEventCount()
                + " / " + stats.getInProgressEventCount()
                + " / " + stats.getPastEventCount());
        Event busiest = stats.getBusiestEvent();
        println("  busiest event        : "
                + (busiest == null ? "(none)"
                        : busiest.getTitle() + " (" + busiest.getReservations().size() + ")"));
        Event next = stats.getNextUpcomingEvent();
        println("  next event           : "
                + (next == null ? "(none scheduled)"
                        : next.getTitle() + " on " + formatDateTime(next.getStartDateTime())));
        blankLine();
    }

    /** Looks up the event id given as the first argument. */
    private Optional<Event> readEvent(Command command) {
        if (command.argCount() < 1) {
            printError("Usage: " + command.name + " <id>");
            return Optional.empty();
        }
        int id = parseInt(command.arg(0), -1);
        if (id < 0) {
            printError("\"" + command.arg(0) + "\" is not a valid event id.");
            return Optional.empty();
        }
        Optional<Event> found = eventService.getEventById(id);
        if (!found.isPresent()) {
            printError("There is no event with id " + id + ".");
        }
        return found;
    }

    private void showHelp() {
        println("  list                          show the events");
        println("  add                           create a new event");
        println("  view <id> / edit <id>         open an event");
        println("  delete <id>                   delete an event");
        println("  search <text>                 find events by title");
        println("  filterby <field> <text>       field: title, description, venue");
        println("  clearfilter                   show every event again");
        println("  sortby <field> [asc|desc]     field: title, capacity, remaining, start, end");
        println("  clearsort                     back to stored order");
        println("  stats                         statistics for the listed events");
        println("  quit                          leave the program");
    }
}
