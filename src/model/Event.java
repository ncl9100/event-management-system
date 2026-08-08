package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity object. Represents one event that attendees can register for.
 *
 * Event COMPOSES its reservations: no Reservation exists outside of an Event,
 * which is why deepCopy() also copies every reservation.
 *
 * Serializable so the repository can write it straight to a file with an
 * ObjectOutputStream.
 */
public class Event implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String title;
    private String description;
    private String venueName;
    private String venueAddress;
    private String venueDetails;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private int capacity;
    private List<Reservation> reservations;

    public Event() {
        this.id = 0;
        this.title = "";
        this.description = "";
        this.venueName = "";
        this.venueAddress = "";
        this.venueDetails = "";
        this.startDateTime = null;
        this.endDateTime = null;
        this.capacity = 0;
        this.reservations = new ArrayList<Reservation>();
    }

    /** Copy constructor -- copies the reservations too, not just the list reference. */
    public Event(Event other) {
        this.id = other.id;
        this.title = other.title;
        this.description = other.description;
        this.venueName = other.venueName;
        this.venueAddress = other.venueAddress;
        this.venueDetails = other.venueDetails;
        this.startDateTime = other.startDateTime;
        this.endDateTime = other.endDateTime;
        this.capacity = other.capacity;
        this.reservations = new ArrayList<Reservation>();
        for (Reservation r : other.reservations) {
            this.reservations.add(r.deepCopy());
        }
    }

    /**
     * True when the event has no room left for another attendee.
     *
     * An event whose capacity has not been set yet is not "full" -- it simply
     * cannot take registrations, which EventService.canAddReservation reports
     * separately and with a clearer message.
     */
    public boolean isFull() {
        return capacity > 0 && reservations.size() >= capacity;
    }

    /** Seats still available. Never negative. */
    public int getRemainingCapacity() {
        int remaining = capacity - reservations.size();
        if (remaining < 0) {
            return 0;
        }
        return remaining;
    }

    /** Returns an independent copy of this event and all of its reservations. */
    public Event deepCopy() {
        return new Event(this);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }

    public String getVenueAddress() { return venueAddress; }
    public void setVenueAddress(String venueAddress) { this.venueAddress = venueAddress; }

    public String getVenueDetails() { return venueDetails; }
    public void setVenueDetails(String venueDetails) { this.venueDetails = venueDetails; }

    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }

    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public List<Reservation> getReservations() { return reservations; }
    public void setReservations(List<Reservation> reservations) {
        if (reservations == null) {
            this.reservations = new ArrayList<Reservation>();
        } else {
            this.reservations = reservations;
        }
    }

    @Override
    public String toString() {
        return "[" + id + "] " + title;
    }
}
