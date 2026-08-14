use crate::{config::BackupConfig, logger};
use anyhow::{Result, bail};
use std::time::{Duration, SystemTime};
use tracing::{error, info, warn};
use walkdir::WalkDir;

/// Vérifie qu'un dump suffisamment récent existe dans `{backup.directory}/daily`.
///
/// Le seuil vient de la configuration (`max_dump_age_hours`, 48 h par défaut) et non plus d'une
/// constante à 3 h : la tâche s'exécute au démarrage, *avant* le premier dump du jour, et le poste
/// peut être resté éteint tout un week-end. Un seuil serré ne produirait que des fausses alertes —
/// et c'est ainsi qu'une vraie panne de sauvegarde passe inaperçue.
pub fn run(cfg: &BackupConfig) -> Result<()> {
    let max_age_secs = cfg.max_dump_age_hours * 3_600;
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

    let cutoff = SystemTime::now() - Duration::from_secs(max_age_secs);
    let mut most_recent: Option<(SystemTime, std::path::PathBuf)> = None;

    for entry in WalkDir::new(&dir).min_depth(1).max_depth(1) {
        let entry = match entry {
            Ok(e) => e,
            Err(err) => {
                warn!("Impossible de lire une entrée : {err}");
                continue;
            }
        };
        // Une entrée illisible ne doit pas faire échouer le contrôle : il resterait peut-être un
        // dump parfaitement valide juste à côté.
        let Ok(meta) = entry.metadata() else { continue };
        if !meta.is_file() {
            continue;
        }
        let path = entry.path();
        if path.extension().and_then(|s| s.to_str()) != Some("dump") {
            continue;
        }
        let Ok(modified) = meta.modified() else {
            continue;
        };
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
                "[ERREUR] check : dump le plus récent trop ancien (> {} h) → {}",
                cfg.max_dump_age_hours,
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
