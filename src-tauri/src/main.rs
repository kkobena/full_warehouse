// Prevents additional console window on Windows in release
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod printer;
mod types;
mod customer_display;
mod scanner;

#[cfg(feature = "bundled-backend")]
mod backend_manager;

#[cfg(feature = "bundled-backend")]
mod config;

#[cfg(feature = "bundled-backend")]
use backend_manager::{start_backend, BackendState};

#[cfg(feature = "bundled-backend")]
use tauri::Manager;

use crate::types::BackendHealthStatus;
use tauri::Emitter;

// ─── I1 : Client HTTP partagé ────────────────────────────────────────────────

/// Client HTTP unique partagé via `tauri::State` pour éviter de recréer
/// un pool de connexions à chaque appel de commande.
struct SharedHttpClient(reqwest::Client);

// ─── Commandes Tauri ──────────────────────────────────────────────────────────

/// Vérifie la disponibilité du backend (mode bundled et standard).
/// I1 : utilise le client partagé plutôt que `reqwest::get()` jetable.
/// Retourne `Result` car Tauri l'exige pour les commandes async avec State.
#[tauri::command]
async fn check_backend_health(
    client: tauri::State<'_, SharedHttpClient>,
    backend_url: String,
) -> Result<BackendHealthStatus, String> {
    let url = format!("{}/management/health", backend_url.trim_end_matches('/'));

    match client.0.get(&url).send().await {
        Ok(response) => {
            if response.status().is_success() {
                Ok(BackendHealthStatus {
                    available: true,
                    message: "Backend is available".to_string(),
                })
            } else {
                Ok(BackendHealthStatus {
                    available: false,
                    message: format!("Backend returned status: {}", response.status()),
                })
            }
        }
        Err(e) => Ok(BackendHealthStatus {
            available: false,
            message: format!("Backend not reachable: {}", e),
        }),
    }
}

/// Retourne l'URL du backend configurée (fichier, env, défaut).
/// Ordre de priorité :
///   1. backend-url.txt (répertoire exe, $PROGRAMDATA\PharmaSmart, $APPDATA\PharmaSmart)
///   2. config.json → server.port   (mêmes répertoires)
///   3. Variable d'environnement BACKEND_URL
///   4. Défaut : http://localhost:9080
fn get_backend_url() -> String {
    let search_dirs = backend_url_search_dirs();

    // 1. backend-url.txt
    for dir in &search_dirs {
        let txt_path = dir.join("backend-url.txt");
        if let Ok(content) = std::fs::read_to_string(&txt_path) {
            if let Some(url) = content
                .lines()
                .map(|l| l.trim())
                .find(|l| !l.is_empty() && !l.starts_with('#'))
            {
                tracing::info!("URL backend depuis backend-url.txt ({:?}) : {}", txt_path, url);
                return url.to_string();
            }
        }
    }

    // 2. config.json → server.port (tous modes, sans feature flag)
    for dir in &search_dirs {
        let json_path = dir.join("config.json");
        if let Ok(config_str) = std::fs::read_to_string(&json_path) {
            if let Ok(config) = serde_json::from_str::<serde_json::Value>(&config_str) {
                if let Some(port) = config
                    .get("server")
                    .and_then(|s| s.get("port"))
                    .and_then(|p| p.as_u64())
                {
                    let url = format!("http://localhost:{}", port);
                    tracing::info!("URL backend depuis config.json ({:?}) : {}", json_path, url);
                    return url;
                }
            }
        }
    }

    // 3. Variable d'environnement
    if let Ok(url) = std::env::var("BACKEND_URL") {
        tracing::info!("URL backend depuis variable d'environnement : {}", url);
        return url;
    }

    // 4. Défaut
    tracing::info!("URL backend par défaut : http://localhost:9080");
    "http://localhost:9080".to_string()
}

/// Répertoires candidats pour backend-url.txt et config.json, par ordre de priorité :
///   1. Répertoire de l'exe (là où l'installateur copie config.json)
///   2. %PROGRAMDATA%\PharmaSmart  (install tous utilisateurs)
///   3. %APPDATA%\PharmaSmart      (install par utilisateur sans élévation)
fn backend_url_search_dirs() -> Vec<std::path::PathBuf> {
    let mut dirs = Vec::new();

    if let Ok(exe) = std::env::current_exe() {
        if let Some(parent) = exe.parent() {
            dirs.push(parent.to_path_buf());
        }
    }

    #[cfg(windows)]
    if let Ok(program_data) = std::env::var("PROGRAMDATA") {
        dirs.push(std::path::PathBuf::from(program_data).join("PharmaSmart"));
    }

    #[cfg(windows)]
    if let Ok(app_data) = std::env::var("APPDATA") {
        dirs.push(std::path::PathBuf::from(app_data).join("PharmaSmart"));
    }

    dirs
}

#[tauri::command]
fn get_backend_url_command() -> String {
    get_backend_url()
}

#[cfg(feature = "bundled-backend")]
#[tauri::command]
fn get_backend_status(state: tauri::State<BackendState>) -> backend_manager::BackendStatus {
    // Note : get_status est async, mais ici on appelle depuis un contexte sync.
    // On utilise tauri::async_runtime::block_on pour le cas d'appel synchrone.
    tauri::async_runtime::block_on(state.get_status())
}

#[cfg(feature = "bundled-backend")]
#[tauri::command]
async fn restart_backend_main(app: tauri::AppHandle) -> Result<String, String> {
    match backend_manager::restart_backend(app).await {
        Ok(pid) => Ok(format!("Backend redémarré avec PID : {}", pid)),
        Err(e) => Err(format!("Échec du redémarrage : {}", e)),
    }
}

#[cfg(feature = "bundled-backend")]
#[tauri::command]
async fn stop_backend_main(app: tauri::AppHandle) -> Result<String, String> {
    match backend_manager::stop_backend(&app).await {
        Ok(_) => Ok("Backend arrêté".to_string()),
        Err(e) => Err(format!("Échec de l'arrêt : {}", e)),
    }
}

/// Persist a new server port to config.json so the next backend launch (or restart)
/// uses the value the user entered in the configuration dialog.
#[cfg(feature = "bundled-backend")]
#[tauri::command]
fn save_server_port(app: tauri::AppHandle, port: u16) -> Result<(), String> {
    let mut cfg = config::AppConfig::load(&app);
    cfg.server.port = port;
    cfg.save(&app)
}

/// Valeurs par défaut renvoyées au wizard de configuration initial.
#[cfg(feature = "bundled-backend")]
#[derive(serde::Serialize)]
struct SetupDefaults {
    db_host: String,
    db_port: u16,
    db_name: String,
    db_username: String,
    db_schema: String,
    server_port: u16,
}

/// Parse une URL JDBC de la forme `jdbc:postgresql://host:port/dbname`.
#[cfg(feature = "bundled-backend")]
fn parse_jdbc_url(url: &str) -> Option<(String, u16, String)> {
    let rest = url.strip_prefix("jdbc:postgresql://")?;
    let (host_port, db_name) = rest.split_once('/')?;
    let (host, port_str) = host_port.rsplit_once(':')?;
    let port: u16 = port_str.parse().ok()?;
    Some((host.to_string(), port, db_name.to_string()))
}

/// Retourne les valeurs actuelles de config.json pour pré-remplir le wizard.
/// Quand setup_complete = false, ces valeurs sont les défauts de l'installeur.
#[cfg(feature = "bundled-backend")]
#[tauri::command]
fn get_setup_defaults(app: tauri::AppHandle) -> SetupDefaults {
    let cfg = config::AppConfig::load(&app);

    let (db_host, db_port, db_name) = cfg
        .database
        .url
        .as_deref()
        .and_then(parse_jdbc_url)
        .unwrap_or_else(|| (cfg.database.host.clone(), cfg.database.port, "pharma_smart".to_string()));

    SetupDefaults {
        db_host,
        db_port,
        db_name,
        db_username: cfg.database.username.clone().unwrap_or_else(|| "pharma_smart".to_string()),
        db_schema: cfg.database.schema.clone().unwrap_or_default(),
        server_port: cfg.server.port,
    }
}

/// Retourne true si la configuration initiale n'a pas encore été validée.
/// Utilisé par Angular au démarrage pour détecter un événement `setup-required` manqué.
#[cfg(feature = "bundled-backend")]
#[tauri::command]
fn check_needs_setup(app: tauri::AppHandle) -> bool {
    !config::AppConfig::load(&app).is_setup_complete()
}

/// Appelée par le wizard de configuration initial (Angular) après que l'utilisateur
/// a validé les paramètres DB et le port serveur.
/// Sauvegarde la config (setup_complete = true) puis démarre le backend.
#[cfg(feature = "bundled-backend")]
#[tauri::command]
async fn complete_initial_setup(
    app: tauri::AppHandle,
    db_host: String,
    db_port: u16,
    db_name: String,
    db_username: String,
    db_password: String,
    db_schema: String,
    server_port: u16,
) -> Result<(), String> {
    let jdbc_url = format!("jdbc:postgresql://{}:{}/{}", db_host, db_port, db_name);

    let mut cfg = config::AppConfig::load(&app);
    cfg.server.port = server_port;
    cfg.database.host = db_host;
    cfg.database.port = db_port;
    cfg.database.url = Some(jdbc_url);
    cfg.database.username = if db_username.is_empty() { None } else { Some(db_username) };
    cfg.database.password = if db_password.is_empty() { None } else { Some(db_password) };
    cfg.database.schema = if db_schema.is_empty() { None } else { Some(db_schema) };
    cfg.setup_complete = true;
    cfg.save(&app)?;

    match start_backend(&app).await {
        Ok(pid) => {
            tracing::info!(pid, "Backend démarré après setup initial");
            Ok(())
        }
        Err(e) => Err(format!("Échec du démarrage du backend : {}", e)),
    }
}

/// DTO complet exposé à l'éditeur de configuration Angular (setup_complete = true).
/// Contient uniquement les champs modifiables par l'utilisateur — les chemins
/// générés par l'installeur (logging, file.*) ne sont pas exposés.
#[cfg(feature = "bundled-backend")]
#[derive(serde::Serialize, serde::Deserialize)]
struct AppConfigDto {
    // Serveur
    server_port: u16,
    // Base de données
    db_host: String,
    db_port: u16,
    db_name: String,
    db_username: String,
    db_password: String,
    db_schema: String,
    // JVM
    jvm_heap_min: String,
    jvm_heap_max: String,
    jvm_metaspace_size: String,
    jvm_metaspace_max: String,
    jvm_direct_memory: String,
    jvm_gc_pause: String,
    jvm_additional_options: Vec<String>,
    // Mail
    mail_username: String,
    mail_email: String,
    // FNE
    fne_url: String,
    fne_api_key: String,
    fne_point_of_sale: String,
    // Divers
    port_com: String,
}

/// Retourne le DTO de configuration pour l'éditeur Angular.
#[cfg(feature = "bundled-backend")]
#[tauri::command]
fn get_app_config_dto(app: tauri::AppHandle) -> AppConfigDto {
    let cfg = config::AppConfig::load(&app);

    let (db_host, db_port, db_name) = cfg
        .database
        .url
        .as_deref()
        .and_then(parse_jdbc_url)
        .unwrap_or_else(|| (cfg.database.host.clone(), cfg.database.port, String::new()));

    AppConfigDto {
        server_port: cfg.server.port,
        db_host,
        db_port,
        db_name,
        db_username:  cfg.database.username.clone().unwrap_or_default(),
        db_password:  cfg.database.password.clone().unwrap_or_default(),
        db_schema:    cfg.database.schema.clone().unwrap_or_default(),
        jvm_heap_min:         cfg.jvm.heap_min.clone(),
        jvm_heap_max:         cfg.jvm.heap_max.clone(),
        jvm_metaspace_size:   cfg.jvm.metaspace_size.clone(),
        jvm_metaspace_max:    cfg.jvm.metaspace_max.clone(),
        jvm_direct_memory:    cfg.jvm.direct_memory_size.clone(),
        jvm_gc_pause:         cfg.jvm.max_gc_pause_millis.clone(),
        jvm_additional_options: cfg.jvm.additional_options.clone(),
        mail_username:    cfg.mail.username.clone(),
        mail_email:       cfg.mail.email.clone(),
        fne_url:          cfg.fne.url.clone(),
        fne_api_key:      cfg.fne.api_key.clone(),
        fne_point_of_sale: cfg.fne.point_of_sale.clone(),
        port_com: cfg.port_com.clone(),
    }
}

/// Sauvegarde le DTO dans config.json.
/// Ne redémarre PAS le backend — laisser Angular décider (bouton "Enregistrer et Redémarrer").
#[cfg(feature = "bundled-backend")]
#[tauri::command]
fn save_app_config_dto(app: tauri::AppHandle, dto: AppConfigDto) -> Result<(), String> {
    let jdbc_url = format!("jdbc:postgresql://{}:{}/{}", dto.db_host, dto.db_port, dto.db_name);

    let mut cfg = config::AppConfig::load(&app);
    cfg.server.port     = dto.server_port;
    cfg.database.host   = dto.db_host;
    cfg.database.port   = dto.db_port;
    cfg.database.url    = Some(jdbc_url);
    cfg.database.username = if dto.db_username.is_empty() { None } else { Some(dto.db_username) };
    cfg.database.password = if dto.db_password.is_empty() { None } else { Some(dto.db_password) };
    cfg.database.schema   = if dto.db_schema.is_empty()   { None } else { Some(dto.db_schema)   };
    cfg.jvm.heap_min             = dto.jvm_heap_min;
    cfg.jvm.heap_max             = dto.jvm_heap_max;
    cfg.jvm.metaspace_size       = dto.jvm_metaspace_size;
    cfg.jvm.metaspace_max        = dto.jvm_metaspace_max;
    cfg.jvm.direct_memory_size   = dto.jvm_direct_memory;
    cfg.jvm.max_gc_pause_millis  = dto.jvm_gc_pause;
    cfg.jvm.additional_options   = dto.jvm_additional_options;
    cfg.mail.username        = dto.mail_username;
    cfg.mail.email           = dto.mail_email;
    cfg.fne.url              = dto.fne_url;
    cfg.fne.api_key          = dto.fne_api_key;
    cfg.fne.point_of_sale    = dto.fne_point_of_sale;
    cfg.port_com             = dto.port_com;
    cfg.save(&app)
}

// ─── main ─────────────────────────────────────────────────────────────────────

fn main() {
    //Initialise le logging structuré avec tracing_subscriber.
    // RUST_LOG permet de surcharger le niveau depuis l'environnement.
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| tracing_subscriber::EnvFilter::new("warn,pharmasmart=info")),
        )
        .init();

    //Crée le client HTTP partagé une seule fois.
    let http_client = reqwest::Client::builder()
        .timeout(std::time::Duration::from_secs(10))
        .build()
        .unwrap_or_default();

    let mut builder = tauri::Builder::default()
        .manage(SharedHttpClient(http_client)) // I1 : partage le client
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
            #[cfg(feature = "bundled-backend")]
            get_backend_status,
            #[cfg(feature = "bundled-backend")]
            restart_backend_main,
            #[cfg(feature = "bundled-backend")]
            stop_backend_main,
            #[cfg(feature = "bundled-backend")]
            save_server_port,
            #[cfg(feature = "bundled-backend")]
            get_setup_defaults,
            #[cfg(feature = "bundled-backend")]
            check_needs_setup,
            #[cfg(feature = "bundled-backend")]
            complete_initial_setup,
            #[cfg(feature = "bundled-backend")]
            get_app_config_dto,
            #[cfg(feature = "bundled-backend")]
            save_app_config_dto
        ]);

    // ── Mode bundled-backend ──────────────────────────────────────────────────
    #[cfg(feature = "bundled-backend")]
    {
        builder = builder.setup(|app| {
            tracing::info!("Démarrage PharmaSmart (backend embarqué)");

            // Moniteur USB Win32 : émet `scan-usb-arrived` à chaque DBT_DEVICEARRIVAL
            // sur un port COM. Déclencheur primaire de la reconnexion CDC après
            // débranchement/rebranchement (cf. scanner::device_monitor).
            scanner::start_device_monitor(app.handle().clone());

            // I4 : Lit le port depuis la configuration au lieu de le coder en dur.
            let config = config::AppConfig::load(app.handle());
            let backend_state = BackendState::new(config.server.port);
            app.manage(backend_state);

            let app_handle = app.handle().clone();

            if config.is_setup_complete() {
                // Configuration validée → démarrage immédiat du backend.
                tauri::async_runtime::spawn(async move {
                    match start_backend(&app_handle).await {
                        Ok(pid) => {
                            tracing::info!(pid, "Backend démarré avec succès");
                        }
                        Err(e) => {
                            tracing::error!(error = %e, "Échec du démarrage du backend");
                            use tauri_plugin_dialog::{DialogExt, MessageDialogKind};
                            app_handle
                                .dialog()
                                .message(format!(
                                    "Échec du démarrage du serveur backend :\n\n{}\n\nVérifiez que Java est installé.",
                                    e
                                ))
                                .kind(MessageDialogKind::Error)
                                .title("Erreur Backend")
                                .blocking_show();
                        }
                    }
                });
            } else {
                // Premier lancement ou configuration incomplète → demander à
                // Angular d'afficher le wizard de configuration initiale.
                // Le backend ne démarrera qu'après appel de `complete_initial_setup`.
                tracing::info!("Configuration initiale requise — wizard de setup affiché");
                let _ = app_handle.emit("setup-required", ());
            }

            #[cfg(debug_assertions)]
            {
                use tauri::Manager;
                if let Some(window) = app.get_webview_window("main") {
                    tracing::debug!("DevTools ouverts");
                    window.open_devtools();
                }
            }

            Ok(())
        });
    }

    // ── Mode standard (backend externe) ──────────────────────────────────────
    #[cfg(not(feature = "bundled-backend"))]
    {
        builder = builder.setup(|app| {
            tracing::info!("Démarrage PharmaSmart (backend externe)");
            use tauri::Manager; // nécessaire pour app.state()

            // Moniteur USB Win32 : émet `scan-usb-arrived` à chaque DBT_DEVICEARRIVAL
            // sur un port COM. Déclencheur primaire de la reconnexion CDC après
            // débranchement/rebranchement (cf. scanner::device_monitor).
            scanner::start_device_monitor(app.handle().clone());

            let backend_url = get_backend_url();
            tracing::info!("URL backend : {}", backend_url);

            // I1 : Clone le client partagé pour la tâche de monitoring.
            let health_client = app.state::<SharedHttpClient>().0.clone();
            let app_handle = app.handle().clone();
            let backend_url_clone = backend_url.clone();

            tauri::async_runtime::spawn(async move {
                tracing::info!("Surveillance du backend sur {}…", backend_url_clone);

                let max_attempts = 60u32;
                let mut attempts = 0u32;
                let mut delay = std::time::Duration::from_millis(500);
                let max_delay = std::time::Duration::from_secs(5);

                loop {
                    let url = format!(
                        "{}/management/health",
                        backend_url_clone.trim_end_matches('/')
                    );
                    // Aide au type pour l'inférence du compilateur
                    let resp = health_client.get(&url).send().await;
                    match resp {
                        Ok(response) if response.status().is_success() => {
                            tracing::info!("Backend disponible sur {}", backend_url_clone);
                            let _ = app_handle.emit(
                                "backend-health-status",
                                BackendHealthStatus {
                                    available: true,
                                    message: format!(
                                        "Backend disponible sur {}",
                                        backend_url_clone
                                    ),
                                },
                            );
                            break;
                        }
                        _ => {
                            attempts += 1;
                            if attempts >= max_attempts {
                                tracing::warn!(
                                    "Backend indisponible après {} tentatives",
                                    max_attempts
                                );
                                let _ = app_handle.emit(
                                    "backend-health-status",
                                    BackendHealthStatus {
                                        available: false,
                                        message: format!(
                                            "Backend indisponible sur {}. Démarrez le serveur ou définissez BACKEND_URL.",
                                            backend_url_clone
                                        ),
                                    },
                                );
                                break;
                            }
                            tokio::time::sleep(delay).await;
                            delay = (delay * 2).min(max_delay);
                        }
                    }
                }
            });

            #[cfg(debug_assertions)]
            {
                use tauri::Manager;
                if let Some(window) = app.get_webview_window("main") {
                    tracing::debug!("DevTools ouverts");
                    window.open_devtools();
                }
            }

            Ok(())
        });
    }

    builder
        .run(tauri::generate_context!())
        .expect("Erreur lors de l'exécution de PharmaSmart");
}
