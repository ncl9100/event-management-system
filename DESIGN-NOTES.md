# Design notes

How the code relates to the diagrams: what matches the class diagram exactly, what was added
and why, which class implements which use case, and where the SOLID principles show up.

---

## 1. Class diagram fidelity

Every class, attribute and method on the class diagram exists in the code with the same name,
the same visibility, the same parameters and the same return type.

| Class diagram | Code | Status |
|---|---|---|
| `Event` — id, title, description, venueName, venueAddress, venueDetails, startDateTime, endDateTime, capacity, reservations; `isFull()`, `getRemainingCapacity()`, `deepCopy()` | `model/Event.java` | exact |
| `Reservation` — firstName, lastName, email, phone, dateRegistered; `deepCopy()` | `model/Reservation.java` | exact |
| `«interface» EventRepository` — `loadAll()`, `saveAll()`, `insertEvent()`, `updateEvent()` | `repository/EventRepository.java` | exact + `deleteEvent()` |
| `JsonEventRepository` — filePath, jsonParser | `repository/FileEventRepository.java` | **renamed** — see section 2 |
| `EventService` — repository, events; `getEvents(Optional<EventQuery>)`, `insertEvent()`, `updateEvent()`, `updateEvents()`, static `isValidEvent()`, `isValidReservation()`, `canAddReservation()` | `service/EventService.java` | exact + additions below |
| `EventQuery` — filterField, filterValue, sortField, sortDirection; `clearFilter()`, `clearSort()` | `service/EventQuery.java` | exact (fields are public, as the diagram's `+` shows) |
| `«enumeration» EventFilterField` — TITLE, DESCRIPTION, VENUE | `service/EventFilterField.java` | exact |
| `«enumeration» EventSortField` — TITLE, CAPACITY, REMAINING_CAPACITY, START_DATE, END_DATE | `service/EventSortField.java` | exact |
| `AbstractCLI` — private scanner, protected eventService; protected `promptAndParse(String)` | `ui/cli/AbstractCLI.java` | exact |
| `Command` — name, args | `ui/cli/Command.java` | exact |
| `EventListCLI` — query, displayedEvents; `run()` | `ui/cli/EventListCLI.java` | exact |
| `EventCLI` — event; `create()`, `edit(Event)` | `ui/cli/EventCLI.java` | exact |
| `ReservationListCLI` — event; `run(Event)` | `ui/cli/ReservationListCLI.java` | exact |
| `ReservationCLI` — event; `create(Event)`, `edit(Event, Reservation)` | `ui/cli/ReservationCLI.java` | exact |
| `«application» main` — ui, eventService; `run()` | `app/EventRegistrationApplication.java` | exact |

Constructors and getters/setters are not listed on the class diagram, and are not counted as
deviations — every class has them.

### Relationships, and the code that implements them

| Diagram | Notation | In code |
|---|---|---|
| `Event` ◆— `Reservation` (1 to 0..\*) | composition | `List<Reservation> reservations` inside `Event`; `Event.deepCopy()` copies the reservations too, so no reservation exists outside an event |
| `JsonEventRepository` ⊳ `EventRepository` | realization (dashed) | `FileEventRepository implements EventRepository` |
| `EventService` —— `EventRepository` (1) | association | field `private EventRepository repository` |
| `EventService` ⇢ `EventQuery` | dependency (dashed) | `EventQuery` appears only as a method parameter, never as a field |
| `EventService` ⇢ `EventFilterField` / `EventSortField` | dependency | used inside methods only |
| `AbstractCLI` ⇢ `Command` | dependency | `promptAndParse` returns a locally created `Command` |
| `EventListCLI`, `EventCLI`, `ReservationListCLI`, `ReservationCLI` —▷ `AbstractCLI` | generalization | `extends AbstractCLI` |
| `EventListCLI` ◆— `EventCLI` ◆— `ReservationListCLI` ◆— `ReservationCLI` | composition (1 each) | each screen holds its child screen as a field, created in its own constructor — the child is owned by the parent and cannot outlive it |
| `main` ◆— `EventListCLI` (1), `main` —— `EventService` | composition / association | fields on `EventRegistrationApplication`, built in its constructor |

The GUI classes repeat the same chain: `EventListGUI` ◆— `EventGUI` ◆— `ReservationListGUI`
◆— `ReservationGUI`.

---

## 2. What was added, and why

These are all additions except the last one, which is a rename. If the diagram is being
resubmitted alongside the code, these are the boxes worth changing on it.

| Addition | Reason |
|---|---|
| **`service/EventStatistics`** | The project brief lists "Display event statistics" as a required feature and UC-04 is *View Event Details **and Statistics***, but no statistics class appears on the class diagram. It is named in `class-overview.txt`. It is a pure calculator over a list of events — it stores nothing and validates nothing, so `EventService` keeps its single responsibility. |
| **`ui/EventListUI` (interface)** | The diagram gives `main` an attribute typed `EventListCLI`. That would hard-wire the application to one front end. `EventListUI` declares the single `run()` method, and `EventListCLI` and `EventListGUI` both implement it, so `EventRegistrationApplication` depends on the abstraction instead of a concrete screen. |
| **`ui/gui/` — `EventListGUI`, `EventGUI`, `ReservationListGUI`, `ReservationGUI`** | The windowed front end. One GUI class per CLI class, same responsibilities, same method names (`create`, `edit`, `run`, `save`, `abort`). |
| **`EventRepository.deleteEvent(int)` and `EventService.deleteEvent(int)`** | UC-06 (Delete Event) cannot be implemented without it. `deleteEvent` is listed under the repository in `class-overview.txt`, so this looks like an omission from the diagram rather than a design decision. |
| **`EventService.validateEvent()` / `validateReservation()`** (static, return `List<String>`) | The diagram's `isValidEvent` / `isValidReservation` return a plain `boolean`, which cannot tell a user *what* is wrong. These return every broken rule; the boolean methods on the diagram are kept and simply return `errors.isEmpty()`. One set of rules, two ways to ask about them. |
| **`EventService.getEventById(int)`, `getEvents()`** | Looking one event up by the id shown in the table, and a no-argument convenience overload. |
| **`JsonEventRepository` renamed to `FileEventRepository`, `JsonParser` removed** | See "Why the storage format changed" immediately below. |
| **`ValidationException`, `RepositoryException`** | `ValidationException` is named in `class-overview.txt`. `RepositoryException` reports an unreadable or unwritable data file. |
| **`config/ApplicationConfig`** | Named in `class-overview.txt`. Holds the two startup choices — which front end, which data file — so neither is hard-coded anywhere else. |
| **`ui/DateTimeFormats`** | One definition of `yyyy-MM-dd HH:mm`, shared by both front ends so they cannot drift apart. |

### Why the storage format changed

The diagram names the store `JsonEventRepository` with a `jsonParser : JsonParser` attribute,
and `class-overview.txt` proposed a JSON file. Since the project uses no external libraries,
JSON meant hand-writing a parser — which came to 303 lines of recursive descent and wildcard
generics, easily the most complicated code in the project and nothing like anything in the
course.

The store now uses `ObjectOutputStream` and `ObjectInputStream` against `data/events.bin`,
which is the exact pattern from the Password Keeper example in Lecture 6. `Event` and
`Reservation` implement `Serializable`; the repository is about 100 lines and contains no
parsing at all. The trade-off is that the data file is no longer human-readable.

**Nothing else in the project moved.** The `EventRepository` interface is unchanged, and it
was the only thing the service layer ever depended on — which is exactly the swap that
interface was put there to allow. The one line that names a concrete store is in
`EventRegistrationApplication`'s constructor.

If the class diagram is being resubmitted, the `JsonEventRepository` box should be renamed
`FileEventRepository`, its `jsonParser` attribute dropped, and the `JsonParser` reference
removed.

### One behavioural decision worth flagging

`Event.isFull()` returns `false` when `capacity` is 0. A brand-new event with no capacity set
is not "full" — it simply cannot take registrations yet, which
`EventService.canAddReservation()` reports separately with a clearer message. Without this, a
blank new event displays as "FULL", which reads as a bug.

---

## 3. Use case → code map

All 17 use cases from the use case diagram are implemented in both front ends.

| Use case | CLI | GUI |
|---|---|---|
| UC-01 Create Event | `EventListCLI.handleAdd()` → `EventCLI.create()` | `EventListGUI.addEvent()` → `EventGUI.create()` |
| UC-02 Browse Events | `EventListCLI.refresh()` / `showEvents()` | `EventListGUI.refresh()` |
| UC-03 Search for Event | `EventListCLI.handleSearch()` | `EventListGUI.applyFilter()` |
| UC-04 View Event Details and Statistics | `EventCLI.showEvent()`, `showStatistics()` | `EventGUI.showEvent()`, `showStatistics()` |
| UC-05 Edit Event | `EventCLI.edit()` | `EventGUI.edit()` |
| UC-06 Delete Event | `EventListCLI.handleDelete()` | `EventListGUI.deleteEvent()` |
| UC-07 View Event Reservations | `ReservationListCLI.run()` | `ReservationListGUI.run()` |
| UC-08 Add Reservation | `ReservationListCLI.handleAdd()` → `ReservationCLI.create()` | `ReservationListGUI.addReservation()` → `ReservationGUI.create()` |
| UC-09 Edit Reservation | `ReservationListCLI.handleEdit()` → `ReservationCLI.edit()` | `ReservationListGUI.editReservation()` → `ReservationGUI.edit()` |
| UC-10 Delete Reservation | `ReservationListCLI.handleDelete()` | `ReservationListGUI.deleteReservation()` |
| UC-11 Filter Events | `EventListCLI.handleFilterBy()` | `EventListGUI.applyFilter()` |
| UC-12 Sort Events | `EventListCLI.handleSortBy()` | `EventListGUI.applySort()` |
| UC-13 Verify Event Information *(included by UC-01, UC-05)* | `EventCLI.trySave()` → `EventService.validateEvent()` | `EventGUI.save()` → `EventService.validateEvent()` |
| UC-14 Confirm Event Deletion *(included by UC-06)* | `AbstractCLI.confirm()` | `JOptionPane.showConfirmDialog` |
| UC-15 Verify Remaining Capacity *(included by UC-08)* | `EventService.canAddReservation()` | `EventService.canAddReservation()` |
| UC-16 Validate Attendee Information *(included by UC-09)* | `ReservationCLI.trySave()` → `EventService.validateReservation()` | `ReservationGUI.save()` → `EventService.validateReservation()` |
| UC-17 Validate Attendee Deletion *(included by UC-10)* | `ReservationListCLI.handleDelete()` confirmation | `ReservationListGUI.deleteReservation()` confirmation |

Note that UC-13 to UC-17 — the five *«include»* use cases — are all implemented by calling
`EventService`. They are included by both the CLI and the GUI paths, which is exactly the
"always happens" meaning of *«include»*: there is no way to save an event or a reservation
that skips them.

---

## 4. Where the message flow comes from

The sequence for creating an event, as implemented:

```
User → EventListCLI : add
       EventListCLI → EventCLI      : create()
                      EventCLI → (user sets fields)
                      EventCLI → EventService    : validateEvent(e)      [UC-13]
                      EventCLI ⇠ returns Optional<Event>
       EventListCLI → EventService  : insertEvent(e)
                      EventService → EventService  : validateEvent(e)    [self-message]
                      EventService → EventRepository : insertEvent(e)
                                     FileEventRepository → (writes events.bin)
                      EventService ⇠ returns int id
       EventListCLI → EventService  : getEvents(query)
       EventListCLI → EventListCLI  : showEvents()                       [self-message]
```

Registering an attendee follows the same shape one level deeper: `EventCLI` →
`ReservationListCLI` → `ReservationCLI`, with `EventService.canAddReservation()` checked
before the form opens and `EventService.validateReservation()` checked before it returns.

The nesting is deliberate. Each screen edits a **deep copy** and returns a value only when the
user saves, so abandoning any screen at any depth leaves everything above it untouched, and
nothing reaches the JSON file until the outermost save.

---

## 5. SOLID

**S — Single Responsibility.** Each class has one identity. `Event` and `Reservation` hold
data. `JsonEventRepository` is the only class that knows a file exists. `EventService` decides
what is valid. `EventStatistics` only does arithmetic. The UI classes only talk to the user.
No GUI class opens a file or decides what a valid email is — that was the specific trap called
out in the Password Keeper example, and both `EventGUI` and `ReservationGUI` call
`EventService` rather than checking anything themselves.

**O — Open/Closed.** Adding a filter or sort option means adding a constant to
`EventFilterField` or `EventSortField` and one branch in the private helper that reads it — the
UIs build their dropdowns by looping over `values()`, so they pick up the new option with no
edit at all. Adding a statistic means one method on `EventStatistics`. Adding a whole new store
means one new class implementing `EventRepository`; nothing above it changes.

**L — Liskov Substitution.** `AbstractCLI` declares only what every screen genuinely needs:
input, parsing, printing, confirmation. There is no method a subclass has to override with an
empty body. Any `EventListUI` can be given to `EventRegistrationApplication` and it works.

**I — Interface Segregation.** `EventListUI` has exactly one method, `run()` — the only thing
the application needs from a top-level screen. `EventRepository` has the five storage
operations and nothing else; it carries no validation or query methods, so a future
implementation is not forced to supply things it has no business supplying.

**D — Dependency Inversion.** `EventService` holds an `EventRepository`, not a
`JsonEventRepository`. `EventRegistrationApplication` holds an `EventListUI`, not an
`EventListCLI`. Both concrete types are chosen in one place —
`EventRegistrationApplication`'s constructor, from `ApplicationConfig` — and nowhere else in
the program names them.

**Patterns used.** *Repository* behind an interface for persistence; *Template Method* in
`AbstractCLI`, where the shared prompt/parse/print mechanics live in the parent and each
screen supplies only the commands it understands; *Prototype* in `deepCopy()`, where each
object copies itself rather than the caller reconstructing it field by field.

---

## 6. Staying inside the course material

The code deliberately avoids anything the lectures did not cover. There are no lambdas, no
method references and no streams anywhere in the project. Specifically:

| Instead of | The code uses |
|---|---|
| a JSON parser | `ObjectOutputStream` / `Serializable`, as in the Password Keeper example |
| `Collections.sort` with a `Comparator` | a hand-written insertion sort in `EventService`, with the choice of algorithm explained in a comment — the brief says the choice of algorithm is the team's to make |
| `GridBagLayout` | `BoxLayout` stacking rows that each use `BorderLayout` |
| `SwingUtilities.invokeLater` | `main` simply constructs the application and calls `run()` |
| `HashSet` for duplicate detection | a nested loop over the reservation list |
| `String.join`, `Collections.unmodifiableList`, `Integer.compare` | a `StringBuilder` loop, a copied `ArrayList`, and a plain `if` comparison |

Three things are kept that go slightly past the lectures, all for the same reason — they are
printed on the class diagram, and the rubric scores implementing the diagram exactly:
`Optional<Event>` as a return type, `LocalDateTime` for the event dates, and `Path` for the
data file location.

One more is kept on merit: `Duration.between(start, end).toMinutes()` in `EventStatistics`.
Computing the gap between two dates by hand would be longer and easier to get wrong, and
`Duration` comes from the same `java.time` package as the `LocalDateTime` the diagram already
requires.

---

## 7. How this was tested

- Compiles with `javac -Xlint:all -Werror` with **zero errors and zero warnings**.
- The CLI was driven through scripted sessions covering all 17 use cases: creating events,
  registering and editing and cancelling attendees, hitting the capacity limit, duplicate
  emails, an end date before the start date, blank required fields, a non-existent event id,
  confirming and declining deletion, search, filter by all three fields, sort ascending and
  descending, and statistics.
- The Swing front end was run against a virtual display and driven programmatically: the event
  table loads, the event form opens with the right data, the reservation list opens from it,
  the attendee form opens from that, and an invalid email raises the warning dialog instead of
  saving.
- Storage was round-trip tested: events and reservations written with `ObjectOutputStream`
  and read back come out identical, including dates and text with accented and non-Latin
  characters. Reading a file that does not exist yet returns an empty list rather than
  failing.
- All five sort fields were checked in both directions against the seeded data, since the
  sort is hand-written rather than a library call.
- `deepCopy()` was tested to confirm that changing a copy leaves the original — including its
  reservations — untouched.

### Numbers worth knowing

Statistics reported for the three seeded events (2 + 1 + 0 reservations, capacities 4 + 25 +
40) are: 3 reservations against 69 seats, so overall occupancy is 4.3%, while average
occupancy — the mean of each event's own rate, 50% + 4% + 0% — is 18.0%. The two figures
differ on purpose; both are shown because a small full event and a large empty one tell
different stories.
