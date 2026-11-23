//! Desktop entry point. On Android the app is launched from `android_main` in
//! `lib.rs` instead, which `cargo apk` packages into the APK.

fn main() {
    budget_app::run_desktop();
}
