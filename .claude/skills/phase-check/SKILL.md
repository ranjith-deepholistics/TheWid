---
name: phase-check
description: Verify a build phase from SHAPES_BUILD_SPEC.md before stopping for review. Runs the device-free checks, compares the work to the definition of done, and writes the phase summary with a physical-device checklist. Use when a phase is finished or the user asks whether one is done.
---

Check the current phase and report what is and isn't verified. Never start an emulator.

1. Run `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug :app:ktlintCheck --console=plain`.
2. Read the results:
   - Unit tests: `app/build/test-results/testDebugUnitTest/*.xml`
   - Lint: `app/build/reports/lint-results-debug.xml`. Errors fail the phase. List warnings in code changed by this phase.
3. Go through section 7 of `SHAPES_BUILD_SPEC.md` for this phase:
   - Previews exist for every screen state added. Search the changed files for `@Preview`.
   - Mockups: read the PNGs in `design/Shapes-V1/` for this phase and say which ones you compared and how.
   - No permission is requested before the user taps a widget that needs it. Search for permission requests in the changed code.
4. Write the summary:
   - What was built.
   - Results of each check, with counts.
   - Differences from the spec or design, and why.
   - **Unverified:** instrumented tests, appearance, animation and anything else that needs a device. Reading code doesn't verify these.
   - **Phone checklist:** the command to run (`./gradlew :app:connectedDebugAndroidTest :app:installDebug`) and specific things to look at, each with the expected result.
5. Stop and wait for review. Don't start the next phase.
