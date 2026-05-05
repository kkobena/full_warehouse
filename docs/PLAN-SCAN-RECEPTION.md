# Plan Scan Réception — Points restants à implémenter

> **Date :** 1 mai 2026
> **Extrait de :** `PLAN-AMELIORATION-RECEPTION-BL.md`
> **Périmètre :** Intégration douchette scanner dans le module réception BL
> **Principe :** tous les items du plan principal `PLAN-AMELIORATION-RECEPTION-BL.md` marqués ✅
> sont exclus — ce document ne contient que les points **non encore réalisés**.

---

## Légende

| Symbole | Sens |
|---|---|
| 🔴 | Priorité haute — bloquant ou impact utilisateur fort |
| 🟡 | Priorité moyenne — amélioration significative |
| 🟢 | Priorité basse — nice-to-have / optionnel |
| 🏗️ | Nécessite du backend Java |
| 🎨 | Frontend Angular uniquement |
| 🦀 | Nécessite du code Rust (Tauri) |
| ⚙️ | Configuration uniquement (zéro code) |
| ✅ | Réalisé |

---

## Tableau de bord — points scan restants

| ID | Description | Priorité | Type | Sprint | Statut |
|---|---|---|---|---|---|
| **AX-14** | Parser AI 37 DataMatrix (quantité N > 1) | 🟡 | 🏗️🎨 | 4 | 🔲 |
| **SC-01** | Rust `list_serial_ports_detailed` + `start_scanner_listener` + `send_to_display` + `is_port_connected` + `check_ports_connection` | 🔴 | 🦀 | 5 | ✅ |
| ~~SC-02~~ | ~~Fusionné dans SC-01~~ | — | — | — | — |
| **SC-03** | Angular `setupBarcodeScanner()` CDC Tauri via PosteDevice ou fallback HID TIMING | 🔴 | 🎨 | 5 | ✅ |
| **SC-04** | PosteDevice (multi-périphériques par poste) + UI config + détection auto Tauri | 🔴 | 🎨🦀🏗️ | 5 | ✅ |
| **SC-04b** | Déplacer le module `entities/poste` → `features/settings/feature/poste` (nouveau pattern) | 🟡 | 🎨 | 5 | ✅ |
| **SC-05** | Configuration STX/ETX (optionnel — zéro code) | 🟢 | ⚙️ | — | 🔲 |
| **SC-06** | Template HTML form-poste : section gestion des devices (UI tableau devices) | 🟡 | 🎨 | 6 | ✅ |
| **SC-07** | Auto-détection device actif au démarrage (is_port_connected → fallback) | 🟡 | 🎨 | 6 | ✅ |
| **AX-23f** | Recherche catalogue multi-code lors scan inconnu | 🟢 | 🏗️ | Backlog | 🔲 |
| **AX-Cam** | Scanner caméra `@zxing/ngx-scanner` (sans douchette) | 🟢 | 🎨 | Backlog | 🔲 |
| **AX-SSCC** | Scan AI 00 (SSCC palette) — réception palette entière | 🟢 | 🏗️🎨 | Backlog | 🔲 |

---

## Sprint 4 — Scan DataMatrix avancé

### AX-14 · Parser AI 37 DataMatrix — quantité N dans le code scanné 🟡 🏗️🎨

**Contexte :** Les codes DataMatrix GS1 peuvent encoder la quantité dans l'AI 37
(ex : une boîte contenant 30 comprimés encode `(37)30`). Actuellement, chaque scan
incrémente toujours de +1. Si l'AI 37 est présent, utiliser la valeur encodée.

**Ce qu'il faut faire :**

**Backend — `DataMatrixParserService.java`**
```java
// Extraire l'AI 37 dans la méthode parse()
// AI 37 = FEFO quantity / quantité de l'unité de mesure
String ai37 = extractAi(raw, "37");
int scannedQty = 1;
if (ai37 != null && !ai37.isBlank()) {
    try { scannedQty = Integer.parseInt(ai37.strip()); }
    catch (NumberFormatException ignored) {}
}
// Inclure scannedQty dans DataMatrixInfo
```

**Backend — `ReceptionScanResultDTO.java`**
```java
private int scannedQty = 1; // nouveau champ
```

**Backend — `StockEntryServiceImpl.processScanReception()`**
```java
// Remplacer le +1 fixe par scannedQty
int newQty = (line.getQuantityReceived() == null ? 0 : line.getQuantityReceived())
           + parsed.getScannedQty(); // au lieu de + 1
```

**Frontend — `commande-received.component.ts`**
```typescript
// Dans onScanReception() → result.found
updated.quantityReceived = (updated.quantityReceived ?? 0) + (result.scannedQty ?? 1);
```

**Effort :** ~20 lignes Java + ~5 lignes TypeScript.

---

## Sprint 5 — Infrastructure scanner Tauri 🦀 ✅ TERMINÉ

> **Résumé des réalisations :**
>
> L'architecture a évolué du plan initial (colonnes scanner sur Poste) vers un modèle
> **multi-périphériques par poste** via l'entité `PosteDevice`. Cette approche est supérieure
> car elle permet de configurer N devices par poste avec un seul actif par type.

### SC-01 · Commandes Rust scanner.rs ✅

**Fichier :** `src-tauri/src/scanner.rs`

**Commandes implémentées :**

| Commande | Description |
|---|---|
| `list_serial_ports_detailed` | Liste ports avec VID/PID, manufacturer, product, suggestedRole, genericAdapter |
| `start_scanner_listener` | Lit le port CDC, émet événement Tauri à chaque code complet |
| `stop_scanner_listener` | Arrête la lecture |
| `send_to_display` | Envoie un message ESC/POS sur l'afficheur |
| `is_port_connected` | Vérifie si un port COM est présent dans le système |
| `check_ports_connection` | Batch : vérifie N ports + retourne les infos device |

**Améliorations par rapport au plan initial :**
- `suggest_role` enrichi avec détection **VID/PID** (plus fiable que les noms produits)
- 30+ fabricants/modèles reconnus (Honeywell, Zebra, Datalogic, Newland, Opticon, Mindeo...)
- Flag `genericAdapter` pour signaler les convertisseurs FTDI/CH340/Prolific/CP210x
- Commandes `is_port_connected` / `check_ports_connection` pour vérification runtime

---

### SC-03 · Angular setupBarcodeScanner() — CDC ou HID ✅

**Fichier :** `commande-received.component.ts`

**Implémentation :**
```typescript
private setupBarcodeScanner(): void {
  if (this.tauriPrinterService.isRunningInTauri()) {
    const poste = this.configurationService.getCurrentPoste?.();
    if (poste?.id) {
      // Lecture du device SCANNER actif via PosteDeviceService
      this.posteDeviceService.getActiveDevice(poste.id, 'SCANNER')
        .subscribe(res => {
          const device = res.body;
          if (device?.portName) {
            this.setupTauriCdcScanner(device.portName, device.baudRate ?? 9600);
          } else {
            this.setupHidTimingFallback();
          }
        });
      return;
    }
  }
  this.setupHidTimingFallback();
}
```

---

### SC-04 · Architecture PosteDevice (multi-périphériques) ✅

**Changement majeur vs plan initial :** Au lieu d'ajouter `scannerPort` directement sur `Poste`,
une entité dédiée `PosteDevice` gère N configurations par poste.

#### Entités et API

| Fichier | Rôle |
|---|---|
| `domain/PosteDevice.java` | Entité JPA (poste_id, device_type, port_name, baud_rate, vid, pid, manufacturer, active...) |
| `domain/enumeration/DeviceType.java` | Enum : `SCANNER`, `DISPLAY`, `PRINTER` |
| `repository/PosteDeviceRepository.java` | Queries : findByPoste, findActive, deactivateAllByType |
| `service/settings/PosteDeviceService.java` | Interface service |
| `service/settings/PosteDeviceServiceImpl.java` | Implémentation avec auto-activation premier device |
| `web/rest/settings/PosteDeviceResource.java` | REST : `/api/postes/{id}/devices` |
| `db/migration/V1.6.1__poste_device_multi_peripherique.sql` | Migration + import données existantes |

#### API REST

| Méthode | URL | Description |
|---|---|---|
| `GET` | `/api/postes/{id}/devices?type=SCANNER` | Liste devices d'un type |
| `GET` | `/api/postes/{id}/devices/active?type=SCANNER` | Device actif pour ce type |
| `POST` | `/api/postes/{id}/devices` | Ajouter un device |
| `PUT` | `/api/postes/{id}/devices/{deviceId}` | Modifier un device |
| `PUT` | `/api/postes/{id}/devices/{deviceId}/activate` | Activer (désactive les autres du même type) |
| `DELETE` | `/api/postes/{id}/devices/{deviceId}` | Supprimer |

#### Frontend

| Fichier | Rôle |
|---|---|
| `shared/model/poste-device.model.ts` | Interfaces IPosteDevice, ISerialPortDetail, IPortConnectionStatus |
| `entities/poste/poste-device.service.ts` | Service HTTP CRUD devices |
| `shared/services/tauri-device-detection.service.ts` | Service Tauri (listPorts, isConnected, checkBatch) |

#### Flux de fonctionnement

```
┌─────────────────────────────────────────────────────────┐
│ CONFIGURATION (une seule fois par l'utilisateur)        │
│                                                         │
│ 1. Tauri liste les ports → listSerialPorts()            │
│ 2. L'utilisateur ajoute un device → POST /devices      │
│ 3. L'utilisateur choisit le préféré → PUT /activate     │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ RUNTIME (à chaque démarrage / utilisation)              │
│                                                         │
│ 1. GET /devices/active?type=SCANNER → device préféré    │
│ 2. Tauri vérifie → isPortConnected(device.portName)     │
│ 3. Si connecté → utiliser (start_scanner_listener)      │
│ 4. Si pas connecté → fallback HID TIMING clavier        │
└─────────────────────────────────────────────────────────┘
```

---

### SC-04b · Déplacement entities/poste → features/settings ✅

Le module existe désormais dans `features/settings/feature/poste/` avec :
- `form-poste.component.ts` — formulaire simplifié (id, name, posteNumber, address)
- `poste.service.ts` — service HTTP
- Détection Tauri intégrée (listSerialPorts, testScanner, testDisplay)

---

## Sprint 6 — Finalisation UI et robustesse

### SC-06 · Template HTML form-poste : section gestion des devices 🟡 🎨

**Contexte :** Le `FormPosteComponent` dans `features/settings/` a les méthodes TypeScript
pour lister les ports, tester scanner/afficheur, et charger les devices. Il manque le
**template HTML** avec la section de gestion des périphériques.

**Ce qu'il faut faire :**

```html
<!-- Section Périphériques (affichée uniquement en mode édition d'un poste existant + Tauri) -->
@if (entity?.id && isTauri()) {
  <p-card header="📟 Périphériques série">

    <!-- Bouton actualiser -->
    <div class="flex justify-content-between align-items-center mb-3">
      <span class="text-sm text-secondary">
        {{ detectedPorts().length }} port(s) détecté(s)
      </span>
      <p-button label="Actualiser" icon="pi pi-refresh"
                [loading]="portsLoading()" severity="secondary" size="small"
                (onClick)="refreshPorts()" />
    </div>

    <!-- Tableau des devices configurés -->
    @if (devices().length > 0) {
      <table class="w-full mb-3">
        <thead>
          <tr>
            <th>Type</th>
            <th>Port</th>
            <th>Label</th>
            <th>Actif</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          @for (device of devices(); track device.id) {
            <tr>
              <td><p-tag [value]="device.deviceType" /></td>
              <td>{{ device.portName }}</td>
              <td>{{ device.label ?? device.productName ?? '—' }}</td>
              <td>
                @if (device.active) {
                  <i class="pi pi-check-circle text-green-500"></i>
                } @else {
                  <p-button icon="pi pi-bolt" severity="secondary" size="small"
                            pTooltip="Activer ce périphérique"
                            (onClick)="activateDevice(device)" />
                }
              </td>
              <td>
                @if (device.deviceType === 'SCANNER') {
                  <p-button icon="pi pi-barcode" severity="info" size="small"
                            pTooltip="Tester"
                            (onClick)="testScanner(device.portName, device.baudRate ?? 9600)" />
                }
                @if (device.deviceType === 'DISPLAY') {
                  <p-button icon="pi pi-desktop" severity="info" size="small"
                            pTooltip="Tester"
                            (onClick)="testDisplay(device.portName)" />
                }
                <p-button icon="pi pi-trash" severity="danger" size="small"
                          (onClick)="deleteDevice(device)" />
              </td>
            </tr>
          }
        </tbody>
      </table>
    }

    <!-- Ports détectés non encore configurés -->
    @if (detectedPorts().length > 0) {
      <h5>Ports disponibles</h5>
      <div class="flex flex-column gap-2">
        @for (port of detectedPorts(); track port.portName) {
          <div class="flex align-items-center gap-2 p-2 surface-ground border-round">
            <span class="font-mono">{{ port.portName }}</span>
            <span class="text-sm text-secondary">{{ port.product ?? port.manufacturer ?? '' }}</span>
            @if (port.suggestedRole) {
              <p-tag [value]="roleLabel(port.suggestedRole)" severity="info" />
            }
            @if (port.genericAdapter) {
              <p-tag value="Adaptateur" severity="warn" />
            }
            <div class="ml-auto">
              <p-select [options]="deviceTypeOptions" placeholder="Assigner rôle…"
                        (onChange)="addDevice(port, $event.value)" />
            </div>
          </div>
        }
      </div>
    }

    <!-- Guide si aucun port détecté -->
    @if (detectedPorts().length === 0 && !portsLoading()) {
      <div class="p-3 surface-ground border-round text-sm">
        <p><strong>💡 Aucun port série détecté.</strong></p>
        <p>Votre douchette est probablement en mode clavier (HID).</p>
        <p>Pour activer le mode CDC :</p>
        <ol>
          <li>Scannez l'étiquette <strong>"USB Virtual COM"</strong> ou <strong>"RS-232 Emulation"</strong>
              dans le manuel de votre douchette</li>
          <li>Cliquez <strong>Actualiser</strong></li>
        </ol>
        <p>Le mode clavier (HID) reste fonctionnel en fallback.</p>
      </div>
    }

    <!-- Résultat tests -->
    @if (scannerTestResult() === 'waiting') {
      <p-tag value="⏳ Scannez un produit..." severity="warn" class="mt-2" />
    }
    @if (scannerTestResult() === 'ok') {
      <p-tag value="✅ Scanner OK" severity="success" class="mt-2" />
    }
    @if (scannerTestResult() === 'error') {
      <p-tag value="❌ Aucun scan reçu (timeout 5s)" severity="danger" class="mt-2" />
    }
    @if (displayTestResult() === 'ok') {
      <p-tag value="✅ Afficheur OK" severity="success" class="mt-2" />
    }
    @if (displayTestResult() === 'error') {
      <p-tag value="❌ Erreur afficheur" severity="danger" class="mt-2" />
    }
  </p-card>
}
```

**Méthodes à ajouter dans `FormPosteComponent` :**
```typescript
protected readonly deviceTypeOptions = [
  { label: 'Scanner', value: 'SCANNER' },
  { label: 'Afficheur', value: 'DISPLAY' },
  { label: 'Imprimante', value: 'PRINTER' },
];

protected activateDevice(device: IPosteDevice): void {
  if (device.id && this.entity?.id) {
    this.posteDeviceService.activate(this.entity.id, device.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadDevices(this.entity.id!));
  }
}

protected deleteDevice(device: IPosteDevice): void {
  if (device.id && this.entity?.id) {
    this.posteDeviceService.delete(this.entity.id, device.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadDevices(this.entity.id!));
  }
}

protected addDevice(port: ISerialPortDetail, deviceType: DeviceType): void {
  if (!this.entity?.id || !deviceType) return;
  const newDevice: IPosteDevice = {
    posteId: this.entity.id,
    deviceType,
    portName: port.portName,
    label: port.product ?? port.manufacturer ?? undefined,
    baudRate: 9600,
    vid: port.vid,
    pid: port.pid,
    manufacturer: port.manufacturer ?? undefined,
    productName: port.product ?? undefined,
    serialNumber: port.serialNumber ?? undefined,
    active: false,
  };
  this.posteDeviceService.create(this.entity.id, newDevice)
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe(() => this.loadDevices(this.entity.id!));
}
```

**Effort :** ~80 lignes HTML + ~40 lignes TypeScript.

---

### SC-07 · Auto-détection device actif au démarrage ✅

**Contexte :** Au démarrage du module réception (ou vente), le système doit vérifier que
le device actif configuré est effectivement connecté. Si pas connecté, tenter les autres
devices du même type, sinon fallback HID.

**Fichier :** `commande-received.component.ts` (et potentiellement un service partagé)

**Ce qu'il faut faire :**

```typescript
/**
 * Stratégie de sélection du scanner au démarrage :
 * 1. Récupérer le device actif (GET /active?type=SCANNER)
 * 2. Vérifier s'il est connecté (Tauri: is_port_connected)
 * 3. Si connecté → l'utiliser
 * 4. Si pas connecté → récupérer tous les devices SCANNER du poste
 * 5. Pour chacun, vérifier la connexion (check_ports_connection en batch)
 * 6. Utiliser le premier connecté trouvé
 * 7. Si aucun connecté → fallback HID TIMING
 */
private async setupBarcodeScannerWithFallback(): Promise<void> {
  if (!this.tauriDeviceService.isTauriAvailable()) {
    this.setupHidTimingFallback();
    return;
  }

  const poste = this.configurationService.getCurrentPoste?.();
  if (!poste?.id) {
    this.setupHidTimingFallback();
    return;
  }

  try {
    // 1. Device préféré
    const activeRes = await firstValueFrom(this.posteDeviceService.getActiveDevice(poste.id, 'SCANNER'));
    const activeDevice = activeRes.body;

    if (activeDevice?.portName) {
      // 2. Vérifier si connecté
      const connected = await this.tauriDeviceService.isPortConnected(activeDevice.portName);
      if (connected) {
        this.setupTauriCdcScanner(activeDevice.portName, activeDevice.baudRate ?? 9600);
        return;
      }
    }

    // 4-5. Fallback : chercher un autre device SCANNER connecté
    const allRes = await firstValueFrom(this.posteDeviceService.fetchAll(poste.id, 'SCANNER'));
    const allDevices = allRes.body ?? [];
    const portNames = allDevices.map(d => d.portName!).filter(Boolean);

    if (portNames.length > 0) {
      const statuses = await this.tauriDeviceService.checkPortsConnection(portNames);
      const connectedDevice = allDevices.find(d =>
        statuses.some(s => s.portName === d.portName && s.connected)
      );

      if (connectedDevice?.portName) {
        // 6. Utiliser le premier connecté
        this.setupTauriCdcScanner(connectedDevice.portName, connectedDevice.baudRate ?? 9600);
        return;
      }
    }
  } catch (error) {
    console.warn('Erreur détection scanner, fallback HID:', error);
  }

  // 7. Aucun scanner CDC connecté
  this.setupHidTimingFallback();
}
```

**Effort :** ~50 lignes TypeScript.

---

### SC-05 · Configuration STX/ETX — optionnel ⚙️ 🟢

> ⚠️ **Ce point est totalement optionnel.** Le système fonctionne sans lui.
> STX/ETX élimine les faux positifs pour les utilisateurs qui veulent
> une détection parfaite sans timing — mais son absence ne dégrade pas l'expérience.

**Condition d'activation :** l'utilisateur doit :
1. Programmer sa douchette pour émettre STX (0x02) avant chaque code et ETX (0x03) après
2. Mettre `APP_SCANNER_MODE = STX_ETX` dans les paramètres applicatifs

**Effort si activé :** ~15 lignes TypeScript.

---

## Ordre de réalisation recommandé (mis à jour)

```
Sprint 4 — AX-14 (DataMatrix AI 37 quantité)                     🔲
  └─ Backend DataMatrixParserService + ReceptionScanResultDTO
  └─ Frontend onScanReception() utilise scannedQty

Sprint 5 — Infrastructure scanner Tauri                            ✅ TERMINÉ
  1. SC-04b · features/settings/feature/poste                      ✅
  2. SC-01 · scanner.rs (Rust) — 6 commandes                      ✅
  3. SC-04 · PosteDevice entity + API REST + services Angular      ✅
  4. SC-03 · setupBarcodeScanner() via PosteDeviceService          ✅

Sprint 6 — Finalisation UI et robustesse                          🔲
  1. SC-06 · Template HTML form-poste section devices              ~1 jour
  2. SC-07 · Auto-détection avec fallback intelligent              ✅
  ─ SC-05 · STX/ETX (uniquement si demandé par l'officine)

Sprint 4 (backend) — peut être fait en parallèle                  🔲
  AX-14 · DataMatrix AI 37 quantité N                             ~0,5 jour
```

---

## Architecture finale — vue d'ensemble

```
┌─────────────────────────────────────────────────────────────────────┐
│                         TAURI (Rust)                                  │
│  scanner.rs                                                          │
│  ├── list_serial_ports_detailed() → [SerialPortDetail]              │
│  ├── is_port_connected(portName) → bool                             │
│  ├── check_ports_connection([ports]) → [PortConnectionStatus]       │
│  ├── start_scanner_listener(port, baud, event) → émet événements    │
│  ├── stop_scanner_listener()                                         │
│  └── send_to_display(port, message, baud)                           │
│                                                                      │
│  suggest_role :                                                       │
│  ├── VID/PID (Honeywell 0x0C2E, Zebra 0x05E0, Datalogic 0x05F9…)  │
│  ├── Noms produits (30+ mots-clés)                                   │
│  ├── Fabricants (15+ heuristiques)                                   │
│  └── genericAdapter flag (FTDI, CH340, Prolific, CP210x)            │
└─────────────────────────────────────────────────────────────────────┘
          │ invoke / listen (événements Tauri)
          ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      ANGULAR (Frontend)                               │
│                                                                      │
│  TauriDeviceDetectionService (shared/services/)                      │
│  ├── isTauriAvailable()                                              │
│  ├── listSerialPorts()                                               │
│  ├── isPortConnected(port)                                           │
│  ├── checkPortsConnection([ports])                                   │
│  ├── startScannerListener(port, baud, event)                        │
│  ├── stopScannerListener()                                           │
│  └── sendToDisplay(port, msg, baud)                                  │
│                                                                      │
│  PosteDeviceService (entities/poste/)                                │
│  ├── fetchAll(posteId, type?)                                        │
│  ├── getActiveDevice(posteId, type)                                  │
│  ├── create(posteId, device)                                         │
│  ├── update(posteId, deviceId, device)                               │
│  ├── activate(posteId, deviceId)                                     │
│  └── delete(posteId, deviceId)                                       │
└─────────────────────────────────────────────────────────────────────┘
          │ HTTP REST
          ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      SPRING BOOT (Backend)                            │
│                                                                      │
│  PosteDeviceResource : /api/postes/{id}/devices                      │
│  PosteDeviceService → PosteDeviceRepository                          │
│                                                                      │
│  Table poste_device :                                                │
│  ├── id, poste_id, device_type (SCANNER/DISPLAY/PRINTER)            │
│  ├── port_name, label, baud_rate                                     │
│  ├── vid, pid, manufacturer, product_name, serial_number            │
│  ├── active (un seul par type par poste)                             │
│  └── last_connected_at, created_at                                   │
│                                                                      │
│  Contrainte : UNIQUE(poste_id, device_type, port_name)              │
│  Index partiel : WHERE active = true                                 │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Différences vs plan initial

| Aspect | Plan initial (SC-04 v1) | Implémentation finale |
|---|---|---|
| Stockage config | Colonnes sur `Poste` (scannerPort, scannerEnabled) | Entité `PosteDevice` séparée (one-to-many) |
| Multi-devices | ❌ Un seul scanner par poste | ✅ N devices par poste, 1 actif par type |
| Détection connexion | ❌ Non prévu | ✅ `is_port_connected` + `check_ports_connection` |
| Suggest rôle | Nom produit seulement | VID/PID + noms + fabricants + flag genericAdapter |
| localStorage | Utilisé comme fallback | ❌ Supprimé — tout en base PostgreSQL |
| Auto-activation HTTP | Envisagé (mark-connected) | ❌ Supprimé — Tauri vérifie en temps réel |
| Champs legacy Poste | scannerPort, customerDisplay, etc. | ✅ Supprimés — migrés vers poste_device |

---

## Références

| Fichier | Rôle |
|---|---|
| `docs/PLAN-AMELIORATION-RECEPTION-BL.md` | Plan complet (items ✅ inclus) |
| `docs/ANALYSE-MODULE-RECEPTION-BL.md` | Analyse détaillée avec code |
| `src-tauri/src/scanner.rs` | Commandes Rust (6 commandes) |
| `src-tauri/src/lib.rs` | Enregistrement des handlers |
| `domain/PosteDevice.java` | Entité JPA multi-périphériques |
| `domain/enumeration/DeviceType.java` | SCANNER, DISPLAY, PRINTER |
| `repository/PosteDeviceRepository.java` | Repository Spring Data |
| `service/settings/PosteDeviceService.java` | Interface service |
| `service/settings/PosteDeviceServiceImpl.java` | Implémentation |
| `web/rest/settings/PosteDeviceResource.java` | REST controller |
| `db/migration/V1.6.1__poste_device_multi_peripherique.sql` | Migration SQL |
| `shared/model/poste-device.model.ts` | Interfaces TS |
| `entities/poste/poste-device.service.ts` | Service HTTP Angular |
| `shared/services/tauri-device-detection.service.ts` | Service Tauri Angular |
| `features/settings/feature/poste/form-poste/form-poste.component.ts` | Formulaire poste + détection |
| `features/commande/feature/commande-received/commande-received.component.ts` | Intégration scanner réception |

---

*Plan révisé le 1 mai 2026 — Sprint 5 terminé — Architecture PosteDevice multi-périphériques*
