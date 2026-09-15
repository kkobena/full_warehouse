# Plan — Amélioration du module Tauri (`src-tauri`)

> Rédigé le 2026-09-14, à partir d'une revue complète du module.
> Périmètre lu : 4 335 lignes Rust (`src/`, 7 modules), les trois `tauri.conf*.json`,
> `capabilities/default.json`, et ~2 000 lignes de PowerShell / NSIS
> (`installer-hooks/`, `service/`, `backup/`).
> Documents liés : [PLAN-HTTPS-ET-CERTIFICATS.md](PLAN-HTTPS-ET-CERTIFICATS.md) (même
> contexte LAN d'officine).

---

## 0. Architecture de déploiement — **en place et fonctionnelle**

L'installation type d'une officine est **un poste serveur + N postes clients**, tous sur
le LAN :

|                     | Poste serveur                                                                 | Poste client                                       |
| ------------------- | ----------------------------------------------------------------------------- | -------------------------------------------------- |
| Backend Spring Boot | **Service Windows** `pharmasmart-app` (WinSW), installé par `installer.nsh`    | Aucun — consomme celui du poste serveur            |
| Batch nocturne      | **Service Windows** `pharmasmart-batch` (WinSW)                               | Aucun                                              |
| Exécutable Tauri    | Build `bundled` / `bundled-jre` (embarque le JAR et le JRE)                   | Build standard, configuré en client                |
| URL backend         | `http://localhost:<port>` via `config.json`                                   | `http://<ip-serveur>:<port>` via `backend-url.txt` |
| PostgreSQL          | Local                                                                          | Distant (celui du poste serveur)                   |

Ce fonctionnement est **opérationnel aujourd'hui** et n'est remis en cause par aucune
recommandation de ce plan. `installer.nsh:447-469` conditionne d'ailleurs correctement
l'installation des deux services à la présence des JAR sidecar : un installeur standard
posé sur un poste client ne crée pas de service orphelin.

**Deux conséquences qui cadrent tout le reste du document :**

1. **Le backend a une durée de vie indépendante de la fenêtre Tauri.** Fermer PharmaSmart
   sur le poste serveur ne doit pas couper les postes clients en cours de vente. Toute
   recommandation qui reviendrait à tuer le backend à la fermeture de la fenêtre est
   écartée — voir §8.1.
2. **Le runtime de production, c'est le service Windows, pas la JVM lancée par Tauri.**
   `build_jvm_args`, `find_jar_file`, `check_postgres_ready` et `wait_for_backend_ready`
   ne servent qu'en repli, quand `try_connect_existing` ne trouve pas de service actif.
   En revanche, **les scripts PowerShell qui configurent ces services lisent
   `config.json`** — ce qui fait de §2.1 le point le plus critique du document, et
   relègue §2.4 à de la dette de faible impact.

---

## 1. Ce qui fonctionne bien (à ne pas casser)

À garder en tête avant de refactorer :

- **`scanner.rs` — gestion USB CDC.** Le moniteur `WM_DEVICECHANGE` (`device_monitor`),
  le primer DTR et le repli `RawCdcPort` en IO overlapped traitent des cas de firmware
  réels que `serialport` seul ne sait pas gérer. Le raisonnement est documenté dans le
  code ; ne pas « simplifier » sans reproduire les pannes d'origine.
- **`check_port_health`** — la détection de ré-énumération USB par la transition
  absence → présence est correcte et non triviale.
- **`AppConfig.extra`** (`#[serde(flatten)]`) — le réflexe est le bon ; il faut
  simplement l'étendre d'un niveau (§2.1).
- **`try_connect_existing`** — se raccrocher à un backend déjà actif (service Windows)
  plutôt que d'en spawner un second est exactement ce qu'impose l'architecture ci-dessus.
- **Découverte de `config.json`** — la logique de priorité entre répertoire exe,
  `%PROGRAMDATA%` et `%APPDATA%`, avec test d'écriture réel plutôt que test d'existence,
  résout un vrai problème d'installation en `Program Files`.

---

## 2. Chantier 1 — Anomalies confirmées, impact client

### 2.1 `save()` détruit `jvm.java_home`, `jvm.app` et `jvm.batch` — ✅ **corrigé**

> ✅ **Traité le 2026-09-14.** `JvmConfig` (`src-tauri/src/config.rs`) modélise désormais
> `java_home` + `app` + `batch`, avec un `#[serde(flatten)] extra` sur chacun des trois
> niveaux. `build_jvm_args` lit `jvm.app`, le DTO de l'IHM aussi (noms de champs Angular
> inchangés), et `resolve_bundled_java` honore `jvm.java_home` avant le JRE des ressources,
> dans le même ordre que les scripts PowerShell. Quatre tests de non-régression ajoutés,
> dont une fixture reprenant mot pour mot le bloc `jvm` écrit par `installer.nsh`.
> Le diagnostic ci-dessous est conservé : il documente le pourquoi de la structure.

**Constat.** L'installeur écrit (`installer-hooks/installer.nsh:315-330`) :

```json
"jvm": {
  "java_home": "C:\\...\\sidecar\\jre",
  "app":   { "heap_min": "2g", "heap_max": "2g", "metaspace_size": "256m" },
  "batch": { "heap_min": "128m", "heap_max": "512m", "additional_options": [] }
}
```

`JvmConfig` (`src-tauri/src/config.rs:23`) modélise un `jvm` **plat** : `heap_min`,
`heap_max`, `metaspace_size`… directement sous `jvm`. Serde ignore les clés inconnues :

1. **À la lecture** — `java_home`, `app` et `batch` sont ignorés ; les valeurs de
   l'installeur ne sont jamais appliquées. `build_jvm_args` utilise les défauts Rust
   (qui coïncident aujourd'hui avec ceux de l'installeur, ce qui masque le problème).
2. **À l'écriture** — `AppConfig::save()` réémet `jvm` au format plat et **efface** les
   trois clés.

**Qui casse, et quand.** Au premier enregistrement depuis l'assistant de configuration
ou l'écran de configuration de l'application :

| Script                                | Lignes | Clé perdue                       | Effet                                                                                                                       |
| ------------------------------------- | ------ | -------------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| `service/setup-backend-service.ps1`   | 70-72  | `jvm.java_home`, `jvm.app.heap_*` | Le service perd le chemin du JRE embarqué → repli sur `JAVA_HOME`/`PATH`, absent sur un poste sans JDK → **le service ne démarre plus** |
| `service/refresh-service-config.ps1`  | 80-82  | idem                             | Rafraîchissement silencieusement dégradé aux valeurs par défaut du script                                                     |
| `service/setup-batch-service.ps1`     | 64-66  | `jvm.batch.heap_*`               | Le service batch repart aux défauts                                                                                           |

Symétriquement, les champs JVM de l'éditeur de configuration (`jvm_heap_min`,
`jvm_heap_max`…) écrivent des clés que **les scripts PowerShell ne lisent jamais** :
l'écran de réglage JVM n'a aucun effet sur le service Windows.

C'est précisément parce que le runtime de production est le service Windows (§0) que ce
point est le plus critique du document : `config.json` n'est pas un fichier de confort
côté Tauri, c'est **le fichier de configuration des deux services**, et l'application le
réécrit en en perdant une partie.

C'est la classe de bug que le champ `extra` documente et corrige — un niveau au-dessus.
`extra` ne protège que les sections racine ; `jvm` étant modélisé, son contenu non
modélisé disparaît.

**Correctif retenu.** Aligner le Rust sur le format de l'installeur, qui est celui que
lisent les trois scripts et donc le contrat de fait :

```rust
pub struct JvmConfig {
    #[serde(default)] pub java_home: String,
    #[serde(default)] pub app: JvmProcessConfig,     // heap_min, heap_max, metaspace_*, …
    #[serde(default)] pub batch: JvmProcessConfig,
    #[serde(flatten)] pub extra: serde_json::Map<String, serde_json::Value>,
}
```

- `build_jvm_args` lit `config.jvm.app`.
- `AppConfigDto` expose `jvm.app` (et, si l'on veut piloter le batch depuis l'IHM,
  `jvm.batch`).
- `find_bundled_jre` doit honorer `jvm.java_home` avant de sonder `resource_dir`,
  comme le font déjà les scripts.
- Ajouter `#[serde(flatten)] extra` **sur chaque sous-structure de configuration**, pas
  seulement sur `AppConfig` : c'est la généralisation de la leçon déjà apprise.

**Test de non-régression.** Reprendre le `config.json` réellement produit par
`installer.nsh` (fixture figée dans `src/config.rs`, pas un JSON réécrit à la main), le
désérialiser, appeler `save()`, relire, et vérifier que `jvm.java_home`, `jvm.app.*`,
`jvm.batch.*` et `backup.*` sont tous intacts.

### 2.2 `detect_scanner_usb_mode` n'est pas enregistrée dans le binaire — ✅ **corrigé**

> ✅ **Traité le 2026-09-14.** La commande est enregistrée dans `main.rs`. Le compilateur
> le signalait d'ailleurs depuis le début : 8 des 9 avertissements du build
> (`detect_scanner_usb_mode is never used`, `find_hid_scanner is never used`,
> `GUID_DEVCLASS_HID is never used`…) n'étaient rien d'autre que le module `hid_detect`
> devenu inatteignable faute de point d'entrée. Build désormais sans aucun avertissement,
> sur les deux jeux de features.

`src/lib.rs:29` la déclare ; `src/main.rs:400-425` — le seul point d'entrée réel — ne la
déclare pas. Angular l'invoque
(`app/shared/services/tauri-device-detection.service.ts:150`), reçoit une erreur, et
`app/shared/scanner/scan-orchestrator.service.ts:293` l'avale en `console.warn` avant de
basculer sur un repli.

**Conséquence :** la détection du mode HID vs CDC de la douchette **ne fonctionne jamais
en production**, sans le moindre signal. Corrigé de fait par §2.3.

### 2.3 `src/lib.rs` est du code mort divergent — ✅ **supprimé**

> ✅ **Traité le 2026-09-14.** `src/lib.rs` et la section `[lib]` du `Cargo.toml` ont été
> retirés après vérification qu'aucun consommateur n'existait : le crate `pharmasmart_lib`
> n'était référencé que par sa propre déclaration ; `src-tauri/gen/` ne contient que
> `schemas` (aucune cible mobile générée) ; aucun script npm `android`/`ios` ; ni
> `sales-android` ni `mobile-inventory` n'y font référence (ce sont des modules Android
> natifs indépendants) ; pas de workspace Cargo ni de `.cargo/config`. Vérifié par une
> compilation **et une édition de liens** réelles sur les deux jeux de features, et par les
> 12 tests de la cible binaire — les 2 tests que portait la cible lib y figuraient déjà.
> Si une cible mobile devenait nécessaire, `tauri android init` régénère le nécessaire.

34 lignes jamais appelées, mais compilées (`crate-type = ["staticlib", "cdylib", "rlib"]`) :
elles dupliquent `scanner` et `customer_display` — donc deux jeux de statiques
`SCANNER_FLAGS` compilés — et exposent une liste de commandes différente de `main.rs`.
C'est la cause racine de §2.2 : deux listes à maintenir, une seule utilisée.

**Correctif.** Une seule source de vérité. Option la plus conventionnelle en Tauri 2 :
déplacer tout le `Builder` dans `lib.rs::run()` et réduire `main.rs` à

```rust
fn main() { pharmasmart_lib::run() }
```

À défaut, supprimer `lib.rs` et la section `[lib]` du `Cargo.toml`. Dans les deux cas,
la liste `generate_handler!` devient unique.

### 2.4 `BackendState.port` devient obsolète après changement de port — *impact faible*

`BackendState::new(config.server.port)` est figé au `setup` (`main.rs`, bloc
`bundled-backend`). `save_server_port` et `save_app_config_dto` modifient `config.json`
sans le mettre à jour. `try_graceful_shutdown(state.port, …)` (`backend_manager.rs`,
section « Graceful shutdown ») frappe donc l'ancien port : l'arrêt propre échoue et l'on
tombe sur `taskkill /F`.

**Portée réelle.** En production le backend est le service Windows, donc le PID stocké
vaut `0` et `stop_backend` sort avant même d'atteindre `try_graceful_shutdown` : le
chemin fautif n'est atteint qu'en développement ou dans le repli « JVM lancée par
Tauri ». À corriger par propreté, pas en urgence.

**Correctif.** Relire `AppConfig::load(app).server.port` dans `stop_backend`, comme le
fait déjà `inner_start` — plus simple qu'un `Mutex<u16>` à synchroniser.

### 2.5 « Redémarrer le backend » ne redémarre rien — c'est le cas nominal

Quand `try_connect_existing` a réussi — c'est-à-dire **à chaque lancement sur un poste
serveur**, puisque le service Windows tourne déjà — le PID stocké vaut `0`.
`stop_backend` retourne alors immédiatement, `start_backend` se reconnecte au même
processus, et `restart_backend_main` renvoie « Backend redémarré » sans avoir rien
redémarré.

Le bouton ne fonctionne donc que dans le cas le plus rare. Les trois situations à
distinguer :

| Situation                                                       | Comportement attendu                                                                                                                                                      |
| --------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Service Windows local `pharmasmart-app`** *(cas nominal poste serveur)* | `refresh-service-config.ps1` puis `Restart-Service pharmasmart-app` — le motif n°1 d'un redémarrage étant justement un changement de configuration, l'enchaînement des deux est ce qu'attend l'utilisateur |
| Backend d'un autre poste *(poste client)*                       | Refuser explicitement : « Le backend est hébergé par le poste `<hôte>` — le redémarrage doit être effectué depuis ce poste. »                                              |
| JVM lancée par ce Tauri (PID réel, repli / dev)                 | Comportement actuel : arrêt gracieux puis relance                                                                                                                            |

Distinguer les deux premiers cas ne demande rien de nouveau : il suffit de tester si
l'URL backend effective pointe vers `localhost` (cf. §6). L'élévation requise par
`Restart-Service` doit être gérée — soit en relançant le script PowerShell via
`runas`, soit en désactivant le bouton avec un message clair si l'application n'est pas
élevée.

---

## 3. Chantier 2 — Diagnostic en production (angle mort total)

**Constat.** `src/main.rs:2` pose `windows_subsystem = "windows"` en release : pas de
console. Or :

- `tracing_subscriber::fmt()` (`main.rs:381`) écrit sur **stdout** ;
- `config.rs` utilise en plus des `println!` / `eprintln!` ;
- `printer.rs` également.

**En production, 100 % des traces Rust sont perdues** — alors même que le chemin de log
est connu et le répertoire créé (`config.logging.directory`, `ensure_log_dir()`).
Concrètement : quand une douchette ne répond plus chez un client, il n'existe aucune
trace exploitable côté Tauri.

**Aggravant — `panic = "abort"` (`Cargo.toml:68`).** Combiné aux
`SCANNER_FLAGS.lock().unwrap()` (`scanner.rs:627, 769, 778, 875, 888, 898`) et aux
`unwrap()` de `printer.rs`, un panic dans un thread scanner **tue l'application entière**,
sans laisser de trace. Pour un poste de caisse, c'est le pire scénario possible.

**Correctifs.**

1. Ajouter `tracing-appender` avec rotation quotidienne vers
   `config.logging.directory/pharmasmart-tauri.log`, en conservant la sortie console en
   `debug_assertions`. Initialiser après lecture de la configuration (ou en deux temps :
   un `fmt` minimal, puis rebranchement une fois le chemin connu).
2. Poser un `std::panic::set_hook` qui journalise le panic **avant** l'abort.
3. Remplacer tous les `lock().unwrap()` par `lock().unwrap_or_else(|e| e.into_inner())` :
   un mutex empoisonné ne doit pas propager une panique en cascade sur toutes les
   opérations scanner suivantes.
4. Uniformiser : plus aucun `println!` / `eprintln!` dans `src/`, uniquement `tracing`.
5. Exposer une commande `open_log_directory` (plugin `shell`, `open`) pour que le support
   puisse guider un pharmacien vers ses logs en un clic.

---

## 4. Chantier 3 — Sécurité

| #   | Point                                                                            | Emplacement                                                      | Action                                                                                                                                                                                                 |
| --- | -------------------------------------------------------------------------------- | ---------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 4.1 | **Clé API FNE en clair** dans un fichier versionné                               | `src-tauri/config.default.json:41`                               | Rotationner la clé, la sortir du dépôt (variable d'environnement / saisie à l'installation), purger l'historique git si la clé est encore valide                                                        |
| 4.2 | **Mot de passe PostgreSQL en clair**, fichier suivi par git et livré dans l'installeur | `src-tauri/installer-hooks/db-defaults.json:6`                   | Idem : générer le mot de passe à l'installation plutôt que de livrer une valeur par défaut identique chez tous les clients                                                                               |
| 4.3 | Compte mail de service en clair                                                  | `config.default.json` (`mail.username`, `mail.email`)            | Déplacer en configuration poste, hors dépôt                                                                                                                                                             |
| 4.4 | **CSP inopérante** : `connect-src … http://*:* ws://*:*`                         | `tauri.conf.json`                                                | Restreindre aux cibles réelles : `http://localhost:*` + le sous-réseau LAN. Avec `script-src 'unsafe-inline' 'unsafe-eval'`, la CSP actuelle n'oppose aucun obstacle à l'exfiltration en cas de XSS — sur des données de santé, ce n'est pas neutre |
| 4.5 | **IP privée d'un poste de dev figée dans la CSP livrée** : `172.23.162.15`        | `tauri.bundled.conf.json:74`, `tauri.bundled-jre.conf.json:74`   | Retirer ; les plages `192.168.*` / `10.*` déjà présentes couvrent le besoin réel                                                                                                                        |
| 4.6 | Mot de passe DB écrit en clair dans `config.json` puis dans le XML WinSW          | `save_app_config_dto`, `setup-backend-service.ps1`               | Vérifier que `config.json` reçoit bien une ACL restrictive dans `installer.nsh` (le XML WinSW, lui, est documenté comme protégé). À terme : DPAPI (`ProtectedData`) pour le secret                        |
| 4.7 | **Installeurs non signés** — pas de `certificateThumbprint`                       | `tauri.conf*.json` → `bundle.windows`                            | Signature Authenticode : SmartScreen affiche aujourd'hui un avertissement à chaque installation chez le pharmacien                                                                                       |

`capabilities/default.json` est en revanche correctement calibré : `fs` limité en écriture
avec une portée `$DOWNLOAD` / `$DOCUMENT` / `$DESKTOP`, `shell` réduit à `open`. Rien à
resserrer de ce côté.

---

## 5. Chantier 4 — Robustesse de la configuration

### 5.1 Écriture non atomique — risque de perte de configuration

`AppConfig::save()` fait deux `fs::write` directs (répertoire de données, puis miroir
dans le répertoire exe), sans verrou ni fichier temporaire. Une coupure de courant ou un
arrêt brutal en cours d'écriture laisse un `config.json` tronqué → parsing en échec →
repli sur les défauts → **perte du mot de passe DB et de `setup_complete`** : l'assistant
de configuration initiale se ré-affiche chez le client, base pourtant déjà en place.

**Correctif :** écrire dans `config.json.tmp` puis `fs::rename` (atomique sur NTFS pour un
même volume). Faire de même pour le miroir.

### 5.2 Échec de parsing silencieux

Un `config.json` invalide passe au répertoire candidat suivant avec un simple `eprintln!`
— invisible en production (§3) — et finit sur les défauts. L'utilisateur voit l'assistant
réapparaître sans explication, et un enregistrement écrasera alors la configuration
valide.

**Correctif :** journaliser en `error!`, **renommer** le fichier fautif en
`config.json.invalid-<horodatage>` plutôt que de risquer de l'écraser, et remonter
l'anomalie à l'IHM.

### 5.3 Champs obligatoires trop stricts

`server`, `logging` et `installation` n'ont pas de `#[serde(default)]` : l'absence de l'un
d'eux invalide tout le fichier et déclenche le scénario 5.2. Leur donner un défaut.

### 5.4 Relecture disque à chaque commande

`AppConfig::load()` est appelée dans `get_setup_defaults`, `check_needs_setup`,
`get_app_config_dto`, `save_server_port`, `save_app_config_dto`, `inner_start`… et chaque
appel déclenche `is_dir_writable`, qui **crée et supprime un fichier test**. Mettre la
configuration en `State` (`Mutex<AppConfig>`), rechargée explicitement.

### 5.5 Pas d'instance unique

Aucun `tauri-plugin-single-instance`. Deux instances lancées sur le même poste se
disputent le port COM de la douchette (`SCANNER_FLAGS` est par processus) et, en mode
bundled, tentent toutes deux de démarrer un backend. Ajouter le plugin et focaliser la
fenêtre existante.

---

## 6. Chantier 5 — Diagnostic de la configuration poste client

> **Portée volontairement réduite.** Le mécanisme actuel — installeur standard sur le
> poste client + `backend-url.txt` pointant sur le serveur — fonctionne et reste la
> référence. Il n'est **pas** proposé d'introduire un champ `role` dans `config.json` :
> ce serait une seconde source de vérité pour une information que le déploiement encode
> déjà (voir §8.3). Seule la lisibilité des pannes est en cause ici.

**Le point faible est le silence en cas de mauvaise configuration.** `get_backend_url()`
(`main.rs`) applique cette priorité :

1. `backend-url.txt` (répertoire exe, `%PROGRAMDATA%\PharmaSmart`, `%APPDATA%\PharmaSmart`)
2. `config.json` → `server.port`, qui construit **toujours** `http://localhost:<port>`
3. variable d'environnement `BACKEND_URL`
4. défaut `http://localhost:9080`

Sur un poste client, si `backend-url.txt` est absent, mal nommé, placé dans un répertoire
non balayé ou vidé par une mise à jour, l'étape 2 prend le relais **sans rien signaler** :
le poste interroge son propre `localhost`, ne trouve rien, et affiche « backend
indisponible » — message qui n'oriente vers aucune cause. La trace `tracing::info!` qui
dirait laquelle des quatre sources a gagné est, elle, perdue (§3).

**Améliorations proposées, toutes locales :**

- Faire remonter à l'IHM **la source retenue** et l'URL résolue (nouveau champ dans le
  retour de `get_backend_url_command`, ou commande dédiée), et l'afficher dans l'écran
  d'état backend. Un installateur voit immédiatement « source : défaut — aucun
  `backend-url.txt` trouvé » au lieu d'un échec opaque.
- Quand l'URL résolue vaut `localhost` **et** qu'aucun JAR sidecar n'est présent, le
  message d'erreur doit devenir explicite : « Ce poste est configuré en client mais
  aucune adresse de serveur n'a été trouvée — créez `backend-url.txt` à côté de
  `PharmaSmart.exe`. »
- Permettre la saisie de l'URL serveur depuis l'écran d'état backend, avec écriture de
  `backend-url.txt` : évite de faire manipuler un fichier texte à l'installateur, sans
  changer le format ni la priorité.
- Journaliser systématiquement la source retenue au démarrage (dépend de §3).

---

## 7. Chantier 6 — Outillage, tests et dette

### 7.1 `Cargo.lock` est ignoré par git

`src-tauri/.gitignore:3`. Pour un crate **applicatif**, c'est un contresens : les builds
ne sont pas reproductibles et une mise à jour transitive peut s'inviter entre deux
installeurs livrés chez un client, sans aucune trace. **À committer.**

### 7.2 Aucune intégration continue

`.github/workflows/` est vide. Ajouter un job Windows : `cargo fmt --check`,
`cargo clippy -- -D warnings`, `cargo test`, sur les deux jeux de features (défaut et
`bundled-backend`), pour éviter qu'une commande cfg-gated ne casse silencieusement
(cf. §2.2).

### 7.3 Couverture de test quasi nulle

5 tests au total : `build_jvm_args` ×2, préservation de `extra` ×1, `customer_display` ×2.
Non couvertes alors qu'elles sont **pures et triviales à tester** :

- `parse_jdbc_url` (`main.rs`) — y compris les cas dégradés (IPv6, port manquant)
- `get_backend_url` / `backend_url_search_dirs` — priorités entre les sources
- `check_port_health` (`scanner.rs`) — la machine à états des 4 transitions
- `suggest_role`, `suggest_role_by_vid_pid`, `chipset_name`, `clean_product_name`,
  `resolve_manufacturer`
- `AppConfig` : cycle complet installeur → lecture → `save()` → relecture (§2.1)

`scanner.rs` fait à lui seul 1 478 lignes sans un seul test.

### 7.4 Code mort et dette

| Élément                                                                             | Emplacement          | Action                                                          |
| ----------------------------------------------------------------------------------- | -------------------- | --------------------------------------------------------------- |
| `types::PrinterError` défini, jamais utilisé (`printer.rs` renvoie des `String` brutes) | `src/types.rs:48`    | Soit l'employer dans `printer.rs`, soit le supprimer             |
| `mod cdc_primer_stub_placeholder_remove_old {}`                                     | `src/scanner.rs:253` | Supprimer                                                        |
| `let _ = hit_gen_failure;` — variable calculée puis neutralisée                      | `src/scanner.rs`     | L'exploiter dans une trace de diagnostic, ou la retirer          |
| Feature `libusb` réservée, jamais implémentée                                       | `Cargo.toml`         | Documenter comme non tenue, ou retirer                           |
| `tokio = { features = ["full"] }`                                                    | `Cargo.toml`         | Réduire à `["net", "time", "process", "sync", "rt-multi-thread"]` |
| `base64 = "0.23"`                                                                    | `Cargo.toml`         | Vérifier : la branche publiée est 0.22                           |
| `RemoveBackendService` / `RemoveBatchService` (versions **sans** préfixe `un.`) jamais référencées — seules les variantes `un.` servent | `installer-hooks/installer.nsh` | Supprimer ; repéré via l'avertissement NSIS 6010 à la compilation |

### 7.5 Numéro de version — unifié à la compilation, un résidu cosmétique

**Le dispositif est en place et correct.** `scripts/sync-version.js`, exécuté avant chaque
build Tauri (`npm run version:sync`), propage `pom.xml/<revision>` — source unique de
vérité — vers `package.json` et les trois `tauri.conf*.json`, en normalisant au passage
les suffixes que NSIS/MSI refusent. Les versions Maven, Angular et Tauri sont donc
unifiées dans tout artefact livré. C'est un prérequis de §7.6 (l'updater compare du
semver) qui est **déjà satisfait**.

Reste un seul résidu, sans effet fonctionnel : `Cargo.toml:3` vaut `0.2.3` et n'est pas
touché par le script. Comme `tauri.conf.json` porte un champ `version`, c'est lui qui
l'emporte pour les métadonnées du binaire, et `Cargo.toml` est ignoré. L'écart est donc
purement cosmétique — mais trompeur à la lecture. Deux options : ajouter `Cargo.toml`
aux cibles du script, ou **retirer la ligne `version` de `Cargo.toml`** pour supprimer la
source d'ambiguïté (Tauri sait s'en passer dès lors que la conf la porte).

### 7.6 Mise à jour des postes clients par relais LAN

> **Contrainte de départ.** Il n'est pas prévu d'héberger les exécutables : les versions
> sont acheminées chez le client **par transfert réseau** (envoi de fichier, prise en
> main à distance, clé USB). Aucune infrastructure éditeur — endpoint public, CDN,
> dépôt de releases — n'entre donc dans le périmètre.

#### 7.6.1 Le problème à résoudre

Aujourd'hui, un correctif purement front oblige à passer physiquement sur **chacun des N
postes** de l'officine. C'est le coût de maintenance le plus visible, et il croît avec le
nombre de postes installés.

#### 7.6.2 Comment fonctionne `tauri-plugin-updater` (v2)
> ⚠ **Corrigé le 2026-09-14 — ni le plugin, ni la signature.** Deux constats ont
> successivement simplifié ce chantier :
>
> 1. **Le plugin officiel ne s'applique pas.** Les postes clients ne sont pas installés :
>    on y dépose `pharmasmart.exe`. Or l'updater Tauri télécharge et **exécute un
>    installeur**, et la documentation est explicite — *« there is no support for
>    portable/standalone .exe updates »*. Le mécanisme retenu est donc maison
>    (`src-tauri/src/self_update.rs`).
> 2. **La signature a été retirée** — décision de l'éditeur, et elle se tient : le binaire
>    est récupéré sur le LAN de l'officine, auprès de son propre serveur, par un poste qui
>    lui parle déjà pour tout le reste. Le niveau de confiance est celui de la copie
>    manuelle qu'il remplace. Une chaîne de clés (conservation, sauvegarde, rotation, perte
>    irréversible) aurait coûté cher sans déplacer le modèle de menace réel.

#### 7.6.2 Le mécanisme retenu

1. **Manifeste** — le poste interroge
   `<url backend>/api/updates/windows/x86_64/<version installée>`. Le serveur répond
   `204 No Content` s'il n'y a rien, ou `200` + `{version, pub_date, url, notes}`.
2. **Bascule** — téléchargement, contrôle de taille (un fichier tronqué rendrait le poste
   indémarrable), mise de côté de l'ancien binaire en `.exe.old`, écriture du nouveau,
   relance. Windows autorise le renommage d'un exécutable en cours d'exécution, donc aucun
   processus tiers n'est nécessaire.
3. **Retour arrière** — le `.exe.old` n'est pas un résidu : c'est le moyen de revenir à la
   version précédente en le renommant. C'est le filet retenu à la place d'une signature.

#### 7.6.3 Architecture retenue — relais par le poste serveur

```
Éditeur ──(1) transfert réseau, manuel──▶ Poste serveur ──(2) relais LAN, auto──▶ Postes clients
```

**Étape (1) reste manuelle** et inchangée : un fichier est transmis, quelqu'un l'exécute
sur le poste serveur. JAR, services Windows et migrations Flyway ne doivent de toute
façon pas se mettre à jour sans surveillance.

**Étape (2) est automatisée.** Le backend Spring Boot du poste serveur — déjà en service,
déjà joignable par tous les postes clients — sert de relais. Les postes clients pointent :

```
http://<ip-serveur>:<port>/api/updates/{{target}}/{{arch}}/{{current_version}}
```

Deux routes suffisent, sans stockage ni table :

| Route                                                   | Rôle                                                                                                          |
| ------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| `GET /api/updates/{target}/{arch}/{current_version}`    | Compare à la version de l'installeur client présent dans `%PROGRAMDATA%\PharmaSmart\updates\`. Identique → `204`. Différente → `{version, url, signature, notes}` |
| `GET /api/updates/download/{fichier}`                   | Sert l'exe depuis ce même répertoire                                                                            |

**L'installeur client est embarqué comme ressource de l'installeur serveur**, et déposé
dans `updates/` par `NSIS_HOOK_POSTINSTALL`. Deux bénéfices :

- **un seul fichier à transférer par version** — si l'exploitant doit en acheminer deux
  et penser à les déposer au bon endroit, le mécanisme sera contourné dès la première
  urgence ;
- **la cohérence front / back devient structurelle** : l'installeur client présent sur le
  serveur est, par construction, celui de la version du JAR installé au même instant. Un
  poste client ne peut être ni en avance ni en retard sur son backend — ce qui est le
  risque principal d'un updater où chaque poste se met à jour de son côté.

##### L'endpoint ne peut pas être figé au build

Point apparu en montant le socle de signature (étape 2), et qui conditionne l'étape 4 :
**chaque officine a une adresse de serveur différente**, alors que
`plugins.updater.endpoints` de `tauri.conf.json` est figé à la compilation. Produire un
installeur par officine serait absurde.

Le plugin prévoit le cas : `updater_builder().endpoints(…)` surcharge l'endpoint **à
l'exécution**. Or le poste client résout déjà son URL backend (`get_backend_url()`, §6) —
l'endpoint de mise à jour en découle directement :

```
<url backend résolue> + /api/updates/{{target}}/{{arch}}/{{current_version}}
```

Un seul artefact client, valable pour toutes les officines, qui se met à jour depuis le
serveur auquel il est déjà rattaché. C'est `self_update::check_url` qui l'assemble, à
partir de l'URL backend déjà résolue par le poste.

#### 7.6.4 Deux produits serveur, un artefact client commun

Le poste serveur se décline en **deux cas d'utilisation, choisis par officine** — ce n'est
pas une distinction entre première installation et mise à jour, mais bien deux lignées de
produit distinctes, chacune avec ses installations et ses mises à jour :

| Produit                         | Script npm                | Contenu           | Java utilisé                                                |
| ------------------------------- | ------------------------- | ----------------- | ------------------------------------------------------------ |
| **Serveur avec JRE embarquée**  | `tauri:build:bundled-jre` | Angular + JAR + JRE | La JRE livrée ; `jvm.java_home` pointe dessus                |
| **Serveur sans JRE**            | `tauri:build:bundled`     | Angular + JAR     | Java du poste — `jvm.java_home` s'il est renseigné, sinon `JAVA_HOME` puis `PATH` |

**L'artefact client est commun aux deux.** `tauri:build` ne contient ni JAR ni JRE : un
poste client n'exécute aucun Java. La question de la JRE ne concerne donc que le poste
serveur, et **le relais de mise à jour du §7.6.3 est identique dans les deux cas** — il
sert le même installeur client quelle que soit la lignée du serveur. C'est une
simplification réelle : rien à différencier côté endpoint, manifeste ou `{{target}}`.

**Les hooks NSIS supportent déjà les deux lignées** sans modification : `installer.nsh:306-312`
conditionne `jvm.java_home` à `${FileExists} "$INSTDIR\sidecar\jre\bin\java.exe"` et le
laisse vide sinon ; `installer.nsh:432-433` fait de même pour le Java du service. Seule la
configuration de bundling diffère, et les deux fichiers existent déjà
(`tauri.bundled-jre.conf.json` / `tauri.bundled.conf.json`).

##### Le risque : le croisement de lignées

`NSIS_HOOK_PREUNINSTALL` fait `RMDir /r "$INSTDIR\sidecar"` — et son propre commentaire
rappelle que *« l'updater Tauri lance ce même désinstalleur avant de réinstaller »*.
À l'intérieur d'une lignée, c'est sans conséquence : le produit avec JRE la resupprime et
la réinstalle à chaque mise à jour, le produit sans JRE n'en a pas.

**Le danger est d'appliquer par erreur l'installeur d'une lignée sur l'autre** — ce qui
devient probable dès lors que les versions circulent par transfert réseau, avec deux
fichiers portant le même numéro de version :

| Croisement                                         | Conséquence                                                                                                             |
| -------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| Installeur **sans JRE** sur une installation **avec** | La JRE est supprimée et jamais réinstallée. Le poste n'ayant par définition pas de Java système, **le backend ne redémarre plus** |
| Installeur **avec JRE** sur une installation **sans** | Bénin : le poste se retrouve avec une JRE embarquée en plus, et `jvm.java_home` repointé dessus                              |

> ✅ **Traité le 2026-09-14.** Nommage : `scripts/label-installer.js`, branché sur les
> quatre scripts npm `tauri:build:bundled*`, suffixe l'installeur produit en `-avec-jre` /
> `-sans-jre`. Détection :
> marqueur `jre-bundled.flag` dans `$PS_DataDir` + avertissement explicite dans
> `NSIS_HOOK_POSTINSTALL`. Repli : les trois scripts de service validaient déjà la
> présence effective de `java.exe` avant d'utiliser `jvm.java_home` — le côté Rust fait
> désormais de même, donc un `java_home` devenu mort dégrade au lieu de casser.

**Correctif à prévoir :** nommer les deux artefacts sans ambiguïté (suffixe `-jre`), et
surtout ajouter une **garde dans `NSIS_HOOK_POSTINSTALL`** — si une installation
précédente utilisait une JRE embarquée (`jvm.java_home` non vide dans le `config.json`
existant) et que le paquet en cours n'en apporte pas, refuser ou avertir explicitement
plutôt que de laisser un poste sans Java.

##### Lien avec §2.1

C'est dans la lignée **avec JRE embarquée** que la destruction de `jvm.java_home` par
`save()` est la plus grave : le poste n'a pas de Java système, donc le repli
`JAVA_HOME` → `PATH` des scripts de service ne trouve rien et le service ne démarre plus.
Dans la lignée sans JRE, la perte est moins systématiquement fatale — le repli sur le
`PATH` fonctionne souvent — mais reste dommageable dès que le site a désigné un Java
précis. **Dans les deux cas, §2.1 reste le correctif n°1.**

##### Optimisation optionnelle : alléger les mises à jour de la lignée « avec JRE »

Chaque mise à jour de ce produit retransporte la JRE, alors qu'elle ne change
qu'exceptionnellement — coûteux sur des liaisons médiocres. Si cela devient gênant, la
piste est de sortir la JRE de `$INSTDIR\sidecar` vers un emplacement qui survit à la
désinstallation (`$PS_DataDir\runtime\jre`, à l'image de `config.json` et des
sauvegardes) et d'y faire pointer `jvm.java_home`. À traiter comme une amélioration
séparée, **pas comme un prérequis** : les deux produits fonctionnent sans elle.

#### 7.6.5 Deux blocages NSIS à lever avant toute automatisation

> ✅ **Traité le 2026-09-14** dans `installer-hooks/installer.nsh`. Le diagnostic
> ci-dessous est conservé : il documente le pourquoi du marqueur `services-installed.flag`
> et des gardes `${Silent}`.

Ils valent indépendamment de l'updater — ils dégradent déjà les mises à jour manuelles.

**a) Les services Windows sont supprimés à chaque mise à jour, et recréés seulement sur
clic.** `NSIS_HOOK_PREUNINSTALL` appelle `un.RemoveBackendService` et
`un.RemoveBatchService` ; la recréation, elle, est dans `NSIS_HOOK_POSTINSTALL` **derrière
un `MessageBox MB_YESNO`** (« Installer le backend et le pipeline nocturne comme services
Windows ? »). En mise à jour silencieuse, les deux services seraient supprimés et jamais
recréés. Même en mise à jour manuelle, un « Non » cliqué par réflexe laisse l'officine
sans backend ni pipeline nocturne. La recréation doit devenir **inconditionnelle lorsque
des services existaient avant la mise à jour** ; la question ne se pose qu'en première
installation.

**b) Aucun `MessageBox` n'est protégé par `${Silent}`.** Il n'y a pas un seul `IfSilent`
dans `installer.nsh`, alors que `NSIS_HOOK_POSTINSTALL` se termine par le `MB_YESNO`
ci-dessus puis un `MB_OK` récapitulatif. En NSIS, le mode silencieux supprime les pages
mais **pas les `MessageBox`** : une mise à jour `passive` ou `quiet` resterait bloquée sur
une boîte modale, éventuellement masquée, application déjà fermée.

Point favorable en revanche : la branche « MODE MISE À JOUR » (`installer.nsh:583`) est
déjà correcte — `config.json` préservé, assistant base de données non relancé. C'est la
partie délicate, et elle est faite.

#### 7.6.5 bis — Réversibilité : le dispositif doit rester optionnel

Exigence posée en cours de réalisation, et qui a orienté chaque brique : **il doit être
possible de revenir au fonctionnement actuel** — installation Tauri sans clés — si la
signature s'avère contraignante. Ce n'est pas une intention, c'est une propriété
vérifiable à chaque maillon :

| Maillon | Comportement sans clés / sans artefact |
| --- | --- |
| `tauri:build`, `tauri:build:bundled[-jre]` | Ne signent rien, ne réclament aucune clé. Inchangés. |
| `scripts/prepare-client-update.js` | Étape facultative. Non jouée → `src-tauri/updates/` ne contient que son README. |
| Ressource `updates/*` | Toujours satisfaite grâce au README, donc le build serveur ne casse jamais. |
| `NSIS_HOOK_POSTINSTALL` | Ne dépose rien s'il n'y a pas d'`.exe` embarqué ; trace la raison. |
| `ClientUpdateService` | Répertoire absent ou vide → rien n'est publié. Aucune de ces situations n'est une erreur. |
| `/api/updates/**` | Répond `204 No Content`, que le poste client interprète comme « à jour ». |

La désactivation d'un relais déjà en place se fait en vidant le répertoire `updates`
sous `%PROGRAMDATA%` du poste serveur.

#### 7.6.6 Ce qui reste manuel — et c'est normal

- La **première** installation d'un poste client : il faut y aller une fois ; seules les
  mises à jour suivantes sont automatiques.
- La mise à jour du poste serveur (étape 1).
- Une officine mono-poste : l'updater ne lui apporte rien, elle reste en manuel.

#### 7.6.7 Découpage

| Étape | Contenu                                                                                              | Remarque                                                                       |
| ----- | ---------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| 0     | ✅ **Fait** — §7.6.5 : recréation des services pilotée par un marqueur, `MessageBox` guardés par `${Silent}` | Utile immédiatement, même sans updater ; corrigeait un risque de mise à jour manuelle |
| 1     | ✅ **Fait** — §2.1 (`jvm.java_home` préservé) + garde anti-croisement de lignées et nommage des artefacts (§7.6.4) | Protège les deux produits serveur, indépendamment de l'updater                    |
| 2     | ❌ **Abandonné** — socle de signature minisign : retiré à la demande de l'éditeur, le gain ne justifiant pas la gestion de clés (voir l'encadré du §7.6.2) | Authenticode reste envisageable séparément (§4.7) |
| 3     | ✅ **Fait** — installeur client embarqué dans les **deux** installeurs serveur, dépôt dans `updates/`, routes backend `/api/updates/**` | Cœur du dispositif ; identique dans les deux lignées                  |
| 4     | Plugin updater sur le build client, permission `updater:default`, IHM de notification                 | Bénéfice immédiat, risque faible : un poste client en échec ne coupe personne     |
| 5     | *(optionnel)* JRE relogée hors de `$INSTDIR` pour alléger les mises à jour de la lignée « avec JRE »  | Confort de transfert, pas une correction                                          |

L'étape 4 seule couvre déjà le cas le plus pénible : l'officine à plusieurs postes où un
correctif front impose aujourd'hui une tournée des machines.

---

## 8. Points examinés et **non retenus**

### 8.1 Arrêter le backend à la fermeture de la fenêtre — **écarté volontairement**

Une revue naïve signale que `main.rs` n'a ni `on_window_event` ni
`RunEvent::ExitRequested`, que le `CommandChild` est droppé aussitôt après `child.pid()`,
et que `tauri-plugin-shell` n'implémente pas `Drop` pour tuer l'enfant — donc que fermer
PharmaSmart laisse une JVM « orpheline ».

**C'est le comportement voulu.** Le backend du poste serveur sert également les postes
clients du LAN (§0) : le couper à la fermeture de la fenêtre interromprait les ventes en
cours sur les autres postes. Le processus n'est d'ailleurs pas réellement orphelin — au
relancement, `try_connect_existing` s'y raccroche.

Deux points de confort restent toutefois ouverts, sans remettre en cause la décision :

- **Documenter** cette intention dans `backend_manager.rs`, juste au-dessus du `spawn`,
  pour qu'une prochaine revue ne « corrige » pas le comportement.
- **Conserver le `CommandChild`** dans `BackendState` plutôt que de le laisser tomber :
  cela permettrait un `child.kill()` propre lors d'un arrêt **explicitement demandé** par
  l'utilisateur, au lieu du `taskkill /F` actuel. L'arrêt reste déclenché uniquement par
  l'IHM, jamais par la fermeture de la fenêtre.

### 8.2 Introduire un champ `poste.role` dans `config.json` — **écarté**

Une première version de ce plan proposait de rendre le rôle du poste explicite
(`"poste": { "role": "serveur" | "client" }`). **Écarté :** le déploiement encode déjà
l'information, et il fonctionne.

- Le rôle est déterminé par **l'installeur choisi** : build `bundled` / `bundled-jre`
  (JAR + JRE + services Windows) sur le poste serveur, build standard sur les postes
  clients. `installer.nsh:447-469` conditionne l'installation des services à la présence
  des JAR, donc un poste client ne se retrouve pas avec un service mort.
- L'adresse du serveur est portée par `backend-url.txt`, mécanisme simple, éditable sans
  outil, et déjà en production.

Ajouter un `role` créerait une seconde source de vérité, désynchronisable de la première
(un poste déclaré « serveur » sans JAR, ou l'inverse), pour un gain nul sur un
déploiement qui marche. Ce qui manque n'est pas la déclaration du rôle mais le
**diagnostic** quand la configuration est fausse — c'est l'objet du §6, ramené à cela.

### 8.3 Refonte de la stratégie d'ouverture CDC

Le code d'ouverture de `scanner.rs` (un essai, primer DTR, un dernier essai, pas de
back-off interne) paraît inhabituel, mais la justification en commentaire est solide :
multiplier les tentatives entretient l'état bloqué du firmware. Ne pas y toucher sans
matériel de test réel.

---

## 9. Ordre d'exécution

| Ordre | Chantier                                                             | Motif                                                                                                                                                              |
| ----- | -------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1     | ✅ **Fait** — §2.1 : format `jvm`                                     | Seule anomalie qui casse déjà des installations en clientèle : `config.json` est le fichier de configuration des deux services Windows, et l'application en détruit une partie |
| 2     | §2.3 + §2.2 — fusion `lib.rs` / `main.rs`                            | Répare la détection HID au passage ; supprime une source de divergence permanente                                                                                      |
| 3     | §3 — journalisation fichier + hook de panic + `lock()` non panicant  | Prérequis de diagnostic : sans cela, aucun des points suivants n'est observable chez un client                                                                          |
| 4     | §4.1–4.3, §4.5 — secrets et IP de dev                                | Rotation à faire indépendamment du calendrier de livraison                                                                                                             |
| 5     | §5.1–5.3 — écriture atomique et parsing explicite                    | Protège la configuration des services, dont le mot de passe DB                                                                                                         |
| 6     | **§7.6.5 — services recréés inconditionnellement, `MessageBox` guardés `${Silent}`** | **Risque actif dès aujourd'hui** : toute mise à jour supprime les deux services Windows et ne les recrée que sur clic. Ne dépend d'aucun autre chantier |
| 7     | §2.5 — redémarrage réel du service Windows                           | Le bouton actuel ment dans le cas nominal ; corrige aussi le message sur poste client                                                                                  |
| 8     | §7.1, §7.2 — `Cargo.lock` committé, CI `fmt` / `clippy` / `test`     | Empêche la réapparition silencieuse des régressions ci-dessus                                                                                                          |
| 9     | §7.6 — relais de mise à jour LAN (étapes 1 à 4)                      | Le meilleur rapport effort / valeur à moyen terme : supprime la tournée des postes à chaque correctif front                                                            |
| 10    | §6 — diagnostic de la configuration poste client                     | Confort d'installation et de support ; dépend de §3 pour la partie journalisation                                                                                      |
| 11    | §4.4, §4.7 — CSP, signature Authenticode                             | §4.7 est aussi un prérequis de §7.6 ; à planifier avec [PLAN-HTTPS-ET-CERTIFICATS.md](PLAN-HTTPS-ET-CERTIFICATS.md)                                                    |
| 12    | §2.4, §5.4, §5.5, §7.3–7.5 — port obsolète, cache config, instance unique, tests, dette | Amélioration continue                                                                                                                            |
