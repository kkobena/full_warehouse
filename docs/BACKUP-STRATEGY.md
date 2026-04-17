# Stratégie de Backup — PharmaSmart / PostgreSQL 18+ sur Windows

> **Hypothèses :**
> - L'application fonctionne normalement en journée, mais peut tourner **24h/24 en période de garde**.
> - Le backup est **entièrement autonome** (Windows Task Scheduler) — aucun déclenchement par l'application.
> - Tous les chemins sont **dynamiques** (lus depuis `config.json`, aucune valeur codée en dur).
> - Les scripts sont écrits en **Rust** (`pharmasmart-backup.exe`), binaire standalone.

---

## Vue d'ensemble

| Niveau | Méthode | Déclenchement | Rétention | RPO |
|---|---|---|---|---|
| Logique fréquent | `pg_dump` | Toutes les 2 h | 30 jours | 2 h |
| Physique base | `pg_basebackup` | Dimanche 02h00 | 4 semaines | 1 semaine |
| WAL continu | Archivage WAL | Continu (si garde active) | 7 jours | < 5 min |
| Purge | `std::fs` | Quotidien 03h00 | — | — |

Le backup toutes les 2 h couvre **à la fois** le fonctionnement normal et les périodes de garde, sans nécessiter de configuration différente.

---

## 1. Extension de `config.json` , le fichier à gerer par l'installeur NSIS et à lire par le binaire Rust

### 1.1 Section `backup` à ajouter

La section `backup` est écrite par le **installeur NSIS** (voir §5) et lue par `pharmasmart-backup.exe`.

```json
{
  "backup": {
    "directory": "D:\\backups\\pharmasmart",
    "db": "pharmasmart",
    "host": "localhost",
    "port": 5432,
    "user": "pharmasmart",
    "retention_daily_days": 30,
    "retention_base_weeks": 4,
    "wal_archiving": false,
    "wal_directory": ""
  }
}
```

| Clé | Description |
|---|---|
| `directory` | Répertoire **parent** des backups — choisi à l'installation |
| `db` | Nom de la base PostgreSQL |
| `host` / `port` | Connexion PostgreSQL |
| `user` | Utilisateur PostgreSQL |
| `retention_daily_days` | Durée de rétention des dumps (jours) |
| `retention_base_weeks` | Durée de rétention des base backups (semaines) |
| `wal_archiving` | `true` pour activer l'archivage WAL (périodes de garde) |
| `wal_directory` | Répertoire de destination des WAL archivés (vide = `{directory}/wal`) |

### 1.2 Résolution du fichier `config.json`

`pharmasmart-backup.exe` cherche `config.json` dans l'ordre :

1. `%PROGRAMDATA%\PharmaSmart\config.json`  ← installation multi-utilisateurs
2. `%APPDATA%\PharmaSmart\config.json`       ← installation mono-utilisateur
3. Répertoire du binaire lui-même            ← fallback développement

---

## 2. Architecture du binaire Rust

```
pharmasmart-backup.exe <sous-commande>

  dump      — pg_dump logique (toutes les 2 h via Task Scheduler)
  base      — pg_basebackup physique (hebdomadaire)
  purge     — rotation/suppression des anciens fichiers
  check     — vérifie la présence d'un dump récent
  wal-purge — purge les WAL archivés > retention
```

### Structure du projet à mettre dans `src-backup`

```
backup-tool/
├── Cargo.toml
└── src/
    ├── main.rs
    ├── config.rs      # Lecture config.json + découverte chemins
    ├── pg_discover.rs # Détection dynamique du binaire PostgreSQL
    ├── dump.rs        # pg_dump
    ├── base.rs        # pg_basebackup
    ├── purge.rs       # Rotation fichiers
    ├── check.rs       # Vérification intégrité
    └── logger.rs      # Logs horodatés
```

### `Cargo.toml`

```toml
[package]
name    = "pharmasmart-backup"
version = "0.1.0"
edition = "2024"

[dependencies]
anyhow             = "1"
chrono             = { version = "0.4", features = ["serde"] }
clap               = { version = "4", features = ["derive"] }
dirs               = "6"
serde              = { version = "1", features = ["derive"] }
serde_json         = "1"
tracing            = "0.1"
tracing-subscriber = { version = "0.3", features = ["env-filter"] }
walkdir            = "2"

[target.'cfg(windows)'.dependencies]
winreg = "0.52"
```

---

## 3. Code source

### `config.rs` — Lecture de `config.json`

```rust
use anyhow::{Context, Result};
use dirs::{config_dir, data_dir};
use serde::Deserialize;
use std::path::{Path, PathBuf};

#[derive(Debug, Deserialize)]
pub struct BackupConfig {
    pub directory:            PathBuf,
    pub db:                   String,
    pub host:                 String,
    pub port:                 u16,
    pub user:                 String,
    #[serde(default = "default_retention_daily")]
    pub retention_daily_days: u64,
    #[serde(default = "default_retention_base")]
    pub retention_base_weeks: u64,
    #[serde(default)]
    pub wal_archiving:        bool,
    #[serde(default)]
    pub wal_directory:        Option<PathBuf>,
}

#[derive(Debug, Deserialize)]
struct AppConfig {
    backup: BackupConfig,
}

fn default_retention_daily() -> u64 { 30 }
fn default_retention_base()  -> u64 { 4  }

impl BackupConfig {
    pub fn load() -> Result<Self> {
        let path = find_config_file()
            .context("config.json introuvable (ProgramData, AppData, répertoire courant)")?;
        tracing::info!("config.json lu depuis : {}", path.display());

        let raw = std::fs::read_to_string(&path)
            .with_context(|| format!("Lecture de {}", path.display()))?;
        let app: AppConfig = serde_json::from_str(&raw)
            .context("Erreur de parsing config.json — section 'backup' manquante ?")?;
        Ok(app.backup)
    }

    pub fn daily_dir(&self)  -> PathBuf { self.directory.join("daily") }
    pub fn base_dir(&self)   -> PathBuf { self.directory.join("basebackup") }
    pub fn wal_dir(&self)    -> PathBuf {
        self.wal_directory.clone()
            .unwrap_or_else(|| self.directory.join("wal"))
    }
    pub fn log_dir(&self)    -> PathBuf { self.directory.join("logs") }
    pub fn log_file(&self)   -> PathBuf { self.log_dir().join("backup.log") }
}

fn find_config_file() -> Option<PathBuf> {
    let candidates: Vec<PathBuf> = vec![
        // 1. ProgramData\PharmaSmart  (install multi-utilisateurs)
        data_dir().map(|d| d.join("PharmaSmart").join("config.json"))
                  .unwrap_or_default(),
        // 2. AppData\Roaming\PharmaSmart (install mono-utilisateur)
        config_dir().map(|d| d.join("PharmaSmart").join("config.json"))
                    .unwrap_or_default(),
        // 3. Répertoire du binaire
        std::env::current_exe().ok()
            .and_then(|p| p.parent().map(|d| d.join("config.json")))
            .unwrap_or_default(),
    ];

    candidates.into_iter().find(|p| p.exists())
}
```

---

### `pg_discover.rs` — Découverte dynamique de PostgreSQL

```rust
use anyhow::{bail, Result};
use std::path::PathBuf;

/// Renvoie le répertoire bin/ de la première installation PostgreSQL trouvée.
///
/// Ordre de recherche :
///   1. Variable d'environnement PGBIN
///   2. Registre Windows : HKLM\SOFTWARE\PostgreSQL\Installations\*\Base Directory
///   3. Chemins courants : C:\Program Files\PostgreSQL\{18,17,16,...}\bin
///   4. pg_dump dans le PATH
pub fn find_pg_bin() -> Result<PathBuf> {
    // 1. Env var (surcharge manuelle)
    if let Ok(v) = std::env::var("PGBIN") {
        let p = PathBuf::from(v);
        if p.join("pg_dump.exe").exists() {
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
    for ver in (13u8..=18).rev() {
        let p = PathBuf::from(format!(
            r"C:\Program Files\PostgreSQL\{ver}\bin"
        ));
        if p.join("pg_dump.exe").exists() {
            tracing::info!("PostgreSQL bin (chemin standard v{ver}) : {}", p.display());
            return Ok(p);
        }
    }

    // 4. PATH
    if let Ok(out) = std::process::Command::new("where").arg("pg_dump").output() {
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

    bail!("Impossible de localiser le répertoire bin/ de PostgreSQL. \
           Définissez la variable d'environnement PGBIN.")
}

#[cfg(windows)]
fn find_pg_bin_from_registry() -> Option<PathBuf> {
    use winreg::enums::*;
    use winreg::RegKey;

    let hklm = RegKey::predef(HKEY_LOCAL_MACHINE);
    let installations = hklm
        .open_subkey(r"SOFTWARE\PostgreSQL\Installations")
        .ok()?;

    // Parcourt les sous-clés (ex. "postgresql-x64-18")
    for name in installations.enum_keys().flatten() {
        if let Ok(sub) = installations.open_subkey(&name) {
            if let Ok(base_dir) = sub.get_value::<String, _>("Base Directory") {
                let bin = PathBuf::from(&base_dir).join("bin");
                if bin.join("pg_dump.exe").exists() {
                    return Some(bin);
                }
            }
        }
    }
    None
}
```

---

### `dump.rs` — pg_dump toutes les 2 h

```rust
use crate::{config::BackupConfig, logger, pg_discover::find_pg_bin};
use anyhow::{bail, Result};
use chrono::Local;
use std::process::Command;
use tracing::{error, info};

pub fn run(cfg: &BackupConfig) -> Result<()> {
    std::fs::create_dir_all(cfg.daily_dir())?;
    let pg_bin = find_pg_bin()?;

    let timestamp = Local::now().format("%Y%m%d_%H%M%S");
    let filename  = cfg.daily_dir()
        .join(format!("{}_{}.dump", cfg.db, timestamp));

    info!("pg_dump → {}", filename.display());

    let mut cmd = Command::new(pg_bin.join("pg_dump.exe"));
    cmd.args([
        "-h", &cfg.host,
        "-p", &cfg.port.to_string(),
        "-U", &cfg.user,
        "-F", "c",
        "-Z", "6",        // Z9 divise le CPU par ~3 pour un gain marginal sur Z6
        "--no-password",
        "-f", filename.to_str().unwrap(),
        &cfg.db,
    ]);
    // Mot de passe via variable d'environnement (jamais dans la ligne de commande)
    if let Ok(pw) = std::env::var("PGPASSWORD") {
        cmd.env("PGPASSWORD", pw);
    }

    let status = cmd.status()?;
    if !status.success() {
        let msg = format!("[ERREUR] dump échoué : {}", filename.display());
        error!("{msg}");
        logger::append(cfg, &msg)?;
        bail!("{msg}");
    }

    let size = std::fs::metadata(&filename)?.len();
    let msg  = format!("[OK] dump → {} ({} octets)", filename.display(), size);
    info!("{msg}");
    logger::append(cfg, &msg)
}
```

---

### `base.rs` — pg_basebackup hebdomadaire

```rust
use crate::{config::BackupConfig, pg_discover::find_pg_bin};
use anyhow::{bail, Result};
use chrono::Local;
use std::process::Command;
use tracing::{error, info};

pub fn run(cfg: &BackupConfig) -> Result<()> {
    let dest = cfg.base_dir()
        .join(format!("base_{}", Local::now().format("%Y%m%d")));
    std::fs::create_dir_all(&dest)?;
    let pg_bin = find_pg_bin()?;

    info!("pg_basebackup → {}", dest.display());

    let mut cmd = Command::new(pg_bin.join("pg_basebackup.exe"));
    cmd.args([
        "-h", &cfg.host,
        "-p", &cfg.port.to_string(),
        "-U", &cfg.user,
        "-D", dest.to_str().unwrap(),
        "-F", "tar", "-z",
        "-Xs",
        "--checkpoint=fast",
        "--progress",
    ]);
    if let Ok(pw) = std::env::var("PGPASSWORD") {
        cmd.env("PGPASSWORD", pw);
    }

    let status = cmd.status()?;
    if !status.success() {
        let msg = format!("[ERREUR] basebackup échoué → {}", dest.display());
        error!("{msg}");
        bail!("{msg}");
    }

    info!("[OK] basebackup → {}", dest.display());
    Ok(())
}
```

---

### `purge.rs` — Rotation des fichiers

```rust
use crate::config::BackupConfig;
use anyhow::Result;
use std::time::{Duration, SystemTime};
use tracing::info;
use walkdir::WalkDir;

pub fn run(cfg: &BackupConfig) -> Result<()> {
    let daily_cutoff = cfg.retention_daily_days * 86_400;
    let base_cutoff  = cfg.retention_base_weeks * 7 * 86_400;

    purge_dir(cfg.daily_dir(), daily_cutoff, false)?;
    purge_dir(cfg.base_dir(),  base_cutoff,  true)?;

    if cfg.wal_archiving {
        purge_dir(cfg.wal_dir(), 7 * 86_400, false)?;
    }
    Ok(())
}

fn purge_dir(dir: std::path::PathBuf, max_age_secs: u64, dirs_only: bool) -> Result<()> {
    if !dir.exists() { return Ok(()); }
    let cutoff = SystemTime::now() - Duration::from_secs(max_age_secs);

    for entry in WalkDir::new(&dir).min_depth(1).max_depth(1) {
        let entry = entry?;
        let meta  = entry.metadata()?;
        if meta.modified()? > cutoff { continue; }

        if dirs_only && meta.is_dir() {
            std::fs::remove_dir_all(entry.path())?;
            info!("Supprimé (répertoire) : {}", entry.path().display());
        } else if !dirs_only && meta.is_file() {
            std::fs::remove_file(entry.path())?;
            info!("Supprimé (fichier) : {}", entry.path().display());
        }
    }
    Ok(())
}
```

---

### `main.rs`

```rust
use clap::{Parser, Subcommand};
use anyhow::Result;

mod config;
mod pg_discover;
mod dump;
mod base;
mod purge;
mod check;
mod logger;

#[derive(Parser)]
#[command(name = "pharmasmart-backup", version)]
struct Cli {
    #[command(subcommand)]
    command: Cmd,
}

#[derive(Subcommand)]
enum Cmd {
    /// pg_dump logique (à planifier toutes les 2 h)
    Dump,
    /// pg_basebackup physique (hebdomadaire)
    Base,
    /// Rotation/purge des anciens fichiers
    Purge,
    /// Vérifie la présence d'un dump récent (< 3 h)
    Check,
}

fn main() -> Result<()> {
    tracing_subscriber::fmt::init();
    let cli = Cli::parse();
    let cfg = config::BackupConfig::load()?;

    match cli.command {
        Cmd::Dump  => dump::run(&cfg),
        Cmd::Base  => base::run(&cfg),
        Cmd::Purge => purge::run(&cfg),
        Cmd::Check => check::run(&cfg),
    }
}
```

---

## 4. Planification — Windows Task Scheduler

### Principe : la machine ne tourne pas à horaire fixe

Les tâches sont conçues pour **ne pas dépendre d'un horaire garanti** :

| Mécanisme | Rôle |
|---|---|
| `AtStartup + PT3M` | Dump 3 min après le démarrage — laisse la JVM s'initialiser d'abord |
| `RepetitionInterval` | Continue toutes les 2 h pendant que la machine tourne |
| `StartWhenAvailable` | Si une tâche (base, purge, check) a été manquée, elle s'exécute au prochain démarrage |
| `StopAtDurationEnd = false` | La répétition ne s'arrête jamais (pas d'expiration après 24 h) |

**Conséquence concrète :**
- Machine allumée à 09h37 un mercredi → dump immédiat, puis dump à 11h37, 13h37…
- Dimanche passé machine éteinte → base backup s'exécute au prochain démarrage
- Purge non effectuée depuis 5 jours → elle s'exécute au prochain démarrage

---

Le script PowerShell suivant est exécuté **une seule fois** par l'installeur NSIS après installation (voir §5), ou manuellement en tant qu'administrateur.

```powershell
$exe = "$env:ProgramFiles\PharmaSmart\pharmasmart-backup.exe"

# Paramètres communs : rattraper les tâches manquées au démarrage,
# ne pas exiger le réseau, limiter chaque exécution à 1 h.
$settings = New-ScheduledTaskSettingsSet `
  -StartWhenAvailable `
  -RunOnlyIfNetworkAvailable:$false `
  -ExecutionTimeLimit (New-TimeSpan -Hours 1)

# ── DUMP : au démarrage + toutes les 2 h ────────────────────────────────
#
# Deux triggers combinés :
#   1. AtStartup  → un dump immédiat dès que la machine s'allume
#   2. Once + RepetitionInterval → continue toutes les 2 h (sans expiration)
#
# Résultat : au moins un dump par session, quel que soit l'horaire de démarrage.

# Décalage de 3 minutes après le démarrage : laisse la JVM démarrer avant
# de lancer un dump CPU-intensif.
$tBoot = New-ScheduledTaskTrigger -AtStartup
$tBoot.Delay = "PT3M"   # ISO 8601 : 3 minutes

$tRepeat = New-ScheduledTaskTrigger -Once -At (Get-Date -Hour 0 -Minute 0 -Second 0)
$tRepeat.Repetition.Interval          = "PT2H"    # ISO 8601 : toutes les 2 heures
$tRepeat.Repetition.StopAtDurationEnd = $false    # répétition indéfinie, jamais expirée

# Priorité basse : pg_dump cède le CPU aux processus de l'application.
# BELOW_NORMAL (6) = pg_dump n'impacte pas la réactivité de PharmaSmart.
$settings = New-ScheduledTaskSettingsSet `
  -StartWhenAvailable `
  -RunOnlyIfNetworkAvailable:$false `
  -ExecutionTimeLimit (New-TimeSpan -Hours 1) `
  -Priority 6

Register-ScheduledTask -TaskName "PharmaSmart_Backup_Dump" `
  -Action  (New-ScheduledTaskAction -Execute $exe -Argument "dump") `
  -Trigger @($tBoot, $tRepeat) `
  -Settings $settings `
  -RunLevel Highest -User "SYSTEM" -Force

# ── BASE BACKUP hebdomadaire ─────────────────────────────────────────────
#
# Planifié le dimanche à 02h00 mais StartWhenAvailable garantit qu'il
# s'exécutera au prochain démarrage si la machine était éteinte ce jour-là.
# RPO acceptable pour un snapshot hebdomadaire.

Register-ScheduledTask -TaskName "PharmaSmart_Backup_Base" `
  -Action  (New-ScheduledTaskAction -Execute $exe -Argument "base") `
  -Trigger (New-ScheduledTaskTrigger -Weekly -DaysOfWeek Sunday -At "02:00") `
  -Settings $settings `
  -RunLevel Highest -User "SYSTEM" -Force

# ── PURGE hebdomadaire ───────────────────────────────────────────────────
#
# Planifiée le dimanche à 03h00 (après le base backup).
# Si manquée, StartWhenAvailable la déclenche au prochain démarrage.
# La purge n'est pas urgente : l'exécuter un lundi ou mardi ne pose aucun problème.

Register-ScheduledTask -TaskName "PharmaSmart_Backup_Purge" `
  -Action  (New-ScheduledTaskAction -Execute $exe -Argument "purge") `
  -Trigger (New-ScheduledTaskTrigger -Weekly -DaysOfWeek Sunday -At "03:00") `
  -Settings $settings `
  -RunLevel Highest -User "SYSTEM" -Force

# ── VÉRIFICATION hebdomadaire ────────────────────────────────────────────
#
# Vérifie qu'un dump récent existe. Planifiée le lundi à 08h00.
# Avec StartWhenAvailable, s'exécute au premier démarrage de la semaine.

Register-ScheduledTask -TaskName "PharmaSmart_Backup_Check" `
  -Action  (New-ScheduledTaskAction -Execute $exe -Argument "check") `
  -Trigger (New-ScheduledTaskTrigger -Weekly -DaysOfWeek Monday -At "08:00") `
  -Settings $settings `
  -RunLevel Highest -User "SYSTEM" -Force
```

> **Impact sur l'application :**
> - `-Z 6` : compression suffisante (≈ 75 % de réduction), CPU divisé par ~3 vs `-Z 9`
> - Priorité `BELOW_NORMAL` (6) : pg_dump cède le CPU à PharmaSmart sous charge
> - Délai `PT3M` au démarrage : la JVM est opérationnelle avant le premier dump
> - pg_dump est non-bloquant (verrous `ACCESS SHARE` uniquement) — aucune interruption de service

---

## 5. Intégration installeur NSIS

### 5.1 Ajout d'une page de choix du répertoire backup

Dans `installer.nsh`, ajouter les éléments suivants :

```nsis
!include "nsDialogs.nsh"
!include "FileFunc.nsh"

Var BackupDir
Var BackupDirCtrl
Var BackupDirDialog

; ── Page personnalisée : choix du répertoire backup ─────────────────────
Page custom BackupDirPage BackupDirPageLeave

Function BackupDirPage
  ; Valeur par défaut selon le mode d'installation
  ${If} $PS_DataDir == "$PROGRAMDATA\PharmaSmart"
    StrCpy $BackupDir "$PROGRAMDATA\PharmaSmart\backups"
  ${Else}
    StrCpy $BackupDir "$APPDATA\PharmaSmart\backups"
  ${EndIf}

  nsDialogs::Create 1018
  Pop $BackupDirDialog

  ${NSD_CreateLabel} 0 0 100% 24u \
    "Choisissez le dossier parent pour les sauvegardes de la base de donn$\u00e9es :"
  ${NSD_CreateDirRequest} 0 28u 80% 14u $BackupDir
  Pop $BackupDirCtrl

  ${NSD_CreateBrowseButton} 82% 28u 18% 14u "Parcourir..."
  Pop $0
  ${NSD_OnClick} $0 BackupDirBrowse

  nsDialogs::Show
FunctionEnd

Function BackupDirBrowse
  ${NSD_GetText} $BackupDirCtrl $0
  nsDialogs::SelectFolderDialog "S$\u00e9lectionnez le dossier de sauvegarde" $0
  Pop $0
  ${If} $0 != "error"
    ${NSD_SetText} $BackupDirCtrl $0
  ${EndIf}
FunctionEnd

Function BackupDirPageLeave
  ${NSD_GetText} $BackupDirCtrl $BackupDir
FunctionEnd
```

### 5.2 Écriture de la section `backup` dans `config.json`

Dans `CreateConfigFile`, avant `FileClose $3`, ajouter :

```nsis
  ; backup directory (JSON-safe)
  Push "$BackupDir"
  Call EscapeBackslashes
  Pop $R6

  ; ── section backup ────────────────────────────────────────────────────
  FileWrite $3 '  "backup": {$\r$\n'
  FileWrite $3 '    "directory": "$R6",$\r$\n'
  FileWrite $3 '    "db": "pharmasmart",$\r$\n'
  FileWrite $3 '    "host": "localhost",$\r$\n'
  FileWrite $3 '    "port": 5432,$\r$\n'
  FileWrite $3 '    "user": "pharmasmart",$\r$\n'
  FileWrite $3 '    "retention_daily_days": 30,$\r$\n'
  FileWrite $3 '    "retention_base_weeks": 4,$\r$\n'
  FileWrite $3 '    "wal_archiving": false,$\r$\n'
  FileWrite $3 '    "wal_directory": ""$\r$\n'
  FileWrite $3 '  }$\r$\n'
```

### 5.3 Enregistrement des tâches planifiées après installation

À la fin de `!macro customInstall`, appeler le script PowerShell embarqué :

```nsis
!macro customInstall
  Call CreateConfigFile

  ; Créer les répertoires de backup
  CreateDirectory "$BackupDir"
  CreateDirectory "$BackupDir\daily"
  CreateDirectory "$BackupDir\basebackup"
  CreateDirectory "$BackupDir\wal"
  CreateDirectory "$BackupDir\logs"

  ; Enregistrer les tâches planifiées
  ExecWait 'powershell -ExecutionPolicy Bypass -File "$INSTDIR\setup-backup-tasks.ps1"' $0
  ${If} $0 != 0
    DetailPrint "Avertissement : enregistrement des tâches planifiées échoué (code $0)."
  ${EndIf}

  MessageBox MB_OK|MB_ICONINFORMATION \
    "Installation terminée !$\r$\n$\r$\n\
Sauvegardes : $BackupDir$\r$\n\
Les tâches planifiées ont été créées automatiquement."
!macroend
```

---

## 6. Archivage WAL (périodes de garde)

L'archivage WAL est **désactivé par défaut** (`wal_archiving: false`).
L'administrateur l'active manuellement lorsque l'officine est en garde :

### 6.1 Activer

1. Éditer `config.json` → `"wal_archiving": true`
2. Configurer `postgresql.conf` :

```ini
wal_level       = replica
archive_mode    = on
archive_command = 'copy "%p" "<wal_directory>\%f"'
wal_keep_size   = 512MB
```

3. Redémarrer PostgreSQL :

```powershell
Restart-Service postgresql-x64-18
```

4. Vérifier :

```sql
SELECT archived_count, failed_count, last_archived_wal
FROM pg_stat_archiver;
-- failed_count doit rester à 0
```

### 6.2 Désactiver (fin de garde)

Remettre `"wal_archiving": false` dans `config.json` et rétablir `archive_mode = off` dans `postgresql.conf`.

---

## 7. Arborescence et rétention

```
{backup.directory}\          ← choisi à l'installation
├── daily\                   # pg_dump toutes les 2 h — rétention 30 j
│   ├── pharmasmart_20250417_080012.dump
│   └── pharmasmart_20250417_100015.dump
├── basebackup\              # pg_basebackup — rétention 4 semaines
│   └── base_20250413\
├── wal\                     # WAL archivés — rétention 7 j (si garde)
└── logs\
    └── backup.log
```

---

## 8. Procédure de restauration

### Restauration complète

```powershell
$pgBin = pharmasmart-backup check  # révèle le bin via le même mécanisme de découverte
# Ou manuellement :
$pgBin = "C:\Program Files\PostgreSQL\18\bin"

$env:PGPASSWORD = "<mot_de_passe>"

& "$pgBin\createdb.exe" -h localhost -U postgres -T template0 pharmasmart_restored
& "$pgBin\pg_restore.exe" `
    -h localhost -U postgres `
    -d pharmasmart_restored `
    -F c -j 4 `
    "{backup.directory}\daily\pharmasmart_20250417_100015.dump"
```

### Restauration partielle (table)

```powershell
& "$pgBin\pg_restore.exe" -d pharmasmart -t sales `
    "{backup.directory}\daily\pharmasmart_20250417_100015.dump"
```

### PITR (si WAL archivage était actif)

```powershell
# 1. Arrêter PostgreSQL
Stop-Service postgresql-x64-18

# 2. Restaurer le base backup
tar -xzf "{backup.directory}\basebackup\base_20250414\base.tar.gz" `
    -C "C:\Program Files\PostgreSQL\18\data"

# 3. Paramétrer la recovery
Add-Content "C:\Program Files\PostgreSQL\18\data\postgresql.conf" @"
restore_command        = 'copy "{backup.directory}\wal\%f" "%p"'
recovery_target_time   = '2025-04-17 09:30:00'
recovery_target_action = 'promote'
"@

# 4. Redémarrer
Start-Service postgresql-x64-18
```

---

## 9. Sécurité

| Mesure | Implémentation |
|---|---|
| Mot de passe | Variable d'environnement `PGPASSWORD` définie dans la tâche planifiée (Properties → Run As, ou via `cmdkey`) |
| Alternative | `%APPDATA%\postgresql\pgpass.conf` — `pharmasmart-backup.exe` n'a pas besoin de lire le mot de passe |
| Chiffrement | BitLocker sur le volume hébergeant `{backup.directory}` |
| Copie externe | `robocopy` vers NAS — ajouter une 5ᵉ tâche planifiée quotidienne |
| Accès | `{backup.directory}` accessible uniquement au compte SYSTEM et à l'administrateur local |

### pgpass.conf (recommandé)

```
# %APPDATA%\postgresql\pgpass.conf
localhost:5432:pharmasmart:pharmasmart:<mot_de_passe>
localhost:5432:*:postgres:<mot_de_passe_admin>
```

---

## 10. Checklist de validation

- [ ] `pharmasmart-backup.exe dump` s'exécute sans erreur (test manuel)
- [ ] Tâches planifiées visibles dans le Task Scheduler (`PharmaSmart_Backup_*`)
- [ ] `{backup.directory}\logs\backup.log` horodaté et sans erreur
- [ ] Base backup hebdomadaire présent dans `basebackup/`
- [ ] Purge automatique : fichiers > 30 j supprimés
- [ ] Test de restauration partielle effectué mensuellement
- [ ] Si garde activée : `pg_stat_archiver.failed_count = 0`
- [ ] Copie externe (NAS) vérifiée hebdomadairement
