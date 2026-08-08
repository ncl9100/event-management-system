package config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The choices that are made once, when the program starts: which front end to
 * show and where the data lives.
 *
 * Isolating this here is what stops "which UI" and "which file" from being
 * hard-coded inside the application, the service or a UI class.
 */
public class ApplicationConfig {

    /** The available front ends. */
    public enum UiMode {
        CLI,
        GUI
    }

    private static final String DEFAULT_DATA_FILE = "data/events.bin";

    private UiMode uiMode;
    private Path dataFilePath;

    public ApplicationConfig() {
        this.uiMode = UiMode.CLI;
        this.dataFilePath = Paths.get(DEFAULT_DATA_FILE);
    }

    /**
     * Builds the configuration from the command line.
     *
     * Accepted arguments:
     *   --cli | --gui        which front end to start (default: CLI)
     *   --data &lt;path&gt;  where to keep the data file
     */
    public static ApplicationConfig fromArgs(String[] args) {
        ApplicationConfig config = new ApplicationConfig();
        if (args == null) {
            return config;
        }
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equalsIgnoreCase("--gui")) {
                config.uiMode = UiMode.GUI;
            } else if (arg.equalsIgnoreCase("--cli")) {
                config.uiMode = UiMode.CLI;
            } else if (arg.equalsIgnoreCase("--data") && i + 1 < args.length) {
                config.dataFilePath = Paths.get(args[i + 1]);
                i++;
            }
        }
        return config;
    }

    public UiMode getUiMode() {
        return uiMode;
    }

    public void setUiMode(UiMode uiMode) {
        this.uiMode = uiMode;
    }

    public Path getDataFilePath() {
        return dataFilePath;
    }

    public void setDataFilePath(Path dataFilePath) {
        this.dataFilePath = dataFilePath;
    }

    @Override
    public String toString() {
        return "ui=" + uiMode + ", data=" + dataFilePath.toAbsolutePath();
    }
}
