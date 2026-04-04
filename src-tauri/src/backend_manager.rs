use crate::config::AppConfig;
use crate::types::BackendError;
use serde::Serialize;
use std::path::{Path, PathBuf};
use std::sync::Arc;
use std::time::Duration;
use tauri::{AppHandle, Emitter, Manager};
use tauri_plugin_shell::ShellExt;
use tokio::net::TcpStream;
use tokio::sync::Mutex; // C3 : tokio::sync::Mutex

#[derive(Clone, Serialize)]
pub struct BackendStatus {
    pub status: String,
    pub progress: u8,
    pub message: String,
}

// ─── State ────────────────────────────────────────────────────────────────────

pub struct BackendState {
    pub process_id: Mutex<Option<u32>>,
    pub port: u16,
    status: Arc<Mutex<BackendStatus>>,
}

impl BackendState {
    pub fn new(port: u16) -> Self {
        Self {
            process_id: Mutex::new(None),
            port,
            status: Arc::new(Mutex::new(BackendStatus {
                status: "initializing".to_string(),
                progress: 0,
                message: "Initialisation du backend…".to_string(),
            })),
        }
    }

    pub async fn get_status(&self) -> BackendStatus {
        self.status.lock().await.clone()
    }

    /// Met à jour le statut et émet l'événement vers le frontend.
    /// Le verrou est relâché avant l'émission pour éviter toute contention.
    pub async fn set_status(&self, app: &AppHandle, status: &str, progress: u8, message: &str) {
        let snapshot = {
            let mut s = self.status.lock().await;
            s.status = status.to_string();
            s.progress = progress;
            s.message = message.to_string();
            s.clone()
        };
        let _ = app.emit("backend-status", snapshot);
    }
}

// ─── JRE / JAR discovery ─────────────────────────────────────────────────────

fn find_bundled_jre(app: &AppHandle) -> Option<PathBuf> {
    let resource_dir = app.path().resource_dir().ok()?;
    let jre_dir = resource_dir.join("sidecar").join("jre");
    if !jre_dir.exists() {
        tracing::debug!("JRE embarqué absent : {:?}", jre_dir);
        return None;
    }
    #[cfg(target_os = "windows")]
    let java_exe = jre_dir.join("bin").join("java.exe");
    #[cfg(not(target_os = "windows"))]
    let java_exe = jre_dir.join("bin").join("java");

    if java_exe.exists() {
        Some(java_exe)
    } else {
        tracing::warn!("Répertoire JRE présent mais java introuvable : {:?}", java_exe);
        None
    }
}

/// Vérifie que Java système est disponible. I7 : tokio::process::Command async.
async fn check_java_version() -> Result<(), BackendError> {
    use tokio::process::Command;
    let output = Command::new("java")
        .arg("-version")
        .output()
        .await
        .map_err(|_| BackendError::JavaNotFound("Java introuvable — installez un JRE.".to_string()))?;

    let ver = String::from_utf8_lossy(&output.stderr);
    if ver.is_empty() {
        return Err(BackendError::JavaNotFound("Java ne retourne aucune sortie.".to_string()));
    }
    if let Some(first) = ver.lines().next() {
        tracing::info!("Java détecté : {}", first);
    }
    Ok(())
}

fn find_jar_file(app: &AppHandle) -> Result<PathBuf, BackendError> {
    let resource_dir = app.path().resource_dir()
        .map_err(|e| BackendError::ConfigError(format!("Répertoire ressources : {}", e)))?;
    let sidecar_dir = resource_dir.join("sidecar");
    if !sidecar_dir.exists() {
        return Err(BackendError::ConfigError(format!("Répertoire sidecar absent : {:?}", sidecar_dir)));
    }
    let mut jars: Vec<PathBuf> = std::fs::read_dir(&sidecar_dir)
        .map_err(|e| BackendError::ConfigError(format!("Lecture sidecar : {}", e)))?
        .filter_map(|e| e.ok())
        .map(|e| e.path())
        .filter(|p| {
            p.file_name().and_then(|n| n.to_str())
                .map(|n| n.starts_with("pharmaSmart-") && n.ends_with(".jar"))
                .unwrap_or(false)
        })
        .collect();

    jars.sort();
    jars.into_iter().next_back()
        .inspect(|p| tracing::info!("JAR sélectionné : {:?}", p))
        .ok_or_else(|| BackendError::JarNotFound(sidecar_dir))
}

// ─── JVM args builder (I3) ───────────────────────────────────────────────────

/// Construit la liste complète des arguments JVM + Spring Boot.
/// Retourne un `Vec<String>` testable unitairement.
fn build_jvm_args(config: &AppConfig, jar_path: &Path) -> Vec<String> {
    let log_dir = config.get_log_dir();
    let gc_log = log_dir.join("gc.log");
    let heap_dump = log_dir.join("heapdump.hprof");

    let mut args: Vec<String> = vec![
        format!("-Xms{}", config.jvm.heap_min),
        format!("-Xmx{}", config.jvm.heap_max),
        format!("-XX:MetaspaceSize={}", config.jvm.metaspace_size),
        format!("-XX:MaxMetaspaceSize={}", config.jvm.metaspace_max),
        format!("-XX:MaxDirectMemorySize={}", config.jvm.direct_memory_size),
        "-XX:+UseG1GC".to_string(),
        format!("-XX:MaxGCPauseMillis={}", config.jvm.max_gc_pause_millis),
        "-XX:+UseStringDeduplication".to_string(),
        "-XX:+UseCompressedOops".to_string(),
        "-XX:+HeapDumpOnOutOfMemoryError".to_string(),
        format!("-XX:HeapDumpPath={}", heap_dump.to_str().unwrap_or("heapdump.hprof")),
        format!("-Xlog:gc*:file={}:time,level,tags", gc_log.to_str().unwrap_or("gc.log")),
        "-Dfile.encoding=UTF-8".to_string(),
        "-Duser.timezone=UTC".to_string(),
        "-Djava.net.preferIPv4Stack=true".to_string(),
    ];

    args.extend(config.jvm.additional_options.iter().cloned());
    if !config.jvm.additional_options.is_empty() {
        tracing::debug!("Options JVM additionnelles : {:?}", config.jvm.additional_options);
    }

    args.push("-jar".to_string());
    args.push(jar_path.to_string_lossy().to_string());
    args.push("--spring.profiles.active=standalone,tauri,prod".to_string());
    args.push(format!("--server.port={}", config.server.port));
    args.push(format!("--logging.file.name={}", config.get_log_path().to_str().unwrap_or("pharmasmart.log")));
    args.push(format!("--file.report={}", config.file.report));
    args.push(format!("--file.images={}", config.file.images));
    args.push(format!("--file.import.json={}", config.file.import.json));
    args.push(format!("--file.import.csv={}", config.file.import.csv));
    args.push(format!("--file.import.excel={}", config.file.import.excel));
    args.push(format!("--file.pharmaml={}", config.file.pharmaml));
    args.push(format!("--fne.url={}", config.fne.url));
    args.push(format!("--fne.api-key={}", config.fne.api_key));
    args.push(format!("--fne.point-of-sale={}", config.fne.point_of_sale));
    args.push(format!("--spring.mail.username={}", config.mail.username));
    args.push(format!("--mail.email={}", config.mail.email));
    args.push(format!("--port-com={}", config.port_com));
    args.push(format!("--pharma-smart.jobs.nightly-pipeline-cron={}", config.jobs.nightly_pipeline_cron));
    args.push(format!("--pharma-smart.semois.freeze-delay-days={}", config.semois.freeze_delay_days));
    args.push(format!("--pharma-smart.semois.batch-size={}", config.semois.batch_size));
    args.push(format!("--pharma-smart.views.dashboards-cron={}", config.views.dashboards_cron));
    args.push(format!("--pharma-smart.views.analytique-cron={}", config.views.analytique_cron));
    args.push(format!("--pharma-smart.views.reporting-cron={}", config.views.reporting_cron));
    args
}

// ─── PostgreSQL readiness check ──────────────────────────────────────────────

/// Vérifie la disponibilité de PostgreSQL via une connexion TCP au `host:port` configuré.
/// Effectue des tentatives avec backoff exponentiel jusqu'au délai `check_timeout_secs`.
///
/// Cette vérification légère (pas de driver pg) suffit à confirmer que le service écoute
/// avant de lancer Spring Boot : si le port est fermé, la JVM échouera de toute façon.
async fn check_postgres_ready(app: &AppHandle, config: &AppConfig) -> Result<(), BackendError> {
    let addr = format!("{}:{}", config.database.host, config.database.port);
    let timeout_secs = config.database.check_timeout_secs;
    let state = app.state::<BackendState>();

    tracing::info!("Vérification disponibilité PostgreSQL sur {} (timeout: {}s)…", addr, timeout_secs);

    let start = std::time::Instant::now();
    let total_timeout = Duration::from_secs(timeout_secs);
    let mut delay = Duration::from_millis(500);
    let max_delay = Duration::from_secs(5);
    let mut attempt = 0u32;

    loop {
        attempt += 1;

        if start.elapsed() >= total_timeout {
            let msg = format!(
                "Timeout ({}s atteint) — vérifiez que PostgreSQL est démarré sur {}",
                timeout_secs, addr
            );
            tracing::error!("PostgreSQL non disponible après {} tentatives : {}", attempt, msg);
            return Err(BackendError::DatabaseNotReady(addr, msg));
        }

        let elapsed = start.elapsed().as_secs();
        let progress = 15u8.saturating_add(
            ((elapsed as f32 / timeout_secs as f32) * 10.0) as u8
        ).min(24);

        state.set_status(
            app,
            "checking_database",
            progress,
            &format!(
                "En attente de PostgreSQL sur {} ({}/{}s, tentative {})…",
                addr, elapsed, timeout_secs, attempt
            ),
        ).await;

        // Connexion TCP avec délai de 3s maximum par tentative
        match tokio::time::timeout(
            Duration::from_secs(3),
            TcpStream::connect(&addr),
        ).await {
            Ok(Ok(_)) => {
                tracing::info!(
                    "PostgreSQL disponible sur {} (tentative {}, délai: {}ms)",
                    addr, attempt, start.elapsed().as_millis()
                );
                state.set_status(
                    app,
                    "database_ready",
                    25,
                    &format!("PostgreSQL prêt sur {}", addr),
                ).await;
                return Ok(());
            }
            Ok(Err(e)) => {
                tracing::debug!("PostgreSQL inaccessible (tentative {}): {}", attempt, e);
            }
            Err(_) => {
                tracing::debug!("Connexion PostgreSQL timeout (tentative {})", attempt);
            }
        }

        tokio::time::sleep(delay).await;
        delay = (delay * 2).min(max_delay);
    }
}

// ─── Graceful shutdown ────────────────────────────────────────────────────────

async fn try_graceful_shutdown(port: u16, client: &reqwest::Client) {
    let url = format!("http://localhost:{}/management/shutdown", port);
    match client.post(&url).send().await {
        Ok(resp) if resp.status().is_success() => {
            tracing::info!(port, "Arrêt gracieux envoyé, attente 5s…");
            tokio::time::sleep(Duration::from_secs(5)).await;
        }
        Ok(resp) => tracing::warn!(port, status = %resp.status(), "Arrêt gracieux refusé"),
        Err(e) => tracing::debug!(error = %e, "Endpoint shutdown inaccessible"),
    }
}

// ─── start_backend ───────────────────────────────────────────────────────────

pub async fn start_backend(app: &AppHandle) -> Result<u32, String> {
    inner_start(app).await.map_err(|e| e.to_string())
}

async fn inner_start(app: &AppHandle) -> Result<u32, BackendError> {
    let config = AppConfig::load(app);
    let state = app.state::<BackendState>();

    state.set_status(app, "checking_java", 10, "Vérification de Java…").await;

    let java_executable = if let Some(bundled) = find_bundled_jre(app) {
        state.set_status(app, "checking_java", 15, "Utilisation du JRE embarqué…").await;
        bundled.to_string_lossy().to_string()
    } else {
        state.set_status(app, "checking_java", 12, "Vérification du Java système…").await;
        check_java_version().await?;
        "java".to_string()
    };

    // ── Vérifier que PostgreSQL est accessible avant de lancer la JVM ──────
    state.set_status(app, "checking_database", 15, "Vérification de PostgreSQL…").await;
    check_postgres_ready(app, &config).await?;

    state.set_status(app, "finding_jar", 30, "Recherche du fichier JAR…").await;
    let jar_path = find_jar_file(app)?;

    state.set_status(app, "starting", 30, "Démarrage du backend Spring Boot…").await;

    if let Err(e) = config.ensure_log_dir() {
        tracing::warn!(error = %e, "Impossible de créer le répertoire de logs");
    }

    let args = build_jvm_args(&config, &jar_path);
    tracing::info!("Lancement Java avec {} arguments", args.len());

    let (mut rx, child) = app
        .shell()
        .command(&java_executable)
        .args(args)
        .spawn()
        .map_err(|e| BackendError::SpawnFailed(e.to_string()))?;

    let pid = child.pid();
    *state.process_id.lock().await = Some(pid);
    state.set_status(app, "launched", 40, &format!("Backend lancé (PID : {})…", pid)).await;
    tracing::info!(pid, "Processus backend démarré");

    // Surveillance de la sortie du processus
    let app_handle = app.clone();
    tauri::async_runtime::spawn(async move {
        while let Some(event) = rx.recv().await {
            match event {
                tauri_plugin_shell::process::CommandEvent::Stdout(line) => {
                    tracing::debug!("[Backend stdout] {}", String::from_utf8_lossy(&line));
                }
                tauri_plugin_shell::process::CommandEvent::Stderr(line) => {
                    let text = String::from_utf8_lossy(&line);
                    if text.contains("ERROR") || text.contains("FATAL") || text.contains("Exception") {
                        tracing::error!("[Backend] {}", text);
                        let _ = app_handle.emit("backend-log", text.to_string());
                    } else {
                        tracing::debug!("[Backend stderr] {}", text);
                    }
                }
                tauri_plugin_shell::process::CommandEvent::Terminated(status) => {
                    tracing::warn!("[Backend] Processus terminé : {:?}", status);
                    let _ = app_handle.emit("backend-log", format!("Backend terminé : {:?}", status));
                    break;
                }
                _ => {}
            }
        }
    });

    state.set_status(app, "waiting", 50, "En attente de la disponibilité du backend…").await;

    let client = reqwest::Client::builder()
        .timeout(Duration::from_secs(10))
        .build()
        .unwrap_or_default();

    if let Err(e) = wait_for_backend_ready(app, config.server.port, 60, &client).await {
        state.set_status(app, "error", 0, &format!("Échec démarrage backend : {}", e)).await;
        return Err(BackendError::SpawnFailed(e));
    }

    state.set_status(app, "ready", 100, "Le backend est prêt !").await;
    tracing::info!(pid, "Backend opérationnel");
    Ok(pid)
}

// ─── wait_for_backend_ready ───────────────────────────────────────────────────

async fn wait_for_backend_ready(
    app: &AppHandle,
    port: u16,
    timeout_secs: u64,
    client: &reqwest::Client,
) -> Result<(), String> {
    let start = std::time::Instant::now();
    let timeout = Duration::from_secs(timeout_secs);
    let state = app.state::<BackendState>();

    // I5 : backoff exponentiel
    let mut delay = Duration::from_millis(500);
    let max_delay = Duration::from_secs(5);
    let url = format!("http://localhost:{}/management/health", port);

    loop {
        if start.elapsed() >= timeout {
            return Err(BackendError::StartupTimeout(timeout_secs).to_string());
        }

        let elapsed = start.elapsed().as_secs();
        let progress = 50u8.saturating_add(
            ((elapsed as f32 / timeout_secs as f32) * 45.0) as u8
        ).min(95);

        state.set_status(
            app,
            "waiting",
            progress,
            &format!("Vérification état backend… ({}/{}s)", elapsed, timeout_secs),
        ).await;

        match client.get(&url).send().await {
            Ok(resp) if resp.status().is_success() => return Ok(()),
            _ => {
                tokio::time::sleep(delay).await;
                delay = (delay * 2).min(max_delay);
            }
        }
    }
}

// ─── stop_backend ─────────────────────────────────────────────────────────────

pub async fn stop_backend(app: &AppHandle) -> Result<(), String> {
    let state = app.state::<BackendState>();
    let pid = state.process_id.lock().await.take();

    let Some(pid) = pid else {
        tracing::info!("Aucun processus backend actif");
        return Ok(());
    };

    tracing::info!(pid, "Arrêt du backend");
    state.set_status(app, "stopping", 0, "Arrêt du backend en cours…").await;

    let client = reqwest::Client::builder()
        .timeout(Duration::from_secs(8))
        .build()
        .unwrap_or_default();
    try_graceful_shutdown(state.port, &client).await;

    #[cfg(windows)]
    {
        use tokio::process::Command; // I7 : async
        match Command::new("taskkill").args(["/F", "/PID", &pid.to_string()]).output().await {
            Ok(r) if r.status.success() => {
                tracing::info!(pid, "Processus arrêté (taskkill)");
                state.set_status(app, "stopped", 100, "Backend arrêté").await;
                Ok(())
            }
            Ok(r) => Err(format!("Échec taskkill : {}", String::from_utf8_lossy(&r.stderr))),
            Err(e) => Err(format!("Impossible d'exécuter taskkill : {}", e)),
        }
    }

    // C1 fix : remplace nix (absent de Cargo.toml) par tokio::process::Command
    #[cfg(not(windows))]
    {
        use tokio::process::Command;
        match Command::new("kill").args(["-15", &pid.to_string()]).output().await {
            Ok(_) => {
                tracing::info!(pid, "SIGTERM envoyé au processus backend");
                state.set_status(app, "stopped", 100, "Backend arrêté").await;
                Ok(())
            }
            Err(e) => Err(format!("Impossible d'envoyer SIGTERM : {}", e)),
        }
    }
}

// ─── restart_backend ──────────────────────────────────────────────────────────

pub async fn restart_backend(app: AppHandle) -> Result<u32, String> {
    tracing::info!("Redémarrage du backend…");
    let state = app.state::<BackendState>();
    state.set_status(&app, "restarting", 10, "Redémarrage…").await;

    if let Err(e) = stop_backend(&app).await {
        tracing::warn!(error = %e, "Arrêt non propre, tentative de redémarrage quand même");
    }
    tokio::time::sleep(Duration::from_secs(2)).await;
    state.set_status(&app, "starting", 30, "Démarrage du backend…").await;
    start_backend(&app).await
}

// ─── Tests unitaires ──────────────────────────────────────────────────────────

#[cfg(test)]
mod tests {
    use super::*;
    use crate::config::AppConfig;

    #[test]
    fn test_build_jvm_args_required_flags() {
        let config = AppConfig::default();
        let jar = Path::new("/opt/sidecar/pharmaSmart-1.0.jar");
        let args = build_jvm_args(&config, jar);

        assert!(args.iter().any(|a| a.starts_with("-Xms")), "heap_min manquant");
        assert!(args.iter().any(|a| a.starts_with("-Xmx")), "heap_max manquant");
        assert!(args.iter().any(|a| a == "-XX:+UseG1GC"), "G1GC manquant");
        assert!(args.iter().any(|a| a == "-jar"), "-jar manquant");
        assert!(args.iter().any(|a| a.contains("pharmaSmart-1.0.jar")), "JAR path manquant");
        assert!(args.iter().any(|a| a.contains("standalone,tauri,prod")), "profiles manquants");
    }

    #[test]
    fn test_build_jvm_args_additional_options() {
        let mut config = AppConfig::default();
        config.jvm.additional_options = vec!["-Xss4m".to_string(), "-XX:+PrintGC".to_string()];
        let args = build_jvm_args(&config, Path::new("app.jar"));
        assert!(args.iter().any(|a| a == "-Xss4m"));
        assert!(args.iter().any(|a| a == "-XX:+PrintGC"));
    }
}
