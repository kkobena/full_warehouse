use crate::config::AppConfig;
use serde::Serialize;
use std::path::PathBuf;
use std::sync::{Arc, Mutex};
use std::time::Duration;
use tauri::{AppHandle, Emitter, Manager};
use tauri_plugin_shell::ShellExt;

#[derive(Clone, Serialize)]
pub struct BackendStatus {
    pub status: String,
    pub progress: u8,
    pub message: String,
}

// Global state to track the backend process
pub struct BackendState {
    pub process_id: Mutex<Option<u32>>,
    pub port: u16,
    pub status: Arc<Mutex<BackendStatus>>,
}

impl BackendState {
    pub fn new(port: u16) -> Self {
        Self {
            process_id: Mutex::new(None),
            port,
            status: Arc::new(Mutex::new(BackendStatus {
                status: "initializing".to_string(),
                progress: 0,
                message: "Initializing backend...".to_string(),
            })),
        }
    }

    pub fn get_status(&self) -> BackendStatus {
        self.status.lock().unwrap().clone()
    }

    pub fn update_status(&self, status: String, progress: u8, message: String) {
        let mut current_status = self.status.lock().unwrap();
        current_status.status = status;
        current_status.progress = progress;
        current_status.message = message;
    }

    /// Update status and emit the event to the frontend in a single call.
    pub fn set_status(&self, app: &AppHandle, status: &str, progress: u8, message: &str) {
        self.update_status(status.to_string(), progress, message.to_string());
        let _ = app.emit("backend-status", self.get_status());
    }
}

/// Find the bundled JRE in the resources directory
fn find_bundled_jre(app: &AppHandle) -> Option<PathBuf> {
    let resource_dir = app.path().resource_dir().ok()?;
    let jre_dir = resource_dir.join("sidecar").join("jre");

    if !jre_dir.exists() {
        println!("Bundled JRE not found at: {:?}", jre_dir);
        return None;
    }

    #[cfg(target_os = "windows")]
    let java_exe = jre_dir.join("bin").join("java.exe");

    #[cfg(not(target_os = "windows"))]
    let java_exe = jre_dir.join("bin").join("java");

    if java_exe.exists() {
        Some(java_exe)
    } else {
        println!(
            "Bundled JRE directory exists but java executable not found at: {:?}",
            java_exe
        );
        None
    }
}

/// Check if Java/JRE is installed and available
fn check_java_version() -> Result<(), String> {
    use std::process::Command;

    let output = Command::new("java").arg("-version").output().map_err(|_| {
        "Java not found. Please install Java Runtime Environment (JRE).".to_string()
    })?;

    // Java outputs version to stderr
    let version_output = String::from_utf8_lossy(&output.stderr);

    if version_output.is_empty() {
        return Err(
            "Java is not installed or returned no output. Please install Java Runtime Environment (JRE).".to_string()
        );
    }

    if let Some(version_line) = version_output.lines().next() {
        println!("Java detected: {}", version_line);
    }

    Ok(())
}

/// Find the Spring Boot JAR file in the resources directory.
/// If multiple matching JARs exist, the lexicographically last one is selected.
fn find_jar_file(app: &AppHandle) -> Result<PathBuf, String> {
    let resource_dir = app
        .path()
        .resource_dir()
        .map_err(|e| format!("Failed to get resource directory: {}", e))?;

    let sidecar_dir = resource_dir.join("sidecar");

    if !sidecar_dir.exists() {
        return Err(format!("Sidecar directory not found: {:?}", sidecar_dir));
    }

    let entries = std::fs::read_dir(&sidecar_dir)
        .map_err(|e| format!("Failed to read sidecar directory: {}", e))?;

    let mut jar_files: Vec<PathBuf> = entries
        .filter_map(|e| e.ok())
        .map(|e| e.path())
        .filter(|p| {
            p.file_name()
                .and_then(|n| n.to_str())
                .map(|n| n.starts_with("pharmaSmart-") && n.ends_with(".jar"))
                .unwrap_or(false)
        })
        .collect();

    // Sort for deterministic selection; pick the lexicographically last (latest version).
    jar_files.sort();

    match jar_files.into_iter().next_back() {
        Some(path) => {
            println!("Found JAR file: {:?}", path);
            Ok(path)
        }
        None => Err(format!(
            "No pharmaSmart-*.jar file found in {:?}",
            sidecar_dir
        )),
    }
}

/// Attempt a graceful Spring Boot shutdown via the Actuator endpoint.
/// Waits up to 5 seconds for the process to exit before returning.
async fn try_graceful_shutdown(port: u16) {
    let url = format!("http://localhost:{}/management/shutdown", port);
    match reqwest::Client::new().post(&url).send().await {
        Ok(_) => {
            println!("Graceful shutdown signal sent to backend on port {}", port);
            tokio::time::sleep(Duration::from_secs(5)).await;
        }
        Err(_) => {
            // Actuator endpoint unavailable or backend already stopped — proceed to force-kill.
        }
    }
}

/// Start the Spring Boot backend by launching Java directly.
pub async fn start_backend(app: &AppHandle) -> Result<u32, String> {
    // Load configuration from file (or use defaults)
    let config = AppConfig::load(app);
    let port = config.server.port;

    let state = app.state::<BackendState>();

    state.set_status(app, "checking_java", 10, "Vérification de Java...");

    // Try to find bundled JRE first, fall back to system Java
    let java_executable = if let Some(bundled_jre) = find_bundled_jre(app) {
        state.set_status(app, "checking_java", 15, "Utilisation du JRE embarqué...");
        bundled_jre.to_string_lossy().to_string()
    } else {
        state.set_status(app, "checking_java", 12, "Vérification du Java système...");
        check_java_version()?;
        "java".to_string()
    };

    state.set_status(app, "finding_jar", 20, "Recherche du fichier JAR...");

    let jar_path = find_jar_file(app)?;

    state.set_status(app, "starting", 30, "Démarrage du backend Spring Boot...");

    let java_command = app.shell().command(&java_executable);

    if let Err(e) = config.ensure_log_dir() {
        eprintln!("Warning: Failed to create log directory: {}", e);
    }

    let log_file = config.get_log_path();
    let log_dir = config.get_log_dir();

    // Prepare JVM arguments
    let gc_log_path = log_dir.join("gc.log");
    let heap_dump_path = log_dir.join("heapdump.hprof");

    let heap_min_arg = format!("-Xms{}", config.jvm.heap_min);
    let heap_max_arg = format!("-Xmx{}", config.jvm.heap_max);
    let metaspace_size_arg = format!("-XX:MetaspaceSize={}", config.jvm.metaspace_size);
    let metaspace_max_arg = format!("-XX:MaxMetaspaceSize={}", config.jvm.metaspace_max);
    let direct_memory_arg = format!("-XX:MaxDirectMemorySize={}", config.jvm.direct_memory_size);
    let gc_pause_arg = format!("-XX:MaxGCPauseMillis={}", config.jvm.max_gc_pause_millis);
    let gc_log_arg = format!(
        "-Xlog:gc*:file={}:time,level,tags",
        gc_log_path.to_str().unwrap_or("gc.log")
    );
    let heap_dump_arg = format!(
        "-XX:HeapDumpPath={}",
        heap_dump_path.to_str().unwrap_or("heapdump.hprof")
    );
    let server_port_arg = format!("--server.port={}", port);
    let logging_file_arg = format!(
        "--logging.file.name={}",
        log_file.to_str().unwrap_or("pharmasmart.log")
    );

    // Spring Boot application properties from config.json
    let file_report_arg = format!("--file.report={}", config.file.report);
    let file_images_arg = format!("--file.images={}", config.file.images);
    let file_import_json_arg = format!("--file.import.json={}", config.file.import.json);
    let file_import_csv_arg = format!("--file.import.csv={}", config.file.import.csv);
    let file_import_excel_arg = format!("--file.import.excel={}", config.file.import.excel);
    let file_pharmaml_arg = format!("--file.pharmaml={}", config.file.pharmaml);

    let fne_url_arg = format!("--fne.url={}", config.fne.url);
    let fne_api_key_arg = format!("--fne.api-key={}", config.fne.api_key);
    let fne_point_of_sale_arg = format!("--fne.point-of-sale={}", config.fne.point_of_sale);

    let mail_username_arg = format!("--spring.mail.username={}", config.mail.username);
    let mail_email_arg = format!("--mail.email={}", config.mail.email);

    let port_com_arg = format!("--port-com={}", config.port_com);

    let mut args: Vec<&str> = Vec::new();

    // Memory configuration
    args.push(&heap_min_arg);
    args.push(&heap_max_arg);
    args.push(&metaspace_size_arg);
    args.push(&metaspace_max_arg);
    args.push(&direct_memory_arg);

    // Garbage collection - G1GC
    args.push("-XX:+UseG1GC");
    args.push(&gc_pause_arg);

    // Performance optimizations
    args.push("-XX:+UseStringDeduplication");
    args.push("-XX:+UseCompressedOops");

    // Error handling
    args.push("-XX:+HeapDumpOnOutOfMemoryError");
    args.push(&heap_dump_arg);
    args.push(&gc_log_arg);

    // System properties
    args.push("-Dfile.encoding=UTF-8");
    args.push("-Duser.timezone=UTC");
    args.push("-Djava.net.preferIPv4Stack=true");

    // Additional user-defined JVM options from config.json
    let additional_options: Vec<String> = config.jvm.additional_options.clone();
    for option in &additional_options {
        args.push(option.as_str());
    }

    if !additional_options.is_empty() {
        println!("Additional JVM options: {:?}", additional_options);
    }

    println!("Complete JVM Options: {:?}", args);

    // JAR and Spring Boot arguments
    args.push("-jar");
    args.push(jar_path.to_str().ok_or("Invalid JAR path")?);
    args.push("--spring.profiles.active=standalone,tauri,prod");
    args.push(&server_port_arg);
    args.push(&logging_file_arg);

    args.push(&file_report_arg);
    args.push(&file_images_arg);
    args.push(&file_import_json_arg);
    args.push(&file_import_csv_arg);
    args.push(&file_import_excel_arg);
    args.push(&file_pharmaml_arg);

    args.push(&fne_url_arg);
    args.push(&fne_api_key_arg);
    args.push(&fne_point_of_sale_arg);

    args.push(&mail_username_arg);
    args.push(&mail_email_arg);

    args.push(&port_com_arg);

    let (mut rx, child) = java_command.args(args).spawn().map_err(|e| {
        format!(
            "Failed to spawn Java process: {}. Ensure Java is installed and in PATH.",
            e
        )
    })?;

    let pid = child.pid();

    // Store PID immediately so stop_backend can find it regardless of call site.
    *state.process_id.lock().unwrap() = Some(pid);

    state.set_status(
        app,
        "launched",
        40,
        &format!("Processus backend démarré (PID : {})...", pid),
    );

    // Monitor backend output and forward critical errors to the frontend.
    let app_handle = app.clone();
    tauri::async_runtime::spawn(async move {
        while let Some(event) = rx.recv().await {
            match event {
                tauri_plugin_shell::process::CommandEvent::Stdout(line) => {
                    println!("[Backend] {}", String::from_utf8_lossy(&line));
                }
                tauri_plugin_shell::process::CommandEvent::Stderr(line) => {
                    let text = String::from_utf8_lossy(&line);
                    eprintln!("[Backend Error] {}", text);
                    // Emit ERROR / FATAL / Exception lines so the frontend can surface them.
                    if text.contains("ERROR")
                        || text.contains("FATAL")
                        || text.contains("Exception")
                    {
                        let _ = app_handle.emit("backend-log", text.to_string());
                    }
                }
                tauri_plugin_shell::process::CommandEvent::Terminated(status) => {
                    eprintln!("[Backend] Process terminated with status: {:?}", status);
                    let _ = app_handle.emit(
                        "backend-log",
                        format!("Backend process terminated: {:?}", status),
                    );
                    break;
                }
                _ => {}
            }
        }
    });

    state.set_status(app, "waiting", 50, "En attente de la disponibilité du backend...");

    if let Err(e) = wait_for_backend_ready(app, port, 60).await {
        state.set_status(app, "error", 0, &format!("Échec : {}", e));
        return Err(format!("Backend failed to start: {}", e));
    }

    state.set_status(app, "ready", 100, "Le backend est prêt !");

    Ok(pid)
}

/// Wait for the backend to be ready by polling the health endpoint.
async fn wait_for_backend_ready(
    app: &AppHandle,
    port: u16,
    timeout_secs: u64,
) -> Result<(), String> {
    let start = std::time::Instant::now();
    let timeout = Duration::from_secs(timeout_secs);
    let state = app.state::<BackendState>();

    loop {
        if start.elapsed() >= timeout {
            return Err(format!("Timeout waiting for backend on port {}", port));
        }

        let elapsed = start.elapsed().as_secs();
        let progress = 50 + ((elapsed as f32 / timeout_secs as f32) * 45.0) as u8;
        state.set_status(
            app,
            "waiting",
            progress.min(95),
            &format!("Vérification de l'état du backend... ({}/{}s)", elapsed, timeout_secs),
        );

        let url = format!("http://localhost:{}/management/health", port);
        match reqwest::get(&url).await {
            Ok(response) if response.status().is_success() => {
                return Ok(());
            }
            _ => {
                // Backend not ready yet (connection refused OR non-2xx) — wait and retry.
                tokio::time::sleep(Duration::from_millis(500)).await;
            }
        }
    }
}

/// Stop the backend process if it's running.
/// Attempts a graceful Spring Boot Actuator shutdown first, then force-kills.
pub async fn stop_backend(app: &AppHandle) -> Result<(), String> {
    let state = app.state::<BackendState>();
    let process_id = state.process_id.lock().unwrap().take();

    if let Some(pid) = process_id {
        println!("Stopping backend process with PID: {}", pid);
        state.set_status(app, "stopping", 0, "Arrêt du backend en cours...");

        // Graceful shutdown first: give Spring Boot a chance to finish transactions.
        let port = state.port;
        try_graceful_shutdown(port).await;

        // Force-kill whatever is left.
        #[cfg(windows)]
        {
            use std::process::Command;
            let output = Command::new("taskkill")
                .args(["/F", "/PID", &pid.to_string()])
                .output();

            match output {
                Ok(result) if result.status.success() => {
                    println!("Backend process stopped successfully");
                    state.set_status(app, "stopped", 100, "Backend arrêté");
                    Ok(())
                }
                Ok(result) => {
                    let error = String::from_utf8_lossy(&result.stderr);
                    Err(format!("Failed to stop backend: {}", error))
                }
                Err(e) => Err(format!("Failed to execute taskkill: {}", e)),
            }
        }

        #[cfg(not(windows))]
        {
            use nix::sys::signal::{kill, Signal};
            use nix::unistd::Pid;

            match kill(Pid::from_raw(pid as i32), Signal::SIGTERM) {
                Ok(_) => {
                    println!("Backend process stopped successfully");
                    state.set_status(app, "stopped", 100, "Backend arrêté");
                    Ok(())
                }
                Err(e) => Err(format!("Failed to stop backend: {}", e)),
            }
        }
    } else {
        println!("No backend process is running");
        Ok(())
    }
}

/// Restart the backend process.
pub async fn restart_backend(app: AppHandle) -> Result<u32, String> {
    println!("Restarting backend...");

    let state = app.state::<BackendState>();
    state.set_status(&app, "restarting", 10, "Redémarrage du backend...");

    if let Err(e) = stop_backend(&app).await {
        eprintln!("Warning: Failed to stop backend cleanly: {}", e);
        // Continue anyway — the new process might still start successfully.
    }

    // Brief pause after stop_backend (graceful shutdown already waited 5 s internally).
    tokio::time::sleep(Duration::from_secs(2)).await;

    state.set_status(&app, "starting", 30, "Démarrage du backend...");

    start_backend(&app).await
}
