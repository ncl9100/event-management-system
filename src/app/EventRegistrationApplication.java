package app;

import javax.swing.UIManager;

import config.ApplicationConfig;
import repository.EventRepository;
import repository.FileEventRepository;
import service.EventService;
import ui.EventListUI;
import ui.cli.EventListCLI;
import ui.gui.EventListGUI;

/**
 * The program's starting point.
 *
 * It builds the object graph once -- repository, then service, then the chosen
 * top-level UI -- and hands control to that UI. Notice that everything below it
 * is wired through the EventRepository and EventListUI abstractions, so
 * swapping the store or the front end changes only this class.
 */
public class EventRegistrationApplication {

    private EventListUI ui;
    private EventService eventService;

    public EventRegistrationApplication(ApplicationConfig config) {
        EventRepository repository = new FileEventRepository(config.getDataFilePath());
        this.eventService = new EventService(repository);
        this.ui = createUI(config, this.eventService);
    }

    /** Starts the configured user interface. */
    public void run() {
        ui.run();
    }

    public EventService getEventService() {
        return eventService;
    }

    public EventListUI getUi() {
        return ui;
    }

    private EventListUI createUI(ApplicationConfig config, EventService service) {
        if (config.getUiMode() == ApplicationConfig.UiMode.GUI) {
            useSystemLookAndFeel();
            return new EventListGUI(service);
        }
        return new EventListCLI(service);
    }

    private void useSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // A missing look and feel is not worth failing to start over.
        }
    }

    public static void main(String[] args) {
        ApplicationConfig config = ApplicationConfig.fromArgs(args);
        EventRegistrationApplication application = new EventRegistrationApplication(config);
        application.run();
    }
}
