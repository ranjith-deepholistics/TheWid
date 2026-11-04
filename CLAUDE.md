# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Product spec, design tokens and build phases: @SHAPES_BUILD_SPEC.md

## Workflow

- One phase per branch and PR. Stop after each phase, summarise, and wait for review. Run `/phase-check` before stopping.
- Never start an emulator. Build the app, then give the user a checklist to run on their physical device. Report anything that needs a device as unverified.
- Mockups are in `design/Shapes-V1/`, numbered as in the spec. Files 27-37 are the concept shots.
- The app name isn't decided: the spec says "Shapes", the project says "TheWid". Don't rename anything without asking.

## Commands

- Device-free checks: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:ktlintCheck`
- Format: `./gradlew :app:ktlintFormat`. A hook runs this after every Kotlin edit, but it can't fix everything, so `ktlintCheck` can still fail.
- Instrumented tests need the user's phone: `./gradlew :app:connectedDebugAndroidTest`

## Build setup

- Repositories go in `settings.gradle.kts`; project-level repositories fail the build.
- Configuration cache is on, so custom build logic must be compatible with it.
- Dependency versions live in `gradle/libs.versions.toml`.
- `material3` is pinned to a 1.5 pre-release above the Compose BOM, because `MaterialShapes` isn't public in 1.4.0.
- Haze stays on 1.x until the project moves to Kotlin 2.4, which Haze 2.x requires.

## Architecture decisions

- All shape drawing goes through `ShapePainter`. `GlossyShape` and `ShapeBitmapRenderer` share it so widgets match the app, so don't add a second drawing path.
- Get shapes from `ShapeCatalog`, never from hand-drawn paths.
- Only `FrostedGlass.kt` may import Haze.
- Paint alpha multiplies a shader. Reset it before setting a shader on a reused `Paint`.
- Dark atmospheres use a dimmer glass tile than the spec's values (8% fill, 20% border), following the Moss mockups. Light atmospheres use the spec's values.
