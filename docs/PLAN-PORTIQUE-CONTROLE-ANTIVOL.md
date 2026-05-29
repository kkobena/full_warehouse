# Plan d'Action — Portique de Contrôle Anti-Vol

**Projet :** Pharma-Smart  
**Date :** 2026-05-25  
**Besoin :** Détecter les produits quittant la pharmacie sans passage en caisse (vols)  
**Stack :** Spring Boot 4 · Angular 20 · Tauri (Rust) · PostgreSQL

---

## 1. Contexte et Besoin

L'objectif est de mettre en place un **système de détection de vol** capable de :

- Identifier en temps réel qu'un produit sort sans avoir été enregistré en vente
- Journaliser chaque événement (scan, alerte, validation) pour l'audit
- Afficher des alertes visuelles/sonores dans l'application Tauri du pharmacien
- Fonctionner même si aucune machine n'est démarrée (fail-safe hardware)
- Conserver une traçabilité complète exploitable dans les rapports existants

### Ce que le projet a déjà (atouts)

| Existant | Utilité pour le portique |
|---|---|
| `InventoryTransaction` (partitionné par date) | Journal d'audit unifié — y intégrer les scans portique |
| `DataMatrixParserServiceImpl` | Parse EAN-8/13, CIP-7/13, DataMatrix GS1 — réutilisable pour mapper produit ↔ tag |
| `LotStockLocation` | Traçabilité lot × storage (FEFO) — indique quel lot doit sortir |
| `Storage` multi-type (PRINCIPAL, RESERVE, RAYON) | Hiérarchie spatiale — localiser le portique |
| `StockEntryService`, `RepartitionStockService` | Points d'injection naturels |
| `jSerialComm` (déjà en dépendance) | Communication série avec lecteurs RFID caisse |
| Tauri + Spring Boot bundlé local | Dashboard et monitoring — pas le chef d'orchestre RFID |

---

## 2. Explication des Technologies Disponibles

### 2.1 EAS RF — Radio Fréquence 8.2 MHz

**Fonctionnement :**
1. Une étiquette RF adhésive est collée sur chaque produit
2. À la caisse, un désactivateur magnétique grille l'étiquette définitivement
3. Si le produit passe le portique avec l'étiquette active → alarme sonore (hardware pur)

**Avantages :**
- Coût faible : étiquettes ~0,05 €/unité, portique 1 500–3 000 €
- Fonctionne sans aucun PC, sans réseau — 100 % autonome
- Installation simple

**Limites :**
- Sait qu'*un* produit sort sans passage en caisse, mais pas *lequel*
- Aucune donnée exploitable dans le système informatique
- Pas de traçabilité, pas de rapport

**Intégration Pharma-Smart :** minimale — juste un log d'alarme physique

---

### 2.2 EAS AM — Acousto-Magnétique 58 kHz (Sensormatic)

**Fonctionnement :** identique au RF, technologie différente (aimant permanent neutralisé par impulsion)

**Avantages :**
- Taux de détection > 99 %
- Moins de faux positifs (cartes bancaires, téléphones)
- Autonome comme le RF

**Limites :** identiques au RF — pas d'identification du produit

**Coût portique :** 3 000–5 000 €

---

### 2.3 RFID UHF — 860–960 MHz ✅ RECOMMANDÉ

**Fonctionnement :**

```
[Produit] ── [Tag RFID unique : EPC E200-xxxx + User Memory]
     │
     ▼  à la réception / étiquetage
Spring Boot associe : tag_id ↔ Produit ↔ Lot en base
Spring Boot commande le writer RFID caisse : écrire User Memory vide
     │
     ▼  à la caisse (vente confirmée en DB)
Spring Boot confirme la vente en DB
Spring Boot commande le writer RFID : écrire "SOLD" dans User Memory du tag
Spring Boot envoie "SOLD" au portique (whitelist RAM)
     │
     ▼  à la sortie
[Portique RFID autonome] lit TOUS les tags passant (même dans un sac fermé)
  ├─ User Memory du tag == "SOLD" → silence (aucun PC requis)
  ├─ Tag dans whitelist RAM locale → silence
  └─ Ni l'un ni l'autre → ALARME HARDWARE IMMÉDIATE
```

**Avantages :**
- Identifie exactement *quel* produit, *quand*, par *quel* portique
- Lecture jusqu'à 200 tags/seconde sans contact visuel (sac fermé)
- Intégration complète avec le système de stock
- Traçabilité totale exploitable en rapports
- Fonctionne sans PC grâce à la mémoire du tag

**Coût :** tags avec user memory 0,15–0,35 €/unité · portique 5 000–15 000 €

---

### 2.4 RFID HF + NFC — 13.56 MHz

**Usage :** médicaments à haute valeur, stupéfiants, traçabilité FMD (AI 21 DataMatrix)  
**Portée :** ~10 cm — adapté au scan manuel, pas au portique de passage  
**Complémentaire** à l'option RFID UHF pour les produits sensibles

---

### Tableau de choix

| Besoin | Technologie recommandée |
|---|---|
| Budget limité, alarme simple, 100 % autonome | EAS RF (Option 2.1) |
| Meilleure fiabilité d'alarme, 100 % autonome | EAS AM (Option 2.2) |
| Savoir quel produit sort + intégration système | RFID UHF (Option 2.3) ✅ |
| Stupéfiants / médicaments contrôlés | RFID HF complémentaire (Option 2.4) |

---

## 3. Principe Fondamental : Hardware-First

> **Le portique déclenche toujours l'alarme par défaut.**  
> **Le logiciel supprime l'alarme uniquement si le produit est vendu.**

```
❌ MAUVAISE approche (software-first) :
   Portique silencieux → logiciel déclenche l'alarme
   Problème : PC éteint = aucune alarme = vol non détecté

✅ BONNE approche (hardware-first) :
   Portique sonne sur tout tag → logiciel coupe l'alarme si produit vendu
   PC éteint = alarme sur tout = fail-safe garanti
```

### Scénarios de résilience

| Situation | Comportement |
|---|---|
| Tout fonctionne | Validation triple : tag memory + whitelist RAM + DB |
| PC éteint | Mémoire du tag suffit → silence si SOLD écrit |
| Réseau coupé | Mémoire du tag suffit → silence si SOLD écrit |
| Portique redémarre (RAM vide) | Mémoire du tag suffit → silence si SOLD écrit |
| Vieux tags sans user memory + PC éteint | Alarme sur tout → fail-safe ✅ |
| Coupure de courant totale | Batterie intégrée portique (2–4h) → alarme continue ✅ |

---

## 4. Les 3 Niveaux de Mémoire

### Niveau 1 — Mémoire du tag RFID (sur le produit lui-même)

```
┌─────────────────────────────────────────────────────┐
│              TAG RFID UHF Gen2                       │
│                                                     │
│  EPC Memory (96 bits — obligatoire)                 │
│  → identifiant unique du tag : E200-ABCD-1234       │
│                                                     │
│  User Memory (512 bits à 8 KB — optionnelle)        │
│  → écrit à la réception  : { status: "ACTIVE" }     │
│  → écrit à la vente      : { status: "SOLD",        │
│                              saleId: 12345,          │
│                              ts: "2026-05-25T14:32" }│
│                                                     │
│  Reserved Memory (64 bits)                          │
│  → mots de passe d'accès                            │
└─────────────────────────────────────────────────────┘
```

**Avantage clé :** la validation à la sortie ne nécessite **aucun PC, aucun réseau**.  
Le portique lit directement la mémoire du tag — si `status == "SOLD"` → silence immédiat.

**Coût supplémentaire :** ~0,05–0,10 € par tag (choisir tags avec user memory).

---

### Niveau 2 — Mémoire du portique (RAM + Flash embarquée)

| Modèle | RAM | Flash | OS embarqué |
|---|---|---|---|
| Zebra FX7500 | 256 MB | 512 MB | Linux |
| Impinj Speedway R420 | 128 MB | 256 MB | Linux |
| Feig ID ISC | 64 MB | 128 MB | RTOS |

**Ce qu'il stocke :**
- **RAM volatile** : whitelist temporaire des tags vendus récemment (synchronisée depuis Spring Boot)
- **Flash** : configuration, firmware, quelques milliers d'événements en log

**Limite :** la RAM est perdue à chaque redémarrage → reconstruite depuis Spring Boot au démarrage.  
Si Spring Boot est indisponible → whitelist vide → alarme sur tout (fail-safe correct).

---

### Niveau 3 — Base de données Spring Boot (PostgreSQL)

Source de vérité pour l'audit, les rapports et la reconstruction de la whitelist portique.  
N'est pas sur le chemin critique de l'alarme — sert à l'intelligence et à la traçabilité.

---

## 5. Qui Écrit dans la Mémoire du Tag ?

### Rôle de chaque composant

| Composant | Rôle |
|---|---|
| **Angular / Tauri** | Interface utilisateur uniquement — déclenche la vente, affiche le dashboard |
| **Spring Boot** | Chef d'orchestre — confirme la vente en DB, puis commande l'écriture RFID |
| **Lecteur RFID caisse** | Exécute l'écriture physique dans la mémoire du tag sur ordre de Spring Boot |
| **Portique** | Lit la mémoire du tag et décide alarme ou silence de façon autonome |

### Pourquoi pas Tauri ?

- Tauri peut être fermé avant confirmation de l'écriture
- La vente est validée dans Spring Boot — c'est lui qui sait si elle a réellement abouti en DB
- Si l'écriture échoue (tag hors portée), Spring Boot peut gérer le retry et l'alerte
- Le lecteur RFID caisse peut être connecté en réseau, pas forcément sur la machine Tauri
- Séparation des responsabilités : UI ≠ pilotage matériel

### Flux complet à la caisse

```
Caissier scanne les produits et valide le paiement (Angular dans Tauri)
        │
        ▼
POST /api/sales → Spring Boot traite et enregistre la vente en DB
        │
        ▼  seulement si la vente est CONFIRMÉE (statut CLOSED en DB)
Spring Boot envoie commande WRITE au lecteur RFID caisse
via LLRP/TCP ou HTTP REST du lecteur :
  WRITE { tagEpc: "E200-ABCD-1234",
          userMemory: { status:"SOLD", saleId:12345, ts:"2026-05-25T14:32" } }
        │
        ├──► Lecteur RFID caisse écrit dans la mémoire du tag
        │
        └──► Spring Boot envoie aussi au portique :
             SOLD_NOTIFICATION { tagId: "E200-ABCD-1234", ttl: 300s }
             → portique ajoute le tag en whitelist RAM locale (5 min)
        │
        ▼  si l'écriture échoue (tag pas posé sur le lecteur)
Spring Boot log un RfidWriteFailure et alerte le caissier via SSE
→ caissier demande au client de reposer le produit pour retry
```

---

## 6. Architecture Technique Complète

```
┌──────────────────────────────────────────────────────────────────────┐
│                        PHARMACIE                                      │
│                                                                      │
│  ZONE CAISSE                          SORTIE                         │
│                                                                      │
│  ┌─────────────────────────────┐   ┌──────────────────────────────┐ │
│  │   PC Caisse (Tauri)         │   │  PORTIQUE RFID AUTONOME      │ │
│  │                             │   │                              │ │
│  │  Angular UI                 │   │  CPU embarqué Linux          │ │
│  │  → déclenche la vente       │   │  Batterie secours 2–4h       │ │
│  │  → affiche alertes SSE      │   │  Relais câblé → sirène       │ │
│  │                             │   │                              │ │
│  │  Spring Boot :8080          │   │  Logique autonome :          │ │
│  │  → confirme vente en DB     │   │  1. Lit User Memory du tag   │ │
│  │  → commande WRITE lecteur   │   │     SOLD ? → silence         │ │
│  │  → notifie portique (SOLD)  │   │  2. Tag en whitelist RAM ?   │ │
│  │  → diffuse alertes SSE      │   │     OUI  → silence           │ │
│  │                             │   │  3. Aucun des deux           │ │
│  │  Lecteur RFID caisse        │   │     → ALARME HARDWARE        │ │
│  │  (USB/TCP sur comptoir)     │   │                              │ │
│  │  → exécute WRITE sur ordre  │   │  Sync avec Spring Boot :     │ │
│  │    de Spring Boot           │   │  → reconstruit whitelist RAM │ │
│  │                             │   │  → envoie logs événements    │ │
│  └──────────────┬──────────────┘   └──────────────┬───────────────┘ │
│                 │                                  │                 │
│                 │  TCP/IP local                    │  TCP/IP local   │
│                 └──────────────┬───────────────────┘                 │
│                                │                                     │
│                    Spring Boot :8080 (maître)                        │
└──────────────────────────────────────────────────────────────────────┘
```

### Rôle de Tauri dans ce schéma

Tauri est le **dashboard** et l'**interface utilisateur**, pas le chef d'orchestre RFID :

```
Tauri / Angular
  ├── Affiche les alertes vol (SSE depuis Spring Boot)
  ├── Affiche l'état des portiques (en ligne / hors ligne)
  ├── Affiche l'historique des scans et des alertes
  ├── Permet de marquer un faux positif
  └── Déclenche la vente → Spring Boot prend le relais pour le RFID
```

---

## 7. Modèle de Données

### 7.1 Nouvelles entités

#### `PortiqueControle` — configuration des appareils

```java
@Entity
@Table(name = "portique_controle", schema = "warehouse")
public class PortiqueControle {
    @Id Long id;
    String name;

    @ManyToOne Magasin magasin;
    @ManyToOne Storage storage;         // zone surveillée

    @Enumerated TypePortique type;      // RECEPTION | VENTE | SORTIE | INVENTAIRE
    String ipAddress;                   // portique réseau (TCP/LLRP)
    Integer port;                       // port TCP (défaut LLRP: 5084)
    boolean actif;
    Instant lastSeenAt;                 // heartbeat pour détecter hors-ligne
}
```

#### `RfidTag` — association tag ↔ produit (table maître)

```java
@Entity
@Table(name = "rfid_tag", schema = "warehouse")
public class RfidTag {
    @Id Long id;
    String epc;                         // identifiant EPC unique du tag

    @ManyToOne Produit produit;
    @ManyToOne Lot lot;                 // nullable si pas gestion_lot

    RfidTagStatus status;               // ACTIVE | SOLD | LOST | DAMAGED
    Instant activatedAt;               // date d'étiquetage
    Instant soldAt;                    // date d'écriture SOLD dans le tag
    boolean userMemoryWritten;         // true si l'écriture physique est confirmée
}
```

#### `RfidScanEvent` — journal immuable des scans portique

```java
@Entity
@Table(name = "rfid_scan_event", schema = "warehouse")
public class RfidScanEvent {
    @Id Long id;

    @ManyToOne PortiqueControle portique;
    @ManyToOne RfidTag rfidTag;         // nullable si tag inconnu
    @ManyToOne Produit produit;         // nullable si tag inconnu
    @ManyToOne Lot lot;

    RfidScanStatus status;              // VALIDATED | THEFT_ALERT | UNKNOWN_TAG | FALSE_POSITIVE
    String validationSource;            // "TAG_MEMORY" | "RAM_WHITELIST" | "DB_QUERY"
    Integer rssi;                       // force signal → distance approximative
    Instant scannedAt;
    @ManyToOne User reviewedBy;         // opérateur si validation manuelle
    String falsePositiveReason;
}
```

#### `RfidWriteLog` — traçabilité des écritures dans les tags

```java
@Entity
@Table(name = "rfid_write_log", schema = "warehouse")
public class RfidWriteLog {
    @Id Long id;

    @ManyToOne RfidTag rfidTag;
    @ManyToOne Sales sale;              // nullable (écriture SOLD liée à une vente)

    RfidWriteType writeType;            // ACTIVATE | SOLD | RESET
    RfidWriteStatus status;            // SUCCESS | FAILED | PENDING_RETRY
    String writtenData;                // contenu JSON écrit en user memory
    Instant attemptedAt;
    Integer retryCount;
    String errorMessage;
}
```

### 7.2 Migration Flyway — `V1.7.6__portique_controle.sql`

```sql
-- Portiques configurés
CREATE TABLE warehouse.portique_controle (
    id              BIGINT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    magasin_id      BIGINT REFERENCES warehouse.magasin(id),
    storage_id      BIGINT REFERENCES warehouse.storage(id),
    type            VARCHAR(30) NOT NULL,
    ip_address      VARCHAR(50),
    port            INT DEFAULT 5084,
    actif           BOOLEAN DEFAULT TRUE,
    last_seen_at    TIMESTAMP
);

-- Association tag RFID <-> produit
CREATE TABLE warehouse.rfid_tag (
    id                   BIGINT PRIMARY KEY,
    epc                  VARCHAR(100) NOT NULL,
    produit_id           BIGINT REFERENCES warehouse.produit(id),
    lot_id               BIGINT REFERENCES warehouse.lot(id),
    status               VARCHAR(20) DEFAULT 'ACTIVE',
    activated_at         TIMESTAMP NOT NULL,
    sold_at              TIMESTAMP,
    user_memory_written  BOOLEAN DEFAULT FALSE
);
CREATE UNIQUE INDEX uidx_rfid_epc       ON warehouse.rfid_tag(epc);
CREATE INDEX idx_rfid_tag_produit       ON warehouse.rfid_tag(produit_id);
CREATE INDEX idx_rfid_tag_status        ON warehouse.rfid_tag(status);

-- Journal immuable des scans portique
CREATE TABLE warehouse.rfid_scan_event (
    id                  BIGINT PRIMARY KEY,
    portique_id         BIGINT REFERENCES warehouse.portique_controle(id),
    rfid_tag_id         BIGINT REFERENCES warehouse.rfid_tag(id),
    produit_id          BIGINT REFERENCES warehouse.produit(id),
    lot_id              BIGINT REFERENCES warehouse.lot(id),
    status              VARCHAR(30) DEFAULT 'PENDING',
    validation_source   VARCHAR(20),
    rssi                INT,
    scanned_at          TIMESTAMP NOT NULL,
    reviewed_by         BIGINT REFERENCES warehouse.jhi_user(id),
    false_positive_reason TEXT
);
CREATE INDEX idx_rfid_scan_status    ON warehouse.rfid_scan_event(status, scanned_at DESC);
CREATE INDEX idx_rfid_scan_portique  ON warehouse.rfid_scan_event(portique_id, scanned_at DESC);
CREATE INDEX idx_rfid_scan_produit   ON warehouse.rfid_scan_event(produit_id, scanned_at DESC);
-- Index partiel pour le dashboard (alertes uniquement)
CREATE INDEX idx_rfid_theft_alerts   ON warehouse.rfid_scan_event(scanned_at DESC)
    WHERE status = 'THEFT_ALERT';

-- Traçabilité des écritures dans les tags
CREATE TABLE warehouse.rfid_write_log (
    id            BIGINT PRIMARY KEY,
    rfid_tag_id   BIGINT REFERENCES warehouse.rfid_tag(id),
    sale_id       BIGINT,
    write_type    VARCHAR(20) NOT NULL,
    status        VARCHAR(20) DEFAULT 'PENDING_RETRY',
    written_data  TEXT,
    attempted_at  TIMESTAMP NOT NULL,
    retry_count   INT DEFAULT 0,
    error_message TEXT
);
CREATE INDEX idx_rfid_write_status ON warehouse.rfid_write_log(status, attempted_at DESC);
```

---

## 8. Implémentation Backend (Spring Boot)

### 8.1 `RfidWriterService.java` — écriture dans la mémoire des tags

Ce service est appelé par `SalesService` après confirmation de la vente en DB.  
**C'est Spring Boot qui pilote l'écriture, pas Tauri.**

```java
@Service
public class RfidWriterService {

    private final LlrpClient llrpClient;                // client LLRP vers lecteur caisse
    private final RfidWriteLogRepository writeLogRepo;
    private final RfidTagRepository rfidTagRepo;

    /**
     * Appelé par SalesService après que la vente est CONFIRMÉE en DB.
     * Écrit "SOLD" dans la mémoire user du tag via le lecteur caisse.
     */
    @Async
    @Transactional
    public void writeSoldToTag(RfidTag tag, Sales sale) {
        String payload = buildSoldPayload(tag, sale);
        RfidWriteLog log = RfidWriteLog.builder()
            .rfidTag(tag)
            .sale(sale)
            .writeType(RfidWriteType.SOLD)
            .writtenData(payload)
            .attemptedAt(Instant.now())
            .status(RfidWriteStatus.PENDING_RETRY)
            .build();
        writeLogRepo.save(log);

        try {
            llrpClient.writeUserMemory(tag.getEpc(), payload);   // LLRP write command
            log.setStatus(RfidWriteStatus.SUCCESS);
            tag.setStatus(RfidTagStatus.SOLD);
            tag.setSoldAt(Instant.now());
            tag.setUserMemoryWritten(true);
        } catch (LlrpException e) {
            log.setStatus(RfidWriteStatus.FAILED);
            log.setErrorMessage(e.getMessage());
            // notifier le caissier : le client doit reposer le produit
            sseEmitterService.broadcast("RFID_WRITE_FAILED", WriteFailureAlert.of(tag, sale));
        }
    }

    private String buildSoldPayload(RfidTag tag, Sales sale) {
        return """
            {"s":"SOLD","id":%d,"ts":"%s"}
            """.formatted(sale.getId(), Instant.now());
    }
}
```

### 8.2 Intégration dans `SalesService`

```java
@Service
@Transactional
public class SalesService {

    private final RfidWriterService rfidWriterService;
    private final PortiqueNotifierService portiqueNotifier;

    public Sales closeSale(Long saleId) {
        Sales sale = findAndValidate(saleId);
        sale.setStatus(SaleStatut.CLOSED);           // 1. confirmer en DB
        salesRepository.save(sale);

        // 2. pour chaque ligne vendue
        sale.getSalesLines().forEach(line -> {
            rfidTagRepository
                .findActiveBySaleLineProduct(line.getProduit().getId())
                .ifPresent(tag -> {
                    rfidWriterService.writeSoldToTag(tag, sale);   // 3. écrire dans le tag
                    portiqueNotifier.notifySold(tag.getEpc());     // 4. whitelist portique RAM
                });
        });

        return sale;
    }
}
```

### 8.3 `PortiqueService.java` — traitement des scans à la sortie

```java
@Service
@Transactional
public class PortiqueService {

    /**
     * Appelé par le portique lui-même via son API embarquée.
     * Le portique a déjà déclenché l'alarme hardware — ce service
     * peut la couper si le produit est vendu (via signal retour au portique).
     */
    public ScanResult processScan(RfidScanRequest req) {
        RfidTag tag = rfidTagRepository.findByEpc(req.epc()).orElse(null);

        if (tag == null) {
            logAndBroadcast(req, RfidScanStatus.UNKNOWN_TAG, null);
            return ScanResult.unknownTag(req.epc());
        }

        // Source de validation : TAG_MEMORY (reportée par le portique) > RAM_WHITELIST > DB
        String validationSource = req.tagMemorySold()
            ? "TAG_MEMORY"
            : portiqueWhitelistService.contains(req.portiqueId(), req.epc())
                ? "RAM_WHITELIST"
                : "DB_QUERY";

        boolean isSold = req.tagMemorySold()
            || portiqueWhitelistService.contains(req.portiqueId(), req.epc())
            || salesRepository.existsRecentSaleForTag(tag.getId(), Instant.now().minusSeconds(300));

        RfidScanEvent event = buildEvent(req, tag, isSold, validationSource);
        rfidScanEventRepository.save(event);

        if (!isSold) {
            TheftAlert alert = TheftAlert.of(event, tag.getProduit());
            sseEmitterService.broadcast("THEFT_ALERT", alert);
            inventoryTransactionService.logSuspicionVol(tag.getProduit(), event);
            // NE PAS envoyer de signal "SILENCE" au portique → laisser l'alarme sonner
            return ScanResult.theftAlert(alert);
        }

        // Envoyer signal SILENCE au portique (alarme hardware coupée)
        portiqueController.silenceAlarm(req.portiqueId(), req.epc());
        return ScanResult.ok(validationSource);
    }
}
```

### 8.4 `PortiqueResource.java`

```java
@RestController
@RequestMapping("/api/portique")
public class PortiqueResource {

    // Appelé par le portique lui-même (son CPU embarqué)
    @PostMapping("/scan")
    public ResponseEntity<ScanResult> scan(@RequestBody RfidScanRequest req) {
        return ResponseEntity.ok(portiqueService.processScan(req));
    }

    // SSE → dashboard Angular (alertes vol temps réel)
    @GetMapping("/alerts")
    public SseEmitter streamAlerts() {
        return sseEmitterService.newEmitter();
    }

    // Historique paginé
    @GetMapping("/events")
    public ResponseEntity<Page<RfidScanEvent>> events(
        @RequestParam(required = false) RfidScanStatus status,
        Pageable pageable) { ... }

    // Marquer faux positif
    @PutMapping("/events/{id}/false-positive")
    public ResponseEntity<Void> markFalsePositive(
        @PathVariable Long id, @RequestBody FalsePositiveRequest req) { ... }

    // Whitelist : Spring Boot pousse les SOLD au portique
    @PostMapping("/portiques/{id}/whitelist")
    public ResponseEntity<Void> pushToWhitelist(
        @PathVariable Long id, @RequestBody WhitelistEntry entry) { ... }

    // CRUD configuration portiques
    @GetMapping("/portiques")
    @PostMapping("/portiques")
    @PutMapping("/portiques/{id}")
}
```

---

## 9. Rôle de Tauri : Dashboard et Monitoring uniquement

Tauri **ne pilote pas** le matériel RFID. Il affiche ce que Spring Boot lui communique via SSE.

```typescript
// features/portique/data-access/portique-sse.service.ts
@Injectable({ providedIn: 'root' })
export class PortiqueSseService {
  private http = inject(HttpClient);

  readonly theftAlerts = signal<TheftAlert[]>([]);
  readonly portiquesStatus = signal<PortiqueStatus[]>([]);

  connect(): void {
    const source = new EventSource('/api/portique/alerts');

    source.addEventListener('THEFT_ALERT', (e) => {
      const alert: TheftAlert = JSON.parse(e.data);
      this.theftAlerts.update(current => [alert, ...current].slice(0, 100));
      this.playAlertSound();
    });

    source.addEventListener('PORTIQUE_STATUS', (e) => {
      const status: PortiqueStatus = JSON.parse(e.data);
      this.portiquesStatus.update(current =>
        current.map(p => p.id === status.id ? status : p)
      );
    });

    source.addEventListener('RFID_WRITE_FAILED', (e) => {
      // Le caissier doit faire reposer le produit au client
      const fail = JSON.parse(e.data);
      this.showWriteFailureDialog(fail);
    });
  }

  private playAlertSound(): void {
    new Audio('/assets/sounds/alert.mp3').play().catch(() => {});
  }
}
```

```html
<!-- features/portique/feature/dashboard/portique-dashboard.component.html -->
<div class="portique-dashboard">

  <!-- Statut des portiques -->
  <div class="portiques-status">
    @for (portique of sseService.portiquesStatus(); track portique.id) {
      <div class="portique-card" [class.active]="portique.online">
        <span class="portique-name">{{ portique.name }}</span>
        <p-tag
          [value]="portique.online ? 'EN LIGNE' : 'HORS LIGNE'"
          [severity]="portique.online ? 'success' : 'danger'" />
        <small>Dernière activité : {{ portique.lastSeenAt | date:'HH:mm:ss' }}</small>
      </div>
    }
  </div>

  <!-- Alertes vol temps réel -->
  @if (sseService.theftAlerts().length > 0) {
    <div class="alerts-panel">
      <h3>Alertes en cours ({{ sseService.theftAlerts().length }})</h3>
      @for (alert of sseService.theftAlerts(); track alert.scannedAt; let i = $index) {
        <div class="alert-card theft-alert">
          <div class="alert-info">
            <strong>{{ alert.produitNom }}</strong>
            <span>Portique : {{ alert.portiqueNom }}</span>
            <span>{{ alert.scannedAt | date:'HH:mm:ss' }}</span>
          </div>
          <p-button
            icon="pi pi-check"
            label="Faux positif"
            severity="secondary"
            (onClick)="markFalsePositive(alert, i)" />
        </div>
      }
    </div>
  }

  <!-- Historique des scans avec AG Grid -->
  <app-scan-event-list />

</div>
```

---

## 10. Structure du module Angular `features/portique/`

```
src/main/webapp/app/features/portique/
├── portique.routes.ts
├── data-access/
│   ├── portique-api.service.ts       # HTTP CRUD vers /api/portique/**
│   ├── portique-sse.service.ts       # SSE alertes temps réel
│   └── portique.store.ts             # ngrx/signals store
├── feature/
│   ├── dashboard/
│   │   └── portique-dashboard.component.ts/.html/.scss
│   ├── events/
│   │   └── scan-event-list.component.ts/.html
│   └── config/
│       └── portique-config.component.ts/.html
└── shared/
    └── portique.model.ts
```

---

## 11. Plan d'Implémentation par Phases

### Phase 1 — Base de données et entités (Semaine 1)

- [ ] Créer migration `V1.7.6__portique_controle.sql`
- [ ] Créer entités JPA : `PortiqueControle`, `RfidTag`, `RfidScanEvent`, `RfidWriteLog`
- [ ] Créer repositories Spring Data correspondants
- [ ] Ajouter `TypeMouvementStock.SUSPICION_VOL` dans l'enum existant
- [ ] Tester la migration avec `./mvnw flyway:info`

### Phase 2 — Backend Spring Boot (Semaine 2)

- [ ] Créer `RfidWriterService` (écriture SOLD dans tags via LLRP)
- [ ] Créer `PortiqueNotifierService` (envoi whitelist RAM au portique)
- [ ] Créer `PortiqueService` (traitement scans, logique SOLD/THEFT)
- [ ] Créer `SseEmitterService` pour les alertes temps réel
- [ ] Créer `PortiqueResource` avec tous les endpoints
- [ ] Intégrer `RfidWriterService` dans `SalesService.closeSale()`
- [ ] Écrire tests unitaires `PortiqueServiceTest`, `RfidWriterServiceTest`

### Phase 3 — Intégration matériel (Semaine 3)

- [ ] Implémenter `LlrpClient` (protocole LLRP vers lecteurs Zebra/Impinj)
- [ ] Tester écriture user memory sur tags physiques
- [ ] Configurer le portique pour appeler `POST /api/portique/scan` à chaque détection
- [ ] Tester le signal SILENCE retour vers le portique

### Phase 4 — Frontend Angular / Tauri (Semaine 4)

- [ ] Créer module `features/portique/` avec structure complète
- [ ] Implémenter `PortiqueSseService` (connexion SSE Spring Boot)
- [ ] Créer `PortiqueDashboardComponent` (alertes + statut portiques)
- [ ] Créer `ScanEventListComponent` avec AG Grid
- [ ] Créer `PortiqueConfigComponent` (CRUD portiques)
- [ ] Ajouter route dans la navigation principale
- [ ] Ajouter `alert.mp3` dans `src/main/webapp/assets/sounds/`

### Phase 5 — Étiquetage produits (Semaine 5)

- [ ] Créer interface d'étiquetage : scan CIP → génère EPC → écriture tag + association DB
- [ ] Intégrer étiquetage dans le flux de réception (`StockEntryService`)
- [ ] Créer rapport "produits non étiquetés" (stock sans `rfid_tag` actif)
- [ ] Procédure de reset tag lors d'un retour client (`RfidWriteType.RESET`)

---

## 12. Points d'Attention

### Fenêtre de grâce (5 minutes)
La vérification DB utilise une fenêtre de 5 minutes pour couvrir le délai caisse → portique.  
Valeur configurable via `ConfigurationService`. La mémoire du tag, elle, est permanente.

### Priorité de validation (du plus au moins fiable)
1. **User memory du tag** — validée par le portique sans aucun réseau
2. **Whitelist RAM portique** — synchronisée depuis Spring Boot, perdue au redémarrage
3. **Requête DB Spring Boot** — fallback si les deux précédents échouent

### Écriture tag échouée (tag hors portée à la caisse)
- Spring Boot alerte le caissier via SSE (`RFID_WRITE_FAILED`)
- Caissier demande au client de reposer le produit sur le lecteur
- `RfidWriteLog` conserve les tentatives et retry count
- En dernier recours : tag reste en whitelist RAM portique (fenêtre 5 min)

### Faux positifs courants
- Pharmacien transportant du stock → ajouter rôle "transit autorisé" (bypass portique)
- Retour client : reset le tag (`RfidWriteType.RESET`) pour le réactiver
- Tag défectueux → alerte `UNKNOWN_TAG` (moins prioritaire que `THEFT_ALERT`)

### Performance
- `RfidScanEvent` → envisager partitionnement par mois (comme `inventory_transaction`)
- Index partiel sur `status = 'THEFT_ALERT'` pour requêtes dashboard rapides
- `RfidWriteLog` → purge automatique après 90 jours (Flyway script de maintenance)

### Matériel compatible (protocole LLRP)
- **Zebra FX7500 / FX9600** — réseau TCP/IP, API LLRP, GPIO relais
- **Impinj Speedway R420** — réseau TCP/IP, API LLRP + REST
- **Lecteurs USB HID** — protocole ASCII simple (plus facile, moins de fonctionnalités)

---

## 13. Nouveaux Types dans les Enums

```java
// Dans TypeMouvementStock (existant à étendre)
SUSPICION_VOL,        // produit sorti sans vente détectée
SORTIE_PORTIQUE,      // sortie validée via portique
TAG_INCONNU,          // tag RFID non enregistré en base

// Nouveaux enums
enum TypePortique     { RECEPTION, VENTE, SORTIE, INVENTAIRE }
enum RfidTagStatus    { ACTIVE, SOLD, LOST, DAMAGED }
enum RfidScanStatus   { VALIDATED, THEFT_ALERT, UNKNOWN_TAG, FALSE_POSITIVE }
enum RfidWriteType    { ACTIVATE, SOLD, RESET }
enum RfidWriteStatus  { SUCCESS, FAILED, PENDING_RETRY }
```

---

## 14. Endpoints REST — Référence Complète

| Méthode | URL | Appelé par | Description |
|---|---|---|---|
| `POST` | `/api/portique/scan` | Portique (CPU embarqué) | Traiter un scan à la sortie |
| `GET` | `/api/portique/alerts` | Angular (SSE) | Stream alertes temps réel |
| `GET` | `/api/portique/events` | Angular | Historique paginé |
| `PUT` | `/api/portique/events/{id}/false-positive` | Angular | Marquer faux positif |
| `POST` | `/api/portique/portiques/{id}/whitelist` | Spring Boot interne | Pousser SOLD au portique |
| `GET` | `/api/portique/portiques` | Angular | Liste portiques configurés |
| `POST` | `/api/portique/portiques` | Angular | Créer un portique |
| `PUT` | `/api/portique/portiques/{id}` | Angular | Modifier un portique |
| `POST` | `/api/portique/tags` | Angular (étiquetage) | Associer tag ↔ produit |
| `GET` | `/api/portique/tags/{epc}` | Angular | Infos sur un tag |
| `GET` | `/api/portique/stats` | Angular | Statistiques (alertes/jour) |
| `GET` | `/api/portique/write-failures` | Angular | Écritures tag échouées |

---

---

## 15. Modèles de Matériel et Coûts d'Investissement

### 15.1 Portiques RFID UHF (détection à la sortie)

#### Zebra ATR7000 — Portique retail entrée de gamme pro

```
┌─────────────────────────────────────────────────────┐
│  ZEBRA ATR7000                                       │
│                                                     │
│  Fréquence    : 860–960 MHz UHF Gen2                │
│  Portée       : jusqu'à 2,5 m entre les colonnes   │
│  Lecture      : 500 tags/seconde                    │
│  CPU embarqué : Linux, API HTTP REST + LLRP         │
│  Relais       : sortie GPIO pour sirène externe     │
│  Batterie     : non intégrée (onduleur recommandé)  │
│  Connectivité : Ethernet RJ45 + WiFi optionnel      │
│                                                     │
│  Prix unitaire : 6 000 – 9 000 €                   │
│  Installation  : 500 – 1 500 €                     │
└─────────────────────────────────────────────────────┘
```

#### Zebra FX9600 Portal — Portique milieu de gamme

```
┌─────────────────────────────────────────────────────┐
│  ZEBRA FX9600 (monté en portique)                   │
│                                                     │
│  4 ports antennes (antennes externes incluses)      │
│  Portée       : jusqu'à 3 m                        │
│  CPU embarqué : Linux, LLRP + Zebra DNA Platform   │
│  Filtrage     : par zone (RSSI) pour limiter       │
│                 les lectures hors portique          │
│  Relais GPIO  : oui                                │
│                                                     │
│  Prix reader  : 2 500 – 4 000 €                   │
│  + antennes×4 : 400 – 800 €                       │
│  + structure  : 500 – 1 500 €                     │
│  TOTAL        : 3 500 – 6 500 €                   │
└─────────────────────────────────────────────────────┘
```

> Solution la plus flexible pour Pharma-Smart : le FX9600 peut servir à la fois
> de portique sortie ET de lecteur caisse (ports supplémentaires).

#### Impinj xSpan Gateway — Portique haut de gamme

```
┌─────────────────────────────────────────────────────┐
│  IMPINJ xSPAN GATEWAY                               │
│                                                     │
│  16 ports antennes                                 │
│  Impinj Octane SDK (LLRP + HTTP REST)              │
│  ItemSense middleware optionnel (cloud)            │
│  Zoning précis : sait de quel côté le tag passe   │
│  Batterie backup : option disponible               │
│                                                     │
│  Prix portique : 12 000 – 20 000 €                │
│  Adapté        : multi-sorties, grande surface     │
└─────────────────────────────────────────────────────┘
```

#### Nedap !D Top — Portique dédié retail/pharmacie

```
┌─────────────────────────────────────────────────────┐
│  NEDAP !D TOP                                       │
│                                                     │
│  Spécialement conçu pour retail et pharmacie       │
│  Hybride EAS + RFID (option)                       │
│  API REST embarquée, webhook configurable          │
│  Batterie intégrée 4h                              │
│  Discret (design colonne fine)                     │
│                                                     │
│  Prix portique : 8 000 – 14 000 €                 │
│  Batterie incluse → fail-safe natif                │
└─────────────────────────────────────────────────────┘
```

---

### 15.2 Lecteurs RFID Caisse (écriture dans les tags)

Ces lecteurs sont posés sur le comptoir caisse. Le caissier pose le produit dessus,
Spring Boot envoie la commande LLRP d'écriture.

#### Zebra FX7500 — Lecteur fixe (recommandé caisse)

```
┌──────────────────────────────────────────┐
│  ZEBRA FX7500                            │
│                                          │
│  2 ports antennes                        │
│  API LLRP + HTTP REST + Zebra DNA       │
│  Portée antenne plate : 30–50 cm        │
│  Écriture user memory : oui             │
│  Connexion : Ethernet                   │
│                                          │
│  Prix reader       : 1 500 – 2 500 €   │
│  + antenne plate   :   200 –   400 €   │
│  TOTAL             : 1 700 – 2 900 €   │
└──────────────────────────────────────────┘
```

#### Impinj Speedway R420 — Lecteur fixe alternatif

```
┌──────────────────────────────────────────┐
│  IMPINJ SPEEDWAY R420                    │
│                                          │
│  4 ports antennes                        │
│  Octane SDK + LLRP                       │
│  Très répandu, bonne documentation      │
│                                          │
│  Prix reader       : 2 000 – 3 000 €   │
│  + antenne plate   :   200 –   500 €   │
│  TOTAL             : 2 200 – 3 500 €   │
└──────────────────────────────────────────┘
```

#### Zebra RFD8500 — Lecteur portable (étiquetage réception)

```
┌──────────────────────────────────────────┐
│  ZEBRA RFD8500 (sled Bluetooth)          │
│                                          │
│  Se fixe sur smartphone Android         │
│  Lecture + écriture tags                │
│  Utilisé pour étiqueter à la réception  │
│  Bluetooth → app Android dédiée         │
│                                          │
│  Prix : 800 – 1 500 €                  │
│  Optionnel si antenne plate suffisante  │
└──────────────────────────────────────────┘
```

---

### 15.3 Tags RFID UHF (par produit)

| Type de tag | Usage | Prix unitaire | Notes |
|---|---|---|---|
| Tag papier basique (EPC seul) | Produits courants | 0,05 – 0,10 € | Pas de user memory |
| Tag papier + user memory 512b | **Recommandé** | 0,12 – 0,20 € | Validation offline possible |
| Tag papier + user memory 2KB | Produits haute valeur | 0,20 – 0,35 € | Stocke plus de données |
| Tag plastique dur | Réutilisable (matériel) | 0,50 – 2,00 € | Non adapté aux boîtes médicaments |
| Tag résistant (humidité, froid) | Chaîne du froid | 0,30 – 0,80 € | Médicaments thermosensibles |

> Pour Pharma-Smart : choisir **tag papier + user memory 512 bits** (~0,15 €/unité).
> 512 bits = 64 octets = largement suffisant pour `{"s":"SOLD","id":12345,"ts":"..."}`.

---

### 15.4 Coût Total d'Investissement par Scénario

#### Scénario 1 — Petite pharmacie (1 sortie, 1 caisse)

| Poste | Modèle | Coût |
|---|---|---|
| Portique sortie | Zebra FX9600 + 2 antennes + structure | 5 000 € |
| Lecteur caisse | Zebra FX7500 + antenne plate | 2 000 € |
| Lecteur étiquetage réception | Zebra RFD8500 (optionnel) | 1 000 € |
| Installation réseau + câblage | Prestataire | 800 € |
| **Total matériel** | | **~8 800 €** |
| Tags (stock initial 5 000 unités) | 0,15 € × 5 000 | 750 € |
| Tags (consommable annuel 20 000) | 0,15 € × 20 000 | 3 000 €/an |
| **Investissement initial total** | | **~9 550 €** |

#### Scénario 2 — Pharmacie moyenne (2 sorties, 2 caisses)

| Poste | Modèle | Coût |
|---|---|---|
| Portiques sortie ×2 | Nedap !D Top ×2 (batterie incluse) | 20 000 € |
| Lecteurs caisse ×2 | Zebra FX7500 ×2 | 4 000 € |
| Lecteur réception | Zebra RFD8500 | 1 000 € |
| Installation + réseau | Prestataire | 2 000 € |
| **Total matériel** | | **~27 000 €** |
| Tags (stock initial 15 000 unités) | 0,15 € × 15 000 | 2 250 € |
| Tags (consommable annuel 50 000) | 0,15 € × 50 000 | 7 500 €/an |
| **Investissement initial total** | | **~29 250 €** |

#### Scénario 3 — Grande pharmacie / parapharmacie (4 sorties, 3 caisses)

| Poste | Modèle | Coût |
|---|---|---|
| Portiques sortie ×4 | Impinj xSpan ×4 | 60 000 € |
| Lecteurs caisse ×3 | Impinj Speedway R420 ×3 | 9 000 € |
| Lecteurs réception ×2 | Zebra RFD8500 ×2 | 2 000 € |
| Middleware RFID | Impinj ItemSense (licence annuelle) | 5 000 €/an |
| Installation + intégration réseau | Prestataire spécialisé | 8 000 € |
| **Total matériel** | | **~79 000 €** |
| Tags (stock initial 50 000 unités) | 0,15 € × 50 000 | 7 500 € |
| **Investissement initial total** | | **~86 500 €** |

---

### 15.5 Retour sur Investissement (ROI)

Le ROI dépend du taux de vol actuel. En pharmacie française, le **taux de démarque
inconnue** est estimé entre 1 % et 3 % du chiffre d'affaires.

```
Exemple : CA annuel 2 000 000 €, démarque 2 % = 40 000 € de perte/an

Scénario 1 (investissement 9 550 €) :
  Si réduction démarque de 80 % → économie 32 000 €/an
  ROI < 4 mois

Scénario 2 (investissement 29 250 €) :
  Si réduction démarque de 80 % → économie 32 000 €/an
  ROI < 11 mois
```

---

### 15.6 Tableau Comparatif Final

| Critère | Zebra FX9600 | Nedap !D Top | Impinj xSpan |
|---|---|---|---|
| **Prix portique** | 3 500–6 500 € | 8 000–14 000 € | 12 000–20 000 € |
| **Batterie intégrée** | Non | Oui (4h) | Option |
| **API** | LLRP + HTTP | HTTP REST | LLRP + Octane SDK |
| **Compatibilité Spring Boot** | ✅ Excellente | ✅ Bonne | ✅ Excellente |
| **Zoning précis** | Moyen | Bon | Excellent |
| **Adapté pharmacie** | ✅ | ✅ ✅ | ✅ (grandes surfaces) |
| **Recommandé pour** | Petite pharmacie | Pharmacie moyenne | Grande surface |

---

## 16. Bibliothèque LLRP et Intégration Maven

### Qu'est-ce que LLRP ?

**LLRP (Low Level Reader Protocol)** est le protocole standard ISO/IEC 24791-3 pour
piloter les lecteurs RFID UHF. Il définit les messages permettant de :
- Configurer les antennes et paramètres de lecture
- Démarrer/arrêter les sessions de lecture
- Recevoir les tags lus en temps réel
- Écrire dans la mémoire des tags

Communication : **TCP/IP persistante sur le port 5084** (défaut LLRP).  
Messages : binaire encodé en XML/TLV — la bibliothèque cache cette complexité.

---

### Option A — llrp-toolkit (recommandée)

Bibliothèque de référence, implémentation du standard LLRP, compatible Zebra, Impinj, et tout
lecteur certifié LLRP.

#### Dépendance Maven

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.llrp</groupId>
    <artifactId>llrp-toolkit</artifactId>
    <version>1.0.1</version>
</dependency>

<!-- Nécessaire pour l'encodage/décodage des messages LLRP -->
<dependency>
    <groupId>org.apache.mina</groupId>
    <artifactId>mina-core</artifactId>
    <version>2.2.3</version>
</dependency>
```

> **Note :** Vérifier la dernière version sur [Maven Central](https://central.sonatype.com)
> en cherchant `org.llrp:llrp-toolkit`. Si indisponible sur Central, le jar est disponible
> sur [SourceForge llrp-toolkit](https://sourceforge.net/projects/llrp-toolkit/).

#### Alternative si llrp-toolkit non disponible : Fosstrak

```xml
<dependency>
    <groupId>org.fosstrak.llrp</groupId>
    <artifactId>llrp-client-sdk</artifactId>
    <version>0.1.0</version>
</dependency>
```

---

### Option B — HTTP REST embarquée (plus simple, moins universel)

Certains lecteurs modernes (Zebra FX7500 firmware récent, Impinj Octane SDK) exposent
une **API HTTP REST locale** en plus de LLRP. Aucune bibliothèque spéciale requise —
Spring Boot utilise son `RestClient` standard.

```java
// Pas de dépendance supplémentaire — RestClient est dans Spring Boot 4
restClient.post()
    .uri("http://192.168.1.20:8080/api/v1/tags/write")
    .body(writeRequest)
    .retrieve()
    .toBodilessEntity();
```

**Limitation :** API propriétaire à chaque marque — pas interopérable.

---

### Implémentation Spring Boot avec llrp-toolkit

#### Interface d'abstraction

```java
// service/portique/RfidReaderClient.java
public interface RfidReaderClient {
    void connect(String host, int port) throws RfidConnectionException;
    void disconnect();
    void writeUserMemory(String epc, String data) throws RfidWriteException;
    void addSoldToWhitelist(String epc, Duration ttl);
    boolean isConnected();
}
```

L'interface isole le reste de l'application du protocole LLRP — on peut changer
d'implémentation (LLRP → HTTP REST) sans toucher aux services métier.

#### Implémentation LLRP

```java
// service/portique/impl/LlrpReaderClientImpl.java
@Component
@Slf4j
public class LlrpReaderClientImpl implements RfidReaderClient {

    private LLRPConnection connection;

    @Override
    public void connect(String host, int port) throws RfidConnectionException {
        try {
            connection = new LLRPConnector(new LLRPEndpoint() {
                @Override
                public void messageReceived(LLRPMessage message) {
                    handleIncomingMessage(message);       // tags lus en temps réel
                }
                @Override
                public void errorOccurred(String error) {
                    log.error("LLRP error: {}", error);
                }
            }, host, port);
            connection.connect();
            initReader();
        } catch (Exception e) {
            throw new RfidConnectionException("Cannot connect to reader " + host, e);
        }
    }

    @Override
    public void writeUserMemory(String epc, String data) throws RfidWriteException {
        // 1. Construire le message LLRP C1G2Write
        C1G2Write writeOp = new C1G2Write();
        writeOp.setOpSpecID(new UnsignedShort(1));
        writeOp.setMB(new TwoBitField("11"));           // MB=3 : User Memory Bank
        writeOp.setWordPointer(new UnsignedShort(0));
        writeOp.setWriteData(toHex(data));

        // 2. Encapsuler dans un AccessSpec
        AccessSpec accessSpec = buildAccessSpec(epc, writeOp);

        // 3. Envoyer au lecteur
        ADD_ACCESSSPEC addSpec = new ADD_ACCESSSPEC();
        addSpec.setAccessSpec(accessSpec);
        connection.transact(addSpec);                    // synchrone, attend la réponse

        // 4. Activer et exécuter
        ENABLE_ACCESSSPEC enable = new ENABLE_ACCESSSPEC();
        enable.setAccessSpecID(new UnsignedInteger(1));
        LLRPMessage response = connection.transact(enable);

        if (!(response instanceof ENABLE_ACCESSSPEC_RESPONSE resp)
            || !resp.getLLRPStatus().getStatusCode().equals(LLRPStatusCode.M_Success)) {
            throw new RfidWriteException("Write failed for EPC: " + epc);
        }
    }

    private void handleIncomingMessage(LLRPMessage message) {
        // Tags lus par le portique arrivent ici en RO_ACCESS_REPORT
        if (message instanceof RO_ACCESS_REPORT report) {
            report.getTagReportDataList().forEach(tagData -> {
                String epc = tagData.getEPCParameter().toString();
                // publier dans Spring ApplicationEvent pour que PortiqueService traite
                applicationEventPublisher.publishEvent(new RfidTagDetectedEvent(epc));
            });
        }
    }

    @Override
    public void disconnect() {
        if (connection != null) {
            try { connection.disconnect(); } catch (Exception ignored) {}
        }
    }

    @Override
    public boolean isConnected() {
        return connection != null && connection.isConnected();
    }
}
```

#### Configuration Spring Boot

```java
// config/RfidConfiguration.java
@Configuration
@ConfigurationProperties(prefix = "rfid")
public class RfidConfiguration {

    private String readerHost = "192.168.1.20";
    private int readerPort = 5084;          // port LLRP standard
    private int connectionTimeoutMs = 5000;
    private int writeTimeoutMs = 2000;

    @Bean
    public RfidReaderClient rfidReaderClient() {
        return new LlrpReaderClientImpl();
    }

    @Bean
    public ApplicationRunner rfidAutoConnect(RfidReaderClient client) {
        return args -> {
            try {
                client.connect(readerHost, readerPort);
                log.info("RFID reader connected: {}:{}", readerHost, readerPort);
            } catch (RfidConnectionException e) {
                log.warn("RFID reader not available at startup — will retry");
                // retry géré par un @Scheduled
            }
        };
    }
}
```

#### Propriétés `application.yml`

```yaml
rfid:
  reader-host: 192.168.1.20       # IP du lecteur RFID caisse
  reader-port: 5084               # port LLRP standard
  connection-timeout-ms: 5000
  write-timeout-ms: 2000

portique:
  exit-host: 192.168.1.21         # IP du portique sortie
  exit-port: 8090                 # port API HTTP du portique (callback URL)
  sold-window-seconds: 300        # fenêtre de grâce vente → sortie (5 min)
  write-retry-max: 3              # max tentatives écriture tag échouée
```

---

### Gestion de la connexion LLRP (reconnexion automatique)

La connexion TCP avec le lecteur peut se perdre (reboot du lecteur, réseau instable).
Un `@Scheduled` gère la reconnexion automatique sans intervention manuelle :

```java
// service/portique/RfidConnectionWatchdog.java
@Component
@Slf4j
public class RfidConnectionWatchdog {

    private final RfidReaderClient client;
    private final RfidConfiguration config;

    @Scheduled(fixedDelay = 30_000)         // vérifie toutes les 30 secondes
    public void ensureConnected() {
        if (!client.isConnected()) {
            log.warn("RFID reader disconnected — attempting reconnect...");
            try {
                client.connect(config.getReaderHost(), config.getReaderPort());
                log.info("RFID reader reconnected");
            } catch (RfidConnectionException e) {
                log.error("RFID reconnect failed: {}", e.getMessage());
            }
        }
    }
}
```

---

### Comparaison finale des options de communication

| Critère | LLRP (Option A) | HTTP REST embarquée (Option B) |
|---|---|---|
| **Standard** | ISO/IEC 24791-3 — universel | Propriétaire selon marque |
| **Compatibilité** | Zebra, Impinj, Feig, tous LLRP | Zebra FX uniquement (ou Impinj) |
| **Complexité** | Moyenne (bibliothèque requise) | Faible (RestClient standard) |
| **Connexion** | TCP persistante (push temps réel) | HTTP polling ou webhook |
| **Écriture tag** | Native LLRP C1G2Write | Via API propriétaire |
| **Dépendance Maven** | `org.llrp:llrp-toolkit` | Aucune |
| **Recommandé si** | Multi-marque ou environnement pro | Lecteur unique, budget serré |

**Recommandation :** implémenter l'interface `RfidReaderClient` avec LLRP.  
Si le lecteur physique choisi expose aussi une API HTTP REST, switcher l'implémentation
sans changer les services métier.

---

*Document mis à jour le 2026-05-25 — Pharma-Smart / Portique Anti-Vol RFID*  
*Architecture : Hardware-first · 3 niveaux de mémoire · Spring Boot pilote l'écriture RFID · LLRP*
