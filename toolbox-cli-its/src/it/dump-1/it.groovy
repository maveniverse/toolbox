/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */

StringBuilder out = new StringBuilder()
StringBuilder err = new StringBuilder()
String[] args = ["${java}", '-jar', "${cli}", "-e", "dump", "-Dverbosity=CHATTER"]

ProcessBuilder proc = new ProcessBuilder(args)
Process process = proc.start()
process.consumeProcessOutput(out, err)
process.waitFor()

assert out.contains("Toolbox ${project.version}")
