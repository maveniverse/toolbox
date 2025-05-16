/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommandoVersion;
import eu.maveniverse.maven.toolbox.shared.output.Output;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import picocli.CommandLine;

/**
 * Support class for all Mojos and Commands.
 */
public abstract class MojoSupport extends AbstractMojo
        implements Callable<Integer>, CwdAware, CommandLine.IVersionProvider {

    // CLI

    @CommandLine.Option(
            names = {"--verbosity"},
            defaultValue = "NORMAL",
            description =
                    "Output verbosity level in CLI. Accepted values: SILENT, TIGHT, NORMAL (default), SUGGEST, CHATTER")
    @Parameter(property = "verbosity", defaultValue = "NORMAL")
    private Output.Verbosity verbosity;

    @CommandLine.Option(
            names = {"-X", "--debug"},
            description = "Enable debug logging in CLI.")
    private boolean debug;

    @CommandLine.Option(
            names = {"-Y", "--trace"},
            description = "Enable trace logging in CLI.")
    private boolean trace;

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

    @CommandLine.Option(
            names = {"--fail-on-logical-failure"},
            defaultValue = "true",
            description = "Fail on operation logical failure")
    @Parameter(property = "failOnLogicalFailure", defaultValue = "true")
    private boolean failOnLogicalFailure;

    // cwd

    private Path cwd = Path.of(System.getProperty("user.dir"));

    @Override
    public void setCwd(Path cwd) {
        requireNonNull(cwd, "cwd");
        if (!Files.isDirectory(cwd)) {
            throw new IllegalArgumentException("cwd must be an existing directory");
        }
        this.cwd = cwd;
    }

    @Override
    public Path getCwd() {
        return cwd;
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
        if (cwd != null) {
            builder.withBasedirOverride(cwd);
        }
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

    private ContextOverrides createMojoContextOverrides() {
        ContextOverrides.Builder contextOverrides = ContextOverrides.create();
        if (cwd != null) {
            contextOverrides.withBasedirOverride(cwd);
        }
        return contextOverrides.build();
    }

    protected Context getContext() {
        return get(Context.class);
    }

    protected Output getOutput() {
        return get(Output.class);
    }

    protected ToolboxCommando getToolboxCommando() {
        return getOrCreate(ToolboxCommando.class, () -> ToolboxCommando.create(getOutput(), getContext()));
    }

    /**
     * Picocli CLI entry point.
     * <p>
     * This entry point is re-entrant due REPL.
     */
    @Override
    public final Integer call() {
        if (trace) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "TRACE");
        } else if (debug) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG");
        }
        boolean seeded = CONTEXT.compareAndSet(null, new HashMap<>());
        getOrCreate(Runtime.class, Runtimes.INSTANCE::getRuntime);
        getOrCreate(Context.class, () -> get(Runtime.class).create(createCLIContextOverrides()));
        getOrCreate(Output.class, () -> OutputFactory.createCliOutput(batch, errors, verbosity));

        try {
            Result<?> result = doExecute();
            if (!result.isSuccess() && failOnLogicalFailure) {
                return 1;
            } else {
                return 0;
            }
        } catch (RuntimeException e) {
            System.err.println("Error: " + e.getClass() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            return 1;
        } catch (Exception e) {
            System.err.println("Error: " + e.getClass() + ": " + e.getMessage());
            if (errors) {
                e.printStackTrace(System.err);
            }
            return 1;
        } finally {
            if (seeded) {
                try {
                    getOutput().close();
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
                try {
                    getContext().close();
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
                CONTEXT.set(null);
            }
        }
    }

    // Mojo

    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    protected Settings mojoSettings;

    @Parameter(defaultValue = "${session.request.interactiveMode}", readonly = true, required = true)
    protected boolean mojoInteractiveMode;

    @Parameter(defaultValue = "${session.request.showErrors}", readonly = true, required = true)
    protected boolean mojoErrors;

    @Parameter(property = "forceStdout")
    protected boolean forceStdout;

    /**
     * Option to run potentially destructive commands without performing any IO.
     */
    @CommandLine.Option(
            names = {"--dry-run"},
            defaultValue = "false",
            description = "Option to run potentially destructive commands without performing any IO")
    @Parameter(property = "dryRun", defaultValue = "false")
    protected boolean dryRun;

    /**
     * Maven Mojo entry point.
     * <p>
     * Maven Mojos are not re-entrant (Mojo cannot, or should not, call itself).
     */
    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        CONTEXT.compareAndSet(null, new HashMap<>());
        getOrCreate(Runtime.class, Runtimes.INSTANCE::getRuntime);
        getOrCreate(Context.class, () -> get(Runtime.class).create(createMojoContextOverrides()));
        if (forceStdout) {
            getOrCreate(Output.class, () -> OutputFactory.createCliOutput(!mojoInteractiveMode, mojoErrors, verbosity));
        } else {
            getOrCreate(
                    Output.class, () -> OutputFactory.createMojoOutput(!mojoInteractiveMode, mojoErrors, verbosity));
        }
        try {
            Result<?> result = doExecute();
            if (!result.isSuccess() && failOnLogicalFailure) {
                throw new MojoFailureException("Operation failed: " + result.getMessage());
            }
        } catch (RuntimeException e) {
            throw new MojoExecutionException("Execution failed: ", e);
        } catch (Exception e) {
            throw new MojoFailureException("Operation failed: ", e);
        } finally {
            try {
                getOutput().close();
            } catch (Exception e) {
                getLog().error(e);
            }
            try {
                getContext().close();
            } catch (Exception e) {
                getLog().error(e);
            }
        }
    }

    protected abstract Result<?> doExecute() throws Exception;
}
