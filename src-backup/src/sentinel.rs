use crate::config::BackupConfig;
use anyhow::Result;
use std::time::Duration;

/// Returns `true` if a sentinel file for `name` was written less than `min_secs` seconds ago,
/// meaning the task already ran recently and should be skipped.
pub fn ran_recently(cfg: &BackupConfig, name: &str, min_secs: u64) -> bool {
    let path = cfg.log_dir().join(format!(".last_{name}"));
    match std::fs::metadata(&path) {
        Ok(m) => m
            .modified()
            .map(|t| t.elapsed().unwrap_or(Duration::MAX).as_secs() < min_secs)
            .unwrap_or(false),
        Err(_) => false,
    }
}

/// Writes (or touches) the sentinel file for `name`, recording that the task just ran.
pub fn mark_ran(cfg: &BackupConfig, name: &str) -> Result<()> {
    std::fs::create_dir_all(cfg.log_dir())?;
    let path = cfg.log_dir().join(format!(".last_{name}"));
    // Write an empty file — mtime is the timestamp we care about.
    std::fs::write(&path, b"")?;
    Ok(())
}
