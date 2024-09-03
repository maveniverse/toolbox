/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
String buildLog = new File(basedir, 'build.log').text

// These 3 version comes from ASF parent (also 3 different versions) and ASF project BOMs inherit it
assert buildLog.contains('[WARNING] DM conflicts discovered:')
assert buildLog.contains('[WARNING]  * org.apache.maven.plugin-tools:maven-plugin-annotations:jar version 3.11.0 prevails, but met versions [3.11.0, 3.10.2, 3.7.0]')
