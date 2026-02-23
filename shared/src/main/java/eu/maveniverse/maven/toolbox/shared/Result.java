/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

/**
 * The Toolbox Commando result.
 */
public final class Result<T> {
    public static <T> Result<T> failure(String message) {
        requireNonNull(message, "message");
        return new Result<>(false, message, null);
    }

    public static <T> Result<T> success(T data) {
        requireNonNull(data, "data");
        return new Result<>(true, "Success", data);
    }

    private final boolean success;
    private final String message;
    private final T data;

    private Result(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Optional<T> getData() {
        return Optional.ofNullable(data);
    }
}
