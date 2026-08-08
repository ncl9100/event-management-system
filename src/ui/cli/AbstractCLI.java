package ui.cli;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

import service.EventService;
import ui.DateTimeFormats;

/**
 * Everything the four command-line screens share: reading a line of input,
 * turning it into a Command, printing consistently, and asking for confirmation.
 *
 * This is the Template Method idea applied to a menu loop -- the shared
 * mechanics live here, each subclass supplies only the commands it understands.
 *
 * All four screens deliberately share ONE Scanner. Opening a second Scanner on
 * System.in would let it buffer ahead and swallow input meant for the screen
 * that opened it.
 */
public abstract class AbstractCLI {

    /** How dates are typed in and printed back out. Shared with the windowed UI. */
    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormats.DISPLAY;

    private static final Scanner SHARED_INPUT = new Scanner(System.in);

    private Scanner scanner;
    protected EventService eventService;

    protected AbstractCLI(EventService eventService) {
        this.eventService = eventService;
        this.scanner = SHARED_INPUT;
    }

    /**
     * Prints the prompt, reads one line and parses it.
     * Returns the "quit" command when input runs out, so a piped session ends
     * cleanly instead of looping forever.
     */
    protected Command promptAndParse(String s) {
        System.out.print(s);
        System.out.flush();
        if (!scanner.hasNextLine()) {
            System.out.println();
            return Command.parse("quit");
        }
        return Command.parse(scanner.nextLine());
    }

    /** Yes/no question. Anything other than y / yes counts as no. */
    protected boolean confirm(String question) {
        Command answer = promptAndParse(question + " (y/n): ");
        return answer.name.equals("y") || answer.name.equals("yes");
    }

    // ------------------------------------------------------------- printing

    protected void println(String text) {
        System.out.println(text);
    }

    protected void blankLine() {
        System.out.println();
    }

    protected void printHeading(String text) {
        System.out.println();
        System.out.println(text);
        System.out.println(repeat('=', text.length()));
    }

    protected void printError(String text) {
        System.out.println("  ! " + text);
    }

    protected void printErrors(List<String> errors) {
        for (String error : errors) {
            printError(error);
        }
    }

    protected void printInfo(String text) {
        System.out.println("  " + text);
    }

    protected void printUnknownCommand(String name) {
        printError("Unknown command \"" + name + "\". Type help to see the options.");
    }

    // -------------------------------------------------------------- parsing

    /** Parses "yyyy-MM-dd HH:mm". Returns null when the text is not a valid date. */
    protected LocalDateTime parseDateTime(String text) {
        return DateTimeFormats.parse(text);
    }

    /** Parses an integer. Returns the fallback when the text is not a number. */
    protected int parseInt(String text, int fallback) {
        if (text == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ------------------------------------------------------------ formatting

    protected static String formatDateTime(LocalDateTime value) {
        return DateTimeFormats.format(value);
    }

    /** Pads or truncates so table columns line up. */
    protected static String pad(String text, int width) {
        String safe = (text == null) ? "" : text;
        if (safe.length() > width) {
            if (width <= 1) {
                return safe.substring(0, width);
            }
            return safe.substring(0, width - 1) + "…";
        }
        StringBuilder builder = new StringBuilder(safe);
        while (builder.length() < width) {
            builder.append(' ');
        }
        return builder.toString();
    }

    protected static String repeat(char c, int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            builder.append(c);
        }
        return builder.toString();
    }
}
