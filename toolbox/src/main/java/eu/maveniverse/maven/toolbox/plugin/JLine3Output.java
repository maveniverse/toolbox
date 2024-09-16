package eu.maveniverse.maven.toolbox.plugin;

import static org.jline.jansi.Ansi.Attribute.INTENSITY_BOLD;
import static org.jline.jansi.Ansi.Attribute.INTENSITY_BOLD_OFF;
import static org.jline.jansi.Ansi.Attribute.ITALIC;
import static org.jline.jansi.Ansi.Attribute.ITALIC_OFF;
import static org.jline.jansi.Ansi.ansi;

import eu.maveniverse.maven.toolbox.shared.Output;
import java.io.IOException;
import java.io.PrintWriter;
import org.jline.terminal.Terminal;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

public class JLine3Output implements Output {
    private final Verbosity verbosity;
    private final Terminal terminal;

    public JLine3Output(Verbosity verbosity, Terminal terminal) {
        this.verbosity = verbosity;
        this.terminal = terminal;
    }

    public void close() throws IOException {
        terminal.flush();
        terminal.close();
    }

    @Override
    public Verbosity getVerbosity() {
        return verbosity;
    }

    @Override
    public void handle(Verbosity verbosity, String message, Object... params) {
        FormattingTuple tuple = MessageFormatter.arrayFormat(message, params);
        terminal.writer().println(tuple.getMessage());
        if (tuple.getThrowable() != null) {
            writeThrowable(tuple.getThrowable(), terminal.writer());
        }
    }

    private void writeThrowable(Throwable t, PrintWriter stream) {
        if (t == null) {
            return;
        }
        String builder = failure(t.getClass().getName());
        if (t.getMessage() != null) {
            builder += ": " + failure(t.getMessage());
        }
        stream.println(builder);

        if (true) {
            printStackTrace(t, stream, "");
        }
        stream.println(ansi().reset());
    }

    private void printStackTrace(Throwable t, PrintWriter stream, String prefix) {
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement e : t.getStackTrace()) {
            builder.append(prefix);
            builder.append("    ");
            builder.append(strong("at"));
            builder.append(" ");
            builder.append(e.getClassName());
            builder.append(".");
            builder.append(e.getMethodName());
            builder.append(" (");
            builder.append(strong(getLocation(e)));
            builder.append(")");
            stream.println(builder);
            builder.setLength(0);
        }
        for (Throwable se : t.getSuppressed()) {
            writeThrowable(se, stream, "Suppressed", prefix + "    ");
        }
        Throwable cause = t.getCause();
        if (cause != null && t != cause) {
            writeThrowable(cause, stream, "Caused by", prefix);
        }
    }

    private void writeThrowable(Throwable t, PrintWriter stream, String caption, String prefix) {
        StringBuilder builder = new StringBuilder();
        builder.append(prefix)
                .append(strong(caption))
                .append(": ")
                .append(t.getClass().getName());
        if (t.getMessage() != null) {
            builder.append(": ").append(failure(t.getMessage()));
        }
        stream.println(builder);

        printStackTrace(t, stream, prefix);
    }

    private String getLocation(final StackTraceElement e) {
        if (e.isNativeMethod()) {
            return "Native Method";
        } else if (e.getFileName() == null) {
            return "Unknown Source";
        } else if (e.getLineNumber() >= 0) {
            return e.getFileName() + ":" + e.getLineNumber();
        } else {
            return e.getFileName();
        }
    }

    private static String failure(String format) {
        return ansi().a(ITALIC).a(format).a(ITALIC_OFF).toString();
    }

    private static String strong(String format) {
        return ansi().a(INTENSITY_BOLD).a(format).a(INTENSITY_BOLD_OFF).toString();
    }
}
