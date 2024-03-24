/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import static java.util.Objects.requireNonNull;
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
import eu.maveniverse.maven.toolbox.shared.Slf4jOutput;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommandoVersion;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.jline.jansi.Ansi;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import picocli.CommandLine;

/**
 * Support class for all Mojos and Commands.
 */
public abstract class MojoSupport extends AbstractMojo implements Callable<Integer>, CommandLine.IVersionProvider {

    // CLI

    @CommandLine.Option(
            names = {"-v", "--verbose"},
            description = "Be verbose about things happening")
    private boolean verbose;

    @CommandLine.Option(
            names = {"-o", "--offline"},
            description = "Work offline")
    private boolean offline;

    @CommandLine.Option(
            names = {"-s", "--settings"},
            description = "The Maven User Settings file to use")
    private Path userSettingsXml;

    @CommandLine.Option(
            names = {"-gs", "--global-settings"},
            description = "The Maven Global Settings file to use")
    private Path globalSettingsXml;

    @CommandLine.Option(
            names = {"-P", "--activate-profiles"},
            split = ",",
            description = "Comma delimited list of profile IDs to activate (may use '+', '-' and '!' prefix)")
    private java.util.List<String> profiles;

    @CommandLine.Option(
            names = {"-D", "--define"},
            description = "Define a user property")
    private List<String> userProperties;

    @CommandLine.Option(
            names = {"--proxy"},
            description = "Define a HTTP proxy (host:port)")
    private String proxy;

    @CommandLine.Option(
            names = {"-B", "--batch-mode"},
            defaultValue = "false",
            description = "Work in batch mode (do not use ANSI colors)")
    private boolean batch;

    @CommandLine.Option(
            names = {"-e", "--errors"},
            defaultValue = "false",
            description = "Show error stack traces")
    private boolean errors;

    private Output createCliOutput() {
        return new Output() {
            @Override
            public boolean isVerbose() {
                return verbose;
            }

            @Override
            public void verbose(String msg, Object... params) {
                MojoSupport.this.verbose(msg, params);
            }

            @Override
            public void normal(String msg, Object... params) {
                MojoSupport.this.normal(msg, params);
            }

            @Override
            public void warn(String msg, Object... params) {
                MojoSupport.this.warn(msg, params);
            }

            @Override
            public void error(String msg, Object... params) {
                MojoSupport.this.error(msg, params);
            }
        };
    }

    private static final AtomicReference<Map<Object, Object>> CONTEXT = new AtomicReference<>(null);

    protected <T> T getOrCreate(Class<T> key, Supplier<T> supplier) {
        return (T) CONTEXT.get().computeIfAbsent(key, k -> supplier.get());
    }

    protected <T> T get(Class<T> key) {
        return (T) requireNonNull(CONTEXT.get().get(key), "key is not present");
    }

    @Override
    public String[] getVersion() {
        return new String[] {
            "MIMA " + Runtimes.INSTANCE.getRuntime().version(), "Toolbox " + ToolboxCommandoVersion.getVersion()
        };
    }

    private ContextOverrides createCLIContextOverrides() {
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
    }

    private ContextOverrides createMavenContextOverrides() {
        return ContextOverrides.create().build();
    }

    protected Runtime getRuntime() {
        return get(Runtime.class);
    }

    protected Context getContext() {
        return get(Context.class);
    }

    protected Output getOutput() {
        return get(Output.class);
    }

    protected ToolboxCommando getToolboxCommando() {
        return getOrCreate(ToolboxCommando.class, () -> ToolboxCommando.create(getRuntime(), getContext()));
    }

    private void verbose(String format, Object... args) {
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

    private void normal(String format, Object... args) {
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

    private void warn(String format, Object... args) {
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

    private void error(String format, Object... args) {
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

    /**
     * Picocli CLI entry point.
     */
    @Override
    public final Integer call() {
        Ansi.setEnabled(!batch);
        CONTEXT.compareAndSet(null, new HashMap<>());
        getOrCreate(Output.class, this::createCliOutput);
        getOrCreate(Runtime.class, Runtimes.INSTANCE::getRuntime);
        getOrCreate(Context.class, () -> get(Runtime.class).create(createCLIContextOverrides()));

        try {
            boolean result = doExecute(getOutput(), getToolboxCommando());
            if (!result && failOnLogicalFailure) {
                return 1;
            } else {
                return 0;
            }
        } catch (Exception e) {
            error("Error", e);
            return 1;
        }
    }

    // Mojo

    private Output createMojoOutput() {
        return new Slf4jOutput(LoggerFactory.getLogger(getClass()));
    }

    @CommandLine.Option(
            names = {"--fail-on-logical-failure"},
            defaultValue = "true",
            description = "Fail on operation logical failure")
    @Parameter(property = "failOnLogicalFailure", defaultValue = "true")
    protected boolean failOnLogicalFailure;

    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    protected Settings settings;

    /**
     * Maven Mojo entry point.
     */
    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        CONTEXT.compareAndSet(null, getPluginContext());
        getOrCreate(Output.class, this::createMojoOutput);
        getOrCreate(Runtime.class, Runtimes.INSTANCE::getRuntime);
        getOrCreate(Context.class, () -> get(Runtime.class).create(createMavenContextOverrides()));

        try {
            boolean result = doExecute(getOutput(), getToolboxCommando());
            if (!result && failOnLogicalFailure) {
                throw new MojoFailureException("Operation failed");
            }
        } catch (RuntimeException e) {
            throw new MojoExecutionException(e);
        } catch (Exception e) {
            throw new MojoFailureException(e);
        }
    }

    protected abstract boolean doExecute(Output output, ToolboxCommando toolboxCommando) throws Exception;
}
