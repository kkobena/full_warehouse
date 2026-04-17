# pharmasmart-backup

Outil de sauvegarde autonome de la base PostgreSQL de **PharmaSmart**.
Binaire standalone en Rust — aucune dépendance sur la JVM, zéro intégration dans l'application Spring Boot.

> Spécification complète : [`../docs/BACKUP-STRATEGY.md`](../docs/BACKUP-STRATEGY.md)

---

## Table des matières

- [Architecture](#architecture)
- [Prérequis](#prérequis)
- [Utilisateur PostgreSQL dédié](#utilisateur-postgresql-dédié)
- [Configuration](#configuration)
- [Build — Développement](#build--développement)
- [Build — Release](#build--release)
- [Bundle avec Tauri](#bundle-avec-tauri)
- [Sous-commandes](#sous-commandes)
- [Planification (Task Scheduler)](#planification-task-scheduler)
- [Utilisation manuelle](#utilisation-manuelle)
- [Logs](#logs)
- [Restauration](#restauration)
- [Dépannage](#dépannage)

---

## Architecture

```
src-backup/
├── Cargo.toml
├── scripts/
│   ├── setup-backup-tasks.ps1   # enregistre les 4 tâches planifiées
│   └── remove-backup-tasks.ps1  # les supprime (utilisé à la désinstallation)
└── src/
    ├── main.rs          # CLI (clap) — dispatch dump|base|purge|check
    ├── config.rs        # lecture config.json + résolution des chemins
    ├── pg_discover.rs   # détection dynamique de PostgreSQL (registre + PATH)
    ├── dump.rs          # pg_dump logique
    ├── base.rs          # pg_basebackup physique
    ├── purge.rs         # rotation / suppression
    ├── check.rs         # vérifie qu'un dump < 3 h existe
    └── logger.rs        # écriture horodatée dans backup.log
```

| Commande | Fréquence recommandée | Rôle |
|---|---|---|
| `dump`  | Toutes les 2 h (et au démarrage) | `pg_dump` compressé (.dump, format custom) |
| `base`  | Hebdomadaire (dimanche 02 h 00)  | `pg_basebackup` tar+gzip pour PITR |
| `purge` | Hebdomadaire (dimanche 03 h 00)  | Rotation selon rétention |
| `check` | Hebdomadaire (lundi 08 h 00)     | Alerte si aucun dump récent |

---

## Utilisateur PostgreSQL dédié

Le backup utilise un compte PostgreSQL **`pharmasmart_backup`** distinct de l'utilisateur applicatif.
Ce compte a uniquement les droits nécessaires : `REPLICATION` (pour `pg_basebackup`) et `SELECT` sur la base (pour `pg_dump`).

> **Extensions, fonctions et routines** (`pg_trgm`, procédures stockées, triggers…) sont lus depuis les tables système `pg_catalog` qui sont lisibles par tout utilisateur authentifié. Aucun grant supplémentaire n'est nécessaire pour que `pg_dump` les inclue dans le dump.

### 1. Création du compte

Se connecter en tant que `postgres` (superuser) :

```powershell
$pgBin = "C:\Program Files\PostgreSQL\18\bin"
$env:PGPASSWORD = "<mot_de_passe_postgres>"
& "$pgBin\psql.exe" -h localhost -U postgres
```

Puis dans `psql` :

```sql
-- Créer l'utilisateur avec le privilège REPLICATION (pas SUPERUSER)
CREATE USER pharmasmart_backup
    WITH LOGIN
         REPLICATION
         PASSWORD '<mot_de_passe_backup>';
```

### 2. Droits sur la base (pour pg_dump)

Le rôle `pg_read_all_data` (disponible depuis PostgreSQL 14) couvre toutes les tables, vues et séquences de tous les schémas, présents et futurs :

```sql
GRANT CONNECT ON DATABASE pharmasmart TO pharmasmart_backup;
GRANT pg_read_all_data          TO pharmasmart_backup;
GRANT USAGE ON SCHEMA pharmasmart TO pharmasmart_backup;
```

### 3. Autoriser la connexion de réplication dans pg_hba.conf

Ouvrir `C:\Program Files\PostgreSQL\18\data\pg_hba.conf` et ajouter **avant** la ligne `host all all ...` :

```
# pharmasmart-backup : connexion logique (pg_dump)
host    pharmasmart     pharmasmart_backup  127.0.0.1/32    scram-sha-256
# pharmasmart-backup : réplication physique (pg_basebackup)
host    replication     pharmasmart_backup  127.0.0.1/32    scram-sha-256
```

Recharger PostgreSQL pour appliquer :

```powershell
& "$pgBin\pg_ctl.exe" reload -D "C:\Program Files\PostgreSQL\18\data"
# ou via les services Windows :
Restart-Service postgresql-x64-18
```

### 4. Vérifier les droits

```powershell
$env:PGPASSWORD = "<mot_de_passe_backup>"
# Test connexion logique
& "$pgBin\psql.exe" -h localhost -U pharmasmart_backup -d pharmasmart -c "\dt warehouse.*"
# Test connexion réplication
& "$pgBin\pg_basebackup.exe" -h localhost -U pharmasmart_backup --list
```

### 5. pgpass.conf pour le compte SYSTEM

Les tâches planifiées s'exécutent sous le compte **SYSTEM**. Créer le fichier :

```
C:\Windows\System32\config\systemprofile\AppData\Roaming\postgresql\pgpass.conf
```

Contenu :

```
# hôte:port:base:user:mot_de_passe
localhost:5432:pharmasmart:pharmasmart_backup:<mot_de_passe_backup>
localhost:5432:replication:pharmasmart_backup:<mot_de_passe_backup>
```

Pour les exécutions **manuelles** (terminal utilisateur), créer également :

```
%APPDATA%\postgresql\pgpass.conf
```

Avec le même contenu. Restreindre l'accès en lecture :

```powershell
icacls "$env:APPDATA\postgresql\pgpass.conf" /inheritance:r /grant:r "$env:USERNAME:(R)"
```

---

## Prérequis

| Outil | Version | Installation |
|---|---|---|
| Rust (toolchain `stable`) | 1.85+ (édition 2024) | `winget install Rustlang.Rustup` puis `rustup default stable` |
| PostgreSQL client tools (`pg_dump.exe`, `pg_basebackup.exe`) | 13 – 18 | Installé par le serveur PostgreSQL |

Vérifier l'installation :

```powershell
rustc --version
cargo --version
```

---

## Configuration

Le binaire lit `config.json` dans l'ordre suivant :

1. `%PROGRAMDATA%\PharmaSmart\config.json`  *(installation all-users)*
2. `%APPDATA%\PharmaSmart\config.json`       *(installation mono-utilisateur)*
3. `<dossier du binaire>\config.json`        *(fallback développement)*

Section attendue :

```json
{
  "backup": {
    "directory": "C:\\ProgramData\\PharmaSmart\\backups",
    "db": "pharmasmart",
    "host": "localhost",
    "port": 5432,
    "user": "pharmasmart_backup",
    "retention_daily_days": 30,
    "retention_base_weeks": 4,
    "wal_archiving": false,
    "wal_directory": ""
  }
}
```

Le mot de passe PostgreSQL est fourni **soit** par la variable d'environnement `PGPASSWORD`, **soit** par `%APPDATA%\postgresql\pgpass.conf` (recommandé, non versionné).

### `pgpass.conf`

Voir la section [Utilisateur PostgreSQL dédié § 5](#5-pgpassconf-pour-le-compte-system) pour la configuration complète.

Droits d'accès : lisible uniquement par le compte SYSTEM (ou l'utilisateur exécutant la tâche planifiée).

---

## Build — Développement

```bash
cd src-backup

# Vérification rapide (sans compiler le binaire)
cargo check

# Build debug
cargo build

# Lancer directement (utilise ./target/debug/pharmasmart-backup.exe)
cargo run -- --help
cargo run -- dump

# Tests (si ajoutés plus tard)
cargo test
```

Le binaire debug apparaît dans `src-backup/target/debug/pharmasmart-backup.exe`.

---

## Build — Release

Build optimisé (LTO, `opt-level = "s"`, `strip = true`) :

```bash
cd src-backup
cargo build --release
```

Artefact : `src-backup/target/release/pharmasmart-backup.exe` (~ 2 Mo).

### Vérification après build

```powershell
# Versions / aide
.\target\release\pharmasmart-backup.exe --version
.\target\release\pharmasmart-backup.exe --help

# Test d'un dump (nécessite config.json + PGPASSWORD)
$env:PGPASSWORD = "<mot_de_passe>"
.\target\release\pharmasmart-backup.exe dump
```

### Nettoyage

```bash
cargo clean                       # supprime target/
```

---

## Bundle avec Tauri

Le binaire est embarqué dans l'installeur Tauri via `src-tauri/backup/`.

### Préparation automatique (recommandé)

Depuis la racine du projet :

```bash
npm run tauri:prepare-backup
```

Ce script (`scripts/prepare-backup.js`) :

1. exécute `cargo build --release` dans `src-backup/` ;
2. copie `target/release/pharmasmart-backup.exe` vers `src-tauri/backup/` ;
3. copie les deux scripts PowerShell (`setup-backup-tasks.ps1`, `remove-backup-tasks.ps1`) vers `src-tauri/backup/`.

### Préparation manuelle

```bash
cd src-backup
cargo build --release
cp target/release/pharmasmart-backup.exe ../src-tauri/backup/
cp scripts/setup-backup-tasks.ps1       ../src-tauri/backup/
cp scripts/remove-backup-tasks.ps1      ../src-tauri/backup/
```

### Build complet Tauri

Après préparation, depuis la racine :

```bash
# Build standard (backend externe)
npm run tauri:prepare-backup
npm run tauri:build

# Build avec backend embarqué
npm run tauri:prepare-sidecar
npm run tauri:prepare-backup
npm run tauri:build:bundled-jre
```

L'installeur NSIS généré :

- écrit la section `"backup"` dans `config.json` (défaut : `$PS_DataDir\backups`) ;
- crée l'arborescence `daily/`, `basebackup/`, `wal/`, `logs/` ;
- enregistre les 4 tâches planifiées via `setup-backup-tasks.ps1` ;
- nettoie les tâches à la désinstallation via `remove-backup-tasks.ps1`.

---

## Sous-commandes

```text
pharmasmart-backup <COMMAND>

  dump    pg_dump logique (à planifier toutes les 2 h)
  base    pg_basebackup physique (hebdomadaire)
  purge   Rotation/purge des anciens fichiers
  check   Vérifie la présence d'un dump récent (< 3 h)
```

### `dump`

- Crée `{backup.directory}/daily/pharmasmart_YYYYMMDD_HHMMSS.dump`
- Format custom, compression `-Z 6` (~ 75 % de réduction, CPU modéré)
- Verrous `ACCESS SHARE` uniquement → aucune interruption de service

### `base`

- Crée `{backup.directory}/basebackup/base_YYYYMMDD/` (tar.gz + WAL en streaming `-Xs`)
- Utilisable pour une restauration complète (PITR si WAL archivage actif)

### `purge`

- Supprime les dumps > `retention_daily_days` (défaut : 30 j)
- Supprime les base backups > `retention_base_weeks` (défaut : 4 sem)
- Si `wal_archiving = true` : supprime les WAL > 7 j

### `check`

- Renvoie **code 0** si un `*.dump` < 3 h existe dans `daily/`
- Renvoie **code ≠ 0** sinon — idéal pour un monitoring (intégration avec Task Scheduler → email en cas d'échec)

Chaque commande trace son résultat horodaté dans `{backup.directory}/logs/backup.log`.

---

## Planification (Task Scheduler)

### Enregistrement (automatique via l'installeur)

L'installeur NSIS appelle déjà :

```powershell
powershell -ExecutionPolicy Bypass `
  -File "$INSTDIR\resources\backup\setup-backup-tasks.ps1" `
  -ExePath "$INSTDIR\resources\backup\pharmasmart-backup.exe"
```

### Enregistrement manuel (dev / réinstallation)

Depuis un terminal **Administrateur** :

```powershell
cd src-backup\scripts
powershell -ExecutionPolicy Bypass -File .\setup-backup-tasks.ps1 `
  -ExePath "C:\chemin\absolu\vers\pharmasmart-backup.exe"
```

Tâches créées :

| Nom | Déclencheur | Détail |
|---|---|---|
| `PharmaSmart_Backup_Dump`  | `AtStartup` + `PT3M` **et** répétition `PT2H` | SYSTEM, priorité 6 (BELOW_NORMAL) |
| `PharmaSmart_Backup_Base`  | Dimanche 02 h 00 (`StartWhenAvailable`) | SYSTEM |
| `PharmaSmart_Backup_Purge` | Dimanche 03 h 00 (`StartWhenAvailable`) | SYSTEM |
| `PharmaSmart_Backup_Check` | Lundi 08 h 00 (`StartWhenAvailable`)    | SYSTEM |

Vérification :

```powershell
Get-ScheduledTask -TaskName "PharmaSmart_Backup_*" | Format-Table TaskName, State, LastRunTime, NextRunTime
```

### Suppression

```powershell
cd src-backup\scripts
powershell -ExecutionPolicy Bypass -File .\remove-backup-tasks.ps1
```

---

## Utilisation manuelle

Déclencher une commande à la demande (le mot de passe doit être disponible via `PGPASSWORD` ou `pgpass.conf`) :

```powershell
# Dans un terminal Administrateur, avec PostgreSQL accessible
$env:PGPASSWORD = "<mot_de_passe>"

# Dump logique immédiat
pharmasmart-backup.exe dump

# Base backup physique immédiat
pharmasmart-backup.exe base

# Purge immédiate
pharmasmart-backup.exe purge

# Vérification
pharmasmart-backup.exe check ; echo "code = $LASTEXITCODE"
```

Via le Task Scheduler :

```powershell
Start-ScheduledTask -TaskName "PharmaSmart_Backup_Dump"
```

---

## Logs

Format horodaté, un fichier unique :

```
{backup.directory}\logs\backup.log
```

Exemple :

```
2026-04-17 10:00:12 [OK] dump → C:\ProgramData\PharmaSmart\backups\daily\pharmasmart_20260417_100012.dump (84219011 octets)
2026-04-17 12:00:15 [OK] dump → C:\ProgramData\PharmaSmart\backups\daily\pharmasmart_20260417_120015.dump (84332180 octets)
```

Pour un log plus verbeux en exécution manuelle :

```powershell
$env:RUST_LOG = "debug"
pharmasmart-backup.exe dump
```

---

## Restauration

### Complète (dump logique)

```powershell
$pgBin = "C:\Program Files\PostgreSQL\18\bin"
$env:PGPASSWORD = "<mot_de_passe_admin>"

& "$pgBin\createdb.exe"   -h localhost -U postgres -T template0 pharmasmart_restored
& "$pgBin\pg_restore.exe" -h localhost -U postgres -d pharmasmart_restored -F c -j 4 `
    "C:\ProgramData\PharmaSmart\backups\daily\pharmasmart_20260417_100012.dump"
```

### Partielle (une table)

```powershell
& "$pgBin\pg_restore.exe" -d pharmasmart -t sales `
    "C:\ProgramData\PharmaSmart\backups\daily\pharmasmart_20260417_100012.dump"
```

### PITR (si WAL archivage actif)

Voir [`docs/BACKUP-STRATEGY.md § 8`](../docs/BACKUP-STRATEGY.md#8-procédure-de-restauration).

---

## Dépannage

| Symptôme | Cause probable | Solution |
|---|---|---|
| `config.json introuvable` | Aucune section `backup` dans le JSON | Ajouter la section (voir [Configuration](#configuration)) |
| `Impossible de localiser le répertoire bin/ de PostgreSQL` | PostgreSQL non détecté | Définir `PGBIN` : `$env:PGBIN = "C:\Program Files\PostgreSQL\18\bin"` |
| `pg_dump: error: connection ... password authentication failed` | Mot de passe manquant | Définir `PGPASSWORD` ou configurer `pgpass.conf` |
| `pg_dump` OK en CLI mais la tâche planifiée échoue | Compte SYSTEM ne trouve pas le mot de passe | Mettre `pgpass.conf` dans `C:\Windows\System32\config\systemprofile\AppData\Roaming\postgresql\` |
| Dumps présents mais jamais purgés | Tâche `Purge` jamais exécutée | `Start-ScheduledTask -TaskName PharmaSmart_Backup_Purge` |
| `check` renvoie une erreur | Aucun dump récent → surveillance OK | Relancer manuellement un `dump`, puis investiguer les logs |

### Activer le WAL archivage (périodes de garde)

Voir [`docs/BACKUP-STRATEGY.md § 6`](../docs/BACKUP-STRATEGY.md#6-archivage-wal-périodes-de-garde).

---

## Licence

Propriétaire — usage interne PharmaSmart.
