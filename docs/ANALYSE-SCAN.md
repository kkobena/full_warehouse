# Analyse de la gestion du scan — Pharma-Smart

> Périmètre analysé :
> - `features/sales/data-access/services/sales-scanner.service.ts`
> - `features/commande/feature/commande-received/reception-scanner.service.ts`
> - `features/commande/feature/commande-received/commande-received.component.ts`
> - `features/sales/feature/sales-home/sales-home.component.ts`
> - Dépendances : `shared/scanner/base-scanner.service.ts`, `shared/global-scanner.service.ts`

---

## 1. Vue d'ensemble de l'architecture actuelle

| Couche | Vente (`sales-home`) | Réception (`commande-received`) |
|---|---|---|
| Source HID | `GlobalScannerService` (`providedIn: 'root'`) | `ReceptionScannerService` (instance par composant — extends `BaseScannerService`) |
| Orchestrateur HID + SERIAL | `SalesScannerService` (provider de composant) | **Logique inline dans le composant** (~250 lignes dans `commande-received.component.ts`) |
| Transport SERIAL | Tauri command `start_scanner_listener` + event `scan-vente` | Tauri command `start_scanner_listener` + event `scan-reception` |
| Détection clavier | Mode TIMING + PREFIX_SUFFIX (via `GlobalScannerService.processKeyEvent`) | Mode TIMING uniquement (via `BaseScannerService.processKey`) |
| Découverte device | `PosteDeviceService.getActiveDevice` puis `fetchAll` | Identique |
| Retry CDC | 8 s × 10 max | Identique (constantes dupliquées) |
| Délai de grâce buffer UART | 300 ms hardcodé | 300 ms hardcodé |
| Reconnexion auto sur `scan-error` | Oui | Oui |
| Bouton reconnexion manuelle | Oui (`canReconnect`) | Oui (`canReconnectScanner`) |

L'idée générale est correcte (HID en fallback, CDC en mode privilégié, retry, reconnexion auto), mais elle est implémentée **deux fois** avec des divergences subtiles.

---

## 2. Limites identifiées

### 2.1 Duplication massive de logique
La logique CDC + retry + bascule HID est **copiée-collée** entre `SalesScannerService` et `CommandeReceivedComponent`. Toute correction de bug devra être faite à deux endroits ; la dérive est déjà visible :

- Vente : `setupHid()` est centralisé / Réception : la version inline mélange HID + raccourcis clavier (`setupHidTimingFallback` + `setupKeyboardShortcutsOnly`).
- Vente utilise `GlobalScannerService.onScan$` (mode PREFIX_SUFFIX disponible) ; réception utilise `BaseScannerService.processKey` (TIMING uniquement → **PREFIX_SUFFIX inexploité côté réception**).
- Vente expose un service propre / réception a la logique imbriquée dans un composant déjà long (~1300 lignes).
- Constantes `RETRY_MAX`, `RETRY_INTERVAL_MS`, délai 300 ms : dupliquées sans source unique.

### 2.2 Couplage transport ↔ métier
- `SalesScannerService` est isolé mais le composant `CommandeReceivedComponent` mélange : transport (CDC), buffer (HID), file d'attente (`scanValue` + `onScanReception`), business (excédent / lot DataMatrix / association code provisoire). C'est **non testable unitairement**.
- L'import dynamique `await import('@tauri-apps/api/...')` est répété → pas d'abstraction injectable, donc impossible de mocker Tauri pour les tests.

### 2.3 Robustesse runtime

| Problème | Détail |
|---|---|
| **Pas de déduplication** | Un même code scanné 2× rapidement (rebond mécanique, double Enter) déclenche 2 ajouts. Aucune fenêtre anti-rebond. |
| **Pas de heartbeat** | Si la liaison SERIAL meurt silencieusement (câble coupé sans déclencher d'erreur), `scan-error` peut ne jamais arriver → scanner "vivant" mais muet, pas de bascule HID. |
| **Pas de timeout réseau** | `searchAndDispatch` (vente) et `scanReception` HTTP (réception) n'ont pas de timeout. Sur un backend lent, la file `scanQueue` se remplit indéfiniment. |
| **Délai de grâce hardcodé** | 300 ms pour vider l'UART : trop long pour des scanners rapides, trop court pour des scanners lents. Devrait être configurable par device. |
| **Pas de TTL sur `pendingScanCode`** | Vente : si `salesFacade.loading()` reste bloqué (erreur réseau), le scan en attente ne se déclenche jamais. |
| **`SCANNER_RUNNING` global Rust** | Singleton statique côté backend Rust — empêche d'avoir deux fenêtres avec deux scanners (futur multi-postes). |
| **Race conditions retry / reconnexion** | Mitigées par les `if scannerMode() === 'SERIAL'` mais multipliées dans le code → risque d'oubli sur évolution. |
| **`hidDestroyer$` et `serialDestroyRegistered`** | Logique stateful pour éviter les doublons de souscription : symptôme d'une mauvaise séparation des responsabilités. |
| **`scan-error` non typé** | Aucun payload : impossible de distinguer "déconnecté", "parité erronée", "timeout port" → impossible de donner un message d'erreur précis ou d'adapter la stratégie de retry. |

### 2.4 Effets de bord et fragilité
- `clearActiveInputAfterScan` (sales-home) lit `document.activeElement` et **modifie le DOM directement** (`activeEl.value = ''`, dispatch d'événement synthétique). Anti-pattern Angular qui contournera mal certaines situations (Angular Material, contrôles complexes, OverlayPanel ouverts).
- `onAssocierScanToProvisional` enchaîne **trois `setTimeout` imbriqués** (50 ms / 100 ms / 50 ms) pour piloter AG Grid. Très fragile aux changements de versions / async d'Angular.
- Vente : `searchAndDispatch` prend **toujours `results[0]`** quand plusieurs produits matchent un même code (même cas avec/sans warning). Un code partagé entre deux produits → choix muet et arbitraire.
- Vente : recherche pagination `size: 5` mais la décision est prise sur le 1er résultat sans afficher les autres → l'utilisateur ne saura jamais qu'il y avait ambiguïté.

### 2.5 Observabilité
- Pas de logs structurés (just `console.warn` silencieux dans certains catch).
- Pas de métriques : taux d'erreur, latence moyenne d'un scan → résolution, nombre de bascules HID/SERIAL par session.
- Pas d'audit trail : impossible de répondre à *« le scanner a-t-il bien lu le code à 14 h 22 ? »* en cas de litige inventaire.
- `scannerMode` est un signal simple — pas d'état intermédiaire (`DETECTING`, `RECONNECTING`, `READY`, `ERROR`) → l'utilisateur ne sait pas pourquoi un scan ne marche pas pendant la phase de retry.

### 2.6 UX et accessibilité
- Aucun feedback en cas d'ambiguïté multi-produit (vente).
- Le bouton reconnexion manuelle ne montre pas pourquoi la dernière tentative a échoué.
- Le badge `SERIAL` / `HID` est binaire ; pas de "🟡 reconnexion en cours (3/10)".
- Aucune indication visible quand `scanReady = false` (pendant les 300 ms de grâce) — l'utilisateur peut scanner et croire que ça n'a pas marché.

### 2.7 Configuration / paramétrage
- Mode TIMING vs PREFIX_SUFFIX configuré via param `APP_SCANNER_MODE` global, mais **uniquement appliqué en vente**. La réception ignore PREFIX_SUFFIX.
- Pas de stratégie par scanner : tous les scanners CDC utilisent `9600` baud par défaut, quel que soit le modèle. La table `PosteDevice` porte `baudRate` mais aucune autre info (parité, bits stop, suffixe attendu, longueur min/max, regex de validation).

---

## 3. Axes d'amélioration prioritaires

### 3.1 Refactor — extraire un orchestrateur unique
Créer un `ScannerOrchestratorService` factorisable, paramétré par :
- `eventName: 'scan-vente' | 'scan-reception' | …`
- `hidSource: BaseScannerService` (injection — `GlobalScannerService` ou `ReceptionScannerService`)
- `onScan: (code: string) => void`

→ Élimine ~250 lignes dupliquées et donne **un seul point de correction** pour la stratégie HID/CDC/retry.

### 3.2 State machine explicite
Remplacer les booleans (`scanReady`, `serialDestroyRegistered`, `processingQueue`, `scannerMode`) par une machine à états :

```
IDLE → DETECTING → CONNECTING → READY → SCANNING → READY
                ↓                ↓
                FALLBACK_HID → RETRYING → CONNECTING
                ↓
                ERROR (typé : NO_DEVICE | PORT_BUSY | DISCONNECT | …)
```

Exposée comme signal Angular pour brancher l'UI (badge + tooltip explicite).

### 3.3 Robustesse réseau
- Déduplication : ignorer le même code reçu < 400 ms après le précédent (par scanner).
- Timeout HTTP : `timeout(3000)` sur `searchAndDispatch` et `scanReception`, avec feedback sonore + visuel.
- File bornée : `scanQueue` limité (ex. 10) avec drop-oldest et notification utilisateur.
- TTL sur `pendingScanCode` (5 s max).

### 3.4 Robustesse transport
- Heartbeat côté Rust : ping périodique (ex. 30 s) qui vérifie que le port répond, sinon émet `scan-error` avec un payload typé.
- Backoff exponentiel sur le retry (8 s, 16 s, 32 s, capped) au lieu de 8 s constants — réduit la pression sur l'IO en cas d'arrachage durable.
- Retirer le `SCANNER_RUNNING` global : associer un identifiant de listener au port pour permettre N scanners simultanés.
- Payload `scan-error` typé : `{ code: 'DISCONNECTED' | 'TIMEOUT' | 'PARITY' | …, portName, details }`.

### 3.5 Observabilité
- Logger structuré (`ScannerLogger`) qui pousse chaque événement dans une liste ring-buffer en mémoire + (optionnel) en backend pour audit.
- Métriques exposées (Prometheus / endpoint `/api/metrics/scanner`) : compteurs par mode, latence p50/p95, taux de scan échoué, nombre de reconnexions par session.
- Mode debug activable depuis le settings : overlay temps réel des derniers scans + état machine.

### 3.6 UX
- Badge scanner enrichi : couleur + tooltip avec `mode + état + dernière activité + dernier port`.
- Disambiguation multi-produit : si la recherche renvoie ≥ 2 résultats, ouvrir un sélecteur visuel rapide (touche numérique 1-5) au lieu de prendre `results[0]`.
- Indicateur "en attente" pendant les 300 ms de grâce + pendant un retry CDC (sablier discret).
- Beep différencié : succès / non trouvé / ambigu / réseau lent.

### 3.7 Configuration
- Étendre `PosteDevice` : `parity`, `stopBits`, `dataBits`, `suffix`, `minLength`, `maxLength`, `prefix` configurables.
- Champ `gracePeriodMs` (par défaut 300) pour adapter au modèle.
- Champ `mode` (TIMING / PREFIX_SUFFIX / SERIAL) au niveau du device, plus précis que le param global.

### 3.8 Testabilité
- Abstraire `@tauri-apps/api/core` derrière une interface `TauriBridge` injectable.
- Tests unitaires sur l'orchestrateur (mock du bridge + mock du HID source).
- Tests e2e basiques : faux scanner émettant des codes via `window.postMessage`.

---

## 4. Critères d'un service de scan fiable

| # | Exigence | Acquis aujourd'hui ? |
|---|---|---|
| 1 | **Source unique de vérité** (un orchestrateur, pas N composants) | ❌ logique dupliquée vente/réception |
| 2 | **État observable typé** (state machine) | ⚠ booleans éparpillés |
| 3 | **Découplage transport / domaine** | ❌ réception : tout dans le composant |
| 4 | **Idempotence + déduplication** | ❌ |
| 5 | **Timeouts explicites** (network + transport) | ❌ |
| 6 | **Reconnexion automatique avec backoff** | ⚠ retry constant, pas de backoff |
| 7 | **Health check / heartbeat** | ❌ |
| 8 | **Erreurs typées** (`scan-error` payload, codes d'erreur business) | ❌ |
| 9 | **Logging structuré + audit trail** | ❌ |
| 10 | **Métriques / observabilité** | ❌ |
| 11 | **Feedback utilisateur cohérent** (visuel + sonore + textuel) | ⚠ partiel |
| 12 | **Configuration par device** (baud, parité, suffixe, délais) | ⚠ baudRate seulement |
| 13 | **Tests automatisés** (unit + e2e du flow scan) | ❌ |
| 14 | **Désambiguïsation multi-produit** | ❌ choix arbitraire |
| 15 | **Indépendance Tauri** (mockable) | ❌ import dynamique direct |
| 16 | **Support multi-fenêtres / multi-postes** | ❌ `SCANNER_RUNNING` statique |
| 17 | **Compatibilité PREFIX_SUFFIX uniforme** entre modules | ⚠ uniquement vente |

---

## 5. Plan de remédiation suggéré (par ordre de ROI)

1. **Quick wins (1-2 j)**
   - Déduplication temporelle (anti-rebond 400 ms).
   - Timeout `searchAndDispatch` + `scanReception` (3 s).
   - TTL sur `pendingScanCode`.
   - Désambiguïsation visuelle quand N résultats > 1.

2. **Refactor structurel (3-5 j)**
   - Extraire `ScannerOrchestratorService` partagé (vente + réception).
   - Migrer `commande-received.component.ts` pour ne consommer que `onScan$`.
   - Introduire la state machine + signal `scannerStatus`.

3. **Robustesse transport (3-5 j)**
   - Heartbeat côté Rust + payload typé `scan-error`.
   - Backoff exponentiel.
   - Suppression du `SCANNER_RUNNING` statique (clé par portName).

4. **Observabilité (2-3 j)**
   - Logger ring-buffer + endpoint d'audit.
   - Métriques Prometheus minimales.
   - Overlay debug optionnel.

5. **Configuration & UX avancée (selon besoin terrain)**
   - Champs additionnels `PosteDevice` (parity, suffix, gracePeriodMs).
   - Disambiguation produit.
   - Mode PREFIX_SUFFIX en réception.

---

## 6. Conclusion

L'architecture actuelle **fonctionne** et le mécanisme HID ↔ SERIAL avec retry est plutôt bien pensé sur le principe. Les deux problèmes de fond sont :

1. **La duplication** entre vente et réception, qui condamne le code à dériver.
2. **L'absence de garde-fous runtime** (déduplication, timeout, heartbeat, état typé, observabilité) — qui sont précisément ce qui distingue un scanner *qui marche en démo* d'un scanner *qui marche en officine 8 h par jour pendant 3 ans*.

Le refactor `ScannerOrchestratorService` + state machine + déduplication + timeouts est la priorité. Tout le reste s'enchaîne naturellement une fois cette fondation posée.
