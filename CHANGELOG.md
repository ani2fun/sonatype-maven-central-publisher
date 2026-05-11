# Changelog

All notable changes to this plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
