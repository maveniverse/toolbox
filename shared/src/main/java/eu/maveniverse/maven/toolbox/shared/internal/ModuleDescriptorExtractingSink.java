/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.output.Output;
import java.io.IOException;
import java.lang.module.FindException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;

/**
 * Sink that extracts module descriptors from artifacts.
 */
public final class ModuleDescriptorExtractingSink implements Artifacts.Sink, DependencyVisitor {
    public interface ModuleDescriptor {
        boolean available();

        String name();

        boolean automatic();

        String moduleNameSource();
    }

    private final Output output;
    private final ConcurrentMap<Artifact, ModuleDescriptor> moduleDescriptors;

    public ModuleDescriptorExtractingSink(Output output) {
        this.output = requireNonNull(output, "output");
        this.moduleDescriptors = new ConcurrentHashMap<>();
    }

    @Override
    public void accept(Artifact artifact) throws IOException {
        if (artifact.getFile() != null) {
            moduleDescriptors.computeIfAbsent(
                    artifact, k -> getModuleDescriptor(artifact.getFile().toPath()));
        }
    }

    public String formatString(ModuleDescriptor moduleDescriptor) {
        String moduleInfo;
        if (moduleDescriptor == null || !moduleDescriptor.available()) {
            moduleInfo = "-- n/a";
        } else {
            moduleInfo = "-- module " + moduleDescriptor.name();
            if (moduleDescriptor.automatic()) {
                if ("MANIFEST".equals(moduleDescriptor.moduleNameSource())) {
                    moduleInfo += " [auto]";
                } else {
                    moduleInfo += " (auto)";
                }
            }
        }
        return moduleInfo;
    }

    public ModuleDescriptor getModuleDescriptor(Artifact artifact) {
        return moduleDescriptors.get(artifact);
    }

    public Map<Artifact, ModuleDescriptor> getModuleDescriptors() {
        return Collections.unmodifiableMap(moduleDescriptors);
    }

    private ModuleDescriptor getModuleDescriptor(Path artifactPath) {
        ModuleDescriptorImpl moduleDescriptor = NOT_AVAILABLE;
        try {
            ModuleFinder moduleFinder = ModuleFinder.of(artifactPath);
            Set<ModuleReference> moduleReferences = moduleFinder.findAll();

            // moduleReferences can be empty when referring to target/classes without module-info.class
            if (!moduleReferences.isEmpty()) {
                ModuleReference moduleReference = moduleReferences.iterator().next();
                java.lang.module.ModuleDescriptor moduleDescriptorInstance = moduleReference.descriptor();

                moduleDescriptor = new ModuleDescriptorImpl();
                moduleDescriptor.name = moduleDescriptorInstance.name();
                moduleDescriptor.automatic = moduleDescriptorInstance.isAutomatic();

                if (moduleDescriptor.automatic) {
                    if (Files.isRegularFile(artifactPath)) {
                        try (JarFile jarFile = new JarFile(artifactPath.toFile())) {
                            Manifest manifest = jarFile.getManifest();

                            if (manifest != null
                                    && manifest.getMainAttributes().getValue("Automatic-Module-Name") != null) {
                                moduleDescriptor.moduleNameSource = "MANIFEST";
                            } else {
                                moduleDescriptor.moduleNameSource = "FILENAME";
                            }
                        } catch (IOException e) {
                            // noop
                        }
                    }
                }
            }
        } catch (SecurityException | IllegalArgumentException e) {
            output.warn("Ignored Exception:", e);
        } catch (FindException e) {
            Throwable cause = e.getCause();
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            output.warn("Can't extract module name from {}:", artifactPath.getFileName(), cause);
        }
        return moduleDescriptor;
    }

    @Override
    public boolean visitEnter(DependencyNode dependencyNode) {
        if (dependencyNode.getDependency() != null) {
            try {
                accept(dependencyNode.getDependency().getArtifact());
            } catch (IOException e) {
                output.warn("IO problem: ", e);
            }
        }
        return true;
    }

    @Override
    public boolean visitLeave(DependencyNode dependencyNode) {
        return true;
    }

    public Function<DependencyNode, String> decorator() {
        return node -> {
            if (node != null && node.getDependency() != null) {
                ModuleDescriptor moduleDescriptor =
                        getModuleDescriptor(node.getDependency().getArtifact());
                if (moduleDescriptor != null) {
                    return formatString(moduleDescriptor);
                }
            }
            return null;
        };
    }

    private static final ModuleDescriptorImpl NOT_AVAILABLE = new ModuleDescriptorImpl() {
        @Override
        public boolean available() {
            return false;
        }

        @Override
        public String name() {
            return "n/a";
        }

        @Override
        public boolean automatic() {
            return false;
        }

        @Override
        public String moduleNameSource() {
            return "";
        }
    };

    private static class ModuleDescriptorImpl implements ModuleDescriptor {
        String name;
        boolean automatic = true;
        String moduleNameSource;

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean automatic() {
            return automatic;
        }

        @Override
        public String moduleNameSource() {
            return moduleNameSource;
        }
    }
}
