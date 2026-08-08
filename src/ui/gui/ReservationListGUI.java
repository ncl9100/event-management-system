package ui.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import model.Event;
import model.Reservation;
import service.EventService;
import ui.DateTimeFormats;

/**
 * Boundary object. The windowed attendee list for one event.
 *
 * Implements UC-07 and hosts UC-08 / UC-09 / UC-10, including UC-15 (capacity
 * check before adding) and UC-17 (confirm before cancelling).
 *
 * Like its command-line twin it works on a deep copy and only hands it back
 * when the user clicks Done.
 */
public class ReservationListGUI extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNS =
            {"#", "Name", "Email", "Phone", "Registered"};

    private JPanel contentPane;
    private JLabel lblSummary;
    private JTable tblReservations;
    private DefaultTableModel reservationTableModel;
    private JButton btnAdd;
    private JButton btnEdit;
    private JButton btnDelete;
    private JButton btnDone;
    private JButton btnAbort;

    private EventService eventService;
    private Event event;
    private boolean kept;
    private ReservationGUI reservationGUI;

    public ReservationListGUI(Window owner, EventService eventService) {
        super(owner, "Reservations", ModalityType.APPLICATION_MODAL);
        this.eventService = eventService;
        this.kept = false;
        buildUI();
        // Composition: this list owns the attendee form it opens.
        this.reservationGUI = new ReservationGUI(this, eventService);
    }

    /**
     * Shows the attendee list for this event.
     *
     * @return the event with its updated reservations, or an empty Optional
     *         when the user discarded the changes
     */
    public Optional<Event> run(Event event) {
        this.event = event.deepCopy();
        this.kept = false;
        setTitle("Reservations - " + this.event.getTitle());
        showReservations();
        setLocationRelativeTo(getOwner());
        setVisible(true); // blocks until the dialog closes
        if (kept) {
            return Optional.of(this.event);
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------ building

    private void buildUI() {
        contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        lblSummary = new JLabel(" ");

        reservationTableModel = new DefaultTableModel(COLUMNS, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // the table displays, the form edits
            }
        };
        tblReservations = new JTable(reservationTableModel);
        tblReservations.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblReservations.setRowHeight(22);

        JScrollPane scrollPane = new JScrollPane(tblReservations);
        scrollPane.setPreferredSize(new Dimension(720, 260));

        btnAdd = new JButton("Register attendee");
        btnEdit = new JButton("Edit");
        btnDelete = new JButton("Cancel registration");
        btnDone = new JButton("Done");
        btnAbort = new JButton("Discard changes");

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        left.add(btnAdd);
        left.add(btnEdit);
        left.add(btnDelete);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.add(btnAbort);
        right.add(btnDone);

        JPanel buttons = new JPanel(new BorderLayout());
        buttons.add(left, BorderLayout.WEST);
        buttons.add(right, BorderLayout.EAST);

        contentPane.add(lblSummary, BorderLayout.NORTH);
        contentPane.add(scrollPane, BorderLayout.CENTER);
        contentPane.add(buttons, BorderLayout.SOUTH);

        setContentPane(contentPane);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();

        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == btnAdd) {
                    addReservation();
                } else if (e.getSource() == btnEdit) {
                    editReservation();
                } else if (e.getSource() == btnDelete) {
                    deleteReservation();
                } else if (e.getSource() == btnDone) {
                    keepChanges();
                } else if (e.getSource() == btnAbort) {
                    discardChanges();
                }
            }
        };
        btnAdd.addActionListener(listener);
        btnEdit.addActionListener(listener);
        btnDelete.addActionListener(listener);
        btnDone.addActionListener(listener);
        btnAbort.addActionListener(listener);
    }

    // ------------------------------------------------------------- actions

    /** UC-08, including the UC-15 capacity check. */
    public void addReservation() {
        if (!EventService.canAddReservation(event)) {
            String reason = event.getCapacity() <= 0
                    ? "This event has no capacity set, so nobody can be registered yet."
                    : "This event is full (" + event.getReservations().size()
                            + " of " + event.getCapacity() + " seats taken).";
            JOptionPane.showMessageDialog(this, reason, "No room",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Optional<Reservation> created = reservationGUI.create(event);
        if (created.isPresent()) {
            event.getReservations().add(created.get());
        }
        showReservations();
    }

    /** UC-09. */
    public void editReservation() {
        int index = selectedIndex();
        if (index < 0) {
            return;
        }
        Reservation original = event.getReservations().get(index);
        Optional<Reservation> edited = reservationGUI.edit(event, original);
        if (edited.isPresent()) {
            event.getReservations().set(index, edited.get());
        }
        showReservations();
    }

    /** UC-10, including the UC-17 confirmation. */
    public void deleteReservation() {
        int index = selectedIndex();
        if (index < 0) {
            return;
        }
        Reservation target = event.getReservations().get(index);
        int answer = JOptionPane.showConfirmDialog(this,
                "Cancel the reservation for " + target.getFullName() + "?",
                "Confirm cancellation", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        event.getReservations().remove(index);
        showReservations();
    }

    public void keepChanges() {
        kept = true;
        dispose();
    }

    public void discardChanges() {
        kept = false;
        dispose();
    }

    // ------------------------------------------------------------- display

    private void showReservations() {
        reservationTableModel.setRowCount(0);
        List<Reservation> reservations = event.getReservations();
        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);
            reservationTableModel.addRow(new Object[] {
                    Integer.valueOf(i + 1),
                    r.getFullName(),
                    r.getEmail(),
                    r.getPhone(),
                    DateTimeFormats.format(r.getDateRegistered())
            });
        }
        lblSummary.setText(event.getTitle()
                + "     " + reservations.size() + " of " + event.getCapacity()
                + " seats taken     " + event.getRemainingCapacity() + " free");
    }

    /** The selected row as a list index, or -1 after telling the user to pick one. */
    private int selectedIndex() {
        int row = tblReservations.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Select an attendee in the list first.",
                    "Nothing selected", JOptionPane.INFORMATION_MESSAGE);
            return -1;
        }
        return row;
    }
}
