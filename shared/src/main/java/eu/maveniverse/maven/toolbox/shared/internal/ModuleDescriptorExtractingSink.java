/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sink that extracts module descriptors from artifacts.
 */
public final class ModuleDescriptorExtractingSink extends ArtifactSink implements DependencyVisitor {
    public interface ModuleDescriptor {
        String name();

        boolean automatic();

        String moduleNameSource();
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ConcurrentMap<Artifact, ModuleDescriptor> moduleDescriptors;

    public ModuleDescriptorExtractingSink() {
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
        String moduleInfo = "-- module " + moduleDescriptor.name();
        if (moduleDescriptor.automatic()) {
            if ("MANIFEST".equals(moduleDescriptor.moduleNameSource())) {
                moduleInfo += " [auto]";
            } else {
                moduleInfo += " (auto)";
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
        ModuleDescriptorImpl moduleDescriptor = null;
        try {
            // Use Java9 code to get moduleName, don't try to do it better with own implementation
            Class<?> moduleFinderClass = Class.forName("java.lang.module.ModuleFinder");

            Method ofMethod = moduleFinderClass.getMethod("of", java.nio.file.Path[].class);
            Object moduleFinderInstance = ofMethod.invoke(null, new Object[] {new java.nio.file.Path[] {artifactPath}});

            Method findAllMethod = moduleFinderClass.getMethod("findAll");
            Set<Object> moduleReferences = (Set<Object>) findAllMethod.invoke(moduleFinderInstance);

            // moduleReferences can be empty when referring to target/classes without module-info.class
            if (!moduleReferences.isEmpty()) {
                Object moduleReference = moduleReferences.iterator().next();
                Method descriptorMethod = moduleReference.getClass().getMethod("descriptor");
                Object moduleDescriptorInstance = descriptorMethod.invoke(moduleReference);

                Method nameMethod = moduleDescriptorInstance.getClass().getMethod("name");
                String name = (String) nameMethod.invoke(moduleDescriptorInstance);

                moduleDescriptor = new ModuleDescriptorImpl();
                moduleDescriptor.name = name;

                Method isAutomaticMethod = moduleDescriptorInstance.getClass().getMethod("isAutomatic");
                moduleDescriptor.automatic = (Boolean) isAutomaticMethod.invoke(moduleDescriptorInstance);

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
        } catch (ClassNotFoundException
                | SecurityException
                | IllegalAccessException
                | IllegalArgumentException
                | NoSuchMethodException e) {
            logger.debug("Ignored Exception:", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            logger.debug("Can't extract module name from {}:", artifactPath.getFileName(), cause);
        }
        return moduleDescriptor;
    }

    @Override
    public boolean visitEnter(DependencyNode dependencyNode) {
        if (dependencyNode.getDependency() != null) {
            try {
                accept(dependencyNode.getDependency().getArtifact());
            } catch (IOException e) {
                logger.warn("IO problem: ", e);
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

    private static class ModuleDescriptorImpl implements ModuleDescriptor {
        String name;
        boolean automatic = true;
        String moduleNameSource;

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
