// Customer display module for ESC/POS communication
mod customer_display;
// Scanner module for USB CDC barcode reader
mod scanner;

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_http::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_fs::init())
        .setup(|app| {
            // Symétrique avec main.rs : moniteur USB Win32 pour la reconnexion
            // CDC transparente (cf. scanner::device_monitor).
            scanner::start_device_monitor(app.handle().clone());
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            customer_display::send_to_customer_display,
            customer_display::list_serial_ports,
            customer_display::test_customer_display_connection,
            scanner::list_serial_ports_detailed,
            scanner::start_scanner_listener,
            scanner::stop_scanner_listener,
            scanner::send_to_display,
            scanner::is_port_connected,
            scanner::check_ports_connection,
            scanner::get_system_info,
            scanner::detect_scanner_usb_mode,
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
