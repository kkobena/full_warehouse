# PLAN — Réception de bon & gestion des lots

> Fichier généré à partir de l'analyse du 2026-04-26.
> Périmètre : `StockEntryServiceImpl`, `commande-received.component`, `Lot`, `FormLotComponent`, `ListLotComponent`.

---

## 1. Bugs bloquants (priorité P0)

### 1.1 `lotPredicate` inconditionnel — finalisation impossible sans lots

**Fichier :** `StockEntryServiceImpl.java` `:282`

**Problème :** Le prédicat est vérifié pour chaque ligne sans vérifier si `APP_GESTION_LOT` est actif. Quand la gestion des lots est désactivée, `getLots()` est toujours vide → le prédicat retourne `false` → `GenericError("lotManquant")` → finalisation impossible.

```java
// ❌ Actuel
if (!lotPredicate.test(orderLine)) {
    throw new GenericError("lotManquant");
}

// ✅ Corrigé
boolean lotActif = appConfigurationService.useLot().orElse(false);
if (lotActif && !lotPredicate.test(orderLine)) {
    throw new GenericError("lotManquant");
}
```

**Impact :** Bloque 100 % des finalisations sur les pharmacies sans gestion de lots.

---

### 1.2 `buildLot()` — champs `@NotNull` non renseignés (chemin CSV)

**Fichier :** `StockEntryServiceImpl.java` `:787`

**Problème :** La méthode `buildLot()` utilisée lors de l'import CSV ne renseigne pas `currentQuantity`, `prixAchat`, `prixUnit`, `createdDate`, `statut` — tous annotés `@NotNull` en base → violation de contrainte à la persistance.

```java
// ❌ Actuel — champs obligatoires absents
new Lot()
    .setNumLot(lotNumber)
    .setExpiryDate(expirationDate)
    .setQuantity(quantity)

// ✅ Corrigé — aligner sur la logique scan (`:551`)
new Lot()
    .setOrderLine(orderLine)
    .setProduit(orderLine.getFournisseurProduit().getProduit())
    .setNumLot(lotNumber)
    .setExpiryDate(expirationDate)
    .setQuantity(quantity)
    .setCurrentQuantity(quantity)
    .setFreeQty(freeQuantity)
    .setPrixAchat(orderLine.getOrderCostAmount())
    .setPrixUnit(orderLine.getOrderUnitPrice())
    .setCreatedDate(LocalDateTime.now())
    .setStatut(StatutLot.AVAILABLE)
```

**Impact :** Crash à l'import CSV pour tout grossiste transmettant des numéros de lot (LABOREX, COPHARMED…).

---

### 1.3 `canEntreeStockIsAuthorize2` — réception partielle bloquée

**Fichier :** `StockEntryServiceImpl.java` `:128`

**Problème :** Quand `updated = true` ET `quantityReceived ≠ quantityRequested`, le prédicat retourne `false` → `GenericError("commandeManquante")`. Une réception partielle intentionnelle (rupture grossiste partielle) est donc indistinguable d'une ligne non saisie.

```java
// ❌ Actuel — partiel interdit si updated=true
private final Predicate<OrderLine> canEntreeStockIsAuthorize2 = orderLine -> {
    if (!BooleanUtils.isTrue(orderLine.getUpdated())) return true;
    return nonNull(orderLine.getQuantityReceived()) &&
        orderLine.getQuantityReceived().compareTo(orderLine.getQuantityRequested()) == 0;
};

// ✅ Corrigé — autoriser si quantité reçue explicitement saisie (≥ 0)
private final Predicate<OrderLine> canEntreeStockIsAuthorize2 = orderLine -> {
    if (!BooleanUtils.isTrue(orderLine.getUpdated())) return true;
    return nonNull(orderLine.getQuantityReceived()) && orderLine.getQuantityReceived() >= 0;
};
```

**Impact :** Bloque la finalisation de toute commande avec au moins une ligne partiellement servie saisie manuellement.

---

## 2. Lacunes fonctionnelles (priorité P1)

### 2.1 Pas de flag `suivi_lot` par produit

**Problème :** La gestion des lots est **globale** (`APP_GESTION_LOT`). Les consommables, dispositifs médicaux, parapharmacie ne nécessitent pas de traçabilité de lot — mais en activant le mode global, ils bloquent la finalisation.

**Solution :** Ajouter un booléen `gestionLot` sur l'entité `Produit` (ou `FournisseurProduit`).

**Backend :**
```java
// Produit.java
@Column(name = "gestion_lot", nullable = false, columnDefinition = "boolean default false")
private boolean gestionLot = false;

// StockEntryServiceImpl — lotPredicate conditionné par produit ET config globale
boolean lotActif = appConfigurationService.useLot().orElse(false);
boolean produitNecessiteLot = orderLine.getFournisseurProduit().getProduit().isGestionLot();
if (lotActif && produitNecessiteLot && !lotPredicate.test(orderLine)) { ... }
```

**Flyway migration :**
```sql
ALTER TABLE warehouse.produit ADD COLUMN IF NOT EXISTS gestion_lot BOOLEAN NOT NULL DEFAULT FALSE;
```

**Frontend :** Ajouter un toggle dans le formulaire produit. Dans `commande-received`, la colonne "Lots" de la grille et le badge "lots manquants" ne s'affichent que si `gestionLot = true` pour la ligne.

---

### 2.2 Pas de blocage / rappel de lot

**Problème :** L'enum `StatutLot` existe mais aucun workflow de blocage n'est exposé. Un lot reçu suspect ou rappelé par l'ANSM reste distribuable.

**Solution :**

1. Vérifier que `StatutLot` contient `BLOCKED` et `RECALLED`.
2. Bloquer la dispensation côté vente si `lot.statut ∈ {BLOCKED, RECALLED}`.
3. Exposer une action "Bloquer / Débloquer" dans la liste des lots produit.
4. Ajouter une entrée de menu dans le module stock : **Alertes lots bloqués**.

---

### 2.3 Pas d'alerte DLC courte à la dispensation

**Problème :** `isLotMinExpiryValid()` protège à la réception mais rien n'alerte le pharmacien lors de la dispensation d'un lot proche de son expiration.

**Solution :** Dans le service de vente, au moment de la réservation du lot, vérifier :
```java
long joursRestants = ChronoUnit.DAYS.between(LocalDate.now(), lot.getExpiryDate());
if (joursRestants < configService.getAlerteDlcVenteJours()) {
    // Retourner un warning (pas un blocage) dans la réponse
}
```
Afficher un badge amber dans la vente avec le nombre de jours restants.

---

### 2.4 ~~Pas de FEFO automatique à la vente~~ ✅ DÉJÀ IMPLÉMENTÉ

`LotService.findByProduitId(Integer produitId)` retourne les lots triés FEFO.
`adjustLots()` applique le FEFO (qtyDelta < 0 → débit FEFO, qtyDelta > 0 → crédit lot le plus récent).
**Aucune action requise.**

---

## 3. UX — Saisie de lot inline dans la grille de réception

### 3.1 Problème actuel

Pour une commande de 50 lignes avec gestion de lots, le pharmacien doit ouvrir **50 modales successives**, chacune avec un formulaire en plusieurs étapes. C'est la lacune UX la plus impactante dans l'usage quotidien.

### 3.2 Proposition : saisie inline expandable par ligne AG Grid

#### Concept

Chaque ligne de la grille de réception peut être **expandée** pour révéler un sous-panel de saisie de lots directement dans la grille, sans modale. La saisie est possible au clavier sans souris.

```
┌────────────────────────────────────────────────────────────────────────────────┐
│ Code    │ Désignation          │ Stock │ P.A  │ Qté.cmd │ Qté.reçue │ Statut │ ⊕ │
├────────────────────────────────────────────────────────────────────────────────┤
│ 3400935 │ DOLIPRANE 1000MG     │  24   │ 1050 │   10    │    10     │ Servi  │ ▼ │
│ ─ ─ ─ ─ LOTS ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─│
│  [Lot]  │ Num lot  │ Date péremption │ Qté  │ UG │ Statut    │         [+ Lot] │
│  ✓      │ 24A001   │ 12/2026         │  7   │  0 │ ●VALIDE   │                 │
│  ✓      │ 24A002   │ 06/2027         │  3   │  0 │ ●VALIDE   │                 │
│         │ ________ │ __/__/____      │ __   │ __ │           │                 │
│                              Saisie rapide : Tab = champ suivant, Enter = valider│
├────────────────────────────────────────────────────────────────────────────────┤
│ 3400921 │ AMOXICILLINE 500MG   │   5   │  980 │    6    │    6      │ Servi  │ ▶ │
│ 3400950 │ DAFALGAN PEDIATRIQUE │   2   │  750 │    4    │    4      │ Servi  │ ⚠ │  ← aucun lot
└────────────────────────────────────────────────────────────────────────────────┘
```

#### Icône expandeur dans la colonne "Lots"

| Icône | Signification |
|-------|---------------|
| `▶` (gris) | Lot requis, aucun lot saisi |
| `⚠` (amber) | Lot requis, saisie incomplète (qtés ne couvrent pas qté reçue) |
| `✓` (vert) | Lots complets |
| `—` | Produit sans gestion de lot |

#### Comportement

1. **Clic sur `▶` / `▼`** → expand/collapse le sous-panel inline (animation `slideDown`).
2. **Dans le sous-panel** : tableau des lots existants + une **ligne de saisie vide** en bas.
3. **Champs de la ligne de saisie :**
   - `numLot` — texte — focus automatique à l'expand
   - `expiryDate` — format `MM/AAAA` (simplification : seul le mois compte pour la DLC officine) — auto-convertion `01/MM/AAAA`
   - `qté` — nombre — max = `quantityReceived - totalLotsSaisis`
   - `UG` — nombre — facultatif, max = `freeQty`
4. **Tab** navigue entre les champs. **Enter** valide la ligne de lot et ouvre une nouvelle ligne vide si la quantité n'est pas encore complète.
5. **Validation à la frappe** : si `expiryDate < today + 3 mois` → fond amber + tooltip "DLC courte".
6. **Collapse automatique** quand les lots couvrent exactement la `quantityReceived`.
7. **Raccourci clavier global** : `F7` → aller à la prochaine ligne sans lots (navigation rapide de complétion).

#### Implémentation Angular

**Composant :** `LotInlineEditorComponent` (standalone)

```typescript
@Component({
  selector: 'app-lot-inline-editor',
  // Inséré comme fullWidthRow renderer AG Grid
})
export class LotInlineEditorComponent implements ICellRendererAngularComp {
  line = signal<IOrderLine | null>(null);
  lots  = signal<ILot[]>([]);
  draft = signal<Partial<ILot>>({});

  remainingQty = computed(() =>
    (this.line()?.quantityReceived ?? 0) -
    this.lots().reduce((s, l) => s + (l.quantityReceived ?? 0), 0)
  );

  isComplete = computed(() => this.remainingQty() <= 0);
}
```

**AG Grid — fullWidthRow pattern :**
```typescript
// Dans buildColumnDefs()
isFullWidthRow: p => p.rowNode.data?.__expanded === true,
fullWidthCellRenderer: LotInlineEditorComponent,
```

Chaque `IOrderLine` reçoit une propriété transiente `__expanded: boolean` gérée par un clic sur l'icône de la colonne "Lots".

**Service :** `LotInlineEditorService` — sauvegarde un lot à la frappe (debounce 300 ms) via `LotService.addLot()`. Pas d'état local intermédiaire.

#### Indicateur visuel de progression (caption de grille)

```
Lots : 12 / 18 lignes complètes   [████████░░░░] 67 %
```

Ce compteur est déjà calculable depuis `lignesSansLot` existant dans le composant.

---

## 4. Récapitulatif des tâches d'implémentation

| # | Tâche | Fichier(s) | Priorité | Effort |
|---|-------|-----------|----------|--------|
| T1 | Conditionner `lotPredicate` par `useLot()` | `StockEntryServiceImpl.java` | P0 | 30 min |
| T2 | Compléter `buildLot()` (champs @NotNull) | `StockEntryServiceImpl.java` | P0 | 30 min |
| T3 | Corriger `canEntreeStockIsAuthorize2` | `StockEntryServiceImpl.java` | P0 | 15 min |
| T4 | Flyway : colonne `gestion_lot` sur `produit` | migration `.sql` | P1 | 15 min |
| T5 | Backend : `lotPredicate` conditionné par `produit.gestionLot` | `StockEntryServiceImpl.java` | P1 | 1h |
| T6 | Frontend : toggle `gestion_lot` dans formulaire produit | `produit-update.component` | P1 | 2h |
| T7 | Workflow blocage lot (`BLOCKED`/`RECALLED`) | `LotService`, UI lots | P1 | 4h |
| T8 | Alerte DLC à la dispensation | service vente | P1 | 2h |
| ~~T9~~ | ~~FEFO automatique à la vente~~ | — | ~~P1~~ | ✅ déjà fait |
| T10 | **UX : `LotInlineEditorComponent`** | `commande-received`, nouveau composant | **P0** | 2j |
| T11 | Raccourci F7 (navigation prochaine ligne sans lot) | `commande-received.component.ts` | P2 | 2h |
| T12 | Message d'erreur doublon lot (catch contrainte DB) | `StockEntryServiceImpl.java` | P2 | 1h |
| T13 | Indicateur de progression lots (caption grille) | `commande-received.component.html` | P2 | 1h |

---

## 5. Ordre d'implémentation recommandé

```
Sprint 1 (corrections critiques)
  T1 → T2 → T3    (bugs P0 — 1h30 au total) ✅ FAIT (2026-04-27)

Sprint 2 (flag produit + inline editor)
  T4 → T5 → T6    (gestion_lot par produit) ✅ FAIT (2026-04-26)
  T10              (LotInlineEditorComponent — tâche principale UX) ✅ FAIT (2026-04-27)
  T13              (indicateur progression) ✅ FAIT (2026-04-27)

Sprint 3 (réglementaire)
  T7               (blocage lot)
  T8 → T9          (DLC vente + FEFO)
  T11 → T12        (UX secondaire)
```
