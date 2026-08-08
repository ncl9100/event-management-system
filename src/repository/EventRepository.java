package repository;

import java.util.List;

import model.Event;

/**
 * Abstraction over wherever event data physically lives.
 *
 * The service layer depends on this interface and never on a concrete store, so
 * a different backing store (a database, a web service) can be dropped in
 * without touching anything above it. That is the Dependency Inversion
 * Principle, and it is the reason this type exists at all.
 */
public interface EventRepository {

    /** Reads every stored event. Returns an empty list when nothing is stored yet. */
    List<Event> loadAll();

    /** Overwrites the whole store with the supplied events. */
    void saveAll(List<Event> events);

    /** Stores a new event and returns the id assigned to it. */
    int insertEvent(Event e);

    /** Replaces the stored event that has the same id. Returns false if not found. */
    boolean updateEvent(Event e);

    /** Removes the stored event with this id. Returns false if not found. */
    boolean deleteEvent(int id);
}
