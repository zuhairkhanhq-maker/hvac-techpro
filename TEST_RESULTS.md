# Verification results

Verification date: 2026-10-12 (UTC)

The pre-Month-1 audited baseline completed the following local Gradle tasks successfully:

```text
:app:assembleDebug
:app:testDebugUnitTest
:app:lintDebug
```

## Automated result

- 24 of 24 baseline JVM/Robolectric tests passed.
- 0 tests failed or were skipped.
- Android lint reported 0 errors and 6 non-blocking warnings.
- The Month-1 source still contains 24 tests and now adds field-record persistence and structured nameplate parsing while replacing obsolete prototype navigation tests.

The Month-1 changes could not be rerun in the final packaging workspace because the Gradle distribution was not present and external Gradle download access was unavailable. Treat a clean `assembleDebug`, `testDebugUnitTest` and `lintDebug` run in Android Studio as mandatory before installation or release.

The six lint warnings were five legacy launcher-icon shape advisories and one newer-library-version notice. They do not block compilation, but the launcher artwork should be reviewed on actual launchers before store submission.

## Important scope

This is verified source for a development/pilot build, not a signed public release. The package intentionally excludes signing credentials and generated build directories. Build the included wrapper with JDK 17 and Android SDK 36.1.

The automated suite cannot prove HVAC technical correctness, camera/OCR performance on field hardware, accessibility on every device, or billing lifecycle behavior. Those are launch gates documented in `LAUNCH_AUDIT.md`.
