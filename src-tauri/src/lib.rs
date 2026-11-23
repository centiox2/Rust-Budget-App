mod model;
use model::BudgetData;
use std::{fs, io::Write, sync::Mutex};
use tauri::Manager;

#[tauri::command]
fn load_data(
    app: tauri::AppHandle,
    lock: tauri::State<'_, Mutex<()>>,
) -> Result<Option<BudgetData>, String> {
    let _guard = lock.lock().map_err(|e| e.to_string())?;
    let path = app
        .path()
        .app_data_dir()
        .map_err(|e| e.to_string())?
        .join("budget.json");
    if !path.exists() {
        return Ok(None);
    }
    let data = fs::read_to_string(path).map_err(|e| e.to_string())?;
    let parsed: BudgetData = serde_json::from_str(&data).map_err(|e| e.to_string())?;
    parsed.validate()?;
    Ok(Some(parsed))
}
#[tauri::command]
fn save_data(
    app: tauri::AppHandle,
    data: BudgetData,
    lock: tauri::State<'_, Mutex<()>>,
) -> Result<(), String> {
    let _guard = lock.lock().map_err(|e| e.to_string())?;
    data.validate()?;
    let dir = app.path().app_data_dir().map_err(|e| e.to_string())?;
    fs::create_dir_all(&dir).map_err(|e| e.to_string())?;
    let bytes = serde_json::to_vec(&data).map_err(|e| e.to_string())?;
    let temp = dir.join("budget.tmp");
    let mut file = fs::File::create(&temp).map_err(|e| e.to_string())?;
    file.write_all(&bytes).map_err(|e| e.to_string())?;
    file.sync_all().map_err(|e| e.to_string())?;
    drop(file);
    fs::rename(temp, dir.join("budget.json")).map_err(|e| e.to_string())
}
#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .manage(Mutex::new(()))
        .invoke_handler(tauri::generate_handler![load_data, save_data])
        .run(tauri::generate_context!())
        .expect("Unable to start Verdant Budget");
}
