# Amara — a budgeting app for Android

A dark, purple-and-lime budgeting app written entirely in Rust and drawn with
[egui](https://github.com/emilk/egui). There is no web view, no JavaScript and
no platform UI toolkit: every pixel is painted by Rust.

## Screens

| Screen | What it does |
| --- | --- |
| **Home** | Safe-to-spend for the month, week-over-week change, quick actions, this month's envelope with a progress bar, and the three latest purchases. |
| **Budgets** | Every envelope with its own budget, spend and percentage. Over-budget envelopes turn coral. |
| **Insights** | Daily / weekly / monthly totals, a bar chart of the current week, and a breakdown of where the money went. |
| **All spending** | Every logged spending, grouped by day. |
| **Profile** | Name, currency, the month at a glance, and a reset back to the demo data. |
| **Sheets** | **New expense** (amount, category chips, note) and **New category** (name, monthly budget, colour and icon). |

Data is a single JSON file, written after every change. On Android it lives in
the app's internal storage; on the desktop it is `%APPDATA%/Amara Budget` on
Windows or `~/.amara-budget` elsewhere. `BUDGET_DATA_DIR` overrides the path.

## Building the APK

The APK is built by GitHub Actions — see
[`.github/workflows/android.yml`](.github/workflows/android.yml). Every push to
`main` (or a manual "Run workflow") produces an installable artifact:

1. Open the **Actions** tab of the repository.
2. Pick the latest green **Android APK** run.
3. Download the **budget-apk** artifact and install `amara-budget.apk`.

The workflow installs the Android SDK, NDK r27 and the Rust Android targets,
then runs `cargo apk build` in `app/` for `aarch64-linux-android`,
`armv7-linux-androideabi` and `x86_64-linux-android`. The package is
debug-signed, so it installs on any phone or emulator without extra setup; a
release build needs a keystore in `[package.metadata.android.signing.release]`.

To build locally with the same toolchain you need the Android SDK, the NDK, a
JDK, and:

```sh
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
cargo install cargo-apk
cd app
ANDROID_NDK_HOME=/path/to/ndk cargo apk build
```

## Running on the desktop

```sh
cargo run -p budget-desktop
```

The desktop build is the same code as the phone build and is the quickest way
to work on the interface.

## Layout

```
app/       the app itself: cdylib + lib, and the cargo apk metadata
desktop/   a thin binary that runs the app in a window
```

- `app/src/app.rs` — app state, bottom navigation, sheet and toast hosting.
- `app/src/screens/` — one file per screen.
- `app/src/sheets.rs` — the two bottom sheets.
- `app/src/widgets.rs`, `app/src/icons.rs` — painted primitives and a
  hand-drawn vector icon set (no icon font, no images).
- `app/src/theme.rs` — palette, bundled Inter typefaces, egui style.
- `app/src/model.rs`, `store.rs`, `seed.rs` — data, JSON persistence and the
  demo workspace.
- `app/assets/fonts/` — Inter and Inter Display, bundled into the binary.
