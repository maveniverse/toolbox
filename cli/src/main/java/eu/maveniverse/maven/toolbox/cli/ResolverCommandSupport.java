/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.cli;

import eu.maveniverse.maven.mima.context.Context;

/**
 * Support.
 */
public abstract class ResolverCommandSupport extends CommandSupport {

    @Override
    public final Integer call() {
        try (Context context = getContext()) {
            return doCall(context);
        } catch (Exception e) {
            error("Error", e);
            return 1;
        }
    }

    protected Integer doCall(Context context) throws Exception {
        throw new RuntimeException("Not implemented; you should override this method in subcommand");
    }
}
