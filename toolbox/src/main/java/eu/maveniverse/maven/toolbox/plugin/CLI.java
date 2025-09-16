/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import eu.maveniverse.maven.toolbox.plugin.gav.GavArtifactPathMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavClasspathDiffMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavClasspathMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavCopyGavMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavCopyMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavCopyRecordedMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavCopyTransitiveMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavDeployMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavDeployRecordedMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavDirtyTreeMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavDmListMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavDmTreeMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavDumpMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavEffectiveModelMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavExistsMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavFlattenBomMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavIdentifyMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavInstallMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavLibYearMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavListAvailablePluginsMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavListMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavListRepositoriesMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavLocalRepositoryPathMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavMetadataPathMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavRecordMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavReplMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavResolveMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavResolveTransitiveMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavSearchMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavTreeDiffMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavTreeMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavVerifyMojo;
import eu.maveniverse.maven.toolbox.plugin.gav.GavVersionsMojo;
import eu.maveniverse.maven.toolbox.plugin.hello.AddCoreExtension;
import eu.maveniverse.maven.toolbox.plugin.hello.AddManagedDependency;
import eu.maveniverse.maven.toolbox.plugin.hello.AddManagedPlugin;
import eu.maveniverse.maven.toolbox.plugin.hello.NewProject;
import eu.maveniverse.maven.toolbox.plugin.hello.NewSubProject;
import eu.maveniverse.maven.toolbox.plugin.hello.RemoveCoreExtension;
import eu.maveniverse.maven.toolbox.shared.Result;
import picocli.CommandLine;

/**
 * Main CLI class.
 */
@CommandLine.Command(
        name = "toolbox",
        subcommands = {
            GavArtifactPathMojo.class,
            GavClasspathDiffMojo.class,
            GavClasspathMojo.class,
            GavCopyGavMojo.class,
            GavCopyMojo.class,
            GavCopyRecordedMojo.class,
            GavCopyTransitiveMojo.class,
            GavDeployMojo.class,
            GavDeployRecordedMojo.class,
            GavDirtyTreeMojo.class,
            GavDmListMojo.class,
            GavDmTreeMojo.class,
            GavDumpMojo.class,
            GavEffectiveModelMojo.class,
            GavExistsMojo.class,
            GavFlattenBomMojo.class,
            GavIdentifyMojo.class,
            GavInstallMojo.class,
            GavLibYearMojo.class,
            GavListAvailablePluginsMojo.class,
            GavListMojo.class,
            GavListRepositoriesMojo.class,
            GavLocalRepositoryPathMojo.class,
            GavMetadataPathMojo.class,
            GavRecordMojo.class,
            GavReplMojo.class,
            GavResolveMojo.class,
            GavResolveTransitiveMojo.class,
            GavSearchMojo.class,
            GavTreeDiffMojo.class,
            GavTreeMojo.class,
            GavVerifyMojo.class,
            GavVersionsMojo.class,
            AddCoreExtension.class,
            AddManagedDependency.class,
            AddManagedPlugin.class,
            NewProject.class,
            NewSubProject.class,
            RemoveCoreExtension.class
        },
        versionProvider = CLI.class,
        description = "Toolbox CLI",
        mixinStandardHelpOptions = true)
public class CLI extends MojoSupport {
    @Override
    protected Result<String> doExecute() throws Exception {
        return new GavReplMojo().doExecute();
    }

    public static void main(String... args) {
        System.exit(new CommandLine(new CLI()).execute(args));
    }
}
