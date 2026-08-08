package ui.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import model.Event;
import service.EventFilterField;
import service.EventQuery;
import service.EventService;
import service.EventSortField;
import service.EventStatistics;
import service.ValidationException;
import ui.DateTimeFormats;
import ui.EventListUI;

/**
 * Boundary object. The windowed home screen: the event table and everything
 * that starts from it.
 *
 * Implements UC-02, UC-03, UC-11 and UC-12 directly, launches UC-01 / UC-04 /
 * UC-05 through EventGUI, and handles UC-06 with its UC-14 confirmation.
 *
 * It owns no rules and touches no files. The table is only a display of
 * displayedEvents, which always comes back from EventService.
 */
public class EventListGUI extends JFrame implements EventListUI {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNS =
            {"ID", "Title", "Venue", "Start", "End", "Capacity", "Reserved", "Free"};

    private JPanel contentPane;
    private JTextField txtFilter;
    private JComboBox<String> cboFilterField;
    private JComboBox<String> cboSortField;
    private JComboBox<String> cboSortDirection;
    private JButton btnFilter;
    private JButton btnClearFilter;
    private JTable tblEvents;
    private DefaultTableModel eventTableModel;
    private JButton btnAdd;
    private JButton btnView;
    private JButton btnDelete;
    private JButton btnStatistics;
    private JLabel lblStatus;

    private EventService eventService;
    private EventQuery query;
    private List<Event> displayedEvents;
    private EventGUI eventGUI;

    public EventListGUI(EventService eventService) {
        super("Event Management System");
        this.eventService = eventService;
        this.query = new EventQuery();
        this.displayedEvents = new ArrayList<Event>();
        buildUI();
        // Composition: this window owns the event form it opens. Built after
        // buildUI() so this frame is a usable parent for the dialog.
        this.eventGUI = new EventGUI(this, eventService);
    }

    @Override
    public void run() {
        refresh();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ------------------------------------------------------------ building

    private void buildUI() {
        contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        contentPane.add(buildQueryPanel(), BorderLayout.NORTH);
        contentPane.add(buildTable(), BorderLayout.CENTER);
        contentPane.add(buildActionPanel(), BorderLayout.SOUTH);

        setContentPane(contentPane);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
    }

    private JPanel buildQueryPanel() {
        txtFilter = new JTextField(18);

        cboFilterField = new JComboBox<String>();
        for (EventFilterField field : EventFilterField.values()) {
            cboFilterField.addItem(field.getLabel());
        }

        cboSortField = new JComboBox<String>();
        cboSortField.addItem("(stored order)");
        for (EventSortField field : EventSortField.values()) {
            cboSortField.addItem(field.getLabel());
        }

        cboSortDirection = new JComboBox<String>();
        cboSortDirection.addItem("asc");
        cboSortDirection.addItem("desc");

        btnFilter = new JButton("Search");
        btnClearFilter = new JButton("Show all");

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Find events where"));
        panel.add(cboFilterField);
        panel.add(new JLabel("contains"));
        panel.add(txtFilter);
        panel.add(btnFilter);
        panel.add(btnClearFilter);
        panel.add(new JLabel("     Sort by"));
        panel.add(cboSortField);
        panel.add(cboSortDirection);
        return panel;
    }

    private JScrollPane buildTable() {
        eventTableModel = new DefaultTableModel(COLUMNS, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // the table displays, the form edits
            }
        };
        tblEvents = new JTable(eventTableModel);
        tblEvents.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblEvents.setRowHeight(22);

        JScrollPane scrollPane = new JScrollPane(tblEvents);
        scrollPane.setPreferredSize(new Dimension(940, 320));
        return scrollPane;
    }

    private JPanel buildActionPanel() {
        btnAdd = new JButton("Create event");
        btnView = new JButton("View / edit");
        btnDelete = new JButton("Delete");
        btnStatistics = new JButton("Statistics");
        lblStatus = new JLabel(" ");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(btnAdd);
        buttons.add(btnView);
        buttons.add(btnDelete);
        buttons.add(btnStatistics);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(buttons, BorderLayout.WEST);
        panel.add(lblStatus, BorderLayout.SOUTH);

        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == btnAdd) {
                    addEvent();
                } else if (e.getSource() == btnView) {
                    viewEvent();
                } else if (e.getSource() == btnDelete) {
                    deleteEvent();
                } else if (e.getSource() == btnStatistics) {
                    showStatistics();
                } else if (e.getSource() == btnFilter) {
                    applyFilter();
                } else if (e.getSource() == btnClearFilter) {
                    clearFilter();
                } else {
                    applySort();
                }
            }
        };
        btnAdd.addActionListener(listener);
        btnView.addActionListener(listener);
        btnDelete.addActionListener(listener);
        btnStatistics.addActionListener(listener);
        btnFilter.addActionListener(listener);
        btnClearFilter.addActionListener(listener);
        txtFilter.addActionListener(listener);
        cboSortField.addActionListener(listener);
        cboSortDirection.addActionListener(listener);
        return panel;
    }

    // ------------------------------------------------------------- actions

    /** UC-01. */
    public void addEvent() {
        Optional<Event> created = eventGUI.create();
        if (created.isPresent()) {
            try {
                int id = eventService.insertEvent(created.get());
                setStatus("Created event " + id + ".");
            } catch (ValidationException e) {
                showErrors("The event could not be saved", e.getErrors());
            }
        }
        refresh();
    }

    /** UC-04 and UC-05. */
    public void viewEvent() {
        Event selected = selectedEvent();
        if (selected == null) {
            return;
        }
        Optional<Event> edited = eventGUI.edit(selected);
        if (edited.isPresent()) {
            try {
                if (eventService.updateEvent(edited.get())) {
                    setStatus("Saved event " + edited.get().getId() + ".");
                } else {
                    setStatus("Event " + edited.get().getId() + " no longer exists.");
                }
            } catch (ValidationException e) {
                showErrors("The changes could not be saved", e.getErrors());
            }
        }
        refresh();
    }

    /** UC-06, including the UC-14 confirmation. */
    public void deleteEvent() {
        Event selected = selectedEvent();
        if (selected == null) {
            return;
        }
        String warning = selected.getReservations().isEmpty()
                ? ""
                : "\n\nThis also cancels " + selected.getReservations().size()
                        + " reservation(s).";
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete \"" + selected.getTitle() + "\"?" + warning,
                "Confirm deletion", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        if (eventService.deleteEvent(selected.getId())) {
            setStatus("Deleted event " + selected.getId() + ".");
        } else {
            setStatus("Event " + selected.getId() + " could not be deleted.");
        }
        refresh();
    }

    /** UC-03 and UC-11. */
    public void applyFilter() {
        String text = txtFilter.getText().trim();
        if (text.isEmpty()) {
            query.clearFilter();
        } else {
            query.filterField = EventFilterField.values()[cboFilterField.getSelectedIndex()];
            query.filterValue = text;
        }
        refresh();
    }

    public void clearFilter() {
        txtFilter.setText("");
        query.clearFilter();
        refresh();
    }

    /** UC-12. */
    public void applySort() {
        int selected = cboSortField.getSelectedIndex();
        if (selected <= 0) {
            query.clearSort();
        } else {
            query.sortField = EventSortField.values()[selected - 1];
            query.sortDirection = cboSortDirection.getSelectedIndex() == 1
                    ? EventQuery.DESCENDING
                    : EventQuery.ASCENDING;
        }
        refresh();
    }

    /** The statistics half of UC-04, across everything currently listed. */
    public void showStatistics() {
        EventStatistics stats = new EventStatistics(displayedEvents);
        Event busiest = stats.getBusiestEvent();
        Event next = stats.getNextUpcomingEvent();

        StringBuilder message = new StringBuilder();
        message.append("Events listed        : ").append(stats.getTotalEvents()).append('\n');
        message.append("Total reservations   : ").append(stats.getTotalReservations()).append('\n');
        message.append("Total capacity       : ").append(stats.getTotalCapacity()).append('\n');
        message.append("Seats still free     : ").append(stats.getTotalRemainingCapacity()).append('\n');
        message.append(String.format("Overall occupancy    : %.1f%%%n", stats.getOverallOccupancyRate()));
        message.append(String.format("Average occupancy    : %.1f%%%n", stats.getAverageOccupancyRate()));
        message.append("Full events          : ").append(stats.getFullEventCount()).append('\n');
        message.append("Events with nobody   : ").append(stats.getEmptyEventCount()).append('\n');
        message.append("Upcoming / now / past: ")
                .append(stats.getUpcomingEventCount()).append(" / ")
                .append(stats.getInProgressEventCount()).append(" / ")
                .append(stats.getPastEventCount()).append('\n');
        message.append("Busiest event        : ")
                .append(busiest == null ? "(none)"
                        : busiest.getTitle() + " (" + busiest.getReservations().size() + ")")
                .append('\n');
        message.append("Next event           : ")
                .append(next == null ? "(none scheduled)"
                        : next.getTitle() + " on " + DateTimeFormats.format(next.getStartDateTime()));

        JOptionPane.showMessageDialog(this, message.toString(),
                "Event statistics", JOptionPane.INFORMATION_MESSAGE);
    }

    // ------------------------------------------------------------- display

    /** UC-02: reload through the service and redraw the table. */
    private void refresh() {
        displayedEvents = eventService.getEvents(Optional.of(query));
        eventTableModel.setRowCount(0);
        for (Event e : displayedEvents) {
            eventTableModel.addRow(new Object[] {
                    Integer.valueOf(e.getId()),
                    e.getTitle(),
                    e.getVenueName(),
                    DateTimeFormats.format(e.getStartDateTime()),
                    DateTimeFormats.format(e.getEndDateTime()),
                    Integer.valueOf(e.getCapacity()),
                    Integer.valueOf(e.getReservations().size()),
                    e.isFull() ? "FULL" : String.valueOf(e.getRemainingCapacity())
            });
        }
        setStatus(displayedEvents.size() + " event(s) shown.     " + query.toString());
    }

    /** The selected row's event, or null after telling the user to pick one. */
    private Event selectedEvent() {
        int row = tblEvents.getSelectedRow();
        if (row < 0 || row >= displayedEvents.size()) {
            JOptionPane.showMessageDialog(this,
                    "Select an event in the list first.",
                    "Nothing selected", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return displayedEvents.get(row);
    }

    private void setStatus(String text) {
        lblStatus.setText(text);
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
