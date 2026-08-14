use crate::{config::BackupConfig, logger, sentinel};
use anyhow::Result;
use std::path::PathBuf;
use std::time::{Duration, SystemTime};
use tracing::{info, warn};
use walkdir::WalkDir;

const MIN_INTERVAL_SECS: u64 = 23 * 3_600; // 23 h — une purge par jour maximum

/// Base backups conservés quel que soit leur âge — de quoi couvrir une restauration même après une
/// longue période d'arrêt.
const MIN_BASE_KEPT: usize = 2;

pub fn run(cfg: &BackupConfig) -> Result<()> {
    if sentinel::ran_recently(cfg, "purge", MIN_INTERVAL_SECS) {
        let msg = "[SKIP] purge : déjà effectuée il y a moins de 23 h.".to_string();
        info!("{msg}");
        logger::append(cfg, &msg)?;
        return Ok(());
    }

    let daily_cutoff = cfg.retention_daily_days * 86_400;
    let base_cutoff = cfg.retention_base_weeks * 7 * 86_400;

    let daily_removed = purge_dir(cfg.daily_dir(), daily_cutoff, false, cfg.min_daily_kept)?;
    let base_removed = purge_dir(cfg.base_dir(), base_cutoff, true, MIN_BASE_KEPT)?;

    let mut wal_removed = 0usize;
    if cfg.wal_archiving {
        // Les WAL ne sont exploitables qu'avec le base backup qui les précède : aucun plancher,
        // c'est la rétention du basebackup qui commande.
        wal_removed = purge_dir(cfg.wal_dir(), 7 * 86_400, false, 0)?;
    }

    sentinel::mark_ran(cfg, "purge")?;

    let msg = format!(
        "[OK] purge : {} dump(s), {} basebackup(s), {} WAL supprimés",
        daily_removed, base_removed, wal_removed
    );
    info!("{msg}");
    logger::append(cfg, &msg)
}

/// Supprime les entrées de `dir` plus anciennes que `max_age_secs`, en conservant toujours les
/// `min_kept` plus récentes.
///
/// Ce plancher n'est pas un raffinement : sur un poste resté éteint plusieurs semaines — congés,
/// panne, officine fermée — une purge purement calendaire déclarerait *toutes* les sauvegardes
/// expirées et viderait le répertoire au moment précis où ce sont les seules qui restent.
///
/// Aucune erreur d'entrée n'interrompt le balayage : un fichier verrouillé par l'antivirus ne doit
/// pas empêcher les vingt suivants d'être purgés — sinon le répertoire grossit indéfiniment pendant
/// que la purge se termine en échec, silencieusement, sous le Planificateur de tâches.
fn purge_dir(dir: PathBuf, max_age_secs: u64, dirs_only: bool, min_kept: usize) -> Result<usize> {
    if !dir.exists() {
        return Ok(0);
    }
    let cutoff = SystemTime::now() - Duration::from_secs(max_age_secs);

    // (date de modification, chemin) des entrées du bon type, les plus récentes d'abord.
    let mut candidates: Vec<(SystemTime, PathBuf)> = Vec::new();
    for entry in WalkDir::new(&dir).min_depth(1).max_depth(1) {
        let entry = match entry {
            Ok(e) => e,
            Err(err) => {
                warn!("Impossible de lire une entrée : {err}");
                continue;
            }
        };
        let meta = match entry.metadata() {
            Ok(m) => m,
            Err(err) => {
                warn!("Métadonnées illisibles ({}) : {err}", entry.path().display());
                continue;
            }
        };
        if meta.is_dir() != dirs_only {
            continue;
        }
        match meta.modified() {
            Ok(modified) => candidates.push((modified, entry.path().to_path_buf())),
            Err(err) => warn!("Date illisible ({}) : {err}", entry.path().display()),
        }
    }
    candidates.sort_unstable_by(|a, b| b.0.cmp(&a.0));

    let mut removed = 0usize;
    for (modified, path) in candidates.into_iter().skip(min_kept) {
        if modified > cutoff {
            continue;
        }
        let outcome = if dirs_only {
            std::fs::remove_dir_all(&path)
        } else {
            std::fs::remove_file(&path)
        };
        match outcome {
            Ok(()) => {
                info!("Supprimé : {}", path.display());
                removed += 1;
            }
            Err(err) => warn!("Suppression impossible ({}) : {err}", path.display()),
        }
    }
    Ok(removed)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::testing::{scratch_dir, write_aged_file};

    #[test]
    fn supprime_les_fichiers_expires_au_dela_du_plancher() {
        let dir = scratch_dir("purge_expires");
        for age_days in 1..=6 {
            write_aged_file(&dir, &format!("dump_{age_days}.dump"), age_days * 86_400);
        }

        // Rétention 2 jours, plancher 2 : les 2 plus récents sont gardés d'office, les 3 restants
        // (4, 5 et 6 jours) tombent — celui de 3 jours étant le 3ᵉ plus récent, il tombe aussi.
        let removed = purge_dir(dir.clone(), 2 * 86_400, false, 2).expect("purge");

        assert_eq!(removed, 4);
        assert_eq!(std::fs::read_dir(&dir).unwrap().count(), 2);
    }

    /// Le cas qui justifie le plancher : poste éteint des semaines, tout est expiré.
    #[test]
    fn ne_vide_jamais_le_repertoire_meme_si_tout_est_expire() {
        let dir = scratch_dir("purge_plancher");
        for i in 1..=3 {
            write_aged_file(&dir, &format!("dump_{i}.dump"), 400 * 86_400);
        }

        let removed = purge_dir(dir.clone(), 30 * 86_400, false, 10).expect("purge");

        assert_eq!(removed, 0);
        assert_eq!(std::fs::read_dir(&dir).unwrap().count(), 3);
    }

    #[test]
    fn ignore_les_repertoires_quand_on_purge_des_fichiers() {
        let dir = scratch_dir("purge_types");
        write_aged_file(&dir, "vieux.dump", 90 * 86_400);
        std::fs::create_dir_all(dir.join("base_20260101")).unwrap();

        let removed = purge_dir(dir.clone(), 30 * 86_400, false, 0).expect("purge");

        assert_eq!(removed, 1);
        assert!(dir.join("base_20260101").exists());
    }
}
