package repository;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import model.Event;

/**
 * Boundary object. Stores every event, with its reservations, in one file.
 *
 * This is the only class in the program that knows a file exists. It writes the
 * list of events with an ObjectOutputStream, which is why Event and Reservation
 * both implement Serializable.
 */
public class FileEventRepository implements EventRepository {

    private Path filePath;

    public FileEventRepository(Path filePath) {
        this.filePath = filePath;
    }

    public Path getFilePath() {
        return filePath;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Event> loadAll() {
        if (!Files.exists(filePath)) {
            return new ArrayList<Event>(); // nothing saved yet, so start empty
        }
        try {
            FileInputStream fileStream = new FileInputStream(filePath.toFile());
            ObjectInputStream objectStream = new ObjectInputStream(fileStream);
            List<Event> events = (List<Event>) objectStream.readObject();
            objectStream.close();
            if (events == null) {
                return new ArrayList<Event>();
            }
            return events;
        } catch (IOException e) {
            throw new RepositoryException("Could not read " + filePath + ": " + e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            throw new RepositoryException("The data in " + filePath + " is not readable by this "
                    + "version of the program: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveAll(List<Event> events) {
        try {
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            FileOutputStream fileStream = new FileOutputStream(filePath.toFile());
            ObjectOutputStream objectStream = new ObjectOutputStream(fileStream);
            objectStream.writeObject(new ArrayList<Event>(events));
            objectStream.close();
        } catch (IOException e) {
            throw new RepositoryException("Could not write " + filePath + ": " + e.getMessage(), e);
        }
    }

    @Override
    public int insertEvent(Event e) {
        List<Event> events = loadAll();
        int newId = nextId(events);
        Event stored = e.deepCopy();
        stored.setId(newId);
        events.add(stored);
        saveAll(events);
        return newId;
    }

    @Override
    public boolean updateEvent(Event e) {
        List<Event> events = loadAll();
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getId() == e.getId()) {
                events.set(i, e.deepCopy());
                saveAll(events);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean deleteEvent(int id) {
        List<Event> events = loadAll();
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getId() == id) {
                events.remove(i);
                saveAll(events);
                return true;
            }
        }
        return false;
    }

    /** Ids are handed out here: one higher than the highest already stored. */
    private int nextId(List<Event> events) {
        int highest = 0;
        for (Event e : events) {
            if (e.getId() > highest) {
                highest = e.getId();
            }
        }
        return highest + 1;
    }
}
