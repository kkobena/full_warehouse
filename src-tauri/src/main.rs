// Prevents additional console window on Windows in release
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod printer;

fn main() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_http::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_fs::init())
        .invoke_handler(tauri::generate_handler![
            printer::get_printers,
            printer::print_image
        ])
        .setup(|app| {
            #[cfg(debug_assertions)]
            {
                use tauri::Manager;

                println!("Starting PharmaSmart application...");

                // Get the main window (created by tauri.conf.json)
                if let Some(window) = app.get_webview_window("main") {
                    println!("Window found successfully!");

                    // Open devtools in debug mode to see JavaScript errors
                    window.open_devtools();
                    println!("DevTools opened");
                }
            }

            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running PharmaSmart application");
}
