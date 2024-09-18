/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
String buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('org.apache.maven:maven-core:jar:3.6.3 EXISTS')
assert buildLog.contains('Checked TOTAL of 5 (existing: 5 not existing: 0)')
