# Ressources Backup (bundle Tauri)

Ce dossier est **embarqué** dans l'installeur via `tauri.conf.json → bundle.resources`.
Son contenu finit à `$INSTDIR\resources\backup\` sur la machine cible.

## Contenu attendu à la **release**

| Fichier | Source | Obligatoire |
|---|---|---|
| `setup-backup-tasks.ps1`  | copie de `src-backup/scripts/setup-backup-tasks.ps1`  | oui |
| `remove-backup-tasks.ps1` | copie de `src-backup/scripts/remove-backup-tasks.ps1` | oui |
| `pharmasmart-backup.exe`  | build de `src-backup/` (`cargo build --release`)      | oui |

## Préparation avant bundle Tauri

```bash
# 1. Compiler le binaire Rust (Release)
cd src-backup
cargo build --release

# 2. Copier le binaire dans ce dossier
cp target/release/pharmasmart-backup.exe ../src-tauri/backup/
```

(Le script npm `tauri:prepare-backup` automatise ces deux étapes — voir `package.json`.)

## Rôle à l'installation

L'installeur NSIS (`src-tauri/installer-hooks/installer.nsh`) :

1. crée l'arborescence de sauvegarde (`{backup.directory}/daily`, `basebackup`, `wal`, `logs`) ;
2. invoque `setup-backup-tasks.ps1 -ExePath "$INSTDIR\resources\backup\pharmasmart-backup.exe"`
   pour enregistrer les 4 tâches planifiées `PharmaSmart_Backup_*` ;
3. appelle `remove-backup-tasks.ps1` à la désinstallation.

Voir [`docs/BACKUP-STRATEGY.md`](../../docs/BACKUP-STRATEGY.md) pour la stratégie complète.
