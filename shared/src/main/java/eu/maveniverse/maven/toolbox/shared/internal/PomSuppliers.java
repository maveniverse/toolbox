/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

/**
 * Some simple suppliers of POM.
 */
public final class PomSuppliers {
    public static String empty400(String groupId, String artifactId, String version) {
        requireNonNull(groupId, "groupId");
        requireNonNull(artifactId, "artifactId");
        requireNonNull(version, "version");
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
                + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
                + "  <modelVersion>4.0.0</modelVersion>\n" //
                + "\n" //
                + "  <groupId>" + groupId + "</groupId>\n" //
                + "  <artifactId>" + artifactId + "</artifactId>\n" //
                + "  <version>" + version + "</version>\n" //
                + "</project>\n";
    }
}
