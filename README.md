# Verdant Budget — Kotlin Multiplatform

A native Compose Multiplatform budget app, rebuilt entirely in Kotlin. Shared screens, financial calculations, validation, and repository code run on Android and desktop JVM. Android and desktop have thin platform-specific entry points for private storage and native file pickers. There is no Rust, Tauri, JavaScript, or WebView runtime in this implementation.

The visual direction follows the supplied reference with dark surfaces, forest-green accents, gold coin artwork, Poppins typography, circular actions, and rounded analytics cards. Version 2.1 replaces static background gradients with an animated liquid mesh inspired by the supplied Music App reference. Settings provides six presets, a custom hex/RGB picker, live previews, and a saved animation toggle. All controls, navigation, dialogs, sheets, icons, and the calendar are custom Compose Foundation implementations; Material 3 and Material icon dependencies have been removed.

## Features

- Income and expense creation, editing, deletion, categories, and dates.
- Lifetime balance and truthful monthly cash flow, clearable search, exact category filters, preserved navigation state, and a quick return to the current month.
- Monthly category budgets, copying last month's limits into an unplanned month, and over-budget indicators.
- Daily spending charts, category breakdowns, and savings percentages.
- USD, KES, EUR, GBP, and INR. Currency selection changes the display symbol without converting amounts.
- Local storage with atomic writes and Keystore-backed encryption on Android. Failed writes leave the active ledger unchanged.
- Native JSON backup export and restore with validation and confirmation before replacement.
- Backup compatibility with the previous Verdant Rust/Tauri implementation.
- Optional sample data, kept separate from a new empty ledger.
- Comma or dot decimal keyboard input, large-text layouts, adaptive money labels, and retry-safe forms that preserve drafts on failed writes.
- Home action tiles are fully tappable; home budget and transaction links always open the current month. The budget banner reports planned-category overspending and opens the overview instead of unexpectedly opening an editor.

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
