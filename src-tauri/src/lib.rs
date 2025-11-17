// Customer display module for ESC/POS communication
mod customer_display;

// Configuration module for loading app settings
mod config;

// Backend manager module for Spring Boot process control
mod backend_manager;

// Learn more about Tauri commands at https://tauri.app/develop/calling-rust/
#[tauri::command]
fn greet(name: &str) -> String {
    format!("Hello, {}! You've been greeted from Rust!", name)
}

/// Restart the Spring Boot backend
#[tauri::command]
async fn restart_backend(app: tauri::AppHandle) -> Result<String, String> {
    match backend_manager::restart_backend(app).await {
        Ok(pid) => Ok(format!("Backend restarted successfully with PID: {}", pid)),
        Err(e) => Err(format!("Failed to restart backend: {}", e)),
    }
}

/// Stop the Spring Boot backend
#[tauri::command]
fn stop_backend(app: tauri::AppHandle) -> Result<String, String> {
    match backend_manager::stop_backend(&app) {
        Ok(_) => Ok("Backend stopped successfully".to_string()),
        Err(e) => Err(format!("Failed to stop backend: {}", e)),
    }
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_http::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_fs::init())
        .invoke_handler(tauri::generate_handler![
            greet,
            customer_display::send_to_customer_display,
            customer_display::list_serial_ports,
            customer_display::test_customer_display_connection,
            restart_backend,
            stop_backend
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
