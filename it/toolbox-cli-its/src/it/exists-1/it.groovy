/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */

StringBuilder out = new StringBuilder()
StringBuilder err = new StringBuilder()
String[] args = ["${java}", '-jar', "${cli}", "-e", "exists", "org.apache.maven:maven-core:3.6.3", "--pom", "--javadoc", "--sources", "--signature", "--all-required"]

ProcessBuilder proc = new ProcessBuilder(args)
Process process = proc.start()
process.consumeProcessOutput(out, err)
process.waitFor()

assert out.contains('org.apache.maven:maven-core:jar:3.6.3 EXISTS')
assert out.contains('Checked TOTAL of 5 (existing: 5 not existing: 0)')
