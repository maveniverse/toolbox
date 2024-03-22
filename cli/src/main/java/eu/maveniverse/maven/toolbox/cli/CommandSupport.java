/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.cli;

import static org.jline.jansi.Ansi.Attribute.INTENSITY_BOLD;
import static org.jline.jansi.Ansi.Attribute.INTENSITY_BOLD_OFF;
import static org.jline.jansi.Ansi.Attribute.INTENSITY_FAINT;
import static org.jline.jansi.Ansi.Attribute.ITALIC;
import static org.jline.jansi.Ansi.Attribute.ITALIC_OFF;
import static org.jline.jansi.Ansi.Color.RED;
import static org.jline.jansi.Ansi.Color.WHITE;
import static org.jline.jansi.Ansi.Color.YELLOW;
import static org.jline.jansi.Ansi.ansi;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import eu.maveniverse.maven.toolbox.shared.Output;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.jline.jansi.Ansi;
import org.slf4j.helpers.MessageFormatter;
import picocli.CommandLine;

/**
 * Support class.
 */
public abstract class CommandSupport implements Callable<Integer>, CommandLine.IVersionProvider {
    @CommandLine.Option(
            names = {"-v", "--verbose"},
            description = "Be verbose about things happening")
    protected boolean verbose;

    @CommandLine.Option(
            names = {"-o", "--offline"},
            description = "Work offline")
    protected boolean offline;

    @CommandLine.Option(
            names = {"-s", "--settings"},
            description = "The Maven User Settings file to use")
    protected Path userSettingsXml;

    @CommandLine.Option(
            names = {"-gs", "--global-settings"},
            description = "The Maven Global Settings file to use")
    protected Path globalSettingsXml;

    @CommandLine.Option(
            names = {"-P", "--activate-profiles"},
            split = ",",
            description = "Comma delimited list of profile IDs to activate (may use '+', '-' and '!' prefix)")
    protected java.util.List<String> profiles;

    @CommandLine.Option(
            names = {"-D", "--define"},
            description = "Define a user property")
    protected List<String> userProperties;

    @CommandLine.Option(
            names = {"--proxy"},
            description = "Define a HTTP proxy (host:port)")
    protected String proxy;

    @CommandLine.Option(
            names = {"-B", "--batch-mode"},
            defaultValue = "false",
            description = "Work in batch mode (do not use ANSI colors)")
    protected boolean batch;

    @CommandLine.Option(
            names = {"-e", "--errors"},
            defaultValue = "false",
            description = "Show error stack traces")
    protected boolean errors;

    protected final Output output = new Output() {
        @Override
        public boolean isVerbose() {
            return verbose;
        }

        @Override
        public void verbose(String msg, Object... params) {
            CommandSupport.this.verbose(msg, params);
        }

        @Override
        public void normal(String msg, Object... params) {
            CommandSupport.this.normal(msg, params);
        }

        @Override
        public void warn(String msg, Object... params) {
            CommandSupport.this.warn(msg, params);
        }

        @Override
        public void error(String msg, Object... params) {
            CommandSupport.this.error(msg, params);
        }
    };

    private static final ConcurrentHashMap<String, ArrayDeque<Object>> EXECUTION_CONTEXT = new ConcurrentHashMap<>();

    protected Object getOrCreate(String key, Supplier<?> supplier) {
        ArrayDeque<Object> deque = EXECUTION_CONTEXT.computeIfAbsent(key, k -> new ArrayDeque<>());
        if (deque.isEmpty()) {
            deque.push(supplier.get());
        }
        return deque.peek();
    }

    protected void push(String key, Object object) {
        ArrayDeque<Object> deque = EXECUTION_CONTEXT.computeIfAbsent(key, k -> new ArrayDeque<>());
        deque.push(object);
    }

    protected Object pop(String key) {
        ArrayDeque<Object> deque = EXECUTION_CONTEXT.computeIfAbsent(key, k -> new ArrayDeque<>());
        return deque.pop();
    }

    @Override
    public String[] getVersion() {
        return new String[] {
            "MIMA " + getRuntime().version(),
            "Toolbox " + getToolboxCommando(getContext()).getVersion()
        };
    }

    protected Runtime getRuntime() {
        return (Runtime) getOrCreate(Runtime.class.getName(), Runtimes.INSTANCE::getRuntime);
    }

    protected ContextOverrides getContextOverrides() {
        return (ContextOverrides) getOrCreate(ContextOverrides.class.getName(), () -> {
            // create builder with some sane defaults
            ContextOverrides.Builder builder = ContextOverrides.create().withUserSettings(true);
            if (offline) {
                builder.offline(true);
            }
            if (userSettingsXml != null) {
                builder.withUserSettingsXmlOverride(userSettingsXml);
            }
            if (globalSettingsXml != null) {
                builder.withGlobalSettingsXmlOverride(globalSettingsXml);
            }
            if (profiles != null && !profiles.isEmpty()) {
                ArrayList<String> activeProfiles = new ArrayList<>();
                ArrayList<String> inactiveProfiles = new ArrayList<>();
                for (String profile : profiles) {
                    if (profile.startsWith("+")) {
                        activeProfiles.add(profile.substring(1));
                    } else if (profile.startsWith("-") || profile.startsWith("!")) {
                        inactiveProfiles.add(profile.substring(1));
                    } else {
                        activeProfiles.add(profile);
                    }
                }
                builder.withActiveProfileIds(activeProfiles).withInactiveProfileIds(inactiveProfiles);
            }
            if (userProperties != null && !userProperties.isEmpty()) {
                HashMap<String, String> defined = new HashMap<>(userProperties.size());
                String name;
                String value;
                for (String property : userProperties) {
                    int i = property.indexOf('=');
                    if (i <= 0) {
                        name = property.trim();
                        value = Boolean.TRUE.toString();
                    } else {
                        name = property.substring(0, i).trim();
                        value = property.substring(i + 1);
                    }
                    defined.put(name, value);
                }
                builder.userProperties(defined);
            }
            if (proxy != null) {
                String[] elems = proxy.split(":");
                if (elems.length != 2) {
                    throw new IllegalArgumentException("Proxy must be specified as 'host:port'");
                }
                Proxy proxySettings = new Proxy();
                proxySettings.setId("mima-mixin");
                proxySettings.setActive(true);
                proxySettings.setProtocol("http");
                proxySettings.setHost(elems[0]);
                proxySettings.setPort(Integer.parseInt(elems[1]));
                Settings proxyMixin = new Settings();
                proxyMixin.addProxy(proxySettings);
                builder.withEffectiveSettingsMixin(proxyMixin);
            }
            return builder.build();
        });
    }

    protected Context getContext() {
        return (Context) getOrCreate(Context.class.getName(), () -> getRuntime().create(getContextOverrides()));
    }

    protected ToolboxCommando getToolboxCommando(Context context) {
        return (ToolboxCommando)
                getOrCreate(ToolboxCommando.class.getName(), () -> ToolboxCommando.create(getRuntime(), context));
    }

    protected void verbose(String format, Object... args) {
        if (!verbose) {
            return;
        }
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            log(
                    System.err,
                    ansi().a(INTENSITY_FAINT)
                            .fg(WHITE)
                            .a(MessageFormatter.arrayFormat(format, Arrays.copyOfRange(args, 0, args.length - 1))
                                    .getMessage())
                            .reset()
                            .toString(),
                    (Throwable) args[args.length - 1]);
        } else {
            log(
                    System.err,
                    ansi().a(INTENSITY_FAINT)
                            .fg(WHITE)
                            .a(MessageFormatter.arrayFormat(format, args).getMessage())
                            .reset()
                            .toString());
        }
    }

    protected void normal(String format, Object... args) {
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            log(
                    System.err,
                    ansi().fg(WHITE)
                            .a(MessageFormatter.arrayFormat(format, Arrays.copyOfRange(args, 0, args.length - 1))
                                    .getMessage())
                            .reset()
                            .toString(),
                    (Throwable) args[args.length - 1]);
        } else {
            log(
                    System.err,
                    ansi().fg(WHITE)
                            .a(MessageFormatter.arrayFormat(format, args).getMessage())
                            .reset()
                            .toString());
        }
    }

    protected void warn(String format, Object... args) {
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            log(
                    System.err,
                    ansi().fg(YELLOW)
                            .a(MessageFormatter.arrayFormat(format, Arrays.copyOfRange(args, 0, args.length - 1))
                                    .getMessage())
                            .reset()
                            .toString(),
                    (Throwable) args[args.length - 1]);
        } else {
            log(
                    System.err,
                    ansi().fg(YELLOW)
                            .a(MessageFormatter.arrayFormat(format, args).getMessage())
                            .reset()
                            .toString());
        }
    }

    protected void error(String format, Object... args) {
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            log(
                    System.err,
                    ansi().a(INTENSITY_BOLD)
                            .fg(RED)
                            .a(MessageFormatter.arrayFormat(format, Arrays.copyOfRange(args, 0, args.length - 1))
                                    .getMessage())
                            .reset()
                            .toString(),
                    (Throwable) args[args.length - 1]);
        } else {
            log(
                    System.err,
                    ansi().a(INTENSITY_BOLD)
                            .fg(RED)
                            .a(MessageFormatter.arrayFormat(format, args).getMessage())
                            .reset()
                            .toString());
        }
    }

    private void log(PrintStream ps, String message) {
        log(ps, message, null);
    }

    private void log(PrintStream ps, String message, Throwable throwable) {
        ps.println(message);
        writeThrowable(throwable, ps);
    }

    private static String failure(String format) {
        return ansi().a(ITALIC).a(format).a(ITALIC_OFF).toString();
    }

    private static String strong(String format) {
        return ansi().a(INTENSITY_BOLD).a(format).a(INTENSITY_BOLD_OFF).toString();
    }

    private void writeThrowable(Throwable t, PrintStream stream) {
        if (t == null) {
            return;
        }
        String builder = failure(t.getClass().getName());
        if (t.getMessage() != null) {
            builder += ": " + failure(t.getMessage());
        }
        stream.println(builder);

        if (errors) {
            printStackTrace(t, stream, "");
        }
        stream.println(ansi().reset());
    }

    private void printStackTrace(Throwable t, PrintStream stream, String prefix) {
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

    private void writeThrowable(Throwable t, PrintStream stream, String caption, String prefix) {
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
        assert e != null;

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

    @Override
    public final Integer call() {
        Ansi.setEnabled(!batch);
        try (Context context = getContext()) {
            return doCall(getToolboxCommando(context)) ? 0 : 1;
        } catch (Exception e) {
            error("Error", e);
            return 1;
        }
    }

    protected abstract boolean doCall(ToolboxCommando toolboxCommando) throws Exception;
}
