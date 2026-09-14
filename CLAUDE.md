# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Aniyomi, an Android app for reading manga and watching anime, forked from Mihon (itself a fork of
Tachiyomi). Kotlin, Gradle multi-module, Jetpack Compose UI. It ships both a "manga" side and an
"anime" side as parallel, largely mirrored feature sets (e.g. `entries`/`items` domain models cover
both manga+chapters and anime+episodes, `source` vs `animesource`, `extension/manga` vs
`extension/anime`).

## Build / lint / test

```
./gradlew assembleRelease           # build release APKs (what CI builds)
./gradlew assembleDebug              # build debug APK for local testing
./gradlew spotlessCheck              # check Kotlin/XML formatting (ktlint via Spotless) — CI gate
./gradlew spotlessApply              # auto-fix formatting
./gradlew testReleaseUnitTest        # run unit tests (release variant, matches CI)
./gradlew test                       # run unit tests for all variants
./gradlew :domain:test               # run tests for a single module
./gradlew :app:testReleaseUnitTest --tests "eu.kanade.tachiyomi.SomeTest"   # run a single test class
```

There are no Android instrumentation/UI tests to run in this repo; unit tests live under each
module's `src/test`. CI (`.github/workflows/build_pull_request.yml`) runs, in order:
`spotlessCheck` → `assembleRelease` → `testReleaseUnitTest`. Always run `spotlessCheck` and the
relevant module's tests before considering a change done.

Git must be on `PATH` — the build reads commit count/SHA via `git` commands
(`buildSrc/.../Commands.kt`) for `BuildConfig` fields. JDK 17 is required (matches CI's
`temurin`/17 setup).

## Module architecture

This is a layered, multi-module Gradle project (module graph in [settings.gradle.kts](settings.gradle.kts)).
Convention/build logic lives in `buildSrc` (`mihon.android.application`, `mihon.library`,
`mihon.android.application.compose`, `mihon.code.lint` plugins) rather than being duplicated per module.

- **`app`** — the application module: UI (Compose + Voyager navigator), `data/` (repositories,
  network sources, workers), `ui/`, `extension/{anime,manga}` (extension installer, API clients for
  the extension store/repo), `di/` (Injekt-based dependency injection, not Hilt/Dagger). Depends on
  every other module.
- **`domain`** — pure Kotlin business models and repository *interfaces*, no Android framework deps.
  Packages under `tachiyomi.domain.*` (shared: category, history, library, track, updates, backup,
  storage, release) plus anime-specific additions under `aniyomi.domain.anime`.
- **`data`** — repository *implementations* backed by SQLDelight. Two parallel SQLDelight schemas:
  `src/main/sqldelight` (manga) and `src/main/sqldelightanime` (anime), each with their own
  `migrations`/`view` directories — schema changes usually need a matching migration in both trees
  where the feature is duplicated for manga+anime.
- **`source-api`** — Kotlin Multiplatform (`commonMain`/`androidMain`) module defining the extension
  source contract: `eu.kanade.tachiyomi.source` (manga sources) and `eu.kanade.tachiyomi.animesource`
  (anime sources), plus `torrentutils`. This is the public surface extensions implement against, so
  changes here are effectively an API contract with all installed extensions.
- **`source-local`** — local (on-device) source implementation, for reading/watching files without an
  extension.
- **`core/common`** — shared utilities with no domain knowledge (preferences, network, i18n helpers).
- **`core/archive`** — archive/compression handling (e.g. for local library files, CBZ-like formats).
- **`core-metadata`** — metadata parsing (e.g. ComicInfo.xml-style formats).
- **`i18n`** / **`i18n-aniyomi`** — MOKO-resources string catalogs; `i18n` holds the upstream/shared
  strings, `i18n-aniyomi` holds Aniyomi-specific additions. String changes should go in the `base`
  (English) files — translations are managed externally via Weblate, don't hand-edit other locales.
- **`presentation-core`** — shared Compose UI building blocks/theme used across the app.
- **`presentation-widget`** — Android home-screen widget implementation.
- **`macrobenchmark`** — Macrobenchmark test module (separate `benchmark` build type in `app`).

### Package naming convention

Code is split across `eu.kanade.tachiyomi.*` (legacy Tachiyomi-inherited code), `tachiyomi.*`/`mihon.*`
(Mihon-era rewrites, generally the newer/cleaner modules such as `domain`/`data`), and `aniyomi.*`
(Aniyomi-specific anime additions layered on top). When adding anime-side features, check for an
existing manga-side equivalent first — most features are intentionally implemented twice in parallel
(once per media type) rather than through a shared abstraction.

### Dependency injection & architecture patterns

- DI is via **Injekt** (`get()`/`addSingletonFactory` style), not Hilt/Koin/Dagger — wiring is
  centralized in `app/src/main/java/.../di`.
- Navigation uses **Voyager**, not Jetpack Navigation.
- Database access is **SQLDelight** (typed SQL), not Room.
- Networking is **OkHttp**; sources parse HTML with **Jsoup**.

### Extensions

Aniyomi/extensions are separate APKs installed at runtime, fetched via an extension store (see
recent history: `extension/{anime,manga}/api`, `extension/{anime,manga}/installer`). Extension
capability is defined by `source-api`'s public classes — do not make breaking changes there without
considering extension compatibility.

## Forking notes (from CONTRIBUTING.md)

If working on a fork/rebrand of this app, the app name/icon, `applicationId` in
[app/build.gradle.kts](app/build.gradle.kts), `google-services.json`, and ACRA endpoint should all be
changed to avoid conflicting with the upstream app's identity, installs, and crash/analytics reporting.
