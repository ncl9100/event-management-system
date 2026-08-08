# Event Management System

Final project for Object Oriented Programming, NYU (26 Summer).

An app for organizing events and managing attendee registrations. You can create and edit
events, register and cancel attendees, view the attendee list, search and sort events, and see
statistics.

It has two interfaces, a command line one and a Swing GUI. Both use the same classes
underneath.

Written in plain Java with no external libraries. You only need a JDK (version 8 or newer).

## How to run it

See [SETUP.md](SETUP.md) if you want the step by step version, including how to install the
JDK.

Windows:

```
run.bat          command line version
run.bat gui      windowed version
```

Mac or Linux:

```
bash run.sh          command line version
bash run.sh gui      windowed version
```

Or compile it yourself:

```
javac -d build $(find src -name '*.java')
java -cp build app.EventRegistrationApplication --cli
```

Events are saved in `data/events.bin`. Three sample events come with the project. Delete that
file to start empty.

## Command line reference

At the event list:

| Command | What it does |
|---|---|
| `list` | show the events |
| `add` | create a new event |
| `view <id>` or `edit <id>` | open an event |
| `delete <id>` | delete an event (asks first) |
| `search <text>` | find events by title |
| `filterby <title\|description\|venue> <text>` | narrow the list |
| `clearfilter` | show everything again |
| `sortby <title\|capacity\|remaining\|start\|end> [asc\|desc]` | reorder the list |
| `clearsort` | back to normal order |
| `stats` | show statistics |
| `quit` | exit |

Inside an event: `set <field> <value>`, `show`, `stats`, `reservations`, `save`, `abort`.
The fields are title, description, venuename, venueaddress, venuedetails, start, end,
capacity. Dates look like `2026-09-14 18:30`.

Inside the attendee list: `list`, `add`, `edit <row>`, `delete <row>`, `done`, `abort`.

Type `help` on any screen to see its commands. Nothing is saved to the file until you `save`
the event, so `abort` is always safe.

## Project structure

```
src/
  model/         Event, Reservation
  repository/    EventRepository (interface), FileEventRepository, RepositoryException
  service/       EventService, EventQuery, EventFilterField, EventSortField,
                 EventStatistics, ValidationException
  ui/            EventListUI (interface), DateTimeFormats
  ui/cli/        AbstractCLI, Command, EventListCLI, EventCLI,
                 ReservationListCLI, ReservationCLI
  ui/gui/        EventListGUI, EventGUI, ReservationListGUI, ReservationGUI
  config/        ApplicationConfig
  app/           EventRegistrationApplication (has main)
data/
  events.bin     saved events
```

The GUI classes match the CLI classes one for one. Neither of them opens files or decides what
is valid, they both go through `EventService`.

## Validation rules

An event needs a title, a venue name, a start and end date with the end after the start, and a
capacity of at least 1. Capacity can never be lower than the number of people already
registered.

A reservation needs a first and last name, a valid email that is not already registered for
that event, and a phone number with at least 7 digits if you enter one.

The app checks if an event is full before opening the attendee form, so you get told straight
away instead of filling in a form that gets rejected.

## Design decisions

The project brief left the algorithm and the persistence approach up to us.

**Saving data.** We use `ObjectOutputStream` to write all the events to one file,
`data/events.bin`. `Event` and `Reservation` implement `Serializable`. This is the same
approach as the Password Keeper example from class. Every save rewrites the whole file, which
is fine for this many events.

**Sorting.** `EventService` uses an insertion sort instead of `Collections.sort`. The list is
small and usually almost sorted already, which is the case insertion sort is best at, and it
is stable so events that tie keep their original order.

**Searching.** A plain loop with a case insensitive `contains` check, so searching for "gala"
finds "Annual Gala Dinner".

## Differences from the class diagram

Two things changed while we were coding:

1. `JsonEventRepository` is now `FileEventRepository` and there is no `JsonParser`. Doing JSON
   without a library meant writing a 300 line parser, which was way more complicated than
   anything else in the project. Switching to `ObjectOutputStream` only changed this one class
   because everything else goes through the `EventRepository` interface.

2. We added an `EventStatistics` class. The brief asks for event statistics and UC-04 is "View
   Event Details and Statistics", but there was no class for it on the diagram.

We also added an `EventListUI` interface so `main` can start either the CLI or the GUI, and a
`deleteEvent` method on the repository since UC-06 needs it.
