use crate::config::BackupConfig;
use anyhow::{Context, Result};
use chrono::Local;
use std::fs::OpenOptions;
use std::io::Write;

/// Ajoute une ligne horodatée dans `{backup.directory}/logs/backup.log`.
pub fn append(cfg: &BackupConfig, msg: &str) -> Result<()> {
    std::fs::create_dir_all(cfg.log_dir())
        .with_context(|| format!("Création du répertoire de logs : {}", cfg.log_dir().display()))?;

    let line = format!("{} {msg}\n", Local::now().format("%Y-%m-%d %H:%M:%S"));

    let mut file = OpenOptions::new()
        .create(true)
        .append(true)
        .open(cfg.log_file())
        .with_context(|| format!("Ouverture du fichier log : {}", cfg.log_file().display()))?;
    file.write_all(line.as_bytes())?;
    Ok(())
}
