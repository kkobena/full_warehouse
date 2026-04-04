// Prevents additional console window on Windows in release
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod printer;
mod types; // I2 : expose les types partagés (nécessaire pour backend_manager::BackendError)

#[cfg(feature = "bundled-backend")]
mod backend_manager;

#[cfg(feature = "bundled-backend")]
mod config;

#[cfg(feature = "bundled-backend")]
use backend_manager::{BackendState, start_backend};

#[cfg(feature = "bundled-backend")]
use tauri::Manager;

// Emitter est uniquement nécessaire en mode standard (app_handle.emit dans le monitor)
#[cfg(not(feature = "bundled-backend"))]
use tauri::Emitter;
use crate::types::BackendHealthStatus; // I2 : réutilise la struct de types.rs

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
fn get_backend_url() -> String {
    //  Config file (backend-url.txt next to executable)
    if let Ok(exe_dir) = std::env::current_exe() {
        if let Some(parent) = exe_dir.parent() {
            let config_path = parent.join("backend-url.txt");
            if let Ok(content) = std::fs::read_to_string(&config_path) {
                let url = content
                    .lines()
                    .map(|l| l.trim())
                    .find(|l| !l.is_empty() && !l.starts_with('#'));
                if let Some(url) = url {
                    tracing::info!("URL backend depuis backend-url.txt : {}", url);
                    return url.to_string();
                }
            }

            //config.json port
            #[cfg(feature = "bundled-backend")]
            {
                let json_config_path = parent.join("config.json");
                if let Ok(config_str) = std::fs::read_to_string(&json_config_path) {
                    if let Ok(config) = serde_json::from_str::<serde_json::Value>(&config_str) {
                        if let Some(port) = config
                            .get("server")
                            .and_then(|s| s.get("port"))
                            .and_then(|p| p.as_u64())
                        {
                            let url = format!("http://localhost:{}", port);
                            tracing::info!("URL backend depuis config.json : {}", url);
                            return url;
                        }
                    }
                }
            }
        }
    }

    //Environment variable
    if let Ok(url) = std::env::var("BACKEND_URL") {
        tracing::info!("URL backend depuis variable d'environnement : {}", url);
        return url;
    }

    //Default
    tracing::info!("URL backend par défaut : http://localhost:9080");
    "http://localhost:9080".to_string()
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
            #[cfg(feature = "bundled-backend")]
            get_backend_status,
            #[cfg(feature = "bundled-backend")]
            restart_backend_main,
            #[cfg(feature = "bundled-backend")]
            stop_backend_main
        ]);

    // ── Mode bundled-backend ──────────────────────────────────────────────────
    #[cfg(feature = "bundled-backend")]
    {
        builder = builder.setup(|app| {
            tracing::info!("Démarrage PharmaSmart (backend embarqué)");

            // I4 : Lit le port depuis la configuration au lieu de le coder en dur.
            let config = config::AppConfig::load(app.handle());
            let backend_state = BackendState::new(config.server.port);
            app.manage(backend_state);

            let app_handle = app.handle().clone();
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
