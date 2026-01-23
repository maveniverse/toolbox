/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */

StringBuilder out = new StringBuilder()
StringBuilder err = new StringBuilder()
String[] args = ["${java}", '-jar', "${cli}", "-e", "resolve", "junit:junit:4.13.2", "--poms"]

ProcessBuilder proc = new ProcessBuilder(args)
Process process = proc.start()
process.consumeProcessOutput(out, err)
process.waitFor()

// this is junit:junit:pom:4.13.2 sha-512 hash:
assert out.contains("SHA-512: abf1cf90ab6a525ae0cfa5235563b00bc6ef07c59f8cdd5c5495ea8b14941b5803a3f7adffaa36ec37152a7904a10e04939c0d11b48115f1943a1606cc5066c0")