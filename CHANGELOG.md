# Changelog

All notable changes to this plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.1] - 2026-05-11

Internal architecture pass on top of 1.1.0. No DSL changes; bug fixes
and refactors only. 27 → 42 unit tests.

### Added
- **`BundleComposition` module** (`api/BundleComposition.kt`) holds the
  "what files compose a Sonatype bundle, with their canonical names"
  rules — the filter for Spring Boot's `-plain.jar`, the renames from
  `pom-default.xml` / `module.json` / `versions.toml` to Maven Central's
  `{artifactId}-{version}.{pom,module,toml}` convention. Pure functions
  returning `List<NamedFile>`; previously buried inside
  `AggregateFiles.action()` and untested.
- **`HashAlgorithm` enum** (`api/HashAlgorithm.kt`) replaces the
  `MessageDigestAlgorithm` String constants. Each entry carries both
  `javaName` and `fileSuffix` side by side, so the algorithm-to-suffix
  mapping lives in one place.
- 15 new unit tests: `BundleCompositionTest` (×7), `HashAlgorithmTest`
  (×4), `ZipUtilsTest` (×4).

### Changed
- **`aggregateFiles` no longer mutates Gradle's output directories.**
  The previous code used `File.renameTo()` to move files in place inside
  `publications/maven/` and `version-catalog/` before copying them into
  staging. The new `BundleComposition` is pure: it returns target names,
  and `AggregateFiles` copies the source into staging under the new name.
  Gradle's outputs stay intact, and reruns are idempotent.
- **`ZipUtils.prepareZipFile` throws on failure.** Previously it
  `println`'d and returned silently if the source folder was missing or
  the zip file already existed, which could silently leave a stale
  `upload.zip` on disk for `publishToSonatype` to upload. Now it throws
  `GradleException` on a missing source and overwrites an existing zip
  for idempotent reruns. This matches the discipline established by
  `SonatypeApiException` in 1.1.0.
- **Invalid `shaAlgorithms` entries fail fast with a listed error.**
  Previously a typo like `"SHA-265"` produced a cryptic
  `NoSuchAlgorithmException` deep inside the hash loop. Now
  `HashAlgorithm.fromJavaName()` rejects unknown names at task action
  time with `Unknown hash algorithm: '<name>'. Supported: MD5, SHA-1,
  …`.
- **`generateMavenArtifacts` is a lifecycle task**, not a
  `DefaultTask` subclass. The previous `GenerateMavenArtifacts` class
  had an empty `@TaskAction` body — pure dependency aggregation, which
  Gradle does with lifecycle tasks directly.

### Fixed
- Latent bug: `SHA-512/224` and `SHA-512/256` would have produced
  checksum files with a `/` in the name (e.g. `app.jar.sha512/224`) —
  treated as a path, breaking the write. `HashAlgorithm` now carries
  explicit, filesystem-safe `fileSuffix` values (`sha512_224`,
  `sha3_256`, etc.). The previously documented SHA-3 / SHA-512-truncated
  algorithms are now actually usable.

### Removed
- `utils/MessageDigestAlgorithm.kt` (replaced by the `HashAlgorithm`
  enum in `api/`).
- `GenerateMavenArtifacts` task class (replaced by a lifecycle-task
  registration in the plugin).
- Unused `HashUtils.getCheckSumFromFile(digest, filePath: String)`
  overload — zero callers.

## [1.1.0] - 2026-05-11

### Added
- **Conventions for every required DSL property.** `groupId`, `artifactId`, and
  `version` fall back to `project.group`, `project.name`, and `project.version`.
  `componentType` defaults to `"java"`, `publishingType` to `"USER_MANAGED"`,
  and `shaAlgorithms` to an empty list (MD5 + SHA-1 are always produced as
  Maven Central requires). The DSL block is now optional for typical Java
  projects — credentials are the only mandatory fields.
- **`BundleLayout` value object** as the single source of truth for the
  staging directory and zip path. Replaces five copies of layout knowledge
  scattered across the plugin and four tasks.
- **`SonatypeCentralClient` interface** in `eu.kakde.sonatypecentral.api`
  with `OkHttpSonatypeCentralClient` as the production adapter. Centralizes
  auth, URL construction, JSON parsing, and timeouts (30s connect / read /
  write).
- **`SonatypeApiException` extends `GradleException`.** HTTP failures now
  propagate as task failures (see Changed below).
- **Comprehensive unit tests** (27 total) including `MockWebServer`-backed
  request-shape tests for every API method, fake-client task-level tests,
  and a regression test for `pre-existing sourcesJar` (#5).

### Changed
- **`publishToSonatype` now fails the build on a non-2xx HTTP response.**
  Previously the task printed the error to stdout and exited successfully,
  causing CI/CD pipelines to silently treat failed publishes as successes.
  Existing pipelines that relied on the silent-failure behavior should add
  explicit error tolerance.
- **Kotlin compiled with `-Xjdk-release=1.8`.** The compiler now rejects
  uses of post-JDK-8 Java APIs at build time, preventing the
  `NoSuchMethodError` class of consumer-runtime failures (#3).
- **Internal dependency bumps:** Kotlin 1.9.20 → 2.1.21, Gson 2.10.1 →
  2.11.0, Okio 3.8.0 → 3.10.2, plugin-publish 1.2.1 → 1.3.1,
  foojay-resolver-convention 0.8.0 → 0.10.0. OkHttp stays at 4.12.0 (5.x
  is alpha).

### Fixed
- **#1** Plugin no longer publishes Java-21-only metadata. `sourceCompatibility`
  is now pinned to 1.8 alongside `targetCompatibility`, so Android consumers
  on JDK 17 resolve the plugin without "no matching variant" errors.
- **#2** Applying the plugin without a configured DSL block no longer fails
  with `Cannot query the value of extension … property 'groupId' because it
  has no value available`. Conventions cover every required property.
- **#3** `aggregateFiles` no longer throws
  `NoSuchMethodError: java.util.List.addLast` on consumer JDKs below 21.
- **#4** `aggregateFiles` no longer throws `UnsupportedOperationException` on
  Windows. The POSIX permissions step is guarded by a filesystem-support
  check.
- **#5** Applying the plugin to a project that already has a `sourcesJar`
  task (e.g. via `kotlin("jvm")`) no longer fails with
  `InvalidUserDataException: The task 'sourcesJar' … is not a subclass of
  the given type`.

### Removed
- `utils/IOUtils.kt` (`createDirectoryStructure` and `renameFile` were inlined
  into `AggregateFiles`; `printFileContent` was dead code).
- `utils/HashComputation.kt` (collapsed into the `ComputeHash` task).
- `utils/ENDPOINT.kt` (URL constants moved behind `SonatypeCentralClient`).

## [1.0.6]

Last release before this rewrite — see Git history.
