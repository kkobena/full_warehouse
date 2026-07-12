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
    #[serde(default)]
    pub wal_archiving: bool,
    #[serde(default)]
    pub wal_directory: Option<PathBuf>,
}

#[derive(Debug, Deserialize)]
struct DatabaseConfig {
    #[serde(default)]
    password: Option<String>,
}

#[derive(Debug, Deserialize)]
struct AppConfig {
    backup: BackupConfig,
    #[serde(default)]
    database: Option<DatabaseConfig>,
}

fn default_retention_daily() -> u64 {
    30
}
fn default_retention_base() -> u64 {
    4
}

impl BackupConfig {
    pub fn load() -> Result<Self> {
        let path = find_config_file()
            .context("config.json introuvable (ProgramData, AppData, répertoire courant)")?;
        tracing::info!("config.json lu depuis : {}", path.display());

        let raw = std::fs::read_to_string(&path)
            .with_context(|| format!("Lecture de {}", path.display()))?;
        let app: AppConfig = serde_json::from_str(&raw)
            .context("Erreur de parsing config.json — section 'backup' manquante ?")?;
        let mut backup = app.backup;
        if backup.password.as_deref().map_or(true, str::is_empty) {
            backup.password = app
                .database
                .and_then(|d| d.password)
                .filter(|p| !p.is_empty());
        }
        Ok(backup)
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
