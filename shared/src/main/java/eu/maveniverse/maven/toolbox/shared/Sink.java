/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Construct to accept collection of things.
 */
@FunctionalInterface
public interface Sink<T> extends AutoCloseable {
    void accept(T thing) throws IOException;

    default void accept(Stream<T> stream) throws IOException {
        try {
            stream.forEach(t -> {
                try {
                    accept(t);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    default void accept(Collection<T> things) throws IOException {
        requireNonNull(things, "things");
        try {
            for (T thing : things) {
                accept(thing);
            }
        } catch (IOException e) {
            cleanup(e);
            throw e;
        }
    }

    default void cleanup(Exception e) {}

    @Override
    default void close() throws Exception {}

    // nonClosing
    default Sink<T> nonClosing() {
        return new Sink<>() {
            @Override
            public void accept(T thing) throws IOException {
                Sink.this.accept(thing);
            }

            @Override
            public void accept(Stream<T> stream) throws IOException {
                Sink.this.accept(stream);
            }

            @Override
            public void accept(Collection<T> things) throws IOException {
                Sink.this.accept(things);
            }

            @Override
            public void cleanup(Exception e) {
                Sink.this.cleanup(e);
            }

            @Override
            public void close() {
                // nothing
            }
        };
    }

    // matching
    default Sink<T> matching(Predicate<T> predicate) {
        requireNonNull(predicate, "predicate");
        return new Sink<>() {
            public void accept(T thing) throws IOException {
                if (predicate.test(thing)) {
                    Sink.this.accept(thing);
                }
            }

            @Override
            public void accept(Stream<T> stream) throws IOException {
                Sink.this.accept(stream.filter(predicate));
            }

            @Override
            public void accept(Collection<T> things) throws IOException {
                Sink.this.accept(things.stream().filter(predicate).toList());
            }

            @Override
            public void cleanup(Exception e) {
                Sink.this.cleanup(e);
            }

            @Override
            public void close() throws Exception {
                Sink.this.close();
            }
        };
    }

    // mapping
    default Sink<T> mapping(Function<T, T> mapper) {
        requireNonNull(mapper, "mapper");
        return new Sink<>() {
            public void accept(T thing) throws IOException {
                Sink.this.accept(mapper.apply(thing));
            }

            @Override
            public void accept(Stream<T> stream) throws IOException {
                Sink.this.accept(stream.map(mapper));
            }

            @Override
            public void accept(Collection<T> things) throws IOException {
                Sink.this.accept(things.stream().map(mapper).toList());
            }

            @Override
            public void cleanup(Exception e) {
                Sink.this.cleanup(e);
            }

            @Override
            public void close() throws Exception {
                Sink.this.close();
            }
        };
    }

    // counting

    // tee
}
