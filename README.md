# Toolbox

The Maveniverse Toolbox started as manifold project. It aims to:
* replace MIMA CLI (basically continue along its goals)
* replace maven-dependency-plugin, providing similar (sub)set of Mojos
* showcase how MIMA helps to write reusable Resolver code that runs in Mojos but also outside of them
* and as a side effect, it codifies dependency scopes and resolution scopes (they are not only String labels anymore)

Structure of the project:
* shared is a reused library module, that depends on MIMA Context only, and implements all the logic
* maven-plugin is a Maven Plugin module, that exposes Toolbox operations as Mojos. Each Mojo comes in two
"flavors": without prefix, that requires project, and uses MavenProject to get the data for requests, and "gav-" 
prefixed ones, that do not require project, and all data should be parametrized (like "gav" itself).
* Toolbox CLI implements SAME operations as maven-plugin, but in form of a CLI.

Project Goals:
* classpath
* copy
* get
* list-repositories
* properties
* repocopy
* tree
* unpack
