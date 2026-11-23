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
2. Pick the latest **Android APK** run.
3. Download the **budget-apk** artifact and install `budget.apk`.

The workflow installs the Android SDK, NDK r27 and the Rust Android targets,
then runs `cargo apk build --release` for `aarch64-linux-android`,
`armv7-linux-androideabi` and `x86_64-linux-android` (so it runs on phones and
on emulators).

To build locally with the same toolchain you need the Android SDK, the NDK, a
JDK, and:

```sh
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
cargo install cargo-apk
ANDROID_NDK_HOME=/path/to/ndk cargo apk build --release
```

## Running on the desktop

```sh
cargo run
```

The desktop build is the same code as the phone build and is the quickest way
to work on the interface.

## Where things are

- `src/app.rs` — app state, bottom navigation, sheet and toast hosting.
- `src/screens/` — one file per screen.
- `src/sheets.rs` — the two bottom sheets.
- `src/widgets.rs`, `src/icons.rs` — painted primitives and a hand-drawn vector
  icon set (no icon font, no images).
- `src/theme.rs` — palette, bundled Inter typefaces, egui style.
- `src/model.rs`, `src/store.rs`, `src/seed.rs` — data, JSON persistence and the
  demo workspace.
- `assets/fonts/` — Inter and Inter Display, bundled into the binary.
