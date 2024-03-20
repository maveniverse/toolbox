/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.toolbox.shared.internal.ToolboxCommandoImpl;
import java.io.Closeable;
import java.io.IOException;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;

/**
 * The Toolbox Commando, that implements all the commands that are exposed via Mojos or CLI.
 * <p>
 * This instance manages {@link Context}, corresponding {@link ToolboxResolver} and maps one-to-one onto commands.
 * Can be considered something like "high level" API of Toolbox.
 */
public interface ToolboxCommando extends Closeable {
    /**
     * Gets or creates context. This method should be used to get {@link ToolboxResolver} instance that may be shared
     * across context (session).
     */
    static ToolboxCommando getOrCreate(Context context) {
        requireNonNull(context, "context");
        return (ToolboxCommando) context.repositorySystemSession()
                .getData()
                .computeIfAbsent(ToolboxCommando.class, () -> new ToolboxCommandoImpl(context));
    }

    /**
     * Removes toolbox from context (session).
     */
    static void unset(Context context) {
        requireNonNull(context, "context");
        context.repositorySystemSession().getData().set(ToolboxCommando.class, null);
    }

    /**
     * Derives new customized commando instance.
     */
    ToolboxCommando derive(ContextOverrides contextOverrides);

    /**
     * Closes this instance. Closed instance should be used anymore.
     */
    void close();

    /**
     * The toolbox.
     */
    ToolboxResolver toolboxResolver();

    ToolboxSearchApi toolboxSearchApi();

    boolean tree(ResolutionScope resolutionScope, ResolutionRoot resolutionRoot, boolean verbose, Logger output);

    // Search API related commands: they target one single RemoteRepository

    boolean exists(
            RemoteRepository remoteRepository,
            String gav,
            boolean pom,
            boolean sources,
            boolean javadoc,
            boolean signature,
            boolean allRequired,
            Logger output)
            throws IOException;

    boolean identify(RemoteRepository remoteRepository, String target, Logger output) throws IOException;

    boolean list(RemoteRepository remoteRepository, String gavoid, Logger output) throws IOException;

    boolean search(RemoteRepository remoteRepository, String expression, Logger output) throws IOException;

    boolean verify(RemoteRepository remoteRepository, String gav, String sha1, Logger output) throws IOException;
}
