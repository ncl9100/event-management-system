package model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity object. Represents one person attending an Event.
 *
 * Class diagram note: a Reservation lives *inside* an Event (composition), so it
 * deliberately has no id field of its own -- it is identified by its position in
 * the owning event's reservation list.
 *
 * Serializable because it is written to the file inside its Event.
 */
public class Reservation implements Serializable {

    private static final long serialVersionUID = 1L;

    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDateTime dateRegistered;

    public Reservation() {
        this.firstName = "";
        this.lastName = "";
        this.email = "";
        this.phone = "";
        this.dateRegistered = LocalDateTime.now();
    }

    public Reservation(String firstName, String lastName, String email, String phone,
                       LocalDateTime dateRegistered) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.dateRegistered = dateRegistered;
    }

    /**
     * Copy constructor. Used by deepCopy() so a UI can edit a throw-away copy and
     * only commit the change if the user saves.
     */
    public Reservation(Reservation other) {
        this.firstName = other.firstName;
        this.lastName = other.lastName;
        this.email = other.email;
        this.phone = other.phone;
        this.dateRegistered = other.dateRegistered;
    }

    /** Returns an independent copy of this reservation. */
    public Reservation deepCopy() {
        return new Reservation(this);
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDateTime getDateRegistered() { return dateRegistered; }
    public void setDateRegistered(LocalDateTime dateRegistered) { this.dateRegistered = dateRegistered; }

    public String getFullName() {
        return (firstName + " " + lastName).trim();
    }

    @Override
    public String toString() {
        return getFullName() + " <" + email + ">";
    }
}
