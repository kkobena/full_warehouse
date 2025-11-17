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
}

/// Find the bundled JRE in the resources directory
fn find_bundled_jre(app: &AppHandle) -> Option<PathBuf> {
    let resource_dir = app.path().resource_dir().ok()?;
    let jre_dir = resource_dir.join("sidecar").join("jre");

    if !jre_dir.exists() {
        println!("Bundled JRE not found at: {:?}", jre_dir);
        return None;
    }

    // Find java executable in bundled JRE
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

    // Just verify we got some output (Java is installed)
    if !version_output.is_empty() {
        if let Some(version_line) = version_output.lines().next() {
            println!("Java detected: {}", version_line);
            return Ok(());
        }
    }

    Ok(())
}

/// Find the Spring Boot JAR file in the resources directory
fn find_jar_file(app: &AppHandle) -> Result<PathBuf, String> {
    let resource_dir = app
        .path()
        .resource_dir()
        .map_err(|e| format!("Failed to get resource directory: {}", e))?;

    let sidecar_dir = resource_dir.join("sidecar");

    if !sidecar_dir.exists() {
        return Err(format!("Sidecar directory not found: {:?}", sidecar_dir));
    }

    // Find JAR file matching pattern pharmaSmart-*.jar
    let entries = std::fs::read_dir(&sidecar_dir)
        .map_err(|e| format!("Failed to read sidecar directory: {}", e))?;

    for entry in entries {
        if let Ok(entry) = entry {
            let path = entry.path();
            if let Some(filename) = path.file_name().and_then(|n| n.to_str()) {
                if filename.starts_with("pharmaSmart-") && filename.ends_with(".jar") {
                    println!("Found JAR file: {:?}", path);
                    return Ok(path);
                }
            }
        }
    }

    Err(format!(
        "No pharmaSmart-*.jar file found in {:?}",
        sidecar_dir
    ))
}

/// Start the Spring Boot backend by launching Java directly
pub async fn start_backend(app: &AppHandle, _default_port: u16) -> Result<u32, String> {
    // Load configuration from file (or use defaults)
    let config = AppConfig::load(app);
    let port = config.server.port;

    let state = app.state::<BackendState>();

    // Update status: Checking Java
    state.update_status(
        "checking_java".to_string(),
        10,
        "Checking for Java...".to_string(),
    );
    let _ = app.emit("backend-status", state.get_status());

    // Try to find bundled JRE first, fall back to system Java
    let java_executable = if let Some(bundled_jre) = find_bundled_jre(app) {
        state.update_status(
            "checking_java".to_string(),
            15,
            "Using bundled JRE...".to_string(),
        );
        let _ = app.emit("backend-status", state.get_status());
        bundled_jre.to_string_lossy().to_string()
    } else {
        state.update_status(
            "checking_java".to_string(),
            12,
            "Checking system Java...".to_string(),
        );
        let _ = app.emit("backend-status", state.get_status());

        // Check if system Java is available
        check_java_version()?;
        "java".to_string()
    };

    // Update status: Finding JAR
    state.update_status(
        "finding_jar".to_string(),
        20,
        "Locating backend JAR file...".to_string(),
    );
    let _ = app.emit("backend-status", state.get_status());

    // Find the JAR file
    let jar_path = find_jar_file(app)?;

    // Update status: Starting backend
    state.update_status(
        "starting".to_string(),
        30,
        "Starting Spring Boot backend...".to_string(),
    );
    let _ = app.emit("backend-status", state.get_status());

    // Build Java command
    let java_command = app.shell().command(&java_executable);

    // Ensure log directory exists (from config)
    if let Err(e) = config.ensure_log_dir() {
        eprintln!("Warning: Failed to create log directory: {}", e);
    }

    let log_file = config.get_log_path();
    let log_dir = config.get_log_dir();

    // Prepare formatted JVM arguments (from config.json)
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
    // File configuration
    let file_report_arg = format!("--file.report={}", config.file.report);
    let file_images_arg = format!("--file.images={}", config.file.images);
    let file_import_json_arg = format!("--file.import.json={}", config.file.import.json);
    let file_import_csv_arg = format!("--file.import.csv={}", config.file.import.csv);
    let file_import_excel_arg = format!("--file.import.excel={}", config.file.import.excel);
    let file_pharmaml_arg = format!("--file.pharmaml={}", config.file.pharmaml);

    // FNE configuration
    let fne_url_arg = format!("--fne.url={}", config.fne.url);
    let fne_api_key_arg = format!("--fne.api-key={}", config.fne.api_key);
    let fne_point_of_sale_arg = format!("--fne.point-of-sale={}", config.fne.point_of_sale);

    // Mail configuration
    let mail_username_arg = format!("--spring.mail.username={}", config.mail.username);
    let mail_email_arg = format!("--mail.email={}", config.mail.email);

    // Port-Com configuration
    let port_com_arg = format!("--port-com={}", config.port_com);

    // Build JVM arguments vector from config
    let mut args: Vec<&str> = Vec::new();

    // Memory Configuration (from config.json)
    args.push(&heap_min_arg);
    args.push(&heap_max_arg);
    args.push(&metaspace_size_arg);
    args.push(&metaspace_max_arg);
    args.push(&direct_memory_arg);

    // Garbage Collection - G1GC
    args.push("-XX:+UseG1GC");
    args.push(&gc_pause_arg);

    // Performance Optimizations (always enabled)
    args.push("-XX:+UseStringDeduplication");
    args.push("-XX:+UseCompressedOops");

    // Error Handling
    args.push("-XX:+HeapDumpOnOutOfMemoryError");
    args.push(&heap_dump_arg);
    args.push(&gc_log_arg);

    // System Properties (always enabled)
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

    // Add JAR and Spring Boot arguments
    args.push("-jar");
    args.push(jar_path.to_str().ok_or("Invalid JAR path")?);
    args.push("--spring.profiles.active=standalone,tauri,prod");
    args.push(&server_port_arg);
    args.push(&logging_file_arg);

    // Application properties from config.json
    // File paths
    args.push(&file_report_arg);
    args.push(&file_images_arg);
    args.push(&file_import_json_arg);
    args.push(&file_import_csv_arg);
    args.push(&file_import_excel_arg);
    args.push(&file_pharmaml_arg);

    // FNE configuration
    args.push(&fne_url_arg);
    args.push(&fne_api_key_arg);
    args.push(&fne_point_of_sale_arg);

    // Mail configuration
    args.push(&mail_username_arg);
    args.push(&mail_email_arg);

    // Port-Com configuration
    args.push(&port_com_arg);

    // Spawn the Java process with JVM options and Spring Boot arguments
    let (mut rx, child) = java_command.args(args).spawn().map_err(|e| {
        format!(
            "Failed to spawn Java process: {}. Ensure Java is installed and in PATH.",
            e
        )
    })?;

    let pid = child.pid();
    // Update status: Backend launched
    state.update_status(
        "launched".to_string(),
        40,
        format!("Backend process started (PID: {})...", pid),
    );
    let _ = app.emit("backend-status", state.get_status());

    // Spawn a task to monitor backend output
    tauri::async_runtime::spawn(async move {
        while let Some(event) = rx.recv().await {
            match event {
                tauri_plugin_shell::process::CommandEvent::Stdout(line) => {
                    println!("[Backend] {}", String::from_utf8_lossy(&line));
                }
                tauri_plugin_shell::process::CommandEvent::Stderr(line) => {
                    eprintln!("[Backend Error] {}", String::from_utf8_lossy(&line));
                }
                tauri_plugin_shell::process::CommandEvent::Terminated(status) => {
                    eprintln!("[Backend] Process terminated with status: {:?}", status);
                    break;
                }
                _ => {}
            }
        }
    });

    // Update status: Waiting for readiness
    state.update_status(
        "waiting".to_string(),
        50,
        "Waiting for backend to be ready...".to_string(),
    );
    let _ = app.emit("backend-status", state.get_status());

    // Wait for backend to be ready (check if port is listening)
    if let Err(e) = wait_for_backend_ready(app, port, 60).await {
        state.update_status("error".to_string(), 0, format!("Failed: {}", e));
        let _ = app.emit("backend-status", state.get_status());
        return Err(format!("Backend failed to start: {}", e));
    }

    // Update status: Ready
    state.update_status("ready".to_string(), 100, "Backend is ready!".to_string());
    let _ = app.emit("backend-status", state.get_status());

    Ok(pid)
}

/// Wait for the backend to be ready by checking if the port is listening
async fn wait_for_backend_ready(
    app: &AppHandle,
    port: u16,
    timeout_secs: u64,
) -> Result<(), String> {
    let start = std::time::Instant::now();
    let timeout = Duration::from_secs(timeout_secs);
    let state = app.state::<BackendState>();

    loop {
        let elapsed = start.elapsed().as_secs();
        if elapsed > timeout_secs {
            return Err(format!("Timeout waiting for backend on port {}", port));
        }

        // Calculate progress (50-95% during waiting phase)
        let progress = 50 + ((elapsed as f32 / timeout_secs as f32) * 45.0) as u8;
        state.update_status(
            "waiting".to_string(),
            progress.min(95),
            format!("Checking backend health... ({}/{}s)", elapsed, timeout_secs),
        );
        let _ = app.emit("backend-status", state.get_status());

        // Try to connect to the backend health endpoint
        let url = format!("http://localhost:{}/management/health", port);
        match reqwest::get(&url).await {
            Ok(response) => {
                if response.status().is_success() {
                    return Ok(());
                }
            }
            Err(_) => {
                // Backend not ready yet, wait and retry
                tokio::time::sleep(Duration::from_millis(500)).await;
            }
        }
    }
}

/// Stop the backend process if it's running
pub fn stop_backend(app: &AppHandle) -> Result<(), String> {
    let state = app.state::<BackendState>();
    let process_id = state.process_id.lock().unwrap().take();

    if let Some(pid) = process_id {
        println!("Stopping backend process with PID: {}", pid);

        state.update_status("stopping".to_string(), 0, "Stopping backend...".to_string());
        let _ = app.emit("backend-status", state.get_status());

        // Kill the process
        #[cfg(windows)]
        {
            use std::process::Command;
            let output = Command::new("taskkill")
                .args(["/F", "/PID", &pid.to_string()])
                .output();

            match output {
                Ok(result) => {
                    if result.status.success() {
                        println!("Backend process stopped successfully");
                        state.update_status(
                            "stopped".to_string(),
                            100,
                            "Backend stopped".to_string(),
                        );
                        let _ = app.emit("backend-status", state.get_status());
                        Ok(())
                    } else {
                        let error = String::from_utf8_lossy(&result.stderr);
                        Err(format!("Failed to stop backend: {}", error))
                    }
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
                    state.update_status("stopped".to_string(), 100, "Backend stopped".to_string());
                    let _ = app.emit("backend-status", state.get_status());
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

/// Restart the backend process
pub async fn restart_backend(app: AppHandle) -> Result<u32, String> {
    println!("Restarting backend...");

    let state = app.state::<BackendState>();
    state.update_status(
        "restarting".to_string(),
        10,
        "Restarting backend...".to_string(),
    );
    let _ = app.emit("backend-status", state.get_status());

    // Stop the backend if it's running
    if let Err(e) = stop_backend(&app) {
        eprintln!("Warning: Failed to stop backend cleanly: {}", e);
        // Continue anyway - the new process might still start
    }

    // Wait a bit for the process to fully terminate
    tokio::time::sleep(Duration::from_secs(2)).await;

    state.update_status(
        "starting".to_string(),
        30,
        "Starting backend...".to_string(),
    );
    let _ = app.emit("backend-status", state.get_status());

    // Start the backend again
    start_backend(&app, state.port).await
}
