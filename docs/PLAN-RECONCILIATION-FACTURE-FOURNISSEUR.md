# Plan d'implémentation — Réconciliation Facture Fournisseur

> Contexte : amélioration de la fonctionnalité `ReconciliationFactureComponent` existante pour
> répondre au besoin métier complet de rapprochement BL ↔ facture fournisseur.

---

## 1. État des lieux

### Ce qui existe

| Composant | Rôle actuel |
|---|---|
| `ReconciliationFactureComponent` | Modale de comparaison HT/TVA globale, sauvegarde via `deliveryService.update()` |
| `RapprochementResource` + `RapprochementServiceImpl` | **Tiers payants uniquement** (mutuelles) — aucun lien avec les fournisseurs |
| `RetourDepuisReceptionComponent` | Crée des avoirs fournisseur ligne par ligne via `AvoirFournisseur` |

### Avertissement : homonymie des "rapprochements"

Le `RapprochementServiceImpl` backend fait la réconciliation **mutuelles/tiers payants**
(`FactureTiersPayant`). La `ReconciliationFactureComponent` frontend fait une comparaison **facture
fournisseur vs BL**. Même vocabulaire, deux processus métier orthogonaux — risque de confusion
durable dans le code.

---

## 2. Lacunes identifiées

### 2.1 Absence de domaine dédié (critique)

La réconciliation est "stockée" en écrasant des champs de la `Commande` (`receiptAmount`,
`taxAmount`, `receiptReference`) via un `PUT` générique. Il n'existe pas d'entité
`ReconciliationFactureFournisseur` avec :
- un statut (`EN_ATTENTE` / `RECONCILIEE` / `ECART` / `LITIGE`)
- une date d'enregistrement
- un lien vers le document de facturation (référence, date)
- un lien vers l'avoir éventuel généré

**Conséquence :** aucune traçabilité. Si l'utilisateur modifie deux fois, la première saisie est
perdue.

### 2.2 Pas de statut de réconciliation sur le BL

Le BL reste `CLOSED` qu'il soit réconcilié ou non. Il est impossible de filtrer "bons en attente
de réconciliation facture" ou d'afficher une alerte "facture non rapprochée depuis X jours".

### 2.3 Granularité trop grossière

La comparaison se fait uniquement au niveau des **totaux globaux** (HT + TVA du BL entier). Les
écarts surgissent en réalité :
- ligne par ligne (prix unitaire facturé ≠ prix BL)
- sur les remises commerciales (remise de fin d'année, remise de volume)
- sur la TVA par taux (médicaments remboursables 2,1 %, non remboursables 10 %)

Sans comparaison ligne par ligne, l'utilisateur ne peut pas identifier quelle ligne pose problème.

### 2.4 Mauvais placement UX

Le bouton "Rapprocher facture" est dans `commande-received` (vue d'édition d'un BL `RECEIVED`).
Le `@if` dans le HTML (`orderStatus === 'RECEIVED' || 'CLOSED'`) le rend accessible pendant la
saisie, ce qui n'a pas de sens : on réconcilie une facture sur un stock déjà entré, pas pendant la
saisie.

### 2.5 Pas de workflow sur les écarts

Quand `ecartHT !== 0`, la modale propose "Enregistrer avec écart" mais il n'y a :
- aucun ticket de litige créé
- aucune suggestion de générer un avoir automatique
- aucune relance ou rappel ultérieur

### 2.6 Pas de lien avec les avoirs fournisseur

`RetourDepuisReceptionComponent` crée des `AvoirFournisseur`. Quand la facture arrive avec un
montant inférieur au BL (le fournisseur a déjà déduit un retour), il n'y a aucun mécanisme pour
"solder" l'avoir existant via la réconciliation.

### 2.7 Pré-remplissage trompeur

```typescript
this.factureMontantHT = this.commande.receiptAmount ?? this.commande.grossAmount ?? null;
```
Si `receiptAmount` est null (première ouverture), le champ se pré-remplit avec `grossAmount`
(montant calculé des lignes). L'utilisateur peut valider sans saisir la vraie valeur et croire le
BL réconcilié.

### 2.8 Absence de document exportable

Une réconciliation métier doit pouvoir être imprimée comme PV de rapprochement (document comptable
horodaté, signé, référencé).

---

## 3. Positionnement dans la chaîne vs marché

```
Commande REQUESTED
       │  delivery-modal.component         ← Saisie réception physique (BL)
       ▼
  BL RECEIVED
       │  commande-received.component      ← Validation lignes, lots, scan, prix
       ▼
  BL CLOSED  ──────────────────────────────────────────────────────
       │                                                           │
       │  retour-depuis-reception          ← Retour avant facture  │
       │                                                           │
       ▼                                                           ▼
  Facture fournisseur reçue (J+3 à J+15)       [manquant aujourd'hui]
       │  reconciliation-workspace         ← Doit être ici (CLOSED only)
       ▼
  Rapprochement BL ↔ Facture
       ├── Concordant → archivage, paiement autorisé
       └── Écart → Litige / avoir complémentaire / demande de correction
```

| Logiciel | Étape réconciliation | Granularité | Statut dédié | EDI INVOIC |
|---|---|---|---|---|
| **Pharmagest LGPI** | Post-clôture BL, module Achats | Ligne | Oui (Rapproché/Écart/Litige) | Oui (AS2) |
| **Winpharma** | Onglet dédié post-clôture | Ligne + totaux | Oui | Oui (AS2 / SFTP) |
| **Primoris (Alliadis)** | Workflow automatisé EDI | Ligne | Oui, alerte automatique | Oui |
| **SAGE/Cegid** | Module Comptabilité Fournisseurs | Total + analytique | Oui | Import manuel |
| **Actuel** | Modale sur BL RECEIVED/CLOSED | Total seulement | Non | Non |

---

## 4. Proposition UX cible

### 4.1 Retirer le bouton de `commande-received`

Supprimer le bouton "Rapprocher facture" de `commande-received.component.html`. Le déplacer dans
`list-bons-actions.component.ts` comme action de menu sur les BL `CLOSED` uniquement.

### 4.2 Nouvelle vue `reconciliation-workspace`

Inspirée de `retour-workspace` : vue plein écran (non modale), activée depuis `list-bons` via un
signal `reconciliationWorkspaceBon`.

```
┌─────────────────────────────────────────────────────────────────────┐
│  ← Retour    BL n°BL-2024-001 — Phoenix Pharma        [RECONCILIEE] │
├─────────────────────────────────────────────────────────────────────┤
│  Réf. facture: [___________]        Date facture: [__/__/____]      │
├───────────────┬──────────────┬──────────────────┬───────────────────┤
│  Produit      │  Prix BL     │  Prix Facturé    │  Écart            │
├───────────────┼──────────────┼──────────────────┼───────────────────┤
│  DOLIPRANE    │   1 250 F    │  [  1 250 F   ]  │  ✓  0 F           │
│  AMOXICILLINE │     890 F    │  [    950 F   ]  │  ⚠ +60 F          │
├───────────────┼──────────────┼──────────────────┼───────────────────┤
│  TOTAL HT     │  45 200 F    │     45 260 F     │  ⚠ +60 F          │
│  TVA          │     950 F    │  [    950 F   ]  │  ✓  0 F           │
│  TOTAL TTC    │  46 150 F    │     46 210 F     │  ⚠ +60 F          │
└───────────────┴──────────────┴──────────────────┴───────────────────┘
│  [Valider — Réconcilié]  [Enregistrer avec écart]                   │
│  [Générer un avoir pour l'écart]  [Imprimer PV de rapprochement]    │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.3 Badge de statut dans `list-bons`

Ajouter un badge sur chaque BL `CLOSED` :

| Statut | Badge |
|---|---|
| Aucune réconciliation | gris — `Facture en attente` |
| `RECONCILIEE` | vert — `Réconciliée` |
| `ECART` | orange — `Écart — 60 F` |
| `LITIGE` | rouge — `Litige` |

---

## 5. Plan d'implémentation

### Phase 1 — Backend : domaine dédié

> **Décision d'architecture** : la FK est portée par `Commande` (pas par
> `ReconciliationFactureFournisseur`). `Commande.reconciliation` est un `@OneToOne` lazy nullable.
> Avantages : navigation directe `commande.getReconciliation()`, DTO enrichi sans requête
> supplémentaire, PK auto-généré sur la réconciliation (évite le pattern `@EmbeddedId` +
> `@MapsId` avec clé composite).

#### 5.1 Nouvelle entité `ReconciliationFactureFournisseur`

**Fichier :** `domain/ReconciliationFactureFournisseur.java`

```java
@Entity
@Table(name = "reconciliation_facture_fournisseur")
public class ReconciliationFactureFournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rff_seq")
    @SequenceGenerator(name = "rff_seq", sequenceName = "warehouse.rff_id_seq", allocationSize = 1)
    private Integer id;

    private String factureReference;
    private LocalDate factureDate;
    private Integer factureMontantHT;
    private Integer factureTVA;
    private Integer blMontantHT;        // snapshot BL au moment du rapprochement
    private Integer blTVA;
    private Integer ecartHT;            // factureMontantHT - blMontantHT
    private Integer ecartTVA;

    @Enumerated(EnumType.STRING)
    private ReconciliationStatut statut;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avoir_fournisseur_id", nullable = true)
    private AvoirFournisseur avoir;     // avoir généré pour l'écart (nullable)
}
```

**Dans `Commande.java`** — ajouter un seul champ :

```java
@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
@JoinColumn(name = "reconciliation_id", nullable = true)
private ReconciliationFactureFournisseur reconciliation;
```

Cela permet d'accéder à `commande.getReconciliation()` directement, et dans les DTOs :

```java
.setReconciliationStatut(
    c.getReconciliation() != null ? c.getReconciliation().getStatut() : null
)
```

**Enum :** `domain/enumeration/ReconciliationStatut.java`

```java
public enum ReconciliationStatut {
    EN_ATTENTE, RECONCILIEE, ECART, LITIGE
}
```

#### 5.2 Migration Flyway

**Fichier :** `resources/db/migration/V{next}__reconciliation_facture_fournisseur.sql`

```sql
-- Table réconciliation (PK auto-générée, indépendante)
CREATE SEQUENCE warehouse.rff_id_seq START 1 INCREMENT 1;

CREATE TABLE warehouse.reconciliation_facture_fournisseur (
    id                   INTEGER NOT NULL DEFAULT nextval('warehouse.rff_id_seq'),
    facture_reference    VARCHAR(100),
    facture_date         DATE,
    facture_montant_ht   INTEGER,
    facture_tva          INTEGER,
    bl_montant_ht        INTEGER,
    bl_tva               INTEGER,
    ecart_ht             INTEGER,
    ecart_tva            INTEGER,
    statut               VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE',
    created_at           TIMESTAMP NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP,
    avoir_fournisseur_id INTEGER,
    CONSTRAINT pk_recon_facture PRIMARY KEY (id)
);

-- FK portée par commande
ALTER TABLE warehouse.commande
    ADD COLUMN reconciliation_id INTEGER,
    ADD CONSTRAINT fk_commande_reconciliation
        FOREIGN KEY (reconciliation_id)
        REFERENCES warehouse.reconciliation_facture_fournisseur (id);

-- Migrer les réconciliations déjà saisies (receiptAmount renseigné)
WITH inserted AS (
    INSERT INTO warehouse.reconciliation_facture_fournisseur
        (facture_reference, facture_date, facture_montant_ht, facture_tva,
         bl_montant_ht, bl_tva, ecart_ht, ecart_tva, statut, created_at)
    SELECT
        receipt_reference,
        receipt_date,
        receipt_amount,
        tax_amount,
        gross_amount,
        tax_amount,
        COALESCE(receipt_amount, 0) - COALESCE(gross_amount, 0),
        0,
        CASE WHEN receipt_amount = gross_amount THEN 'RECONCILIEE' ELSE 'ECART' END,
        NOW()
    FROM warehouse.commande
    WHERE receipt_amount IS NOT NULL
      AND order_status = 'CLOSED'
    RETURNING id, facture_reference
)
UPDATE warehouse.commande c
SET reconciliation_id = i.id
FROM inserted i
WHERE c.receipt_reference = i.facture_reference
  AND c.receipt_amount IS NOT NULL
  AND c.order_status = 'CLOSED';
```

#### 5.3 Repository et Service

**Fichier :** `repository/ReconciliationFactureFournisseurRepository.java`

```java
@Repository
public interface ReconciliationFactureFournisseurRepository
    extends JpaRepository<ReconciliationFactureFournisseur, Integer> {

    @Query("""
        SELECT r FROM ReconciliationFactureFournisseur r
        WHERE r.statut <> 'RECONCILIEE'
        ORDER BY r.createdAt DESC
        """)
    List<ReconciliationFactureFournisseur> findAllNonReconciliees();
}
```

**Fichier :** `service/stock/ReconciliationFournisseurService.java` (interface + impl)

Méthodes :
- `save(CommandeId, ReconciliationFactureCommand) → ReconciliationFactureFournisseur`
- `find(CommandeId) → Optional<ReconciliationFactureFournisseur>`
- `exportPv(CommandeId) → byte[]` (PDF via Thymeleaf/Flying Saucer)

#### 5.4 Nouveaux endpoints REST

**Fichier :** `web/rest/commande/ReconciliationFournisseurResource.java`

```
POST   /api/bons/{id}/{date}/reconciliation      ← créer ou mettre à jour
GET    /api/bons/{id}/{date}/reconciliation      ← charger la réconciliation existante
GET    /api/bons/{id}/{date}/reconciliation/pv   ← PDF du PV de rapprochement
GET    /api/bons/reconciliation/en-attente       ← liste BL CLOSED sans réconciliation
```

#### 5.5 Enrichir `DeliveryReceiptDTO`

Ajouter le champ `reconciliationStatut` (nullable) pour que `list-bons` puisse afficher le badge
sans appel supplémentaire.

---

### Phase 2 — Frontend

#### 5.6 Nouveau service `ReconciliationFournisseurService`

**Fichier :** `entities/commande/reconciliation/reconciliation-fournisseur.service.ts`

```typescript
@Injectable({ providedIn: 'root' })
export class ReconciliationFournisseurService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/bons';

  find(id: number, date: string): Observable<IReconciliationFactureFournisseur | null> { ... }
  save(id: number, date: string, cmd: IReconciliationCommand): Observable<IReconciliationFactureFournisseur> { ... }
  exportPv(id: number, date: string): Observable<Blob> { ... }
}
```

#### 5.7 Ajouter l'action dans `list-bons-actions.component.ts`

```typescript
export type BonAction =
  | 'voirDetail' | 'receive' | 'cancel'
  | 'exportPdf' | 'printEtiquette'
  | 'retourComplet' | 'retourParLigne'
  | 'reconcilierFacture';                    // ← nouveau
```

Dans `menuItems()` : ajouter l'entrée uniquement si `!received`.

```typescript
items.push({
  label: 'Rapprocher la facture',
  icon: 'pi pi-file-check',
  command: () => this.menuAction.emit('reconcilierFacture')
});
```

#### 5.8 Dispatcher dans `list-bons.component.ts`

```typescript
// signal
readonly reconciliationWorkspaceBon = signal<IDelivery | null>(null);

// dans onBonMenuAction
case 'reconcilierFacture':
  this.reconciliationWorkspaceBon.set(delivery);
  break;
```

#### 5.9 Nouveau composant `reconciliation-workspace`

**Fichier :** `features/commande/ui/reconciliation-workspace/reconciliation-workspace.component.ts`

- Input : `delivery = input.required<IDelivery>()`
- Outputs : `done = output<void>()`, `cancelled = output<void>()`
- Au `ngOnInit` : charge les lignes du BL + `reconciliationFournisseurService.find()`
- Tableau ligne par ligne avec colonne "Prix facturé" éditable
- Totaux calculés en temps réel
- Bouton "Générer un avoir" si `ecartHT < 0` → ouvre `RetourDepuisReceptionComponent` pré-rempli
- Bouton "Imprimer PV" → `exportPv()`

#### 5.10 Intégrer dans `list-bons.component.html`

```html
@if (reconciliationWorkspaceBon()) {
  <app-reconciliation-workspace
    [delivery]="reconciliationWorkspaceBon()!"
    (done)="onReconciliationDone()"
    (cancelled)="onReconciliationCancelled()"
  />
} @else {
  <!-- liste normale -->
}
```

#### 5.11 Badge dans `list-bons.component.html`

```html
@if (d.orderStatus !== 'RECEIVED') {
  @switch (d.reconciliationStatut) {
    @case ('RECONCILIEE') {
      <span class="badge bg-success ms-1">Réconciliée</span>
    }
    @case ('ECART') {
      <span class="badge bg-warning text-dark ms-1">Écart</span>
    }
    @case ('LITIGE') {
      <span class="badge bg-danger ms-1">Litige</span>
    }
    @default {
      <span class="badge bg-secondary ms-1">Facture en attente</span>
    }
  }
}
```

#### 5.12 Supprimer le bouton de `commande-received`

Dans `commande-received.component.html` : retirer le bloc `@if (RECEIVED || CLOSED)` contenant
le bouton "Rapprocher facture" et l'import de `ReconciliationFactureComponent`.

---

### Phase 3 — Import EDI (optionnel, post-MVP)

#### 5.13 Import fichier INVOIC

Ajouter dans le workspace un bouton "Importer facture EDI" qui :
1. Ouvre un input `<file>` acceptant `.edi`, `.txt`, `.xml`
2. Envoie le fichier à `POST /api/bons/{id}/{date}/reconciliation/import-invoic`
3. Le backend parse le message EDIFACT `INVOIC` (segment `LIN` → lignes, `MOA` → montants)
4. Retourne un `ReconciliationCommand` pré-rempli que le frontend affiche dans le workspace

**Grossistes compatibles :** Alliance Healthcare (AS2 / portail), OCP (SFTP), CERP, Phoenix Pharma.

---

## 6. Ordre d'exécution recommandé

```
[ ] Phase 1.1  Créer enum ReconciliationStatut
[ ] Phase 1.2  Créer entité ReconciliationFactureFournisseur
[ ] Phase 1.3  Écrire migration Flyway
[ ] Phase 1.4  Créer Repository
[ ] Phase 1.5  Créer Service (save + find + exportPv)
[ ] Phase 1.6  Créer Resource REST
[ ] Phase 1.7  Enrichir DeliveryReceiptDTO (reconciliationStatut)
[ ] Phase 2.1  Créer ReconciliationFournisseurService (Angular)
[ ] Phase 2.2  Créer interfaces IReconciliationFactureFournisseur, IReconciliationCommand
[ ] Phase 2.3  Créer reconciliation-workspace.component
[ ] Phase 2.4  Ajouter action 'reconcilierFacture' dans list-bons-actions
[ ] Phase 2.5  Câbler signal + dispatcher dans list-bons
[ ] Phase 2.6  Ajouter badge statut dans list-bons.component.html
[ ] Phase 2.7  Supprimer bouton de commande-received
[ ] Phase 3    Import INVOIC (optionnel)
```

---

## 7. Fichiers impactés

### Backend

| Fichier | Action |
|---|---|
| `domain/ReconciliationFactureFournisseur.java` | Créer |
| `domain/enumeration/ReconciliationStatut.java` | Créer |
| `repository/ReconciliationFactureFournisseurRepository.java` | Créer |
| `service/stock/ReconciliationFournisseurService.java` | Créer (interface + impl) |
| `web/rest/commande/ReconciliationFournisseurResource.java` | Créer |
| `service/dto/DeliveryReceiptDTO.java` | Modifier — ajouter `reconciliationStatut` |
| `service/stock/impl/StockEntryDataServiceImpl.java` | Modifier — alimenter `reconciliationStatut` |
| `resources/db/migration/V{next}__reconciliation_facture.sql` | Créer |

### Frontend

| Fichier | Action |
|---|---|
| `entities/commande/reconciliation/reconciliation-fournisseur.service.ts` | Créer |
| `shared/model/reconciliation-facture-fournisseur.model.ts` | Créer |
| `features/commande/ui/reconciliation-workspace/` (4 fichiers) | Créer |
| `features/commande/ui/list-bons/list-bons-actions.component.ts` | Modifier — action + type |
| `features/commande/ui/list-bons/list-bons.component.ts` | Modifier — signal + dispatcher |
| `features/commande/ui/list-bons/list-bons.component.html` | Modifier — workspace + badge |
| `features/commande/feature/commande-received/commande-received.component.html` | Modifier — supprimer bouton |
| `features/commande/feature/commande-received/commande-received.component.ts` | Modifier — supprimer `onReconciliationFacture` |
| `features/commande/ui/reconciliation-facture/` | Supprimer (remplacé par workspace) |
