//! Utilitaires partagés par les tests unitaires.
//!
//! Volontairement sans dépendance externe : le module n'a besoin que d'un répertoire jetable et de
//! fichiers dont on maîtrise la date de modification, ce que la bibliothèque standard suffit à
//! fournir depuis `File::set_modified`.

use std::path::{Path, PathBuf};
use std::sync::atomic::{AtomicU32, Ordering};
use std::time::{Duration, SystemTime};

/// Répertoire temporaire vide, propre à l'appelant.
pub fn scratch_dir(label: &str) -> PathBuf {
    static COUNTER: AtomicU32 = AtomicU32::new(0);
    let unique = COUNTER.fetch_add(1, Ordering::Relaxed);
    let dir = std::env::temp_dir().join(format!(
        "pharmasmart-backup-test-{label}-{}-{unique}",
        std::process::id()
    ));
    let _ = std::fs::remove_dir_all(&dir);
    std::fs::create_dir_all(&dir).expect("création du répertoire de test");
    dir
}

/// Crée un fichier daté d'il y a `age_secs`.
pub fn write_aged_file(dir: &Path, name: &str, age_secs: u64) -> PathBuf {
    let path = dir.join(name);
    std::fs::write(&path, b"x").expect("écriture du fichier de test");
    std::fs::OpenOptions::new()
        .write(true)
        .open(&path)
        .expect("ouverture du fichier de test")
        .set_modified(SystemTime::now() - Duration::from_secs(age_secs))
        .expect("datation du fichier de test");
    path
}
