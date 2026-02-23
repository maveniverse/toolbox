/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
String buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('0235ba8b489512805ac13a8f9ea77a1ca5ebe3e8 (./aopalliance-1.0.jar) = aopalliance:aopalliance:jar:1.0')
