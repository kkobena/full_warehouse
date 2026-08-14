use crate::{config::BackupConfig, logger, pg_discover::find_pg_bin};
use anyhow::{Result, bail};
use chrono::{DateTime, Local, NaiveDate};
use std::process::Command;
use std::time::{Duration, SystemTime};
use tracing::{error, info};
use walkdir::WalkDir;

pub fn run(cfg: &BackupConfig) -> Result<()> {
    std::fs::create_dir_all(cfg.daily_dir())?;

    // Le rythme se décide ici, pas dans le Planificateur de tâches : voir defer_reason().
    if let Some(reason) = defer_reason(cfg) {
        info!("{reason}");
        logger::append(cfg, &reason)?;
        return Ok(());
    }

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

/// Motif de report du dump, ou `None` s'il faut le prendre maintenant.
///
/// La tâche planifiée se présente souvent — au démarrage puis toutes les 2 h — parce qu'un poste
/// d'officine s'allume et s'éteint à des heures qu'on ne connaît pas : un horaire fixe raterait
/// simplement ses rendez-vous. Le cadencement réel se décide donc ici, à partir de ce qui a
/// effectivement été produit dans la journée. Résultat : deux sauvegardes par jour ouvré, l'une au
/// premier démarrage, l'autre en début d'après-midi, sans dépendre de l'heure d'allumage.
///
/// En cas de doute (horloge incohérente, métadonnée illisible) on dumpe : rater une sauvegarde
/// coûte plus cher qu'en prendre une de trop.
fn defer_reason(cfg: &BackupConfig) -> Option<String> {
    let todays = todays_dumps(cfg, Local::now().date_naive());
    deferral(&todays, SystemTime::now(), cfg)
}

/// Cœur de décision, isolé du disque et de l'horloge pour être vérifiable.
fn deferral(todays: &[SystemTime], now: SystemTime, cfg: &BackupConfig) -> Option<String> {
    if todays.len() >= cfg.max_daily_dumps {
        return Some(format!(
            "[SKIP] dump : {} sauvegarde(s) déjà prise(s) aujourd'hui (maximum {}).",
            todays.len(),
            cfg.max_daily_dumps
        ));
    }

    let elapsed = now.duration_since(*todays.iter().max()?).ok()?;
    let minimum = Duration::from_secs(cfg.min_dump_interval_hours * 3_600);
    if elapsed < minimum {
        return Some(format!(
            "[SKIP] dump : dernière sauvegarde il y a {:.1} h (intervalle minimum {} h).",
            elapsed.as_secs_f64() / 3_600.0,
            cfg.min_dump_interval_hours
        ));
    }
    None
}

/// Dates de modification des dumps datés du jour `today` (heure locale).
fn todays_dumps(cfg: &BackupConfig, today: NaiveDate) -> Vec<SystemTime> {
    let mut found = Vec::new();

    for entry in WalkDir::new(cfg.daily_dir())
        .min_depth(1)
        .max_depth(1)
        .into_iter()
        .flatten()
    {
        if entry.path().extension().and_then(|s| s.to_str()) != Some("dump") {
            continue;
        }
        let Ok(meta) = entry.metadata() else { continue };
        if !meta.is_file() {
            continue;
        }
        let Ok(modified) = meta.modified() else {
            continue;
        };
        if DateTime::<Local>::from(modified).date_naive() == today {
            found.push(modified);
        }
    }
    found
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::testing::{scratch_dir, write_aged_file};
    use std::path::PathBuf;

    fn config(directory: PathBuf) -> BackupConfig {
        BackupConfig {
            directory,
            db: "pharma_smart".into(),
            host: "localhost".into(),
            port: 5432,
            user: "pharmasmart_backup".into(),
            password: None,
            pg_bin: None,
            retention_daily_days: 30,
            retention_base_weeks: 4,
            max_daily_dumps: 2,
            min_dump_interval_hours: 6,
            max_dump_age_hours: 48,
            min_daily_kept: 10,
            wal_archiving: false,
            wal_directory: None,
        }
    }

    fn daily_dir(label: &str) -> (BackupConfig, PathBuf) {
        let root = scratch_dir(label);
        let daily = root.join("daily");
        std::fs::create_dir_all(&daily).unwrap();
        (config(root), daily)
    }

    fn hours_ago(now: SystemTime, hours: u64) -> SystemTime {
        now - Duration::from_secs(hours * 3_600)
    }

    // ── cadencement ─────────────────────────────────────────────────────────

    #[test]
    fn premier_demarrage_de_la_journee_declenche_un_dump() {
        let (cfg, _) = daily_dir("dump_premier");
        let now = SystemTime::now();

        // Aucun dump daté d'aujourd'hui : celui de la veille ne compte pas dans le quota.
        assert_eq!(deferral(&[], now, &cfg), None);
    }

    #[test]
    fn un_dump_trop_recent_reporte_le_suivant() {
        let (cfg, _) = daily_dir("dump_recent");
        let now = SystemTime::now();

        let reason = deferral(&[hours_ago(now, 2)], now, &cfg).expect("report attendu");
        assert!(reason.contains("intervalle minimum"), "{reason}");
    }

    #[test]
    fn passe_le_delai_la_seconde_sauvegarde_du_jour_est_prise() {
        let (cfg, _) = daily_dir("dump_seconde");
        let now = SystemTime::now();

        assert_eq!(deferral(&[hours_ago(now, 7)], now, &cfg), None);
    }

    #[test]
    fn le_quota_journalier_bloque_la_troisieme() {
        let (cfg, _) = daily_dir("dump_quota");
        let now = SystemTime::now();

        let reason = deferral(&[hours_ago(now, 9), hours_ago(now, 7)], now, &cfg)
            .expect("report attendu");
        assert!(reason.contains("maximum 2"), "{reason}");
    }

    // ── recensement des dumps du jour ───────────────────────────────────────

    #[test]
    fn seuls_les_dumps_du_jour_demande_sont_comptes() {
        let (cfg, daily) = daily_dir("dump_recensement");
        let recent = write_aged_file(&daily, "pharma_smart_matin.dump", 3_600);
        write_aged_file(&daily, "pharma_smart_avant_hier.dump", 40 * 3_600);

        let jour = DateTime::<Local>::from(
            std::fs::metadata(&recent).unwrap().modified().unwrap(),
        )
        .date_naive();

        assert_eq!(todays_dumps(&cfg, jour).len(), 1);
        assert_eq!(todays_dumps(&cfg, jour - chrono::Duration::days(7)).len(), 0);
    }

    /// Un fichier étranger ne doit ni consommer le quota ni décaler le rythme.
    #[test]
    fn les_fichiers_hors_dump_sont_ignores() {
        let (cfg, daily) = daily_dir("dump_etrangers");
        write_aged_file(&daily, "notes.txt", 3_600);
        write_aged_file(&daily, "pharma_smart_partiel.tmp", 3_600);

        assert!(todays_dumps(&cfg, Local::now().date_naive()).is_empty());
    }
}
