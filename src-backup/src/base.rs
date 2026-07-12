use crate::{config::BackupConfig, logger, pg_discover::find_pg_bin, sentinel};
use anyhow::{Result, bail};
use chrono::Local;
use std::process::Command;
use std::time::{Duration, SystemTime};
use tracing::{error, info};
use walkdir::WalkDir;

pub fn run(cfg: &BackupConfig) -> Result<()> {
    // Skip si un base backup a déjà été produit il y a moins de 6 jours.
    // Permet de déclencher la tâche AtStartup sans risquer de doublons.
    if let Some(age_days) = newest_base_age_days(cfg) {
        if age_days < 6.0 {
            let msg = format!(
                "[SKIP] base : backup récent ({:.1} j < 6) — rien à faire.",
                age_days
            );
            info!("{msg}");
            logger::append(cfg, &msg)?;
            return Ok(());
        }
    }

    // Localiser PostgreSQL AVANT de créer le répertoire : un dossier base_*
    // vide laissé par un échec ferait skipper les prochains runs pendant 6 jours.
    let pg_bin = find_pg_bin(cfg.pg_bin.as_deref())?;

    let dest = cfg
        .base_dir()
        .join(format!("base_{}", Local::now().format("%Y%m%d")));
    std::fs::create_dir_all(&dest)?;

    info!("pg_basebackup → {}", dest.display());

    let exe = pg_bin.join(if cfg!(windows) {
        "pg_basebackup.exe"
    } else {
        "pg_basebackup"
    });
    let mut cmd = Command::new(exe);
    cmd.args([
        "-h",
        &cfg.host,
        "-p",
        &cfg.port.to_string(),
        "-U",
        &cfg.user,
        "-D",
        dest.to_str().unwrap(),
        "-F",
        "tar",
        "-z",
        "-Xs",
        "--checkpoint=fast",
        "--progress",
    ]);
    if let Ok(pw) = std::env::var("PGPASSWORD") {
        cmd.env("PGPASSWORD", pw);
    } else if let Some(pw) = &cfg.password {
        cmd.env("PGPASSWORD", pw);
    }

    let status = cmd.status()?;
    if !status.success() {
        // Ne pas laisser un répertoire incomplet : il fausserait le skip < 6 j
        // et la purge le prendrait pour un backup valide.
        let _ = std::fs::remove_dir_all(&dest);
        let msg = format!("[ERREUR] basebackup échoué → {}", dest.display());
        error!("{msg}");
        logger::append(cfg, &msg)?;
        bail!("{msg}");
    }

    sentinel::mark_ran(cfg, "base")?;
    let msg = format!("[OK] basebackup → {}", dest.display());
    info!("{msg}");
    logger::append(cfg, &msg)
}

/// Age en jours du répertoire `base_*` le plus récent, ou None si aucun.
fn newest_base_age_days(cfg: &BackupConfig) -> Option<f64> {
    let dir = cfg.base_dir();
    if !dir.exists() {
        return None;
    }

    let mut newest: Option<SystemTime> = None;
    for entry in WalkDir::new(&dir).min_depth(1).max_depth(1) {
        let entry = entry.ok()?;
        if !entry.metadata().ok()?.is_dir() {
            continue;
        }
        // Un dossier vide est un reste d'échec, pas un backup valide.
        let non_empty = std::fs::read_dir(entry.path())
            .map(|mut d| d.next().is_some())
            .unwrap_or(false);
        if !non_empty {
            continue;
        }
        if let Ok(m) = entry.metadata().unwrap().modified() {
            newest = Some(match newest {
                Some(prev) if prev > m => prev,
                _ => m,
            });
        }
    }

    newest.map(|t| {
        t.elapsed()
            .unwrap_or(Duration::MAX)
            .as_secs_f64()
            / 86_400.0
    })
}
