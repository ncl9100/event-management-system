package ui.cli;

import java.util.ArrayList;
import java.util.List;

/**
 * One line of user input, already split into a command name and its arguments.
 *
 * Entity object: it carries the parsed input around, it does not act on it.
 */
public class Command {

    public String name;
    public List<String> args;

    public Command(String name, List<String> args) {
        this.name = name;
        this.args = args;
    }

    /** Parses a raw input line. An empty line yields a command with an empty name. */
    public static Command parse(String line) {
        List<String> args = new ArrayList<String>();
        if (line == null) {
            return new Command("", args);
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return new Command("", args);
        }
        String[] parts = trimmed.split("\\s+");
        String name = parts[0].toLowerCase();
        for (int i = 1; i < parts.length; i++) {
            args.add(parts[i]);
        }
        return new Command(name, args);
    }

    public int argCount() {
        return args.size();
    }

    /** The argument at this position, or null when there are fewer arguments. */
    public String arg(int index) {
        if (index < 0 || index >= args.size()) {
            return null;
        }
        return args.get(index);
    }

    /**
     * Every argument from this position onward, joined with single spaces.
     * This is what lets "set title Annual Gala Dinner" keep its spaces.
     */
    public String argsFrom(int index) {
        StringBuilder builder = new StringBuilder();
        for (int i = index; i < args.size(); i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args.get(i));
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return name + " " + argsFrom(0);
    }
}
