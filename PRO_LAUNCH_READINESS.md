# Pro launch readiness

## Implemented offline value

- Branded PDF identity: company name, contact/license text, logo, terms and warranty text.
- Service evidence: before/after photographs and explicit typed technician/customer approval.
- Reusable inspections: three built-in workflows plus custom checklist creation.
- Maintenance workflow: unit-linked upcoming, overdue and completed reminders.
- Private dashboard: 30-day jobs, parts usage, approvals, overdue/upcoming reminders and lifetime totals.
- Recovery: AES-256-GCM encrypted backups using PBKDF2-HMAC-SHA256, authenticated decryption, schema checks, relationship validation and transactional restore.
- Offline posture: no Internet permission and Android cloud backup disabled.

## Automated launch gate

The GitHub workflow runs a clean compile, unit/Robolectric tests, Android lint, debug APK assembly and API 35 emulator instrumentation. Pro tests cover encryption round trips, wrong-password rejection, damaged-relationship rejection and tier access rules.

## External production gates

- Google Play Billing subscriptions and Play-verified entitlements.
- Upload-key signing and a release AAB.
- Play purchase, renewal, cancellation, refund and restore testing.
- Physical phone tests for camera/photo import, OCR, PDF sharing, backup export and cross-device restore.
- Qualified HVAC review of safety wording and technical reference data.
