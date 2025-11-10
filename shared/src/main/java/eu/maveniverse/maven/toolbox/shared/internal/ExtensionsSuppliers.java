/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import eu.maveniverse.domtrip.maven.ExtensionsEditor;

/**
 * Some simple suppliers of extensions.
 */
public final class ExtensionsSuppliers {
    public static String empty120() {
        ExtensionsEditor editor = new ExtensionsEditor();
        editor.createExtensionsDocument();
        return editor.toXml();
    }
}
