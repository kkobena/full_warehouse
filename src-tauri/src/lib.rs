// Customer display module for ESC/POS communication
mod customer_display;

// Configuration module for loading app settings
mod config;

// Learn more about Tauri commands at https://tauri.app/develop/calling-rust/
#[tauri::command]
fn greet(name: &str) -> String {
    format!("Hello, {}! You've been greeted from Rust!", name)
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
            customer_display::test_customer_display_connection
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
