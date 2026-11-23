//! Desktop entry point. On Android the app is launched from `android_main` in
//! `lib.rs` instead, which `cargo apk` packages into the APK.

#[cfg(not(target_os = "android"))]
fn main() {
    budget_app::run_desktop();
}

/// Android never runs this binary — the APK is built from the library — but the
/// target still has to compile.
#[cfg(target_os = "android")]
fn main() {}
