# Toolbox

The Toolbox project started with manifold aims:
* replace `MIMA CLI`, provide drop-in replacement, but also continue improving it.
* provide replacement for `maven-dependency-plugin`, offering similar (sub)set of Mojos
* showcase how MIMA helps to write reusable Resolver code that runs in Maven (as Mojos) but also outside of Maven as well
* was birthplace of Resolver 2.x `ScopeManager` (that is now part of Resolver 2.x, while Resolver 1.x circumvention is present in Toolbox that supports Maven 3.6+), fixing MNG-8041

Structure of the project:
* Module "shared" is a reusable library module, that depends on MIMA `Context` only (and Resolver APIs), and implements all the logic.
* Module "toolbox" is a Maven Plugin and a CLI at the same time, that exposes Toolbox operations as Mojos and commands. Each Mojo comes in two
"flavors": without prefix (i.e. "tree"), that requires project, and uses `MavenProject` to get the data for requests, and "gav-" 
prefixed ones (i.e. "gav-tree"), that do not require project, and is able to target any existing Artifact out there.

To use it as Maven plugin, introspect available Mojos and parameters:
```
$ mvn eu.maveniverse.maven.plugins:toolbox:help -Ddetail
```
Or, to use it as CLI:
```
$ jbang toolbox@maveniverse
```
or you can download the CLI JAR from Maven Central and run it directly.
