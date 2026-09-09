/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
String buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('ERROR')

assert buildLog.contains('[INFO] Downloading central::https://repo.maven.apache.org')
assert buildLog.contains('and unpacking it')
assert buildLog.contains('Checksum mismatch for SHA-512')

assert !new File(basedir, 'target/http-get/apache-maven-3.9.16').isDirectory()