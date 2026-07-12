use crate::{config::BackupConfig, logger, pg_discover::find_pg_bin};
use anyhow::{Result, bail};
use chrono::Local;
use std::process::Command;
use tracing::{error, info};

pub fn run(cfg: &BackupConfig) -> Result<()> {
    std::fs::create_dir_all(cfg.daily_dir())?;
    let pg_bin = find_pg_bin(cfg.pg_bin.as_deref())?;

    let timestamp = Local::now().format("%Y%m%d_%H%M%S");
    let filename = cfg
        .daily_dir()
        .join(format!("{}_{}.dump", cfg.db, timestamp));

    info!("pg_dump → {}", filename.display());

    let exe = pg_bin.join(if cfg!(windows) {
        "pg_dump.exe"
    } else {
        "pg_dump"
    });
    let mut cmd = Command::new(exe);
    cmd.args([
        "-h",
        &cfg.host,
        "-p",
        &cfg.port.to_string(),
        "-U",
        &cfg.user,
        "-F",
        "c",
        "-Z",
        "6", // Z9 divise le CPU par ~3 pour un gain marginal sur Z6
        "--no-password",
        "-f",
        filename.to_str().unwrap(),
        &cfg.db,
    ]);
    // Mot de passe via variable d'environnement (jamais dans la ligne de commande).
    // PGPASSWORD ambiant prioritaire, sinon celui de config.json — indispensable
    // sous le compte SYSTEM des tâches planifiées (pas de pgpass.conf).
    if let Ok(pw) = std::env::var("PGPASSWORD") {
        cmd.env("PGPASSWORD", pw);
    } else if let Some(pw) = &cfg.password {
        cmd.env("PGPASSWORD", pw);
    }

    let status = cmd.status()?;
    if !status.success() {
        // Supprimer le fichier partiel : il ferait passer le check et
        // ressemblerait à un dump valide dans daily/.
        let _ = std::fs::remove_file(&filename);
        let msg = format!("[ERREUR] dump échoué : {}", filename.display());
        error!("{msg}");
        logger::append(cfg, &msg)?;
        bail!("{msg}");
    }

    let size = std::fs::metadata(&filename)?.len();
    let msg = format!("[OK] dump → {} ({} octets)", filename.display(), size);
    info!("{msg}");
    logger::append(cfg, &msg)
}
