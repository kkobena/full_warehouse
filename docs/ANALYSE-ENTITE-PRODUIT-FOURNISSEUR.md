# Analyse — Entités `Produit` & `FournisseurProduit`

> **Fichiers analysés :**
> `domain/Produit.java` · `domain/FournisseurProduit.java`
> **Contexte :** Gestion du référentiel produit et des relations fournisseurs — Pharma-Smart
> **Date d'analyse :** Mai 2026

---

## Table des matières

1. [Vue d'ensemble](#1-vue-densemble)
2. [Entité `Produit`](#2-entité-produit)
3. [Entité `FournisseurProduit`](#3-entité-fournisseurproduit)
4. [Relations entre entités](#4-relations-entre-entités)
5. [Logique métier embarquée](#5-logique-métier-embarquée)
6. [Champs remarquables & particularités](#6-champs-remarquables--particularités)
7. [Points d'attention & recommandations](#7-points-dattention--recommandations)

---

## 1. Vue d'ensemble

Le domaine produit est le cœur du système Pharma-Smart. Il gère :

| Périmètre | Description |
|-----------|-------------|
| Référentiel produit | Médicaments, parapharmacies, dispositifs médicaux |
| Tarification | Prix d'achat, prix unitaire, prix MNP, TVA |
| Classification ABC | Criticité automatique ou forcée par l'administrateur |
| Gestion fournisseurs | Association produit ↔ fournisseur avec colisage et prix |
| Rangement | Association produit ↔ rayon (`RayonProduit`) |
| Stock | Association produit ↔ points de vente (`StockProduit`) |

---

## 2. Entité `Produit`

### 2.1 Identité et contraintes DB

```
Table : produit
Contrainte unique : (libelle, type_produit)
Index : libelle, code_ean_labo, status
Cache Hibernate : READ_WRITE
```

### 2.2 Attributs principaux

| Champ | Type Java | Colonne DB | Rôle |
|-------|-----------|------------|------|
| `id` | `Integer` | `id` (PK, auto) | Identifiant technique |
| `libelle` | `String` | `libelle` | Nom du produit (obligatoire) |
| `typeProduit` | `TypeProduit` (enum) | `type_produit` (varchar 15) | Type : médicament, parapharmacie… |
| `costAmount` | `Integer` | `cost_amount` | Coût d'achat total (en centimes) |
| `regularUnitPrice` | `Integer` | `regular_unit_price` | Prix de vente unitaire public |
| `netUnitPrice` | `Integer` | `net_unit_price` | Prix de vente net (après remise) |
| `itemQty` | `Integer` | `item_qty` (≥ 0) | Quantité par conditionnement |
| `itemCostAmount` | `Integer` | `item_cost_amount` | Coût par unité |
| `itemRegularUnitPrice` | `Integer` | `item_regular_unit_price` | Prix unitaire public par unité |
| `prixMnp` | `Integer` | `prix_mnp` | Prix MNP (Médicaments Non Pris en charge) |
| `qtyAppro` | `Integer` | `qty_appro` | Quantité d'approvisionnement par défaut |
| `qtySeuilMini` | `Integer` | `qty_seuil_mini` | Seuil minimal de stock déclenchant un réapprovisionnement |
| `codeEanLaboratoire` | `String` | `code_ean_labo` (13 char) | Code EAN laboratoire |
| `status` | `Status` (enum) | `status` (varchar 10) | ENABLE / DISABLE |
| `statutLegal` | `StatutLegal` (enum) | `statut_legal` (varchar 20) | SANS_LISTE, LISTE_I, LISTE_II… |
| `classeCriticite` | `ClasseCriticite` (enum) | `classe_criticite` (varchar 10) | Classification ABC : A, A_PLUS, B, C |
| `codeRemise` | `CodeRemise` (enum) | `code_remise` (varchar 6) | Code mappé sur les grilles de remises |

### 2.3 Flags booléens

| Champ | Défaut | Description |
|-------|--------|-------------|
| `gestionLot` | `true` | Activer la gestion par lot/péremption |
| `thermosensible` | `false` | Produit à conserver au froid |
| `remisable` | `true` | Peut faire l'objet d'une remise commerciale |
| `chiffre` | `true` | Pris en compte dans le chiffre d'affaires |
| `deconditionnable` | `false` | Peut être vendu à l'unité (déconditionnement) |
| `checkExpiryDate` | `false` | ⚠️ Marqué TODO: to remove — à supprimer |
| `isClassificationOverridden` | `false` | Empêche la reclassification ABC automatique |
| `estMedicamentEssentiel` | `false` | Liste OMS/LGO, protégé contre descente sous classe B |
| `estProduitGarde` | `false` | Classé automatiquement A_PLUS (produit de garde officine) |

### 2.4 Relations ManyToOne

| Relation | Entité cible | Obligatoire |
|----------|-------------|-------------|
| `tva` | `Tva` | ✅ (`optional = false`) |
| `famille` | `FamilleProduit` | ✅ (`optional = false`) |
| `laboratoire` | `Laboratoire` | ❌ optionnel |
| `forme` | `FormProduit` | ❌ optionnel |
| `gamme` | `GammeProduit` | ❌ optionnel |
| `dci` | `Dci` | ❌ optionnel |
| `tableau` | `Tableau` | ❌ optionnel |
| `parent` | `Produit` | ❌ autoreférence (produits déconditionnables) |

### 2.5 Relations OneToMany

| Relation | Entité cible | Cascade | Cache |
|----------|-------------|---------|-------|
| `fournisseurProduits` | `FournisseurProduit` | PERSIST, MERGE, REMOVE | ✅ READ_WRITE |
| `stockProduits` | `StockProduit` | REMOVE | ❌ |
| `rayonProduits` | `RayonProduit` | PERSIST, MERGE, REMOVE | ✅ READ_WRITE |
| `optionPrixProduit` | `OptionPrixProduit` | REMOVE | ❌ |
| `produits` (enfants) | `Produit` | REMOVE | ❌ |

### 2.6 Relation OneToOne

```java
@OneToOne
@JoinColumn(name = "fournisseur_produit_principal_id", referencedColumnName = "id")
private FournisseurProduit fournisseurProduitPrincipal;
```

Désigne le fournisseur **principal** parmi la liste des fournisseurs associés. Clé étrangère directe sur `fournisseur_produit`.

---

## 3. Entité `FournisseurProduit`

### 3.1 Identité et contraintes DB

```
Table : fournisseur_produit
Contrainte unique 1 : (produit_id, fournisseur_id)
Contrainte unique 2 : (code_cip, fournisseur_id)
Index : code_cip ASC, code_ean
Cache Hibernate : READ_WRITE
```

### 3.2 Attributs principaux

| Champ | Type Java | Colonne DB | Rôle |
|-------|-----------|------------|------|
| `id` | `Integer` | `id` (PK, auto) | Identifiant technique |
| `codeCip` | `String` | `code_cip` (20 char) | Code CIP (identifiant produit officiel pharmacie FR) |
| `codeEan` | `String` | `code_ean` (20 char) | Code EAN du conditionnement fournisseur |
| `prixAchat` | `Integer` | `prix_achat` | Prix d'achat HT au fournisseur (en centimes) |
| `prixUni` | `Integer` | `prix_uni` | Prix unitaire fournisseur |
| `qteColis` | `Integer` | `qte_colis` | Nb unités par colis (colisage fournisseur) |
| `qteMinimaleCommande` | `Integer` | `qte_minimale_commande` | Quantité minimale de commande imposée |
| `createdDate` | `LocalDateTime` | `created_date` (non modifiable) | Date de création |
| `lastModifiedDate` | `LocalDateTime` | `last_modified_date` | Dernière modification |

### 3.3 Relations

| Relation | Entité cible | Obligatoire |
|----------|-------------|-------------|
| `produit` | `Produit` | ✅ (`optional = false`) |
| `fournisseur` | `Fournisseur` | ✅ (`optional = false`, LAZY) |
| `orderLines` | `OrderLine` | ❌ (Set, LAZY) |

---

## 4. Relations entre entités

```
Produit (1) ──────────────── (*) FournisseurProduit
    │                                │
    │ fournisseurProduitPrincipal      │── prixAchat, prixUni
    └──────── OneToOne ───────────────┘── codeCip, codeEan
                                     │── qteColis, qteMinimaleCommande
                                     │
                                     (* ) OrderLine
                                     │
                                     (1) Fournisseur

Produit (1) ──── (*) StockProduit     → stock par point de vente
Produit (1) ──── (*) RayonProduit     → emplacement rayon
Produit (1) ──── (*) OptionPrixProduit → options de tarification
Produit (*) ──── (1) Produit.parent   → déconditionnement (parent/enfant)
```

---

## 5. Logique métier embarquée

### 5.1 `Produit.isOrdonnanceObligatoire()`
```java
public boolean isOrdonnanceObligatoire() {
    return statutLegal != null && statutLegal.isOrdonnanceObligatoire();
}
```
Délègue à `StatutLegal.isOrdonnanceObligatoire()` pour identifier les médicaments de liste I/II.

### 5.2 `Produit.getEffectiveClasseCriticite()`
```java
public ClasseCriticite getEffectiveClasseCriticite() {
    return Objects.requireNonNullElse(classeCriticite, ClasseCriticite.B);
}
```
Retourne la classe B par défaut si aucune classification n'est définie.

### 5.3 `FournisseurProduit.appliquerColisage(int qty)`
```java
public int appliquerColisage(int qty) {
    int colis = (qteColis != null && qteColis > 1) ? qteColis : 1;
    int minimum = (qteMinimaleCommande != null && qteMinimaleCommande > 0) ? qteMinimaleCommande : 0;
    int arrondi = colis == 1 ? qty : (int) Math.ceil((double) qty / colis) * colis;
    return Math.max(Math.max(1, arrondi), minimum);
}
```
**Règle SEMOIS :** `max(qteMin, ceil(qty / colis) * colis)` — arrondit la quantité calculée au multiple supérieur du colisage fournisseur tout en respectant la quantité minimale de commande. Résultat toujours ≥ 1.

---

## 6. Champs remarquables & particularités

### 6.1 Classification ABC automatique
Le système supporte une reclassification automatique via CMM (Consommation Moyenne Mensuelle) :
- `isClassificationOverridden = true` → la classe est figée (ne sera pas modifiée automatiquement)
- `estMedicamentEssentiel = true` → protégé contre la descente sous la classe B
- `estProduitGarde = true` → forcé en A_PLUS automatiquement

### 6.2 Déconditionnement
- `deconditionnable = true` → le produit peut avoir des enfants (`produits` list)
- `seuilDeconditionnement` → seuil de stock en vente détail pour déclencher un déconditionnement
- La relation `parent` permet d'identifier le produit conditionné source

### 6.3 Fournisseur principal
Un produit peut avoir **plusieurs fournisseurs**, mais un seul est désigné **principal** via la FK `fournisseur_produit_principal_id`. Cette relation est maintenue en synergie avec la collection `fournisseurProduits`.

### 6.4 Prix en centimes
Tous les montants (`costAmount`, `regularUnitPrice`, `netUnitPrice`, `prixMnp`, `prixAchat`, `prixUni`) sont stockés en **centimes (entiers)** pour éviter les erreurs d'arrondi flottant.

### 6.5 Code CIP
Le `codeCip` est l'identifiant officiel français pour les médicaments (Code Identifiant de Présentation). La contrainte unique `(code_cip, fournisseur_id)` permet à deux fournisseurs différents d'avoir le même CIP (cas de distributeurs).

---

## 7. Points d'attention & recommandations

| # | Priorité | Constat | Recommandation |
|---|----------|---------|----------------|
| 1 | 🔴 Haute | `checkExpiryDate` marqué `TODO: to remove` | Supprimer le champ + migration Flyway + adapter les services |
| 2 | 🟡 Moyenne | `fournisseurProduitPrincipal` est un OneToOne sur `FournisseurProduit` | Vérifier la cohérence lors de la suppression d'un FournisseurProduit (risque de FK orpheline) |
| 3 | 🟡 Moyenne | Cascade `REMOVE` large sur `fournisseurProduits` | Attention aux suppressions en cascade accidentelles si le produit est supprimé |
| 4 | 🟢 Basse | `codeCip` déclaré `@NotNull` dans le getter mais `@Column` sans `nullable = false` | Aligner la contrainte DB avec la contrainte de validation |
| 5 | 🟢 Basse | `seuilDeconditionnement` initialisé à `0` | Vérifier que la valeur `0` est bien interprétée comme "désactivé" dans tous les services |
| 6 | 🟢 Basse | Pas d'index sur `fournisseur_id` dans `fournisseur_produit` | Envisager un index pour améliorer les requêtes par fournisseur |

---

*Document généré automatiquement — Pharma-Smart · Mai 2026*

