# Plan d'amélioration — Module Réception BL

> **Date :** 29 avril 2026  
> **Basé sur :** `ANALYSE-MODULE-RECEPTION-BL.md`  
> **Périmètre :** `features/commande/feature/commande-received/` + `sequential/`  
> **Principe :** chaque item est autonome et livrable indépendamment.

---

## Légende

| Symbole | Sens |
|---|---|
| ✅ | Déjà implémenté |
| 🔴 | Priorité haute — bloquant ou impact utilisateur fort |
| 🟡 | Priorité moyenne — amélioration significative |
| 🟢 | Priorité basse — nice-to-have |
| 🏗️ | Nécessite du backend Java |
| 🎨 | Frontend Angular uniquement |
| 🦀 | Nécessite du code Rust (Tauri) |
| ⚙️ | Configuration uniquement (zéro code) |

---

## Ce qui est déjà fait ✅

| ID | Description | Fichier concerné |
|---|---|---|
| ✅ **AX-05** | `scanLotPrefill` effacé à la validation de la ligne (plus de timer fixe 10 s) | `commande-received.component.ts` |
| ✅ **AX-15a** | `ReceptionScannerService` scopé au composant — buffer isolé de la vente | `reception-scanner.service.ts` |
| ✅ **AX-15b** | `event.preventDefault()` anti-pollution des inputs lors d'un scan en cours | `commande-received.component.ts` |
| ✅ **Provisional** | `provisionalCode` + colonne CIP éditable AG Grid + `rowClassRules` fond jaune + filtre `PROVISOL_CIP` + dialog substitution | `commande-received.component.ts` |

---

## Sprint 1 — Quick wins · < 2 jours · 🎨 Frontend pur

> Aucun changement backend. Aucune migration. Gains visibles immédiatement.

---

### AX-01 · Feedback sonore scan ✅ IMPLÉMENTÉ

**Service :** `shared/services/scan-audio-feedback.service.ts` — déjà existant.

**Intégration réalisée dans `commande-received.component.ts` :**
```typescript
// inject
private readonly audioFeedback = inject(ScanAudioFeedbackService);

// dans onScanReception() → next:
if (result.found) {
  this.audioFeedback.beepSuccess(); // 800 Hz, 100 ms
} else {
  this.audioFeedback.beepError();   // 300 Hz, 300 ms
}
```

---

### AX-02 · Flash ligne + auto-scroll après scan en mode grille 🔴 🎨

**Quoi :** Après un scan réussi, la ligne mise à jour clignote 1 s en vert et la grille défile vers elle.

**Pourquoi :** L'opérateur ne perd pas de vue la ligne qu'il vient de scanner.

**Où :** `commande-received.component.ts` — après `refreshCells()` dans `onScanReception()`.

```typescript
const rowNode = this.gridApi?.getRowNode(String(updated.id));
if (rowNode) {
  this.gridApi?.ensureNodeVisible(rowNode, 'middle');
  this.gridApi?.flashCells({ rowNodes: [rowNode], flashDuration: 800, fadeDuration: 400 });
}
```

**Effort :** ~8 lignes dans le code existant.

---

### AX-03 · Écart colisage dans le panneau concordance 🟡 🎨

**Quoi :** Ajouter un compteur `ecartColisage` dans `ConcordanceStats` et l'afficher dans
`ReceptionConcordanceComponent` et dans le récapitulatif final.

**Pourquoi :** L'alerte PCB existe dans la grille (badge) mais disparaît du panneau concordance
et du modal de finalisation.

**Où :**
- `reception-concordance.component.ts` — calculer `ecartColisage`
- `reception-finalize-modal.component.html` — ajouter la ligne dans le récapitulatif

**Effort :** ~15 lignes.

---

### AX-04 · Unifier la logique de finalisation 🟡 🎨

**Quoi :** Les deux modes (séquentiel via `ReceptionFinalizeModal` et grille via
`onConfirmFinalize()`) passent par deux chemins différents mais font exactement la même chose.

**Solution :** Extraire la chaîne `checkPriceVariation → checkPutaway → finalizeSaisieEntreeStock
→ checkReliquat → printEtiquette` dans une méthode privée `executeFinalizationFlow()` appelée
par les deux entrées.

**Effort :** ~30 lignes de refactoring, zéro régression fonctionnelle.

---

## Sprint 2 — Scan & codes CIP · 2–3 jours · 🎨 Frontend

> Priorité : AX-23 en premier (impact direct opérateur), puis AX-15 et AX-09.

---

### AX-23 · Pont Scan-to-Provisional (codes CIP à mettre à jour) 🔴 🎨

> **Contexte :** Quand un scan retourne `found = false`, le mécanisme `provisionalCode` existe
> déjà (ligne fond jaune, filtre PROVISOL_CIP, cellule CIP éditable en un clic). Le seul
> problème : le code scanné est **perdu** et l'opérateur doit re-scanner dans la cellule.
> Ces 3 sous-tâches corrigent ça en ~70 lignes — aucun endpoint backend nécessaire.

#### AX-23a · Détecter les lignes provisoires lors d'un scan inconnu 🔴 🎨

**Où :** `commande-received.component.ts` — méthode `onScanReception()`

```typescript
if (!result.found) {
  const provisionalLines = this.orderLines
    .filter(l => l.provisionalCode)
    .map(l => ({ id: l.id!, libelle: l.produitLibelle! }));
  // Enrichir le signal avec les lignes disponibles + le code scanné
  this.lastScanResult.set({ ...result, provisionalLines, scannedCode: raw });
}
```

**Effort :** ~15 lignes.

#### AX-23b · Badge scan orange + bouton "Associer" 🔴 🎨

**Où :** `commande-received.component.html` — bloc `@if (!lastScanResult()!.found)`

```html
@if (lastScanResult()!.provisionalLines?.length) {
  <!-- Orange : lignes provisoires disponibles -->
  <span class="cr-scan-badge cr-scan-badge--provisional">
    <i class="pi pi-exclamation-triangle"></i>
    Code inconnu — {{ lastScanResult()!.provisionalLines!.length }} ligne(s) provisoire(s)
    <button type="button" (click)="onAssocierScanToProvisional()">
      <i class="pi pi-link"></i> Associer
    </button>
  </span>
} @else {
  <!-- Rouge : code vraiment inconnu, aucune ligne provisoire -->
  <span class="cr-scan-badge cr-scan-badge--err">
    <i class="pi pi-times-circle"></i>{{ lastScanResult()!.warningMessage }}
  </span>
}
```

**Effort :** ~12 lignes HTML.

#### AX-23c · Méthode `onAssocierScanToProvisional()` 🔴 🎨

**Où :** `commande-received.component.ts`

```typescript
protected onAssocierScanToProvisional(): void {
  const scan = this.lastScanResult();
  if (!scan?.provisionalLines?.length || !scan.scannedCode) return;
  const rowNode = this.gridApi?.getRowNode(String(scan.provisionalLines[0].id));
  if (!rowNode) return;
  this.gridApi?.ensureNodeVisible(rowNode, 'middle');
  setTimeout(() => {
    this.gridApi?.startEditingCell({ rowIndex: rowNode.rowIndex!, colKey: 'produitCip' });
    setTimeout(() => {
      const eInput = document.querySelector<HTMLInputElement>('.ag-cell-editor input');
      if (eInput) { eInput.value = scan.scannedCode!; eInput.dispatchEvent(new Event('input')); }
    }, 50);
  }, 100);
  this.lastScanResult.set(null);
}
```

**Effort :** ~20 lignes.

---

### AX-23d · Banner CIP provisoire en mode séquentiel 🟡 🎨

**Quoi :** Quand la ligne courante a `provisionalCode = true`, afficher un bandeau d'alerte dans
le `product-header`. Si un scan `found = false` arrive, proposer directement
"Associer ce code à ce produit".

**Où :**
- `reception-sequential.component.ts` — `signal pendingCipAssociation` + `effect` sur `lastScanResult`
- `reception-sequential.component.html` — bandeau sous le `product-header`

```typescript
// effect dans le constructeur
effect(() => {
  const scan = this.lastScanResult();
  untracked(() => {
    if (!scan || scan.found) return;
    if (this.currentLine()?.provisionalCode && scan.scannedCode) {
      this.pendingCipAssociation.set(scan.scannedCode);
    }
  });
});
```

```html
@if (currentLine()?.provisionalCode) {
  <div class="rh-seq__provisional-cip-banner">
    <i class="pi pi-exclamation-triangle"></i>
    <span>Code CIP provisoire — à mettre à jour</span>
    @if (pendingCipAssociation()) {
      <span>Code scanné : <strong>{{ pendingCipAssociation() }}</strong></span>
      <p-button  size="small" severity="warn" icon="pi pi-check"
              label="Associer" (click)="onConfirmCipAssociation()"></p-button>
    } @else {
      <p-button pButton size="small" severity="secondary" icon="pi pi-pencil"
              label="Modifier CIP" (click)="isEditingCip.set(true)"></p-button>
    }
  </div>
}
```

**Effort :** ~25 lignes TS + HTML.

---

### AX-15 · Feedback scan visible en mode séquentiel 🔴 🎨

**Quoi :** Passer le signal `lastScanResult` en `input()` de `ReceptionSequentialComponent`
et afficher une bande animée sous le `product-header` (verte si trouvé, rouge sinon,
avec badges lot et FMD).

**Où :**
- `commande-received.component.html` — ajouter `[lastScanResult]="lastScanResult()"`
- `reception-sequential.component.ts` — `lastScanResult = input<IReceptionScanResult | null>(null)`
- `reception-sequential.component.html` — bloc `rh-seq__scan-feedback` après `product-header`

```html
@if (lastScanResult(); as scan) {
  <div class="rh-seq__scan-feedback"
       [class.rh-seq__scan-feedback--ok]="scan.found"
       [class.rh-seq__scan-feedback--err]="!scan.found">
    <i [class]="scan.found ? 'pi pi-check-circle' : 'pi pi-times-circle'"></i>
    <span>{{ scan.found ? scan.produitLibelle : (scan.warningMessage ?? 'Produit non trouvé') }}</span>
    @if (scan.found && scan.lotNumero) {
      <span class="rh-seq__scan-chip rh-seq__scan-chip--lot">
        <i class="pi pi-box"></i>{{ scan.lotNumero }}
      </span>
    }
    @if (scan.found && scan.fmdStatus === 'PRESENT') {
      <span class="rh-seq__scan-chip rh-seq__scan-chip--fmd-ok">
        <i class="pi pi-shield"></i>FMD ✓
      </span>
    }
    @if (scan.found && scan.fmdStatus === 'DUPLICATE') {
      <span class="rh-seq__scan-chip rh-seq__scan-chip--fmd-dup">
        <i class="pi pi-shield"></i>DOUBLON !
      </span>
    }
  </div>
}
```

**Effort :** ~30 lignes TS + HTML.

> Détail complet : `ANALYSE-MODULE-RECEPTION-BL.md` §4.2

---

### AX-09 · Bandeau concordance compact en mode séquentiel 🟡 🎨

**Quoi :** Insérer un bandeau de 28 px entre `nav-header` et `nav-list`. Il affiche
`X/Y lignes · N écarts qté · N écarts prix · N lots manquants`. Vert quand tout est conforme.

**Où :** `reception-sequential.component.ts` + `.html`

```typescript
protected readonly navConcordance = computed(() => {
  const lines = this.orderLines();
  let ecartQte = 0, ecartPrix = 0, lotsManquants = 0;
  for (const l of lines) {
    if ((l.quantityReceivedTmp ?? 0) !== (l.quantityRequested ?? 0)) ecartQte++;
    if (l.costAmount && l.orderCostAmount && l.costAmount !== l.orderCostAmount) ecartPrix++;
    if (this.showLotBtn() && l.gestionLot !== false && (l.lots?.length ?? 0) === 0) lotsManquants++;
  }
  return { ecartQte, ecartPrix, lotsManquants,
           hasAnomaly: ecartQte + ecartPrix + lotsManquants > 0 };
});
```

**Effort :** ~25 lignes TS + HTML + 8 lignes SCSS. Aucun appel HTTP.

> Détail complet : `ANALYSE-MODULE-RECEPTION-BL.md` §4.1

---

## Sprint 3 — Données & affichage · 2–3 jours · 🎨 Frontend

---

### AX-13 · État "À saisir" dans la colonne Statut 🟡 🎨

**Quoi :** Ajouter un 5ème état `"À saisir"` dans `lineStatut()` et
`CommandeReceivedStatutComponent`. Distinguer une ligne jamais ouverte d'une ligne saisie à 0.

**Où :** `commande-received.component.ts` — méthode `lineStatut()` + `commande-received-statut.component.ts`

```typescript
// Règle : quantityReceivedTmp est null (jamais touché) → "À saisir"
if (ol.quantityReceivedTmp == null) return { label: 'À saisir', severity: 'secondary' };
```

**Effort :** ~10 lignes.

---

### AX-08 · Taux de service du BL dans le header 🟡 🎨

**Quoi :** Badge `"Taux de service : 87%"` dans la barre header.
Calculé : `lignes avec quantityReceivedTmp >= quantityRequested / total lignes`.

**Où :** `commande-received.component.ts` (computed) + `commande-received.component.html` (header)

**Effort :** ~15 lignes.

---

### AX-10 · Badge chaîne du froid en mode séquentiel 🔴 🎨

**Quoi :** Si le produit est thermosensible, afficher un badge `"❄ < 8°C"` dans le
`product-header` séquentiel.

**À vérifier :** présence d'un champ `thermosensible` ou `conservationTemperature` dans
`Produit.java` / `IOrderLine`. Si absent → migration + champ à ajouter.

**Effort :** ~10 lignes HTML + 1 computed (si le champ existe déjà).

---

### AX-11 · Numéro de commande fournisseur (PO) dans le résumé BL 🟡 🎨

**Quoi :** Afficher le numéro PO fournisseur dans le panneau "Détails de la réception"
(mode grille, panneau gauche).

**Champ :** `Commande.orderReference` — déjà présent. Vérifier si `supplierReference` est
distinct et plus pertinent.

**Effort :** ~5 lignes HTML.

---

### AX-06 · Colonne TVA dans la grille 🟢 🎨

**Quoi :** Colonne `tva` (taux %) masquée par défaut, activable via le menu colonnes AG Grid.

**Champ :** `OrderLine.tva` (relation `@ManyToOne` vers `Tva`) — déjà en base.

**Effort :** ~8 lignes dans `buildColumnDefs()`.

---

### AX-07 · Colonnes remise et montant net 🟢 🎨

**Quoi :** Colonnes `discountAmount` et `netAmount` dans la grille, masquées par défaut.

**Champs :** déjà mappés dans `OrderLine.java`.

**Effort :** ~12 lignes dans `buildColumnDefs()`.

---

### AX-12 · Colonne jours de couverture stock 🟢 🎨

**Quoi :** Colonne `couvertureStockJours`, masquée par défaut.

**À vérifier :** champ `couvertureStockJours` présent dans le DTO retourné par
`CommandeService.filterCommandeLines()`.

**Effort :** ~8 lignes dans `buildColumnDefs()`.

---

## Sprint 4 — Scan avancé · 3–5 jours

---

### AX-14 · Parser AI 37 (quantité) dans les DataMatrix 🟡 🏗️🎨

**Quoi :** Les DataMatrix GS1 peuvent encoder la quantité dans l'AI 37. Au lieu de toujours
incrémenter de +1, utiliser la valeur de l'AI 37 si présente.

**Ce qu'il faut faire :**
1. **Backend** `DeliveryService.scanReception()` — parser AI 37, retourner `scannedQty` dans le DTO
2. **Frontend** `onScanReception()` — utiliser `result.scannedQty ?? 1`

**Effort :** ~20 lignes Java + ~5 lignes TypeScript.

---


### AX-17 · Raccourcis clavier manquants 🟢 🎨

**Quoi :**

| Raccourci | Action |
|---|---|
| `Ctrl+G` ou `Ctrl+S` | Basculer Séquentiel ↔ Grille |
| `F5` | Rafraîchir les lignes BL |
| `F11` | Tout valider |
| `Alt+H` | Ouvrir l'aide |

**Où :** `commande-received.component.ts` — listener `keydown` global.

**Effort :** ~20 lignes.

---

### AX-23g · Rapport "lignes CIP mises à jour" dans la finalisation 🟢 🎨

**Quoi :** Afficher la liste des lignes dont le CIP a été modifié pendant ce BL dans
`ReceptionFinalizeModalComponent`.

**Données :** lignes avec `updated = true` ou `provisionalCode` passé de `true` à `false`.

**Effort :** ~10 lignes frontend.

---

## Sprint 6 — Fonctionnalités métier · 🏗️ Backend + 🎨 Frontend

---

### AX-20 · Récapitulatif TVA par taux dans le modal de finalisation 🔴 🎨

**Quoi :** Tableau `Taux 0% / 5,5% / 10% / 20% → Montant HT → Montant TVA` dans
`ReceptionFinalizeModalComponent`.

**Données :** `OrderLine.taxAmount` + `OrderLine.tva.taux` — déjà en base.

**Effort :** ~25 lignes frontend uniquement (grouper par taux dans un computed).

---


### AX-22 · Lien direct vers le module de rapprochement facture 🟡 🎨

**Quoi :** Bouton `"Rapprocher la facture"` dans le header du BL, naviguant vers
`reconciliation-facture` avec le BL pré-sélectionné.

**Effort :** ~5 lignes HTML + routage paramétré.

---

## Sprint 5 — Infrastructure scanner Tauri · ~2 jours · 🦀 Rust + 🎨 Angular · **PRIORITÉ**

> **Priorité maximale pour le déploiement desktop Tauri.**  
> C'est la solution la plus fiable pour les officines — indépendante du clavier, zéro  
> faux positif, zéro pollution des inputs.

### Réalité terrain : USB HID par défaut + problème des drivers CDC

> **Les douchettes USB sont en mode HID clavier (émulation clavier) par défaut.**  
> Passer en **mode USB CDC (Virtual COM)** peut nécessiter des **drivers supplémentaires**
> selon le fabricant et la version de Windows — c'est un risque d'installation en officine.

#### Situation des drivers USB CDC par fabricant

| Fabricant | Windows 10/11 | Windows 7/8 | USB CDC possible | Remarque |
|---|---|---|---|---|
| **Honeywell / Metrologic** | ✅ Driver intégré `usbser.sys` | ⚠️ Driver fabricant | ✅ Oui | Séries Voyager, Xenon |
| **Zebra / Symbol** | ✅ Driver intégré | ⚠️ Driver fabricant | ✅ Oui | Séries DS, LS |
| **Datalogic** | ⚠️ Driver fabricant requis | ❌ | ✅ Oui | Toutes séries |
| **Newland** | ✅ Souvent intégré | ⚠️ Variable | ✅ Oui | Courant Afrique de l'Ouest |
| **Opticon** | ⚠️ Driver fabricant requis | ❌ | ✅ Oui | |
| **NETUM RF 2.4G** | ⚠️ **Pilote fabricant requis** | ❌ | ✅ Oui (avec pilote) | "Émulation Port USB COM" dans le manuel — mais nécessite l'installation du pilote NETUM |
| **Marques génériques USB** | ❌ Très variable | ❌ | ❌ Souvent non | Risque élevé en officine |

> **Cas NETUM RF 2.4G :**
> - "Émulation Port USB COM" disponible dans le manuel ✅
> - Par défaut : **HID clavier** via le dongle 2.4G
> - Pour le mode COM : scan d'étiquette config **+ installation du pilote NETUM** ⚠️
> - Sans pilote installé → le port COM ne s'affiche pas → fallback automatique TIMING
> - **VID/PID variable selon les lots** → détection par classe USB plus fiable que par VID
>
> Ce cas confirme que **le pilote est le vrai obstacle**, pas la config scanner.
> L'approche `hidapi` (HID direct) évite complètement ce problème :
> **zéro pilote, zéro config, compatible avec le NETUM en mode HID par défaut.**

#### Le cas des dongles 2.4G (NETUM, Symcode, Inateck sans fil…)

Les scanners sans fil avec dongle USB 2.4G présentent un défi supplémentaire :
- Le **VID/PID appartient au dongle**, pas au scanner physique → variable selon les lots
- **Solution :** détection par **classe USB HID** (0x03) + **Usage Page barcode** (0x8C) plutôt que par VID

```rust
// Détection par classe HID + Usage Page scanner (plus universel que VID)
// HID Usage Page 0x8C = "Bar Code Scanner Page" (USB HID spec)
// Permet de détecter les dongles 2.4G génériques sans connaître leur VID
fn is_barcode_scanner_hid(device: &Device<impl UsbContext>) -> bool {
    // Vérifier la classe HID + lire le descripteur HID pour l'Usage Page
    // Usage Page 0x8C (Bar Code Scanner) ou Usage Page 0x01 (Generic Desktop)
    // avec Usage 0x06 (Keyboard) mais avec report format de scanner
    if let Ok(config) = device.active_config_descriptor() {
        for iface in config.interfaces() {
            for desc in iface.descriptors() {
                if desc.class_code() == 0x03 { // HID class
                    return true; // Scanner HID potentiel
                }
            }
        }
    }
    false
}
```

> **Conclusion pratique :** Pour les dongles 2.4G, `hidapi` est la **seule solution Tauri
> viable** — et elle fonctionne sans driver, sans config, sur tous les Windows.

#### Alternative sans driver : `hidapi` — lecture HID directe en Rust ← PRIORITÉ ABSOLUE

**Objectif principal : éviter que l'utilisateur ait à placer son curseur dans un input avant de scanner.**

##### Pourquoi le problème existe en mode HID TIMING actuel

```
[Scanner]  →  [Windows HID Driver]  →  [Event Queue OS]  →  [keydown/keyup Tauri/WebView]
                                                                        ↓
                                                          Angular reçoit les events clavier
                                                          → les données arrivent dans l'input ACTIF
                                                          → si aucun input actif → les données sont perdues
                                                          → si mauvais input actif → pollution du champ
```

**Le problème :** le scanner se comporte comme un clavier. Windows envoie les events clavier à l'élément qui a le focus. Si l'utilisateur a cliqué ailleurs, le code se perd ou polue un champ.

AX-15b (`preventDefault`) atténue la pollution, mais ne résout pas le problème du focus manquant.

##### Comment `hidapi` résout le problème à la racine

```
[Scanner]  →  [Windows HID Driver]  →  [Windows HID Raw API (ReadFile)]
                                                 ↑
                                    hidapi ouvre le device en "exclusive mode"
                                    → lit les rapports USB directement (8 octets)
                                    → les keystrokes n'atteignent JAMAIS le navigateur
                                    → Rust décode report → chaîne barcode
                                    → app.emit('scan-reception', code)
                                                 ↓
                                    [Angular onScanReception()]
                                    → peu importe le focus → toujours reçu ✅
```

**L'utilisateur peut scaner à tout moment, quelle que soit la position du curseur.**  
Le code arrive directement dans `onScanReception()` via l'event Tauri, jamais via le clavier.

##### Implémentation Rust `hidapi`

```rust
use hidapi::{HidApi, HidDevice};
use tauri::{AppHandle, Emitter};
use std::thread;

// Usage Page 0x01 (Generic Desktop) + Usage 0x06 (Keyboard) = scanner HID standard
// La plupart des scanners USB (y compris dongles 2.4G) utilisent ce profil

#[tauri::command]
pub async fn start_hid_scanner_listener(app: AppHandle, vid: u16, pid: u16) 
    -> Result<(), String> 
{
    thread::spawn(move || {
        let api = HidApi::new().expect("HID API init failed");
        // Ouvrir le device en mode exclusif → les keystrokes ne passent plus au navigateur
        let device = api.open(vid, pid)
            .expect("Cannot open HID device");
        
        let mut buf = String::new();
        let mut report = [0u8; 9]; // Rapport HID clavier standard = 8 octets + report ID
        
        loop {
            if device.read_timeout(&mut report, 100).is_ok() {
                let modifier = report[1]; // Shift, Ctrl, Alt…
                let keycode  = report[3]; // Usage ID principal
                
                match keycode {
                    // Enter (0x28) = fin du code-barres → émettre
                    0x28 => {
                        if !buf.is_empty() {
                            app.emit("scan-reception", buf.clone()).ok();
                            buf.clear();
                        }
                    }
                    // Digits et lettres → accumuler
                    _ => {
                        let shift = modifier & 0x22 != 0; // Left/Right Shift
                        if let Some(c) = hid_keycode_to_char(keycode, shift) {
                            buf.push(c);
                        }
                    }
                }
            }
        }
    });
    Ok(())
}

fn hid_keycode_to_char(keycode: u8, shift: bool) -> Option<char> {
    match keycode {
        0x04..=0x1D => {
            let base = b'a' + (keycode - 0x04);
            Some(if shift { (base - 32) as char } else { base as char })
        }
        0x1E..=0x26 => Some((b'1' + (keycode - 0x1E)) as char),
        0x27 => Some('0'),
        // Caractères spéciaux courants dans les codes EAN/CIP
        0x2D => Some(if shift { '_' } else { '-' }),
        0x2E => Some(if shift { '+' } else { '=' }),
        _ => None,
    }
}
```

##### Détection automatique du VID/PID pour `hidapi`

```rust
// SC-01 enrichi : detect_scanner retourne aussi le VID/PID pour hidapi
// Angular appelle ensuite start_hid_scanner_listener(vid, pid) directement
// → zéro config manuelle, zéro focus requis ✅
```

##### Comparaison finale — ce que l'utilisateur ressent

| Mode | Focus requis avant scan | Config requise | Driver |
|---|---|---|---|
| **`hidapi` Tauri** | ❌ **Non** | ❌ Non | ❌ Non |
| **HID TIMING actuel** | ⚠️ Oui (ou focus géré par le composant) | ❌ Non | ❌ Non |
| **USB CDC serialport** | ❌ Non | ✅ Oui — USB COM | ⚠️ Parfois |

> **`hidapi` est la solution la plus transparente pour l'utilisateur :**
> il branche la douchette, il scanne, ça marche — sans jamais penser au focus ou à la configuration.

### Stratégie de détection et d'accès au scanner — révisée

```
Au démarrage du BL (ngOnInit)
        ↓
invoke('detect_scanner_and_connect')
        ↓
┌─ Scanner VID connu → essai HID direct (hidapi) ──────────────────────────┐
│  Aucun driver requis — Windows HID API natif                             │
│  → Lire les rapports HID → décoder les keycodes → code-barres            │
│  → emit('scan-reception', code) → Angular                               │
│  → 100% isolé du clavier, aucune config manuelle ✅                       │
└───────────────────────────────────────────────────────────────────────────┘
                  ↓ Si hidapi échoue (permission, périph. occupé)
┌─ Essai CDC (serialport) ─────────────────────────────────────────────────┐
│  → Port COM virtuel détecté → ouvrir → lire                              │
│  → Nécessite que le scanner soit en mode CDC ET driver installé          │
└───────────────────────────────────────────────────────────────────────────┘
                  ↓ Si les deux échouent ou scanner non reconnu
┌─ Fallback HID TIMING + preventDefault ───────────────────────────────────┐
│  ✅ Toujours actif, aucune dépendance                                     │
└───────────────────────────────────────────────────────────────────────────┘
```

### Stratégie de fallback — 3 niveaux imbriqués

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Niveau 3 — Tauri Serial Port USB CDC / série physique  ← PRIORITÉ      │
│  Configuration : scanner en mode USB CDC (1 fois, manuel)               │
│  → Port COM virtuel → Rust lit → emit→ Angular                          │
│  → 100% isolé du clavier, zéro faux positif, zéro pollution inputs      │
│  → Libéré entre 2 BL (pas de conflit avec la caisse ou autres logiciels)│
└─────────────────────────────────────────────────────────────────────────┘
            ↓ Si non configuré (port absent ou scanner en HID)
┌─────────────────────────────────────────────────────────────────────────┐
│  Niveau 2 — HID STX/ETX  (optionnel, si scanner configuré STX/ETX)     │
│  Configuration : scanner en mode STX/ETX (1 fois, manuel)              │
│  → Zéro faux positif sur frappe normale                                 │
└─────────────────────────────────────────────────────────────────────────┘
            ↓ Si non configuré (scanner USB HID par défaut usine)
┌─────────────────────────────────────────────────────────────────────────┐
│  Niveau 1 — HID TIMING + preventDefault  ← TOUJOURS ACTIF, ZÉRO CONFIG │
│  ✅ Déjà en production (AX-15b)                                          │
│  → Fonctionne dès la sortie de boîte, aucune action requise             │
│  → Protection inputs déjà en place                                      │
└─────────────────────────────────────────────────────────────────────────┘
```

**Comparaison des approches pour douchette USB :**

| Mode | Driver requis | Config scanner | Dongle 2.4G | Déploiement |
|---|---|---|---|---|
| **HID direct → `hidapi`** | ❌ Aucun | ❌ Aucune | ✅ Oui | Tauri desktop ← **PRIORITÉ** |
| **USB CDC → `serialport`** | ⚠️ Parfois | Oui — 1 étiquette | ❌ **Non** | Tauri (IT géré, câblé) |
| **USB HID + STX/ETX** | ❌ Aucun | Oui — 1 étiquette | ❌ Non | Tous (câblé haut de gamme) |
| **USB HID + TIMING** | ❌ Aucun | ❌ Aucune | ✅ Oui | Tous — défaut actuel |

---

### SC-01 · Commande Rust `detect_scanner` + `list_serial_ports_detailed` 🔴 🦀

**Principe de détection — 3 couches :**

```
SCANNER_VENDORS = table VID → nom fabricant
│
├─ Couche 1 : serialport (ports COM)
│   Si VID connu apparaît en port COM → mode CDC ✅ → retourner port_name + vid/pid
│
├─ Couche 2 : rusb (énumération USB complète)
│   Si VID connu apparaît USB mais PAS en COM → mode HID → retourner vid/pid + conseil
│
└─ Couche 3 : détection générique par classe HID (pour dongles 2.4G, NETUM…)
    Si VID inconnu MAIS device = classe 0x03 (HID) à haute fréquence de rapport → scanner probable
    → retourner vid/pid + mode HID (pour hidapi) sans nom fabricant
```

**Pourquoi `SCANNER_VENDORS` ne suffit pas pour les génériques :**
- NETUM, Symcode et autres dongles 2.4G ont des VID variables selon les lots
- Ils ne figurent pas dans la liste → `detect_scanner()` retournerait `NONE` sans la couche 3
- La couche 3 (classe HID) permet de les détecter même sans VID connu

**Dépendances :**
```toml
serialport = { version = "4.x" }  # déjà présent — couche 1 CDC
rusb       = "0.9"                 # NOUVEAU — couche 2 + 3 USB complet
hidapi     = "2.6"                 # NOUVEAU — lecture HID exclusive (SC-02)
```

**Où :** `src-tauri/src/scanner.rs` (nouveau fichier)

```rust
use rusb::{Context, UsbContext};
use serde::Serialize;

// Fabricants connus → permet d'afficher un nom lisible à l'utilisateur
// et de prioriser ces devices pour hidapi
const SCANNER_VENDORS: &[(u16, &str)] = &[
    (0x0536, "Honeywell / Metrologic"),
    (0x05E0, "Zebra / Symbol"),
    (0x0C2E, "Datalogic"),
    (0x04B4, "Newland / Cypress"),   // courant en Afrique de l'Ouest
    (0x0483, "Opticon"),
    (0x1EAB, "Inateck"),
    // NETUM, Symcode, génériques → non listés → détectés par classe HID (couche 3)
];

#[derive(Serialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct ScannerDetectionResult {
    pub detected: bool,
    pub manufacturer: Option<String>, // None si générique inconnu
    pub mode: String,                 // "CDC" | "HID" | "NONE"
    pub port_name: Option<String>,    // ex: "COM6" si mode CDC
    pub usb_vid: Option<u16>,         // transmis à hidapi.open(vid, pid)
    pub usb_pid: Option<u16>,         // transmis à hidapi.open(vid, pid)
    pub guidance: Option<String>,     // conseil affiché si mode HID
}

#[tauri::command]
pub fn detect_scanner() -> ScannerDetectionResult {
    // ── Couche 1 : VID connu en port COM → mode CDC ─────────────────────────
    if let Ok(ports) = serialport::available_ports() {
        for port in &ports {
            if let serialport::SerialPortType::UsbPort(info) = &port.port_type {
                if let Some((_, name)) = SCANNER_VENDORS.iter()
                    .find(|(vid, _)| *vid == info.vid) {
                    return ScannerDetectionResult {
                        detected: true, manufacturer: Some(name.to_string()),
                        mode: "CDC".to_string(), port_name: Some(port.port_name.clone()),
                        usb_vid: Some(info.vid), usb_pid: Some(info.pid), guidance: None,
                    };
                }
            }
        }
    }

    if let Ok(ctx) = Context::new() {
        if let Ok(devices) = ctx.devices() {
            // ── Couche 2 : VID connu mais PAS en COM → mode HID ──────────────
            for device in devices.iter() {
                if let Ok(desc) = device.device_descriptor() {
                    if let Some((_, name)) = SCANNER_VENDORS.iter()
                        .find(|(vid, _)| *vid == desc.vendor_id()) {
                        return ScannerDetectionResult {
                            detected: true, manufacturer: Some(name.to_string()),
                            mode: "HID".to_string(), port_name: None,
                            usb_vid: Some(desc.vendor_id()), usb_pid: Some(desc.product_id()),
                            guidance: Some(format!(
                                "Scanner {} détecté en mode HID. Mode actuel : opérationnel.", name
                            )),
                        };
                    }
                }
            }
            // ── Couche 3 : VID inconnu mais classe HID → générique/dongle 2.4G ─
            for device in devices.iter() {
                if let Ok(desc) = device.device_descriptor() {
                    if let Ok(config) = device.active_config_descriptor() {
                        for iface in config.interfaces() {
                            for idesc in iface.descriptors() {
                                // Classe 0x03 = HID, sous-classe 0x01 = Boot Interface (clavier/scanner)
                                if idesc.class_code() == 0x03 && idesc.sub_class_code() == 0x01 {
                                    return ScannerDetectionResult {
                                        detected: true,
                                        manufacturer: None, // inconnu — dongle générique
                                        mode: "HID".to_string(), port_name: None,
                                        usb_vid: Some(desc.vendor_id()),
                                        usb_pid: Some(desc.product_id()),
                                        guidance: Some(
                                            "Scanner HID générique (dongle 2.4G ?) détecté. Mode actuel : opérationnel.".into()
                                        ),
                                    };
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Aucun scanner reconnu → HID TIMING par défaut ───────────────────────
    ScannerDetectionResult {
        detected: false, manufacturer: None, mode: "NONE".to_string(),
        port_name: None, usb_vid: None, usb_pid: None, guidance: None,
    }
}
```

**Intégration Angular — `setupBarcodeScanner()` avec les 3 couches :**

```typescript
const detection = await invoke<ScannerDetectionResult>('detect_scanner');

if (detection.mode === 'CDC' && detection.portName) {
  // Couche 1 : CDC → serialport (focus inutile)
  await invoke('start_scanner_listener', { portName: detection.portName, baudRate: 9600 });
  this.scannerMode.set('SERIAL');
} else if (detection.mode === 'HID' && detection.usbVid && detection.usbPid) {
  // Couche 2 ou 3 : HID direct → hidapi (focus inutile ✅)
  await invoke('start_hid_scanner_listener', { vid: detection.usbVid, pid: detection.usbPid });
  this.scannerMode.set('HID_DIRECT');
  // Afficher le guidance si présent (ne pas gêner le travail)
  if (detection.guidance) this.scannerGuidance.set(detection.guidance);
} else {
  // Aucun scanner reconnu → HID TIMING (focus requis ⚠️)
  this.setupHidFallback();
}
```

**Effort :** ~110 lignes Rust + ~30 lignes TypeScript.

---

### SC-02 · Commandes Rust `start_scanner_listener` / `stop_scanner_listener` 🔴 🦀

**Quoi :** Lire le port COM en boucle et émettre des événements Tauri `scan-reception`
vers Angular.

**Où :** `src-tauri/src/scanner.rs`

```rust
#[tauri::command]
async fn start_scanner_listener(app: AppHandle, port_name: String, baud_rate: u32) {
    let mut port = serialport::new(&port_name, baud_rate)
        .timeout(Duration::from_millis(100)).open()?;
    let mut buf = Vec::new();
    loop {
        let mut byte = [0u8; 1];
        if port.read(&mut byte).is_ok() {
            if byte[0] == b'\r' || byte[0] == b'\n' {
                if !buf.is_empty() {
                    let code = String::from_utf8_lossy(&buf).to_string();
                    app.emit("scan-reception", &code).ok();
                    buf.clear();
                }
            } else { buf.push(byte[0]); }
        }
    }
}
```

**Cycle de vie :**

```
ngOnInit (BL ouvert)   → invoke('start_scanner_listener')  → port ouvert
destroyRef.onDestroy() → invoke('stop_scanner_listener')   → port libéré ✅
```

Le port est **libéré entre deux BL** — pas de conflit avec d'autres logiciels (caisse, etc.).

**Effort :** ~60 lignes Rust.

---

### SC-03 · Intégration Angular — `setupBarcodeScanner()` avec fallback 🔴 🎨

**Quoi :** Détecter si Tauri est disponible et si un port est configuré. Utiliser le port
série si oui, sinon basculer silencieusement sur HID. **Le mode TIMING reste le filet de
sécurité final — il ne dépend d'aucune configuration utilisateur.**

**Où :** `commande-received.component.ts`

```typescript
private async setupBarcodeScanner(): Promise<void> {
  // ── Niveau 3 : Tauri Serial Port (USB CDC ou série physique) ─────────────
  if (this.tauriPrinterService.isRunningInTauri()) {
    const scannerPort = localStorage.getItem('reception-scanner-port');
    if (scannerPort) {
      try {
        const { invoke } = await import('@tauri-apps/api/core');
        const { listen }  = await import('@tauri-apps/api/event');
        await invoke('start_scanner_listener', { portName: scannerPort, baudRate: 9600 });
        this.scannerMode.set('SERIAL');
        const unlistenScan = await listen<string>('scan-reception', ev => {
          this.ngZone.run(() => { this.scanValue = ev.payload; this.onScanReception(); });
        });
        const unlistenErr = await listen<string>('scan-reception-error', () => {
          this.ngZone.run(() => {
            this.notificationService.warn('Scanner déconnecté — mode clavier actif', 'Scanner');
            this.scannerMode.set('HID');
            this.setupHidFallback(); // ← retour niveau 1 ou 2
          });
        });
        this.destroyRef.onDestroy(async () => {
          unlistenScan(); unlistenErr();
          try { await invoke('stop_scanner_listener'); } catch { /* déjà fermé */ }
        });
        return; // ← sortie si Serial OK
      } catch {
        // Port occupé, absent ou non configuré → fallback silencieux vers HID
      }
    }
  }
  // ── Niveau 1 (défaut) ou Niveau 2 (STX/ETX si configuré) ─────────────────
  this.setupHidFallback();
}

private setupHidFallback(): void {
  const scannerMode = this.configurationService.getParamByKey(Params.APP_SCANNER_MODE)?.value;
  if (scannerMode === 'STX_ETX') {
    // Niveau 2 — STX/ETX : uniquement si l'utilisateur a configuré le scanner physique
    // GlobalScannerService + PrefixSuffixScannerService gèrent le parsing
    this.receptionScanner.activateStxEtxMode(); // déléguer à GlobalScannerService
    this.scannerMode.set('STX_ETX');
  } else {
    // Niveau 1 (défaut) — TIMING + preventDefault
    // Déjà en place via setupBarcodeScanner() original (AX-15b)
    // Aucune configuration requise, robustesse garantie
    this.scannerMode.set('HID');
  }
  // Dans les deux cas, le listener keydown est activé (déjà présent)
}
```

**Garantie :** si l'utilisateur **n'a jamais touché** à la configuration du scanner,
le système tombe sur le **Niveau 1 (TIMING + preventDefault)** — qui fonctionne
depuis le début et protège déjà les inputs grâce à AX-15b.

**Effort :** ~55 lignes TypeScript.

---

### SC-04 · UI de configuration scanner (page Paramètres) 🔴 🎨

**Quoi :** Composant Angular guidant l'utilisateur pour configurer son scanner en mode USB CDC
et choisir le port COM.

**Fonctionnalités :**
- Détection automatique des ports : `invoke('list_serial_ports_detailed')`
- Badge `"Scanner détecté : Honeywell — COM6"` si VID USB reconnu
- **Guide contextuel intégré** : "Votre scanner est actuellement en mode HID. Pour utiliser
  le mode série, scannez l'étiquette 'USB CDC' dans le manuel de votre scanner."
- Sauvegarde du port choisi dans `localStorage('reception-scanner-port')`
- Bouton "Tester" : ouvre le port 3 s et attend un scan de validation
- Si aucun port détecté → message "Mode HID actif — fonctionne sans configuration"

> Cette UI est **essentielle** car c'est elle qui guide l'utilisateur vers la configuration
> USB CDC. Sans elle, la plupart des utilisateurs resteront en HID TIMING par défaut.

**Effort :** ~100 lignes (composant standalone).

---

### SC-05 · Configuration STX/ETX — amélioration optionnelle ⚙️ 🟢

> ⚠️ **Ce point est optionnel et ne doit pas être un prérequis.**  
> Le système fonctionne correctement en mode TIMING sans cette configuration.
> STX/ETX est une amélioration de confort pour les officines qui veulent éliminer
> totalement les faux positifs — mais son absence ne dégrade pas l'expérience nominale.

**Quoi :** Si l'utilisateur configure sa douchette pour émettre STX/ETX, il peut activer
`APP_SCANNER_MODE = STX_ETX` dans les paramètres pour bénéficier d'une détection sans
faux positifs.

**Ce qu'il faut faire SI l'utilisateur veut l'activer :**
1. Adapter `ReceptionScannerService.activateStxEtxMode()` pour déléguer à
   `GlobalScannerService` (~15 lignes)
2. L'utilisateur scanne l'étiquette STX/ETX dans le manuel de sa douchette (une fois)
3. Mettre `APP_SCANNER_MODE = STX_ETX` dans les paramètres applicatifs

**Si non fait :** aucun impact — le système reste sur TIMING + preventDefault (AX-15b).

---

## Backlog · Basse priorité 🟢

| ID | Description | Effort |
|---|---|---|
| **AX-16** | Rapport FMD conditionnel dans finalisation (si FMD activé — non applicable Afrique de l'Ouest) | 🏗️🎨 ~70 lignes |
| **AX-18** | Ajout ligne hors commande (bonus fournisseur, substitution) | 🏗️🎨 ~2 jours |
| **AX-19** | Commentaire de réception par ligne (annotation interne, distinct des retours) | 🏗️🎨 ~1 jour |
| **AX-21** | Alerte stock max dépassé après réception | 🏗️🎨 ~1 jour |
| **AX-23f** | Recherche catalogue multi-code lors d'un scan inconnu (CIP13 + EAN + code interne) | 🏗️ ~30 lignes Java |
| **AX-Cam** | Scanner caméra `@zxing/ngx-scanner` (fallback sans douchette) | 🎨 npm + ~40 lignes |
| **AX-Photo** | Photo réception pour documenter les dommages d'emballage | 🏗️🎨 ~2 jours |
| **AX-SSCC** | Scan AI 00 (SSCC palette) → réceptionner une palette entière | 🏗️🎨 ~3 jours |

---

## Tableau de bord — tous les items

| ID           | Description | Priorité | Type | Sprint |
|--------------|---|---|---|---|
| ✅ AX-05      | timer scanLotPrefill supprimé | ✅ Fait | 🎨 | — |
| ✅ AX-15a/b   | ReceptionScannerService isolé + anti-pollution | ✅ Fait | 🎨 | — |
| ✅ **AX-01**  | Bip sonore OK / KO — `ScanAudioFeedbackService` injecté dans `onScanReception()` | `commande-received.component.ts` |
| ✅ **AX-02**  | Flash ligne + scroll après scan grille | 🔴 | 🎨 | 1 |
| ✅ **AX-03**  | Écart colisage dans concordance | 🟡 | 🎨 | 1 |
| ✅ **AX-04**  | Unifier logique finalisation | 🟡 | 🎨 | 1 |
| ✅ **AX-23a** | Détecter lignes provisoires lors scan inconnu | 🔴 | 🎨 | 2 |
| ✅ **AX-23b**   | Badge orange + bouton Associer | ✅ Fait | 🎨 | 2 |
| ✅ **AX-23c** | `onAssocierScanToProvisional()` | 🔴 | 🎨 | 2 |
| ✅ **AX-23d** | Banner CIP provisoire en séquentiel | 🟡 | 🎨 | 2 |
| ✅**AX-15**   | Feedback scan en mode séquentiel | 🔴 | 🎨 | 2 |
| ✅**AX-09**   | Bandeau concordance compact en séquentiel | 🟡 | 🎨 | 2 |
| ✅**AX-13**   | État "À saisir" colonne Statut | 🟡 | 🎨 | 3 |
| ✅ **AX-08**  | Taux de service BL dans le header | 🟡 | 🎨 | 3 |
| ✅ **AX-10**    | Badge chaîne du froid séquentiel | ✅ Fait | 🎨 | 3 |
| ✅ **AX-11**    | Numéro PO fournisseur dans résumé BL | ✅ Fait | 🎨 | 3 |
| ✅ **AX-06**  | Colonne TVA grille | 🟢 | 🎨 | 3 |
| ✅ **AX-07**  | Colonnes remise + montant net grille | 🟢 | 🎨 | 3 |
| ✅ **AX-12**  | Colonne jours couverture stock | 🟢 | 🎨 | 3 |
| **AX-14**    | Parser AI 37 DataMatrix (quantité N) | 🟡 | 🏗️🎨 | 4 |
| ✅**AX-17**   | Raccourcis clavier (Ctrl+G, F5, F11, Alt+H) | 🟢 | 🎨 | 4 |
| ✅**AX-23g**  | Rapport CIP mis à jour dans finalisation | 🟢 | 🎨 | 4 |
| ✅ **AX-20**  | Récap TVA par taux dans finalisation | 🔴 | 🎨 | 6 |
| ✅ **AX-22**  | Lien rapprochement facture | 🟡 | 🎨 | 6 |
| **SC-01**    | Rust `detect_scanner` via `hidapi` + `rusb` (HID direct + CDC fallback) | 🔴 | 🦀 | 5 |
| **SC-02**    | Rust `start_hid_listener` (hidapi) + `start_cdc_listener` (serialport) | 🔴 | 🦀 | 5 |
| **SC-03**    | Angular `setupBarcodeScanner()` avec fallback | 🔴 | 🎨 | 5 |
| **SC-04**    | UI config scanner + guide USB CDC (si mode HID détecté) | 🔴 | 🎨 | 5 |
| **SC-05**    | Config STX/ETX scanner (optionnel, zéro code) | 🟢 Optionnel | ⚙️ | — |

---

## Ordre de réalisation recommandé

```
Semaine 1 — Gains immédiats (frontend pur, sans backend)
  AX-01 ✅  AX-02  AX-23a  AX-23b  AX-23c  AX-15  AX-09

Semaine 2 — Compléter le scan et l'affichage
  AX-23d  AX-04  AX-03  AX-13  AX-08  AX-17

Semaine 3 — Colonnes et indicateurs métier
  AX-10  AX-11  AX-06  AX-07  AX-12  AX-20

Semaine 4 — Scan avancé
  AX-14  AX-23g

Semaine 5 — Infrastructure scanner Tauri ← PRIORITÉ scan long terme
  SC-01 (Rust list ports)
  SC-02 (Rust start/stop listener)
  SC-03 (Angular fallback 3 niveaux)
  SC-04 (UI config scanner + guide USB CDC)
  Note : la plupart des douchettes sont USB HID par défaut.
         SC-04 guide l'utilisateur pour passer en USB CDC (1 scan d'étiquette manuel).
         Sans config → fallback automatique TIMING (toujours fonctionnel).

Semaine 6 — Fonctionnalités métier
  AX-22

Backlog (si besoin) → AX-16  AX-18  AX-19  AX-21

SC-05 (STX/ETX) → uniquement si l'utilisateur configure sa douchette (totalement optionnel)
```

---

## Références

| Document | Contenu |
|---|---|
| `docs/ANALYSE-MODULE-RECEPTION-BL.md` | Analyse complète avec code détaillé pour chaque item |
| `commande-received.component.ts` | Composant principal (1 073 lignes) |
| `sequential/reception-sequential.component.ts` | Mode séquentiel |
| `OrderLine.java` | Entité domaine (champs disponibles) |
| `src-tauri/Cargo.toml` | Dépendances Rust (serialport déjà présent) |
| `src-tauri/src/lib.rs` | Point d'entrée Rust — enregistrer les nouvelles commandes ici |

---

*Plan généré le 29 avril 2026 — Pharma-Smart*

