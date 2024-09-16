package eu.maveniverse.maven.toolbox.plugin;

import static org.jline.jansi.Ansi.Attribute.INTENSITY_BOLD;
import static org.jline.jansi.Ansi.Attribute.INTENSITY_BOLD_OFF;
import static org.jline.jansi.Ansi.Attribute.ITALIC;
import static org.jline.jansi.Ansi.Attribute.ITALIC_OFF;
import static org.jline.jansi.Ansi.ansi;

import eu.maveniverse.maven.toolbox.shared.Output;
import eu.maveniverse.maven.toolbox.shared.internal.DependencyGraphDumper;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.eclipse.aether.graph.DependencyNode;
import org.jline.jansi.Ansi;
import org.jline.terminal.Terminal;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

public class JLine3Output implements Output {
    private final Terminal terminal;
    private final Verbosity verbosity;
    private final boolean errors;

    public JLine3Output(Terminal terminal, Verbosity verbosity, boolean errors) {
        this.terminal = terminal;
        this.verbosity = verbosity;
        this.errors = errors;
    }

    public void close() throws IOException {
        terminal.flush();
        terminal.close();
    }

    @Override
    public <T> T tool(Class<T> klazz, Supplier<T> supplier) {
        if (DependencyGraphDumper.LineFormatter.class.equals(klazz)) {
            return klazz.cast(new JLine3TreeFormatter());
        }
        return supplier.get();
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
        String builder = bold(t.getClass().getName());
        if (t.getMessage() != null) {
            builder += ": " + italic(t.getMessage());
        }
        stream.println(builder);

        if (errors) {
            printStackTrace(t, stream, "");
        }
        stream.println(ansi().reset());
    }

    private void printStackTrace(Throwable t, PrintWriter stream, String prefix) {
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement e : t.getStackTrace()) {
            builder.append(prefix);
            builder.append("    ");
            builder.append(bold("at"));
            builder.append(" ");
            builder.append(e.getClassName());
            builder.append(".");
            builder.append(e.getMethodName());
            builder.append(" (");
            builder.append(bold(getLocation(e)));
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
                .append(bold(caption))
                .append(": ")
                .append(t.getClass().getName());
        if (t.getMessage() != null) {
            builder.append(": ").append(italic(t.getMessage()));
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

    private static String italic(String format) {
        return ansi().a(ITALIC).a(format).a(ITALIC_OFF).toString();
    }

    private static String bold(String format) {
        return ansi().a(INTENSITY_BOLD).a(format).a(INTENSITY_BOLD_OFF).toString();
    }

    // Tree

    private static class JLine3TreeFormatter extends DependencyGraphDumper.PlainLineFormatter {
        private final Function<DependencyNode, String> winner = DependencyGraphDumper.winnerNode();
        private final Function<DependencyNode, Boolean> premanaged = DependencyGraphDumper.isPremanaged();

        @Override
        public String formatLine(Deque<DependencyNode> nodes, List<Function<DependencyNode, String>> decorators) {
            if (nodes.size() == 1) {
                return renderRoot(nodes, decorators);
            } else if (nodes.peek().getChildren().isEmpty()) {
                return renderLeaf(nodes, decorators);
            } else {
                return renderBranch(nodes, decorators);
            }
        }

        private String renderBranch(Deque<DependencyNode> nodes, List<Function<DependencyNode, String>> decorators) {
            DependencyNode node = nodes.peek();
            boolean loser = isLoser(node);
            String nodeStr = formatNode(nodes, decorators);
            return formatIndentation(nodes, "╰─", "├─", "  ", "│ ")
                    + (loser ? loser(nodeStr) : (isModded(node) ? modified(nodeStr) : winner(nodeStr)));
        }

        private String renderLeaf(Deque<DependencyNode> nodes, List<Function<DependencyNode, String>> decorators) {
            return renderBranch(nodes, decorators);
        }

        private String renderRoot(Deque<DependencyNode> nodes, List<Function<DependencyNode, String>> decorators) {
            return "\uD83C\uDF33" + winner(formatNode(nodes, decorators));
        }

        private boolean isLoser(DependencyNode node) {
            return winner.apply(node) != null;
        }

        private boolean isModded(DependencyNode node) {
            return premanaged.apply(node);
        }

        private String winner(String string) {
            return ansi().fgBright(Ansi.Color.GREEN).a(string).reset().toString();
        }

        private String modified(String string) {
            return ansi().fgBright(Ansi.Color.YELLOW).a(string).reset().toString();
        }

        private String loser(String string) {
            return ansi().fg(Ansi.Color.YELLOW).a(string).reset().toString();
        }
    }
}
