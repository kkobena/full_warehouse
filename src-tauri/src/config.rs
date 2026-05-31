use serde::{Deserialize, Deserializer, Serialize};
use std::fs;
use std::path::PathBuf;
use tauri::{AppHandle, Manager};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ServerConfig {
    pub port: u16,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LoggingConfig {
    pub directory: String,
    pub file: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct InstallationConfig {
    pub directory: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JvmConfig {
    #[serde(default = "default_heap_min")]
    pub heap_min: String,
    #[serde(default = "default_heap_max")]
    pub heap_max: String,
    #[serde(default = "default_metaspace_size")]
    pub metaspace_size: String,
    #[serde(default = "default_metaspace_max")]
    pub metaspace_max: String,
    #[serde(default = "default_direct_memory")]
    pub direct_memory_size: String,
    #[serde(default = "default_gc_pause_millis")]
    pub max_gc_pause_millis: String,
    #[serde(default = "default_additional_options")]
    pub additional_options: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FileImportConfig {
    #[serde(default = "default_json_dir")]
    pub json: String,
    #[serde(default = "default_csv_dir")]
    pub csv: String,
    #[serde(default = "default_excel_dir")]
    pub excel: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FileConfig {
    #[serde(default = "default_report_dir")]
    pub report: String,
    #[serde(default = "default_images_dir")]
    pub images: String,
    #[serde(default)]
    pub import: FileImportConfig,
    #[serde(default = "default_pharmaml")]
    pub pharmaml: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FneConfig {
    #[serde(default = "default_fne_url")]
    pub url: String,
    #[serde(default = "default_fne_api_key", rename = "api-key")]
    pub api_key: String,
    #[serde(default = "default_fne_point_of_sale", rename = "point-of-sale")]
    pub point_of_sale: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MailConfig {
    #[serde(default = "default_mail_username")]
    pub username: String,
    #[serde(default = "default_mail_email")]
    pub email: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JobsConfig {
    #[serde(default = "default_jobs_nightly_pipeline_cron", rename = "nightly-pipeline-cron")]
    pub nightly_pipeline_cron: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SemoisConfig {
    #[serde(default = "default_semois_freeze_delay_days", rename = "freeze-delay-days")]
    pub freeze_delay_days: u32,
    #[serde(default = "default_semois_batch_size", rename = "batch-size")]
    pub batch_size: u32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ViewsConfig {
    #[serde(default = "default_views_dashboards_cron", rename = "dashboards-cron")]
    pub dashboards_cron: String,
    #[serde(default = "default_views_analytique_cron", rename = "analytique-cron")]
    pub analytique_cron: String,
    #[serde(default = "default_views_reporting_cron", rename = "reporting-cron")]
    pub reporting_cron: String,
}

/// Désérialiseur partagé : `""` → `None`, toute autre valeur → `Some(value)`.
/// Garantit que les champs DB vides dans config.json n'écrasent pas les
/// defaults de Spring Boot définis dans application-prod.yml.
fn empty_string_as_none<'de, D>(de: D) -> Result<Option<String>, D::Error>
where
    D: Deserializer<'de>,
{
    let s: Option<String> = Option::deserialize(de)?;
    Ok(s.filter(|v| !v.trim().is_empty()))
}

/// Configuration de la connexion PostgreSQL.
/// Les champs `url`, `username`, `password` et `schema` sont optionnels :
/// s'ils sont absents ou vides (`""`), Spring Boot résout les credentials depuis
/// `application-prod.yml` (variables d'environnement PHARMA_DB_*).
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DatabaseConfig {
    /// Hôte PostgreSQL — utilisé pour le ping TCP de disponibilité au démarrage
    #[serde(default = "default_db_host")]
    pub host: String,
    /// Port PostgreSQL (défaut : 5432)
    #[serde(default = "default_db_port")]
    pub port: u16,
    /// Délai maximum (en secondes) pour attendre PostgreSQL (défaut : 30s)
    #[serde(default = "default_db_check_timeout_secs", rename = "check-timeout-secs")]
    pub check_timeout_secs: u64,
    /// URL JDBC complète — injectée dans `--spring.datasource.url` si non vide
    #[serde(default, skip_serializing_if = "Option::is_none", deserialize_with = "empty_string_as_none")]
    pub url: Option<String>,
    /// Utilisateur PostgreSQL — injecté dans `--spring.datasource.username` si non vide
    #[serde(default, skip_serializing_if = "Option::is_none", deserialize_with = "empty_string_as_none")]
    pub username: Option<String>,
    /// Mot de passe PostgreSQL — injecté dans `--spring.datasource.password` si non vide
    #[serde(default, skip_serializing_if = "Option::is_none", deserialize_with = "empty_string_as_none")]
    pub password: Option<String>,
    /// Schéma PostgreSQL — injecté dans `--spring.jpa.properties.hibernate.default_schema`
    /// et `--spring.flyway.schemas` si non vide
    #[serde(default, skip_serializing_if = "Option::is_none", deserialize_with = "empty_string_as_none")]
    pub schema: Option<String>,
}

// Default values for JVM configuration
fn default_heap_min() -> String {
    "2g".to_string()
}
fn default_heap_max() -> String {
    "2g".to_string()
}
fn default_metaspace_size() -> String {
    "256m".to_string()
}
fn default_metaspace_max() -> String {
    "384m".to_string()
}
fn default_direct_memory() -> String {
    "384m".to_string()
}
fn default_gc_pause_millis() -> String {
    "200".to_string()
}
fn default_additional_options() -> Vec<String> {
    Vec::new()
}

// Default values for File configuration
fn default_report_dir() -> String {
    "./reports".to_string()
}
fn default_images_dir() -> String {
    "./images".to_string()
}
fn default_json_dir() -> String {
    "./json".to_string()
}
fn default_csv_dir() -> String {
    "./csv".to_string()
}
fn default_excel_dir() -> String {
    "./excel".to_string()
}
fn default_pharmaml() -> String {
    "./pharmaml".to_string()
}

// Default values for FNE configuration
fn default_fne_url() -> String {
    String::new()
}
fn default_fne_api_key() -> String {
    String::new()
}
fn default_fne_point_of_sale() -> String {
    String::new()
}

// Default values for Mail configuration
fn default_mail_username() -> String {
    String::new()
}
fn default_mail_email() -> String {
    String::new()
}

// Default values for Jobs configuration
fn default_jobs_nightly_pipeline_cron() -> String {
    "0 0 9 * * *".to_string()
}

// Default values for Semois configuration
fn default_semois_freeze_delay_days() -> u32 {
    30
}
fn default_semois_batch_size() -> u32 {
    100
}

// Default values for Views configuration
fn default_views_dashboards_cron() -> String {
    "0 */15 8-20 * * *".to_string()
}
fn default_views_analytique_cron() -> String {
    "0 0 8-20 * * *".to_string()
}
fn default_views_reporting_cron() -> String {
    "0 0 9,12,15,18 * * *".to_string()
}

// Default values for Database configuration
fn default_db_host() -> String {
    "localhost".to_string()
}
fn default_db_port() -> u16 {
    5432
}
fn default_db_check_timeout_secs() -> u64 {
    30
}

// Default value for Port-Com configuration
fn default_port_com() -> String {
    "".to_string()
}

impl Default for JobsConfig {
    fn default() -> Self {
        Self {
            nightly_pipeline_cron: default_jobs_nightly_pipeline_cron(),
        }
    }
}

impl Default for SemoisConfig {
    fn default() -> Self {
        Self {
            freeze_delay_days: default_semois_freeze_delay_days(),
            batch_size: default_semois_batch_size(),
        }
    }
}

impl Default for ViewsConfig {
    fn default() -> Self {
        Self {
            dashboards_cron: default_views_dashboards_cron(),
            analytique_cron: default_views_analytique_cron(),
            reporting_cron: default_views_reporting_cron(),
        }
    }
}

impl Default for DatabaseConfig {
    fn default() -> Self {
        Self {
            host: default_db_host(),
            port: default_db_port(),
            check_timeout_secs: default_db_check_timeout_secs(),
            url: None,
            username: None,
            password: None,
            schema: None,
        }
    }
}

impl Default for JvmConfig {
    fn default() -> Self {
        Self {
            heap_min: default_heap_min(),
            heap_max: default_heap_max(),
            metaspace_size: default_metaspace_size(),
            metaspace_max: default_metaspace_max(),
            direct_memory_size: default_direct_memory(),
            max_gc_pause_millis: default_gc_pause_millis(),
            additional_options: default_additional_options(),
        }
    }
}

impl Default for FileImportConfig {
    fn default() -> Self {
        Self {
            json: default_json_dir(),
            csv: default_csv_dir(),
            excel: default_excel_dir(),
        }
    }
}

impl Default for FileConfig {
    fn default() -> Self {
        Self {
            report: default_report_dir(),
            images: default_images_dir(),
            import: FileImportConfig::default(),
            pharmaml: default_pharmaml(),
        }
    }
}

impl Default for FneConfig {
    fn default() -> Self {
        Self {
            url: default_fne_url(),
            api_key: default_fne_api_key(),
            point_of_sale: default_fne_point_of_sale(),
        }
    }
}

impl Default for MailConfig {
    fn default() -> Self {
        Self {
            username: default_mail_username(),
            email: default_mail_email(),
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AppConfig {
    pub server: ServerConfig,
    pub logging: LoggingConfig,
    pub installation: InstallationConfig,
    #[serde(default)]
    pub jvm: JvmConfig,
    #[serde(default)]
    pub file: FileConfig,
    #[serde(default)]
    pub fne: FneConfig,
    #[serde(default)]
    pub mail: MailConfig,
    #[serde(default = "default_port_com", rename = "port-com")]
    pub port_com: String,
    #[serde(default)]
    pub jobs: JobsConfig,
    #[serde(default)]
    pub semois: SemoisConfig,
    #[serde(default)]
    pub views: ViewsConfig,
    /// Configuration PostgreSQL pour le ping de disponibilité au démarrage.
    #[serde(default)]
    pub database: DatabaseConfig,
    /// Mis à true une fois que l'utilisateur a validé le wizard de configuration
    /// initial (paramètres DB + port serveur). Tant qu'il est false, Tauri
    /// affiche l'écran de setup au lieu de démarrer le backend.
    #[serde(default = "default_setup_complete")]
    pub setup_complete: bool,
}

fn default_setup_complete() -> bool {
    false
}

impl AppConfig {
    /// Retourne true si la configuration initiale a été validée par l'utilisateur.
    pub fn is_setup_complete(&self) -> bool {
        self.setup_complete
    }
}

impl Default for AppConfig {
    fn default() -> Self {
        // Default configuration
        let default_log_dir = if let Some(home_dir) = dirs::home_dir() {
            home_dir
                .join("PharmaSmart")
                .join("logs")
                .to_string_lossy()
                .to_string()
        } else {
            std::env::temp_dir()
                .join("PharmaSmart")
                .join("logs")
                .to_string_lossy()
                .to_string()
        };

        let log_file = format!("{}/pharmasmart.log", default_log_dir);

        Self {
            server: ServerConfig { port: 9080 },
            logging: LoggingConfig {
                directory: default_log_dir.clone(),
                file: log_file,
            },
            installation: InstallationConfig {
                directory: String::from(""),
            },
            jvm: JvmConfig::default(),
            file: FileConfig::default(),
            fne: FneConfig::default(),
            mail: MailConfig::default(),
            port_com: default_port_com(),
            jobs: JobsConfig::default(),
            semois: SemoisConfig::default(),
            views: ViewsConfig::default(),
            database: DatabaseConfig::default(),
            setup_complete: false,
        }
    }
}

impl AppConfig {
    /// Check whether a directory is actually writable by attempting to create a temp file.
    /// Q8 : Le nom du fichier test inclut le PID pour éviter les collisions
    ///       en cas de démarrage simultané de plusieurs instances.
    fn is_dir_writable(dir: &PathBuf) -> bool {
        let test = dir.join(format!(".pharmasmart_write_test_{}", std::process::id()));
        if fs::write(&test, b"").is_ok() {
            let _ = fs::remove_file(&test);
            true
        } else {
            false
        }
    }

    /// Return candidate directories for config.json, in priority order:
    /// 1. Exe directory             — original location, next to the sidecar JAR
    /// 2. $PROGRAMDATA\PharmaSmart  — all-users Program Files installs (installer writes here)
    /// 3. $APPDATA\PharmaSmart      — per-user installs without elevation
    /// 4. resource_dir / its parent — Tauri fallback
    fn config_search_dirs(app: &AppHandle) -> Vec<PathBuf> {
        let mut dirs: Vec<PathBuf> = Vec::new();

        // Priority 1: next to the exe (same level as the sidecar JAR — original behaviour)
        if let Some(exe_dir) = std::env::current_exe()
            .ok()
            .and_then(|p| p.parent().map(|p| p.to_path_buf()))
        {
            dirs.push(exe_dir);
        }

        // Priority 2: $PROGRAMDATA\PharmaSmart (all-users installer writes here when exe is read-only)
        #[cfg(windows)]
        if let Ok(program_data) = std::env::var("PROGRAMDATA") {
            dirs.push(PathBuf::from(program_data).join("PharmaSmart"));
        }

        // Priority 3: $APPDATA\PharmaSmart (per-user NSIS install without elevation)
        #[cfg(windows)]
        if let Ok(app_data) = std::env::var("APPDATA") {
            dirs.push(PathBuf::from(app_data).join("PharmaSmart"));
        }

        // Priority 4: resource_dir itself, then its parent (Tauri fallback)
        if let Ok(resource_dir) = app.path().resource_dir() {
            dirs.push(resource_dir.clone());
            if let Some(parent) = resource_dir.parent() {
                dirs.push(parent.to_path_buf());
            }
        }

        dirs
    }

    /// Return the first writable config directory, in priority order:
    /// 1. Exe directory — original location, next to the sidecar JAR (most installs)
    /// 2. $PROGRAMDATA\PharmaSmart — fallback for Program Files (read-only exe dir)
    /// 3. $APPDATA\PharmaSmart     — fallback for per-user installs without elevation
    ///
    /// Each candidate is tested for actual write access (not just existence).
    fn writable_config_dir(app: &AppHandle) -> PathBuf {
        // Candidate 1: next to the executable (same level as the sidecar JAR — original behaviour)
        if let Some(exe_dir) = std::env::current_exe()
            .ok()
            .and_then(|p| p.parent().map(|p| p.to_path_buf()))
        {
            if Self::is_dir_writable(&exe_dir) {
                return exe_dir;
            }
        }

        // Candidates 2 & 3: Windows-only fallbacks when exe dir is read-only (Program Files)
        #[cfg(windows)]
        {
            if let Ok(program_data) = std::env::var("PROGRAMDATA") {
                let dir = PathBuf::from(program_data).join("PharmaSmart");
                let _ = fs::create_dir_all(&dir);
                if Self::is_dir_writable(&dir) {
                    return dir;
                }
            }

            if let Ok(app_data) = std::env::var("APPDATA") {
                let dir = PathBuf::from(app_data).join("PharmaSmart");
                if fs::create_dir_all(&dir).is_ok() && Self::is_dir_writable(&dir) {
                    return dir;
                }
            }
        }

        // Last resort
        app.path()
            .resource_dir()
            .ok()
            .and_then(|r| r.parent().map(|p| p.to_path_buf()))
            .unwrap_or_else(|| std::env::current_dir().unwrap_or_else(|_| PathBuf::from(".")))
    }

    /// Load configuration from file, falling back to defaults if no file is found.
    ///
    /// On first launch (no config.json anywhere):
    /// 1. Load `config.default.json` from the resource/exe directory if present —
    ///    this preserves any user-tunable fields (server.port, jvm, fne, mail, port-com).
    /// 2. Override path fields (logging, installation, file.*) with absolute paths
    ///    resolved from `writable_config_dir()` so they are always valid.
    /// 3. Save the resulting `config.json` to `writable_config_dir()` and also
    ///    copy it next to the executable for easy discovery/editing.
    pub fn load(app: &AppHandle) -> Self {
        let search_dirs = Self::config_search_dirs(app);

        for dir in &search_dirs {
            let config_path = dir.join("config.json");
            if !config_path.exists() {
                continue;
            }
            println!("Loading configuration from: {:?}", config_path);
            match fs::read_to_string(&config_path) {
                Ok(config_str) => match serde_json::from_str::<AppConfig>(&config_str) {
                    Ok(config) => return config,
                    Err(e) => eprintln!("Failed to parse {:?}: {} — trying next location", config_path, e),
                },
                Err(e) => eprintln!("Failed to read {:?}: {} — trying next location", config_path, e),
            }
        }

        println!("No config.json found — generating from config.default.json");
        let data_dir = Self::writable_config_dir(app);
        let config = Self::generate_from_default(app, &data_dir);

        // Persist so the file is present on subsequent launches and for editing.
        if let Ok(json) = serde_json::to_string_pretty(&config) {
            let dest = data_dir.join("config.json");
            if let Err(e) = fs::write(&dest, &json) {
                eprintln!("Could not save config.json to {:?}: {}", dest, e);
            } else {
                println!("config.json generated at {:?}", dest);
            }
        }

        config
    }

    /// Build a configuration by merging `config.default.json` (for user-tunable fields)
    /// with absolute paths derived from `data_dir` (for path fields).
    fn generate_from_default(app: &AppHandle, data_dir: &PathBuf) -> Self {
        // Base: absolute paths from data_dir
        let mut config = Self::default_with_data_dir(data_dir);

        // Overlay: preserve user-tunable fields from config.default.json if present
        let default_path = Self::find_default_config(app);
        if let Some(path) = default_path {
            if let Ok(text) = fs::read_to_string(&path) {
                if let Ok(seed) = serde_json::from_str::<AppConfig>(&text) {
                    config.server = seed.server;
                    config.jvm    = seed.jvm;
                    config.fne    = seed.fne;
                    config.mail   = seed.mail;
                    config.port_com = seed.port_com;
                    config.jobs   = seed.jobs;
                    config.semois = seed.semois;
                    config.views  = seed.views;
                    config.database = seed.database;
                    println!("Merged user-tunable fields from {:?}", path);
                }
            }
        }

        config
    }

    /// Search for `config.default.json` in exe dir and resource dir.
    fn find_default_config(app: &AppHandle) -> Option<PathBuf> {
        let mut candidates: Vec<PathBuf> = Vec::new();

        if let Some(exe_dir) = std::env::current_exe()
            .ok()
            .and_then(|p| p.parent().map(|p| p.to_path_buf()))
        {
            candidates.push(exe_dir.join("config.default.json"));
        }
        if let Ok(res_dir) = app.path().resource_dir() {
            candidates.push(res_dir.join("config.default.json"));
        }

        candidates.into_iter().find(|p| p.exists())
    }

    /// Create default configuration rooted at the given data directory.
    /// All path fields are absolute so Spring Boot resolves them correctly
    /// regardless of the working directory at launch time.
    fn default_with_data_dir(data_dir: &PathBuf) -> Self {
        let s = |p: PathBuf| p.to_string_lossy().to_string();

        Self {
            server: ServerConfig { port: 9080 },
            logging: LoggingConfig {
                directory: s(data_dir.join("logs")),
                file: s(data_dir.join("logs").join("pharmasmart.log")),
            },
            installation: InstallationConfig {
                directory: s(data_dir.to_path_buf()),
            },
            jvm: JvmConfig::default(),
            file: FileConfig {
                report:  s(data_dir.join("reports")),
                images:  s(data_dir.join("images")),
                import: FileImportConfig {
                    json:  s(data_dir.join("json")),
                    csv:   s(data_dir.join("csv")),
                    excel: s(data_dir.join("excel")),
                },
                pharmaml: s(data_dir.join("pharmaml")),
            },
            fne: FneConfig::default(),
            mail: MailConfig::default(),
            port_com: default_port_com(),
            jobs: JobsConfig::default(),
            semois: SemoisConfig::default(),
            views: ViewsConfig::default(),
            database: DatabaseConfig::default(),
            setup_complete: false,
        }
    }

    /// Sauvegarde la configuration dans le répertoire de config accessible en écriture.
    /// Utilise `$PROGRAMDATA\PharmaSmart` si disponible pour éviter les erreurs de droits
    /// dans Program Files.
    pub fn save(&self, app: &AppHandle) -> Result<(), String> {
        let config_dir = Self::writable_config_dir(app);
        let config_path = config_dir.join("config.json");

        let config_json = serde_json::to_string_pretty(self)
            .map_err(|e| format!("Failed to serialize config: {}", e))?;

        fs::write(&config_path, &config_json)
            .map_err(|e| format!("Failed to write config to {:?}: {}", config_path, e))?;

        println!("Configuration saved to {:?}", config_path);

        // config_search_dirs() reads the exe-dir copy first (priority 1).
        // If the writable dir differs (e.g. Program Files install → PROGRAMDATA),
        // we must also update the exe-dir copy so the next load picks up the new values.
        if let Some(exe_dir) = std::env::current_exe()
            .ok()
            .and_then(|p| p.parent().map(|p| p.to_path_buf()))
        {
            if exe_dir != config_dir {
                let exe_copy = exe_dir.join("config.json");
                if let Err(e) = fs::write(&exe_copy, &config_json) {
                    println!("Note: could not update exe-dir config.json ({:?}): {}", exe_copy, e);
                } else {
                    println!("config.json mirror updated at {:?}", exe_copy);
                }
            }
        }

        Ok(())
    }

    /// Get the log file path as PathBuf
    pub fn get_log_path(&self) -> PathBuf {
        PathBuf::from(&self.logging.file)
    }

    /// Get the log directory as PathBuf
    pub fn get_log_dir(&self) -> PathBuf {
        PathBuf::from(&self.logging.directory)
    }

    /// Ensure log directory exists
    pub fn ensure_log_dir(&self) -> Result<(), String> {
        let log_dir = self.get_log_dir();
        fs::create_dir_all(&log_dir)
            .map_err(|e| format!("Failed to create log directory {:?}: {}", log_dir, e))?;

        Ok(())
    }
}
