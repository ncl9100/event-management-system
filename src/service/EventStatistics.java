package service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import model.Event;

/**
 * Control object. Computes the figures shown on the statistics screens.
 *
 * Deliberately separate from EventService: EventService decides what is valid
 * and what is stored; this class only does arithmetic over events it is handed.
 * Adding a new statistic touches nothing else in the program.
 *
 * Construct it with the events you want summarised -- pass one event to get
 * that event's numbers, or the whole list for a system-wide summary.
 */
public class EventStatistics {

    private final List<Event> events;
    private final LocalDateTime now;

    public EventStatistics(List<Event> events) {
        this(events, LocalDateTime.now());
    }

    /** Overload that fixes "now", so the numbers are reproducible in tests. */
    public EventStatistics(List<Event> events, LocalDateTime now) {
        this.events = events;
        this.now = now;
    }

    public int getTotalEvents() {
        return events.size();
    }

    public int getTotalReservations() {
        int total = 0;
        for (Event e : events) {
            total = total + e.getReservations().size();
        }
        return total;
    }

    public int getTotalCapacity() {
        int total = 0;
        for (Event e : events) {
            total = total + e.getCapacity();
        }
        return total;
    }

    public int getTotalRemainingCapacity() {
        int total = 0;
        for (Event e : events) {
            total = total + e.getRemainingCapacity();
        }
        return total;
    }

    /** Reservations as a percentage of capacity, across every event. 0 when there is no capacity. */
    public double getOverallOccupancyRate() {
        int capacity = getTotalCapacity();
        if (capacity <= 0) {
            return 0.0;
        }
        return (getTotalReservations() * 100.0) / capacity;
    }

    /** The mean of each event's own occupancy rate. 0 when there are no events. */
    public double getAverageOccupancyRate() {
        if (events.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (Event e : events) {
            sum = sum + occupancyRateOf(e);
        }
        return sum / events.size();
    }

    public int getFullEventCount() {
        int count = 0;
        for (Event e : events) {
            if (e.isFull()) {
                count++;
            }
        }
        return count;
    }

    public int getEmptyEventCount() {
        int count = 0;
        for (Event e : events) {
            if (e.getReservations().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public int getUpcomingEventCount() {
        int count = 0;
        for (Event e : events) {
            if (e.getStartDateTime() != null && e.getStartDateTime().isAfter(now)) {
                count++;
            }
        }
        return count;
    }

    public int getInProgressEventCount() {
        int count = 0;
        for (Event e : events) {
            if (e.getStartDateTime() != null && e.getEndDateTime() != null
                    && !e.getStartDateTime().isAfter(now) && e.getEndDateTime().isAfter(now)) {
                count++;
            }
        }
        return count;
    }

    public int getPastEventCount() {
        int count = 0;
        for (Event e : events) {
            if (e.getEndDateTime() != null && !e.getEndDateTime().isAfter(now)) {
                count++;
            }
        }
        return count;
    }

    /** The event with the most reservations, or null when there are no events. */
    public Event getBusiestEvent() {
        Event busiest = null;
        for (Event e : events) {
            if (busiest == null || e.getReservations().size() > busiest.getReservations().size()) {
                busiest = e;
            }
        }
        return busiest;
    }

    /** The soonest event that has not started yet, or null when there is none. */
    public Event getNextUpcomingEvent() {
        Event next = null;
        for (Event e : events) {
            if (e.getStartDateTime() == null || !e.getStartDateTime().isAfter(now)) {
                continue;
            }
            if (next == null || e.getStartDateTime().isBefore(next.getStartDateTime())) {
                next = e;
            }
        }
        return next;
    }

    // ------------------------------------------------- single-event figures

    /** One event's reservations as a percentage of its capacity. */
    public static double occupancyRateOf(Event e) {
        if (e == null || e.getCapacity() <= 0) {
            return 0.0;
        }
        return (e.getReservations().size() * 100.0) / e.getCapacity();
    }

    /** How long the event runs for, in minutes. 0 when either date is missing. */
    public static long durationInMinutesOf(Event e) {
        if (e == null || e.getStartDateTime() == null || e.getEndDateTime() == null) {
            return 0;
        }
        return Duration.between(e.getStartDateTime(), e.getEndDateTime()).toMinutes();
    }

    /** A readable "starts in 3 days, 4 hours" / "already finished" line for one event. */
    public static String describeTimeUntilStart(Event e) {
        return describeTimeUntilStart(e, LocalDateTime.now());
    }

    public static String describeTimeUntilStart(Event e, LocalDateTime now) {
        if (e == null || e.getStartDateTime() == null) {
            return "no start date set";
        }
        if (e.getEndDateTime() != null && !e.getEndDateTime().isAfter(now)) {
            return "already finished";
        }
        if (!e.getStartDateTime().isAfter(now)) {
            return "in progress";
        }
        Duration until = Duration.between(now, e.getStartDateTime());
        long days = until.toDays();
        long hours = until.toHours() % 24;
        long minutes = until.toMinutes() % 60;
        if (days > 0) {
            return "starts in " + days + "d " + hours + "h";
        }
        if (hours > 0) {
            return "starts in " + hours + "h " + minutes + "m";
        }
        return "starts in " + minutes + "m";
    }
}
