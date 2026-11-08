# Toolbox spec languages

Many Toolbox commands accept a *spec* string (options like `--artifactVersionMatcherSpec`, `--sinkSpec`, or the
source/sink parameters of `gav-copy`). Every spec is parsed by the same tiny parser
(`eu.maveniverse.maven.toolbox.shared.internal.SpecParser`) and then interpreted by a class-specific builder. This page
documents the common syntax and every "language" built on top of it.

| Language                                        | Produces                       | Example                                                 |
|-------------------------------------------------|--------------------------------|---------------------------------------------------------|
| [Artifact version matcher](#artifact-version-matcher)   | `Predicate<Version>`           | `and(noSnapshotsAndPreviews(), constraint('[,4)'))`     |
| [Artifact version selector](#artifact-version-selector) | version string                 | `contextualSnapshotsAndPreviews(last())`                |
| [Artifact matcher](#artifact-matcher)           | `Predicate<Artifact>`          | `and(artifact(org.apache.maven*), not(snapshot()))`     |
| [Dependency matcher](#dependency-matcher)       | `Predicate<Dependency>`        | `and(scopeIncluded(compile,runtime), optional(false))`  |
| [Artifact name mapper](#artifact-name-mapper)   | `String` from artifact         | `compose(fixed(libs/), AbVE())`                         |
| [Artifact mapper](#artifact-mapper)             | `Artifact` → `Artifact`        | `compose(omitClassifier(), baseVersion())`              |
| [Dependency mapper](#dependency-mapper)         | `Dependency` → `Dependency`    | `compose(scope(compile), optional(false))`              |
| [Artifact key factory](#artifact-key-factory)   | key `String`                   | `versionlessId()`                                       |
| [Artifact differentiator](#artifact-differentiator) | key `String`               | `majorVersion()`                                        |
| [Artifact source](#artifact-source)             | stream of artifacts            | `resolveTransitive(gav(g:a:1.0), test)`                 |
| [Artifact sink](#artifact-sink)                 | consumer of artifacts          | `matching(not(snapshot()), flat(target/libs))`          |
| [Dependency sink](#dependency-sink)             | consumer of dependencies       | `matching(scopeIncluded(test), counting())`             |

## Common syntax

A spec is a tree of **operations** and **literals**:

```
spec    := op
op      := name '(' [ arg ( ',' arg )* ] ')'
arg     := op | literal
literal := bare | quoted
```

* The root of a spec **must** be an operation: `any()` is a spec, `any` is not.
* Operation names are case-sensitive. An operation without arguments still needs `()`.
* **Bare literals** may contain letters, digits and the characters `* : . - / \ $ { } [ ]`. Whitespace outside of
  quotes is ignored (also *inside* bare literals: `a b` is the same as `ab`).
* **Quoted literals** are enclosed in single quotes and may contain any character except `'` (there is no escaping).
  Whitespace is preserved. Use them whenever a value contains `(`, `)`, `,`, `+`, `_`, `@`, spaces, etc., for example
  version ranges: `constraint('[,32.1)')`. `''` is the empty string.
* **Property references**: a literal that is *exactly* `${name}` is replaced by the value of property `name` (the
  resolver session config properties, which include Java system and user properties). Unknown properties become the
  empty string. Partial interpolation (`foo-${name}`) is **not** supported. Note: a few operations read some arguments
  raw, without interpolation; these are marked with *(raw)* below.
* Syntax errors (bad characters, unbalanced parentheses, dangling commas, unterminated quote) and unknown operations
  fail with an "Invalid ... spec" error.

Some operations take *another language* as argument; e.g. `matching(...)` in a sink takes an artifact matcher as
first argument and a sink as second. This is noted in the argument descriptions below.

### Known quirks

* Variadic arguments (`X...`) are collected in reverse order. For `and`/`or`/`tee` this rarely matters, but
  `compose(...)` of artifact and dependency mappers applies its arguments **right to left**.

Notation in the tables: `str` is a literal, `bool` is a literal `true`/`false` (anything else is `false`), `X...`
means zero or more arguments of type `X`, `[x]` means optional.

---

## Artifact version matcher

`ArtifactVersionMatcher` — a predicate over versions. Used for filtering available versions (e.g. options
`--artifactVersionMatcherSpec`, default usually `noSnapshotsAndPreviews()`, and `--parentVersionMatcherSpec`).
Versions are parsed using the session version scheme.

| Operation                  | Matches                                                                                     |
|----------------------------|---------------------------------------------------------------------------------------------|
| `any()`                    | every version                                                                               |
| `snapshot()`               | snapshot versions (`-SNAPSHOT` or timestamped)                                              |
| `preview()`                | "preview" versions: contains `alpha`, `beta`, `milestone`, `rc` or `cr`, or `a`/`b`/`m` immediately followed by a digit (case-insensitive) |
| `noSnapshots()`            | shorthand for `not(snapshot())`                                                             |
| `noPreviews()`             | shorthand for `not(preview())`                                                              |
| `noSnapshotsAndPreviews()` | shorthand for `and(noSnapshots(), noPreviews())`                                            |
| `eq(str)`                  | versions equal to given version                                                             |
| `gt(str)` / `gte(str)`     | versions greater than / greater or equal to given version                                   |
| `lt(str)` / `lte(str)`     | versions less than / less or equal to given version                                         |
| `range(str)`               | versions contained in given version range, e.g. `range('[1.0,2.0)')`                        |
| `constraint(str)`          | versions contained in given version constraint (a range, or a plain version), e.g. `constraint('[,32.1)')` |
| `not(matcher)`             | negation                                                                                    |
| `and(matcher...)`          | all matchers match                                                                          |
| `or(matcher...)`           | any matcher matches                                                                         |

Examples:

```
noSnapshotsAndPreviews()
and(noSnapshotsAndPreviews(), lt(4.0.0))
and(noPreviews(), constraint('[3.9,4)'))
or(eq(1.0), eq(2.0))
```

## Artifact version selector

`ArtifactVersionSelector` — given an artifact and the **ascending sorted** list of its available versions, selects one
version. Used by options `--artifactVersionSelectorSpec` (default usually `contextualSnapshotsAndPreviews()`). Every
selector falls back to the artifact's own version when it cannot select anything (e.g. empty list).

| Operation                                      | Selects                                                                         |
|------------------------------------------------|---------------------------------------------------------------------------------|
| `identity()`                                   | the artifact's current version                                                  |
| `first()`                                      | the first (lowest) version                                                      |
| `last()`                                       | the last (highest) version                                                      |
| `prev()`                                       | the highest version lower than the current one                                  |
| `next()`                                       | the lowest version higher than the current one                                  |
| `major()`                                      | the last version with the same major (prefix up to first `.`) as the current one; if the current version has no major part, does `last()` |
| `minor()`                                      | the last version with the same major.minor (prefix up to second `.`); if the current version has no minor part, does `major()` |
| `filtered(versionMatcher, selector)`           | applies `selector` to versions accepted by the [version matcher](#artifact-version-matcher) |
| `noSnapshots(selector)`                        | `selector` applied to non-snapshot versions                                     |
| `noPreviews(selector)`                         | `selector` applied to non-preview versions                                      |
| `noSnapshotsAndPreviews(selector)`             | `selector` applied to versions that are neither snapshots nor previews          |
| `contextualSnapshotsAndPreviews([selector])`   | filtering depends on current version: if it is a snapshot, no filtering; if it is a preview, snapshots are filtered out; otherwise both snapshots and previews are filtered out. Then `selector` is applied (default `major()`) |

Examples:

```
last()
contextualSnapshotsAndPreviews(last())
filtered(lt(4.0.0), noSnapshotsAndPreviews(last()))
```

## Artifact matcher

`ArtifactMatcher` — a predicate over artifacts.

| Operation              | Matches                                                                                   |
|------------------------|-------------------------------------------------------------------------------------------|
| `any()`                | every artifact                                                                            |
| `none()`               | no artifact                                                                               |
| `snapshot()`           | snapshot artifacts                                                                        |
| `withoutClassifier()`  | artifacts with no (or blank) classifier                                                   |
| `artifact(str)`        | artifacts matching a coordinate pattern, see below                                        |
| `unique()`             | **stateful**: matches an artifact only the first time it is seen (keyed by full coordinates) |
| `uniqueBy(nameMapper)` | **stateful**: like `unique()`, keyed by the given [artifact name mapper](#artifact-name-mapper), e.g. `uniqueBy(GAKey())` |
| `not(matcher)`         | negation                                                                                  |
| `and(matcher...)`      | all match                                                                                 |
| `or(matcher...)`       | any matches                                                                               |

Coordinate patterns for `artifact(...)`; missing parts mean `*`:

| Pattern form                                       | Example                     |
|----------------------------------------------------|-----------------------------|
| `groupId`                                          | `org.apache.maven`          |
| `groupId:artifactId`                               | `org.apache.maven:maven-core` |
| `groupId:artifactId:version`                       | `org.slf4j:*:2*`            |
| `groupId:artifactId:extension:version`             | `*:*:pom:*`                 |
| `groupId:artifactId:classifier:extension:version`  | `*:*:sources:jar:*`         |

Each part is matched with a simple glob: `*` (anything), `foo*` (starts with), `*foo` (ends with), `*foo*`
(contains), otherwise exact match. Note that the 5-part form is **classifier before extension**.

Examples:

```
artifact(org.apache.maven*)
and(artifact(org.slf4j), not(snapshot()))
and(withoutClassifier(), uniqueBy(GAKey()))
```

## Dependency matcher

`DependencyMatcher` — a predicate over dependencies (artifact + scope + optional flag). Used by tree-related commands
(e.g. `gav-tree`) and by [dependency sinks](#dependency-sink).

| Operation                | Matches                                                              |
|--------------------------|----------------------------------------------------------------------|
| `any()`                  | every dependency                                                     |
| `none()`                 | no dependency                                                        |
| `scopeIncluded(str...)`  | dependencies whose scope is one of the given scopes                  |
| `scopeExcluded(str...)`  | dependencies whose scope is none of the given scopes                 |
| `optional(bool)`         | dependencies whose optional flag equals the given value              |
| `artifact(str)`          | dependencies whose artifact matches the coordinate pattern (same as `artifact(...)` of [artifact matcher](#artifact-matcher)) |
| `not(matcher)`           | negation                                                             |
| `and(matcher...)`        | all match                                                            |
| `or(matcher...)`         | any matches                                                          |

Examples:

```
scopeExcluded(test,provided)
and(scopeIncluded(compile,runtime), optional(false), not(artifact(org.slf4j)))
```

## Artifact name mapper

`ArtifactNameMapper` — maps an artifact to a string; used for file names (`flat(...)`, `unpack(...)` sinks) and keys
(`uniqueBy(...)`). Letters stand for **G**roupId, **A**rtifactId, **V**ersion, **bV** (base version, i.e.
`1.0-SNAPSHOT` instead of `1.0-20240101.123456-1`), **C**lassifier and **E**xtension.

Single fields:

| Operation        | Result                                                   |
|------------------|----------------------------------------------------------|
| `G()` `A()` `V()` `bV()` `C()` `E()` | the corresponding single field       |
| `P(key, default)`| artifact property `key`, or `default` if absent          |

Keys (colon separated):

| Operation    | Result                                     |
|--------------|--------------------------------------------|
| `GAKey()`    | `g:a`                                      |
| `GAVKey()`   | `g:a:v`                                    |
| `GAbVKey()`  | `g:a:bV`                                   |
| `GACEVKey()` | full artifact string `g:a:e[:c]:v`         |

File names (classifier part including its `-` is omitted when the classifier is empty):

| Operation   | Result                  | Operation   | Result               |
|-------------|-------------------------|-------------|----------------------|
| `GACVE()`   | `g.a[-c]-v.e`           | `ACVE()`    | `a[-c]-v.e`          |
| `GACbVE()`  | `g.a[-c]-bV.e`          | `ACbVE()`   | `a[-c]-bV.e`         |
| `GACE()`    | `g.a[-c].e`             | `AVCE()`    | `a-v[-c].e`          |
| `GAVE()`    | `g.a-v.e`               | `AbVCE()`   | `a-bV[-c].e`         |
| `GAbVE()`   | `g.a-bV.e`              | `ACE()`     | `a[-c].e`            |
| `GAE()`     | `g.a.e`                 | `AVE()`     | `a-v.e`              |
|             |                         | `AbVE()`    | `a-bV.e`             |
|             |                         | `AE()`      | `a.e`                |

Others:

| Operation                           | Result                                                                       |
|-------------------------------------|------------------------------------------------------------------------------|
| `repositoryDefault()`               | Maven repository layout path, using the OS file separator                   |
| `repository(str)`                   | Maven repository layout path, using given separator (e.g. `repository(/)`)  |
| `empty()`                           | empty string                                                                 |
| `fixed(str)`                        | the given (non-blank) string                                                 |
| `optionalPrefix(str, mapper)`       | `str` + mapper result, or empty string if the mapper result is blank         |
| `optionalSuffix(str, mapper)`       | mapper result + `str`, or empty string if the mapper result is blank         |
| `compose(mapper...)`                | concatenation of all mapper results, in order                                |

Examples:

```
AbVCE()
compose(G(), fixed(/), AVE())
compose(A(), optionalPrefix(-, C()), fixed(.), E())
```

## Artifact mapper

`ArtifactMapper` — transforms an artifact into another artifact. Used by `mapping(...)` sources/sinks.

| Operation                      | Result                                                                         |
|--------------------------------|--------------------------------------------------------------------------------|
| `identity()`                   | artifact unchanged                                                             |
| `baseVersion()`                | version replaced with base version (timestamped snapshot → `-SNAPSHOT`)        |
| `versionSuffix(str)`           | version + `str`                                                                |
| `baseVersionSuffix(str)`       | base version + `str`                                                           |
| `omitClassifier()`             | classifier removed                                                             |
| `rename(g, a, v)`              | groupId, artifactId and version replaced; `*` keeps the original value         |
| `compose(mapper...)`           | applies mappers one after another                                              |

Note: `compose(...)` currently applies its arguments **right to left** (the last argument first), so
`compose(versionSuffix(-x), rename(*,*,2))` turns `a:x:1` into `a:x:2-x`.

Examples:

```
omitClassifier()
rename(org.example,*,1.0.0)
rename(${groupId},*,*)
```

## Dependency mapper

`DependencyMapper` — transforms a dependency. Only reachable as the first argument of the `mapping(...)`
[dependency sink](#dependency-sink).

| Operation             | Result                                                                                   |
|-----------------------|------------------------------------------------------------------------------------------|
| `identity()`          | dependency unchanged                                                                     |
| `scope(str)`          | scope set to `str`                                                                       |
| `optional(bool)`      | optional flag set                                                                        |
| `exclusion(str...)`   | exclusions **replaced** with given ones; each is `groupId:artifactId[:extension[:classifier]]` (wildcards like `*:*` allowed) |
| `compose(mapper...)`  | applies mappers one after another (currently **right to left**, like the artifact mapper) |

Example: `compose(scope(compile), exclusion(*:*))`

## Artifact key factory

`ArtifactKeyFactory` — computes a key used to group artifacts (e.g. `--keyFactorySpec` of conflict-listing commands,
default `versionlessId()`).

| Operation          | Key                                         |
|--------------------|---------------------------------------------|
| `id()`             | `g:a:e[:c]:v`                               |
| `baseId()`         | `g:a:e[:c]:bV`                              |
| `versionlessId()`  | `g:a:e[:c]`                                 |
| `ga()`             | `g:a`                                       |

## Artifact differentiator

`ArtifactDifferentiator` — computes a value that tells artifacts within the same key group apart; a group with more
than one distinct value is a conflict (e.g. `--artifactDifferentiatorSpec`, default `majorVersion()`).

| Operation         | Value                                                         |
|-------------------|---------------------------------------------------------------|
| `majorVersion()`  | version up to first `.` (whole version if there is no dot)    |
| `minorVersion()`  | version up to second `.` (whole version if there is none)     |
| `baseVersion()`   | base version                                                  |
| `version()`       | version                                                       |

## Artifact source

`Artifacts.Source` — supplies a stream of artifacts (e.g. first parameter of `gav-copy`). Relative paths are resolved
against the current base directory.

| Operation                             | Supplies                                                                                  |
|---------------------------------------|-------------------------------------------------------------------------------------------|
| `null()`                              | nothing                                                                                   |
| `gav(str)`                            | a single artifact `g:a[:e[:c]]:v` (not resolved)                                          |
| `gavs(str)`                           | artifacts from a comma separated list of GAVs, or from a file containing one GAV per line |
| `directory(str)`                      | all artifacts found in the given directory                                                |
| `localRepository(str)`                | all artifacts installed in the local repository at given path                             |
| `sessionLocalRepository()`            | all artifacts installed in the session's local repository                                 |
| `recorder()`                          | artifacts recorded during this session (see `gav-copy-recorded`)                          |
| `concat(source...)`                   | concatenation of all sources                                                              |
| `resolve(source)`                     | artifacts of `source` resolved (unresolvable ones are skipped with a warning)             |
| `resolveTransitive(source[, scope])`  | each artifact of `source` resolved with its transitive dependencies, in given resolution scope *(raw)*: `none`, `compile`, `compile_plus_runtime`, `runtime` (default), `runtime_plus_system`, `test` (case-insensitive; names with `_` must be quoted) |
| `matching(artifactMatcher, source)`   | artifacts of `source` accepted by the [artifact matcher](#artifact-matcher)               |
| `mapping(artifactMapper, source)`     | artifacts of `source` transformed by the [artifact mapper](#artifact-mapper)              |

Examples:

```
gav(org.slf4j:slf4j-api:2.0.13)
resolveTransitive(gavs(gavs.txt), 'runtime_plus_system')
matching(not(snapshot()), sessionLocalRepository())
```

## Artifact sink

`Artifacts.Sink` — consumes artifacts (e.g. `--sinkSpec`, last parameter of `gav-copy`; default often `null()`).
Relative paths are resolved against the current base directory. Sinks that modify something honor dry-run mode.

| Operation                             | Does                                                                                       |
|---------------------------------------|--------------------------------------------------------------------------------------------|
| `null()`                              | discards everything                                                                        |
| `counting()`                          | counts artifacts                                                                           |
| `sizing()`                            | sums artifact file sizes                                                                   |
| `logging()`                           | prints each artifact                                                                       |
| `stat([[list,] details])`             | prints statistics; `stat()` = `stat(true,true)`, `stat(x)` = `stat(true,x)`; details add module descriptor, checksums, origin URI and PURL info |
| `artifactUri([bool])`                 | collects origin repository URIs of artifacts and prints them on close if `bool` (default `true`) |
| `moduleDescriptor()`                  | extracts JPMS module descriptors                                                           |
| `modules(file[, classifier])` *(raw)* | writes a `module=uri#SIZE=..&SHA-1=..` properties file for modular resolved JARs (optionally only of given classifier) |
| `flat(dir[, nameMapper])` *(raw dir)* | copies files into directory `dir`, named by the [artifact name mapper](#artifact-name-mapper) (default `AbVCE()`); does not overwrite |
| `repository(dir)`                     | copies files into directory `dir` using Maven repository layout (release artifacts only)   |
| `unpack(dir[, nameMapper])` *(raw dir)* | unpacks archives into `dir`, each into a subdirectory named by the name mapper (default `ACVE()`); `fixed(.)` overlays all into `dir` |
| `install([localRepoDir])`             | installs into the session local repository, or into the local repository at given path    |
| `deploy(remoteRepo)`                  | deploys into given remote repository                                                       |
| `purge(remoteRepo...)`                | purges whole GAVs from the session local repository, considering given remote repositories |
| `tee(sink...)`                        | passes artifacts to all sinks                                                              |
| `nonClosing(sink)`                    | delegates but never closes `sink`                                                          |
| `matching(artifactMatcher, sink)`     | passes on only artifacts accepted by the [artifact matcher](#artifact-matcher)             |
| `mapping(artifactMapper, sink)`       | passes on artifacts transformed by the [artifact mapper](#artifact-mapper)                 |

Remote repositories are given as `url`, `id::url` or `id::layout::url`; as URLs contain `:` and `/`, they work
unquoted, but quoting is safer.

Examples:

```
flat(target/libs)
flat(target/libs, compose(G(), fixed(/), AVE()))
tee(stat(), matching(withoutClassifier(), install(target/local-repo)))
deploy('myrepo::https://repo.example.com/releases/')
```

## Dependency sink

`Dependencies.Sink` — consumes dependencies.

| Operation                               | Does                                                                         |
|-----------------------------------------|------------------------------------------------------------------------------|
| `null()`                                | discards everything                                                          |
| `counting()`                            | counts dependencies                                                          |
| `tee(sink...)`                          | passes dependencies to all sinks                                             |
| `nonClosing(sink)`                      | delegates but never closes `sink`                                            |
| `matching(dependencyMatcher, sink)`     | passes on only dependencies accepted by the [dependency matcher](#dependency-matcher) |
| `mapping(dependencyMapper, sink)`       | passes on dependencies transformed by the [dependency mapper](#dependency-mapper) |
