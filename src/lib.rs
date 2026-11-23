//! Amara — a budgeting app for Android, drawn entirely with egui.

mod app;
mod icons;
mod model;
mod screens;
mod seed;
mod sheets;
mod store;
mod theme;
mod time;
mod widgets;

use std::path::PathBuf;

pub use app::App;

fn default_data_dir() -> PathBuf {
    if let Ok(dir) = std::env::var("BUDGET_DATA_DIR") {
        return PathBuf::from(dir);
    }
    #[cfg(target_os = "windows")]
    if let Ok(appdata) = std::env::var("APPDATA") {
        return PathBuf::from(appdata).join("Amara Budget");
    }
    if let Ok(home) = std::env::var("HOME") {
        return PathBuf::from(home).join(".amara-budget");
    }
    PathBuf::from("budget-data")
}

fn launch(options: eframe::NativeOptions, data_dir: PathBuf) {
    log::info!("starting, data dir {}", data_dir.display());
    let result = eframe::run_native(
        "Budget",
        options,
        Box::new(move |cc| Ok(Box::new(App::new(cc, data_dir)))),
    );
    if let Err(err) = result {
        log::error!("could not start the app: {err}");
    }
}

#[cfg(not(target_os = "android"))]
pub fn run_desktop() {
    let options = eframe::NativeOptions {
        viewport: egui::ViewportBuilder::default()
            .with_inner_size([412.0, 892.0])
            .with_min_inner_size([340.0, 560.0])
            .with_title("Budget"),
        ..Default::default()
    };
    launch(options, default_data_dir());
}

#[cfg(target_os = "android")]
#[no_mangle]
pub fn android_main(android_app: winit::platform::android::activity::AndroidApp) {
    android_logger::init_once(
        android_logger::Config::default().with_max_level(log::LevelFilter::Info),
    );

    let data_dir = android_app
        .internal_data_path()
        .map(|path| path.to_path_buf())
        .unwrap_or_else(default_data_dir);

    let options = eframe::NativeOptions {
        android_app: Some(android_app),
        ..Default::default()
    };
    launch(options, data_dir);
}
