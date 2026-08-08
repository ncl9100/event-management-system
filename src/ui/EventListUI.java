package ui;

/**
 * The top-level screen of the application.
 *
 * EventRegistrationApplication depends on this interface rather than on
 * EventListCLI or EventListGUI directly, which is what lets the same
 * application object start either front end (Dependency Inversion).
 *
 * It has exactly one method on purpose -- starting the UI is the only thing the
 * application needs from it (Interface Segregation).
 */
public interface EventListUI {

    /** Shows the screen and hands control to the user. */
    void run();
}
