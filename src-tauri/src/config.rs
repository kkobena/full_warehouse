use serde::{Deserialize, Serialize};
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

// Default value for Port-Com configuration
fn default_port_com() -> String {
    "".to_string()
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
        }
    }
}

impl AppConfig {
    /// Return candidate directories for config.json, in priority order:
    /// 1. $PROGRAMDATA\PharmaSmart  — written by the NSIS installer, writable by all users
    ///    even when the app is installed in Program Files (no admin required at runtime).
    /// 2. The directory containing the .exe — for dev builds and per-user installs.
    fn config_search_dirs(app: &AppHandle) -> Vec<PathBuf> {
        let mut dirs: Vec<PathBuf> = Vec::new();

        // Priority 1: $PROGRAMDATA\PharmaSmart (set by NSIS installer)
        #[cfg(windows)]
        if let Ok(program_data) = std::env::var("PROGRAMDATA") {
            dirs.push(PathBuf::from(program_data).join("PharmaSmart"));
        }

        // Priority 2: directory next to the executable (dev / per-user install)
        if let Some(exe_dir) = std::env::current_exe()
            .ok()
            .and_then(|p| p.parent().map(|p| p.to_path_buf()))
        {
            dirs.push(exe_dir);
        }

        // Priority 3: resource directory parent (Tauri fallback)
        if let Ok(resource_dir) = app.path().resource_dir() {
            if let Some(parent) = resource_dir.parent() {
                dirs.push(parent.to_path_buf());
            }
        }

        dirs
    }

    /// Return the writable config directory: $PROGRAMDATA\PharmaSmart when available
    /// (always writable, even for Program Files installs), otherwise the exe dir.
    fn writable_config_dir(app: &AppHandle) -> PathBuf {
        #[cfg(windows)]
        {
            if let Ok(program_data) = std::env::var("PROGRAMDATA") {
                let dir = PathBuf::from(program_data).join("PharmaSmart");
                // Try to create it if not present (first launch after manual install)
                if fs::create_dir_all(&dir).is_ok() {
                    return dir;
                }
            }
        }

        // Fallback: exe directory
        std::env::current_exe()
            .ok()
            .and_then(|p| p.parent().map(|p| p.to_path_buf()))
            .or_else(|| {
                app.path()
                    .resource_dir()
                    .ok()
                    .and_then(|r| r.parent().map(|p| p.to_path_buf()))
            })
            .unwrap_or_else(|| std::env::current_dir().unwrap_or_else(|_| PathBuf::from(".")))
    }

    /// Load configuration from file, falling back to defaults if no file is found.
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

        println!("No valid config.json found — using defaults");
        let data_dir = Self::writable_config_dir(app);
        Self::default_with_data_dir(&data_dir)
    }

    /// Create default configuration rooted at the given data directory.
    fn default_with_data_dir(data_dir: &PathBuf) -> Self {
        let log_dir = data_dir.join("logs");
        let log_file = log_dir.join("pharmasmart.log");

        Self {
            server: ServerConfig { port: 9080 },
            logging: LoggingConfig {
                directory: log_dir.to_string_lossy().to_string(),
                file: log_file.to_string_lossy().to_string(),
            },
            installation: InstallationConfig {
                directory: data_dir.to_string_lossy().to_string(),
            },
            jvm: JvmConfig::default(),
            file: FileConfig::default(),
            fne: FneConfig::default(),
            mail: MailConfig::default(),
            port_com: default_port_com(),
        }
    }

    /// Save configuration to the writable config directory.
    /// Uses $PROGRAMDATA\PharmaSmart when available so the write never fails
    /// due to Program Files write restrictions.
    pub fn save(&self, app: &AppHandle) -> Result<(), String> {
        let config_dir = Self::writable_config_dir(app);
        let config_path = config_dir.join("config.json");

        let config_json = serde_json::to_string_pretty(self)
            .map_err(|e| format!("Failed to serialize config: {}", e))?;

        fs::write(&config_path, config_json)
            .map_err(|e| format!("Failed to write config to {:?}: {}", config_path, e))?;

        println!("Configuration saved to {:?}", config_path);
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
