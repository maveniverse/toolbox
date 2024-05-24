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
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import org.eclipse.aether.version.Version;

/**
 * Filter that matches artifact versions.
 */
public interface ArtifactVersionMatcher extends Predicate<Version> {
    /**
     * A version matcher "any".
     */
    static ArtifactVersionMatcher any() {
        return v -> true;
    }

    /**
     * A version matcher that filters out "preview" versions.
     */
    static ArtifactVersionMatcher noPreviews() {
        return v -> !isPreviewVersion(v.toString());
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

    static ArtifactVersionMatcher build(Map<String, ?> properties, String spec) {
        requireNonNull(properties, "properties");
        requireNonNull(spec, "spec");
        ArtifactVersionMatcherBuilder builder = new ArtifactVersionMatcherBuilder(properties);
        SpecParser.parse(spec).accept(builder);
        return builder.build();
    }

    class ArtifactVersionMatcherBuilder extends SpecParser.Builder {
        public ArtifactVersionMatcherBuilder(Map<String, ?> properties) {
            super(properties);
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
                default:
                    throw new IllegalArgumentException("unknown op " + node.getValue());
            }
        }

        public ArtifactVersionMatcher build() {
            if (params.size() != 1) {
                throw new IllegalArgumentException("bad spec");
            }
            return (ArtifactVersionMatcher) params.get(0);
        }
    }
}
