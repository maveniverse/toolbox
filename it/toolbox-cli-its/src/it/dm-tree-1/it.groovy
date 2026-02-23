/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
StringBuilder out = new StringBuilder()
StringBuilder err = new StringBuilder()
String[] args = ["${java}", '-jar', "${cli}", "-e", "dm-tree", "org.springframework.boot:spring-boot-dependencies:3.3.3"]

ProcessBuilder proc = new ProcessBuilder(args)
Process process = proc.start()
process.consumeProcessOutput(out, err)
process.waitFor()

assert out.contains('DM conflicts discovered:')
assert out.contains(' * org.apache.maven.plugin-tools:maven-plugin-annotations:jar version 3.11.0 prevails, but met versions [3.11.0, 3.10.2, 3.7.0]')
