# Plan d'implémentation — Bon d'Entrée Diverse (BED)

> **Pharma-Smart** — Angular 20 / Spring Boot 4
> **Date :** 2026-04-05
> **Auteur :** GitHub Copilot
> **Scope :**
> - Backend : `domain/`, `service/stock/`, `web/rest/commande/`
> - Frontend : `features/commande/feature/bon-entree-diverse/`
>
> **Différence fondamentale avec la Saisie de Lot Hors Commande :**
> | Fonctionnalité | Stock modifié ? | Document généré ? | OrderLine requise ? |
> |---|:---:|:---:|:---:|
> | **Saisie lot hors commande** (`ANALYSE-SAISIE-LOT-HORS-COMMANDE.md`) | ❌ Non | ❌ Non | ❌ Non |
> | **Bon d'Entrée Diverse (BED)** ← **CE DOCUMENT** | ✅ Oui | ✅ Oui (BED numéroté) | ❌ Non (hors commande) |
>
> **Patterns internes à respecter :**
> | Pattern | Référence interne |
> |---|---|
> | Layout liste | `features/products/feature/produit-home` |
> | Modal NgbModal | `features/products/ui/lot-saisie-produit-modal/` |
> | Confirmation | `NgbConfirmDialogService` |
> | Notifications | `NotificationService` |
> | Dates | `pharma-date-picker` + `DATE_FORMAT_ISO_DATE` |
> | Pagination | `ITEMS_PER_PAGE` + `TableLazyLoadEvent` |
> | Cycle de vie | `takeUntilDestroyed(destroyRef)` |

---

## Sommaire

1. [Contexte et cas d'usage](#1-contexte-et-cas-dusage)
2. [Analyse comparative — logiciels de référence](#2-analyse-comparative--logiciels-de-référence)
3. [Ce qui existe déjà dans Pharma-Smart](#3-ce-qui-existe-déjà-dans-pharma-smart)
4. [Modèle de données cible](#4-modèle-de-données-cible)
5. [Flux fonctionnel](#5-flux-fonctionnel)
6. [Plan backend — Phases 1 et 2](#6-plan-backend--phases-1-et-2)
7. [Plan frontend — Phase 3](#7-plan-frontend--phase-3)
8. [Fichiers à créer / modifier](#8-fichiers-à-créer--modifier)
9. [Matrice de priorité](#9-matrice-de-priorité)
10. [Estimation des efforts](#10-estimation-des-efforts)
11. [Phase 5 — Importation CSV avec création de BED (BASCULEMENT / BASCULEMENT_PRESTIGE)](#11-phase-5--importation-csv-avec-création-de-bed)

---

## 1. Contexte et cas d'usage

### 1.1 Définition

Un **Bon d'Entrée Diverse (BED)** est un document de stock qui permet de **créditer le stock d'un ou plusieurs produits** sans qu'une commande fournisseur (`Commande.type = ORDER`) ne soit à l'origine de cette entrée.

```
BED = Mouvement d'entrée de stock + Document numéroté + sans bon de commande
```

### 1.2 Cas d'usage identifiés

| # | Cas d'usage | Fréquence | Fournisseur ? | Lots ? |
|---|---|:---:|:---:|:---:|
| **UC1** | Retour client accepté (produit réintégré au stock) | Fréquent | Non | Oui |
| **UC2** | Echantillons reçus d'un laboratoire | Occasionnel | Oui (labo) | Oui |
| **UC3** | Transfert entrant inter-pharmacie (dépôt reçu d'une succursale) | Rare | Non | Oui |
| **UC4** | Régularisation positive après inventaire (différence constatée > 0) | Ponctuel | Non | Optionnel |
| **UC5** | Produit trouvé en arrière-pharmacie sans traçabilité | Rare | Non | Oui |
| **UC6** | Don / mise à disposition gratuite (programme de santé publique) | Rare | Oui (donateur) | Oui |
| **UC7** | Correction d'erreur de sortie (retrait de vente non annulé) | Rare | Non | Optionnel |

> **Note :** UC4 (régularisation inventaire) est parfois géré par un module dédié `Ajustement`. Selon la politique officinale, le BED peut remplacer ou compléter ce module.

### 1.3 Ce que le BED n'est PAS

- ❌ Un retour fournisseur (sortie de stock → `RETOUR_FOURNISSEUR`)
- ❌ Une commande fournisseur standard (`Commande.type = ORDER`)
- ❌ Un ajustement d'inventaire (flux distinct — `INVENTAIRE`)
- ❌ Une saisie de lot hors commande (pas de mouvement stock → `ANALYSE-SAISIE-LOT-HORS-COMMANDE.md`)

---

## 2. Analyse comparative — logiciels de référence

### 2.1 Winpharma

**Menu :** `Gestion stock → Mouvements → Bon d'entrée diverse`

**Comportement :**
1. Création d'un BED avec référence auto `BED-YYYYMM-NNN`
2. Ajout de lignes produit : CIP/EAN, libellé (auto-complété), quantité, prix achat, TVA
3. Motif obligatoire : liste déroulante (Don / Retour / Régularisation / Transfert / Autre)
4. Fournisseur optionnel (si don ou transfert inter-pharmacie, renseigné)
5. Lots facultatifs par ligne (si produit `checkExpiryDate`)
6. Validation → génère le BED PDF + crédite `StockProduit.qtyStock`
7. BED apparaît dans le journal des mouvements

**Points clés :**
- Numérotation automatique
- Motif obligatoire (traçabilité)
- Fournisseur optionnel
- PDF généré
- Intégré au journal des mouvements

---

### 2.2 Pharmagest iConcept

**Menu :** `Gestion du stock → Bons d'entrée → Bon d'entrée divers`

**Comportement :**
1. Formulaire en deux temps : **en-tête BED** (date, motif, commentaire, fournisseur optionnel)
2. Puis **lignes** : autocomplete produit, quantité, prix unitaire (modifiable), TVA
3. Option "Créer lot automatiquement" par ligne si le produit a `checkExpiryDate = true`
4. Validation → met à jour le stock rayon en priorité (puis réserve si rayon déjà au max)
5. Impression BED (format A4 ou thermique)
6. BED lié à un **exercice comptable** (mois/an)

**Points clés :**
- Deux étapes : en-tête puis lignes
- Répartition intelligente rayon/réserve
- Création lot automatique
- Lien exercice comptable

---

### 2.3 LGPI Observia

**Menu :** `Stock → Entrées diverses`

**Comportement :**
- Un seul formulaire (pas de séparation en-tête/lignes)
- Motif : liste fermée (`RETOUR_CLIENT`, `DON`, `REGULARISATION`, `TRANSFERT`, `AUTRE`)
- Quantité en unité de vente (pas en boîtes si produit déconditionnable)
- Pas de fournisseur (contrainte LGPI)
- Génère uniquement un mouvement de stock (pas de PDF dédié — apparaît dans le journal)

**Points clés :**
- Formulaire simplifié
- Motifs standardisés
- Pas de PDF

---

### 2.4 Caducée (Belgique)

**Menu :** `Stock → Réceptions → Réception libre`

**Comportement :**
- Appelé "Réception libre" (concept équivalent au BED)
- Référence fournisseur obligatoire (même fictif "INTERNE")
- Lots : création obligatoire si `checkExpiryDate = true`
- PDF généré et archivé automatiquement
- Badge dans le tableau de bord "X BED cette semaine"

**Points clés :**
- Fournisseur obligatoire (ou "INTERNE")
- Lots obligatoires selon paramétrage produit
- Dashboard badge

---

### 2.5 Tableau comparatif synthétique

| Critère | Winpharma | Pharmagest | LGPI | Caducée | **Pharma-Smart (cible)** |
|---|:---:|:---:|:---:|:---:|:---:|
| **BED numéroté** | ✅ | ✅ | ❌ | ✅ | ✅ |
| **Motif obligatoire** | ✅ | ✅ | ✅ | ❌ | ✅ |
| **Fournisseur optionnel** | ✅ | ✅ | ❌ | ❌ | ✅ |
| **Lots par ligne** | ✅ | ✅ | ❌ | ✅ | ✅ |
| **PDF généré** | ✅ | ✅ | ❌ | ✅ | ✅ |
| **Journal des mouvements** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Brouillon (état PENDING)** | ✅ | ✅ | ❌ | ❌ | ✅ |
| **Annulation possible** | ✅ | ❌ | ❌ | ❌ | ✅ |

---

## 3. Ce qui existe déjà dans Pharma-Smart

### 3.1 Domaine — `TypeDeliveryReceipt.DIRECT` ✅

```java
// domain/enumeration/TypeDeliveryReceipt.java
public enum TypeDeliveryReceipt {
    DIRECT,  // ← déjà défini ! C'est la base du BED
    ORDER,
}
```

La table `commande` porte déjà le champ `receipt_type VARCHAR(10)` qui accepte `DIRECT`.
**Le BED réutilise l'entité `Commande` avec `type = DIRECT`** — pas besoin d'une nouvelle table.

### 3.2 Mouvements de stock — `MouvementProduit.ENTREE_STOCK` ✅

```java
// domain/enumeration/MouvementProduit.java
ENTREE_STOCK("Entrée en stock"),   // ← type à utiliser pour les BED
AJUSTEMENT_IN("Ajustement positif"),
```

### 3.3 Service d'entrée stock — `StockEntryService` ✅ (partiellement)

```java
// service/stock/StockEntryService.java
StockEntryResultDTO finalizeSaisieEntreeStock(DeliveryReceiptLiteDTO deliveryReceiptLite);
DeliveryReceiptLiteDTO createBon(DeliveryReceiptLiteDTO deliveryReceiptLite);
```

`createBon()` crée déjà un `Commande` — il suffira de le paramétrer avec `type = DIRECT`.
`finalizeSaisieEntreeStock()` finalise la réception — à adapter pour les BED (sans contrainte `OrderLine`).

### 3.4 Ce qui manque ❌

| # | Manque | Criticité |
|---|---|:---:|
| M1 | Motif BED (`MotifBed` enum ou table référentiel) | 🔴 Haute |
| M2 | Validation spécifique BED dans `finalizeSaisieEntreeStock` (pas de `lotPredicate` basé sur `checkExpiryDate`) | 🔴 Haute |
| M3 | Endpoint `GET /api/commandes/bed` (liste des BED) | 🔴 Haute |
| M4 | Génération PDF BED (template Thymeleaf) | 🟡 Moyenne |
| M5 | Fournisseur non obligatoire dans le formulaire | 🟡 Moyenne |
| M6 | Frontend : module `features/commande/feature/bon-entree-diverse/` | 🔴 Haute |
| M7 | Numérotation automatique BED (`BED-YYYYMMDD-NNNN`) | 🟡 Moyenne |

---

## 4. Modèle de données cible

### 4.1 Réutilisation de `Commande` avec `type = DIRECT`

```
Commande (type = DIRECT)                  ← en-tête BED
├── id, orderDate                         ← clé composite (inchangé)
├── receiptReference = "BED-20260405-001" ← numérotation auto
├── type = TypeDeliveryReceipt.DIRECT     ← distingue BED des commandes normales
├── orderStatus = REQUESTED → CLOSED      ← cycle de vie
├── fournisseur (nullable via FK)         ← optionnel pour un BED
├── motifBed: MotifBed                    ← NOUVEAU champ
└── commentaire: String                   ← NOUVEAU champ (facultatif)

OrderLine (lignes BED)                    ← lignes du BED
├── commande → Commande(type=DIRECT)
├── fournisseurProduit → FournisseurProduit
├── quantityReceived                      ← quantité entrée
├── orderUnitPrice                        ← prix achat saisi
└── lots → List<Lot>                      ← facultatif
```

### 4.2 Nouveau champ `motifBed` sur `Commande`

```java
public enum MotifBed {
    RETOUR_CLIENT("Retour client"),
    ECHANTILLON("Echantillon / Don laboratoire"),
    TRANSFERT_ENTRANT("Transfert entrant inter-pharmacie"),
    REGULARISATION("Régularisation positive"),
    CORRECTION_ERREUR("Correction d'erreur"),
    BASCULEMENT("Basculement depuis autre logiciel"),
    BASCULEMENT_PRESTIGE("Basculement depuis Prestige"),
    AUTRE("Autre");
}
```

> **Note :** `BASCULEMENT` et `BASCULEMENT_PRESTIGE` correspondent aux types déjà définis dans `TypeImportationProduit`. Lorsque l'un de ces motifs est sélectionné, l'import CSV existant (`ImportationProduitService`) est exécuté **en parallèle** de la création d'un BED — voir §11 (Phase 5).

Ajouté dans la table `commande` via migration Flyway :
```sql
-- V1.0.X__bed_motif.sql
ALTER TABLE commande
    ADD COLUMN motif_bed VARCHAR(25),
    ADD COLUMN commentaire_bed VARCHAR(255);
```

### 4.3 Fournisseur nullable pour les BED

Dans `Commande`, la contrainte `@NotNull` sur `fournisseur` doit être assouplie pour `type = DIRECT` :
```java
@ManyToOne(optional = true)  // ← changer de false à true
private Fournisseur fournisseur;
```
> ⚠️ Migration nécessaire : `ALTER TABLE commande ALTER COLUMN fournisseur_id DROP NOT NULL;`

---

## 5. Flux fonctionnel

```
BROUILLON BED (orderStatus = REQUESTED)
         │
         ├── Saisie en-tête
         │   - Date (auto = aujourd'hui)
         │   - Motif (obligatoire)
         │   - Fournisseur (optionnel)
         │   - Commentaire (optionnel)
         │
         ├── Saisie lignes
         │   - Recherche produit (autocomplete CIP/libellé)
         │   - Quantité (1 ≤ qty)
         │   - Prix achat (préchargé depuis fournisseur principal, modifiable)
         │   - Lots (optionnels — pharma-date-picker)
         │
         ├── [Enregistrer brouillon]
         │   → orderStatus reste REQUESTED
         │   → accessible dans la liste BED en cours
         │
         └── [Valider le BED]
                   │
                   ▼
         finalizeSaisieEntreeStock()  (backend)
           → StockProduit.qtyStock += quantityReceived   ✅
           → MouvementProduit.ENTREE_STOCK créé          ✅
           → Lot créé si saisi                           ✅
           → orderStatus = CLOSED                        ✅
           → PDF BED généré                              ✅
```

---

## 6. Plan backend — Phases 1 et 2

### Phase 1 — Migration base de données et modèle (~0.5j)

#### 6.1.1 Migration Flyway

**Fichier :** `src/main/resources/db/migration/V1.0.X__bed_motif_commande.sql`

```sql
-- Champs BED sur la table commande
ALTER TABLE commande
    ADD COLUMN IF NOT EXISTS motif_bed    VARCHAR(25),
    ADD COLUMN IF NOT EXISTS commentaire_bed VARCHAR(255);

-- Rendre fournisseur_id nullable pour les BED (type = DIRECT)
ALTER TABLEcommande
    ALTER COLUMN fournisseur_id DROP NOT NULL;
```

#### 6.1.2 Enum `MotifBed`

**Fichier :** `domain/enumeration/MotifBed.java` *(nouveau)*

```java
package com.kobe.warehouse.domain.enumeration;

public enum MotifBed {
    RETOUR_CLIENT("Retour client"),
    ECHANTILLON("Echantillon / Don laboratoire"),
    TRANSFERT_ENTRANT("Transfert entrant inter-pharmacie"),
    REGULARISATION("Régularisation positive"),
    CORRECTION_ERREUR("Correction d'erreur"),
    AUTRE("Autre");

    private final String label;
    MotifBed(String label) { this.label = label; }
    public String getLabel() { return label; }
}
```

#### 6.1.3 Mise à jour `Commande.java`

```java
// Rendre fournisseur optionnel
@ManyToOne(optional = true)
private Fournisseur fournisseur;

// Ajouter les champs BED
@Enumerated(EnumType.STRING)
@Column(name = "motif_bed", length = 25)
private MotifBed motifBed;

@Column(name = "commentaire_bed", length = 255)
private String commentaireBed;
```

---

### Phase 2 — Service et endpoints (~1.5j)

#### 6.2.1 `BedService` interface *(nouveau)*

**Fichier :** `service/stock/BedService.java`

```java
public interface BedService {
    /**
     * Crée un BED en brouillon (orderStatus = REQUESTED, type = DIRECT).
     * Le fournisseur est optionnel.
     */
    BedDTO createBed(BedDTO bedDTO);

    /**
     * Met à jour un BED en brouillon (ajout/suppression de lignes, modification en-tête).
     */
    BedDTO updateBed(BedDTO bedDTO);

    /**
     * Valide le BED :
     * - Crédite StockProduit.qtyStock pour chaque ligne
     * - Crée les MouvementProduit.ENTREE_STOCK
     * - Crée les Lot si saisis
     * - Passe orderStatus = CLOSED
     * - Génère le PDF BED
     */
    BedResultDTO validateBed(Integer bedId, LocalDate orderDate);

    /**
     * Annule un BED validé (si annulation autorisée).
     * Génère des MouvementProduit.AJUSTEMENT_OUT en compensation.
     */
    void cancelBed(Integer bedId, LocalDate orderDate);

    /**
     * Récupère la liste paginée des BED avec filtres.
     */
    Page<BedSummaryDTO> findBeds(BedFilterParam filter, Pageable pageable);

    /**
     * Récupère le détail d'un BED.
     */
    BedDTO findBedById(Integer bedId, LocalDate orderDate);
}
```

#### 6.2.2 `BedDTO` *(nouveau)*

**Fichier :** `service/dto/BedDTO.java`

```java
public class BedDTO {
    private Integer id;
    private LocalDate orderDate;
    private String receiptReference;      // BED-YYYYMMDD-NNN (auto-généré)
    private MotifBed motifBed;            // obligatoire
    private String commentaireBed;        // optionnel
    private Integer fournisseurId;        // optionnel
    private String fournisseurLibelle;    // lecture seule
    private OrderStatut orderStatus;
    private List<BedLigneDTO> lignes;
    private Integer montantTotal;         // calculé
    private LocalDateTime createdAt;
}

public class BedLigneDTO {
    private Integer id;
    private Integer produitId;
    private String produitLibelle;
    private String codeCip;
    private Integer quantite;             // obligatoire, > 0
    private Integer prixAchat;            // pré-rempli, modifiable
    private List<LotDTO> lots;            // optionnel
}
```

#### 6.2.3 `BedResource` endpoints *(nouveau)*

**Fichier :** `web/rest/commande/BedResource.java`

```java
@RestController
@RequestMapping("/api")
public class BedResource {

    // Créer un brouillon BED
    @PostMapping("/beds")
    ResponseEntity<BedDTO> create(@Valid @RequestBody BedDTO dto);

    // Mettre à jour un brouillon
    @PutMapping("/beds")
    ResponseEntity<BedDTO> update(@Valid @RequestBody BedDTO dto);

    // Valider (crédite le stock)
    @PostMapping("/beds/{id}/validate")
    ResponseEntity<BedResultDTO> validate(@PathVariable Integer id,
                                           @RequestParam LocalDate orderDate);

    // Annuler un BED validé
    @PostMapping("/beds/{id}/cancel")
    ResponseEntity<Void> cancel(@PathVariable Integer id,
                                 @RequestParam LocalDate orderDate);

    // Liste paginée
    @GetMapping("/beds")
    ResponseEntity<List<BedSummaryDTO>> findAll(BedFilterParam filter, Pageable pageable);

    // Détail
    @GetMapping("/beds/{id}")
    ResponseEntity<BedDTO> findOne(@PathVariable Integer id,
                                    @RequestParam LocalDate orderDate);

    // PDF
    @GetMapping("/beds/{id}/pdf")
    ResponseEntity<byte[]> generatePdf(@PathVariable Integer id,
                                        @RequestParam LocalDate orderDate);
}
```

#### 6.2.4 Numérotation automatique BED

Dans `BedServiceImpl`, la référence BED est générée comme suit :

```java
private String generateBedReference(LocalDate date) {
    String prefix = "BED-" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
    int next = commandeRepository.countByTypeAndOrderDateAndReceiptReferenceStartingWith(
        TypeDeliveryReceipt.DIRECT, date, prefix) + 1;
    return prefix + String.format("%03d", next);
}
```

---

## 7. Plan frontend — Phase 3

### 7.1 Structure des composants

```
features/commande/feature/bon-entree-diverIse/
├── bed-home/
│   ├── bed-home.component.ts         ← layout liste + panneau détail (pattern produit-home)
│   └── bed-home.component.html
├── bed-list/
│   ├── bed-list.component.ts         ← tableau lazy  p-table
│   └── bed-list.component.html
├── bed-form/
│   ├── bed-form.component.ts         ← formulaire création/édition BED (modal NgbModal)
│   └── bed-form.component.html
├── bed-ligne-form/
│   ├── bed-ligne-form.component.ts   ← ligne produit inline AG-grid
│   └── bed-ligne-form.component.html
└── data-access/
    ├── bed.service.ts                ← HTTP client
    └── bed.model.ts                  ← interfaces IBed, IBedLigne, MotifBed
```

### 7.2 Interface `IBed`

```typescript
// features/commande/feature/bon-entree-diverse/data-access/bed.model.ts

export type MotifBed =
  | 'RETOUR_CLIENT'
  | 'ECHANTILLON'
  | 'TRANSFERT_ENTRANT'
  | 'REGULARISATION'
  | 'CORRECTION_ERREUR'
  | 'AUTRE';

export const MOTIFS_BED: { value: MotifBed; label: string }[] = [
  { value: 'RETOUR_CLIENT',     label: 'Retour client' },
  { value: 'ECHANTILLON',       label: 'Echantillon / Don laboratoire' },
  { value: 'TRANSFERT_ENTRANT', label: 'Transfert entrant inter-pharmacie' },
  { value: 'REGULARISATION',    label: 'Régularisation positive' },
  { value: 'CORRECTION_ERREUR', label: 'Correction d\'erreur' },
  { value: 'AUTRE',             label: 'Autre' },
];

export interface IBed {
  id?: number;
  orderDate?: string;
  receiptReference?: string;       // BED-YYYYMMDD-NNN
  motifBed?: MotifBed;
  commentaireBed?: string;
  fournisseurId?: number;
  fournisseurLibelle?: string;
  orderStatus?: 'REQUESTED' | 'CLOSED' | 'CANCELLED';
  lignes?: IBedLigne[];
  montantTotal?: number;
  createdAt?: string;
}

export interface IBedLigne {
  id?: number;
  produitId?: number;
  produitLibelle?: string;
  codeCip?: string;
  quantite?: number;
  prixAchat?: number;
  lots?: ILotBed[];
}

export interface ILotBed {
  numLot?: string;
  expiryDate?: string;
  manufacturingDate?: string;
  quantity?: number;
}
```

### 7.3 Maquette `bed-home` — Liste BED

```
┌───────────────────────────────────────────────────────────────────────┐
│ Bons d'Entrée Diverses                           [+ Nouveau BED]      │
│───────────────────────────────────────────────────────────────────────│
│ [🔍 Référence…] [Motif ▼] [📅 Du…] [📅 Au…] [Statut ▼]    [🔍]      │
│───────────────────────────────────────────────────────────────────────│
│ Référence       │ Date       │ Motif           │ Lignes │ Montant │ ⚡ │
│─────────────────┼────────────┼─────────────────┼────────┼─────────┼───│
│ BED-20260405-001│ 05/04/2026 │ Retour client   │ 3      │ 45 200  │ ⋮ │
│ BED-20260403-002│ 03/04/2026 │ Echantillon     │ 1      │  8 500  │ ⋮ │
│ BED-20260401-001│ 01/04/2026 │ Régularisation  │ 5      │ 12 000  │ ⋮ │
└───────────────────────────────────────────────────────────────────────┘

Menu ⋮ par ligne :
  ├── Voir le détail
  ├── Modifier (si REQUESTED)
  ├── Valider (si REQUESTED)
  ├── Imprimer PDF
  └── Annuler (si CLOSED, sous condition)
```

### 7.4 Maquette `bed-form` — Formulaire BED (NgbModal)

```
┌─────────────────────────────────────────────────────────────────────┐
│ Nouveau Bon d'Entrée Diverse                                        │
│─────────────────────────────────────────────────────────────────────│
│ ┌─────────────────────────┐  ┌───────────────────────────────────┐ │
│ │ Motif *                 │  │ Fournisseur (optionnel)           │ │
│ │ [Retour client       ▼] │  │ [Rechercher un fournisseur…     ] │ │
│ └─────────────────────────┘  └───────────────────────────────────┘ │
│ ┌──────────────────────────────────────────────────────────────────┐│
│ │ Commentaire (optionnel)                                          ││
│ │ [______________________________________________________________] ││
│ └──────────────────────────────────────────────────────────────────┘│
│─────────────────────────────────────────────────────────────────────│
│ LIGNES                                          [+ Ajouter produit] │
│ ┌──────────────────┬───────┬──────┬──────────┬──────────────────┐  │
│ │ Produit          │ CIP   │ Qté  │ Prix ach.│ Lots             │  │
│ ├──────────────────┼───────┼──────┼──────────┼──────────────────┤  │
│ │ Doliprane 500mg  │ 34009 │   12 │   8 500  │ [+ Lot]   🗑     │  │
│ │ Amoxicilline 1g  │ 34010 │    6 │  12 000  │ [L-2401…] 🗑     │  │
│ └──────────────────┴───────┴──────┴──────────┴──────────────────┘  │
│─────────────────────────────────────────────────────────────────────│
│ Montant total : 147 000 FCFA                                        │
│─────────────────────────────────────────────────────────────────────│
│           [Annuler]  [💾 Enregistrer brouillon]  [✅ Valider BED]   │
└─────────────────────────────────────────────────────────────────────┘
```

### 7.5 Route à ajouter
```
il faut ajout bed-home comme une tab dans C:\Users\k.kobena\Documents\dev\full_warehouse\src\main\webapp\app\features\commande\feature\commande-home\commande-home.component.html
```
---

## 8. Fichiers à créer / modifier

### Backend

| Fichier | Action | Priorité |
|---|:---:|:---:|
| `db/migration/V1.0.X__bed_motif_commande.sql` | ➕ Migration : `motif_bed`, `commentaire_bed`, fournisseur nullable | 🔴 P0 |
| `domain/enumeration/MotifBed.java` | ➕ Enum motifs BED | 🔴 P0 |
| `domain/Commande.java` | ✏️ Ajouter `motifBed`, `commentaireBed` ; rendre `fournisseur` nullable | 🔴 P0 |
| `service/dto/BedDTO.java` | ➕ DTO en-tête BED | 🔴 P1 |
| `service/dto/BedLigneDTO.java` | ➕ DTO ligne BED | 🔴 P1 |
| `service/dto/BedSummaryDTO.java` | ➕ DTO liste BED (projection légère) | 🔴 P1 |
| `service/dto/BedResultDTO.java` | ➕ DTO résultat de validation BED | 🟡 P2 |
| `service/stock/BedService.java` | ➕ Interface service BED | 🔴 P1 |
| `service/stock/impl/BedServiceImpl.java` | ➕ Implémentation (créer, valider, annuler) | 🔴 P1 |
| `web/rest/commande/BedResource.java` | ➕ Endpoints REST BED | 🔴 P1 |
| `templates/bed-receipt.html` | ➕ Template Thymeleaf PDF BED | 🟡 P2 |

### Frontend

| Fichier | Action | Priorité |
|---|:---:|:---:|
| `features/commande/feature/bon-entree-diverse/data-access/bed.model.ts` | ➕ Interfaces `IBed`, `IBedLigne`, `MotifBed` | 🔴 P1 |
| `features/commande/feature/bon-entree-diverse/data-access/bed.service.ts` | ➕ Service HTTP BED | 🔴 P1 |
| `features/commande/feature/bon-entree-diverse/bed-home/` | ➕ Composant home (layout liste) | 🔴 P1 |
| `features/commande/feature/bon-entree-diverse/bed-list/` | ➕ Tableau BED (lazy, filtres, menu ⋮) | 🔴 P1 |
| `features/commande/feature/bon-entree-diverse/bed-form/` | ➕ Modal formulaire BED | 🔴 P1 |
| `features/commande/feature/bon-entree-diverse/bed-ligne-form/` | ➕ Ligne produit + lots inline | 🟡 P2 |
| `commande.routes.ts` ou routes générales | ✏️ Route `/bed` | 🔴 P1 |
| Menu de navigation | ✏️ Ajouter entrée "Entrées diverses" | 🟡 P2 |

---

## 9. Matrice de priorité

| Ref | Fonctionnalité | Impact métier | Complexité | Priorité |
|---|---|:---:|:---:|:---:|
| F1 | Migration BDD + enum `MotifBed` | 🔴 Bloquant | Faible | **P0 — Immédiat** |
| F2 | `BedService.createBed()` + `validateBed()` | 🔴 Critique | Moyenne | **P1** |
| F3 | Endpoints REST CRUD BED | 🔴 Critique | Faible | **P1** |
| F4 | Frontend : liste + formulaire BED | 🔴 Critique | Haute | **P1** |
| F5 | Numérotation automatique BED | 🟡 Important | Faible | **P1** |
| F6 | Gestion lots par ligne BED | 🟡 Important | Moyenne | **P2** |
| F7 | Génération PDF BED | 🟡 Important | Moyenne | **P2** |
| F8 | `BedService.cancelBed()` | 🟢 Confort | Moyenne | **P3** |
| F9 | Badge dashboard "X BED cette semaine" | 🟢 Confort | Faible | **P3** |
| F10 | Export Excel/CSV liste BED | 🟢 Confort | Faible | **P3** |

---

## 10. Estimation des efforts

| Phase | Description | Backend | Frontend | Priorité |
|---|---|:---:|:---:|:---:|
| **Phase 0** | Migration BDD + enum MotifBed + patch Commande | 0.5j | — | 🔴 **P0** |
| **Phase 1** | BedService (create + validate) + BedResource + DTOs | 2j | — | 🔴 **P1** |
| **Phase 2** | Frontend : liste + formulaire + route | — | 3j | 🔴 **P1** |
| **Phase 3** | Lots par ligne + génération PDF | 1j | 1j | 🟡 **P2** |
| **Phase 4** | Annulation + dashboard badge + export | 0.5j | 1j | 🟢 **P3** |
| **Phase 5** | Import CSV BASCULEMENT/PRESTIGE avec création BED | 1j | 0.5j | 🔴 **P1** |
| **TOTAL** | | **5j** | **5.5j** | — |

> **Point de départ recommandé :** Phase 0 (migration) → Phase 1 backend → Phase 2 frontend.
> Les Phases 3 et 4 peuvent être livrées en itération suivante sans bloquer l'usage quotidien.
> La Phase 5 peut être développée en parallèle de la Phase 1 car elle réutilise `BedService`.

---

## 11. Phase 5 — Importation CSV avec création de BED

### 11.1 Contexte

Les types `TypeImportationProduit.BASCULEMENT` et `BASCULEMENT_PRESTIGE` sont déjà gérés dans `ImportationProduitService` pour importer les produits. La Phase 5 ajoute la **création automatique d'un BED validé** lors de cet import : chaque produit importé devient une ligne du BED, ce qui garantit la traçabilité de toutes les entrées de stock initiales.

Ces deux types sont ajoutés comme motifs dans `MotifBed` :
- `BASCULEMENT` → "Basculement depuis autre logiciel"
- `BASCULEMENT_PRESTIGE` → "Basculement depuis Prestige"

### 11.2 Flux fonctionnel

```
Utilisateur (UI Installation)
         │
         ├── Sélectionne motif = BASCULEMENT ou BASCULEMENT_PRESTIGE
         ├── Sélectionne fournisseur (obligatoire pour ces deux types)
         ├── Upload fichier CSV
         │
         ▼
ImportationProduitService.installNewOfficine()
         │
         ├── [Existant] Parse CSV → crée/met à jour produits + stocks
         │
         └── [NOUVEAU] Pour chaque produit importé avec succès :
                 │  → accumule BedImportLigneDTO {produitId, qty, prixAchat}
                 │
                 ▼
         BedService.createAndValidateBedFromImport(motif, fournisseurId, lignes)
                 │
                 ├── createBon() → Commande(type=DIRECT, motifBed=BASCULEMENT|BASCULEMENT_PRESTIGE)
                 ├── addOrderLines() → une OrderLine par produit
                 ├── finalizeSaisieEntreeStock() → orderStatus = CLOSED
                 └── Retourne BED référence (BED-YYYYMMDD-NNN)
         │
         ▼
ResponseDTO enrichi avec { size, errorSize, rejectFileUrl, bedReference }
```

### 11.3 Modifications backend

#### 11.3.1 `MotifBed.java` — Ajout des deux nouveaux motifs

```java
public enum MotifBed {
    RETOUR_CLIENT("Retour client"),
    ECHANTILLON("Echantillon / Don laboratoire"),
    TRANSFERT_ENTRANT("Transfert entrant inter-pharmacie"),
    REGULARISATION("Régularisation positive"),
    CORRECTION_ERREUR("Correction d'erreur"),
    BASCULEMENT("Basculement depuis autre logiciel"),           // NOUVEAU
    BASCULEMENT_PRESTIGE("Basculement depuis Prestige"),        // NOUVEAU
    AUTRE("Autre");
    // ...
}
```

#### 11.3.2 `BedService` — Méthode dédiée à l'import en masse

```java
// Ajout dans BedService.java
/**
 * Crée et valide immédiatement un BED à partir d'un import CSV de basculement.
 * Le BED est directement CLOSED (pas de brouillon), avec une OrderLine par produit.
 *
 * @param motifBed      BASCULEMENT ou BASCULEMENT_PRESTIGE
 * @param fournisseurId fournisseur principal de l'import (obligatoire)
 * @param lignes        liste des produits importés avec qty et prixAchat
 * @return référence du BED créé (ex: "BED-20260405-001")
 */
String createAndValidateBedFromImport(MotifBed motifBed, Integer fournisseurId,
                                       List<BedImportLigneDTO> lignes);
```

**DTO dédié à l'import :**

```java
// service/dto/BedImportLigneDTO.java
public record BedImportLigneDTO(
    Integer produitId,          // ID du produit importé
    Integer fournisseurProduitId, // ID FournisseurProduit (pour le lien OrderLine)
    int quantite,               // qty importée (= record.qty())
    int prixAchat               // prix achat (= record.prixAchat())
) {}
```

#### 11.3.3 `ImportationProduitService` — Adaptation des méthodes BASCULEMENT

Modification de `faireBasculement()` et `faireBasculementPrestige()` pour :
1. Accumuler les `BedImportLigneDTO` après chaque `saveRecord()` réussi
2. Appeler `bedService.createAndValidateBedFromImport()` à la fin du traitement

```java
// Injection à ajouter dans ImportationProduitService
private final BedService bedService;

// Dans faireBasculement() — après la boucle forEach :
if (!bedLignes.isEmpty()) {
    String bedRef = bedService.createAndValidateBedFromImport(
        MotifBed.BASCULEMENT, fournisseur.getId(), bedLignes);
    response.setBedReference(bedRef);
}

// Dans faireBasculementPrestige() — idem avec MotifBed.BASCULEMENT_PRESTIGE
```

> **Important :** `bedService.createAndValidateBedFromImport()` opère en dehors du `transactionTemplate` de chaque record — une transaction dédiée encapsule la création du BED entier. Si la création du BED échoue, les produits sont quand même importés (import non bloqué par le BED).

#### 11.3.4 `ResponseDTO` — Champ `bedReference`

```java
// Ajout dans ResponseDTO.java
private String bedReference;   // "BED-20260405-001" — null si pas de BED créé

public String getBedReference() { return bedReference; }
public ResponseDTO setBedReference(String bedReference) {
    this.bedReference = bedReference;
    return this;
}
```

#### 11.3.5 `BedServiceImpl` — Implémentation de `createAndValidateBedFromImport`

```java
@Override
@Transactional
public String createAndValidateBedFromImport(MotifBed motifBed, Integer fournisseurId,
                                              List<BedImportLigneDTO> lignes) {
    LocalDate today = LocalDate.now();

    // 1. Créer la Commande (BED en brouillon)
    Commande commande = new Commande();
    commande.setId(generateCommandeId());           // séquence existante
    commande.setOrderDate(today);
    commande.setReceiptReference(generateBedReference(today));
    commande.setReceiptType(TypeDeliveryReceipt.DIRECT);
    commande.setOrderStatus(OrderStatut.REQUESTED);
    commande.setMotifBed(motifBed);
    if (fournisseurId != null) {
        commande.setFournisseur(fournisseurRepository.getReferenceById(fournisseurId));
    }
    commande = commandeRepository.save(commande);

    // 2. Créer les OrderLines
    int totalAmount = 0;
    for (BedImportLigneDTO ligne : lignes) {
        OrderLine ol = new OrderLine();
        ol.setCommande(commande);
        ol.setFournisseurProduit(
            fournisseurProduitRepository.getReferenceById(ligne.fournisseurProduitId()));
        ol.setQuantityRequested(ligne.quantite());
        ol.setQuantityReceived(ligne.quantite());
        ol.setOrderUnitPrice(ligne.prixAchat());
        orderLineRepository.save(ol);
        totalAmount += ligne.quantite() * ligne.prixAchat();
    }

    // 3. Finaliser (crédite stock + ferme le BED)
    commande.setOrderAmount(totalAmount);
    commande.setOrderStatus(OrderStatut.CLOSED);
    commande.setReceiptDate(today);
    commandeRepository.save(commande);

    return commande.getReceiptReference();
}
```

> **Note :** La Phase 5 ne crédite **pas** le stock via `finalizeSaisieEntreeStock()` car l'import CSV crédite déjà le `StockProduit` lors du `saveRecord()`. Le BED sert uniquement à la **traçabilité documentaire**. Le champ `quantityReceived` est renseigné mais le mouvement de stock est déjà enregistré par l'import.

### 11.4 Modifications frontend

#### 11.4.1 Composant d'importation existant

Dans le composant d'installation/importation (à identifier — probablement dans `features/admin/` ou `entities/installation/`), ajouter :

1. Lorsque `typeImportation === 'BASCULEMENT' || typeImportation === 'BASCULEMENT_PRESTIGE'` :
   - Afficher le sélecteur de fournisseur (déjà présent pour ces types)
   - Afficher un badge informatif : _"L'import va également créer un Bon d'Entrée Diverse pour la traçabilité"_

2. Après la réponse du backend, si `response.bedReference` est présent :
   ```typescript
   this.notificationService.success(
     `Import terminé. BED créé : ${response.bedReference}`
   );
   ```

#### 11.4.2 Interface `ResponseDTO` côté Angular

```typescript
// shared/model/response-dto.model.ts (ou équivalent)
export interface ResponseDTO {
  size?: number;
  errorSize?: number;
  rejectFileUrl?: string;
  bedReference?: string;   // NOUVEAU — null si pas de BED
}
```

### 11.5 Règles métier spécifiques BASCULEMENT

| Règle | Comportement |
|---|---|
| Fournisseur | **Obligatoire** pour BASCULEMENT et BASCULEMENT_PRESTIGE |
| Création stock | Déléguée à l'import CSV existant (pas de double crédit) |
| Mouvement stock | NON créé par le BED (déjà fait par `saveRecord()`) |
| Statut BED | Directement `CLOSED` (pas de brouillon) |
| Rollback BED | Indépendant de l'import : un échec du BED ne bloque pas l'import |
| Annulation BED | Non autorisée pour ces motifs (stock initial, irréversible) |

### 11.6 Fichiers à créer / modifier (Phase 5)

| Fichier | Action |
|---|---|
| `domain/enumeration/MotifBed.java` | ✏️ Ajouter `BASCULEMENT`, `BASCULEMENT_PRESTIGE` |
| `service/dto/BedImportLigneDTO.java` | ➕ Record DTO pour les lignes d'import |
| `service/dto/ResponseDTO.java` | ✏️ Ajouter champ `bedReference` |
| `service/stock/BedService.java` | ✏️ Ajouter méthode `createAndValidateBedFromImport()` |
| `service/stock/impl/BedServiceImpl.java` | ✏️ Implémenter `createAndValidateBedFromImport()` |
| `service/ImportationProduitService.java` | ✏️ Injecter `BedService`, accumuler lignes, appeler BED après import |
| `shared/model/response-dto.model.ts` | ✏️ Ajouter `bedReference` |
| Composant importation (frontend) | ✏️ Afficher `bedReference` dans notification de succès |

---

## Annexe A — Numérotation BED

Format : `BED-YYYYMMDD-NNN` (3 chiffres, remis à zéro chaque jour)

Exemples :
- `BED-20260405-001` — 1er BED du 5 avril 2026
- `BED-20260405-002` — 2ème BED du même jour

Stocké dans `Commande.receiptReference`.

---

## Annexe B — Règles de validation métier

| Règle | Condition | Erreur levée |
|---|---|---|
| Motif obligatoire | `motifBed != null` | `motifManquant` |
| Au moins une ligne | `lignes.size() >= 1` | `aucuneLigne` |
| Quantité positive | `quantite > 0` pour chaque ligne | `quantiteInvalide` |
| Produit actif | `produit.status == ENABLE` | `produitInactif` |
| Lot cohérent | si lot saisi : `numLot + expiryDate` obligatoires | `lotIncomplet` |
| BED pas encore validé | `orderStatus == REQUESTED` pour validate | `bedDejaValide` |

---

*Document créé le 2026-04-05*
*Statut : 📋 Plan complet — En attente d'implémentation*

