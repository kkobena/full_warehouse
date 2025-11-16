// Prevents additional console window on Windows in release
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod printer;

#[cfg(feature = "bundled-backend")]
mod backend_manager;

#[cfg(feature = "bundled-backend")]
mod config;

#[cfg(feature = "bundled-backend")]
use backend_manager::{BackendState, start_backend};

#[cfg(feature = "bundled-backend")]
use tauri::Manager;

use tauri::Emitter;
use serde::Serialize;

#[derive(Clone, Serialize)]
pub struct BackendHealthStatus {
    pub available: bool,
    pub message: String,
}

/// Check if backend is available (works in both bundled and standard modes)
#[tauri::command]
async fn check_backend_health(backend_url: String) -> BackendHealthStatus {
    let url = format!("{}/management/health", backend_url.trim_end_matches('/'));

    match reqwest::get(&url).await {
        Ok(response) => {
            if response.status().is_success() {
                BackendHealthStatus {
                    available: true,
                    message: "Backend is available".to_string(),
                }
            } else {
                BackendHealthStatus {
                    available: false,
                    message: format!("Backend returned status: {}", response.status()),
                }
            }
        }
        Err(e) => {
            BackendHealthStatus {
                available: false,
                message: format!("Backend not reachable: {}", e),
            }
        }
    }
}

/// Get the backend URL from config file, environment, or use default
fn get_backend_url() -> String {
    // Priority 1: Config file (backend-url.txt next to executable)
    if let Ok(exe_dir) = std::env::current_exe() {
        if let Some(parent) = exe_dir.parent() {
            let config_path = parent.join("backend-url.txt");
            if let Ok(url) = std::fs::read_to_string(&config_path) {
                let trimmed = url.trim();
                if !trimmed.is_empty() {
                    println!("Using backend URL from config file: {}", trimmed);
                    return trimmed.to_string();
                }
            }
        }
    }

    // Priority 2: Environment variable
    if let Ok(url) = std::env::var("BACKEND_URL") {
        println!("Using backend URL from environment: {}", url);
        return url;
    }

    // Priority 3: Default
    println!("Using default backend URL: http://localhost:8080");
    "http://localhost:8080".to_string()
}

/// Tauri command to get the configured backend URL
#[tauri::command]
fn get_backend_url_command() -> String {
    get_backend_url()
}

#[cfg(feature = "bundled-backend")]
#[tauri::command]
fn get_backend_status(state: tauri::State<BackendState>) -> backend_manager::BackendStatus {
    state.get_status()
}

fn main() {
    #[cfg(feature = "bundled-backend")]
    const BACKEND_PORT: u16 = 8080;

    let mut builder = tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_http::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_fs::init())
        .invoke_handler(tauri::generate_handler![
            printer::get_printers,
            printer::print_image,
            printer::print_escpos,
            check_backend_health,
            get_backend_url_command,
            #[cfg(feature = "bundled-backend")]
            get_backend_status
        ]);

    // Add backend management only when bundled-backend feature is enabled
    #[cfg(feature = "bundled-backend")]
    {
        builder = builder
            .setup(|app| {
                println!("Starting PharmaSmart Standalone application...");
                println!("Backend bundling is ENABLED");

                // Initialize backend state
                let backend_state = BackendState::new(BACKEND_PORT);
                app.manage(backend_state);

                // Start the Spring Boot backend
                let app_handle = app.handle().clone();
                tauri::async_runtime::spawn(async move {
                    match start_backend(&app_handle, BACKEND_PORT).await {
                        Ok(pid) => {
                            println!("Backend started successfully with PID: {}", pid);
                            // Store the PID in state
                            if let Some(state) = app_handle.try_state::<BackendState>() {
                                *state.process_id.lock().unwrap() = Some(pid);
                            }
                        }
                        Err(e) => {
                            eprintln!("Failed to start backend: {}", e);
                            // Show error dialog to user
                            use tauri_plugin_dialog::{DialogExt, MessageDialogKind};
                            app_handle.dialog()
                                .message(format!("Failed to start backend server:\n\n{}\n\nPlease ensure Java is installed.", e))
                                .kind(MessageDialogKind::Error)
                                .title("Backend Error")
                                .blocking_show();
                        }
                    }
                });

                #[cfg(debug_assertions)]
                {
                    use tauri::Manager;
                    // Get the main window (created by tauri.conf.json)
                    if let Some(window) = app.get_webview_window("main") {
                        println!("Window found successfully!");
                        // Open devtools in debug mode to see JavaScript errors
                        window.open_devtools();
                        println!("DevTools opened");
                    }
                }

                Ok(())
            });
    }

    // Standard setup without bundled backend
    #[cfg(not(feature = "bundled-backend"))]
    {
        builder = builder.setup(|app| {
            println!("Starting PharmaSmart application...");
            println!("Backend bundling is DISABLED - connect to external backend");

            let backend_url = get_backend_url();
            println!("Backend URL: {}", backend_url);

            // Monitor backend availability in standard mode
            let app_handle = app.handle().clone();
            let backend_url_clone = backend_url.clone();
            tauri::async_runtime::spawn(async move {
                println!("Monitoring backend availability at {}...", backend_url_clone);

                // Wait for backend to be available (with timeout)
                let max_attempts = 60; // 30 seconds (60 * 500ms)
                let mut attempts = 0;

                loop {
                    let url = format!("{}/management/health", backend_url_clone.trim_end_matches('/'));
                    match reqwest::get(&url).await {
                        Ok(response) if response.status().is_success() => {
                            println!("Backend is available at {}", backend_url_clone);
                            // Emit ready event to frontend
                            let _ = app_handle.emit("backend-health-status", BackendHealthStatus {
                                available: true,
                                message: format!("Backend is available at {}", backend_url_clone),
                            });
                            break;
                        }
                        _ => {
                            attempts += 1;
                            if attempts >= max_attempts {
                                println!("Backend not available after {} attempts", max_attempts);
                                let _ = app_handle.emit("backend-health-status", BackendHealthStatus {
                                    available: false,
                                    message: format!("Backend not available at {}. Please start the backend server or set BACKEND_URL environment variable.", backend_url_clone),
                                });
                                break;
                            }
                            tokio::time::sleep(std::time::Duration::from_millis(500)).await;
                        }
                    }
                }
            });

            #[cfg(debug_assertions)]
            {
                use tauri::Manager;
                // Get the main window (created by tauri.conf.json)
                if let Some(window) = app.get_webview_window("main") {
                    println!("Window found successfully!");
                    // Open devtools in debug mode to see JavaScript errors
                    window.open_devtools();
                    println!("DevTools opened");
                }
            }

            Ok(())
        });
    }

    builder
        .run(tauri::generate_context!())
        .expect("error while running PharmaSmart application");
}
