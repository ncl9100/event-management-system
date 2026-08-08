package app;

import config.ApplicationConfig;
import repository.EventRepository;
import repository.FileEventRepository;
import service.EventService;
import ui.EventListUI;
import ui.UIFactory;

/**
 * The program's starting point.
 *
 * It builds the object graph once -- repository, then service, then the chosen
 * top-level UI -- and hands control to that UI. Notice that everything below it
 * is wired through the EventRepository and EventListUI abstractions, so
 * repository and UI construction are delegated behind abstractions/factories,
 * keeping startup focused on wiring the application together.
 */
public class EventRegistrationApplication {

    private EventListUI ui;
    private EventService eventService;

    public EventRegistrationApplication(ApplicationConfig config) {
        EventRepository repository = new FileEventRepository(config.getDataFilePath());
        this.eventService = new EventService(repository);
        this.ui = UIFactory.createUI(config.getUiMode(), this.eventService);
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

    public static void main(String[] args) {
        ApplicationConfig config = ApplicationConfig.fromArgs(args);
        EventRegistrationApplication application = new EventRegistrationApplication(config);
        application.run();
    }
}
