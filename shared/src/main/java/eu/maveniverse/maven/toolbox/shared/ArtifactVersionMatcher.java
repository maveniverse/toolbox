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
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import org.eclipse.aether.artifact.DefaultArtifact;
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

    static ArtifactVersionMatcher eq(Version version) {
        return version::equals;
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
     * A version matcher that matches "preview" versions.
     */
    static ArtifactVersionMatcher preview() {
        return ArtifactVersionMatcher::isPreviewVersion;
    }

    /**
     * A version matcher that matches "snapshot" versions.
     */
    static ArtifactVersionMatcher snapshot() {
        return ArtifactVersionMatcher::isSnapshotVersion;
    }

    /**
     * A version matcher shorthand, that prevents matching of "preview" versions. Shorthand for "not(preview())".
     */
    static ArtifactVersionMatcher noPreviews() {
        return not(preview());
    }

    /**
     * A version matcher shorthand, that prevents matching of "snapshot" versions. Shorthand for "not(snapshot())".
     */
    static ArtifactVersionMatcher noSnapshots() {
        return not(snapshot());
    }

    /**
     * A version matcher shorthand, that prevents matching of "snapshot" and "preview" versions. Shorthand for "and(not(snapshot()), not(preview())".
     */
    static ArtifactVersionMatcher noSnapshotsAndPreviews() {
        return and(noPreviews(), noSnapshots());
    }

    /**
     * Helper method: tells is a version string a "preview" version or not, as per Resolver version spec.
     *
     * @see <a href="https://maven.apache.org/resolver-archives/resolver-2.0.0-alpha-11/apidocs/org/eclipse/aether/util/version/package-summary.html">Resolver Generic Version spec</a>
     */
    static boolean isPreviewVersion(Version version) {
        return isPreviewVersion(version.toString());
    }

    private static boolean isPreviewVersion(String version) {
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
    static boolean isSnapshotVersion(Version version) {
        return isSnapshotVersion(version.toString());
    }

    private static boolean isSnapshotVersion(String version) {
        try {
            return new DefaultArtifact("g:a:" + version).isSnapshot();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    static ArtifactVersionMatcher build(VersionScheme versionScheme, Map<String, ?> properties, String spec) {
        requireNonNull(properties, "properties");
        requireNonNull(spec, "spec");
        ArtifactVersionMatcherBuilder builder = new ArtifactVersionMatcherBuilder(versionScheme, properties);
        SpecParser.parse(spec).accept(builder);
        return builder.build();
    }

    class ArtifactVersionMatcherBuilder extends SpecParser.Builder {
        public ArtifactVersionMatcherBuilder(VersionScheme versionScheme, Map<String, ?> properties) {
            super(versionScheme, properties);
        }

        @Override
        protected void processOp(SpecParser.Node node) {
            switch (node.getValue()) {
                case "any":
                    params.add(any());
                    break;
                case "preview":
                    params.add(preview());
                    break;
                case "snapshot":
                    params.add(snapshot());
                    break;
                case "not":
                    params.add(not(typedParam(ArtifactVersionMatcher.class, node.getValue())));
                    break;
                case "and":
                    params.add(and(typedParams(ArtifactVersionMatcher.class, node.getValue())));
                    break;
                case "or":
                    params.add(or(typedParams(ArtifactVersionMatcher.class, node.getValue())));
                    break;
                case "eq":
                    params.add(eq(versionParam(node.getValue())));
                    break;
                case "gt":
                    params.add(gt(versionParam(node.getValue())));
                    break;
                case "gte":
                    params.add(gte(versionParam(node.getValue())));
                    break;
                case "lt":
                    params.add(lt(versionParam(node.getValue())));
                    break;
                case "lte":
                    params.add(lte(versionParam(node.getValue())));
                    break;
                case "noPreviews":
                    params.add(noPreviews());
                    break;
                case "noSnapshots":
                    params.add(noSnapshots());
                    break;
                case "noSnapshotsAndPreviews":
                    params.add(noSnapshotsAndPreviews());
                    break;
                default:
                    throw new IllegalArgumentException("unknown op " + node.getValue());
            }
        }

        public ArtifactVersionMatcher build() {
            return build(ArtifactVersionMatcher.class);
        }
    }
}
