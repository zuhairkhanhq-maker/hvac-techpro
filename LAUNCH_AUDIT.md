# HVAC TechPro launch audit

Audit date: 2026-10-11. Scope: supplied native Android source, build configuration, all source screens, calculation and tier logic, local storage and automated regression coverage. This is an engineering audit of a prototype, not proof that every bug is absent or a promise about retention.

**Release decision: not ready for a public paid launch.** Several original features were simulations and technical datasets lack provenance. The revised project makes that visible and removes fabricated results. See remaining gates below.

## Implemented repairs

| Area | Original issue | Change |
| --- | --- | --- |
| Paid access | Local selector implied a purchase and granted paid access | Debug-only tier previews; release ignores locally stored paid tier and disables purchase |
| Label scanner | Ignored photo and returned a fixed equipment identity | Bundled on-device OCR reads actual image text; full-resolution camera capture/gallery flow, size limits, orientation correction and errors |
| Video | Delay and preset result looked like analysis and spent quota | Explicit free educational blink examples, no credit consumption, no brand-to-fault mapping |
| AI guide | Fixed responses presented as AI diagnosis | Clearly labeled keyword guide, conservative checklists, bounded messages and cancellation |
| A2L | Unsupported room-volume formulas generated compliance certificates | Manufacturer-input charge arithmetic only; no pass/fail or certification; missing inputs block export |
| SQLite | Concurrent startup seeding could duplicate templates; stale favorite read/write | Transactional seed; atomic favorite update; Room v2 migration preserves user notes and retires old presets |
| Service notes | Invalid drafts could save; dialog closed before storage success | Required fields, duplicate-save guard, success-only close, visible storage failures, confirmed deletion |
| Measurements | Invalid numbers silently became defaults; unbounded PT extrapolation | Reject non-finite, invalid and out-of-range inputs; no PT extrapolation; guard formula range and blend subcooling |
| Calculation wording | Measurement comparisons asserted specific defects | High/low/target-band labels; explicit estimate scope; no inferred charge diagnosis |
| Duct estimates | Height silently clamped; area comparison could imply friction equivalence | Reject impractical height; label area-only comparison and design-validation requirement |
| Navigation | Dense home and state lost on tab changes | Simple tool list, clear capability summaries, saved tab and screen state, onboarding |
| UI | Dark cards mixed with light-theme text assumptions | Consistent dark default, brighter accents, scrollable plans/forms, larger log actions |
| Reference filters | Filter chips did not match equipment data | Options derived from the catalog, clear empty state/filter reset |
| Build | Missing runnable wrapper; unused backend/plugin configuration | Wrapper included; unnecessary Firebase/secrets setup removed; default debug signing supported |

## Validation

The audited development build completed `assembleDebug`, `testDebugUnitTest`, and `lintDebug`. All 24 JVM/Robolectric tests passed with no skipped tests. Lint reported 0 errors and 6 non-blocking warnings (five launcher-icon shape advisories and one dependency-update notice). Tests cover PT bounds, charge arithmetic, invalid measurements, the plan matrix, release entitlement rejection, quota reset, no-charge examples, Room concurrency/persistence/migration and Compose user flows. Passing these tests does not validate manufacturer data or real-world HVAC diagnosis. See `TEST_RESULTS.md`.

## Remaining launch gates

1. **Commerce:** implement Google Play Billing with product configuration, verified entitlements, restore, pending/cancelled purchases, grace periods, refunds and downgrade behavior. Owner Play Console/backend configuration is required. Do not enable paid plans solely by restoring the old local selector.
2. **Technical accuracy:** source and license manufacturer PT dew/bubble tables, charging targets, code mappings and part catalogs. Validate calculations against published reference cases with a qualified HVAC reviewer. The included tables remain approximate, and duct sizing is not an approved engineering design implementation.
3. **Paid value:** real video recognition, automatic parts matching, live availability and circuit tracing do not exist. Static examples must stay labeled as previews or be removed from paid marketing. The guide is not an LLM service. Current pricing has not been validated with users.
4. **Device acceptance:** test camera/picker cancel, missing camera, low-light/rotated labels, incorrect OCR, large images, rapid navigation, process death, offline operation, low storage and interrupted writes on physical devices. Actual OCR accuracy and the external camera integration have not been validated here.
5. **Usability/accessibility:** test small phones, tablets, landscape, 200% font size, TalkBack, keyboard focus and outdoor readability. Dark default fixes the existing palette inconsistency; a fully supported light theme still needs deliberate design.
6. **Data/privacy:** review the local-note backup policy and customer-data retention. No login, encrypted database layer, sync, export/import or recovery service is provided. Test real backup/restore and migration from a production v1 database, beyond the synthetic same-schema migration fixture.
7. **Release operations:** owner signing, release build/Play prelaunch report, privacy/data-safety declarations, support contact, store listing and staged rollout. No production deployment, real purchase or charge was made.
8. **Retention evidence:** observe representative technicians completing real tasks; measure successful first use, repeat use, failures and cancellation reasons. No analytics or churn improvement has been demonstrated by this audit.

## Acceptance scenarios for the release owner

- Fresh install: understand free value, finish a calculation, create/search/favorite a note, relaunch and find it.
- Invalid input: blank, NaN, infinity, huge and negative values show errors without a fabricated result.
- Upgrade existing notes: preserve every user row and favorite, with one set of new reference templates.
- Paid lifecycle: complete test purchases and every cancellation/refund/restore state against verified entitlements.
- OCR: compare extracted model/serial/voltage text to the photo; never infer compatible parts from unverified text.
- Reference content: every equipment claim has model/revision/source; every numerical reference has provenance.

## Documentation used during implementation

- Android Play Billing integration: https://developer.android.com/google/play/billing/integrate
- ML Kit text recognition on Android: https://developers.google.com/ml-kit/vision/text-recognition/v2/android
- Room transactions: https://developer.android.com/reference/androidx/room/Transaction

These references describe implementation interfaces; they do not validate the app's HVAC datasets.
