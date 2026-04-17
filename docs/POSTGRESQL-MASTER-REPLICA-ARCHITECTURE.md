# Architecture PostgreSQL 18 Master / Replica — Pharma-Smart (Windows)

> **Objectif :** Haute disponibilité et sauvegarde à chaud pour Pharma-Smart avec un replica
> physique en streaming sur une seconde machine Windows du réseau local.
> Cible : **PostgreSQL 18** sur **Windows 11 / Windows Server 2025**.

---

## Table des matières

1. [Vue d'ensemble](#1-vue-densemble)
2. [Nouveautés PostgreSQL 18 utiles pour la réplication](#2-nouveautés-postgresql-18-utiles-pour-la-réplication)
3. [Prérequis Windows](#3-prérequis-windows)
4. [Conventions chemins Windows](#4-conventions-chemins-windows)
5. [Configuration du serveur Primary (Machine A)](#5-configuration-du-serveur-primary-machine-a)
6. [Bootstrapping du Replica — pg\_basebackup](#6-bootstrapping-du-replica--pg_basebackup)
7. [Configuration du serveur Replica (Machine B)](#7-configuration-du-serveur-replica-machine-b)
8. [Vérification de la réplication](#8-vérification-de-la-réplication)
9. [Intégration avec Pharma-Smart (Spring Boot)](#9-intégration-avec-pharma-smart-spring-boot)
10. [Monitoring](#10-monitoring)
11. [Failover manuel](#11-failover-manuel)
12. [Failover automatique avec Patroni sur Windows](#12-failover-automatique-avec-patroni-sur-windows)
13. [Slot de réplication vs WAL archivage](#13-slot-de-réplication-vs-wal-archivage)
14. [Checklist de mise en production](#14-checklist-de-mise-en-production)
15. [Risques et limites Windows](#15-risques-et-limites-windows)

---

## 1. Vue d'ensemble

```
┌──────────────────────────────────────────────────────────────────────┐
│                        RÉSEAU LOCAL (LAN)                            │
│                                                                      │
│  ┌────────────────────────────┐    WAL streaming TCP:5432           │
│  │  MACHINE A — Primary       │ ─────────────────────────────────►  │
│  │  Windows 11 / Server 2025  │                                      │
│  │  PostgreSQL 18             │  ◄──── confirmation réplication ──  │
│  │  IP : 192.168.1.10         │                                      │
│  │  Service : pgsql-18        │  ┌───────────────────────────────┐  │
│  │                            │  │  MACHINE B — Replica          │  │
│  │  Base : pharma_smart       │  │  Windows 11 / Server 2025     │  │
│  │  Schéma : warehouse        │  │  PostgreSQL 18                │  │
│  └────────────────────────────┘  │  IP : 192.168.1.11            │  │
│            ▲                     │  Lecture seule (hot standby)  │  │
│            │ R/W                 └───────────────────────────────┘  │
│  ┌─────────┴──────────────────┐                                      │
│  │  Spring Boot Pharma-Smart  │                                      │
│  │  port 8080 — HikariCP      │                                      │
│  └────────────────────────────┘                                      │
└──────────────────────────────────────────────────────────────────────┘
```

| Machine | Rôle | Accès | Usage |
|---------|------|-------|-------|
| Machine A `192.168.1.10` | **Primary** | Lecture + Écriture | Application Pharma-Smart |
| Machine B `192.168.1.11` | **Replica (Hot Standby)** | Lecture seule | Backups, rapports, failover |

---

## 2. Nouveautés PostgreSQL 18 utiles pour la réplication

| Fonctionnalité PG 18 | Bénéfice pour Pharma-Smart |
|---|---|
| **WAL summarizer** intégré | Incrémental backup natif via `pg_basebackup --incremental` — sans outil tiers |
| **Streaming WAL compression** | Réduction bande passante réseau LAN (zstd/lz4) |
| **`pg_createsubscriber`** | Conversion d'un Replica physique en abonné logique en une commande |
| **OAuth 2.0 device flow** | Authentification améliorée (non critique pour la réplication) |
| **`pg_stat_replication_slots`** enrichi | Monitoring des slots plus précis |
| **Vacuum improvements** | Moins de conflits Replica pendant VACUUM sur le Primary |

> PostgreSQL 18 maintient la compatibilité totale avec PG 17 pour la réplication physique.

---

## 3. Prérequis Windows

### Installation PostgreSQL 18 sur les deux machines

Télécharger l'installeur EDB sur les deux machines :
`https://www.enterprisedb.com/downloads/postgres-postgresql-downloads`

```
Sélectionner lors de l'installation :
  ✅ PostgreSQL Server
  ✅ Command Line Tools
  ✅ pgAdmin 4 (optionnel, Machine A seulement)
  Répertoire de données : C:\PostgreSQL\18\data   (personnalisé, voir section 4)
```

**Vérification sur les deux machines (PowerShell admin) :**

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" --version
# PostgreSQL 18.x
```

### Pare-feu Windows — Machine A (Primary)

```powershell
# Ouvrir le port 5432 en entrée depuis la Machine B uniquement
New-NetFirewallRule `
  -DisplayName "PostgreSQL 18 Replication - Replica B" `
  -Direction Inbound `
  -Protocol TCP `
  -LocalPort 5432 `
  -RemoteAddress 192.168.1.11 `
  -Action Allow `
  -Profile Domain,Private
```

### Nom du service Windows PostgreSQL 18

```powershell
# Vérifier le nom réel du service (varie selon l'installeur EDB)
Get-Service | Where-Object { $_.DisplayName -like "*postgresql*" }
# Typiquement : postgresql-x64-18
```

---

## 4. Conventions chemins Windows

| Élément | Chemin recommandé |
|---------|-------------------|
| Binaires PostgreSQL 18 | `C:\Program Files\PostgreSQL\18\bin\` |
| Données Primary (PGDATA) | `C:\PostgreSQL\18\data\` |
| Données Replica (PGDATA) | `C:\PostgreSQL\18\data\` (Machine B) |
| Archive WAL | `C:\PostgreSQL\wal_archive\` |
| Logs PostgreSQL | `C:\PostgreSQL\18\logs\` |
| Backups pg_basebackup | `D:\Backups\pg_basebackup\` |

> **Conseil :** Mettre `PGDATA` sur un disque séparé du système (ex. `D:\`) en production
> pour isoler les I/O base de données du système Windows.

Ajouter les binaires au PATH (PowerShell admin, permanent) :

```powershell
[System.Environment]::SetEnvironmentVariable(
  "PATH",
  "$env:PATH;C:\Program Files\PostgreSQL\18\bin",
  "Machine"
)
```

---

## 5. Configuration du serveur Primary (Machine A)

### 5.1 `postgresql.conf`

Fichier : `C:\PostgreSQL\18\data\postgresql.conf`

```ini
#----- Réseau -----
listen_addresses = '*'              # écoute sur toutes les interfaces
port = 5432

#----- Réplication -----
wal_level = replica                 # niveau minimum pour le streaming
max_wal_senders = 5                 # connexions Replica simultanées max
wal_keep_size = 512MB               # WAL conservés pour rattrapage

# Compression WAL (nouveau PG 18 — réduit bande passante)
wal_compression = zstd

# Réplication synchrone (optionnel, RPO=0)
# synchronous_standby_names = 'replica1'
# synchronous_commit = remote_write

#----- Archivage WAL -----
archive_mode = on
archive_command = 'copy "%p" "C:\\PostgreSQL\\wal_archive\\%f"'
archive_timeout = 300               # force un segment WAL toutes les 5 min

#----- Logs -----
log_destination = 'stderr'
logging_collector = on
log_directory = 'C:\\PostgreSQL\\18\\logs'
log_filename = 'postgresql-%Y-%m-%d.log'
log_rotation_age = 1d
log_replication_commands = on       # trace les commandes de réplication
```

### 5.2 `pg_hba.conf`

Fichier : `C:\PostgreSQL\18\data\pg_hba.conf`

```
# TYPE  DATABASE     USER                      ADDRESS              METHOD
# Connexions locales
host    all          all                       127.0.0.1/32         scram-sha-256
host    all          all                       ::1/128              scram-sha-256

# Réplication depuis Machine B
host    replication  pharma_smart_backup       192.168.1.11/32      scram-sha-256

# Application Pharma-Smart (si Spring Boot sur la même machine)
host    pharma_smart pharma_smart              127.0.0.1/32         scram-sha-256
```

### 5.3 Créer l'utilisateur de réplication

```sql
-- Se connecter en tant que postgres
CREATE USER pharma_smart_backup
  WITH LOGIN REPLICATION PASSWORD 'backup_password_fort';
```

### 5.4 Redémarrer le service Primary

```powershell
# Redémarrage du service PostgreSQL 18
Restart-Service -Name "postgresql-x64-18"

# Vérifier l'état
Get-Service -Name "postgresql-x64-18"
# Status : Running
```

---

## 6. Bootstrapping du Replica — pg\_basebackup

> Exécuter ces commandes **sur la Machine B (Replica)**, en PowerShell administrateur.

```powershell
# 1. Arrêter PostgreSQL sur le Replica
Stop-Service -Name "postgresql-x64-18" -Force

# 2. Vider le répertoire PGDATA du Replica
Remove-Item -Recurse -Force "C:\PostgreSQL\18\data\*"

# 3. Copie initiale depuis le Primary avec pg_basebackup
#    -R : génère automatiquement standby.signal + postgresql.auto.conf
#    -Xs : inclut les WAL produits pendant la copie (mode streaming)
#    -P  : affiche la progression
#    --checkpoint=fast : évite d'attendre le prochain checkpoint automatique
& "C:\Program Files\PostgreSQL\18\bin\pg_basebackup.exe" `
    -h 192.168.1.10 `
    -p 5432 `
    -U pharma_smart_backup `
    -D "C:\PostgreSQL\18\data" `
    -P `
    -Xs `
    -R `
    --checkpoint=fast `
    --wal-compress=zstd

# PG 18 : backup incrémental (après un premier backup complet avec --create-slot)
# & pg_basebackup.exe ... --incremental="C:\Backups\pg_basebackup\backup_manifest"
```

L'option `-R` génère automatiquement :
- `C:\PostgreSQL\18\data\standby.signal` → indique au Replica son rôle
- Entrée dans `C:\PostgreSQL\18\data\postgresql.auto.conf` :

```ini
primary_conninfo = 'host=192.168.1.10 port=5432 user=pharma_smart_backup password=backup_password_fort sslmode=prefer'
```

---

## 7. Configuration du serveur Replica (Machine B)

### 7.1 `postgresql.conf` (Machine B)

Fichier : `C:\PostgreSQL\18\data\postgresql.conf`

```ini
#----- Réseau -----
listen_addresses = '*'
port = 5432

#----- Mode Standby -----
hot_standby = on                    # autorise SELECT en lecture seule
hot_standby_feedback = on           # évite les conflits de vacuum Primary/Replica
wal_receiver_status_interval = 10s  # fréquence des rapports de statut au Primary

# Délai de réplication (optionnel — utile pour récupérer des suppressions accidentelles)
# recovery_min_apply_delay = 30min

#----- Logs -----
logging_collector = on
log_directory = 'C:\\PostgreSQL\\18\\logs'
log_filename = 'postgresql-%Y-%m-%d.log'
```

### 7.2 Démarrer le Replica

```powershell
# Démarrer le service Replica
Start-Service -Name "postgresql-x64-18"

# Suivre les logs en temps réel
Get-Content "C:\PostgreSQL\18\logs\postgresql-$(Get-Date -Format 'yyyy-MM-dd').log" -Wait -Tail 30
# Attendu : "started streaming WAL from primary at ..."
```

### 7.3 Configurer le service Windows en démarrage automatique

```powershell
Set-Service -Name "postgresql-x64-18" -StartupType Automatic
```

---

## 8. Vérification de la réplication

### Sur le Primary (Machine A)

```sql
-- État des connexions Replica
SELECT
    client_addr,
    application_name,
    state,
    sent_lsn,
    replay_lsn,
    pg_size_pretty(pg_wal_lsn_diff(sent_lsn, replay_lsn)) AS lag_bytes,
    write_lag,
    flush_lag,
    replay_lag,
    sync_state
FROM pg_stat_replication;
```

Sortie attendue :

```
client_addr   | state     | lag_bytes | replay_lag | sync_state
192.168.1.11  | streaming | 0 bytes   | 00:00:00   | async
```

### Sur le Replica (Machine B)

```sql
-- Confirmer le mode standby
SELECT pg_is_in_recovery();
-- → TRUE

-- Décalage avec le Primary
SELECT now() - pg_last_xact_replay_timestamp() AS replication_delay;
-- → quelques millisecondes sur LAN

-- Informations détaillées du receiver (PG 18)
SELECT * FROM pg_stat_wal_receiver;
```

### Test fonctionnel

```sql
-- Sur le Primary : créer une table de test temporaire
CREATE TABLE warehouse.replication_check AS
SELECT now() AS ts, 'ping' AS msg;

-- Sur le Replica (quelques ms après) :
SELECT * FROM warehouse.replication_check;
-- → doit retourner la ligne

-- Nettoyage (Primary)
DROP TABLE warehouse.replication_check;
```

---

## 9. Intégration avec Pharma-Smart (Spring Boot)

### Option A — Primary uniquement (actuel, minimal)

Aucune modification applicative. Le Replica sert aux backups et au failover.

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://192.168.1.10:5432/pharma_smart
    username: pharma_smart
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

### Option B — Routage lecture/écriture (évolution recommandée)

Les services annotés `@Transactional(readOnly = true)` (rapports, listes AG Grid)
sont automatiquement routés vers le Replica.

```java
// ReplicationRoutingDataSource.java
public class ReplicationRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly()
            ? "replica" : "primary";
    }
}
```

```java
// DataSourceConfig.java
@Configuration
public class DataSourceConfig {

    @Bean("primaryDataSource")
    @ConfigurationProperties("spring.datasource.primary")
    DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean("replicaDataSource")
    @ConfigurationProperties("spring.datasource.replica")
    DataSource replicaDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean
    DataSource dataSource(
        @Qualifier("primaryDataSource") DataSource primary,
        @Qualifier("replicaDataSource") DataSource replica
    ) {
        var ds = new ReplicationRoutingDataSource();
        ds.setTargetDataSources(Map.of("primary", primary, "replica", replica));
        ds.setDefaultTargetDataSource(primary);
        ds.afterPropertiesSet();
        return ds;
    }
}
```

```yaml
# application.yml
spring:
  datasource:
    primary:
      url: jdbc:postgresql://192.168.1.10:5432/pharma_smart
      username: pharma_smart
      password: ${DB_PASSWORD}
    replica:
      url: jdbc:postgresql://192.168.1.11:5432/pharma_smart
      username: pharma_smart
      password: ${DB_PASSWORD}
```

### Option C — HAProxy sur Windows (production)

Installer HAProxy pour Windows (binaire disponible sur `haproxy.org`) :

```
# haproxy.cfg
frontend pg_frontend
    bind *:5433
    default_backend pg_primary

backend pg_primary
    option tcp-check
    server primary 192.168.1.10:5432 check port 5432
    server replica 192.168.1.11:5432 check port 5432 backup

frontend pg_readonly
    bind *:5434
    default_backend pg_replicas

backend pg_replicas
    balance roundrobin
    server replica 192.168.1.11:5432 check
```

L'application se connecte à `localhost:5433` (R/W) et `localhost:5434` (lecture).
La bascule en cas de failover est transparente.

---

## 10. Monitoring

### Requêtes de surveillance (à lancer sur le Primary)

```sql
-- Lag de réplication en octets et en temps
SELECT
    application_name,
    client_addr,
    state,
    pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), replay_lsn)) AS total_lag,
    write_lag,
    flush_lag,
    replay_lag
FROM pg_stat_replication;

-- Espace WAL sur disque
SELECT pg_size_pretty(sum(size)) AS wal_size
FROM pg_ls_waldir();

-- Slots de réplication (PG 18 — vue enrichie)
SELECT slot_name, active, wal_status,
       pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn)) AS retained_wal
FROM pg_replication_slots;
```

### Script PowerShell de monitoring — à planifier dans le Planificateur de tâches

```powershell
# check_replication.ps1
$psql = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
$connStr = "host=localhost port=5432 user=postgres dbname=postgres"

$lag = & $psql $connStr -t -A -c @"
    SELECT COALESCE(EXTRACT(EPOCH FROM replay_lag)::int, -1)
    FROM pg_stat_replication LIMIT 1;
"@

$lagInt = [int]$lag.Trim()

if ($lagInt -eq -1) {
    $body = "CRITIQUE : Aucun Replica connecté au Primary !"
    Send-MailMessage -To "admin@pharmacie.fr" -Subject "[PharmaSmart] Réplication KO" `
        -Body $body -SmtpServer "smtp.gmail.com" -Port 587 `
        -From "pharmasmart@gmail.com" -UseSsl `
        -Credential (Get-Credential)
}
elseif ($lagInt -gt 60) {
    Write-Warning "Lag réplication = ${lagInt}s"
}
else {
    Write-Host "Réplication OK — lag = ${lagInt}s"
}
```

Planification dans le Planificateur de tâches Windows :

```powershell
$action  = New-ScheduledTaskAction -Execute "PowerShell.exe" `
             -Argument "-NonInteractive -File C:\Scripts\check_replication.ps1"
$trigger = New-ScheduledTaskTrigger -RepetitionInterval (New-TimeSpan -Minutes 15) `
             -Once -At (Get-Date)
Register-ScheduledTask -Action $action -Trigger $trigger `
  -TaskName "PG18-ReplicationCheck" -RunLevel Highest
```

### Seuils d'alerte recommandés

| Métrique | Seuil alerte | Action |
|----------|-------------|--------|
| `replay_lag` | > 30 s | Vérifier réseau / charge |
| `retained_wal` | > 5 Go | Vérifier état du slot |
| Replica déconnecté | > 5 min | Alerte critique |
| Espace disque PGDATA | > 80% | Purger anciens WAL archivés |
| `pg_stat_replication` vide | immédiat | Réplication interrompue |

---

## 11. Failover manuel

> Procédure si le Primary (Machine A) tombe en panne irrémédiable.

### Étape 1 — Confirmer la panne du Primary

```powershell
# Depuis Machine B
Test-NetConnection -ComputerName 192.168.1.10 -Port 5432
# TcpTestSucceeded : False → Primary inaccessible
```

### Étape 2 — Vérifier le lag avant promotion

```sql
-- Sur Machine B (Replica)
SELECT pg_is_in_recovery();              -- TRUE
SELECT now() - pg_last_xact_replay_timestamp() AS lag;
-- Si lag faible → peu de données perdues
```

### Étape 3 — Promouvoir le Replica en Primary

```powershell
# Option A : via pg_ctl (PowerShell admin sur Machine B)
& "C:\Program Files\PostgreSQL\18\bin\pg_ctl.exe" promote `
    -D "C:\PostgreSQL\18\data"

# Option B : via SQL (connexion locale sur Machine B)
# SELECT pg_promote();
```

PostgreSQL supprime `standby.signal` et commence à accepter les écritures.

```sql
-- Vérification
SELECT pg_is_in_recovery();   -- FALSE → Machine B est maintenant Primary
```

### Étape 4 — Reconfigurer l'application

```powershell
# Modifier la variable d'environnement ou application.yml
[System.Environment]::SetEnvironmentVariable("DB_HOST", "192.168.1.11", "Machine")

# Redémarrer Pharma-Smart (si installé comme service Windows)
Restart-Service -Name "pharmasmart"
```

### Étape 5 — Reconstruire l'ancien Primary comme nouveau Replica

Une fois Machine A réparée, reprendre l'étape 6 en inversant les rôles :
- Machine B devient le nouveau Primary (`192.168.1.11`)
- Machine A devient le nouveau Replica, bootstrappé via `pg_basebackup`

**Durée totale du failover manuel : 5–15 minutes.**

---

## 12. Failover automatique avec Patroni sur Windows

Patroni fonctionne sur Windows via Python. Il orchestre la bascule automatique
en s'appuyant sur etcd comme registre de consensus.

### Installation

```powershell
# Python 3.12+ requis (vérifier : python --version)
pip install patroni[etcd]

# Installer etcd pour Windows (depuis GitHub releases : etcd-io/etcd)
# Extraire etcd.exe et etcdctl.exe dans C:\etcd\
```

### `patroni.yml` (Machine A — Primary initial)

```yaml
scope: pharma-cluster
name: node-a

etcd3:
  host: 192.168.1.10:2379

bootstrap:
  dcs:
    ttl: 30
    loop_wait: 10
    retry_timeout: 30
    maximum_lag_on_failover: 1048576   # 1 Mo max de lag acceptable
  initdb:
    - encoding: UTF8
    - data-checksums

postgresql:
  listen: 0.0.0.0:5432
  connect_address: 192.168.1.10:5432
  data_dir: C:\PostgreSQL\18\data
  bin_dir: C:\Program Files\PostgreSQL\18\bin
  pgpass: C:\PostgreSQL\.pgpass
  authentication:
    replication:
      username: pharma_smart_backup
      password: backup_password_fort
    superuser:
      username: postgres
      password: postgres_password
  parameters:
    wal_level: replica
    max_wal_senders: 5
    wal_keep_size: 512MB
    hot_standby: on
    wal_compression: zstd

tags:
  nofailover: false
  noloadbalance: false
```

### Lancer Patroni comme service Windows

```powershell
# Installer NSSM (Non-Sucking Service Manager) pour wrapper Patroni
# Télécharger nssm.exe sur nssm.cc

nssm install Patroni-NodeA python
nssm set Patroni-NodeA AppParameters "C:\Python\Scripts\patroni C:\patroni.yml"
nssm set Patroni-NodeA AppDirectory "C:\PostgreSQL"
nssm start Patroni-NodeA
```

### Comportement automatique Patroni

```
1. Heartbeat toutes les 10 s vers etcd
2. Si Primary ne répond plus → election etcd (TTL = 30 s)
3. Replica avec le moins de lag est promu automatiquement (< 30 s)
4. Ancien Primary au retour → reintégré automatiquement en Replica
5. API REST sur port 8008 : GET http://192.168.1.10:8008/health
```

---

## 13. Slot de réplication vs WAL archivage

### Slot de réplication physique

```sql
-- Créer un slot dédié (sur le Primary)
SELECT pg_create_physical_replication_slot('replica_machine_b');

-- Configurer dans postgresql.auto.conf du Replica
-- primary_slot_name = 'replica_machine_b'
```

**Avantage :** PostgreSQL conserve tous les WAL nécessaires même si le Replica
est déconnecté — aucune perte au redémarrage.

**Danger Windows :** Le disque `C:\PostgreSQL\18\data\pg_wal\` peut saturer
si le Replica reste hors ligne longtemps.

```sql
-- Surveillance critique (alerter si > 5 Go)
SELECT slot_name, active,
       pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn)) AS retained_wal,
       wal_status   -- 'ok' | 'reserved' | 'extended' | 'unreserved' | 'lost'
FROM pg_replication_slots;
```

### WAL archivage sur partage réseau Windows

```ini
# postgresql.conf (Primary)
archive_mode = on
archive_timeout = 300

# Archivage vers partage réseau (NAS ou Machine B)
archive_command = 'copy "%p" "\\\\192.168.1.20\\pg_archive\\%f"'

# Ou vers un dossier local avec robocopy planifié
# archive_command = 'copy "%p" "D:\\wal_archive\\%f"'
```

**PITR (Point-In-Time Recovery) avec PG 18 :**

```powershell
# Restaurer la base au 18 avril 2026 à 14h30
# (dans recovery.conf ou postgresql.auto.conf)
# recovery_target_time = '2026-04-18 14:30:00'
# restore_command = 'copy "C:\\wal_archive\\%f" "%p"'
```

### PG 18 — Backup incrémental natif

```powershell
# Premier backup complet avec création d'un slot de backup
& pg_basebackup.exe -h 192.168.1.10 -U pharma_smart_backup `
    -D "D:\Backups\full_$(Get-Date -Format 'yyyyMMdd')" `
    --create-slot --slot=incremental_slot -Xs -R -P

# Backup incrémental suivant (PG 18 uniquement)
& pg_basebackup.exe -h 192.168.1.10 -U pharma_smart_backup `
    -D "D:\Backups\incr_$(Get-Date -Format 'yyyyMMdd_HHmm')" `
    --incremental="D:\Backups\full_20260418\backup_manifest" `
    -Xs -P
```

---

## 14. Checklist de mise en production

### Primary — Machine A

- [ ] PostgreSQL 18 installé, service `postgresql-x64-18` en `Automatic`
- [ ] `wal_level = replica` dans `postgresql.conf`
- [ ] `max_wal_senders = 5` et `wal_keep_size = 512MB`
- [ ] `wal_compression = zstd` activé
- [ ] `archive_mode = on` + `archive_command` fonctionnel (tester manuellement)
- [ ] Règle pare-feu Windows : TCP 5432 entrant depuis `192.168.1.11`
- [ ] `pg_hba.conf` : entrée `replication` pour `192.168.1.11/32`
- [ ] Utilisateur `pharma_smart_backup` avec droit `REPLICATION`
- [ ] Service redémarré et `pg_stat_replication` non vide après init Replica

### Replica — Machine B

- [ ] PostgreSQL 18 installé, même version mineure que le Primary
- [ ] `pg_basebackup` exécuté avec `-R` depuis le Primary
- [ ] `standby.signal` présent dans `C:\PostgreSQL\18\data\`
- [ ] `hot_standby = on` et `hot_standby_feedback = on`
- [ ] Service démarré, logs montrent "streaming WAL from primary"
- [ ] `pg_is_in_recovery()` retourne `TRUE`
- [ ] Test `INSERT` sur Primary vérifié sur Replica
- [ ] `INSERT` direct sur Replica refusé (erreur "cannot execute INSERT in a read-only transaction")

### Monitoring

- [ ] Script `check_replication.ps1` planifié toutes les 15 min
- [ ] Alerte mail configurée (SMTP Gmail ou serveur interne)
- [ ] Seuil alerte `retained_wal > 5 Go` actif
- [ ] Dashboard pgAdmin 4 configuré sur Machine A

### Application

- [ ] Spring Boot pointe sur `192.168.1.10:5432` (Primary)
- [ ] Procédure de failover documentée et testée en dry-run
- [ ] Variable `DB_HOST` externalisée (env var ou `application-prod.yml`)

---

## 15. Risques et limites Windows

| Risque | Probabilité | Impact | Mitigation Windows |
|--------|------------|--------|-------------------|
| Mise à jour Windows forcée redémarre PG | Moyen | Moyen | Désactiver redémarrage auto, maintenance planifiée |
| Antivirus scanne les fichiers WAL | Faible | Élevé (performance) | Exclure `C:\PostgreSQL\18\data\pg_wal\` de l'AV |
| Saturation disque WAL (slot inactif) | Moyen | Critique | Alerte PowerShell + `max_slot_wal_keep_size` |
| Split-brain sans Patroni | Faible | Critique | Procédure failover documentée + Patroni évolution |
| Perte données failover asynchrone | Faible | Moyen | Passer en synchrone si RPO=0 exigé |
| Permissions NTFS sur PGDATA | Faible | Bloquant | Service PG tourne sous compte `postgres`, vérifier ACL |
| Résolution DNS lente au failover | Faible | Moyen | Utiliser IPs fixes, pas de DNS dans `primary_conninfo` |

### Configuration antivirus — exclusions obligatoires

```powershell
# Windows Defender — exclure les répertoires PostgreSQL
Add-MpPreference -ExclusionPath "C:\PostgreSQL\18\data"
Add-MpPreference -ExclusionPath "C:\PostgreSQL\wal_archive"
Add-MpPreference -ExclusionPath "D:\Backups"
Add-MpPreference -ExclusionExtension ".conf"
```

### Gestion de `max_slot_wal_keep_size` (PG 13+)

```ini
# postgresql.conf — limite la rétention WAL d'un slot inactif
max_slot_wal_keep_size = 10GB
```

Si le Replica dépasse ce lag, son slot est invalidé (`wal_status = 'lost'`)
plutôt que de saturer le disque Primary. Dans ce cas, relancer un `pg_basebackup`.

---

*Document — Architecture PostgreSQL 18 HA sur Windows — Pharma-Smart*
*Version cible : PostgreSQL 18 / Windows 11 Pro / Windows Server 2025*
*Dernière mise à jour : 2026-04-18*
