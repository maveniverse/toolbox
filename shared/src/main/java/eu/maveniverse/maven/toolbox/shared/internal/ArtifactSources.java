/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.ArtifactMapper;
import eu.maveniverse.maven.toolbox.shared.ArtifactMatcher;
import eu.maveniverse.maven.toolbox.shared.input.StringSlurper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * Various utility source implementations.
 */
public final class ArtifactSources {
    private ArtifactSources() {}

    public static Artifacts.Source build(Map<String, ?> properties, ToolboxCommandoImpl tc, String spec) {
        requireNonNull(properties, "properties");
        requireNonNull(tc, "tc");
        requireNonNull(spec, "spec");
        ArtifactSourceBuilder builder = new ArtifactSourceBuilder(properties, tc);
        SpecParser.parse(spec).accept(builder);
        return builder.build();
    }

    static class ArtifactSourceBuilder extends SpecParser.Builder {
        private final ToolboxCommandoImpl tc;

        public ArtifactSourceBuilder(Map<String, ?> properties, ToolboxCommandoImpl tc) {
            super(properties);
            this.tc = tc;
        }

        @Override
        public boolean visitEnter(SpecParser.Node node) {
            return super.visitEnter(node)
                    && !"matching".equals(node.getValue())
                    && !"mapping".equals(node.getValue())
                    && !"concat".equals(node.getValue());
        }

        @Override
        protected void processOp(SpecParser.Node node) {
            switch (node.getValue()) {
                case "null": {
                    params.add(nullArtifactSource());
                    break;
                }
                case "gav": {
                    String gav = stringParam(node.getValue());
                    params.add(gavArtifactSource(gav));
                    break;
                }
                case "gavs": {
                    String gav = stringParam(node.getValue());
                    params.add(gavsArtifactSource(gav));
                    break;
                }
                case "concat": {
                    List<Artifacts.Source> sources = new ArrayList<>();
                    for (SpecParser.Node sourceNode : node.getChildren()) {
                        ArtifactSources.ArtifactSourceBuilder builder = new ArtifactSourceBuilder(properties, tc);
                        sourceNode.accept(builder);
                        sources.add(builder.build());
                    }
                    params.add(concatArtifactSource(sources));
                    break;
                }
                // resolve, resolveTransitive
                case "directory": {
                    Path p0 = tc.basedir().resolve(stringParam(node.getValue()));
                    params.add(DirectorySource.directory(p0));
                    break;
                }
                case "sessionLocalRepository": {
                    params.add(LocalRepositorySource.local(
                            tc.session().getLocalRepository().getBasedir().toPath()));
                    break;
                }
                case "localRepository": {
                    Path p0 = tc.basedir().resolve(stringParam(node.getValue()));
                    params.add(LocalRepositorySource.local(p0));
                    break;
                }
                case "recorder": {
                    params.add(tc.recorder());
                    break;
                }
                case "matching": {
                    if (node.getChildren().size() != 2) {
                        throw new IllegalArgumentException("op matching accepts only 2 argument");
                    }
                    ArtifactMatcher.ArtifactMatcherBuilder matcherBuilder =
                            new ArtifactMatcher.ArtifactMatcherBuilder(properties);
                    node.getChildren().get(0).accept(matcherBuilder);
                    ArtifactMatcher matcher = matcherBuilder.build();
                    ArtifactSources.ArtifactSourceBuilder sourceBuilder =
                            new ArtifactSources.ArtifactSourceBuilder(properties, tc);
                    node.getChildren().get(1).accept(sourceBuilder);
                    Artifacts.Source delegate = sourceBuilder.build();
                    params.add(matchingArtifactSource(matcher, delegate));
                    node.getChildren().clear();
                    break;
                }
                case "mapping": {
                    if (node.getChildren().size() != 2) {
                        throw new IllegalArgumentException("op mapping accepts only 2 argument");
                    }
                    ArtifactMapper.ArtifactMapperBuilder mapperBuilder =
                            new ArtifactMapper.ArtifactMapperBuilder(properties);
                    node.getChildren().get(0).accept(mapperBuilder);
                    ArtifactMapper mapper = mapperBuilder.build();
                    ArtifactSources.ArtifactSourceBuilder sourceBuilder =
                            new ArtifactSources.ArtifactSourceBuilder(properties, tc);
                    node.getChildren().get(1).accept(sourceBuilder);
                    Artifacts.Source delegate = sourceBuilder.build();
                    params.add(mappingArtifactSource(mapper, delegate));
                    node.getChildren().clear();
                    break;
                }
                default:
                    throw new IllegalArgumentException("unknown op " + node.getValue());
            }
        }

        public Artifacts.Source build() {
            return build(Artifacts.Source.class);
        }
    }

    public static NullArtifactSource nullArtifactSource() {
        return new NullArtifactSource();
    }

    public static class NullArtifactSource implements Artifacts.Source {
        private NullArtifactSource() {}

        @Override
        public Stream<Artifact> get() throws IOException {
            return Stream.empty();
        }
    }

    public abstract static class DelegatingArtifactSource implements Artifacts.Source {
        private final Artifacts.Source delegate;

        public DelegatingArtifactSource(final Artifacts.Source delegate) {
            this.delegate = requireNonNull(delegate, "delegate");
        }

        @Override
        public Stream<Artifact> get() throws IOException {
            return delegate.get();
        }

        @Override
        public void close() throws Exception {
            delegate.close();
        }
    }

    public static MatchingArtifactSource matchingArtifactSource(
            Predicate<Artifact> artifactMatcher, Artifacts.Source delegate) {
        requireNonNull(artifactMatcher, "artifactMatcher");
        requireNonNull(delegate, "delegate");
        return new MatchingArtifactSource(artifactMatcher, delegate);
    }

    public static class MatchingArtifactSource extends DelegatingArtifactSource {
        private final Predicate<Artifact> artifactMatcher;

        private MatchingArtifactSource(Predicate<Artifact> artifactMatcher, Artifacts.Source delegate) {
            super(delegate);
            this.artifactMatcher = artifactMatcher;
        }

        @Override
        public Stream<Artifact> get() throws IOException {
            return super.get().filter(artifactMatcher);
        }
    }

    public static MappingArtifactSource mappingArtifactSource(
            Function<Artifact, Artifact> artifactMapper, Artifacts.Source delegate) {
        requireNonNull(artifactMapper, "artifactMapper");
        requireNonNull(delegate, "delegate");
        return new MappingArtifactSource(artifactMapper, delegate);
    }

    public static class MappingArtifactSource extends DelegatingArtifactSource {
        private final Function<Artifact, Artifact> artifactMapper;

        private MappingArtifactSource(Function<Artifact, Artifact> artifactMapper, Artifacts.Source delegate) {
            super(delegate);
            this.artifactMapper = artifactMapper;
        }

        @Override
        public Stream<Artifact> get() throws IOException {
            return super.get().map(artifactMapper);
        }
    }

    public static Artifacts.Source gavArtifactSource(String gav) {
        requireNonNull(gav, "gav");
        return new GavArtifactSource(gav);
    }

    public static class GavArtifactSource implements Artifacts.Source {
        private final String gav;

        private GavArtifactSource(String gav) {
            this.gav = gav;
        }

        @Override
        public Stream<Artifact> get() throws IOException {
            return Stream.of(new DefaultArtifact(gav));
        }
    }

    public static Artifacts.Source gavsArtifactSource(String gavs) {
        requireNonNull(gavs, "gavs");
        return new GavsArtifactSource(gavs);
    }

    public static class GavsArtifactSource implements Artifacts.Source {
        private final String gavs;

        private GavsArtifactSource(String gavs) {
            this.gavs = gavs;
        }

        @Override
        public Stream<Artifact> get() throws IOException {
            return StringSlurper.slurp(gavs).stream().map(DefaultArtifact::new);
        }
    }

    public static Artifacts.Source concatArtifactSource(Collection<Artifacts.Source> sources) {
        requireNonNull(sources, "sources");
        return new ConcatArtifactSource(sources);
    }

    public static class ConcatArtifactSource implements Artifacts.Source {
        private final Collection<Artifacts.Source> sources;

        private ConcatArtifactSource(Collection<Artifacts.Source> sources) {
            this.sources = sources;
        }

        @Override
        public Stream<Artifact> get() throws IOException {
            Stream<Artifact> result = null;
            for (Artifacts.Source source : sources) {
                if (result == null) {
                    result = source.get();
                } else {
                    result = Stream.concat(result, source.get());
                }
            }
            return result;
        }

        @Override
        public void close() throws Exception {
            ArrayList<Exception> exceptions = new ArrayList<>();
            sources.forEach(s -> {
                try {
                    s.close();
                } catch (Exception e) {
                    exceptions.add(e);
                }
            });
            if (!exceptions.isEmpty()) {
                Exception e = new Exception("Could not close concat() sources: " + exceptions);
                exceptions.forEach(e::addSuppressed);
                throw e;
            }
        }
    }
}
