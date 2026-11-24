# Verdant Budget — Kotlin Multiplatform

A native Compose Multiplatform budget app, rebuilt entirely in Kotlin. Shared screens, financial calculations, validation, and repository code run on Android and desktop JVM. Android and desktop have thin platform-specific entry points for private storage and native file pickers. There is no Rust, Tauri, JavaScript, or WebView runtime in this implementation.

The visual direction follows the supplied reference with dark surfaces, forest-green accents, gold coin artwork, Poppins typography, circular actions, and clear financial summaries. All pages share Home's visual structure: a restrained liquid header above solid near-black content. Activity uses open transaction rows, Budgets uses category progress rows, Insights has a single spending chart and category breakdown, and Settings uses flat sections. Settings provides six presets, a custom hex/RGB picker, live previews, and a saved animation toggle. All controls, navigation, dialogs, sheets, icons, and the calendar are custom Compose Foundation implementations; Material 3 and Material icon dependencies have been removed.

## Features

- Income, expenses, split categories, notes, cleared status, duplicate entries, templates, calculator, date shortcuts, and resumable drafts.
- Cash, M-Pesa, bank, and savings accounts with opening balances, transfers, archive, and reconciliation.
- M-Pesa fee estimates and actual overrides, with confirmed versus estimated costs by transaction type and amount band. Fees count once in spending; withdrawal principal remains a transfer. See [fee reference and limitations](docs/mpesa-fees.md).
- Custom category names/icons/order, archive, and history-safe renaming.
- Monthly, weekly, and payday budgets, optional rollover, threshold alerts, and prior-month copy.
- Recurring expenses/income, due agenda, expected fees, mark paid/received, and optional Android reminders.
- Savings goals, deadlines, and contributions; goal tracking does not invent expenses or account movements.
- Search and date/amount/type/category/account/cleared filters, sorting, bulk recategorize/delete, undo, and recently deleted recovery (up to 200 records for 30 days).
- Monthly spending charts, category drilldown, period comparisons, net cash flow, next scheduled commitments, and a real alerts inbox.
- Full CSV export/import with mapping, preview, row errors, duplicate detection, and spreadsheet formula protection.
- Validated JSON and password-protected portable backups with restore preview and backup reminders. Schema 1 backups migrate to schema 2.
- Android Keystore encryption, optional biometric/device credential lock with background relock, hidden balances, and a private quick-entry widget.
- App-wide accent presets/custom RGB/hex colors, fixed red expense/green income indicators, reduced motion, optional haptics, and preserved page/scroll state.
- USD, KES, EUR, GBP, and INR display currencies. Changing currency does not convert existing amounts.
- Optional sample data and compatibility with prior Verdant Rust/Tauri JSON backups.

Page transitions, selection colors, button presses, chart reveals, progress bars, and toast messages use restrained animations. Scroll tests cover all pages at 360 × 760 with normal and 1.4× text, a 1,000-entry history, end-of-page reachability above navigation, and native keyboard/profile flows.

See [custom interface](docs/design-system.md) and [liquid background](docs/liquid-background.md) implementation notes.

## Project structure

| Module | Responsibility |
| --- | --- |
| `shared` | Kotlin Multiplatform Android library + desktop JVM target; shared Compose UI, model, calculations, serialization, repository, common tests, and rendered UI tests |
| `androidApp` | Android launcher, lifecycle, atomic private storage, native document pickers, and instrumented UI tests |
| `desktopApp` | Desktop JVM launcher, local storage, and native file dialogs |

Android and desktop are implemented targets. iOS is not included in this build.

## Build and run

Use JDK 17 and Android SDK 36. Open the root folder in Android Studio or IntelliJ IDEA with Kotlin Multiplatform support. Set the Android SDK path in `local.properties` when building locally.

```sh
./gradlew :shared:desktopTest
./gradlew :desktopApp:run
./gradlew :androidApp:assembleRelease
./gradlew :androidApp:lintRelease
./gradlew :androidApp:connectedDebugAndroidTest
```

On Windows, use `gradlew.bat`. The Gradle wrapper includes a distribution checksum. Versions are pinned: Kotlin 2.3.10, Compose Multiplatform 1.10.3, Android Gradle Plugin 8.13.2, and Gradle 8.13. The shared module uses the Android-KMP library plugin and the Android launcher is a separate module, following the [KMP module structure](https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html).

## APK and validation

GitHub Actions → **Kotlin Multiplatform Release** builds and verifies a release-signed APK and Android App Bundle. Download **Verdant-Kotlin-Release**, extract `Verdant-Budget-Kotlin.apk`, and allow Android to install it when prompted. The artifact also includes a SHA256 checksum.

The workflow runs shared model/repository tests, rendered Compose UI tests at 360/393/430 widths, desktop compilation, Android lint, APK signature verification, and Android API 26, 35, and 36 emulator tests. In-process debug instrumentation covers storage, native backups, and activity recreation. A separate `releaseDriver` uses UI Automator to control the exact signed, optimized release APK across onboarding, transactions, budgets, analytics, and process restart, without linking its test harness into the app. Screenshots, logcat, and test reports are saved for review.

Release builds use a stable private signing key stored in GitHub Actions encrypted secrets. A previously installed development build may have a different certificate; export a backup before uninstalling it. The Kotlin app uses its own storage file; restore a previous Verdant JSON backup through Settings if migrating from Tauri. Uninstalling removes private device data. Android requires API 26 or newer.

The app requests no internet permission, and all fonts and artwork are bundled. See [artwork and font credits](docs/artwork.md).

See [release and signing instructions](docs/releasing.md) and [privacy and local data](docs/privacy.md). APK/AAB creation does not automatically publish the app to Google Play.
