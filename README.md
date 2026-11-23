# Verdant Budget

A fresh Android budget app built with Rust, Tauri 2, React, and TypeScript. The previous app has been replaced entirely. The visual direction uses charcoal surfaces, forest green gradients, lime accents, and rounded cards inspired by the supplied reference.

## Features

- Onboarding with USD, KES, EUR, GBP, or INR and optional sample data.
- Income and expense creation, editing, deletion, category selection, and dates.
- Lifetime balance, monthly summaries, search, filters, and month navigation.
- Monthly category budgets, remaining amounts, and over-budget indicators.
- Daily spending chart, category breakdown, category mini charts, and income savings rate.
- Rust-managed local storage on Android; browser local storage for development.
- JSON backup export and validated restore. Android exports selectable backup text and restores pasted text. Browser export downloads a file.
- No accounts, servers, ads, or bank connections. Currency selection changes the display symbol without converting amounts.

## Development

Install Node 22+, Rust stable, and the [Tauri prerequisites](https://v2.tauri.app/start/prerequisites/).

```sh
npm ci
npm run dev
npm test
npx playwright install chromium
npx playwright test
npm run build
cargo test --manifest-path src-tauri/Cargo.toml --locked
```

For native desktop development: `npm run tauri -- dev`.

## Android APK

GitHub Actions → **Android APK** builds an installable APK for ARM64 and ARMv7. Download **Verdant-Budget-Android** from a successful run and extract `Verdant-Budget-android.apk`. Enable installation from your download app when Android prompts you.

This is a development-signed debug APK for device testing, not a Play Store release. GitHub runners may use a different development signing key on later runs; export a backup before uninstalling to install a differently signed build. Production distribution needs a private release signing key and release configuration.

To build locally, install JDK 17, Android SDK 35 and NDK 27.2.12479018, set `JAVA_HOME`, `ANDROID_HOME`, and `NDK_HOME`, then:

```sh
npm run tauri -- android init --ci
npm run tauri -- android build --debug --apk --target aarch64 armv7 --ci
```

Android scaffolding is generated from Tauri during each build, rather than copied from the previous app. All UI resources are bundled and work offline. Data is saved under the app's private data directory; uninstalling removes it. Money is stored as integer minor units, with two decimal places for supported currencies.

See [artwork and font credits](docs/artwork.md) for bundled visual assets. The welcome, home, and analytics layouts closely follow the supplied screenshot. Banking actions have been mapped to working income, expense, and budget controls.

Browser tests cover transaction CRUD, budgets, analytics, layout widths, backup restore, native IPC integration with a simulated host, and failed-write recovery. Rust tests cover schema round trips, calendar dates, and invalid monetary values. A successful Android build verifies compilation and packaging; physical device behavior still requires device testing.
