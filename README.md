# HVAC TechPro — offline Month-1 launch candidate

Native Android app using Kotlin, Compose and Room/SQLite. The product centers on customers, equipment, service jobs, offline field tools and professional local reports. Read `MONTH1_BUILD.md` and `LAUNCH_AUDIT.md` before distribution.

## Run

Use Android Studio with JDK 17, Android SDK 36.1, and Build Tools 36.0.0. Set your local SDK path through Android Studio or `local.properties` (not included). The Gradle wrapper is included.

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
```

Debug builds can preview Free, Plus and Pro without purchasing. Release builds reject local paid entitlement selection. No AI API key or Firebase configuration is required. Label OCR uses bundled on-device ML Kit.

Release signing requires the owner's `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`, and alias `upload`. Signing keys and credentials are not included. Do not publish the debug build.

## Feature status

| Plan | Implemented behavior | Limit |
| --- | --- | --- |
| Free | Calculators, PT reference, three equipment records and ten completed service jobs | Technical tables and formulas need specialist/manufacturer validation |
| Plus preview | Unlimited equipment/jobs, history, nameplate OCR, guided workflows, PDF reports and export | Paid activation is not connected |
| Pro preview | Plus features, report branding and local business summary | Manufacturer data packs are not bundled |

There is no payment, server entitlement verification, account system, cloud sync, video recognition, live inventory, automatic parts compatibility or automatic circuit tracing in this project. Founding prices are hypotheses, not purchasable offers in this source package.

## Data

Room database `hvac_field_database.db`, schema version 2, stores service notes on the device. Migration from version 1 preserves user-authored notes and replaces the old built-in examples. Seeding is transactional; favorites update atomically. Notes can contain customer information: review privacy, retention and Android backup policy before release. Room is not separately encrypted. Entitlement preferences are excluded from backup; supported OS backup can include notes.

Use the included regression tests as a starting point. Camera capture, OCR accuracy, physical-device behavior, restore, accessibility and release signing still need device acceptance testing.

See `TEST_RESULTS.md` for the latest automated verification summary.
