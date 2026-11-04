# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Product spec, design tokens and build phases: @SHAPES_BUILD_SPEC.md

## Workflow

- One phase per branch and PR. Stop after each phase, summarise, and wait for review. Run `/phase-check` before stopping.
- Never start an emulator. Build the app, then give the user a checklist to run on their physical device. Report anything that needs a device as unverified.
- Mockups are in `design/Shapes-V1/`, numbered as in the spec. Files 27-37 are the concept shots.
- The app name isn't decided: the spec and in-app text say "Shapes" (`R.string.product_name`), the launcher label and project say "TheWid". Don't rename anything without asking.

## Commands

- Device-free checks: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:ktlintCheck`
- Format: `./gradlew :app:ktlintFormat`. A hook runs this after every Kotlin edit, but it can't fix everything, so `ktlintCheck` can still fail.
- Instrumented tests need the user's phone: `./gradlew :app:connectedDebugAndroidTest`

## Build setup

- Repositories go in `settings.gradle.kts`; project-level repositories fail the build.
- Configuration cache is on, so custom build logic must be compatible with it.
- Dependency versions live in `gradle/libs.versions.toml`.
- `material3` is pinned to a 1.5 pre-release above the Compose BOM, because `MaterialShapes` isn't public in 1.4.0.
- Hilt stays on 2.59.x while the project is on Kotlin 2.2; 2.60.1 is built on Kotlin 2.3.
- Haze stays on 1.x until the project moves to Kotlin 2.4, which Haze 2.x requires.

## Architecture decisions

- All shape drawing goes through `ShapePainter`. `GlossyShape` and `ShapeBitmapRenderer` share it so widgets match the app, so don't add a second drawing path.
- Get shapes from `ShapeCatalog`, never from hand-drawn paths.
- Only `FrostedGlass.kt` may import Haze.
- Glance can't set custom fonts or draw polygons: shapes and Doto values go through bitmaps, and the Time digits use a `TextClock` layout so they stay current between updates. Don't pass `SizeMode.Responsive`, which risks the roughly 1 MB limit on widget updates.
- Each `StatusWidget` kind needs its own subclass: Glance finds instances by class, so a shared class makes `updateAll` redraw every widget as one kind.
- Apps can't remove a launcher's widget or choose its placed size, so My widgets explains removal and Customize's size choice only previews.
- The notification listener counts, and reads only the title of message notifications to match Inner Circle names, in memory. Never read a notification's text or save sender names: the permission screen promises this.
- Build everything the mockups show. A feature that needs a permission gets an `AccessRequest` ask flow, shown only after someone taps it. Adding a person needs no permission (the system contact picker); calling or messaging them asks for contacts.
- Steps, Water, Sleep and the Today screen (mockups 27, 29, 30) are future work, listed in the spec. Don't build them yet.
- Paint alpha multiplies a shader. Reset it before setting a shader on a reused `Paint`.
- Dark atmospheres use a dimmer glass tile than the spec's values (8% fill, 20% border), following the Moss mockups. Light atmospheres use the spec's values.
