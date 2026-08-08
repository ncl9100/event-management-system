package ui;

import javax.swing.UIManager;

import config.ApplicationConfig;
import service.EventService;
import ui.cli.EventListCLI;
import ui.gui.EventListGUI;

/**
 * Simple Factory for creating the application's top-level user interface.
 *
 * Keeping UI construction here removes concrete CLI/GUI creation from the
 * application startup class. The caller only asks for an EventListUI based on
 * the configured mode, while this factory decides which concrete class to
 * instantiate.
 */
public final class UIFactory {

    private UIFactory() {
        // Utility/factory class; do not instantiate.
    }

    /**
     * Creates the top-level UI for the requested mode.
     *
     * @param mode the configured UI mode
     * @param eventService the service shared by the selected UI
     * @return either the GUI or CLI implementation behind EventListUI
     */
    public static EventListUI createUI(ApplicationConfig.UiMode mode,
                                       EventService eventService) {
        if (mode == ApplicationConfig.UiMode.GUI) {
            useSystemLookAndFeel();
            return new EventListGUI(eventService);
        }
        return new EventListCLI(eventService);
    }

    /** Uses the operating system's look and feel when the GUI is selected. */
    private static void useSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // A missing look and feel should not prevent the application starting.
        }
    }
}
