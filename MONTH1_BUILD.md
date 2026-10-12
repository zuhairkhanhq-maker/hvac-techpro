# HVAC TechPro Month-1 build

## Implemented in this package

- Five-part launch navigation: Home, Equipment, Field tools, Reference and Settings.
- Two-tap New Job entry from Home.
- Room/SQLite v3 data model for customers, equipment and service jobs, with foreign keys, indexes and preservation of the v2 logbook.
- Free limits enforced in the data layer: three equipment records and ten completed jobs.
- Unlimited records for Plus and Pro previews.
- Equipment history showing every saved job, measurement summary, finding and work performed.
- Offline nameplate OCR entry for Plus/Pro, structured parsing and mandatory field confirmation.
- Deterministic safety-first workflows for no cooling, no heating, poor airflow, compressor-start, short-cycle and water-leak calls.
- On-device PDF service reports for Plus/Pro.
- Pro company-name branding and private on-device business summary.
- Portable JSON export of customers, equipment and jobs for Plus/Pro.
- Existing calculator, PT, A2L worksheet, fault reference and offline guide retained.
- No Internet permission and no cloud inference/customer-data upload.

## Deliberately not represented as complete

- Google Play purchases: Play product IDs, owner Play Console access and a purchase-verification backend are not included.
- Release signing: owner keystore and credentials are not included.
- Manufacturer-specific codes, parts compatibility and technical documents: no unlicensed or unverified data was invented.
- Encrypted restore: portable export exists; a complete encrypted restore workflow remains a launch gate.
- Physical-device acceptance and qualified HVAC review require external people/devices.

## Launch definition

This is an expanded Month-1 engineering candidate. It becomes a public paid release only after Play Billing/entitlement verification, release signing, verified technical content, physical-device testing and technician acceptance are completed.
