use anyhow::{Context, Result};
use dirs::{config_dir, data_dir};
use serde::Deserialize;
use std::path::PathBuf;

#[derive(Debug, Deserialize)]
pub struct BackupConfig {
    pub directory: PathBuf,
    pub db: String,
    pub host: String,
    pub port: u16,
    pub user: String,
    /// Mot de passe passé à pg_dump/pg_basebackup via PGPASSWORD.
    /// Si absent de la section backup, repli sur database.password.
    #[serde(default)]
    pub password: Option<String>,
    /// Répertoire bin/ de PostgreSQL (pg_dump.exe, pg_basebackup.exe).
    /// Prioritaire sur la détection automatique (PGBIN, registre, chemins standards).
    #[serde(default)]
    pub pg_bin: Option<PathBuf>,
    #[serde(default = "default_retention_daily")]
    pub retention_daily_days: u64,
    #[serde(default = "default_retention_base")]
    pub retention_base_weeks: u64,
    /// Nombre maximum de dumps par jour calendaire.
    ///
    /// Le poste d'une officine n'est pas allumé 24 h/24 (hors gardes) : planifier des dumps à
    /// heures fixes en raterait la moitié. La tâche se présente donc souvent — au démarrage puis
    /// toutes les 2 h — et c'est *ici* que le rythme est décidé, sur ce que le poste a réellement
    /// fait dans la journée.
    #[serde(default = "default_max_daily_dumps")]
    pub max_daily_dumps: usize,
    /// Écart minimum entre deux dumps d'une même journée.
    ///
    /// Sans lui, les deux sauvegardes du jour seraient prises coup sur coup au démarrage et
    /// couvriraient la même chose : la seconde doit tomber assez tard pour capter l'activité de
    /// l'après-midi.
    #[serde(default = "default_min_dump_interval_hours")]
    pub min_dump_interval_hours: u64,
    /// Âge maximum toléré par `check` pour le dump le plus récent, en heures.
    ///
    /// 48 h et non 3 h : à deux dumps par jour et sur un poste éteint la nuit — voire tout un
    /// dimanche — un seuil serré ne signalerait que du bruit, et le jour où la sauvegarde casse
    /// vraiment plus personne ne regarde.
    #[serde(default = "default_max_dump_age_hours")]
    pub max_dump_age_hours: u64,
    /// Nombre de dumps conservés quoi qu'il arrive, même expirés.
    ///
    /// Filet indispensable sur un poste qui peut rester éteint des semaines : une purge purement
    /// calendaire viderait alors le répertoire de *toutes* les sauvegardes d'un coup, au moment
    /// précis où ce sont les seules qui restent.
    #[serde(default = "default_min_daily_kept")]
    pub min_daily_kept: usize,
    #[serde(default)]
    pub wal_archiving: bool,
    #[serde(default)]
    pub wal_directory: Option<PathBuf>,
}

#[derive(Debug, Deserialize)]
struct DatabaseConfig {
    #[serde(default)]
    password: Option<String>,
    #[serde(default)]
    username: Option<String>,
    #[serde(default)]
    host: Option<String>,
    #[serde(default)]
    port: Option<u16>,
    /// URL JDBC : `jdbc:postgresql://hôte:port/base`.
    #[serde(default)]
    url: Option<String>,
}

#[derive(Debug, Deserialize)]
struct AppConfig {
    /// Optionnelle : voir {@link BackupConfig::from_database}.
    #[serde(default)]
    backup: Option<BackupConfig>,
    #[serde(default)]
    database: Option<DatabaseConfig>,
}

fn default_retention_daily() -> u64 {
    30
}
fn default_retention_base() -> u64 {
    4
}
fn default_max_daily_dumps() -> usize {
    2
}
fn default_min_dump_interval_hours() -> u64 {
    6
}
fn default_max_dump_age_hours() -> u64 {
    48
}
fn default_min_daily_kept() -> usize {
    10
}

impl BackupConfig {
    pub fn load() -> Result<Self> {
        let path = find_config_file()
            .context("config.json introuvable (ProgramData, AppData, répertoire courant)")?;
        tracing::info!("config.json lu depuis : {}", path.display());

        let raw = std::fs::read_to_string(&path)
            .with_context(|| format!("Lecture de {}", path.display()))?;
        let app: AppConfig = serde_json::from_str(&raw).context("Erreur de parsing config.json")?;

        let db = app.database;
        let mut backup = match app.backup {
            Some(b) => b,
            // Section absente : plutôt que d'abandonner, on se rabat sur la section `database`.
            // L'incident réel : l'application Tauri réécrivait config.json sans la section
            // `backup`, et l'outil mourait ensuite à chaque exécution sur « missing field
            // `backup` » — plus aucun dump, plus aucune purge, pendant une semaine, sans que
            // personne ne le voie. Une sauvegarde ne doit pas s'arrêter parce qu'un réglage
            // secondaire a disparu.
            None => {
                tracing::warn!("Section 'backup' absente de config.json — repli sur la section 'database'");
                BackupConfig::from_database(db.as_ref())
                    .context("Section 'backup' absente et section 'database' inexploitable")?
            }
        };
        if backup.password.as_deref().is_none_or(str::is_empty) {
            backup.password = db.and_then(|d| d.password).filter(|p| !p.is_empty());
        }
        Ok(backup)
    }

    /// Construit une configuration de sauvegarde à partir des seuls paramètres de connexion de
    /// l'application, quand la section dédiée manque.
    ///
    /// Le compte applicatif est utilisé faute de mieux : il a de toute façon plus de droits que le
    /// compte de sauvegarde dédié, `pg_dump` fonctionnera. Seul `pg_basebackup` peut échouer, faute
    /// du privilège `REPLICATION` — c'est le prix d'un repli, et il est journalisé.
    fn from_database(db: Option<&DatabaseConfig>) -> Result<Self> {
        let db = db.context("aucune section 'database'")?;
        let url = db.url.as_deref().unwrap_or_default();
        let name = database_name(url).context("nom de la base introuvable dans database.url")?;
        let directory = std::env::var_os("ProgramData")
            .map(|d| PathBuf::from(d).join("PharmaSmart").join("backups"))
            .context("variable ProgramData absente")?;

        Ok(Self {
            directory,
            db: name,
            host: db.host.clone().unwrap_or_else(|| "localhost".to_string()),
            port: db.port.unwrap_or(5432),
            user: db
                .username
                .clone()
                .context("database.username absent")?,
            password: db.password.clone(),
            pg_bin: None,
            retention_daily_days: default_retention_daily(),
            retention_base_weeks: default_retention_base(),
            max_daily_dumps: default_max_daily_dumps(),
            min_dump_interval_hours: default_min_dump_interval_hours(),
            max_dump_age_hours: default_max_dump_age_hours(),
            min_daily_kept: default_min_daily_kept(),
            wal_archiving: false,
            wal_directory: None,
        })
    }

    pub fn daily_dir(&self) -> PathBuf {
        self.directory.join("daily")
    }
    pub fn base_dir(&self) -> PathBuf {
        self.directory.join("basebackup")
    }
    pub fn wal_dir(&self) -> PathBuf {
        self.wal_directory
            .clone()
            .filter(|p| !p.as_os_str().is_empty())
            .unwrap_or_else(|| self.directory.join("wal"))
    }
    pub fn log_dir(&self) -> PathBuf {
        self.directory.join("logs")
    }
    pub fn log_file(&self) -> PathBuf {
        self.log_dir().join("backup.log")
    }
}

/// Extrait le nom de la base d'une URL JDBC `jdbc:postgresql://hôte:port/base?options`.
fn database_name(url: &str) -> Option<String> {
    let after_host = url.rsplit_once('/')?.1;
    let name = after_host.split(['?', ';']).next()?.trim();
    (!name.is_empty()).then(|| name.to_string())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn le_nom_de_la_base_se_lit_dans_l_url_jdbc() {
        assert_eq!(
            database_name("jdbc:postgresql://localhost:5432/pharma_smart").as_deref(),
            Some("pharma_smart")
        );
        assert_eq!(
            database_name("jdbc:postgresql://srv:5432/pharma_smart?ssl=true").as_deref(),
            Some("pharma_smart")
        );
        assert_eq!(database_name("jdbc:postgresql://localhost:5432/"), None);
        assert_eq!(database_name(""), None);
    }

    /// Régression : une config amputée de sa section `backup` doit rester exploitable. C'est ce
    /// scénario qui a tenu la sauvegarde à l'arrêt pendant une semaine.
    #[test]
    fn une_config_sans_section_backup_reste_exploitable() {
        let db = DatabaseConfig {
            password: Some("secret".into()),
            username: Some("pharma_smart".into()),
            host: Some("localhost".into()),
            port: Some(5432),
            url: Some("jdbc:postgresql://localhost:5432/pharma_smart".into()),
        };

        let cfg = BackupConfig::from_database(Some(&db)).expect("repli attendu");

        assert_eq!(cfg.db, "pharma_smart");
        assert_eq!(cfg.user, "pharma_smart");
        assert_eq!(cfg.port, 5432);
        assert_eq!(cfg.max_daily_dumps, 2);
        assert!(cfg.directory.ends_with("PharmaSmart/backups") || cfg.directory.ends_with("PharmaSmart\\backups"));
    }

    #[test]
    fn sans_url_exploitable_le_repli_echoue_franchement() {
        let db = DatabaseConfig {
            password: None,
            username: Some("pharma_smart".into()),
            host: None,
            port: None,
            url: None,
        };

        assert!(BackupConfig::from_database(Some(&db)).is_err());
        assert!(BackupConfig::from_database(None).is_err());
    }
}

fn find_config_file() -> Option<PathBuf> {
    let exe_dir = std::env::current_exe()
        .ok()
        .and_then(|p| p.parent().map(|d| d.to_path_buf()));

    let candidates: Vec<PathBuf> = vec![
        // 1. %ProgramData%\PharmaSmart — install machine, visible par le compte
        //    SYSTEM des tâches planifiées. dirs::data_dir() ne couvre PAS ce cas :
        //    sous Windows il renvoie l'AppData Roaming de l'utilisateur courant.
        std::env::var_os("ProgramData")
            .map(|d| PathBuf::from(d).join("PharmaSmart").join("config.json"))
            .unwrap_or_default(),
        // 2. AppData\Roaming\PharmaSmart de l'utilisateur courant
        data_dir()
            .map(|d| d.join("PharmaSmart").join("config.json"))
            .unwrap_or_default(),
        config_dir()
            .map(|d| d.join("PharmaSmart").join("config.json"))
            .unwrap_or_default(),
        // 3. Répertoire du binaire, puis son parent (l'exe est installé dans
        //    $INSTDIR\backup\ alors que config.json est à la racine $INSTDIR)
        exe_dir
            .as_ref()
            .map(|d| d.join("config.json"))
            .unwrap_or_default(),
        exe_dir
            .as_ref()
            .and_then(|d| d.parent().map(|p| p.join("config.json")))
            .unwrap_or_default(),
    ];

    candidates.into_iter().find(|p| p.exists())
}
