# RIVAN CSC Manager

A local-first Android CSC management app built with **Kotlin + Jetpack Compose + Room**. It is designed to be built from GitHub Actions; Android Studio is not required for the GitHub build workflow.

## Exactly two app roles

1. **Admin** — the network owner/admin. Manages CSC shops, operators, master services/rates, and sees network-wide business records.
2. **Operator** — the person who runs an assigned physical CSC shop. Can add customers, record transactions and expenses, and check attendance for their assigned shop.

There is no Super Admin role and no third role.

## Current starter build (v0.1.0)

Implemented in this foundation:

- Login with Admin and Operator role separation.
- Room database persistence on the device.
- Admin: add/enable/disable CSC shops; create/suspend operators; add master services; view network dashboard, customers, transactions, expenses and audit log.
- Operator: assigned-shop dashboard; add customers; record transactions and expenses; view own-shop records; local check-in/check-out; view available services.
- Income and expense totals with net calculation.
- Light/dark theme toggle.
- Demo seed data on first launch.
- GitHub Actions workflow to build and upload a debug APK artifact.

## Demo login

For first-run testing only:

- Admin: `admin` / `rivan123`
- Operator: `operator` / `operator123`

**Important:** These are demo credentials for local testing, not production authentication. Change/remove them before a public production release. The starter stores demo operator credentials in the local Room database; production must replace this with server-side authentication and secure password hashing.

## Build without Android Studio

1. Create a new GitHub repository.
2. Upload/push the contents of this project to the repository root.
3. Open the **Actions** tab and enable workflows if GitHub asks.
4. Push to `main`/`master`, or run **Android CI → Run workflow**.
5. When the workflow completes, open its run and download the `rivan-csc-manager-debug-apk` artifact.

The workflow installs JDK 17, Android SDK and Gradle 8.11.1, then runs `gradle assembleDebug`.

## Local build (optional)

Requires JDK 17, Android SDK Platform 35 and Gradle 8.11.1:

```bash
gradle assembleDebug
```

## Important scope and limitations

This is the first working local-first foundation, not yet the complete PRD release. Room data is stored on one device. It does **not** synchronize different CSC phones or provide true live network monitoring. A secure backend, central database, server-side authorization, secure password hashing, cloud document storage, and conflict-safe sync are required for the 100-shop production network.

Not yet implemented in this foundation: full application workflow/statuses, receipt PDF/QR/WhatsApp delivery, refunds/cancellation approvals and reversal accounting, salary/payroll, leave approvals, camera/photo attendance, OTP/device approval, notifications delivery, document vault, PDF/Excel export, encrypted backup/restore, backend/cloud sync, and production-grade authentication/security. These must be built and tested before calling the PRD complete.

## Project structure

```text
RIVAN-CSC-Manager/
├── .github/workflows/android-ci.yml
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/rivan/cscmanager/
│           ├── MainActivity.kt
│           └── data/
│               ├── AppDatabase.kt
│               └── Models.kt
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Brand palette

- Black: `#000000`
- Purple: `#7E3BED`
- White: `#FFFFFF`
- Lime: `#C6FF34`

## License

Private project. Add a license only if/when you intend to publish the source under specific terms.
