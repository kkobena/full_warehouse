/// Types partagés entre les modules Tauri de PharmaSmart.
use serde::Serialize;
use std::path::PathBuf;
use thiserror::Error;

// ─── Health ──────────────────────────────────────────────────────────────────

#[derive(Clone, Serialize)]
pub struct BackendHealthStatus {
    pub available: bool,
    pub message: String,
}

// ─── Backend errors ──────────────────────────────────────────────────────────

#[derive(Debug, Error)]
pub enum BackendError {
    #[error("Java non trouvé : {0}")]
    JavaNotFound(String),

    #[error("Fichier JAR introuvable dans {0}")]
    JarNotFound(PathBuf),

    #[error("Timeout démarrage backend après {0}s")]
    StartupTimeout(u64),

    #[error("Échec lancement du processus Java : {0}")]
    SpawnFailed(String),

    #[error("Erreur de configuration : {0}")]
    ConfigError(String),

    /// PostgreSQL n'est pas joignable sur l'adresse indiquée.
    #[error("PostgreSQL non disponible sur {0} : {1}")]
    DatabaseNotReady(String, String),
}

/// Conversion vers `String` pour les retours de commandes Tauri.
impl From<BackendError> for String {
    fn from(e: BackendError) -> Self {
        e.to_string()
    }
}

// ─── Printer errors ──────────────────────────────────────────────────────────

#[derive(Debug, Error)]
pub enum PrinterError {
    #[error("Imprimante introuvable : {0}")]
    NotFound(String),

    #[error("Échec ouverture imprimante : {0}")]
    OpenFailed(String),

    #[error("Échec écriture vers l'imprimante : {0}")]
    WriteFailed(String),

    #[error("Erreur PowerShell : {0}")]
    PowerShellFailed(String),
}

impl From<PrinterError> for String {
    fn from(e: PrinterError) -> Self {
        e.to_string()
    }
}

