//! Desktop entry point. The Android APK is built from the library crate and
//! launched from `android_main` there instead.

fn main() {
    budget_app::run_desktop();
}
