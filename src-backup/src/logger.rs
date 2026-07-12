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

/// Journal de secours quand la config n'a pas pu être chargée (donc pas de
/// répertoire de backup connu) : `%ProgramData%\PharmaSmart\logs\backup-error.log`,
/// sinon un fichier à côté du binaire. Best-effort : n'échoue jamais.
pub fn append_fallback(msg: &str) {
    let dir = std::env::var_os("ProgramData")
        .map(|d| std::path::PathBuf::from(d).join("PharmaSmart").join("logs"))
        .or_else(|| {
            std::env::current_exe()
                .ok()
                .and_then(|p| p.parent().map(|d| d.to_path_buf()))
        });

    let Some(dir) = dir else { return };
    if std::fs::create_dir_all(&dir).is_err() {
        return;
    }

    let line = format!("{} {msg}\n", Local::now().format("%Y-%m-%d %H:%M:%S"));
    if let Ok(mut f) = OpenOptions::new()
        .create(true)
        .append(true)
        .open(dir.join("backup-error.log"))
    {
        let _ = f.write_all(line.as_bytes());
    }
}
