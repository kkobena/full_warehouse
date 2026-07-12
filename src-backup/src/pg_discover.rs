use anyhow::{Result, bail};
use std::path::PathBuf;

/// Renvoie le répertoire bin/ de la première installation PostgreSQL trouvée.
///
/// Ordre de recherche :
///   1. `backup.pg_bin` du config.json (si fourni)
///   2. Variable d'environnement PGBIN
///   3. Registre Windows : HKLM\SOFTWARE\PostgreSQL\Installations\*\Base Directory
///   4. Chemins courants : C:\Program Files\PostgreSQL\{18,17,16,...}\bin
///   5. pg_dump dans le PATH
pub fn find_pg_bin(configured: Option<&std::path::Path>) -> Result<PathBuf> {
    // 0. Chemin explicite du config.json
    if let Some(p) = configured.filter(|p| !p.as_os_str().is_empty()) {
        if p.join(pg_dump_exe()).exists() {
            tracing::info!("PostgreSQL bin depuis config.json : {}", p.display());
            return Ok(p.to_path_buf());
        }
        tracing::warn!(
            "backup.pg_bin configuré mais {} absent de {} — détection automatique.",
            pg_dump_exe(),
            p.display()
        );
    }

    // 1. Env var (surcharge manuelle)
    if let Ok(v) = std::env::var("PGBIN") {
        let p = PathBuf::from(v);
        if p.join(pg_dump_exe()).exists() {
            tracing::info!("PostgreSQL bin depuis PGBIN : {}", p.display());
            return Ok(p);
        }
    }

    // 2. Registre Windows
    #[cfg(windows)]
    if let Some(p) = find_pg_bin_from_registry() {
        tracing::info!("PostgreSQL bin depuis le registre : {}", p.display());
        return Ok(p);
    }

    // 3. Chemins standards (versions 18 → 13)
    #[cfg(windows)]
    for ver in (13u8..=18).rev() {
        let p = PathBuf::from(format!(r"C:\Program Files\PostgreSQL\{ver}\bin"));
        if p.join(pg_dump_exe()).exists() {
            tracing::info!("PostgreSQL bin (chemin standard v{ver}) : {}", p.display());
            return Ok(p);
        }
    }

    // 4. PATH
    let lookup_cmd = if cfg!(windows) { "where" } else { "which" };
    if let Ok(out) = std::process::Command::new(lookup_cmd)
        .arg("pg_dump")
        .output()
    {
        if let Ok(s) = std::str::from_utf8(&out.stdout) {
            if let Some(line) = s.lines().next() {
                let p = PathBuf::from(line.trim());
                if let Some(parent) = p.parent() {
                    tracing::info!("PostgreSQL bin depuis PATH : {}", parent.display());
                    return Ok(parent.to_path_buf());
                }
            }
        }
    }

    bail!(
        "Impossible de localiser le répertoire bin/ de PostgreSQL. \
         Définissez la variable d'environnement PGBIN."
    )
}

fn pg_dump_exe() -> &'static str {
    if cfg!(windows) {
        "pg_dump.exe"
    } else {
        "pg_dump"
    }
}

#[cfg(windows)]
fn find_pg_bin_from_registry() -> Option<PathBuf> {
    use winreg::RegKey;
    use winreg::enums::*;

    let hklm = RegKey::predef(HKEY_LOCAL_MACHINE);
    let installations = hklm
        .open_subkey(r"SOFTWARE\PostgreSQL\Installations")
        .ok()?;

    // Parcourt les sous-clés (ex. "postgresql-x64-18")
    let mut candidates: Vec<(String, PathBuf)> = Vec::new();
    for name in installations.enum_keys().flatten() {
        if let Ok(sub) = installations.open_subkey(&name) {
            if let Ok(base_dir) = sub.get_value::<String, _>("Base Directory") {
                let bin = PathBuf::from(&base_dir).join("bin");
                if bin.join("pg_dump.exe").exists() {
                    candidates.push((name, bin));
                }
            }
        }
    }

    // Tri décroissant pour prendre la version la plus récente (ex. postgresql-x64-18 > 17)
    candidates.sort_by(|a, b| b.0.cmp(&a.0));
    candidates.into_iter().next().map(|(_, p)| p)
}
