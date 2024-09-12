/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.DependencyMapper;
import eu.maveniverse.maven.toolbox.shared.DependencyMatcher;
import eu.maveniverse.maven.toolbox.shared.DependencySink;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.eclipse.aether.graph.Dependency;

/**
 * Various utility sink implementations.
 */
public final class DependencySinks {
    private DependencySinks() {}

    public static DependencySink build(Map<String, ?> properties, ToolboxCommandoImpl tc, String spec) {
        requireNonNull(properties, "properties");
        requireNonNull(tc, "tc");
        requireNonNull(spec, "spec");
        DependencySinkBuilder builder = new DependencySinkBuilder(properties, tc);
        SpecParser.parse(spec).accept(builder);
        return builder.build();
    }

    static class DependencySinkBuilder extends SpecParser.Builder {
        private final ToolboxCommandoImpl tc;

        public DependencySinkBuilder(Map<String, ?> properties, ToolboxCommandoImpl tc) {
            super(properties);
            this.tc = tc;
        }

        @Override
        public boolean visitEnter(SpecParser.Node node) {
            return super.visitEnter(node) && !"matching".equals(node.getValue()) && !"mapping".equals(node.getValue());
        }

        @Override
        protected void processOp(SpecParser.Node node) {
            switch (node.getValue()) {
                case "null": {
                    params.add(nullDependencySink());
                    break;
                }
                case "counting": {
                    params.add(countingDependencySink());
                    break;
                }
                case "tee": {
                    params.add(teeDependencySink(typedParams(DependencySink.class, node.getValue())));
                    break;
                }
                case "nonClosing": {
                    params.add(nonClosingDependencySink(typedParam(DependencySink.class, node.getValue())));
                    break;
                }
                case "matching": {
                    if (node.getChildren().size() != 2) {
                        throw new IllegalArgumentException("op matching accepts only 2 argument");
                    }
                    DependencyMatcher.DependencyMatcherBuilder matcherBuilder =
                            new DependencyMatcher.DependencyMatcherBuilder(properties);
                    node.getChildren().getFirst().accept(matcherBuilder);
                    DependencyMatcher matcher = matcherBuilder.build();
                    DependencySinkBuilder sinkBuilder = new DependencySinkBuilder(properties, tc);
                    node.getChildren().get(1).accept(sinkBuilder);
                    DependencySink delegate = sinkBuilder.build();
                    params.add(matchingDependencySink(matcher, delegate));
                    node.getChildren().clear();
                    break;
                }
                case "mapping": {
                    if (node.getChildren().size() != 2) {
                        throw new IllegalArgumentException("op mapping accepts only 2 argument");
                    }
                    DependencyMapper.DependencyMapperBuilder mapperBuilder =
                            new DependencyMapper.DependencyMapperBuilder(properties);
                    node.getChildren().getFirst().accept(mapperBuilder);
                    DependencyMapper mapper = mapperBuilder.build();
                    DependencySinkBuilder sinkBuilder = new DependencySinkBuilder(properties, tc);
                    node.getChildren().get(1).accept(sinkBuilder);
                    DependencySink delegate = sinkBuilder.build();
                    params.add(mappingDependencySink(mapper, delegate));
                    node.getChildren().clear();
                    break;
                }
                default:
                    throw new IllegalArgumentException("unknown op " + node.getValue());
            }
        }

        public DependencySink build() {
            return build(DependencySink.class);
        }
    }

    /**
     * Creates a "/dev/null" dependency sink.
     */
    public static NullDependencySink nullDependencySink() {
        return new NullDependencySink();
    }

    public static class NullDependencySink implements DependencySink {
        private NullDependencySink() {}

        @Override
        public void accept(Collection<Dependency> dependencies) {}

        @Override
        public void accept(Dependency dependency) {}
    }

    public static DelegatingDependencySink delegatingDependencySink(DependencySink delegate) {
        return new DelegatingDependencySink(delegate);
    }

    public static class DelegatingDependencySink implements DependencySink {
        private final DependencySink delegate;

        private DelegatingDependencySink(DependencySink delegate) {
            requireNonNull(delegate, "delegate");
            this.delegate = delegate;
        }

        @Override
        public void accept(Dependency dependency) throws IOException {
            delegate.accept(dependency);
        }

        @Override
        public void cleanup(Exception e) {
            delegate.cleanup(e);
        }

        @Override
        public void close() throws Exception {
            delegate.close();
        }
    }

    /**
     * Creates a delegating sink that prevents closing delegate.
     */
    public static NonClosingDependencySink nonClosingDependencySink(DependencySink delegate) {
        requireNonNull(delegate, "delegate");
        return new NonClosingDependencySink(delegate);
    }

    public static class NonClosingDependencySink extends DelegatingDependencySink {
        private NonClosingDependencySink(DependencySink delegate) {
            super(delegate);
        }

        @Override
        public void close() {
            // do nothing
        }
    }

    /**
     * Creates a delegating sink that delegates calls only with matched artifacts.
     */
    public static MatchingDependencySink matchingDependencySink(
            Predicate<Dependency> matcher, DependencySink delegate) {
        requireNonNull(matcher, "matcher");
        requireNonNull(delegate, "delegate");
        return new MatchingDependencySink(matcher, delegate);
    }

    public static class MatchingDependencySink extends DelegatingDependencySink {
        private final Predicate<Dependency> matcher;

        private MatchingDependencySink(Predicate<Dependency> matcher, DependencySink delegate) {
            super(delegate);
            this.matcher = matcher;
        }

        @Override
        public void accept(Collection<Dependency> dependencies) throws IOException {
            super.accept(dependencies.stream().filter(matcher).collect(Collectors.toList()));
        }

        @Override
        public void accept(Dependency dependency) throws IOException {
            if (matcher.test(dependency)) {
                super.accept(dependency);
            }
        }
    }

    /**
     * Creates a delegating sink that delegates calls with mapped artifacts.
     */
    public static MappingDependencySink mappingDependencySink(
            Function<Dependency, Dependency> mapper, DependencySink delegate) {
        requireNonNull(mapper, "mapper");
        requireNonNull(delegate, "delegate");
        return new MappingDependencySink(mapper, delegate);
    }

    public static class MappingDependencySink extends DelegatingDependencySink {
        private final Function<Dependency, Dependency> mapper;

        private MappingDependencySink(Function<Dependency, Dependency> mapper, DependencySink delegate) {
            super(delegate);
            this.mapper = mapper;
        }

        @Override
        public void accept(Collection<Dependency> dependencies) throws IOException {
            super.accept(dependencies.stream().map(mapper).collect(Collectors.toList()));
        }

        @Override
        public void accept(Dependency dependency) throws IOException {
            super.accept(mapper.apply(dependency));
        }
    }

    /**
     * Creates a counting sink, that simply counts all the accepted artifacts.
     */
    public static CountingArtifactSink countingDependencySink() {
        return new CountingArtifactSink();
    }

    public static class CountingArtifactSink implements DependencySink {
        private final LongAdder counter;

        private CountingArtifactSink() {
            this.counter = new LongAdder();
        }

        @Override
        public void accept(Dependency dependency) {
            counter.increment();
        }

        public int count() {
            return counter.intValue();
        }
    }

    /**
     * Creates a "tee" artifact sink out of supplied sinks.
     */
    public static TeeDependencySink teeDependencySink(DependencySink... dependencySinks) {
        return teeDependencySink(Arrays.asList(dependencySinks));
    }

    /**
     * Creates a "tee" artifact sink out of supplied sinks.
     */
    public static TeeDependencySink teeDependencySink(Collection<? extends DependencySink> dependencySinks) {
        requireNonNull(dependencySinks, "dependencySinks");
        return new TeeDependencySink(dependencySinks);
    }

    public static class TeeDependencySink implements DependencySink {
        private final Collection<DependencySink> dependencySinks;

        private TeeDependencySink(Collection<? extends DependencySink> artifactSinks) {
            this.dependencySinks = Collections.unmodifiableCollection(new ArrayList<>(artifactSinks));
        }

        @Override
        public void accept(Collection<Dependency> dependencies) throws IOException {
            for (DependencySink sink : dependencySinks) {
                sink.accept(dependencies);
            }
        }

        @Override
        public void accept(Dependency dependency) throws IOException {
            for (DependencySink sink : dependencySinks) {
                sink.accept(dependency);
            }
        }

        @Override
        public void cleanup(Exception e) {
            for (DependencySink sink : dependencySinks) {
                sink.cleanup(e);
            }
        }

        @Override
        public void close() throws Exception {
            for (DependencySink sink : dependencySinks) {
                sink.close();
            }
        }
    }
}
