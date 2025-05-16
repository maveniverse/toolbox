/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

/**
 * Some simple suppliers of extensions.
 */
public final class ExtensionsSuppliers {
    public static String empty110() {
        return "<extensions xmlns=\"http://maven.apache.org/EXTENSIONS/1.1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "            xsi:schemaLocation=\"http://maven.apache.org/EXTENSIONS/1.1.0 https://maven.apache.org/xsd/core-extensions-1.1.0.xsd\">\n"
                + "</extensions>\n";
    }
}
