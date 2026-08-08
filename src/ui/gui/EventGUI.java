package ui.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import model.Event;
import service.EventService;
import service.EventStatistics;
import ui.DateTimeFormats;

/**
 * Boundary object. The windowed detail form for one event.
 *
 * Implements UC-01, UC-04 and UC-05, includes UC-13 on save, and is the way
 * into UC-07 through the Manage reservations button.
 *
 * It edits a deep copy and returns it only on save.
 */
public class EventGUI extends JDialog {

    private static final long serialVersionUID = 1L;

    private JPanel contentPane;
    private JTextField txtTitle;
    private JTextArea txtDescription;
    private JTextField txtVenueName;
    private JTextField txtVenueAddress;
    private JTextField txtVenueDetails;
    private JTextField txtStart;
    private JTextField txtEnd;
    private JTextField txtCapacity;
    private JLabel lblReservations;
    private JLabel lblStatistics;
    private JButton btnReservations;
    private JButton btnSave;
    private JButton btnAbort;

    /** Width of the caption column, so every field starts at the same place. */
    private static final int LABEL_WIDTH = 215;

    private EventService eventService;
    private Event event;
    private boolean saved;
    private ReservationListGUI reservationListGUI;

    public EventGUI(Window owner, EventService eventService) {
        super(owner, "Event", ModalityType.APPLICATION_MODAL);
        this.eventService = eventService;
        this.saved = false;
        buildUI();
        // Composition: this form owns the reservation list it opens.
        this.reservationListGUI = new ReservationListGUI(this, eventService);
    }

    /**
     * Opens the form for a brand new event.
     *
     * @return the event to insert, or an empty Optional when cancelled
     */
    public Optional<Event> create() {
        this.event = new Event();
        setTitle("New event");
        return showDialog();
    }

    /**
     * Opens the form for an existing event.
     *
     * @return the changed event to save, or an empty Optional when cancelled
     */
    public Optional<Event> edit(Event event) {
        this.event = event.deepCopy();
        setTitle("Event " + event.getId() + " - " + event.getTitle());
        return showDialog();
    }

    private Optional<Event> showDialog() {
        this.saved = false;
        showEvent();
        setLocationRelativeTo(getOwner());
        setVisible(true); // blocks until the dialog closes
        if (saved) {
            return Optional.of(event);
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------ building

    private void buildUI() {
        contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        txtTitle = new JTextField(28);
        txtDescription = new JTextArea(4, 28);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        txtVenueName = new JTextField(28);
        txtVenueAddress = new JTextField(28);
        txtVenueDetails = new JTextField(28);
        txtStart = new JTextField(28);
        txtEnd = new JTextField(28);
        txtCapacity = new JTextField(28);
        lblReservations = new JLabel(" ");
        lblStatistics = new JLabel(" ");

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        addRow(form, "Title", txtTitle);
        addRow(form, "Description", new JScrollPane(txtDescription));
        addRow(form, "Venue name", txtVenueName);
        addRow(form, "Venue address", txtVenueAddress);
        addRow(form, "Venue details", txtVenueDetails);
        addRow(form, "Starts (" + DateTimeFormats.HINT + ")", txtStart);
        addRow(form, "Ends (" + DateTimeFormats.HINT + ")", txtEnd);
        addRow(form, "Capacity", txtCapacity);
        addRow(form, "Reservations", lblReservations);

        btnReservations = new JButton("Manage reservations…");
        btnSave = new JButton("Save");
        btnAbort = new JButton("Cancel");

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        left.add(btnReservations);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.add(btnAbort);
        right.add(btnSave);

        JPanel buttons = new JPanel(new BorderLayout());
        buttons.add(left, BorderLayout.WEST);
        buttons.add(right, BorderLayout.EAST);

        JPanel south = new JPanel(new BorderLayout());
        south.add(lblStatistics, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);

        contentPane.add(form, BorderLayout.CENTER);
        contentPane.add(south, BorderLayout.SOUTH);

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
                } else if (e.getSource() == btnReservations) {
                    manageReservations();
                }
            }
        };
        btnSave.addActionListener(listener);
        btnAbort.addActionListener(listener);
        btnReservations.addActionListener(listener);
        getRootPane().setDefaultButton(btnSave);
    }

    /**
     * Adds one "caption : field" row to the form.
     *
     * Each row is its own panel with a BorderLayout -- caption on the left at a
     * fixed width so the captions line up, field filling the rest -- and the
     * rows are stacked by the form's BoxLayout. Capping each row's maximum
     * height at its preferred height is what stops BoxLayout stretching a
     * one-line text box to fill the window.
     */
    private void addRow(JPanel form, String label, Component field) {
        JLabel caption = new JLabel(label);
        caption.setPreferredSize(new Dimension(LABEL_WIDTH, caption.getPreferredSize().height));

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        row.add(caption, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));

        form.add(row);
    }

    // ------------------------------------------------------------- actions

    /** Reads the form, checks it with EventService, and closes only if it passes. */
    public void save() {
        List<String> errors = readForm();
        if (errors.isEmpty()) {
            // UC-13: Verify Event Information
            errors = EventService.validateEvent(event);
        }
        if (!errors.isEmpty()) {
            showErrors(errors);
            return;
        }
        saved = true;
        dispose();
    }

    public void abort() {
        saved = false;
        dispose();
    }

    /** UC-07: hand the copy to the reservation list, adopt whatever comes back. */
    public void manageReservations() {
        readForm(); // so the capacity typed in is what the reservation list sees
        Optional<Event> updated = reservationListGUI.run(event);
        if (updated.isPresent()) {
            event.setReservations(updated.get().getReservations());
        }
        showEvent();
    }

    /**
     * Copies the text boxes into the event.
     *
     * @return the problems that stopped a field being read at all (a date that
     *         is not a date, a capacity that is not a number). Rules about the
     *         values themselves are EventService's job, not this form's.
     */
    private List<String> readForm() {
        List<String> errors = new ArrayList<String>();

        event.setTitle(txtTitle.getText().trim());
        event.setDescription(txtDescription.getText().trim());
        event.setVenueName(txtVenueName.getText().trim());
        event.setVenueAddress(txtVenueAddress.getText().trim());
        event.setVenueDetails(txtVenueDetails.getText().trim());

        String startText = txtStart.getText().trim();
        LocalDateTime start = DateTimeFormats.parse(startText);
        if (!startText.isEmpty() && start == null) {
            errors.add("Start \"" + startText + "\" is not a date. Use " + DateTimeFormats.HINT + ".");
        } else {
            event.setStartDateTime(start);
        }

        String endText = txtEnd.getText().trim();
        LocalDateTime end = DateTimeFormats.parse(endText);
        if (!endText.isEmpty() && end == null) {
            errors.add("End \"" + endText + "\" is not a date. Use " + DateTimeFormats.HINT + ".");
        } else {
            event.setEndDateTime(end);
        }

        String capacityText = txtCapacity.getText().trim();
        if (capacityText.isEmpty()) {
            event.setCapacity(0);
        } else {
            try {
                event.setCapacity(Integer.parseInt(capacityText));
            } catch (NumberFormatException e) {
                errors.add("Capacity \"" + capacityText + "\" is not a whole number.");
            }
        }
        return errors;
    }

    // ------------------------------------------------------------- display

    private void showEvent() {
        txtTitle.setText(event.getTitle());
        txtDescription.setText(event.getDescription());
        txtVenueName.setText(event.getVenueName());
        txtVenueAddress.setText(event.getVenueAddress());
        txtVenueDetails.setText(event.getVenueDetails());
        txtStart.setText(DateTimeFormats.formatForInput(event.getStartDateTime()));
        txtEnd.setText(DateTimeFormats.formatForInput(event.getEndDateTime()));
        txtCapacity.setText(String.valueOf(event.getCapacity()));
        lblReservations.setText(event.getReservations().size()
                + "   (" + event.getRemainingCapacity() + " seats free"
                + (event.isFull() ? ", FULL" : "") + ")");
        showStatistics();
    }

    /** UC-04: the statistics half of "View Event Details and Statistics". */
    private void showStatistics() {
        long minutes = EventStatistics.durationInMinutesOf(event);
        lblStatistics.setText(String.format(
                "Occupancy %.1f%%     runs for %dh %dm     %s",
                EventStatistics.occupancyRateOf(event),
                minutes / 60, minutes % 60,
                EventStatistics.describeTimeUntilStart(event)));
    }

    private void showErrors(List<String> errors) {
        StringBuilder message = new StringBuilder("This event cannot be saved yet:\n\n");
        for (String error : errors) {
            message.append("  • ").append(error).append('\n');
        }
        JOptionPane.showMessageDialog(this, message.toString(),
                "Please check the details", JOptionPane.WARNING_MESSAGE);
    }
}
