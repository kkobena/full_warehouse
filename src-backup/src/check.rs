use crate::{config::BackupConfig, logger};
use anyhow::{Result, bail};
use std::time::{Duration, SystemTime};
use tracing::{error, info, warn};
use walkdir::WalkDir;

const MAX_DUMP_AGE_SECS: u64 = 3 * 3_600;

/// Vérifie qu'au moins un dump récent (< 3 h) existe dans `{backup.directory}/daily`.
pub fn run(cfg: &BackupConfig) -> Result<()> {
    let dir = cfg.daily_dir();
    if !dir.exists() {
        let msg = format!(
            "[ERREUR] check : répertoire des dumps absent → {}",
            dir.display()
        );
        error!("{msg}");
        logger::append(cfg, &msg)?;
        bail!("{msg}");
    }

    let cutoff = SystemTime::now() - Duration::from_secs(MAX_DUMP_AGE_SECS);
    let mut most_recent: Option<(SystemTime, std::path::PathBuf)> = None;

    for entry in WalkDir::new(&dir).min_depth(1).max_depth(1) {
        let entry = match entry {
            Ok(e) => e,
            Err(err) => {
                warn!("Impossible de lire une entrée : {err}");
                continue;
            }
        };
        let meta = entry.metadata()?;
        if !meta.is_file() {
            continue;
        }
        let path = entry.path();
        if path.extension().and_then(|s| s.to_str()) != Some("dump") {
            continue;
        }
        let modified = meta.modified()?;
        match &most_recent {
            Some((m, _)) if *m >= modified => {}
            _ => most_recent = Some((modified, path.to_path_buf())),
        }
    }

    match most_recent {
        Some((m, path)) if m >= cutoff => {
            let msg = format!("[OK] check : dump récent trouvé → {}", path.display());
            info!("{msg}");
            logger::append(cfg, &msg)
        }
        Some((_, path)) => {
            let msg = format!(
                "[ERREUR] check : dump le plus récent trop ancien (> 3 h) → {}",
                path.display()
            );
            error!("{msg}");
            logger::append(cfg, &msg)?;
            bail!("{msg}");
        }
        None => {
            let msg = format!(
                "[ERREUR] check : aucun dump *.dump dans {}",
                dir.display()
            );
            error!("{msg}");
            logger::append(cfg, &msg)?;
            bail!("{msg}");
        }
    }
}
