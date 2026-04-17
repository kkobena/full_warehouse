use crate::{config::BackupConfig, logger, sentinel};
use anyhow::Result;
use std::path::PathBuf;
use std::time::{Duration, SystemTime};
use tracing::{info, warn};
use walkdir::WalkDir;

const MIN_INTERVAL_SECS: u64 = 23 * 3_600; // 23 h — une purge par jour maximum

pub fn run(cfg: &BackupConfig) -> Result<()> {
    if sentinel::ran_recently(cfg, "purge", MIN_INTERVAL_SECS) {
        let msg = "[SKIP] purge : déjà effectuée il y a moins de 23 h.".to_string();
        info!("{msg}");
        logger::append(cfg, &msg)?;
        return Ok(());
    }

    let daily_cutoff = cfg.retention_daily_days * 86_400;
    let base_cutoff = cfg.retention_base_weeks * 7 * 86_400;

    let daily_removed = purge_dir(cfg.daily_dir(), daily_cutoff, false)?;
    let base_removed = purge_dir(cfg.base_dir(), base_cutoff, true)?;

    let mut wal_removed = 0usize;
    if cfg.wal_archiving {
        wal_removed = purge_dir(cfg.wal_dir(), 7 * 86_400, false)?;
    }

    sentinel::mark_ran(cfg, "purge")?;

    let msg = format!(
        "[OK] purge : {} dump(s), {} basebackup(s), {} WAL supprimés",
        daily_removed, base_removed, wal_removed
    );
    info!("{msg}");
    logger::append(cfg, &msg)
}

fn purge_dir(dir: PathBuf, max_age_secs: u64, dirs_only: bool) -> Result<usize> {
    if !dir.exists() {
        return Ok(0);
    }
    let cutoff = SystemTime::now() - Duration::from_secs(max_age_secs);
    let mut removed = 0usize;

    for entry in WalkDir::new(&dir).min_depth(1).max_depth(1) {
        let entry = match entry {
            Ok(e) => e,
            Err(err) => {
                warn!("Impossible de lire une entrée : {err}");
                continue;
            }
        };
        let meta = entry.metadata()?;
        if meta.modified()? > cutoff {
            continue;
        }

        if dirs_only && meta.is_dir() {
            std::fs::remove_dir_all(entry.path())?;
            info!("Supprimé (répertoire) : {}", entry.path().display());
            removed += 1;
        } else if !dirs_only && meta.is_file() {
            std::fs::remove_file(entry.path())?;
            info!("Supprimé (fichier) : {}", entry.path().display());
            removed += 1;
        }
    }
    Ok(removed)
}
