//! JSON persistence. One file, written on every change.

use std::path::{Path, PathBuf};

use crate::model::Data;

pub struct Store {
    path: PathBuf,
}

impl Store {
    pub fn new(dir: &Path) -> Self {
        let _ = std::fs::create_dir_all(dir);
        Self {
            path: dir.join("budget.json"),
        }
    }

    pub fn path(&self) -> &Path {
        &self.path
    }

    /// Load the workspace, seeding a demo one the first time.
    pub fn load(&self) -> Data {
        if let Ok(text) = std::fs::read_to_string(&self.path) {
            match serde_json::from_str::<Data>(&text) {
                Ok(data) => return data,
                Err(err) => log::warn!("could not read {}: {err}", self.path.display()),
            }
        }
        let data = crate::seed::demo();
        self.save(&data);
        data
    }

    pub fn save(&self, data: &Data) {
        match serde_json::to_string_pretty(data) {
            Ok(json) => {
                if let Err(err) = std::fs::write(&self.path, json) {
                    log::warn!("could not write {}: {err}", self.path.display());
                }
            }
            Err(err) => log::warn!("could not serialise data: {err}"),
        }
    }
}
