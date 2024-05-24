/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.internal.SpecParser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;

/**
 * Filter that matches artifact versions.
 */
public interface ArtifactVersionMatcher extends Predicate<Version> {
    static ArtifactVersionMatcher any() {
        return v -> true;
    }

    static ArtifactVersionMatcher not(ArtifactVersionMatcher matcher) {
        return new ArtifactVersionMatcher() {
            @Override
            public boolean test(Version version) {
                return !matcher.test(version);
            }
        };
    }

    static ArtifactVersionMatcher and(ArtifactVersionMatcher... matchers) {
        return and(Arrays.asList(matchers));
    }

    static ArtifactVersionMatcher and(Collection<ArtifactVersionMatcher> matchers) {
        return new ArtifactVersionMatcher() {
            @Override
            public boolean test(Version version) {
                for (ArtifactVersionMatcher matcher : matchers) {
                    if (!matcher.test(version)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    static ArtifactVersionMatcher or(ArtifactVersionMatcher... matchers) {
        return or(Arrays.asList(matchers));
    }

    static ArtifactVersionMatcher or(Collection<ArtifactVersionMatcher> matchers) {
        return new ArtifactVersionMatcher() {
            @Override
            public boolean test(Version version) {
                for (ArtifactVersionMatcher matcher : matchers) {
                    if (matcher.test(version)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    static ArtifactVersionMatcher gt(Version version) {
        return v -> version.compareTo(v) < 0;
    }

    static ArtifactVersionMatcher gte(Version version) {
        return v -> version.compareTo(v) <= 0;
    }

    static ArtifactVersionMatcher lt(Version version) {
        return v -> version.compareTo(v) > 0;
    }

    static ArtifactVersionMatcher lte(Version version) {
        return v -> version.compareTo(v) >= 0;
    }

    /**
     * A version matcher that filters out "preview" versions.
     */
    static ArtifactVersionMatcher noPreviews() {
        return v -> !isPreviewVersion(v.toString());
    }

    /**
     * A version matcher that filters out "snapshot" versions.
     */
    static ArtifactVersionMatcher noSnapshots() {
        return v -> !isSnapshotVersion(v.toString());
    }

    /**
     * Helper method: tells is a version string a "preview" version or not, as per Resolver version spec.
     *
     * @see <a href="https://maven.apache.org/resolver-archives/resolver-2.0.0-alpha-11/apidocs/org/eclipse/aether/util/version/package-summary.html">Resolver Generic Version spec</a>
     */
    static boolean isPreviewVersion(String version) {
        // most trivial "preview" version is 'a1'
        if (version.length() > 1) {
            String ver = version.toLowerCase(Locale.ENGLISH);
            // simple case: contains any of these
            if (ver.contains("alpha")
                    || ver.contains("beta")
                    || ver.contains("milestone")
                    || ver.contains("rc")
                    || ver.contains("cr")) {
                return true;
            }
            // complex case: contains 'a', 'b' or 'm' followed immediately by number
            for (char ch : new char[] {'a', 'b', 'm'}) {
                int idx = ver.lastIndexOf(ch);
                if (idx > -1 && ver.length() > idx + 1) {
                    if (Character.isDigit(ver.charAt(idx + 1))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Helper method: tells is a version string a "snapshot" version or not.
     */
    static boolean isSnapshotVersion(String version) {
        try {
            return new DefaultArtifact("g:a:" + version).isSnapshot();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    static ArtifactVersionMatcher build(Map<String, ?> properties, String spec) {
        requireNonNull(properties, "properties");
        requireNonNull(spec, "spec");
        ArtifactVersionMatcherBuilder builder = new ArtifactVersionMatcherBuilder(properties);
        SpecParser.parse(spec).accept(builder);
        return builder.build();
    }

    class ArtifactVersionMatcherBuilder extends SpecParser.Builder {
        private final VersionScheme versionScheme;

        public ArtifactVersionMatcherBuilder(Map<String, ?> properties) {
            super(properties);
            this.versionScheme = new GenericVersionScheme();
        }

        @Override
        protected void processOp(SpecParser.Node node) {
            switch (node.getValue()) {
                case "any":
                    params.add(any());
                    break;
                case "noPreviews":
                    params.add(noPreviews());
                    break;
                case "noSnapshots":
                    params.add(noSnapshots());
                    break;
                case "not": {
                    params.add(not(artifactVersionMatcherParam(node.getValue())));
                    break;
                }
                case "and": {
                    params.add(and(artifactVersionMatcherParams(node.getValue())));
                    break;
                }
                case "or": {
                    params.add(or(artifactVersionMatcherParams(node.getValue())));
                    break;
                }
                case "gt": {
                    params.add(gt(versionParam(node.getValue())));
                    break;
                }
                case "gte": {
                    params.add(gte(versionParam(node.getValue())));
                    break;
                }
                case "lt": {
                    params.add(lt(versionParam(node.getValue())));
                    break;
                }
                case "lte": {
                    params.add(lte(versionParam(node.getValue())));
                    break;
                }
                default:
                    throw new IllegalArgumentException("unknown op " + node.getValue());
            }
        }

        private Version versionParam(String op) {
            try {
                return versionScheme.parseVersion(stringParam(op));
            } catch (InvalidVersionSpecificationException e) {
                throw new IllegalArgumentException("invalid version parameter for " + op, e);
            }
        }

        private ArtifactVersionMatcher artifactVersionMatcherParam(String op) {
            if (params.isEmpty()) {
                throw new IllegalArgumentException("bad parameter count for " + op);
            }
            return (ArtifactVersionMatcher) params.remove(params.size() - 1);
        }

        private List<ArtifactVersionMatcher> artifactVersionMatcherParams(String op) {
            ArrayList<ArtifactVersionMatcher> result = new ArrayList<>();
            while (!params.isEmpty()) {
                if (params.get(params.size() - 1) instanceof ArtifactVersionMatcher) {
                    result.add(artifactVersionMatcherParam(op));
                } else {
                    break;
                }
            }
            return result;
        }

        public ArtifactVersionMatcher build() {
            if (params.size() != 1) {
                throw new IllegalArgumentException("bad spec");
            }
            return (ArtifactVersionMatcher) params.get(0);
        }
    }
}
