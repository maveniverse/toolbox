/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import java.util.Set;

public interface ResolutionScope {
    enum Mode {
        ELIMINATE,
        REMOVE
    }

    String getId();

    Language getLanguage();

    Mode getMode();

    Set<? extends DependencyScope> getDirectlyIncluded();

    Set<? extends DependencyScope> getTransitivelyExcluded();
}
