# Analyse comparative — Module de Suggestions de Commande
> Pharma-Smart vs. logiciels de référence officine  
> Date : 2026-03-26 — v10 (décision finale révisée : batch crée les Suggestion, vue SEMOIS = analytics uniquement)

---

## 1. Périmètre analysé

| Composant | Rôle |
|---|---|
| `suggestions-unified` | Shell parent : onglets + toolbar commun |
| `suggestion-home` | Vue "Par fournisseur" (split-panel fournisseur / produits) |
| `semois-suggestions` | Vue "SEMOIS" (table paginée, KPI, filtres avancés) |
| `suggestion-produit-panel` | Panneau droite — liste des lignes éditable |
| `suggestion-fournisseur-list` | Liste gauche des fournisseurs avec résumé urgents |
| `semois-classe-config` | Paramétrage des classes A/B/C |
| `semois-model-config` | Configuration du modèle SEMOIS |
| `v_semois_suggestion` | Vue PostgreSQL READ-ONLY (données SEMOIS) |
| `semois_configuration` | Table des paramètres SEMOIS par produit |

---

## 2. Architecture technique SEMOIS — Contexte critique

### 2.1 Comment fonctionne `v_semois_suggestion`

La vue est une **vue ordinaire PostgreSQL** (non matérialisée), annotée `@Immutable` côté Hibernate.
Elle combine deux sources :

| Colonne | Source | Fréquence MAJ |
|---|---|---|
| `vmm`, `marge_securite`, `stock_objectif` | `semois_configuration` | Batch nocturne |
| `stock_actuel`, `quantite_a_commander` | `stock_produit` (SUM temps réel) | Temps réel |
| `classe_criticite` | `produit.classe_criticite` | Lors de la classification |
| `delai_livraison_jours` | Cascade: config → fournisseur → groupe → 7j | Configuration |

**Implication directe :** `quantite_a_commander` est **calculé**
(`GREATEST(stock_objectif - stock_actuel, marge_securite - stock_actuel)`),
il n'est **pas stocké**. Il est impossible d'écrire dans cette colonne depuis Hibernate.

---

## 3. Point 1 — Inline editing et vue immuable : analyse et propositions

### 3.1 Le problème

La vue `v_semois_suggestion` est `@Immutable`. La colonne `quantite_a_commander` est une expression
SQL calculée, pas un champ persisté. Impossible de faire un `UPDATE` dessus directement.
Cela empêche l'édition inline de la quantité, fonctionnalité présente chez **tous les concurrents**.

### 3.2 Comment font les autres logiciels ?

Aucun des logiciels de référence ne travaille directement sur une vue de calcul.
Ils utilisent tous le concept de **"panier de commande"** (ou table de travail temporaire) :

| Logiciel | Mécanisme |
|---|---|
| **Winpharma** | Vue suggestion (lecture seule) → "Mettre au panier" → table `panier_reappro` (modifiable) → validation → commande |
| **Pharmagest** | `proposition_commande` (calculée) → sélection → `panier_commande` (éditable) → "Valider et envoyer" |
| **LGPI** | `suggestion_reappro` (vue) → `ligne_commande_brouillon` (éditable) → "Transformer en commande" |
| **Alliadis** | "Préparer la commande" → table temporaire modifiable → validation |

> **Principe commun :** La vue/calcul génère une *proposition*. L'utilisateur travaille sur un
> *brouillon modifiable* avant de valider. Les deux étapes sont toujours clairement séparées.

### 3.3 Proposition retenue — Extension de `Suggestion.java` + `TypeSuggession.SEMOIS`

#### Principe : "Mettre au panier" → `Suggestion` de type `SEMOIS`

L'idée est de **réutiliser l'infrastructure existante** (`Suggestion`, `SuggestionLine`, vue "Par fournisseur")
sans aucun nouveau champ ni nouvelle table. C'est exactement le pattern Winpharma.

**Étape 1 — Ajouter `SEMOIS` dans l'enum `TypeSuggession`**

```java
// TypeSuggession.java
public enum TypeSuggession {
    AUTO,
    MANUELLE,
    SEMOIS  // ← nouveau : suggestions générées depuis le module SEMOIS
}
```

**Étape 2 — Workflow "Mettre au panier"**

```
Vue SEMOIS (tableau immuable)
    │
    ▼ Utilisateur sélectionne N lignes → clique "Mettre au panier"
    │
    ▼ [POST /api/semois/mettre-au-panier]
    │  → Grouper les lignes sélectionnées par fournisseur
    │  → Pour chaque fournisseur :
    │      • Chercher une Suggestion (type=SEMOIS, statut=GENEREE, fournisseur=X, magasin=current)
    │      • Si trouvée → ajouter/mettre à jour les SuggestionLine
    │      • Sinon → créer une nouvelle Suggestion(type=SEMOIS) + SuggestionLine
    │
    ▼ Réponse : { suggestionIds: [42, 43], fournisseurIds: [1, 7] }
    │
    ▼ Frontend : bascule sur l'onglet "Par fournisseur"
       auto-sélectionne la suggestion du premier fournisseur
       → panel produits pré-chargé avec les lignes, quantités éditables inline
```

**Étape 3 — Inline editing dans "Par fournisseur"**

Les lignes générées depuis SEMOIS (`TypeSuggession.SEMOIS`) sont des `SuggestionLine` normales.
L'utilisateur peut modifier les quantités dans `suggestion-produit-panel` exactement comme
pour une suggestion manuelle. **Aucun nouveau composant ni champ.**

**Étape 4 — Validation et commande**

Quand l'utilisateur est satisfait :
- Bouton "Commander tout" → `createCommandeFromSemoisLines` existant
- La `Suggestion` est supprimée (ou passée en `VALIDEE`) comme pour les suggestions AUTO

**Ce que l'on ne touche pas :**
- La vue `v_semois_suggestion` reste immuable ✅
- `semois_configuration` sans champ `qte_override` ✅
- `suggestion-produit-panel` inchangé ✅
- `suggestion-home` inchangé ✅

**Avantages :**
- Zéro nouvelle table, zéro nouvelle migration SQL complexe
- Réutilise 100% de l'infrastructure de suggestion existante
- Le pharmacien travaille dans un environnement qu'il connaît déjà
- Les quantités sont persistées en base (`SuggestionLine.quantity`) entre les sessions

**Seul ajout nécessaire :**
- Valeur `SEMOIS` dans l'enum `TypeSuggession`
- Endpoint `POST /api/semois/mettre-au-panier` côté backend
- Bouton "Mettre au panier" dans l'action bar de la vue SEMOIS
- Navigation automatique vers l'onglet "Par fournisseur" + auto-sélection du fournisseur côté frontend

---

## 4. Point 2 — Historique "dernière commande" : précision

L'analyse initiale était incomplète. La vue "Par fournisseur" expose déjà `consommationMensuelle`
(VMM historique N-1, N-2…). Il faut distinguer deux types d'informations :

| Données déjà présentes | Données manquantes |
|---|---|
| `consommationMensuelle` (VMM historique N-1, N-2…) | Date de la dernière **commande passée** au fournisseur |
| VMM calculée (`vmm`) | Quantité commandée lors de cette commande |
| Jours de stock restant | Délai moyen de livraison **observé** (vs. délai théorique) |

**Ce qui manque réellement :**
> "La dernière fois que j'ai commandé ce produit, c'était le **15/02/2026** — **12 boîtes** commandées"

Cette information est dans la table `commande` / `order_line`, accessible par jointure.
C'est l'**historique achat** (date + qte commandée), distinct de la consommation de vente.

---

## 5. Point 3 — Exclusion temporaire (P2.3) : cycle de vie complet

### 5.1 Ce qui manquait dans l'analyse initiale

L'analyse précédente décrivait uniquement la "sortie" (exclusion) mais pas la "réintroduction".

### 5.2 Cycle de vie complet proposé

```
Produit ACTIF dans SEMOIS
        │
        ▼ [Pharmacien clique "Exclure X jours" + saisit un motif]
  exclusion_date = now()
  exclusion_duree_jours = X (défaut 30)
  exclusion_motif = "surstock promo" | "rupture fournisseur" | ...
        │
        ├──▶ Chaque nuit (batch SEMOIS)
        │    IF now() > exclusion_date + exclusion_duree_jours
        │       → réintégration automatique (exclusion_date = NULL)
        │
        ├──▶ [Pharmacien clique "Annuler l'exclusion"]
        │       → réintégration manuelle immédiate
        │
        └──▶ [Réception de stock → stock_actuel > stock_objectif * 1.5]
                → réintégration automatique optionnelle (paramétrable)
```

### 5.3 Champs à ajouter dans `semois_configuration`

```sql
ALTER TABLE semois_configuration
    ADD COLUMN exclusion_date        TIMESTAMP,
    ADD COLUMN exclusion_duree_jours INTEGER DEFAULT 30,
    ADD COLUMN exclusion_motif       VARCHAR(255);
```

### 5.4 Modification de la vue `v_semois_suggestion`

```sql
-- Ajouter dans le WHERE :
AND (sc.exclusion_date IS NULL
     OR NOW() > sc.exclusion_date + (sc.exclusion_duree_jours || ' days')::INTERVAL)
```

Un produit exclu disparaît automatiquement de la liste SEMOIS et réapparaît une fois la durée
écoulée — sans intervention manuelle. C'est le comportement de Winpharma et LGPI.

### 5.5 Gestion UI des exclusions actives

- Badge "Exclusions actives (X)" dans l'action bar SEMOIS
- Onglet ou modal dédiée : liste des produits exclus avec date de fin et motif
- Actions : "Annuler", "Prolonger X jours"

---

## 6. Point 4 — Workflow de commande : analyse et comparaison

### 6.1 Workflow actuel Pharma-Smart

```
SEMOIS → Sélection lignes → [POST /api/semois/commander]
→ createCommandesFromSemois() → Commande(REQUESTED) créée directement
```

La commande est créée **directement**, sans étape intermédiaire, sans PharmaML.

### 6.2 Workflows des concurrents

| Logiciel | Étape 1 | Étape 2 | Étape 3 | Canal de transmission |
|---|---|---|---|---|
| **Winpharma** | Suggestion SEMOIS | Panier (éditable) | Valider → "Envoyer par EDI" ou "Imprimer" | EDI CERP, OCP, Phoenix |
| **Pharmagest** | Proposition | Panier | Valider → choix canal | EDI grossiste (obligatoire si paramétré) |
| **LGPI** | Suggestion | Brouillon | Commander → EDI ou manuel | EDI ou portail fournisseur |
| **Alliadis** | Suggestion | Revue panier | Commander | EDI ou email automatique |
| **Pharma-Smart actuel** | Suggestion SEMOIS | *(absent)* | Commander → Commande interne | **Aucun — interne seulement** |

### 6.3 Analyse : est-ce le bon workflow ?

**Pour une officine sans EDI** : Oui, créer une commande interne est correct.
La commande sert de traçabilité et déclenche la réception ultérieure.

**Pour une officine avec PharmaML** : Le workflow actuel est **sous-optimal** :
- Le pharmacien crée une commande interne (REQUESTED)
- Puis il doit aller sur PharmaML séparément pour envoyer la commande au grossiste
- Duplication de saisie, risque d'incohérence entre la commande interne et l'envoi réel

### 6.4 Workflow cible recommandé

Enrichir la modal `SemoisCommanderModalComponent` avec un choix de canal :

```
SEMOIS → Sélection lignes
        │
        ▼ Modal récapitulative (SemoisCommanderModalComponent)
   ┌──────────────────────────────────────────────┐
   │  Récapitulatif :                              │
   │  • 3 fournisseurs · 15 produits · 125 000 F  │
   │  • ⚠ Budget dépassé de 15 000 F              │
   │                                              │
   │  [Créer commande interne]  ← comportement actuel │
   │  [Envoyer via PharmaML]    ← nouveau         │
   └──────────────────────────────────────────────┘
```

L'option PharmaML :
1. Crée la commande interne (REQUESTED)
2. Envoie simultanément la demande via `POST /api/pharmaml/commander`
3. Retourne le récapitulatif des prix confirmés par le grossiste

---

## 7. Point 5 — Bug `api/semois/suggestions` (pagination incohérente)

### 7.1 Correction de l'analyse

La vue SQL `v_semois_suggestion` contient des données **correctes**.
Le bug est dans le backend, dans `SemoisCalculationService.getAllSuggestions()`.

### 7.2 Cause racine — Post-filtre Java après pagination DB

```java
// SemoisCalculationService.java — getAllSuggestions()

// 1. Charge N lignes depuis la vue (paginé côté DB)
Page<SemoisSuggestionView> viewPage = semoisSuggestionViewRepository
    .findAllWithFilters(search, classeCriticite, fournisseurId, niveauUrgence, pageable);

// 2. Calcule les quantités en attente (commandes REQUESTED) par produit
List<Integer> produitIds = viewPage.getContent().stream()
    .map(SemoisSuggestionView::getProduitId).toList();
Map<Integer, Integer> pendingQtyMap = loadPendingOrderQtyBatch(produitIds);

// 3. ⚠️ FILTRE JAVA après pagination DB — supprime des lignes déjà comptées
List<SemoisSuggestionDTO> suggestions = viewPage.getContent().stream()
    .map(view -> toDTO(view, pendingQtyMap.getOrDefault(view.getProduitId(), 0)))
    .filter(dto -> dto.quantiteACommander() > 0)   // ← retire les items couverts par commande en cours
    .toList();

// 4. ⚠️ PageImpl avec le total DB original (incorrect après filtre)
return new PageImpl<>(suggestions, pageable, viewPage.getTotalElements());
//                                            ^^^^^^^^^^^^^^^^^^^^^^^^
//                 Ce total vient de la DB AVANT le filtre Java.
//                 Si 20 lignes sont demandées et 3 sont filtrées,
//                 la page retourne 17 items mais totalElements = 50.
```

### 7.3 Conséquences concrètes

| Ce qui se passe | Effet observé |
|---|---|
| La page contient moins d'items que `pageSize` | Le frontend affiche une page incomplète |
| `X-Total-Count` = total DB (trop grand) | Le nombre de pages calculé est supérieur à la réalité |
| La dernière page réelle est atteinte avant la dernière page calculée | Des pages vides apparaissent en fin de liste |
| Décalage d'offset sur la page suivante | Des items sont sautés ou dupliqués selon le décalage |

### 7.4 Correction — Intégrer le filtre `pending_qty` dans la vue SQL

**Principe :** Le filtre doit être entièrement **côté SQL**. La vue `v_semois_suggestion` doit
intégrer la soustraction des commandes en attente directement dans le calcul de
`quantite_a_commander`, de façon à ce que le `COUNT(*)` de la pagination reflète les
vrais items commandables — sans aucun filtre Java post-pagination.

**Nouvelle migration `V1.3.9__semois_view_pending_qty.sql` :**

```sql
CREATE OR REPLACE VIEW v_semois_suggestion AS
SELECT
    p.id                                                                               AS produit_id,
    p.libelle,
    fp.code_cip,
    fp.fournisseur_id,
    f.libelle                                                                          AS fournisseur_libelle,
    p.classe_criticite,
    scc.coefficient_securite,
    COALESCE(sc.delai_livraison_jours, f.delai_livraison_jours,
             gf.delai_livraison_jours, 7)                                              AS delai_livraison_jours,
    COALESCE(sc.vmm_calcule, 0)                                                       AS vmm,
    COALESCE(sc.marge_securite, 0)                                                    AS marge_securite,
    COALESCE(sc.stock_objectif_calcule, 0)                                            AS stock_objectif,
    COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0)                                        AS stock_actuel,

    -- Quantité déjà commandée (commandes REQUESTED non encore livrées)
    COALESCE((
        SELECT SUM(ol.quantity)
        FROM order_line ol
        JOIN commande c ON c.id = ol.commande_id
        WHERE ol.produit_id = p.id
          AND c.order_status = 'REQUESTED'
    ), 0)                                                                              AS pending_qty,

    -- Quantité à commander nette = besoin - pending (jamais négative)
    GREATEST(
        0,
        GREATEST(
            COALESCE(sc.stock_objectif_calcule, 0)
                - COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0)
                - COALESCE((
                    SELECT SUM(ol2.quantity) FROM order_line ol2
                    JOIN commande c2 ON c2.id = ol2.commande_id
                    WHERE ol2.produit_id = p.id AND c2.order_status = 'REQUESTED'
                  ), 0),
            COALESCE(sc.marge_securite, 0)
                - COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0)
                - COALESCE((
                    SELECT SUM(ol3.quantity) FROM order_line ol3
                    JOIN commande c3 ON c3.id = ol3.commande_id
                    WHERE ol3.produit_id = p.id AND c3.order_status = 'REQUESTED'
                  ), 0)
        )
    )                                                                                  AS quantite_a_commander,

    sc.date_dernier_calcul
FROM produit p
    LEFT JOIN fournisseur_produit fp  ON fp.id = p.fournisseur_produit_principal_id
    LEFT JOIN fournisseur f           ON f.id  = fp.fournisseur_id
    LEFT JOIN groupe_fournisseur gf   ON gf.id = f.groupe_pournisseur_id
    LEFT JOIN semois_configuration sc ON sc.produit_id = p.id
    LEFT JOIN semois_classe_config scc ON scc.classe_criticite = p.classe_criticite
    LEFT JOIN stock_produit sp        ON sp.produit_id = p.id
WHERE p.status::text      = 'ENABLE'::text
  AND p.type_produit::text <> 'DETAIL'::text
  AND COALESCE(sc.vmm_calcule, 0) > 0
GROUP BY p.id, p.libelle, fp.code_cip, fp.fournisseur_id, f.libelle,
         f.delai_livraison_jours, gf.delai_livraison_jours,
         p.classe_criticite, scc.coefficient_securite,
         sc.delai_livraison_jours, sc.vmm_calcule, sc.marge_securite,
         sc.stock_objectif_calcule, sc.date_dernier_calcul
-- Filtre final : n'expose que les produits réellement à commander (après pending)
HAVING GREATEST(
    COALESCE(sc.stock_objectif_calcule, 0) - COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0)
        - COALESCE((SELECT SUM(ol4.quantity) FROM order_line ol4
                    JOIN commande c4 ON c4.id = ol4.commande_id
                    WHERE ol4.produit_id = p.id AND c4.order_status = 'REQUESTED'), 0),
    COALESCE(sc.marge_securite, 0) - COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0)
        - COALESCE((SELECT SUM(ol5.quantity) FROM order_line ol5
                    JOIN commande c5 ON c5.id = ol5.commande_id
                    WHERE ol5.produit_id = p.id AND c5.order_status = 'REQUESTED'), 0)
) > 0;
```

**Impact sur le backend `SemoisCalculationService` :**

```java
// AVANT (v3) — filtre Java incorrect
List<SemoisSuggestionDTO> suggestions = viewPage.getContent().stream()
    .map(view -> toDTO(view, pendingQtyMap.getOrDefault(view.getProduitId(), 0)))
    .filter(dto -> dto.quantiteACommander() > 0)  // ← supprimé
    .toList();
return new PageImpl<>(suggestions, pageable, viewPage.getTotalElements()); // ← total faux

// APRÈS (v4) — plus de filtre Java, la vue fait le travail
List<SemoisSuggestionDTO> suggestions = viewPage.getContent().stream()
    .map(this::toDTO)         // toDTO simple, sans pendingQtyMap
    .toList();
return new PageImpl<>(suggestions, pageable, viewPage.getTotalElements()); // ← total exact
```

`loadPendingOrderQtyBatch()` devient inutile et peut être supprimé.
`SemoisSuggestionView` gagne le champ `pending_qty` (exposé dans le DTO si besoin d'affichage).

---

## 8. Inventaire fonctionnel mis à jour

### 8.1 Vue "Par fournisseur"

**Présent ✅**
- Split-panel fournisseurs + lignes produits avec inline editing (AG Grid)
- Résumé par fournisseur : nb urgents, montant estimé
- Ajout manuel de produit, fusion, suppression, export PDF + CSV
- Vérification disponibilité via PharmaML
- Validation / Rejet de suggestion, Comparaison fournisseurs
- Alerte dépassement budget mensuel
- `consommationMensuelle` **mappée** dans le façade (`suggestion-facade.service.ts:513`) ✅

**Absent ❌ — Bug silencieux**
- `consommationMensuelle` est mappée dans `SuggestionLigneEnrichie` et le façade,
  mais **aucun `ColDef` AG Grid** dans `suggestion-produit-panel.component.ts` ne la déclare.
  Les données arrivent depuis le backend mais ne sont **jamais affichées** au pharmacien.
  *(Le vieux composant `edit-suggestion.component.html` l'affiche — pas le nouveau panel)*
- Date + qté de la **dernière commande passée** (≠ consommation VMM)
- Comparaison prix fournisseur sur chaque ligne
- Regroupement par famille / laboratoire
- Colonne valorisation (prix × qté)
- Gestion du colisage

### 8.2 Vue "SEMOIS"

**Présent ✅**
- KPI cards, tableau paginé lazy-loading, filtres avancés
- Couverture actuelle vs. cible, jours restants, code couleur
- Sélection multiple + commande groupée

**Absent ❌**
- **Inline editing qté** (bloqué par vue immuable — cf. Section 3)
- Export CSV/Excel depuis SEMOIS
- Date + qté dernière commande par produit
- Tendance VMM (↑ ↓ ↔)
- Simulation "après commande → couverture = X mois"
- Exclusion temporaire avec cycle de vie clair
- **Filtre `quantiteACommander > 0` par défaut manquant** (bug — cf. Section 7)
- Tooltip "Détail du calcul SEMOIS"
- Barre de progression couverture visuelle
- Canal d'envoi PharmaML depuis SEMOIS
- **"Mettre au panier" (TypeSuggession.SEMOIS → bascule sur onglet fournisseur)** — cf. Section 3

---

## 9. Plan d'implémentation priorisé

### 🔴🔴 Priorité 0 — Bugs bloquants (données présentes mais non affichées / pagination cassée)

| # | Tâche | Fichier cible | Ce qu'il faut faire |
|---|---|---|---|
| P0.1 | **`consommationMensuelle` non affichée dans AG Grid** | `suggestion-produit-panel.component.ts` | Ajouter des `ColDef` dynamiques depuis les clés de `consommationMensuelle` |
| P0.2 | **Bug pagination SEMOIS** — post-filtre Java corrompt `totalElements` | Vue SQL `v_semois_suggestion` + migration `V1.3.9` | Intégrer `pending_qty` dans la vue + `HAVING` SQL, supprimer `.filter()` Java |
| P0.3 | **Cohabitation** — même produit dans 2 onglets avec 2 quantités différentes (vue ignore `pending_qty`) | `suggerer()` | Ajouter filtre `!isSemois || semoisConfigByProduitId.get(q.produit().getId()) == null` |
| P0.4 | **Vue actuelle sans filtre `vmm > 0`** — tous les produits ENABLE apparaissent | `v_semois_suggestion` | Ajouter `AND COALESCE(sc.vmm_calcule, 0) > 0` dans le WHERE |

**Détail P0.1 — Colonnes `consommationMensuelle` dynamiques**

Le champ est dans `SuggestionLigneEnrichie.consommationMensuelle?: Record<string, number>`.
Il est mappé dans la façade (`suggestion-facade.service.ts:513`).
Mais les `columnDefs` dans `suggestion-produit-panel.component.ts` n'en contiennent aucune.

**Solution :** Générer les colonnes dynamiquement depuis les clés du premier objet :

```typescript
// Computed depuis le signal lignes() — ajout dans suggestion-produit-panel.component.ts
readonly moisColumnDefs = computed<ColDef<SuggestionLigneEnrichie>[]>(() => {
  const first = this.lignes().find(l => l.consommationMensuelle);
  if (!first?.consommationMensuelle) return [];
  return Object.keys(first.consommationMensuelle)
    .sort()   // ordre chronologique
    .map(mois => ({
      headerName: mois,          // ex: "Janv.", "Févr."
      width: 70,
      type: 'numericColumn',
      sortable: false,
      valueGetter: (p: any) => p.data?.consommationMensuelle?.[mois] ?? 0,
      cellStyle: { color: '#6c757d', fontSize: '11px' },
    }));
});
// Insérer moisColumnDefs() entre les colonnes "Stock" et "Qté" dans columnDefs.
```

### 🔴 Priorité 1 — Impact immédiat

| # | Tâche | Backend | Frontend |
|---|---|---|---|
| P1.1 | "Mettre au panier" — `TypeSuggession.SEMOIS` | Enum + endpoint `POST /api/semois/mettre-au-panier` | Bouton + bascule onglet + auto-sélect fournisseur |
| P1.2 | Export CSV depuis vue SEMOIS | `GET /api/semois/suggestions/export` | Bouton "Export" dans action bar |
| P1.3 | Dernière commande (date + qté commandée) | Jointure `order_line` dans DTO | Colonne optionnelle dans tableau |
| P1.4 | Tendance VMM | `vmmTendancePct` dans `SemoisCalculationService` | Icône flèche colorée (↑↓) |

### 🟠 Priorité 2 — Amélioration UX

| # | Tâche |
|---|---|
| P2.1 | Barre de progression couverture (frontend only) |
| P2.2 | Simulation "après commande" (frontend only) |
| P2.3 | Exclusion temporaire avec cycle de vie (3 champs SQL + filtre vue + UI) |
| P2.4 | Workflow commander avec choix canal PharmaML |
| P2.5 | Colisage fournisseur (champ + warning + arrondi) |

### 🟡 Priorité 3 — Valeur ajoutée

| # | Tâche |
|---|---|
| P3.1 | Tooltip "Détail calcul SEMOIS" |
| P3.2 | Vue "Produits exclus" dédiée |
| P3.3 | Regroupement par fournisseur dans SEMOIS |
| P3.4 | Colonnes optionnelles (show/hide avec localStorage) |

---

## 10. Tableau comparatif récapitulatif mis à jour

| Fonctionnalité | Pharma-Smart | Winpharma | Pharmagest | LGPI | Alliadis |
|---|:---:|:---:|:---:|:---:|:---:|
| Vue par fournisseur | ✅ | ✅ | ✅ | ❌ | ❌ |
| Vue SEMOIS / ABC criticité | ✅ | ❌ | ✅ | ✅ | ✅ |
| Inline editing qté (fournisseur) | ✅ | ✅ | ✅ | ✅ | ✅ |
| Inline editing qté SEMOIS | ⚙️ via "Mettre au panier" | ✅ (panier) | ✅ (panier) | ✅ (brouillon) | ✅ |
| Consommation mensuelle historique | ✅ | ✅ | ✅ | ✅ | ✅ |
| Dernière commande (date + qté achetée) | ❌ | ✅ | ✅ | ✅ | ❌ |
| Couverture en mois | ✅ | ✅ | ✅ | ❌ | ✅ |
| Barre progression couverture | ❌ | ❌ | ✅ | ❌ | ❌ |
| Simulation "si je commande X" | ❌ | ❌ | ✅ | ❌ | ❌ |
| Tendance VMM (↑↓) | ❌ | ✅ | ❌ | ❌ | ❌ |
| Colisage / qté multiple | ❌ | ✅ | ✅ | ❌ | ❌ |
| Exclusion temporaire (réintroduction auto) | ❌ | ✅ (auto) | ❌ | ✅ (auto) | ❌ |
| Export CSV/Excel (SEMOIS) | ❌ | ✅ | ✅ | ✅ | ✅ |
| Comparaison prix fournisseurs | ✅ (PharmaML) | ❌ | ✅ | ❌ | ✅ |
| Alerte dépassement budget | ✅ | ❌ | ❌ | ❌ | ❌ |
| Tooltip détail calcul SEMOIS | ❌ | ❌ | ✅ | ❌ | ❌ |
| Commande via EDI/PharmaML depuis SEMOIS | ❌ | ✅ | ✅ | ✅ | ✅ |
| Concept "panier" avant validation | ❌ | ✅ | ✅ | ✅ | ✅ |
| Filtre par défaut "à commander" | ❌ (bug) | ✅ | ✅ | ✅ | ✅ |

---

## 12. Analyse — Workflow `suggestionAuto` post-vente

### 12.1 Description du mécanisme actuel

Le déclenchement de la suggestion AUTO est intégré directement dans la sauvegarde des lignes de vente :

```
Caisse → SalesLineServiceImpl.save(salesLines, user, storageId)
    │
    ├── Pour chaque SalesLine :
    │   ├── updateSaleLineLotSold()       → FEFO : débit des lots par date péremption
    │   ├── stockUpdateService.updateStock() → MAJ stock rayon
    │   ├── inventoryTransactionService.save() → journal mouvements
    │   └── collecte QuantitySuggestion(qteSold, stockProduit, produit)
    │
    └── registerSynchronization(afterCommit())
            │
            ↓  (après commit de la transaction vente)
        @Async suggestionProduitService.suggerer(quantitySuggestions, magasin, user)
            │
            ├── 1. Filtre éligibilité produits (etatProduitService.canSuggere)
            ├── 2. Batch-load SemoisConfiguration par produit
            ├── 3. Batch-load VMM (par classe SEMOIS ou global)
            ├── 4. Batch-load SuggestionLine existantes (évite N+1)
            ├── 5. Batch-load pending orders → "stock virtuel" (évite double commande)
            ├── 6. Groupement par fournisseur
            └── 7. seuilReappro() + computeQtyReappro() + saveAll() en batch
```

**Points clés du design :**
- `afterCommit()` garantit que le stock est déjà commité avant de calculer la suggestion
- `@Async` isole le calcul hors du thread de vente → la caisse n'est pas bloquée
- Stock virtuel : `stockPhysique + qtesEnCommande(REQUESTED)` pour éviter de commander en double
- Supporte deux modèles : SEMOIS (`stockObjectifCalcule` du batch) et Classique (`qtySeuilMini`)
- Batch-load systématique : pas de N+1 queries, même pour 100 lignes de vente

### 12.2 Comparaison avec les autres logiciels

| Logiciel | Déclenchement suggestion | Mode | Temps réel ? |
|---|---|---|---|
| **Winpharma** | Batch nocturne (configurable 2h–4h) | Planifié | ❌ |
| **Pharmagest** | Batch quotidien à la clôture de caisse | Planifié + manuel | ❌ |
| **LGPI** | Batch quotidien ou déclenchement manuel | Planifié | ❌ |
| **Alliadis** | Batch nocturne | Planifié | ❌ |
| **SurOrdonnance** | Batch + webhook stock-low event | Événementiel | ⚠️ (quasi-réel) |
| **Pharma-Smart** | `@Async afterCommit` après chaque vente | Événementiel temps réel | ✅ |

> Pharma-Smart est **le seul** parmi les concurrents analysés à mettre à jour les suggestions
> en quasi-temps réel après chaque vente. C'est une approche avancée, mais elle introduit
> des risques techniques spécifiques à maîtriser.

### 12.3 Avantages de l'approche actuelle

| Avantage | Détail |
|---|---|
| **Réactivité** | Le pharmacien voit une suggestion apparaître dans les minutes suivant la vente qui déclenche le seuil |
| **Stock virtuel** | La prise en compte des commandes REQUESTED évite les doublons — meilleure que la plupart des concurrents |
| **Découplage transaction** | `afterCommit()` garantit que la suggestion n'affecte jamais la transaction de vente |
| **Isolation thread** | `@Async` : une erreur dans `suggerer()` ne remonte pas dans le thread caisse |
| **Double modèle** | Supporte SEMOIS (nightly batch) et Classique (seuil statique) en un seul flux |

### 12.4 Risques et faiblesses identifiés

#### ⚠️ R1 — Entités JPA passées cross-thread (anti-pattern)

`QuantitySuggestion(int quantitySold, StockProduit stockProduit, Produit produit)` transporte
des entités JPA détachées vers le thread `@Async`.

Dans `suggerer()`, la ligne suivante accède à une collection potentiellement lazy :

```java
// SuggestionProduitServiceImpl.java ~l.216
int produitAllSock = produit.getStockProduits().stream()   // ← lazy ?
    .filter(s -> s.getStorage().getMagasin().equals(magasin))
    .mapToInt(StockProduit::getTotalStockQuantity)
    .sum();
```

Si `Produit.stockProduits` est `@OneToMany LAZY` et que la session Hibernate de la vente est
fermée avant l'exécution du thread async → `LazyInitializationException`.

> **Mitigation actuelle :** `SuggestionProduitServiceImpl` est `@Service @Transactional`, donc
> le thread `@Async` ouvre sa propre transaction et peut recharger l'entité si nécessaire.
> Mais le `Produit` passé reste **détaché** de cette nouvelle session → risque réel.

**Correction recommandée :** Passer uniquement les IDs dans `QuantitySuggestion` :

```java
// Version robuste
public record QuantitySuggestion(int quantitySold, Integer stockProduitId, Integer produitId) {}
// suggerer() rechargera les entités depuis sa propre session via repositories
```

#### ⚠️ R2 — Race condition sur `SuggestionLine`

Si deux caisses vendent le même produit simultanément → deux threads `@Async` simultanés →
les deux lisent `existingLineByFpId` (ligne de suggestion existante) → les deux tentent une `UPDATE`
sur la même ligne → le dernier gagne, le premier est perdu (lost update).

```
Thread A : lit SuggestionLine (qty=5) → calcule qty=8 → UPDATE qty=8
Thread B : lit SuggestionLine (qty=5) → calcule qty=8 → UPDATE qty=8 (doublon)
```

**Correction recommandée :** Ajouter `@Version` (optimistic locking) sur `SuggestionLine`, ou
utiliser `saveAll()` avec `ON CONFLICT DO UPDATE` en SQL natif.

#### ⚠️ R3 — Absence de gestion d'erreur visible

`@Async` avale silencieusement les exceptions. Si `suggerer()` lève une `RuntimeException`
(DB timeout, lock, etc.), l'erreur est perdue — aucune alerte, aucun retry.

**Correction recommandée :**

```java
@Async
@Override
public void suggerer(List<QuantitySuggestion> quantitySuggestions, Magasin magasin, AppUser user) {
    try {
        doSuggerer(quantitySuggestions, magasin, user);
    } catch (Exception e) {
        LOG.error("Erreur suggestion auto post-vente pour magasin={} : {}", magasin.getId(), e.getMessage(), e);
        // optionnel : alerter via un ApplicationEvent ou stocker en table d'erreurs
    }
}
```

#### ⚠️ R4 — Saturation du thread pool en fort débit

Chaque appel à `save()` (clôture de vente) génère un thread `@Async`. Sur une pharmacie
à fort débit (caisse urgences, promotions), plusieurs dizaines d'appels simultanés peuvent
saturer le pool Spring `TaskExecutor` par défaut (8 threads).

**Correction recommandée :** Configurer un pool dédié avec file d'attente bornée :

```java
// AsyncConfiguration.java
@Bean("suggestionExecutor")
public Executor suggestionExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(500);  // file d'attente bornée
    executor.setThreadNamePrefix("suggestion-auto-");
    executor.setRejectedExecutionHandler(new CallerRunsPolicy()); // pas de perte
    executor.initialize();
    return executor;
}
// Puis @Async("suggestionExecutor") sur suggerer()
```

#### ⚠️ R5 — Calcul déclenché même si stock loin du seuil

Toute vente déclenche `suggerer()`, même pour un produit avec 500 unités en stock et un seuil à 10.
Le filtrage `etatProduitService.canSuggere()` filtre l'éligibilité mais pas la pertinence du stock.

**Amélioration possible :** Pré-filtrer dans `save()` avant de collecter `quantitySuggestions` :

```java
// Avant de collecter — ne déclencher que si stockRayon < seuilAlerte produit
if (stockProduit.getTotalStockQuantity() <= p.getQtySeuilMini() * 2) {
    quantitySuggestions.add(new QuantitySuggestion(...));
}
```

Cela réduit le nombre d'appels `@Async` à ceux qui en ont réellement besoin.

### 12.5 Verdict

| Critère | Évaluation |
|---|---|
| **Approche globale** | ✅ Innovante par rapport aux concurrents, logiquement correcte |
| **`afterCommit` + `@Async`** | ✅ Bonne pratique pour découpler sans bloquer la vente |
| **Stock virtuel** | ✅ Meilleure gestion que Winpharma et Pharmagest |
| **Entités JPA cross-thread** | ⚠️ Anti-pattern — à remplacer par IDs dans `QuantitySuggestion` |
| **Race condition** | ⚠️ Risque faible en officine à caisse unique, réel en multi-caisses |
| **Gestion d'erreur** | ❌ Absente — les échecs sont silencieux |
| **Configuration thread pool** | ❌ Pool par défaut non configuré — risque saturation |

> **Résumé :** L'architecture est correcte et plus avancée que les concurrents.
> Les 3 corrections prioritaires sont : (1) remplacer les entités JPA par des IDs dans `QuantitySuggestion`,
> (2) ajouter un try/catch avec log dans `suggerer()`, (3) configurer un pool dédié `suggestionExecutor`.

---

### 12.6 Analyse perf et ressources — Problèmes identifiés

#### 🔴 PERF-1 — N+1 queries dans la boucle `save()` (critique)

La méthode `save(Set<SalesLine>)` contient une boucle `forEach` qui exécute **des requêtes SQL individuelles pour chaque ligne** :

```
Pour chaque SalesLine (N lignes) :
 ├── 1× SELECT  stockProduitRepository.findOneByProduitIdAndStockageId()
 ├── 1× SELECT  lotService.findByProduitId()
 ├── ─ UPDATE  lotStockLocationService.debit()  (L = nb lots du produit)
 ├── 1× UPDATE  stockUpdateService.updateStock() → StockProduit
 ├── 1× INSERT  salesLineRepository.save()
 └── 1× INSERT  inventoryTransactionService.save()
```

**Total pour une vente de N=10 lignes, L=2 lots en moyenne :**

| Opération | Nb requêtes |
|---|---|
| SELECT StockProduit | 10 |
| SELECT Lots | 10 |
| UPDATE LotStockLocation | 20 |
| UPDATE StockProduit | 10 |
| INSERT SalesLine | 10 |
| INSERT InventoryTransaction | 10 |
| **Total** | **70 requêtes SQL** |

Sans batchs, ces 70 requêtes s'exécutent séquentiellement dans la même transaction.
Avec des batchs (`findAllByProduitIdIn`, `saveAll`, `updateAll`), ce serait **5–6 requêtes**.

---

#### 🟠 PERF-2 — Pool `@Async` configuré mais partagé et surdimensionné vs. Hikari

`AsyncConfiguration.java` est commentée, mais Spring Boot lit **`spring.task.execution`** dans
`application.yml` et auto-configure l'executor sans que la classe Java soit nécessaire :

```yaml
# application.yml
spring:
  task:
    execution:
      thread-name-prefix: pharma-smart-task-
      pool:
        core-size: 2
        max-size: 50          # ← borné, pas Integer.MAX_VALUE
        queue-capacity: 10000  # ← file d'attente bornée à 10 000 tâches
```

✅ Le pool est donc correctement borné. L'analyse initiale (pool illimité) était incorrecte.

**Mais deux problèmes subsistent :**

**Problème A — Pool partagé avec tous les `@Async` de l'application**

Ce pool est le pool **par défaut de l'application entière**. Tous les `@Async` existants
(rapports, emails, autres traitements) l'utilisent. Les suggestions auto entrent en compétition
avec les autres tâches asynchrones. Si un rapport PDF génère 10 threads, il ne reste que 40
threads disponibles pour les suggestions.

**Recommandation :** Dédier un bean `@Bean("suggestionExecutor")` de plus petite taille
et annoter `@Async("suggestionExecutor")` sur `suggerer()` :

```java
@Bean("suggestionExecutor")
public Executor suggestionExecutor() {
    ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
    exec.setCorePoolSize(2);
    exec.setMaxPoolSize(4);          // 4 threads max dédiés suggestions
    exec.setQueueCapacity(500);      // file bornée raisonnable
    exec.setThreadNamePrefix("suggestion-auto-");
    exec.setRejectedExecutionHandler(new CallerRunsPolicy());
    exec.initialize();
    return exec;
}
```

**Problème B — Contention Hikari : 50 threads `@Async` vs. pool DB par défaut**

`max-size: 50` threads peuvent ouvrir simultanément une connexion DB dans `suggerer()`.
Le pool Hikari par défaut (si non configuré explicitement) est **`maximum-pool-size=10`**.

```
50 threads @Async suggerer()
  └── chacun appelle repository.findAll() → 1 connexion DB
      → 50 connexions demandées vs. 10 disponibles
      → 40 threads en attente de connexion
      → timeout HikariCP (30s par défaut)
```

> Si `maximum-pool-size` n'est pas explicitement défini dans `application.yml` ou
> `application-prod.yml`, le risque de timeout Hikari est réel en pic d'activité
> (plusieurs caisses en simultané, fin de journée).

---

#### 🔴 PERF-3 — `LazyInitializationException` en production (confirmé)

`Produit` est chargé dans la session de la transaction vente (thread principal).
Dans le thread `@Async`, c'est une **nouvelle session Hibernate** → `produit` est détaché.
`getStockProduits()` sur une entité détachée → **`LazyInitializationException`**.

**Pourquoi ça ne plante pas systématiquement ?**
`Produit` est annoté `@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)`.
Si la collection `stockProduits` est dans le **cache L2 Hibernate** (Caffeine), Hibernate peut la
servir sans aller en DB. Mais :
- Au démarrage (cache froid) → crash
- Après un éviction de cache → crash
- Sur un nouveau produit jamais encore accédé depuis le démarrage → crash

C'est un **bug silencieux intermittent**, difficile à reproduire en développement.

---

#### 🟠 PERF-4 — Durée des locks DB prolongée par la boucle

La méthode `save()` est `@Transactional`. Toutes les lignes de la boucle sont dans la **même transaction**. Les locks sur les lignes `StockProduit` et `SalesLine` restent actifs pendant toute la durée du forEach.

```
Tx vente ouverte
  ├── lock StockProduit produit A → UPDATE
  ├── lock StockProduit produit B → UPDATE
  ├── ... (N produits)
  └── COMMIT → locks libérés
```

Si une autre caisse vend le **même produit A** pendant ce temps → attente du lock → **caisse bloquée**.
Sur une pharmacie à 3 caisses avec des ventes rapides, ce contention est fréquente en heure de pointe.

---

#### 🟠 PERF-5 — `suggerer()` déclenché pour chaque vente, sans pré-filtrage

Toute vente (même 1 boîte d'un produit ayant 500 unités en stock) déclenche un appel `@Async suggerer()`.

Dans `suggerer()`, le filtre `etatProduitService.canSuggere()` est appliqué en premier, mais il vérifie
l'éligibilité du produit (statut, configuration), **pas si le stock est proche du seuil**.

**Résultat :** Pour une pharmacie vendant 500 produits/jour, 500 appels `@Async` sont déclenchés,
dont la majorité ne produiront aucune suggestion (stock largement > seuil).

**Coût de chaque appel inutile :**
- Chargement SEMOIS configs (batch, OK)
- Chargement VMM (1–5 requêtes SQL selon les classes)
- Chargement lignes existantes (1 requête)
- Chargement pending orders (1 requête)

**→ 3–8 requêtes SQL pour un appel qui ne fera rien.**

---

#### 🟠 PERF-6 — Entités JPA dans `QuantitySuggestion` → mémoire et Lazy chains

```java
public record QuantitySuggestion(int quantitySold, StockProduit stockProduit, Produit produit) {}
```

Chaque `QuantitySuggestion` maintient une référence à l'intégralité du graphe d'objet `Produit`
(avec ses associations lazy — `fournisseurProduits`, `stockProduits`, `rayonProduits`, etc.)
et `StockProduit` (avec `storage`, `magasin`...).

Pour une vente de 20 lignes → 20 graphes d'entités en mémoire jusqu'à la fin de `suggerer()`.
En cas de saturation du thread pool (50 threads en attente), c'est **50 × 20 = 1 000 graphes**
simultanés en heap.

---

#### 📊 Résumé quantifié des impacts

| # | Problème | Impact mesuré (vente 10 lignes) | Sévérité |
|---|---|---|---|
| PERF-1 | N+1 queries en boucle forEach | ~70 req SQL au lieu de ~6 | 🔴 Critique |
| PERF-2A | Pool `@Async` partagé avec toute l'appli (pas de bean dédié) | Compétition suggestions vs. rapports/emails | 🟠 Modéré |
| PERF-2B | 50 threads max vs. pool Hikari (par défaut 10 connexions) | Timeout connexion DB en pic | 🟠 Élevé |
| PERF-3 | `stockProduits` LAZY cross-thread (entité détachée) | `LazyInitializationException` en cache froid | 🔴 Critique |
| PERF-4 | Lock DB ouvert sur toute la boucle forEach | Blocage caisse concurrente | 🟠 Élevé |
| PERF-5 | `@Async suggerer()` pour toute vente sans pré-filtre seuil | 3–8 req SQL inutiles par vente | 🟠 Élevé |
| PERF-6 | Entités JPA complètes dans `QuantitySuggestion` | Pression heap en fort débit | 🟡 Faible |

---

#### ✅ Corrections recommandées

**1. Batch-load tous les StockProduits en une seule requête avant la boucle**

```java
// Avant le forEach, charger tous les StockProduits en batch
Set<Integer> produitIds = salesLines.stream()
    .map(l -> l.getProduit().getId()).collect(Collectors.toSet());
Map<Integer, StockProduit> stockMap = stockProduitRepository
    .findAllByProduitIdInAndStockageId(produitIds, storageId)
    .stream()
    .collect(Collectors.toMap(sp -> sp.getProduit().getId(), Function.identity()));
// Puis dans le forEach : stockMap.get(p.getId()) au lieu de findOneByProduitIdAndStockageId()
```

**2. Dédier un bean `suggestionExecutor` séparé du pool applicatif**

Le pool `spring.task.execution` (core=2, max=50, queue=10000) est **correctement configuré**
dans `application.yml`. `AsyncConfiguration.java` commentée est donc sans impact car Spring Boot
lit directement la configuration YAML.

Le problème restant est que ce pool est **partagé** avec tous les autres `@Async` de l'application
(rapports PDF, emails, etc.). Dédier un pool séparé isole les suggestions :

```java
// AsyncConfiguration.java — décommenter @Configuration et ajouter ce bean
@Bean("suggestionExecutor")
public Executor suggestionExecutor() {
    ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
    exec.setCorePoolSize(2);
    exec.setMaxPoolSize(4);          // 4 threads max dédiés suggestions
    exec.setQueueCapacity(500);      // file bornée raisonnable
    exec.setThreadNamePrefix("suggestion-auto-");
    exec.setRejectedExecutionHandler(new CallerRunsPolicy());
    exec.initialize();
    return exec;
}
// → @Async("suggestionExecutor") sur SuggestionProduitServiceImpl.suggerer()
```

Vérifier aussi que `spring.datasource.hikari.maximum-pool-size` est explicitement défini
dans `application-prod.yml` pour éviter la contention avec 50 threads async potentiels.

**3. Passer des IDs dans `QuantitySuggestion`, pas les entités**

```java
// Remplacement
public record QuantitySuggestion(int quantitySold, Integer stockProduitId, Integer produitId) {}
// Dans suggerer() : recharger les Produits via produitRepository.findAllById(ids)
// → entities dans la nouvelle session, pas de risque LazyInit
```

**4. Pré-filtrer avant de déclencher `@Async`**

```java
// Dans save(), ne collecter que les produits proches du seuil
if (p.getQtySeuilMini() > 0
    && stockProduit.getTotalStockQuantity() <= p.getQtySeuilMini() * 3) {
    quantitySuggestions.add(new QuantitySuggestion(
        salesLine.getQuantityRequested(), stockProduit.getId(), p.getId()));
}
// Si quantitySuggestions est vide → ne pas enregistrer afterCommit du tout
```

---

---

### 12.7 Cohabitation `v_semois_suggestion` / `suggestionAuto` — Analyse

#### Les deux systèmes en présence

| | `v_semois_suggestion` | `suggestionAuto` (`TypeSuggession.AUTO`) |
|---|---|---|
| **Déclenchement** | Vue SQL recalculée à chaque requête | `@Async` après chaque vente |
| **Stockage** | Vue PostgreSQL (lecture seule) | Table `suggestion` + `suggestion_line` |
| **UI** | Onglet "SEMOIS" | Onglet "Par fournisseur" |
| **Formule qté** | `MAX(0, stock_objectif - stock_actuel)` | `MAX(1, stock_objectif - (stock_actuel + pending_qty))` |
| **pending_qty soustrait** | ❌ Non | ✅ Oui (stock virtuel) |
| **Produits couverts** | Produits avec `semois_configuration` | **Tous** les produits ENABLE |
| **Filtre vmm > 0** | ❌ Absent dans la vue actuelle | ✅ Implicite via `vmm > 0` pour SEMOIS |

#### Le problème fondamental : double représentation

Quand le modèle global est **`SEMOIS`** (`APP_MODEL_REAPPRO = SEMOIS`) :

```
Produit A (a une semois_configuration)
    │
    ├──▶ v_semois_suggestion
    │       quantite_a_commander = MAX(0, stock_objectif - stock_actuel)
    │       → ex: MAX(0, 10 - 3) = 7 unités
    │       Visible dans : onglet "SEMOIS"
    │
    └──▶ suggestionAuto (@Async après vente)
            isSemois = true → utilise semoisConfig.stockObjectifCalcule
            qty = MAX(1, stock_objectif - (stock_actuel + pending_qty))
            → ex: MAX(1, 10 - (3 + 2)) = 5 unités (2 en commande REQUESTED)
            Visible dans : onglet "Par fournisseur"
```

**Le même produit apparaît dans les deux onglets avec des quantités différentes (7 vs 5).**

#### Pourquoi les quantités diffèrent

| Cause | Vue SEMOIS | `suggestionAuto` |
|---|---|---|
| Formule de base | `stock_objectif - stock_actuel` | `stock_objectif - stockVirtuel` |
| `pending_qty` (REQUESTED) | **Non soustrait** | **Soustrait** |
| Résultat sur ex. (obj=10, stock=3, pending=2) | **7** | **5** |

La vue sur-estime la quantité à commander car elle ignore les commandes déjà en cours.
`suggestionAuto` est plus précis sur ce point.

#### `canSuggere()` n'empêche pas la duplication

```java
// EtatProduitServiceImpl.java
public boolean canSuggere(Integer idProduit) {
    return getCommandeCount(idProduit, OrderStatut.REQUESTED) == 0   // commande en cours ?
        && getCommandeCount(idProduit, OrderStatut.RECEIVED) == 0;   // livraison en cours ?
}
```

Ce filtre vérifie uniquement les **commandes formelles** (`Commande`).
Il **ne vérifie pas** si le produit est déjà dans `v_semois_suggestion`.
→ Un produit avec `semois_configuration` passe `canSuggere()` → reçoit une `Suggestion(AUTO)` → apparaît dans les deux onglets.

#### Problème supplémentaire : la vue actuelle n'a pas le filtre `vmm > 0`

La vue en production (fournie) n'a **pas** `AND COALESCE(sc.vmm_calcule, 0) > 0` dans son WHERE.
Résultat : tous les produits `ENABLE` non `DETAIL` apparaissent, même ceux sans `semois_configuration`
avec `quantite_a_commander = 0`.

→ C'est la **vraie cause** du bug de pagination : la vue renvoie plus de lignes que prévu
(tous les produits, pas seulement ceux à commander), et le filtre Java `.filter(dto -> dto.quantiteACommander() > 0)`
en supprime une partie après pagination → `totalElements` faux.

#### Comment font les autres logiciels ?

**Tous les concurrents utilisent UN seul système de suggestion**, jamais deux en parallèle :

| Logiciel | Mécanisme unique |
|---|---|
| **Winpharma** | Batch SEMOIS nocturne → `panier_reappro`. Zéro suggestion temps réel à la vente. |
| **Pharmagest** | Batch quotidien → `proposition_commande`. En mode SEMOIS, la vente ne génère RIEN. |
| **LGPI** | Batch → `suggestion_reappro`. Idem. |
| **Alliadis** | Batch nocturne uniquement. |
| **SurOrdonnance** | ML batch + alertes temps réel (1 seul système, pas 2 en parallèle). |

**Aucun logiciel de référence n'a deux calculs de suggestion qui tournent simultanément pour le même produit.**

#### Les 3 options architecturales

**Option A — Exclusion mutuelle stricte** ✅ Recommandée

Quand un produit a une `semois_configuration`, `suggestionAuto` le **saute** :

```java
// Dans suggerer() — après le filtre canSuggere()
List<QuantitySuggestion> eligibles = quantitySuggestions.stream()
    .filter(q -> etatProduitService.canSuggere(q.produit().getId()))
    // ← AJOUT : si mode SEMOIS, sauter les produits déjà gérés par la vue SEMOIS
    .filter(q -> !isSemois || semoisConfigByProduitId.get(q.produit().getId()) == null)
    .toList();
```

- Produits avec `semois_configuration` → **SEMOIS tab uniquement**
- Produits sans `semois_configuration` → **Par fournisseur tab uniquement** (AUTO classique)
- Zéro duplication, zéro incohérence de quantité

**Option B — "Mettre au panier" comme seul pont** (cf. Section 3.3)

`v_semois_suggestion` reste purement en lecture. `suggestionAuto` continue pour les produits sans config SEMOIS. Le pharmacien "met au panier" manuellement depuis la vue SEMOIS → crée un `Suggestion(SEMOIS)` dans la table → traité par "Par fournisseur".

**Option C — Désactivation de `suggestionAuto` en mode SEMOIS**

Quand `APP_MODEL_REAPPRO = SEMOIS`, ne pas appeler `suggestionProduitService.suggerer()` du tout.
`SalesLineServiceImpl.save()` n'enregistre pas d'`afterCommit`. La suggestion est entièrement gérée par la vue et le batch SEMOIS.

```java
// Dans save() — SalesLineServiceImpl
if (appConfigurationService.getCurrentModelReappro() != ModelReapprovisionnement.SEMOIS) {
    TransactionSynchronizationManager.registerSynchronization(...suggerer()...);
}
```

---

#### Verdict cohabitation

| Question | Réponse |
|---|---|
| Est-ce normal d'avoir les deux ? | ❌ Non — aucun logiciel de référence ne fait ça |
| Est-ce que ça pose un vrai problème ? | ✅ Oui — même produit visible dans 2 onglets avec 2 quantités différentes |
| La vue actuelle est-elle correcte ? | ⚠️ Partiellement — formule OK mais manque `vmm > 0` et `pending_qty` |
| Solution la plus simple | Option A — 3 lignes de code dans `suggerer()` |
| Solution la plus robuste | Option B — "Mettre au panier" + Option A combinés |



### Points forts uniques de Pharma-Smart
- Double entrée (fournisseur + SEMOIS) : combinaison unique sur le marché
- Intégration PharmaML pour disponibilité/prix grossiste en temps réel
- Alerte budget mensuel : absent chez tous les concurrents
- Labels métier explicites (Rupture / Sous seuil / Suffisant)
- Consommation mensuelle historique déjà présente

### Déficits critiques à corriger en priorité

| Priorité | Déficit | Impact |
|---|---|---|
| 🔴🔴 P0.1 | `consommationMensuelle` mappée mais zéro `ColDef` AG Grid dans `suggestion-produit-panel` | Donnée existante invisible |
| 🔴🔴 P0.2 | Bug pagination SEMOIS : `.filter()` Java → `totalElements` faux (cause racine : vue sans filtre `vmm > 0`) | Pagination inexploitable |
| 🔴🔴 P0.3 | **Cohabitation** — même produit dans 2 onglets avec 2 quantités différentes (vue ignore `pending_qty`) | Double commande possible |
| 🔴🔴 P0.4 | **Vue actuelle sans filtre `vmm > 0`** — tous les produits ENABLE apparaissent | Cause racine du bug P0.2 |
| 🔴 P1.1 | **N+1 queries** dans `save()` forEach — ~70 SQL au lieu de ~6 par vente | Perf caisse dégradée |
| 🔴 P1.2 | Pool `@Async` partagé + contention Hikari possible (`max-size: 50` vs. pool DB) | Timeout DB en pic |
| 🔴 P1.3 | **`LazyInitializationException`** — `produit.getStockProduits()` LAZY cross-thread | Crash intermittent cache froid |
| 🔴 P1.4 | `suggerer()` : gestion d'erreur absente (`@Async` avale les exceptions) | Échecs silencieux en prod |
| 🔴 P1.5 | `QuantitySuggestion` transporte des entités JPA cross-thread | Anti-pattern, mémoire & Lazy |
| 🔴 P1.6 | Inline editing SEMOIS → "Mettre au panier" (`TypeSuggession.SEMOIS`) | Bloque efficacité quotidienne |
| 🟠 P2.1 | Locks DB ouverts sur toute la boucle forEach — blocage multi-caisses | Caisse bloquée en contention |
| 🟠 P2.2 | `@Async suggerer()` déclenché pour toute vente sans pré-filtre seuil | 3–8 req SQL inutiles / vente |
| 🟠 P2.3 | Race condition sur `SuggestionLine` en multi-caisses | Lost-update possible |
| 🟠 P2.4 | Exclusion temporaire sans cycle de vie complet | Feature incomplète |
| 🟠 P2.5 | Commande SEMOIS sans canal PharmaML intégré | Duplication de saisie |

---

## 13. Recommandation définitive — Quel système de suggestion conserver ?

### 13.1 Vue de la situation actuelle

La vue SQL réelle en production (fournie) confirme les trois problèmes :

```sql
-- Vue actuelle — 3 défauts confirmés
GREATEST(0, stock_objectif_calcule - stock_actuel)   -- (1) pas de pending_qty
WHERE p.status = 'ENABLE' AND p.type_produit <> 'DETAIL'  -- (2) pas de vmm > 0
-- (3) pas de HAVING → quantite_a_commander = 0 visible
```

Les deux systèmes coexistent pour les produits avec `semois_configuration` en mode SEMOIS.
Ce n'est pas une feature, c'est une **accumulation progressive** : le module SEMOIS a été ajouté
sans désactiver l'ancien `suggestionAuto` pour les produits qu'il couvre désormais.

---

### 13.2 Ce que font les experts et les logiciels de référence

| Logiciel | Système unique ? | Déclencheur | Ce qui est recommandé |
|---|---|---|---|
| **Winpharma** | ✅ Un seul | Batch nocturne | Batch SEMOIS → panier → commande |
| **Pharmagest** | ✅ Un seul | Batch daily | Idem, en mode SEMOIS la vente ne génère rien |
| **LGPI** | ✅ Un seul | Batch | Idem |
| **Alliadis** | ✅ Un seul | Batch | Idem |
| **SurOrdonnance** | ✅ Un seul (ML) | Événementiel | Batch ML + alertes push (pas de double calcul) |
| **Experts officine (UTIP, Ordre des pharmaciens)** | — | — | Réassort basé sur VMM + délai, décision du pharmacien |

**Consensus expert :** Le réapprovisionnement doit reposer sur **un seul calcul de référence**,
visible dans un seul endroit, avec une décision explicite du pharmacien avant toute commande.
Les systèmes qui génèrent des suggestions automatiques à chaque vente sont considérés comme
**intrusifs** et créateurs de "bruit" (trop de suggestions → pharmacien les ignore toutes).

---

### 13.3 Analyse comparative des deux systèmes

#### `v_semois_suggestion` (vue SEMOIS)

| Critère | Évaluation |
|---|---|
| **Précision du calcul** | ✅ Temps réel — stock recalculé à chaque consultation |
| **Richesse du modèle** | ✅ VMM × classe × délai livraison × coefficient sécurité |
| **pending_qty** | ❌ Non pris en compte (quantité surestimée) |
| **Filtre vmm > 0** | ❌ Absent — tous les produits apparaissent |
| **Écriture possible** | ❌ Immutable — inline editing impossible directement |
| **Décision pharmacien** | ✅ Explicite — "Mettre au panier" → modifier → commander |
| **Scalabilité** | ✅ Pas de stockage, recalcul à la demande |
| **Alignement experts** | ✅ Pattern identique à Winpharma/Pharmagest |

#### `suggestionAuto` (post-vente, TypeSuggession.AUTO)

| Critère | Évaluation |
|---|---|
| **Précision du calcul** | ✅ Stock virtuel (pending_qty soustrait) — plus précis que la vue |
| **Richesse du modèle** | ✅ Identique (utilise `semoisConfig` si disponible) |
| **Déclenchement** | ⚠️ Après chaque vente → "bruit" — la plupart des déclenchements sont inutiles |
| **Écriture possible** | ✅ Crée des `SuggestionLine` éditables inline |
| **Décision pharmacien** | ⚠️ Semi-automatique — le pharmacien retrouve une suggestion déjà formée |
| **Scalabilité** | ❌ N+1 queries, thread @Async, entités JPA cross-thread |
| **Alignement experts** | ❌ Aucun concurrent ne fait ça en parallèle avec SEMOIS |
| **Cohabitation SEMOIS** | ❌ Double représentation, quantités différentes |

---

### 13.4 Recommandation définitive

#### Principe directeur

> **Un produit ne peut être géré que par un seul système à la fois.**
> Le choix du système dépend de si le produit a une `semois_configuration`.

#### Architecture cible (3 cas)

```
┌─────────────────────────────────────────────────────────┐
│  Mode APP = SEMOIS                                       │
│                                                         │
│  Produit WITH semois_configuration                      │
│  └── SEMOIS tab (v_semois_suggestion)    ← SEUL système │
│       → "Mettre au panier"                              │
│       → Suggestion(SEMOIS)                              │
│       → inline edit → Commander                         │
│  suggestionAuto : SKIP ces produits ←── 3 lignes de code│
│                                                         │
│  Produit WITHOUT semois_configuration                   │
│  └── suggestionAuto (fallback classique) ← SEUL système │
│       → Suggestion(AUTO) dans "Par fournisseur"         │
├─────────────────────────────────────────────────────────┤
│  Mode APP = CLASSIQUE                                   │
│                                                         │
│  Tous les produits                                      │
│  └── suggestionAuto (qtySeuilMini)       ← SEUL système │
│  v_semois_suggestion : inutilisée (données à 0)        │
└─────────────────────────────────────────────────────────┘
```

#### Ce qui se passe concrètement pour chaque scénario

| Scénario | Système actif | Action pharmacien |
|---|---|---|
| Mode SEMOIS + produit configuré | **Vue SEMOIS uniquement** | Consulte SEMOIS tab → Met au panier → Edite → Commande |
| Mode SEMOIS + produit non configuré | **suggestionAuto** (fallback) | Retrouve suggestion dans "Par fournisseur" → Commande |
| Mode CLASSIQUE | **suggestionAuto** | Retrouve suggestion dans "Par fournisseur" → Commande |

#### Modifications nécessaires

**1. Modification mineure dans `SuggestionProduitServiceImpl.suggerer()` — 3 lignes**

```java
// Après le filtre canSuggere(), ajouter :
List<QuantitySuggestion> eligibles = quantitySuggestions.stream()
    .filter(q -> etatProduitService.canSuggere(q.produit().getId()))
    // ← NOUVEAU : en mode SEMOIS, les produits avec config sont gérés par la vue
    .filter(q -> !isSemois
                 || semoisConfigByProduitId.get(q.produit().getId()) == null)
    .toList();
```

**2. Migration SQL `V1.3.9` — Corriger la vue (3 corrections)**

```sql
CREATE OR REPLACE VIEW v_semois_suggestion AS
---

## 13. Décision finale — Architecture cible

### 13.1 Principe acté

> **Un seul système de suggestion : `v_semois_suggestion` (enrichie) + `Suggestion` comme panier.**
> `suggestionAuto` (`suggerer()`) est **décommissionné**.

Ce choix est aligné avec **Winpharma**, **Pharmagest**, **LGPI** et **Alliadis** :
aucun de ces logiciels ne génère de suggestions automatiquement à chaque vente.
La suggestion est une **décision analytique** du pharmacien, pas un effet de bord d'une vente.

---

### 13.2 Les deux rôles clairement séparés

| Composant | Rôle | Type de données | Écriture ? |
|---|---|---|---|
| `v_semois_suggestion` | **Source de vérité** — calcul temps réel des besoins | Vue SQL (lecture seule) | ❌ Non |
| `Suggestion` + `SuggestionLine` | **Panier de commande** — sélection du pharmacien | Table persistée | ✅ Oui |

La vue calcule. La table décide. Le pharmacien fait le pont entre les deux.

---

### 13.3 Décommissionnement de `suggestionAuto`

#### Backend — `SalesLineServiceImpl.java`

Supprimer l'enregistrement du synchronization callback :

```java
// AVANT — à supprimer entièrement
Magasin magasin = user.getMagasin();
List<QuantitySuggestion> suggestions = Collections.unmodifiableList(quantitySuggestions);
TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    @Override
    public void afterCommit() {
        suggestionProduitService.suggerer(suggestions, magasin, user);  // ← SUPPRIMÉ
    }
});

// APRÈS — méthode allégée, la liste quantitySuggestions n'est plus nécessaire
@Override
public void save(Set<SalesLine> salesLines, AppUser user, Integer storageId) {
    if (CollectionUtils.isEmpty(salesLines)) return;
    salesLines.forEach(salesLine -> {
        Produit p = salesLine.getProduit();
        StockProduit stockProduit = stockProduitRepository
            .findOneByProduitIdAndStockageId(p.getId(), storageId);
        updateSaleLineLotSold(salesLine, stockProduit.getStorage());
        save(salesLine, stockProduit);
        this.inventoryTransactionService.save(salesLine);
    });
}
```

#### Backend — `SuggestionProduitService` + `SuggestionProduitServiceImpl`

```java
// Marquer @Deprecated — ne plus appeler
@Deprecated(since = "2026-03", forRemoval = true)
void suggerer(List<QuantitySuggestion> quantitySuggestions, Magasin magasin, AppUser user);
```

Ne pas supprimer immédiatement : les anciennes `Suggestion(AUTO)` existantes en base doivent
pouvoir être consultées et clôturées proprement. Suppression définitive après migration des données.

#### `TypeSuggession` — Clarification des types

```java
public enum TypeSuggession {
    SEMOIS,    // ← type principal : panier créé depuis la vue SEMOIS (nouveau workflow)
    MANUELLE,  // ← conservé : ajout manuel d'un produit au panier
    AUTO       // ← @Deprecated : plus créé, conservé pour compatibilité données existantes
}
```

#### Impact sur `QuantitySuggestion`

Le record `QuantitySuggestion` n'est plus utilisé. Il peut être supprimé (ou conservé le temps
de la migration).

---

### 13.4 Nouveau workflow — Pattern Winpharma

```
┌─────────────────────────────────────────────────────────────────────┐
│ ÉTAPE 1 — "Réapprovisionnement" (onglet principal, anciennement "SEMOIS")    │
│                                                                     │
│  Vue v_semois_suggestion (lecture seule, temps réel)                │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ Filtre : Fournisseur | Classe | Urgence | Recherche         │   │
│  │ ┌──────┬──────────────┬─────┬──────┬──────┬──────────────┐ │   │
│  │ │ ☑ │ Produit       │VMM │Stock│Besoin│ Couv.        │ │   │
│  │ │ ☑ │ AMOXICILLINE  │ 12 │  1  │  11  │ ▓░░░ 0,1m   │ │   │
│  │ │ ☐ │ PARACETAMOL   │  8 │  5  │   3  │ ▓▓░░ 0,6m   │ │   │
│  │ └──────┴──────────────┴─────┴──────┴──────┴──────────────┘ │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  [Sélectionner tout urgent]  [➕ Ajouter au panier (N)]             │
└─────────────────────────────────────────────────────────────────────┘
                          │
                          ▼  POST /api/semois/mettre-au-panier
                          │  → crée Suggestion(type=SEMOIS) par fournisseur
                          │  → bascule automatiquement sur onglet "Panier"
                          ▼
┌─────────────────────────────────────────────────────────────────────┐
│ ÉTAPE 2 — "Panier" (anciennement "Par fournisseur")                 │
│                                                                     │
│  Split panel : liste fournisseurs (gauche) | lignes éditables (droite) │
│  ┌───────────────────┐  ┌──────────────────────────────────────────┐│
│  │ ● Pharmalab   🔴3 │  │ Produit  │Stock│ VMM │ Qté suggérée  │  ││
│  │   COOPER      🟠1 │  │ AMOXI... │  1  │ 12  │ [  11  ] ▲▼  │  ││
│  │   SANOFI         │  │ DOLIPRANE│  5  │  8  │ [   3  ] ▲▼  │  ││
│  └───────────────────┘  └──────────────────────────────────────────┘│
│                                                                     │
│  [✅ Valider]  [🛒 Commander tout]  [📋 PDF]  [📊 CSV]              │
└─────────────────────────────────────────────────────────────────────┘
                          │
                          ▼  POST /api/suggestions/{id}/commander
                          │  → crée Commande(REQUESTED) par fournisseur
                          │  → supprime la Suggestion du panier
                          ▼
┌─────────────────────────────────────────────────────────────────────┐
│ ÉTAPE 3 — Suivi commandes (module Commandes existant)               │
└─────────────────────────────────────────────────────────────────────┘
```

---

### 13.5 Renommage des onglets — Propositions et choix

#### Benchmark des libellés chez les concurrents

| Logiciel | Onglet principal (analytique) | Onglet secondaire (panier) |
|---|---|---|
| **Winpharma** | "Réapprovisionnement" | "Panier" |
| **Pharmagest** | "Propositions d'achat" | "Commande en cours" |
| **LGPI** | "Analyse des besoins" | "Prêt à commander" |
| **Alliadis** | "Tableau de réassort" | "Commandes à passer" |
| **SurOrdonnance** | "Besoins stock" | "À commander" |

#### Options proposées pour Pharma-Smart

| Option | Tab 1 (vue SEMOIS) | Tab 2 (panier) | Tonalité |
|---|---|---|---|
| **A** | "Réapprovisionnement" | "Panier" | Winpharma — standard officine |
| **B** | "Analyse des besoins" | "Prêt à commander" | LGPI — plus explicite |
| **C** | "Besoins SEMOIS" | "Panier de commande" | Technique mais précis |
| **D** | "Réassort" | "Panier" | Court, moderne |

#### Choix recommandé — Option A (alignée Winpharma)

```
Tab 1 : "Réapprovisionnement"   icon: pi pi-boxes
         Sous-titre : "Produits à réapprovisionner — calculé en temps réel"

Tab 2 : "Panier"                icon: pi pi-shopping-cart
         Sous-titre : "Articles sélectionnés — modifier et commander"
         Badge : nombre d'articles dans le panier (nb Suggestion actives)
```

**Justification :** "Réapprovisionnement" est le terme universel en officine française.
"Panier" est immédiatement compris par tout pharmacien habitué au web. Court, sans ambiguïté.

---

### 13.6 Modifications frontend — `command-common.service.ts`

```typescript
// AVANT
export type SuggestionsSource = 'FOURNISSEURS' | 'SEMOIS';
suggestionsActiveSource = signal<SuggestionsSource>('FOURNISSEURS');

// APRÈS
export type SuggestionsSource = 'REAPPRO' | 'PANIER';
suggestionsActiveSource = signal<SuggestionsSource>('REAPPRO'); // ← REAPPRO en premier
```

#### `suggestions-unified.component.html` — Ordre et libellés des onglets

```html
<!-- Tab 1 : REAPPRO (anciennement SEMOIS) — devient l'onglet par défaut -->
<button class="su-tab" [class.su-tab--active]="activeSource() === 'REAPPRO'"
        (click)="setSource('REAPPRO')">
  <i class="pi pi-boxes"></i>
  Réapprovisionnement
</button>

<!-- Tab 2 : PANIER (anciennement FOURNISSEURS) -->
<button class="su-tab" [class.su-tab--active]="activeSource() === 'PANIER'"
        (click)="setSource('PANIER')">
  <i class="pi pi-shopping-cart"></i>
  Panier
  @if (panierCount() > 0) {
    <span class="su-tab-badge">{{ panierCount() }}</span>
  }
</button>
```

#### Navigation après "Ajouter au panier"

```typescript
// CommandCommonService — méthode à renommer
navigateToPanier(): void {
  this.suggestionsActiveSource.set('PANIER');
  this.commandPreviousActiveNav.set('SUGGESTIONS');
}
// Remplace navigateToSemoisSuggestions()
```

---

### 13.7 Plan de migration des données existantes

Les `Suggestion(TypeSuggession.AUTO)` existantes en base ne doivent pas être supprimées brutalement.

```sql
-- Migration optionnelle : convertir les suggestions AUTO en SEMOIS ou les archiver
-- Option 1 : les laisser telles quelles (elles seront visibles dans "Panier" avec type AUTO)
-- Option 2 : les supprimer si > X jours (appliquer la rétention existante)
-- La rétention est déjà configurée via APP_SUGGESTION_RETENTION dans AppConfigurationService
```

Le `TypeSuggession.AUTO` reste lisible en base. L'interface l'affiche normalement dans "Panier".
Aucune migration forcée nécessaire — l'arrêt de la création suffit.

---

## 14. Priorités ajustées — Plan d'implémentation final

### 🔴🔴 Sprint 0 — Correction bloquante (1–2 jours)

| # | Action | Fichier | Effort |
|---|---|---|---|
| **S0.1** | **Décommissionner `suggestionAuto`** : supprimer `registerSynchronization` + `@Async suggerer()` dans `SalesLineServiceImpl` | `SalesLineServiceImpl.java` | 🟢 30 min |
| **S0.2** | **Corriger la vue SQL** : ajouter `vmm > 0` + `HAVING` + `pending_qty` → migration `V1.3.9` | `v_semois_suggestion` | 🟡 2h |
| **S0.3** | **Fix bug pagination** : supprimer `.filter(dto -> dto.quantiteACommander() > 0)` Java (rendu inutile par S0.2) | `SemoisCalculationService.java` | 🟢 15 min |
| **S0.4** | **`consommationMensuelle`** : ajouter `ColDef` dynamiques dans AG Grid | `suggestion-produit-panel.component.ts` | 🟡 1h |

### 🔴 Sprint 1 — Nouveau workflow panier (3–5 jours)

| # | Action | Fichier | Effort |
|---|---|---|---|
| **S1.1** | `TypeSuggession.SEMOIS` dans l'enum | `TypeSuggession.java` | 🟢 5 min |
| **S1.2** | Endpoint `POST /api/semois/mettre-au-panier` | `SemoisResource.java` + service | 🟡 3h |
| **S1.3** | Renommer `SuggestionsSource` : `REAPPRO` / `PANIER`, inverser l'ordre des onglets | `command-common.service.ts` + `suggestions-unified` | 🟡 1h |
| **S1.4** | Bouton "Ajouter au panier" dans `semois-suggestions` + navigation auto | `semois-suggestions.component` | 🟡 2h |
| **S1.5** | Badge panier dans l'onglet "Panier" (compteur suggestions actives) | `suggestions-unified.component` | 🟢 30 min |

### 🟠 Sprint 2 — Enrichissement UX (3–4 jours)

| # | Action |
|---|---|
| **S2.1** | Export CSV depuis vue SEMOIS (`GET /api/semois/suggestions/export`) |
| **S2.2** | Dernière commande passée (date + qté) dans le tableau SEMOIS |
| **S2.3** | Tendance VMM (↑↓) |
| **S2.4** | Simulation "après commande → couverture = X mois" (frontend only) |
| **S2.5** | Workflow commander avec choix canal PharmaML |

### 🟡 Sprint 3 — Qualité technique (2–3 jours)

| # | Action |
|---|---|
| **S3.1** | Batch-load `StockProduit` dans `save()` (N+1 → 1 requête) |
| **S3.2** | `QuantitySuggestion` passer des IDs au lieu des entités JPA |
| **S3.3** | `try/catch` + log dans `suggerer()` (avant sa suppression définitive) |
| **S3.4** | Bean `@Bean("suggestionExecutor")` dédié dans `AsyncConfiguration` |

### 🟡 Sprint 4 — Features avancées

| # | Action |
|---|---|
| **S4.1** | Exclusion temporaire d'un produit (cycle de vie complet) |
| **S4.2** | Colisage fournisseur (quantité minimale commande) |
| **S4.3** | Tooltip "Détail calcul SEMOIS" par ligne |
| **S4.4** | Barre de progression couverture actuelle / cible |

---

### 14.1 Tableau de bord des déficits — Version finale

| Priorité | # | Déficit / Action | Sprint | Effort |
|---|---|---|---|---|
| 🔴🔴 | S0.1 | Décommissionner `suggestionAuto` (supprimer `registerSynchronization`) | 0 | 30 min |
| 🔴🔴 | S0.2 | Migration `V1.3.9` : vue SQL avec `vmm > 0` + `HAVING` + `pending_qty` | 0 | 2h |
| 🔴🔴 | S0.3 | Supprimer `.filter()` Java post-pagination dans `getAllSuggestions()` | 0 | 15 min |
| 🔴🔴 | S0.4 | `consommationMensuelle` : ColDef dynamiques AG Grid | 0 | 1h |
| 🔴 | S1.1–1.5 | Nouveau workflow "Réapprovisionnement → Panier" complet | 1 | 3–5j |
| 🟠 | S2.1–2.5 | Enrichissement UX SEMOIS | 2 | 3–4j |
| 🟡 | S3.1–3.4 | Qualité technique (N+1, LazyInit, thread pool) | 3 | 2–3j |
| 🟡 | S4.1–4.4 | Features avancées (exclusion, colisage, tooltip) | 4 | — |

---

*Document v9 — Décision finale actée : un seul système (v_semois_suggestion enrichie + Suggestion = panier).
Analyse basée sur le code source (`SalesLineServiceImpl.java`, `SuggestionProduitServiceImpl.java`,
`SemoisCalculationService.java`, `v_semois_suggestion` SQL réel, `Suggestion.java`, `TypeSuggession.java`,
`command-common.service.ts`, `suggestions-unified.component.ts`)
et comparaison avec Winpharma, Pharmagest Interactive, LGPI, Alliadis, SurOrdonnance.*

---

## 15. Révision de la décision v9 → v11 — Batch crée les Suggestions + contraintes réelles

> **Contexte :** La décision v9 acte l'usage de `v_semois_suggestion` comme source de vérité et
> un workflow manuel "Mettre au panier" → `Suggestion`. Cette section analyse si un **batch
> périodique qui crée/met à jour directement les `Suggestion`** est une meilleure approche,
> et redéfinit les priorités en conséquence.

---

### 15.1 Ce que le code révèle réellement — 3 couches qui se chevauchent

Contrairement à ce que décrit la v9, la lecture du code montre **3 couches** distinctes :

```
COUCHE 1 — mv_semois_suggestion (vue MATÉRIALISÉE, non une vue ordinaire)
  SQL : calcule VMM par sous-requête corrélée 3× dupliquée depuis ventes_mensuelles_agregees
  Lit : semois_configuration (delai, coefficient) MAIS recalcule VMM lui-même
  Problème : VMM de la MV ≠ VMM calculée par SemoisCalculationService (formule différente)

COUCHE 2 — SemoisCalculationService (@Scheduled cron)
  Calcule : VMM (excluant mois de rupture), marge, stockObjectif
  Stocke  : semois_configuration.vmm_calcule, stock_objectif_calcule, marge_securite_calcule
  NE fait PAS : créer des Suggestion / SuggestionLine

COUCHE 3 — SuggestionProduitServiceImpl.suggerer() (@Async, post-vente)
  Lit     : semois_configuration (si mode SEMOIS)
  Crée    : Suggestion(AUTO) + SuggestionLine par fournisseur
  À décommissionner (décision v9)
```

**Incohérence critique identifiée :** La VMM affichée dans le tableau SEMOIS (depuis la MV)
est différente de la VMM stockée dans `semois_configuration.vmm_calcule` (calculée par le batch).
Raison : la MV n'exclut pas les mois de rupture fournisseur ; le batch oui.
→ Le pharmacien voit 2 valeurs VMM différentes selon l'écran. **Non acceptable.**

**Incohérence supplémentaire :** `createCommandesFromSemois()` dans `SuggestionProduitServiceImpl`
crée déjà une `Commande` DIRECTEMENT depuis les lignes SEMOIS **sans passer par `Suggestion`**.
Cela contredit le workflow v9 "vue → panier → commander".

---

### 15.2 Analyse de l'Option A (décision v9) — Vue SEMOIS + "Mettre au panier" manuel

#### Workflow

```
Pharmacien ouvre "Réapprovisionnement"
  → Lit mv_semois_suggestion (MV, données potentiellement périmées et VMM ≠ batch)
  → Sélectionne manuellement N produits urgents
  → Clique "Mettre au panier"
  → POST /api/semois/mettre-au-panier → crée Suggestion(SEMOIS) par fournisseur
  → Bascule sur "Panier"
  → Ajuste quantités → Commander
```

#### Forces

| Force | Détail |
|---|---|
| Décision explicite du pharmacien | Il choisit CE qu'il met au panier (contrôle total) |
| Pas de "pollution" du panier | Seuls les produits explicitement sélectionnés entrent |
| Stock affiché en temps réel | La MV lit `stock_produit` à chaque consultation (si non matérialisé) |

#### Faiblesses identifiées dans le code

| Faiblesse | Impact |
|---|---|
| **MV recalcule VMM indépendamment du batch** — formule différente (sans exclusion ruptures) | VMM affiché ≠ VMM stocké → incohérence visible pharmacien |
| **Sous-requête VMM 3× dupliquée** dans la MV — `GREATEST(0, stock_objectif - stock_actuel)` recalcule le même CTE 3 fois | Perf REFRESH : ~6 000 sous-requêtes pour 2 000 produits |
| **`mv_semois_suggestion` sans filtre `vmm > 0`** | Tous les produits ENABLE apparaissent (bug pagination) |
| **`pending_qty` absent de la MV** | Quantité surestimée → double commande possible |
| **Workflow 2 étapes** : vue analytique → puis panier | Friction : pharmacien doit "mettre au panier" avant de pouvoir ajuster/commander |
| **`createCommandesFromSemois()` court-circuite le panier** | Incohérence dans le code : 2 chemins vers la `Commande` |
| **Migration `V1.3.9` nécessaire** pour corriger la MV | Investissement SQL pour un composant qui peut être éliminé |

---

### 15.3 Analyse de l'Option B — Batch crée/met à jour les `Suggestion`

#### Principe

Le `SemoisCalculationService` (batch cron nocturne), **après avoir calculé et stocké les données
dans `semois_configuration`**, crée ou met à jour automatiquement les `Suggestion(SEMOIS)` et
`SuggestionLine` par fournisseur.

Le pharmacien arrive le matin, le panier est déjà préparé. Il n'a pas à "mettre au panier".
Il ajuste les quantités si besoin et commande.

C'est exactement **le pattern Winpharma, Pharmagest, LGPI, Alliadis** :
> *"Le batch nocturne peuple le panier. Le pharmacien valide."*

#### Workflow

```
[NUIT — AUTOMATIQUE]
SemoisCalculationService.doRecalculateAllConfigurations()
  → Calcule VMM, marge, stockObjectif pour chaque produit
  → Stocke dans semois_configuration (comme aujourd'hui)
  → NOUVEAU : appelle mettreAuPanierBatch()
       • Pour chaque produit avec quantiteACommander > 0 (après pendingQty)
       • Regroupe par fournisseur
       • Crée Suggestion(type=SEMOIS, statut=GENEREE) si inexistante par fournisseur
       • Crée/met à jour SuggestionLine (protège les qtés modifiées manuellement)
       • Supprime les SuggestionLine des produits dont qteACommander = 0

[MATIN — PHARMACIEN]
Ouvre "Panier"
  → Trouve le panier déjà préparé, groupé par fournisseur
  → Ajuste les quantités si besoin
  → Commande
```

#### Forces

| Force | Détail |
|---|---|
| **Une seule source de vérité** | VMM dans `semois_configuration.vmm_calcule` → utilisée partout (panier + fiche produit) |
| **`pending_qty` intégré nativement** | Le batch a déjà `orderLineRepository.findPendingQtyByProduitIds()` |
| **Exclusion mois de rupture** | `calculateVMM()` l'inclut déjà — profite au panier |
| **0 friction UX** | Panier pré-peuplé chaque matin sans action du pharmacien |
| **`mv_semois_suggestion` simplifiée** | N'est plus nécessaire pour la liste principale ; reste optionnellement pour le dashboard stats uniquement |
| **`createCommandesFromSemois()` devient inutile** | Un seul chemin : `Suggestion` → `Commande` |
| **Code existant réutilisé** | `suggerer()` a déjà 95% de la logique (grouper par fournisseur, créer `Suggestion`/`SuggestionLine`) |
| **Pas de migration SQL V1.3.9 complexe** | La MV peut être droppée proprement |
| **Scalable** | Pour une officine ~2 000 produits, le batch tourne en < 2 min |

#### Faiblesses

| Faiblesse | Mitigation |
|---|---|
| **Panier potentiellement périmé** dans la journée (stock change après le batch) | Le `stock_actuel` est lu en temps réel au moment de l'affichage dans le panier (jointure `stock_produit`) |
| **Ajustements manuels écrasés** au prochain batch | Protection : si `suggestionLine.quantiteModifieeManuel = true`, le batch ne touche pas la ligne |
| **Panier pré-peuplé peut sembler "automatique"** | UX : afficher la date de génération et un bouton "Actualiser maintenant" |

---

### 15.4 Comparaison directe

| Critère | Option A — Vue + manuel | Option B — Batch crée Suggestions ✅ |
|---|---|---|
| **Cohérence VMM** | ❌ MV recalcule ≠ batch | ✅ Une seule VMM (`vmm_calcule`) |
| **pending_qty** | ❌ Absent de la MV | ✅ Nativement dans le batch |
| **Exclusion mois rupture** | ❌ Absente de la MV | ✅ `calculateVMM()` |
| **Friction UX** | ⚠️ 2 étapes (vue → panier) | ✅ 1 étape (panier pré-peuplé) |
| **Alignement logiciels référence** | ✅ Pattern "vue analytique" | ✅ Pattern "batch → panier" (Winpharma, Pharmagest) |
| **Migration SQL nécessaire** | ❌ V1.3.9 + corrections MV | ✅ Aucune migration SQL complexe |
| **Réutilisation code existant** | ⚠️ Nouveau endpoint "mettre-au-panier" | ✅ Extension `SemoisCalculationService` |
| **Gestion `createCommandesFromSemois()`** | ❌ Incohérence (chemin alternatif) | ✅ Supprimé, un seul chemin |
| **Adapté au développement** | ⚠️ Complexité SQL à gérer | ✅ Extension Java simple |

---

### 15.5 Décision finale révisée — Option B actée

> **Le batch `SemoisCalculationService` crée et met à jour les `Suggestion(SEMOIS)` après
> chaque cycle de recalcul.**
> La vue `mv_semois_suggestion` est **déclassée en vue analytics/dashboard** (non paginée),
> éventuellement remplacée par une simple jointure `semois_configuration + stock_produit`.
> L'endpoint `POST /api/semois/mettre-au-panier` devient optionnel (ajout manuel d'un produit).

#### Ce que devient chaque composant

| Composant | Rôle avant (v9) | Rôle après (v10) | Statut |
|---|---|---|---|
| `mv_semois_suggestion` | Liste principale "Réapprovisionnement" | Stats dashboard uniquement | ⚠️ Dégradé |
| `SemoisCalculationService` | Calcule + stocke `semois_configuration` | Calcule + stocke + **crée Suggestions** | 🔧 Étendu |
| `Suggestion(SEMOIS)` + `SuggestionLine` | Panier manuel (créé par "Mettre au panier") | **Panier auto** (créé par batch) | ✅ Source principale |
| `suggerer()` (AUTO) | @Async post-vente | Décommissionné | ❌ Supprimé |
| `createCommandesFromSemois()` | Chemin alternatif vers Commande | Supprimé | ❌ Supprimé |
| `POST /api/semois/mettre-au-panier` | Workflow principal | Ajout manuel ponctuel | ⬇️ Secondaire |
| Tab "Réapprovisionnement" (semois-suggestions) | Liste paginée depuis MV | Dashboard + vue synthétique | 🔧 Simplifié |
| Tab "Panier" (suggestion-home) | Liste suggestions actives | **Interface principale** | ✅ Promu |

---

### 15.6 Architecture technique cible — Extension du batch

#### `SemoisCalculationService` — Nouvelle méthode `mettreAuPanierBatch()`

```java
/**
 * Appelée en fin de doRecalculateAllConfigurations().
 * Crée ou met à jour les Suggestion(SEMOIS) par fournisseur depuis semois_configuration.
 * Protection : ne touche pas les SuggestionLine modifiées manuellement.
 */
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void mettreAuPanierBatch(Magasin magasin) {
    // 1. Lire toutes les configs avec quantiteACommander > 0
    //    (depuis semois_configuration + jointure stock_produit + sous-requête pending)
    List<SemoisConfiguration> configs = semoisConfigRepository
        .findAllEligiblesForPanier(magasin.getId());   // nouvelle query repo

    if (configs.isEmpty()) {
        LOG.info("mettreAuPanierBatch : aucun produit à commander ce cycle");
        return;
    }

    // 2. Grouper par fournisseur
    Map<Fournisseur, List<SemoisConfiguration>> byFournisseur = configs.stream()
        .collect(Collectors.groupingBy(
            c -> c.getProduit().getFournisseurProduitPrincipal().getFournisseur()
        ));

    List<Suggestion> toSave = new ArrayList<>();
    List<SuggestionLine> linesToSave = new ArrayList<>();
    List<SuggestionLine> linesToDelete = new ArrayList<>();

    for (Map.Entry<Fournisseur, List<SemoisConfiguration>> entry : byFournisseur.entrySet()) {
        Fournisseur fournisseur = entry.getKey();
        Suggestion suggestion = getOrCreateSuggestionSemois(fournisseur, magasin);

        for (SemoisConfiguration config : entry.getValue()) {
            int stockActuel = getStockActuel(config.getProduit().getId());
            int pendingQty  = getPendingQty(config.getProduit().getId());
            int qteACommander = Math.max(0,
                config.getStockObjectifCalcule() - stockActuel - pendingQty);

            FournisseurProduit fp = config.getProduit().getFournisseurProduitPrincipal();

            Optional<SuggestionLine> existingOpt = suggestion.getSuggestionLines().stream()
                .filter(l -> l.getFournisseurProduit().getId().equals(fp.getId()))
                .findFirst();

            if (qteACommander > 0) {
                if (existingOpt.isPresent()) {
                    SuggestionLine existing = existingOpt.get();
                    // Protection : ne pas écraser une quantité modifiée manuellement
                    if (!existing.isQuantiteModifieeManuel()) {
                        existing.setQuantity(qteACommander);
                        existing.setUpdatedAt(LocalDateTime.now());
                        linesToSave.add(existing);
                    }
                } else {
                    SuggestionLine newLine = new SuggestionLine();
                    newLine.setCreatedAt(LocalDateTime.now());
                    newLine.setUpdatedAt(newLine.getCreatedAt());
                    newLine.setQuantity(qteACommander);
                    newLine.setFournisseurProduit(fp);
                    newLine.setSuggestion(suggestion);
                    suggestion.getSuggestionLines().add(newLine);
                }
            } else if (existingOpt.isPresent()
                       && !existingOpt.get().isQuantiteModifieeManuel()) {
                // Produit plus à commander → retirer du panier sauf si modif manuelle
                linesToDelete.add(existingOpt.get());
            }
        }
        toSave.add(suggestion);
    }

    suggestionRepository.saveAll(toSave);
    suggestionLineRepository.saveAll(linesToSave);
    suggestionLineRepository.deleteAll(linesToDelete);
    LOG.info("mettreAuPanierBatch : {} Suggestion(s), {} ligne(s) mises à jour, {} retirées",
        toSave.size(), linesToSave.size(), linesToDelete.size());
}
```

#### Champ `quantiteModifieeManuel` à ajouter dans `SuggestionLine`

```java
// SuggestionLine.java — 1 champ à ajouter
@Column(name = "quantite_modifiee_manuel", nullable = false)
private boolean quantiteModifieeManuel = false;
```

Migration `V1.3.9` :

```sql
ALTER TABLE warehouse.suggestion_line
  ADD COLUMN IF NOT EXISTS quantite_modifiee_manuel boolean NOT NULL DEFAULT false;
```

Quand `updateSuggestionLinQuantity()` est appelé (modification manuelle via l'UI) :
```java
line.setQuantiteModifieeManuel(true);  // batch ne touchera plus cette ligne
```

---

### 15.7 Actions obsolètes du plan v9 — Révision

#### Actions qui TOMBENT (obsolètes)

| # | Action v9 | Raison de l'obsolescence |
|---|---|---|
| **S0.2** | Migration `V1.3.9` : corriger `mv_semois_suggestion` (vmm > 0 + HAVING + pending_qty) | La MV n'est plus la source principale de la liste. Elle reste pour le dashboard stats → une correction mineure suffit (juste `vmm_calcule > 0` pour la sanité du dashboard) |
| **S1.2** | `POST /api/semois/mettre-au-panier` (workflow principal) | Remplacé par le batch. Devient optionnel pour ajout manuel ponctuel d'un produit oublié. |
| **S1.4** | Bouton "Ajouter au panier (N)" comme CTA principal dans `semois-suggestions` | N'est plus le déclencheur principal. Devient "Ajouter manuellement" (action secondaire). |
| — | `createCommandesFromSemois()` dans `SuggestionProduitServiceImpl` | Supprimé. Un seul chemin : `Suggestion` → `Commande`. |

#### Actions qui RESTENT valides (inchangées)

| # | Action v9 | Statut |
|---|---|---|
| **S0.1** | Décommissionner `suggestionAuto` (supprimer `registerSynchronization` dans `SalesLineServiceImpl`) | ✅ Inchangé — priorité maximale |
| **S0.3** | Supprimer `.filter(dto -> dto.quantiteACommander() > 0)` Java post-pagination | ✅ Inchangé (mais la cause devient le batch qui filtre en amont) |
| **S0.4** | `consommationMensuelle` : ColDef dynamiques AG Grid dans le panneau produit | ✅ Inchangé |
| **S1.1** | `TypeSuggession.SEMOIS` dans l'enum | ✅ Inchangé |
| **S1.3** | Renommer `SuggestionsSource` : `REAPPRO` → `PANIER` | ✅ Inchangé (libellés conservés) |
| **S1.5** | Badge panier (compteur suggestions actives) | ✅ Inchangé |
| **S2.1–2.5** | Enrichissement UX | ✅ Inchangé |
| **S3.1–3.4** | Qualité technique | ✅ Inchangé |

---

### 15.8 Plan d'implémentation révisé — v10

#### 🔴🔴 Sprint 0 — Corrections bloquantes (1–2 jours)

| # | Action | Fichier | Effort |
|---|---|---|---|
| **S0.1** | Décommissionner `suggester()` : supprimer `registerSynchronization` dans `SalesLineServiceImpl` | `SalesLineServiceImpl.java` | 🟢 30 min |
| **S0.2** ~~(révisé)~~ | ~~Migration V1.3.9 corriger la MV~~ → remplacé par **S0.2b** ci-dessous | ~~`v_semois_suggestion`~~ | ❌ Caduc |
| **S0.2b** | Migration `V1.3.9` mineure : ajouter `quantite_modifiee_manuel` dans `suggestion_line` | `V1.3.9__suggestion_line_flag.sql` | 🟢 5 min |
| **S0.3** | Supprimer `.filter()` Java dans `getAllSuggestions()` | `SemoisCalculationService.java` | 🟢 15 min |
| **S0.4** | `consommationMensuelle` : ColDef AG Grid dynamiques | `suggestion-produit-panel.component.ts` | 🟡 1h |

#### 🔴 Sprint 1 — Batch crée les Suggestions (2–3 jours)

| # | Action | Fichier | Effort |
|---|---|---|---|
| **S1.0** | Champ `quantiteModifieeManuel` dans `SuggestionLine` | `SuggestionLine.java` + migration | 🟢 15 min |
| **S1.1** | `TypeSuggession.SEMOIS` dans l'enum | `TypeSuggession.java` | 🟢 5 min |
| **S1.2** | `SemoisConfigurationRepository.findAllEligiblesForPanier()` | `SemoisConfigurationRepository.java` | 🟡 1h |
| **S1.3** | `mettreAuPanierBatch()` dans `SemoisCalculationService` | `SemoisCalculationService.java` | 🟡 3h |
| **S1.4** | Appel de `mettreAuPanierBatch()` en fin de `doRecalculateAllConfigurations()` | `SemoisCalculationService.java` | 🟢 15 min |
| **S1.5** | `updateSuggestionLinQuantity()` → set `quantiteModifieeManuel = true` | `SuggestionProduitServiceImpl.java` | 🟢 5 min |
| **S1.6** | Supprimer `createCommandesFromSemois()` (chemin alternatif) | `SuggestionProduitServiceImpl.java` | 🟢 15 min |
| **S1.7** | Renommer `SuggestionsSource` → `REAPPRO`/`PANIER`, inverser ordre onglets | `command-common.service.ts` + `suggestions-unified` | 🟡 1h |
| **S1.8** | Badge panier + `panierCount()` signal | `suggestions-unified.component` | 🟢 30 min |

#### 🟠 Sprint 2 — UX Panier (2–3 jours)

| # | Action |
|---|---|
| **S2.1** | Afficher date de génération du batch dans le panier ("Généré le JJ/MM/YYYY à HH:mm") |
| **S2.2** | Bouton "Actualiser maintenant" (force `recalculateAfterClassification()` + `mettreAuPanierBatch()`) |
| **S2.3** | Icône 🔒 sur les lignes `quantiteModifieeManuel = true` ("Quantité modifiée manuellement") |
| **S2.4** | Bouton "Réinitialiser qté" sur une ligne (remet `quantiteModifieeManuel = false`) |
| **S2.5** | Export CSV depuis le panier |
| **S2.6** | "Ajouter un produit manuellement" (ex-S1.2 v9) — feature secondaire |

#### 🟡 Sprint 3 — Analytics (1–2 jours)

| # | Action |
|---|---|
| **S3.1** | Simplifier `semois-suggestions` component → dashboard stats (ruptures, urgences, KPI) sans liste paginée |
| **S3.2** | La MV `mv_semois_suggestion` reste pour les requêtes dashboard (`getDashboard()`, `getAllUrgentSuggestions()`) — ajouter `vmm_calcule > 0` dans son WHERE |
| **S3.3** | Ou : remplacer la MV par une simple query `semois_configuration + stock_produit` dans le dashboard |

---

### 15.9 Tableau récapitulatif des priorités — Version finale v10

| Sprint | # | Action | Effort | Statut v9 |
|---|---|---|---|---|
| 0 | S0.1 | Décommissionner `suggestionAuto` | 30 min | ✅ Inchangé |
| 0 | S0.2b | Migration : champ `quantite_modifiee_manuel` | 5 min | 🆕 Nouveau |
| 0 | S0.3 | Supprimer `.filter()` Java | 15 min | ✅ Inchangé |
| 0 | S0.4 | `consommationMensuelle` ColDef AG Grid | 1h | ✅ Inchangé |
| 1 | S1.0 | `SuggestionLine.quantiteModifieeManuel` | 15 min | 🆕 Nouveau |
| 1 | S1.1 | `TypeSuggession.SEMOIS` | 5 min | ✅ Inchangé |
| 1 | S1.2 | Repo query `findAllEligiblesForPanier()` | 1h | 🆕 Nouveau |
| 1 | S1.3 | `mettreAuPanierBatch()` dans le batch | 3h | 🆕 Nouveau (remplace "mettre-au-panier" API) |
| 1 | S1.4 | Appel `mettreAuPanierBatch()` post-recalcul | 15 min | 🆕 Nouveau |
| 1 | S1.5 | `updateSuggestionLinQuantity()` → flag manuel | 5 min | 🆕 Nouveau |
| 1 | S1.6 | Supprimer `createCommandesFromSemois()` | 15 min | 🆕 Nouveau (suppression) |
| 1 | S1.7 | Renommage onglets `REAPPRO`/`PANIER` | 1h | ✅ Inchangé |
| 1 | S1.8 | Badge panier | 30 min | ✅ Inchangé |
| 2 | S2.1–2.6 | UX enrichissement panier | 2–3j | 🆕 Principalement nouveau |
| 3 | S3.1–3.3 | Analytics / simplification MV | 1–2j | 🔧 Révisé (MV déclassée) |

---

*Document v10 — Décision révisée : batch `SemoisCalculationService` crée/met à jour les `Suggestion(SEMOIS)`.
La vue `mv_semois_suggestion` est déclassée en analytics. `createCommandesFromSemois()` supprimé.
Analyse basée sur le code source réel : `SemoisCalculationService.java`, `SuggestionProduitServiceImpl.java`,
`SalesLineServiceImpl.java`, `Suggestion.java`, `SuggestionLine.java`, `mv_semois_suggestion` SQL (V1.2.1).*

---

## 16. Contraintes réelles et corrections du plan — v12

> **Contexte :** Précisions supplémentaires intégrées :
> 1. L'application ne tourne **pas 24/24** (officine : horaires bureau, redémarrages)
> 2. `creerSuggestionBatch()` (ex-`mettreAuPanierBatch()`) doit être appelé **après** `doRecalculateAllConfigurations()`
> 3. `protected BatchResult processBatch(...)` expose un type privé hors de sa portée → déplacer dans `SemoisBatchJobService`
> 4. **S0.3 corrigé** : `getAllSuggestion()` existe **déjà** dans `SuggestionProduitServiceImpl` — il suffit d'ajouter un filtre `StatutSuggession` et d'enrichir
> 5. S1.3 : `mettreAuPanierBatch()` → renommé `creerSuggestionBatch()`
> 6. **Structure 3 onglets** :
>    - **"Réapprovisionnement"** → `suggestion-home` enrichi, `Suggestion(SEMOIS, GENEREE)` par fournisseur
>    - **"Commandes à passer"** → `Suggestion(SEMOIS, VALIDEE)` — suggestions validées non encore commandées
>    - **"Analyse des stocks"** → onglet actuel "SEMOIS" **renommé**, analytics lecture seule (`mv_semois_suggestion`)
> 7. **Suppression** de l'onglet "Commandes en cours" (géré dans le module Commandes dédié)

---

### 16.1 Contrainte 1 — L'application ne tourne pas 24/24

#### Problème avec le seul `@Scheduled`

Le cron actuel (`0 0 8-19 * * *`) suppose que la JVM tourne en continu.
En officine, l'application peut être arrêtée le soir, redémarrée le matin.
Si le démarrage se fait à 8h30 mais que le cron est configuré à 8h00, le recalcul
est manqué jusqu'à la prochaine tranche horaire.

La garde journalière `lastCalcDate.isEqual(LocalDate.now())` évite le double calcul mais
ne compense pas un démarrage tardif où le cron est déjà passé.

#### Solution — Déclencheur au démarrage (`ApplicationListener`)

```java
// SemoisCalculationService — ajouter un déclencheur @EventListener
@EventListener(ApplicationReadyEvent.class)
@Async("taskExecutor")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public void onApplicationReady() {
    // Identique à recalculateAllConfigurations() mais sans cron
    // La garde journalière (lastCalcDate == today) évite le double calcul
    // si l'app a déjà tourné aujourd'hui
    LOG.info("Application démarrée — vérification recalcul SEMOIS");
    if (!calculEnCours.compareAndSet(false, true)) return;
    try {
        doRecalculateAllConfigurations();
    } finally {
        calculEnCours.set(false);
    }
}
```

**Comportement :**
- Démarrage à 8h30 → recalcul déclenché automatiquement ✅
- Redémarrage à 14h (déjà calculé ce matin) → `lastCalcDate == today` → skip ✅
- Recalcul manuel via API → `recalculateAfterClassification()` force le skip ✅

---

### 16.2 Contrainte 2 — `creerSuggestionBatch()` après `doRecalculateAllConfigurations()`

Le batch de création des Suggestions doit être chaîné en fin de recalcul :

```java
private void doRecalculateAllConfigurations() {
    // ... calcul existant (processBatch par pages) ...

    // ← NOUVEAU : après le dernier batch de calcul, créer/mettre à jour les Suggestions
    semoisBatchJobService.creerSuggestionBatch(getMagasinDefault());

    updateAppConfigurationDate(semoisConfigOpt.orElse(null));
    LOG.info("Recalcul SEMOIS terminé ...");
}
```

Ce chaînage garantit que `creerSuggestionBatch()` utilise toujours des données
`semois_configuration` fraîchement calculées.

---

### 16.3 Contrainte 3 — `BatchResult` exposé hors de sa portée → `SemoisBatchJobService`

#### Problème

```java
// SemoisCalculationService — PROBLÈME
private record BatchResult(int successCount, int errorCount) {}  // portée : private

@Transactional(propagation = Propagation.REQUIRES_NEW)
protected BatchResult processBatch(...)  // ← expose BatchResult hors de la classe → warning IDE
```

Spring exige `protected` (ou public) pour pouvoir proxifier `@Transactional(REQUIRES_NEW)`.
Mais `BatchResult` est `private record` → incohérence de visibilité.

#### Solution — Extraire dans `SemoisBatchJobService`

Créer un nouveau service `SemoisBatchJobService` qui contient :
- `processBatch()` avec `@Transactional(REQUIRES_NEW)`
- `creerSuggestionBatch()` avec la logique de création des `Suggestion`
- `BatchResult` en tant que `public record` (ou package-private)

`SemoisCalculationService` garde :
- L'orchestration `doRecalculateAllConfigurations()`
- Les déclencheurs `@Scheduled` + `@EventListener`
- Les méthodes de lecture/dashboard (`getDashboard()`, `getSuggestionForProduct()`, etc.)
- Les calculs métier (`calculateVMM()`, `computeAllCalculs()`, etc.)

```
SemoisCalculationService         SemoisBatchJobService
─────────────────────────        ──────────────────────────────────────
@Scheduled cron              →   processBatch(produits, ...) @Tx(NEW)
@EventListener startup       →   creerSuggestionBatch(magasin)   @Tx(NEW)
doRecalculateAllConf()           BatchResult (public record)
getDashboard()
getSuggestionForProduct()
calculateVMM()
```

**Avantage :** `SemoisCalculationService` devient lisible et `BatchResult` a une visibilité cohérente.

---

### 16.4 Contrainte 4 (S0.3 corrigé) — `getAllSuggestion()` existe déjà dans `SuggestionProduitServiceImpl`

#### Situation réelle

```java
// SuggestionProduitServiceImpl.java — méthode DÉJÀ PRÉSENTE
@Override
@Transactional(readOnly = true)
public Page<SuggestionProjection> getAllSuggestion(
    String search,
    Integer fournisseurId,
    TypeSuggession typeSuggession,  // ← filtre sur le type (AUTO, SEMOIS, MANUELLE)
    Pageable pageable
) {
    Specification<Suggestion> specification =
        suggestionRepository.filterByDate(appConfigurationService.findSuggestionRetention());
    if (typeSuggession != null)
        specification = specification.and(suggestionRepository.filterByType(typeSuggession));
    if (fournisseurId != null)
        specification = specification.and(suggestionRepository.filterByFournisseurId(fournisseurId));
    if (StringUtils.hasLength(search))
        specification = specification.and(suggestionRepository.filterByProduit(search));
    return suggestionRepository.getAllSuggestion(specification, pageable);
}
```

**La méthode existe et fonctionne.** Ce qui manque pour les deux nouveaux onglets :
- Un filtre sur **`StatutSuggession`** (GENEREE → Réapprovisionnement ; VALIDEE → Commandes à passer)
- La spec correspondante dans `SuggestionRepository`

#### Modification minimale — ajouter `filterByStatut()` dans `SuggestionRepository`

```java
// SuggestionRepository.java — 1 spec à ajouter
default Specification<Suggestion> filterByStatut(StatutSuggession statut) {
    return (root, query, cb) -> cb.equal(root.get(Suggestion_.statut), statut);
}
```

#### Enrichissement de `getAllSuggestion()` — ajouter le paramètre `statut`

```java
// SuggestionProduitServiceImpl.java — ajouter StatutSuggession au filtre
@Override
@Transactional(readOnly = true)
public Page<SuggestionProjection> getAllSuggestion(
    String search,
    Integer fournisseurId,
    TypeSuggession typeSuggession,
    StatutSuggession statut,        // ← nouveau paramètre
    Pageable pageable
) {
    Specification<Suggestion> specification =
        suggestionRepository.filterByDate(appConfigurationService.findSuggestionRetention());
    if (typeSuggession != null)
        specification = specification.and(suggestionRepository.filterByType(typeSuggession));
    if (statut != null)
        specification = specification.and(suggestionRepository.filterByStatut(statut));
    if (fournisseurId != null)
        specification = specification.and(suggestionRepository.filterByFournisseurId(fournisseurId));
    if (StringUtils.hasLength(search))
        specification = specification.and(suggestionRepository.filterByProduit(search));
    return suggestionRepository.getAllSuggestion(specification, pageable);
}
```

**Appels depuis le frontend :**
- Onglet "Réapprovisionnement" → `type=SEMOIS&statut=GENEREE`
- Onglet "Commandes à passer" → `type=SEMOIS&statut=VALIDEE`

**Effort réel : 🟢 30 min** (vs. 🟡 1h estimé en v11 car aucun déplacement nécessaire)

---

### 16.5 Contrainte 5 — Renommage `mettreAuPanierBatch()` → `creerSuggestionBatch()`

Dans `SemoisBatchJobService` (nouveau service, cf. §16.3) :

```java
/**
 * Crée ou met à jour les Suggestion(SEMOIS) + SuggestionLine par fournisseur
 * depuis les données fraîchement calculées dans semois_configuration.
 * Appelé en fin de doRecalculateAllConfigurations().
 */
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void creerSuggestionBatch(Magasin magasin) { ... }
```

---

### 16.6 Structure des 3 onglets — v12

#### Mapping final

| `SuggestionsSource` | Libellé onglet | Icône | Composant | Source de données | Filtre backend | Statut `Suggestion` |
|---|---|---|---|---|---|---|
| `'REAPPRO'` | **Réapprovisionnement** | `pi-boxes` | `app-suggestion-home` (enrichi) | `SuggestionRepository` | `type=SEMOIS & statut=GENEREE` | `GENEREE` |
| `'COMMANDES_A_PASSER'` | **Commandes à passer** | `pi-send` | `app-suggestion-home` (mode validation) | `SuggestionRepository` | `type=SEMOIS & statut=VALIDEE` | `VALIDEE` |
| `'ANALYSE'` | **Analyse des stocks** | `pi-chart-bar` | `app-semois-suggestions` (renommé) | `mv_semois_suggestion` (lecture seule) | — | — (vue MV) |

> **"Commandes en cours"** : **supprimé** de ce module. La gestion des `Commande(REQUESTED)` reste dans le module Commandes dédié.

#### Workflow utilisateur avec les 3 onglets

```
AUTOMATIQUE (batch nuit / démarrage)
  creerSuggestionBatch()
    → crée Suggestion(SEMOIS, statut=GENEREE) par fournisseur
    → crée SuggestionLine pour chaque produit qteACommander > 0

             ▼

ONGLET 1 — "Réapprovisionnement"   (statut = GENEREE)
  ┌──────────────────────────────────────────────────────────┐
  │ split-panel: liste fournisseurs | produits avec VMM,     │
  │ stock, conso mensuelle, indicateur 🔒 si qté manuelle    │
  │                                                          │
  │ Actions : [Éditer qté] [✅ Valider] [🗑 Rejeter]         │
  └──────────────────────────────────────────────────────────┘
    ↓ validerSuggestion(id) → statut = VALIDEE

ONGLET 2 — "Commandes à passer"   (statut = VALIDEE)
  ┌──────────────────────────────────────────────────────────┐
  │ Même layout suggestion-home, lecture enrichie            │
  │ Suggestions prêtes à être envoyées au fournisseur        │
  │                                                          │
  │ Actions : [🛒 Commander] [📋 PDF] [📊 CSV] [↩ Revenir]  │
  └──────────────────────────────────────────────────────────┘
    ↓ commander(id) → crée Commande(REQUESTED), supprime Suggestion

ONGLET 3 — "Analyse des stocks"   (lecture seule, MV)
  ┌──────────────────────────────────────────────────────────┐
  │ KPI : 🔴 Ruptures  🟠 Sous seuil  ✅ OK  🔵 Surstock     │
  │ Répartition par classe A/B/C                             │
  │ Top 10 produits urgents                                  │
  │ Composant actuel semois-suggestions renommé              │
  └──────────────────────────────────────────────────────────┘
```

#### `SuggestionsSource` — nouveau type TypeScript

```typescript
// command-common.service.ts
export type SuggestionsSource = 'REAPPRO' | 'COMMANDES_A_PASSER' | 'ANALYSE';
suggestionsActiveSource = signal<SuggestionsSource>('REAPPRO');  // ← REAPPRO par défaut
```

#### `suggestions-unified.component.html` — 3 onglets

```html
<div class="su-source-tabs-bar">
  <button class="su-tab" [class.su-tab--active]="activeSource() === 'REAPPRO'"
          (click)="setSource('REAPPRO')" type="button">
    <i class="pi pi-boxes"></i>
    Réapprovisionnement
    @if (nbReappro() > 0) { <span class="su-tab-badge su-badge--urgent">{{ nbReappro() }}</span> }
  </button>

  <button class="su-tab" [class.su-tab--active]="activeSource() === 'COMMANDES_A_PASSER'"
          (click)="setSource('COMMANDES_A_PASSER')" type="button">
    <i class="pi pi-send"></i>
    Commandes à passer
    @if (nbCommandesAPasser() > 0) { <span class="su-tab-badge su-badge--info">{{ nbCommandesAPasser() }}</span> }
  </button>

  <button class="su-tab" [class.su-tab--active]="activeSource() === 'ANALYSE'"
          (click)="setSource('ANALYSE')" type="button">
    <i class="pi pi-chart-bar"></i>
    Analyse des stocks
  </button>
</div>

@if (activeSource() === 'REAPPRO') {
  <app-suggestion-home [statut]="'GENEREE'" />
} @else if (activeSource() === 'COMMANDES_A_PASSER') {
  <app-suggestion-home [statut]="'VALIDEE'" />
} @else {
  <app-semois-suggestions />   <!-- inchangé, juste renommé visuellement -->
}
```

**Note :** `app-suggestion-home` reçoit un `@Input() statut` pour filtrer GENEREE ou VALIDEE.
Les actions disponibles changent selon le statut (éditer/valider pour GENEREE, commander pour VALIDEE).

#### Badges des onglets — signaux dans `SuggestionsUnifiedComponent`

```typescript
// suggestions-unified.component.ts
readonly nbReappro = signal<number>(0);           // nb fournisseurs avec suggestions GENEREE
readonly nbCommandesAPasser = signal<number>(0);  // nb fournisseurs avec suggestions VALIDEE

constructor() {
  // Charge les compteurs au démarrage et après chaque action
  this.loadBadgeCounts();
}

private loadBadgeCounts(): void {
  this.suggestionService.countByStatut('GENEREE').subscribe(n => this.nbReappro.set(n));
  this.suggestionService.countByStatut('VALIDEE').subscribe(n => this.nbCommandesAPasser.set(n));
}
```

---

### 16.7 Plan d'implémentation révisé — v12

#### 🔴🔴 Sprint 0 — Corrections bloquantes (1–2 jours)

| # | Action | Fichier(s) | Effort | Δ |
|---|---|---|---|---|
| **S0.1** | Décommissionner `suggestionAuto` : supprimer `registerSynchronization` dans `SalesLineServiceImpl` | `SalesLineServiceImpl.java` | 🟢 30 min | ✅ |
| **S0.2** | Migration `V1.3.9` : `quantite_modifiee_manuel BOOLEAN NOT NULL DEFAULT false` dans `suggestion_line` | `V1.3.9__suggestion_line_flag.sql` | 🟢 5 min | ✅ |
| **S0.3** ✅ | `filterByStatut()` dans `SuggestionRepository` + ajouter paramètre `StatutSuggession statut` à `getAllSuggestion()` | `SuggestionRepository.java` + `SuggestionProduitServiceImpl.java` + interface | 🟢 30 min | 🔧 Simplifié (méthode existe déjà) |
| **S0.4** | `consommationMensuelle` : ColDef AG Grid dynamiques dans `suggestion-produit-panel` | `suggestion-produit-panel.component.ts` | 🟡 1h | ✅ |

#### 🔴 Sprint 1 — Batch + refactoring (3–4 jours)

| # | Action | Fichier(s) | Effort | Δ |
|---|---|---|---|---|
| **S1.0** | Champ `quantiteModifieeManuel` dans `SuggestionLine.java` | `SuggestionLine.java` | 🟢 15 min | ✅ |
| **S1.1** | `TypeSuggession.SEMOIS` dans l'enum | `TypeSuggession.java` | 🟢 5 min | ✅ |
| **S1.2** | Créer `SemoisBatchJobService` : déplacer `processBatch()` + `BatchResult` (public record) | `SemoisBatchJobService.java` (nouveau) | 🟡 2h | 🆕 |
| **S1.3** | `creerSuggestionBatch()` dans `SemoisBatchJobService` | `SemoisBatchJobService.java` | 🟡 3h | 🔧 |
| **S1.4** | Chaîner `semoisBatchJobService.creerSuggestionBatch()` en fin de `doRecalculateAllConfigurations()` | `SemoisCalculationService.java` | 🟢 15 min | ✅ |
| **S1.5** | `@EventListener(ApplicationReadyEvent)` pour déclenchement au démarrage | `SemoisCalculationService.java` | 🟢 30 min | 🆕 |
| **S1.6** | `updateSuggestionLinQuantity()` → set `quantiteModifieeManuel = true` | `SuggestionProduitServiceImpl.java` | 🟢 5 min | ✅ |
| **S1.7** | Supprimer `createCommandesFromSemois()` | `SuggestionProduitServiceImpl.java` + interface | 🟢 15 min | ✅ |
| **S1.8** | `SemoisConfigurationRepository.findAllEligiblesForPanier()` | `SemoisConfigurationRepository.java` | 🟡 1h | ✅ |

#### 🔴 Sprint 2 — Refonte UI 3 onglets (3–4 jours)

| # | Action | Fichier(s) | Effort | Δ |
|---|---|---|---|---|
| **S2.1** | `SuggestionsSource` → `'REAPPRO' \| 'COMMANDES_A_PASSER' \| 'ANALYSE'` ; signal par défaut = `'REAPPRO'` | `command-common.service.ts` | 🟢 15 min | 🔧 3 valeurs au lieu de 2 |
| **S2.2** | 3 onglets dans `suggestions-unified.component.html` ("Réapprovisionnement", "Commandes à passer", "Analyse des stocks") | `suggestions-unified.component.html/.ts` | 🟡 1h | 🔧 |
| **S2.3** | `app-suggestion-home` reçoit `@Input() statut: StatutSuggession` → filtre `GENEREE` ou `VALIDEE` ; actions contextuelles selon statut | `suggestion-home.component.ts/.html` | 🟡 2h | 🔧 |
| **S2.4** | Bandeau "Généré le JJ/MM/YYYY à HH:mm" + bouton "Actualiser" dans `suggestion-home` (onglet REAPPRO uniquement) | `suggestion-home.component.html` | 🟢 30 min | 🆕 |
| **S2.5** | Indicateur 🔒 + bouton "Réinitialiser qté" pour les lignes `quantiteModifieeManuel` | `suggestion-produit-panel.component` | 🟡 1h | 🆕 |
| **S2.6** | `app-semois-suggestions` → renommer visuellement en "Analyse des stocks" (titre + icône) ; supprimer les éventuelles actions d'édition | `semois-suggestions.component.html/.ts` | 🟢 30 min | 🔧 |
| **S2.7** | Badges compteurs sur les 2 premiers onglets (`nbReappro`, `nbCommandesAPasser`) | `suggestions-unified.component.ts/.html` | 🟢 30 min | 🆕 |
| **S2.8** | Supprimer l'onglet "Commandes en cours" s'il existe dans ce module | `suggestions-unified` + routes | 🟢 15 min | 🆕 Suppression |

#### 🟠 Sprint 3 — Enrichissements UX (2–3 jours)

| # | Action |
|---|---|
| **S3.1** | Export CSV/PDF depuis "Commandes à passer" (réutilise `exportToCsv()` / `exportToPdf()` existants) |
| **S3.2** | "Ajouter un produit manuellement" au panier (feature secondaire) |
| **S3.3** | Tendance VMM (↑ ↓ ↔) dans la liste produits |
| **S3.4** | Simulation "après commande → couverture = X mois" (frontend only) |

#### 🟡 Sprint 4 — Qualité technique (2–3 jours)

| # | Action |
|---|---|
| **S4.1** | MV `mv_semois_suggestion` : ajouter `AND sc.vmm_calcule > 0` dans le WHERE (onglet Analyse propre) |
| **S4.2** | Tests unitaires `SemoisBatchJobService` (calcul pendingQty, protection flag manuel) |
| **S4.3** | Exclusion temporaire d'un produit du panier (cycle de vie complet) |
| **S4.4** | Colisage fournisseur (quantité minimale commande) |

---

### 16.8 Tableau récapitulatif des priorités — Version finale v12

| Sprint | # | Action | Effort | Δ |
|---|---|---|---|---|
| 0 | **S0.1** | Décommissionner `suggestionAuto` | 30 min | ✅ |
| 0 | **S0.2** | Migration `quantite_modifiee_manuel` | 5 min | ✅ |
| 0 | **S0.3** | `filterByStatut()` + param `statut` dans `getAllSuggestion()` | 30 min | 🔧 Simplifié |
| 0 | **S0.4** | `consommationMensuelle` ColDef AG Grid | 1h | ✅ |
| 1 | **S1.0** | `SuggestionLine.quantiteModifieeManuel` | 15 min | ✅ |
| 1 | **S1.1** | `TypeSuggession.SEMOIS` | 5 min | ✅ |
| 1 | **S1.2** | `SemoisBatchJobService` (déplace `processBatch` + `BatchResult`) | 2h | 🆕 |
| 1 | **S1.3** | `creerSuggestionBatch()` | 3h | 🔧 |
| 1 | **S1.4** | Chaînage post-recalcul | 15 min | ✅ |
| 1 | **S1.5** | `@EventListener` démarrage | 30 min | 🆕 |
| 1 | **S1.6** | Flag `quantiteModifieeManuel` à la mise à jour | 5 min | ✅ |
| 1 | **S1.7** | Supprimer `createCommandesFromSemois()` | 15 min | ✅ |
| 1 | **S1.8** | `findAllEligiblesForPanier()` repo | 1h | ✅ |
| 2 | **S2.1** | `SuggestionsSource` 3 valeurs | 15 min | 🔧 |
| 2 | **S2.2** | 3 onglets dans `suggestions-unified` | 1h | 🔧 |
| 2 | **S2.3** | `suggestion-home` avec `@Input() statut` | 2h | 🔧 |
| 2 | **S2.4** | Bandeau date génération + bouton Actualiser | 30 min | 🆕 |
| 2 | **S2.5** | Indicateur 🔒 + bouton Réinitialiser | 1h | 🆕 |
| 2 | **S2.6** | `semois-suggestions` renommé "Analyse des stocks" | 30 min | 🔧 |
| 2 | **S2.7** | Badges compteurs onglets | 30 min | 🆕 |
| 2 | **S2.8** | Supprimer onglet "Commandes en cours" | 15 min | 🆕 |
| 3 | **S3.1–3.4** | Enrichissements UX | 2–3j | — |
| 4 | **S4.1–4.4** | Qualité technique | 2–3j | — |

---

### 16.9 Résumé des actions caduques — comparaison v9 → v12

| Action v9 | Statut v12 | Raison |
|---|---|---|
| S0.2 — Migration corriger `mv_semois_suggestion` (vmm>0 + HAVING + pending_qty) | ❌ Caduc | MV reste pour analytics uniquement ; correction mineure `vmm_calcule > 0` en Sprint 4 |
| S0.3 — Déplacer `getAllSuggestions()` depuis `SemoisCalculationService` | 🔧 Révisé (S0.3 v12) | Méthode existe déjà dans `SuggestionProduitServiceImpl` → juste ajouter `filterByStatut()` |
| S1.2 v10 — `POST /api/semois/mettre-au-panier` (workflow principal) | ❌ Caduc | Remplacé par `creerSuggestionBatch()` automatique |
| S1.3 v10 — `mettreAuPanierBatch()` | 🔧 Renommé + déplacé | → `creerSuggestionBatch()` dans `SemoisBatchJobService` |
| S1.4 v9 — Bouton "Ajouter au panier" CTA principal | ❌ Caduc | Batch automatique |
| `createCommandesFromSemois()` | ❌ Supprimé (S1.7) | Un seul chemin : `Suggestion` → `Commande` |
| `BatchResult` private record | 🔧 Extrait | → `public record` dans `SemoisBatchJobService` |
| Tab "SEMOIS" | 🔧 Renommé → "Analyse des stocks" | Conservé en lecture seule, déplacé en 3ème position |
| Tab "Commandes en cours" | ❌ Supprimé (S2.8) | Géré dans le module Commandes dédié |
| 2 onglets (`FOURNISSEURS` + `SEMOIS`) | 🔧 Remplacé | → 3 onglets (`REAPPRO` + `COMMANDES_A_PASSER` + `ANALYSE`) |

---

*Document v12 — Structure 3 onglets actée. `getAllSuggestion()` existe déjà dans `SuggestionProduitServiceImpl` → enrichissement minimal (S0.3 simplifié). Suppression onglet "Commandes en cours". Tab "SEMOIS" renommé "Analyse des stocks" (3ème onglet, lecture seule). `SuggestionsSource` = `'REAPPRO' | 'COMMANDES_A_PASSER' | 'ANALYSE'`.*



