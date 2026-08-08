# Event Management System

Final project — Object Oriented Programming, NYU (26 Summer).

An application for organizing events and managing attendee registrations. An organizer can
create and edit events, register and cancel attendees, view the attendee list, search and
sort events, and see statistics.

Written in plain Java with **no external libraries** — it compiles with `javac` and runs with
`java`. There is nothing to install beyond a JDK.

Everything in it uses material from the course: classes and objects, inheritance, an abstract
class, interfaces, static members, `ArrayList`, exceptions, `Scanner`, and Swing with
`ActionListener`. There are no lambdas, no streams, and no library calls the lectures did not
cover.

---

## Running it

**New to this? Read [SETUP.md](SETUP.md)** — step-by-step instructions for Windows, macOS and
Linux, written for someone who has not used GitHub or a terminal before.

**Windows**

```
run.bat          starts the command line interface
run.bat gui      starts the windowed (Swing) interface
```

**macOS / Linux**

```
./run.sh         starts the command line interface
./run.sh gui     starts the windowed (Swing) interface
```

**By hand**

```
javac -d build $(find src -name '*.java')
java -cp build app.EventRegistrationApplication --cli
java -cp build app.EventRegistrationApplication --gui
```

Requires JDK 8 or newer. The source is pure ASCII, so no `-encoding` flag is needed whatever
your machine's default character set is.

Optional argument: `--data <path>` to use a different data file
(default `data/events.bin`).

Events are stored in `data/events.bin`, written with `ObjectOutputStream`. A few sample events
ship with the project so there is something on screen the first time you start it. Delete that
file to begin from empty.

---

## The command line interface

The home screen lists the events. From there:

| Command | What it does |
|---|---|
| `list` | show the events |
| `add` | create a new event |
| `view <id>` / `edit <id>` | open an event |
| `delete <id>` | delete an event (asks first) |
| `search <text>` | find events by title |
| `filterby <title\|description\|venue> <text>` | narrow the list |
| `clearfilter` | show every event again |
| `sortby <title\|capacity\|remaining\|start\|end> [asc\|desc]` | reorder the list |
| `clearsort` | back to stored order |
| `stats` | statistics for the listed events |
| `quit` | leave |

Inside an event: `set <field> <value>`, `show`, `stats`, `reservations`, `save`, `abort`.
Fields are `title`, `description`, `venuename`, `venueaddress`, `venuedetails`, `start`,
`end`, `capacity`. Dates are typed as `2026-09-14 18:30`.

Inside the attendee list: `list`, `add`, `edit <row>`, `delete <row>`, `done`, `abort`.

Nothing is written to disk until you `save` the event, so `abort` at any level is always safe.

---

## The windowed interface

The same four screens as the CLI, built with Swing:

- **EventListGUI** — the event table, with a search box, a sort control, and
  Create / View-edit / Delete / Statistics buttons.
- **EventGUI** — the detail form for one event, with a Manage reservations button.
- **ReservationListGUI** — the attendee table for that event.
- **ReservationGUI** — the form for one attendee.

Both front ends run on exactly the same service, validation rules and storage. Neither
GUI class touches a file or decides what is valid — they ask `EventService`.

---

## How the code is laid out

```
src/
  model/         Event, Reservation                      entity objects
  repository/    EventRepository (interface)             storage abstraction
                 FileEventRepository                     the file store
                 RepositoryException
  service/       EventService                            validation + queries
                 EventQuery, EventFilterField, EventSortField
                 EventStatistics
                 ValidationException
  ui/            EventListUI (interface), DateTimeFormats
  ui/cli/        AbstractCLI, Command,                   command line screens
                 EventListCLI, EventCLI,
                 ReservationListCLI, ReservationCLI
  ui/gui/        EventListGUI, EventGUI,                 Swing screens
                 ReservationListGUI, ReservationGUI
  config/        ApplicationConfig                       startup choices
  app/           EventRegistrationApplication            main()
data/
  events.bin                                             the stored events
```

`DESIGN-NOTES.md` explains how this maps back to the class diagram, which use case each
class implements, and where the SOLID principles show up.

---

## Validation rules

**An event needs**

- a title (1–120 characters)
- a venue name
- a start and an end date, with the end after the start
- a capacity of at least 1, never lower than the number of people already registered

**A reservation needs**

- a first name and a last name
- a valid email address, not already registered for that event
- a phone number containing at least 7 digits, if one is given at all
- a registration date

Capacity is checked before the attendee form opens, so you are told the event is full
rather than filling in a form that will be rejected.
