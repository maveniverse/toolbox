# Toolbox

The Toolbox project started with manifold aims:
* replace `MIMA CLI`, provide drop-in replacement, but also continue improving it.
* provide replacement for `maven-dependency-plugin`, offering similar (sub)set of Mojos
* showcase how MIMA helps to write reusable Resolver code that runs in Maven (as Mojos) but also outside of Maven as well

Structure of the project:
* Module "shared" is a library module, that depends on MIMA `Context` only, and implements all the logic. This module is then reused in modules below.
* Module "maven-plugin" is a Maven Plugin, that exposes Toolbox operations as Mojos. Each Mojo comes in two
"flavors": without prefix (i.e. "tree"), that requires project, and uses `MavenProject` to get the data for requests, and "gav-" 
prefixed ones (i.e. "gav-tree"), that do not require project, and is able to target any existing Artifact out there.
* The "cli" is the Toolbox CLI, it implements same operations as "maven-plugin", but in a form of a CLI application (similarly to MIMA CLI).

Project Goals:
* classpath
* copy
* get
* list-repositories
* properties
* repocopy
* tree
* unpack
* measure (dep count, size, repo count?)
* list-available-plugins (G given or settings one, A and latest V), groupIds from settings or explicitly given
* sources (P or GAV w/wo transitive deps)
* javadoc (P or GAV w/wo transitive deps)

plus what MIMA CLI already supports.

## TODOs

* sort out proper error handling
* unpack?