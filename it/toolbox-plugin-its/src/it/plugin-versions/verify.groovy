/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
String buildLog = new File(basedir, 'build.log').text
assert !buildLog.contains('ERROR')

String pom = new File(basedir, 'pom.xml').text
String expectedPom = new File(basedir, 'expected-pom.xml').text
assert pom != null && expectedPom != null && pom.equals(expectedPom)
