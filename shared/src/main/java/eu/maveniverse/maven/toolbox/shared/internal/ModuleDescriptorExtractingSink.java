/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.ArtifactSink;
import eu.maveniverse.maven.toolbox.shared.Output;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sink that extracts module descriptors from artifacts.
 */
public final class ModuleDescriptorExtractingSink implements ArtifactSink {
    public interface ModuleDescriptor {
        String name();

        boolean automatic();

        String moduleNameSource();
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Output output;
    private final ConcurrentMap<Artifact, ModuleDescriptor> moduleDescriptors;

    public ModuleDescriptorExtractingSink(Output output) {
        this.output = requireNonNull(output, "output");
        this.moduleDescriptors = new ConcurrentHashMap<>();
    }

    @Override
    public void accept(Artifact artifact) throws IOException {
        if (artifact.getFile() != null) {
            moduleDescriptors.computeIfAbsent(artifact, k -> getModuleDescriptor(artifact.getFile()));
        }
    }

    @Override
    public void close() throws Exception {
        for (Map.Entry<Artifact, ModuleDescriptor> entry : moduleDescriptors.entrySet()) {
            String moduleInfo = "";
            if (entry.getValue() != null) {
                ModuleDescriptor moduleDescriptor = entry.getValue();
                moduleInfo = "-- module " + moduleDescriptor.name();
                if (moduleDescriptor.automatic()) {
                    if ("MANIFEST".equals(moduleDescriptor.moduleNameSource())) {
                        moduleInfo += " [auto]";
                    } else {
                        moduleInfo += " (auto)";
                    }
                }
            }
            if (output.isVerbose()) {
                output.verbose(
                        "{} {} -> {}",
                        entry.getKey(),
                        moduleInfo,
                        entry.getKey().getFile());
            } else {
                output.normal("{} {}", entry.getKey(), moduleInfo);
            }
        }
    }

    public ModuleDescriptor getModuleDescriptor(Artifact artifact) {
        return moduleDescriptors.get(artifact);
    }

    public Map<Artifact, ModuleDescriptor> getModuleDescriptors() {
        return Collections.unmodifiableMap(moduleDescriptors);
    }

    private ModuleDescriptor getModuleDescriptor(File artifactFile) {
        ModuleDescriptorImpl moduleDescriptor = null;
        try {
            // Use Java9 code to get moduleName, don't try to do it better with own implementation
            Class<?> moduleFinderClass = Class.forName("java.lang.module.ModuleFinder");

            Path path = artifactFile.toPath();

            Method ofMethod = moduleFinderClass.getMethod("of", java.nio.file.Path[].class);
            Object moduleFinderInstance = ofMethod.invoke(null, new Object[] {new java.nio.file.Path[] {path}});

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
                    if (artifactFile.isFile()) {
                        try (JarFile jarFile = new JarFile(artifactFile)) {
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
            logger.debug("Can't extract module name from {}:", artifactFile.getName(), cause);
        }
        return moduleDescriptor;
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
