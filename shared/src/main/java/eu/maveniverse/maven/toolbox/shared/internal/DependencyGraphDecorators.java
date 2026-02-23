/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import java.util.function.Supplier;

public final class DependencyGraphDecorators {
    private DependencyGraphDecorators() {}

    public static Supplier<DependencyGraphDumper.LineFormatter> defaultSupplier() {
        return DependencyGraphDumper.LineFormatter::new;
    }

    public static class TreeDecorator extends DependencyGraphDumper.LineFormatter {}

    public static class DmTreeDecorator extends DependencyGraphDumper.LineFormatter {}

    public static class ParentChildTreeDecorator extends DependencyGraphDumper.LineFormatter {}

    public static class SubprojectTreeDecorator extends DependencyGraphDumper.LineFormatter {}

    public static class ProjectDependenciesTreeDecorator extends DependencyGraphDumper.LineFormatter {}
}
