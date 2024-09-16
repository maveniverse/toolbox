package eu.maveniverse.maven.toolbox.plugin;

import static org.jline.jansi.Ansi.Attribute.INTENSITY_BOLD;
import static org.jline.jansi.Ansi.Attribute.INTENSITY_BOLD_OFF;
import static org.jline.jansi.Ansi.Attribute.ITALIC;
import static org.jline.jansi.Ansi.Attribute.ITALIC_OFF;
import static org.jline.jansi.Ansi.ansi;

import eu.maveniverse.maven.toolbox.shared.internal.DependencyGraphDumper;
import eu.maveniverse.maven.toolbox.shared.output.Marker;
import eu.maveniverse.maven.toolbox.shared.output.Output;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.artifact.JavaScopes;
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
            return klazz.cast(new JLine3TreeFormatter(this));
        }
        return supplier.get();
    }

    @Override
    public Marker marker(Verbosity verbosity) {
        return new JLine3Marker(this, verbosity);
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
        private final Output output;
        private final Marker marker;

        public JLine3TreeFormatter(Output output) {
            this.output = output;
            this.marker = output.marker(Verbosity.NORMAL);
        }

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
                    + (loser
                            ? loser(nodeStr)
                            : (isProvided(node)
                                    ? provided(nodeStr)
                                    : (isModded(node) ? modified(nodeStr) : winner(nodeStr))));
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

        private boolean isProvided(DependencyNode node) {
            return JavaScopes.PROVIDED.equals(node.getDependency().getScope());
        }

        private String winner(String string) {
            return marker.outstanding(string).toString();
        }

        private String provided(String string) {
            return marker.unimportant(string).toString();
        }

        private String modified(String string) {
            return marker.scary(string).toString();
        }

        private String loser(String string) {
            return marker.detail(string).toString();
        }
    }

    // MessageBuilder

    private static class JLine3Marker extends Marker {
        public JLine3Marker(Output output, Verbosity verbosity) {
            super(output, verbosity);
        }

        @Override
        public Marker emphasize(String word) {
            return super.emphasize(
                    ansi().bold().fgBright(Ansi.Color.WHITE).a(word).reset().toString());
        }

        @Override
        public Marker outstanding(String word) {
            return super.outstanding(
                    ansi().fgBright(Ansi.Color.GREEN).a(word).reset().toString());
        }

        @Override
        public Marker detail(String word) {
            return super.detail(
                    ansi().fgBright(Ansi.Color.YELLOW).a(word).reset().toString());
        }

        @Override
        public Marker unimportant(String word) {
            return super.unimportant(
                    ansi().fgBright(Ansi.Color.CYAN).a(word).reset().toString());
        }

        @Override
        public Marker scary(String word) {
            return super.scary(ansi().fgBright(Ansi.Color.RED).a(word).reset().toString());
        }
    }
}
