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
pub struct AppConfig {
    pub server: ServerConfig,
    pub logging: LoggingConfig,
    pub installation: InstallationConfig,
}

impl Default for AppConfig {
    fn default() -> Self {
        // Default configuration
        let default_log_dir = if let Some(home_dir) = dirs::home_dir() {
            home_dir.join("PharmaSmart").join("logs").to_string_lossy().to_string()
        } else {
            std::env::temp_dir().join("PharmaSmart").join("logs").to_string_lossy().to_string()
        };

        let log_file = format!("{}/pharmasmart.log", default_log_dir);

        Self {
            server: ServerConfig { port: 8080 },
            logging: LoggingConfig {
                directory: default_log_dir.clone(),
                file: log_file,
            },
            installation: InstallationConfig {
                directory: String::from(""),
            },
        }
    }
}

impl AppConfig {
    /// Load configuration from file, falling back to default if file doesn't exist
    pub fn load(app: &AppHandle) -> Self {
        // Get the application installation directory
        let app_dir = if let Ok(resource_dir) = app.path().resource_dir() {
            // In bundled app, use the directory containing the executable
            resource_dir.parent().unwrap_or(&resource_dir).to_path_buf()
        } else {
            // Fallback to current directory
            std::env::current_dir().unwrap_or_else(|_| PathBuf::from("."))
        };

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
            println!("config.json not found at {:?}, using defaults", config_path);
        }

        // Fallback to default configuration with app directory
        println!("Using default configuration");
        Self::default_with_app_dir(&app_dir)
    }

    /// Create default configuration with custom app directory
    fn default_with_app_dir(app_dir: &PathBuf) -> Self {
        // Set log directory in the application installation directory
        let log_dir = app_dir.join("logs");
        let log_file = log_dir.join("pharmasmart.log");

        Self {
            server: ServerConfig { port: 8080 },
            logging: LoggingConfig {
                directory: log_dir.to_string_lossy().to_string(),
                file: log_file.to_string_lossy().to_string(),
            },
            installation: InstallationConfig {
                directory: app_dir.to_string_lossy().to_string(),
            },
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
