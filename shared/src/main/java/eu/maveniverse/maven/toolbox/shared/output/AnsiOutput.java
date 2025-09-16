/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.output;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.internal.DependencyGraphDecorators;
import eu.maveniverse.maven.toolbox.shared.internal.DependencyGraphDumper;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.jline.jansi.Ansi;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * ANSI output that wraps another {@link Output} to decorate it.
 */
public class AnsiOutput extends OutputSupport {
    private final Output output;

    public AnsiOutput(Output output) {
        super(output.getVerbosity(), output.isShowErrors());
        this.output = output;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T tool(Class<? extends T> klazz, Supplier<T> supplier) {
        if (DependencyGraphDumper.LineFormatter.class.isAssignableFrom(klazz)) {
            if (DependencyGraphDecorators.TreeDecorator.class.equals(klazz)) {
                return (T) new AnsiTreeFormatter(marker(Verbosity.NORMAL), new TreeIntentMapper());
            } else if (DependencyGraphDecorators.DmTreeDecorator.class.equals(klazz)) {
                return (T) new AnsiTreeFormatter(marker(Verbosity.NORMAL), new DmTreeIntentMapper());
            } else if (DependencyGraphDecorators.ParentChildTreeDecorator.class.equals(klazz)) {
                return (T) new AnsiTreeFormatter(marker(Verbosity.NORMAL), new SourceTreeIntentMapper());
            } else if (DependencyGraphDecorators.SubprojectTreeDecorator.class.equals(klazz)) {
                return (T) new AnsiTreeFormatter(marker(Verbosity.NORMAL), new SourceTreeIntentMapper());
            } else if (DependencyGraphDecorators.ProjectDependenciesTreeDecorator.class.equals(klazz)) {
                return (T) new AnsiTreeFormatter(marker(Verbosity.NORMAL), new SourceTreeIntentMapper());
            }
        }
        return supplier.get();
    }

    @Override
    public Marker marker(Verbosity verbosity) {
        return new AnsiMarker(this, verbosity);
    }

    @Override
    protected void doHandle(Verbosity verbosity, String message, Object... params) {
        FormattingTuple tuple = MessageFormatter.arrayFormat(message, params);
        output.handle(verbosity, tuple.getMessage());
        if (tuple.getThrowable() != null) {
            writeThrowable(tuple.getThrowable(), output, verbosity);
        }
    }

    private void writeThrowable(Throwable t, Output output, Verbosity verbosity) {
        if (t == null) {
            return;
        }
        String builder = bold(t.getClass().getName());
        if (t.getMessage() != null) {
            builder += ": " + italic(t.getMessage());
        }
        output.handle(verbosity, builder);

        if (errors) {
            printStackTrace(t, output, verbosity, "");
        }
        output.handle(verbosity, Ansi.ansi().reset().toString());
    }

    private void printStackTrace(Throwable t, Output output, Verbosity verbosity, String prefix) {
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
            output.handle(verbosity, builder.toString());
            builder.setLength(0);
        }
        for (Throwable se : t.getSuppressed()) {
            writeThrowable(se, output, verbosity, "Suppressed", prefix + "    ");
        }
        Throwable cause = t.getCause();
        if (cause != null && t != cause) {
            writeThrowable(cause, output, verbosity, "Caused by", prefix);
        }
    }

    private void writeThrowable(Throwable t, Output output, Verbosity verbosity, String caption, String prefix) {
        StringBuilder builder = new StringBuilder();
        builder.append(prefix)
                .append(bold(caption))
                .append(": ")
                .append(t.getClass().getName());
        if (t.getMessage() != null) {
            builder.append(": ").append(italic(t.getMessage()));
        }
        output.handle(verbosity, builder.toString());

        printStackTrace(t, output, verbosity, prefix);
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
        return Ansi.ansi()
                .a(Ansi.Attribute.ITALIC)
                .a(format)
                .a(Ansi.Attribute.ITALIC_OFF)
                .toString();
    }

    private static String bold(String format) {
        return Ansi.ansi()
                .a(Ansi.Attribute.INTENSITY_BOLD)
                .a(format)
                .a(Ansi.Attribute.INTENSITY_BOLD_OFF)
                .toString();
    }

    // Tree

    private static class AnsiTreeFormatter extends DependencyGraphDecorators.TreeDecorator {
        private final Marker marker;
        private final Function<Deque<DependencyNode>, Marker.Intent> intentMapper;

        public AnsiTreeFormatter(Marker marker, Function<Deque<DependencyNode>, Marker.Intent> intentMapper) {
            this.marker = marker;
            this.intentMapper = intentMapper;
        }

        @Override
        public String formatLine(
                int cmp, Deque<DependencyNode> nodes, List<Function<DependencyNode, String>> decorators) {
            String diff;
            if (cmp == 0) {
                diff = Ansi.ansi().fg(Ansi.Color.WHITE).a("   ").reset().toString();
            } else if (cmp < 0) {
                diff = Ansi.ansi().fg(Ansi.Color.RED).a("---").reset().toString();
            } else {
                diff = Ansi.ansi().fg(Ansi.Color.GREEN).a("+++").reset().toString();
            }
            return diff + " " + formatLine(nodes, decorators);
        }

        @Override
        public String formatLine(Deque<DependencyNode> nodes, List<Function<DependencyNode, String>> decorators) {
            String indentationStr = formatIndentation(nodes, "╰─", "├─", "  ", "│ ");
            List<String> nodeSegments = formatNode(nodes, decorators);
            String nodeLabel = nodeSegments.get(0);
            String nodeDecorators =
                    marker.detail(nodeSegments.get(1)) + String.join(" ", nodeSegments.subList(2, nodeSegments.size()));
            Marker.Intent intent = intentMapper.apply(nodes);
            return switch (intent) {
                case EMPHASIZE -> String.format("%s%s %s", indentationStr, marker.emphasize(nodeLabel), nodeDecorators);
                case OUTSTANDING ->
                    String.format("%s%s %s", indentationStr, marker.outstanding(nodeLabel), nodeDecorators);
                case NORMAL -> String.format("%s%s %s", indentationStr, marker.normal(nodeLabel), nodeDecorators);
                case DETAIL -> String.format("%s%s %s", indentationStr, marker.detail(nodeLabel), nodeDecorators);
                case UNIMPORTANT ->
                    String.format("%s%s %s", indentationStr, marker.unimportant(nodeLabel), nodeDecorators);
                case SCARY -> String.format("%s%s %s", indentationStr, marker.scary(nodeLabel), nodeDecorators);
                case BLOODY -> String.format("%s%s %s", indentationStr, marker.bloody(nodeLabel), nodeDecorators);
            };
        }
    }

    private static class TreeIntentMapper implements Function<Deque<DependencyNode>, Marker.Intent> {
        private final Function<DependencyNode, String> winner = DependencyGraphDumper.winnerNode();
        private final Function<DependencyNode, Boolean> premanaged = DependencyGraphDumper.isPremanaged();

        @Override
        public Marker.Intent apply(Deque<DependencyNode> nodes) {
            DependencyNode node = requireNonNull(nodes.peek(), "bug");
            if (nodes.size() == 1) {
                return Marker.Intent.OUTSTANDING;
            }
            if (winner.apply(node) != null) {
                return Marker.Intent.UNIMPORTANT;
            } else if (JavaScopes.PROVIDED.equals(node.getDependency().getScope())) {
                return Marker.Intent.DETAIL;
            } else if (premanaged.apply(node)) {
                return Marker.Intent.SCARY;
            } else {
                return Marker.Intent.OUTSTANDING;
            }
        }
    }

    private static class DmTreeIntentMapper implements Function<Deque<DependencyNode>, Marker.Intent> {
        @Override
        public Marker.Intent apply(Deque<DependencyNode> nodes) {
            DependencyNode node = requireNonNull(nodes.peek(), "bug");
            if (node.getDependency() != null
                    && "import".equals(node.getDependency().getScope())) {
                return Marker.Intent.SCARY;
            } else {
                return Marker.Intent.OUTSTANDING;
            }
        }
    }

    private static class SourceTreeIntentMapper implements Function<Deque<DependencyNode>, Marker.Intent> {
        @Override
        public Marker.Intent apply(Deque<DependencyNode> nodes) {
            DependencyNode node = requireNonNull(nodes.peek(), "bug");
            if ("external".equals(node.getArtifact().getProperty("source", ""))) {
                return Marker.Intent.UNIMPORTANT;
            } else {
                return Marker.Intent.OUTSTANDING;
            }
        }
    }

    // MessageBuilder

    private static class AnsiMarker extends Marker {
        public AnsiMarker(Output output, Verbosity verbosity) {
            super(output, verbosity);
        }

        @Override
        public Marker emphasize(String word) {
            return super.emphasize(Ansi.ansi()
                    .bold()
                    .fgBright(Ansi.Color.WHITE)
                    .a(word)
                    .reset()
                    .toString());
        }

        @Override
        public Marker outstanding(String word) {
            return super.outstanding(
                    Ansi.ansi().fgBright(Ansi.Color.GREEN).a(word).reset().toString());
        }

        @Override
        public Marker detail(String word) {
            return super.detail(
                    Ansi.ansi().fgBright(Ansi.Color.BLUE).a(word).reset().toString());
        }

        @Override
        public Marker unimportant(String word) {
            return super.unimportant(
                    Ansi.ansi().fg(Ansi.Color.BLUE).a(word).reset().toString());
        }

        @Override
        public Marker scary(String word) {
            return super.scary(
                    Ansi.ansi().fgBright(Ansi.Color.YELLOW).a(word).reset().toString());
        }

        @Override
        public Marker bloody(String word) {
            return super.bloody(
                    Ansi.ansi().bold().fgBright(Ansi.Color.RED).a(word).reset().toString());
        }
    }
}
