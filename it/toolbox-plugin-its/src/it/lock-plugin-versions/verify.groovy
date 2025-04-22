/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
String buildLog = new File(basedir, 'build.log').text
assert !buildLog.contains('ERROR')

String pom = new File(basedir, 'pom.xml').text

// partial assert: original does not have it, but locked one should list it
assert pom.contains('maven-deploy-plugin')
