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

// Default values for JVM configuration
fn default_heap_min() -> String { "512m".to_string() }
fn default_heap_max() -> String { "1g".to_string() }
fn default_metaspace_size() -> String { "128m".to_string() }
fn default_metaspace_max() -> String { "256m".to_string() }
fn default_direct_memory() -> String { "256m".to_string() }
fn default_gc_pause_millis() -> String { "200".to_string() }
fn default_additional_options() -> Vec<String> { Vec::new() }

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

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AppConfig {
    pub server: ServerConfig,
    pub logging: LoggingConfig,
    pub installation: InstallationConfig,
    #[serde(default)]
    pub jvm: JvmConfig,
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
        }
    }
}

impl AppConfig {
    /// Load configuration from file, falling back to default if file doesn't exist
    pub fn load(app: &AppHandle) -> Self {
        // Get the application installation directory (where the .exe is located)
        let app_dir = std::env::current_exe()
            .ok()
            .and_then(|exe_path| {
                let dir = exe_path.parent().map(|p| p.to_path_buf());
                if let Some(ref d) = dir {
                    println!("Using executable directory: {:?}", d);
                }
                dir
            })
            .or_else(|| {
                app.path().resource_dir().ok().and_then(|resource_dir| {
                    let dir = resource_dir.parent().map(|p| p.to_path_buf());
                    if let Some(ref d) = dir {
                        println!("Using resource directory parent: {:?}", d);
                    }
                    dir
                })
            })
            .unwrap_or_else(|| {
                let dir = std::env::current_dir().unwrap_or_else(|_| PathBuf::from("."));
                println!("Using current directory fallback: {:?}", dir);
                dir
            });

        // Try to load from installation directory first
        let config_path = app_dir.join("config.json");

        if config_path.exists() {
            println!("Loading configuration from: {:?}", config_path);
            if let Ok(config_str) = fs::read_to_string(&config_path) {
                if let Ok(config) = serde_json::from_str::<AppConfig>(&config_str) {
                    println!("Configuration loaded successfully");
                    println!("  - Port: {}", config.server.port);
                    println!("  - Log Directory: {}", config.logging.directory);
                    println!("  - Log File: {}", config.logging.file);
                    return config;
                } else {
                    eprintln!("Failed to parse config.json, using defaults");
                }
            } else {
                eprintln!("Failed to read config.json, using defaults");
            }
        } else {
            println!("config.json not found at {:?}", config_path);

            // Try to copy default template from resources
            if let Ok(resource_dir) = app.path().resource_dir() {
                let default_config_path = resource_dir.join("config.default.json");

                if default_config_path.exists() {
                    println!("Found default configuration template at: {:?}", default_config_path);

                    // Create config with proper paths for this installation
                    let default: AppConfig = Self::default_with_app_dir(&app_dir);

                    // Save the default config to installation directory
                    if let Ok(config_json) = serde_json::to_string_pretty(&default) {
                        if fs::write(&config_path, config_json).is_ok() {
                            println!("Default configuration created at: {:?}", config_path);
                            println!("You can modify this file to customize your installation.");
                            return default;
                        }
                    }
                }
            }

            println!("Using default configuration");
        }

        // Fallback to default configuration with app directory
        Self::default_with_app_dir(&app_dir)
    }

    /// Create default configuration with custom app directory
    fn default_with_app_dir(app_dir: &PathBuf) -> Self {
        // Set log directory in the application installation directory
        let log_dir = app_dir.join("logs");
        let log_file = log_dir.join("pharmasmart.log");

        Self {
            server: ServerConfig { port: 9080 },
            logging: LoggingConfig {
                directory: log_dir.to_string_lossy().to_string(),
                file: log_file.to_string_lossy().to_string(),
            },
            installation: InstallationConfig {
                directory: app_dir.to_string_lossy().to_string(),
            },
            jvm: JvmConfig::default(),
        }
    }

    /// Save configuration to file
    pub fn save(&self, app: &AppHandle) -> Result<(), String> {
        if let Ok(resource_dir) = app.path().resource_dir() {
            let exe_dir = resource_dir.parent().unwrap_or(&resource_dir);
            let config_path = exe_dir.join("config.json");

            let config_json = serde_json::to_string_pretty(self)
                .map_err(|e| format!("Failed to serialize config: {}", e))?;

            fs::write(&config_path, config_json)
                .map_err(|e| format!("Failed to write config to {:?}: {}", config_path, e))?;

            println!("Configuration saved to: {:?}", config_path);
            Ok(())
        } else {
            Err("Failed to get resource directory".to_string())
        }
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
        println!("Log directory ensured: {:?}", log_dir);
        Ok(())
    }
}
