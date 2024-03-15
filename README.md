# Toolbox

The Maveniverse Toolbox started as manifold project. It aims to:
* replace `MIMA CLI` (basically continue along its goals)
* replace `maven-dependency-plugin`, providing similar (sub)set of Mojos
* showcase how MIMA helps to write reusable Resolver code that runs in Maven (as Mojos) but also outside of Maven as well
* and as a side effect, it codifies dependency scopes and resolution scopes (they are not only String labels anymore)

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
* list-plugins
* properties
* repocopy
* tree
* unpack

plus what MIMA CLI already supports.