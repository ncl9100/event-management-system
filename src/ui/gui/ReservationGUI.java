package ui.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import model.Event;
import model.Reservation;
import service.EventService;
import ui.DateTimeFormats;

/**
 * Boundary object. The windowed form for one attendee.
 *
 * Same job as ReservationCLI, same rules -- it asks EventService whether the
 * details are acceptable rather than deciding for itself, so the two front ends
 * cannot disagree about what a valid attendee is.
 *
 * Implements the data entry for UC-08 / UC-09 and includes UC-16 on save.
 */
public class ReservationGUI extends JDialog {

    private static final long serialVersionUID = 1L;

    private JPanel contentPane;
    private JLabel lblEvent;
    private JLabel lblRegistered;
    private JTextField txtFirstName;
    private JTextField txtLastName;
    private JTextField txtEmail;
    private JTextField txtPhone;
    private JButton btnSave;
    private JButton btnAbort;

    private EventService eventService;
    private Event event;
    private Reservation reservation;
    private int editingIndex;
    private boolean saved;

    public ReservationGUI(Window owner, EventService eventService) {
        super(owner, "Reservation", ModalityType.APPLICATION_MODAL);
        this.eventService = eventService;
        this.editingIndex = -1;
        this.saved = false;
        buildUI();
    }

    /**
     * Opens the form for a brand new attendee.
     *
     * @return the reservation, or an empty Optional when the user cancelled
     */
    public Optional<Reservation> create(Event event) {
        this.event = event;
        this.editingIndex = -1;
        this.reservation = new Reservation();
        this.reservation.setDateRegistered(LocalDateTime.now());
        setTitle("New reservation");
        return showDialog();
    }

    /**
     * Opens the form for an existing attendee.
     *
     * @return the changed reservation, or an empty Optional when cancelled
     */
    public Optional<Reservation> edit(Event event, Reservation r) {
        this.event = event;
        this.editingIndex = indexOf(event, r);
        this.reservation = r.deepCopy();
        setTitle("Edit reservation");
        return showDialog();
    }

    private Optional<Reservation> showDialog() {
        this.saved = false;
        showReservation();
        setLocationRelativeTo(getOwner());
        setVisible(true); // blocks until the dialog closes
        if (saved) {
            return Optional.of(reservation);
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------ building

    private void buildUI() {
        contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        lblEvent = new JLabel(" ");
        lblRegistered = new JLabel(" ");

        txtFirstName = new JTextField(20);
        txtLastName = new JTextField(20);
        txtEmail = new JTextField(20);
        txtPhone = new JTextField(20);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("First name"));
        form.add(txtFirstName);
        form.add(new JLabel("Last name"));
        form.add(txtLastName);
        form.add(new JLabel("Email"));
        form.add(txtEmail);
        form.add(new JLabel("Phone"));
        form.add(txtPhone);
        form.add(new JLabel("Registered"));
        form.add(lblRegistered);

        btnSave = new JButton("Save");
        btnAbort = new JButton("Cancel");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnAbort);
        buttons.add(btnSave);

        contentPane.add(lblEvent, BorderLayout.NORTH);
        contentPane.add(form, BorderLayout.CENTER);
        contentPane.add(buttons, BorderLayout.SOUTH);

        setContentPane(contentPane);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();

        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == btnSave) {
                    save();
                } else if (e.getSource() == btnAbort) {
                    abort();
                }
            }
        };
        btnSave.addActionListener(listener);
        btnAbort.addActionListener(listener);
        getRootPane().setDefaultButton(btnSave);
    }

    // ------------------------------------------------------------- actions

    /** Reads the form, checks it with EventService, and closes only if it passes. */
    public void save() {
        readForm();
        // UC-16: Validate Attendee Information
        List<String> errors = EventService.validateReservation(reservation);
        if (!errors.isEmpty()) {
            showErrors("This reservation cannot be saved yet", errors);
            return;
        }
        if (isDuplicateEmail()) {
            JOptionPane.showMessageDialog(this,
                    reservation.getEmail() + " is already registered for this event.",
                    "Already registered", JOptionPane.WARNING_MESSAGE);
            return;
        }
        saved = true;
        dispose();
    }

    /** Closes without keeping anything. */
    public void abort() {
        saved = false;
        dispose();
    }

    private void readForm() {
        reservation.setFirstName(txtFirstName.getText().trim());
        reservation.setLastName(txtLastName.getText().trim());
        reservation.setEmail(txtEmail.getText().trim());
        reservation.setPhone(txtPhone.getText().trim());
        if (reservation.getDateRegistered() == null) {
            reservation.setDateRegistered(LocalDateTime.now());
        }
    }

    private void showReservation() {
        lblEvent.setText(event.getTitle() + "   -   "
                + event.getRemainingCapacity() + " of " + event.getCapacity() + " seats free");
        txtFirstName.setText(reservation.getFirstName());
        txtLastName.setText(reservation.getLastName());
        txtEmail.setText(reservation.getEmail());
        txtPhone.setText(reservation.getPhone());
        lblRegistered.setText(DateTimeFormats.format(reservation.getDateRegistered()));
    }

    private boolean isDuplicateEmail() {
        String email = reservation.getEmail() == null
                ? "" : reservation.getEmail().trim().toLowerCase();
        if (email.isEmpty()) {
            return false;
        }
        List<Reservation> existing = event.getReservations();
        for (int i = 0; i < existing.size(); i++) {
            if (i == editingIndex) {
                continue;
            }
            String other = existing.get(i).getEmail();
            String normalised = other == null ? "" : other.trim().toLowerCase();
            if (normalised.equals(email)) {
                return true;
            }
        }
        return false;
    }

    private int indexOf(Event event, Reservation r) {
        List<Reservation> existing = event.getReservations();
        for (int i = 0; i < existing.size(); i++) {
            if (existing.get(i) == r) {
                return i;
            }
        }
        return -1;
    }

    private void showErrors(String heading, List<String> errors) {
        StringBuilder message = new StringBuilder(heading + ":\n\n");
        for (String error : errors) {
            message.append("  • ").append(error).append('\n');
        }
        JOptionPane.showMessageDialog(this, message.toString(),
                "Please check the details", JOptionPane.WARNING_MESSAGE);
    }
}
