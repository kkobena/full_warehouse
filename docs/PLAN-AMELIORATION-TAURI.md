# Plan d'amélioration — Intégration Tauri

**Date :** 2026-05-22  
**Périmètre analysé :**
- `src-tauri/src/` (main.rs, printer.rs, scanner.rs, customer_display.rs, backend_manager.rs, config.rs)
- `src/main/webapp/app/core/tauri/backend-status.service.ts`
- `src/main/webapp/app/shared/services/tauri-printer.service.ts`
- `src/main/webapp/app/layouts/main/main.component.html`

---

## Vue d'ensemble

L'intégration Tauri couvre ~4 000 lignes Rust + ~2 500 lignes TypeScript Angular, déployée en production sur Windows pour un système POS pharmacie. L'architecture est fonctionnelle mais présente des axes d'amélioration sur la maintenabilité, la robustesse et la testabilité.

---

## Axe 1 — Architecture & Modularité (Priorité : P1)

### 1.1 Découpage de `main.rs` (336 lignes)

`main.rs` mélange lifecycle, enregistrement des commandes, plugins et setup. Refactorer en sous-modules :

```
src-tauri/src/
├── commands/
│   ├── printer.rs       # get_printers, print_image, print_escpos
│   ├── scanner.rs       # start/stop_scanner_listener, detect_scanner_usb_mode
│   ├── display.rs       # send_to_customer_display, test_customer_display_connection
│   ├── config.rs        # get_backend_url_command
│   └── health.rs        # check_backend_health, get_backend_status, restart/stop_backend
├── main.rs              # setup + plugin registration uniquement (~80 lignes)
└── ...
```

### 1.2 Découpage de `TauriPrinterService` Angular (640 lignes)

`TauriPrinterService` est un god-object couvrant imprimantes, afficheur client, périphériques et info système. Séparer en :

| Service actuel (tout-en-un) | Nouveau service |
|----------------------------|----------------|
| Gestion imprimantes | `PrinterService` |
| Afficheur client | `CustomerDisplayService` |
| Scanner / ports série | `DeviceService` |
| Hostname / IP / info système | `SystemInfoService` |
| Détection Tauri runtime | `TauriRuntimeService` (partagé) |

### 1.3 Abstraction dual-mode (bundled vs standard)

Le feature flag `bundled-backend` crée deux chemins divergents difficiles à tester. Introduire un trait unifié :

```rust
// Proposition
trait BackendProvider: Send + Sync {
    async fn get_status(&self) -> BackendStatus;
    async fn check_health(&self, url: &str) -> BackendHealthStatus;
    async fn restart(&self) -> Result<(), BackendError>;
    async fn stop(&self) -> Result<(), BackendError>;
}

struct BundledBackend { /* wraps backend_manager */ }
struct ExternalBackend { /* HTTP polling */ }
```

---

## Axe 2 — Sécurité de types IPC (Priorité : P1)

### Problème

Tous les appels Tauri sont des chaînes magiques non typées côté Angular. Un rename de commande Rust casse silencieusement le frontend :

```typescript
// Actuellement — string literal, aucune vérification compile-time
await invoke('get_backend_url_command')
await invoke('check_backend_health', { backendUrl })
await invoke('print_image', { imageData, printerName })
```

### Solution : tauri-specta

Intégrer [tauri-specta](https://github.com/oscartbeaumont/tauri-specta) pour générer des bindings TypeScript depuis les handlers Rust :

```rust
// Cargo.toml — ajouter
[dev-dependencies]
tauri-specta = { version = "2", features = ["derive", "typescript"] }
specta-typescript = "0.0.7"

// main.rs — export automatique des types
#[cfg(debug_assertions)]
tauri_specta::export::typescript(&builder, "../bindings.ts").unwrap();
```

```typescript
// Résultat : bindings.ts généré automatiquement
import * as commands from './bindings';

// Appel type-safe
const url = await commands.getBackendUrlCommand();
const health = await commands.checkBackendHealth({ backendUrl });
```

---

## Axe 3 — Polling du backend (Priorité : P2)

### Problème

**Mode standard** (`BackendStatusService`) : 60 tentatives × 500ms = 30s de latence maximale, intervalle fixe sans backoff.

```typescript
// backend-status.service.ts:119 — intervalle fixe
this.checkInterval = setInterval(async () => { ... }, 500);
```

**Mode bundled** (`backend_manager.rs`) : idem côté Rust, pas de backoff.

### Solution : backoff exponentiel

```typescript
// Algorithme suggéré
// Tentatives 1-5   : 500ms  (détection rapide démarrage Java)
// Tentatives 6-15  : 1 000ms
// Tentatives 16-30 : 2 000ms
// Tentatives 31+   : 5 000ms (backend clairement lent)
// Maximum total    : 3 min (360 tentatives normalisées)

private getDelay(attempt: number): number {
  if (attempt <= 5)  return 500;
  if (attempt <= 15) return 1000;
  if (attempt <= 30) return 2000;
  return 5000;
}
```

---

## Axe 4 — Gestion des erreurs (Priorité : P2)

### 4.1 Erreurs silencieuses Rust

De nombreux `.unwrap_or_default()` échouent silencieusement. Centraliser un enum d'erreur applicatif :

```rust
// types.rs — à enrichir
#[derive(Debug, thiserror::Error, Serialize)]
pub enum AppError {
    #[error("Imprimante introuvable : {name}")]
    PrinterNotFound { name: String },
    #[error("Port série indisponible : {port}")]
    SerialPortUnavailable { port: String },
    #[error("Backend indisponible après {attempts} tentatives")]
    BackendTimeout { attempts: u32 },
    // ...
}
```

### 4.2 Bridge logs Rust → Angular

Actuellement seuls les logs Java critiques sont bridgés (`backend-log`). Les erreurs Rust restent invisibles côté Angular en développement.

```rust
// Ajouter un subscriber tracing qui émet vers Tauri
// Émettre sur 'rust-log' en dev uniquement
app_handle.emit("rust-log", LogEntry { level, message, target })
```

---

## Axe 5 — Détection imprimante thermique (Priorité : P2)

### Problème

Détection par heuristique sur le nom (`tm-`, `rp-`, `thermal`, `pos`, `receipt`) — fragile si le fabricant utilise un nom générique.

```rust
// printer.rs — détection fragile
fn is_thermal_printer(name: &str) -> bool {
    let lower = name.to_lowercase();
    lower.contains("tm-") || lower.contains("thermal") || lower.contains("pos") // ...
}
```

### Solution

Interroger `DeviceCapabilities()` via Win32 pour vérifier la résolution réelle (imprimantes thermiques : 203 DPI ou 300 DPI, pas 600 DPI+) et le type de papier supporté. En fallback, conserver l'heuristique sur le nom.

---

## Axe 6 — Hot-reload de `config.json` (Priorité : P3)

### Problème

Toute modification du port JVM, de la heap, des crons ou de la configuration FNE nécessite un redémarrage complet de l'application.

### Solution

```rust
// Cargo.toml
notify = "6"

// config.rs — watcher sur config.json
fn watch_config(app_handle: AppHandle, config_path: PathBuf) {
    // Émettre 'config-changed' vers Angular à chaque modification
    app_handle.emit("config-changed", new_config);
}
```

Côté Angular, les services qui dépendent de la config s'abonnent à `listen('config-changed', ...)` et se réinitialisent sans redémarrage.

Ajouter aussi une **validation de schéma** au chargement (actuellement tous les champs sont `Option<T>` avec fallback silencieux).

---

## Axe 7 — Splash screen (Priorité : P2)

### Problème

`<jhi-backend-splash>` reste monté en permanence en mode Tauri, même une fois le backend `ready`.

```html
<!-- main.component.html:44 -->
@if (isTauriMode) {
  <jhi-backend-splash></jhi-backend-splash>
}
```

### Solution

Détruire le composant après `status === 'ready'` :

```html
@if (isTauriMode && !backendReady()) {
  <jhi-backend-splash (backendReady)="onBackendReady()"></jhi-backend-splash>
}
```

```typescript
// main.component.ts
backendReady = signal(false);

onBackendReady(): void {
  this.backendReady.set(true);
}
```

---

## Axe 8 — Impression sans PowerShell (Priorité : P3)

### Problème

L'impression sur imprimantes non-thermiques passe par un script PowerShell avec fichier temporaire — lent (~1s), risque de fichier orphelin, et maintenabilité faible.

```rust
// printer.rs — approche PowerShell actuelle
let ps_script = format!("Add-Type -AssemblyName System.Drawing; ...");
Command::new("powershell").arg(ps_script)...
```

### Solution

Remplacer par `windows-rs` (crate Microsoft officielle) pour appeler directement l'API COM d'impression sans subprocess ni fichier temporaire :

```toml
[target.'cfg(windows)'.dependencies]
windows = { version = "0.60", features = ["Win32_Graphics_Printing", "Win32_Graphics_Gdi"] }
```

---

## Axe 9 — Testabilité (Priorité : P3)

### Problème

Aucun test unitaire visible sur le code Rust. Le lifecycle backend, le scanner CDC, et l'imprimante sont du code système non testable en l'état.

### Solution

Abstraire les dépendances système derrière des traits injectables :

```rust
trait SerialPortOpener: Send + Sync {
    fn open(&self, port: &str, baud_rate: u32) -> Result<Box<dyn SerialPort>>;
}

trait PrinterApi: Send + Sync {
    fn list_printers(&self) -> Result<Vec<PrinterInfo>>;
    fn print_raw(&self, name: &str, data: &[u8]) -> Result<()>;
}

// En test : MockPrinterApi, MockSerialPortOpener
// En prod : WindowsPrinterApi, DefaultSerialPortOpener
```

---

## Résumé priorisé

| Priorité | Axe | Effort estimé | Bénéfice principal |
|----------|-----|--------------|-------------------|
| **P1** | Types IPC via tauri-specta | 2-3j | Élimine les bugs runtime silencieux |
| **P1** | Découpage TauriPrinterService | 1-2j | Maintenabilité immédiate |
| **P1** | Découpage main.rs | 1j | Lisibilité et navigation |
| **P2** | Backoff polling backend | 0.5j | UX startup améliorée |
| **P2** | Splash destroy après ready | 0.5j | Bonne pratique Angular, mémoire |
| **P2** | Détection imprimante fiable | 1-2j | Robustesse impression |
| **P2** | Logs Rust → Angular (dev) | 1j | Débogage facilité |
| **P3** | Hot-reload config.json | 3-4j | Confort opérationnel |
| **P3** | Impression sans PowerShell | 2-3j | Performance + fiabilité |
| **P3** | Trait-based testability | 3-5j | Qualité long terme |
| **P3** | Abstraction dual-mode | 2-3j | Testabilité lifecycle |

**Point de départ recommandé** : Axe 2 (tauri-specta) + Axe 1.2 (découpage TauriPrinterService) — faible risque, gain immédiat sur la maintenabilité et la sécurité des appels IPC.
