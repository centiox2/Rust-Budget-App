use std::{fs, sync::Mutex};
use tauri::Manager;

#[tauri::command]
fn load_data(app: tauri::AppHandle, lock: tauri::State<'_, Mutex<()>>) -> Result<Option<serde_json::Value>, String> {
    let _guard = lock.lock().map_err(|e| e.to_string())?;
    let path = app.path().app_data_dir().map_err(|e| e.to_string())?.join("budget.json");
    if !path.exists() { return Ok(None); }
    let data = fs::read_to_string(path).map_err(|e| e.to_string())?;
    serde_json::from_str(&data).map(Some).map_err(|e| e.to_string())
}
#[tauri::command]
fn save_data(app: tauri::AppHandle, data: serde_json::Value, lock: tauri::State<'_, Mutex<()>>) -> Result<(), String> {
    let _guard = lock.lock().map_err(|e| e.to_string())?;
    if data["version"] != 1 || !data["transactions"].is_array() || !data["budgets"].is_object() {
        return Err("Invalid budget data".into());
    }
    let dir = app.path().app_data_dir().map_err(|e| e.to_string())?;
    fs::create_dir_all(&dir).map_err(|e| e.to_string())?;
    let bytes = serde_json::to_vec(&data).map_err(|e| e.to_string())?;
    let temp = dir.join("budget.tmp");
    fs::write(&temp, bytes).map_err(|e| e.to_string())?;
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
