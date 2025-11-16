use std::sync::{Arc, Mutex};
use tauri::{AppHandle, Manager, Emitter};
use tauri_plugin_shell::ShellExt;
use std::time::Duration;
use std::path::PathBuf;
use serde::Serialize;
use crate::config::AppConfig;

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

// Note: Backend process is NOT automatically stopped when Tauri closes
// This allows the backend to continue running for multiple app instances or development
// Users can manually stop the backend process if needed using Task Manager or `taskkill`
/* Drop implementation removed to keep backend running after Tauri closes
impl Drop for BackendState {
    fn drop(&mut self) {
        if let Some(pid) = *self.process_id.lock().unwrap() {
            println!("BackendState dropping, stopping backend process (PID: {})...", pid);

            #[cfg(target_os = "windows")]
            {
                use std::process::Command;
                // Use taskkill to terminate the process tree
                let _ = Command::new("taskkill")
                    .args(["/PID", &pid.to_string(), "/T", "/F"])
                    .output();
            }

            #[cfg(not(target_os = "windows"))]
            {
                use std::process::Command;
                // Use kill command on Unix-like systems
                let _ = Command::new("kill")
                    .args(["-9", &pid.to_string()])
                    .output();
            }

            println!("Backend process stopped");
        }
    }
}
*/

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
        println!("Found bundled JRE at: {:?}", java_exe);
        Some(java_exe)
    } else {
        println!("Bundled JRE directory exists but java executable not found at: {:?}", java_exe);
        None
    }
}

/// Check if Java/JRE is installed and available
fn check_java_version() -> Result<(), String> {
    use std::process::Command;

    let output = Command::new("java")
        .arg("-version")
        .output()
        .map_err(|_| "Java not found. Please install Java Runtime Environment (JRE).".to_string())?;

    // Java outputs version to stderr
    let version_output = String::from_utf8_lossy(&output.stderr);

    // Just verify we got some output (Java is installed)
    if !version_output.is_empty() {
        if let Some(version_line) = version_output.lines().next() {
            println!("Java detected: {}", version_line);
            return Ok(());
        }
    }

    // If we got here, Java responded but with unexpected output
    println!("Warning: Java found but version output unexpected. Proceeding anyway...");
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

    Err(format!("No pharmaSmart-*.jar file found in {:?}", sidecar_dir))
}

/// Start the Spring Boot backend by launching Java directly
pub async fn start_backend(app: &AppHandle, _default_port: u16) -> Result<u32, String> {
    // Load configuration from file (or use defaults)
    let config = AppConfig::load(app);
    let port = config.server.port;

    println!("Starting Spring Boot backend on port {}...", port);
    println!("Configuration loaded:");
    println!("  - Port: {}", config.server.port);
    println!("  - Log directory: {}", config.logging.directory);
    println!("  - Log file: {}", config.logging.file);

    let state = app.state::<BackendState>();

    // Update status: Checking Java
    state.update_status("checking_java".to_string(), 10, "Checking for Java...".to_string());
    let _ = app.emit("backend-status", state.get_status());

    // Try to find bundled JRE first, fall back to system Java
    let java_executable = if let Some(bundled_jre) = find_bundled_jre(app) {
        println!("Using bundled JRE: {:?}", bundled_jre);
        state.update_status("checking_java".to_string(), 15, "Using bundled JRE...".to_string());
        let _ = app.emit("backend-status", state.get_status());
        bundled_jre.to_string_lossy().to_string()
    } else {
        println!("Bundled JRE not found, checking for system Java...");
        state.update_status("checking_java".to_string(), 12, "Checking system Java...".to_string());
        let _ = app.emit("backend-status", state.get_status());

        // Check if system Java is available
        check_java_version()?;
        "java".to_string()
    };

    println!("Using Java executable: {}", java_executable);

    // Update status: Finding JAR
    state.update_status("finding_jar".to_string(), 20, "Locating backend JAR file...".to_string());
    let _ = app.emit("backend-status", state.get_status());

    // Find the JAR file
    let jar_path = find_jar_file(app)?;
    println!("Using JAR file: {:?}", jar_path);

    // Update status: Starting backend
    state.update_status("starting".to_string(), 30, "Starting Spring Boot backend...".to_string());
    let _ = app.emit("backend-status", state.get_status());

    // Build Java command
    let java_command = app.shell().command(&java_executable);

    // Ensure log directory exists (from config)
    if let Err(e) = config.ensure_log_dir() {
        eprintln!("Warning: Failed to create log directory: {}", e);
    }

    let log_file = config.get_log_path();
    println!("Backend logs will be written to: {:?}", log_file);

    // Spawn the Java process with Spring Boot arguments
    let (mut rx, child) = java_command
        .args([
            "-jar",
            jar_path.to_str().ok_or("Invalid JAR path")?,
            "--spring.profiles.active=standalone,tauri,prod",
            &format!("--server.port={}", port),
            &format!("--logging.file.name={}", log_file.to_str().unwrap_or("pharmasmart.log")),
        ])
        .spawn()
        .map_err(|e| format!("Failed to spawn Java process: {}. Ensure Java is installed and in PATH.", e))?;

    let pid = child.pid();
    println!("Backend process started with PID: {}", pid);

    // Update status: Backend launched
    state.update_status("launched".to_string(), 40, format!("Backend process started (PID: {})...", pid));
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
    state.update_status("waiting".to_string(), 50, "Waiting for backend to be ready...".to_string());
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

    println!("Backend is ready and accepting connections on port {}", port);
    Ok(pid)
}

/// Wait for the backend to be ready by checking if the port is listening
async fn wait_for_backend_ready(app: &AppHandle, port: u16, timeout_secs: u64) -> Result<(), String> {
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
            format!("Checking backend health... ({}/{}s)", elapsed, timeout_secs)
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

