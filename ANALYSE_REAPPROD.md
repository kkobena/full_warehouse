# 📊 Analyse Comparative — Module Réapprovisionnement Pharma-Smart
**Date :** Mars 2026 | **Périmètre :** `service/scheduler` + services associés

---

## 1. Cartographie du module actuel

### 1.1 Architecture en place

```
service/scheduler/
├── StockReapproServiceImpl          → Modèle CLASSIQUE (moyenne 3 mois)
├── SemoisCalculationService         → Modèle SEMOIS (VMM pondérée + marge de sécurité)
├── VentesAgregeesService            → Agrégation mensuelle (gel progressif)
├── ClassificationCriticiteService   → Classification ABC Pareto automatique (A+/A/B/C/D)
├── ClassificationBatchProcessor     → Batch de reclassification mensuelle
├── TournantSchedulerService         → Inventaire tournant (Cycle Counting)
├── MaterializedViewRefreshService   → Rafraîchissement vues matérialisées
└── SemoisStartupCoordinator         → Coordination au démarrage
```

### 1.2 Paramètres stock gérés (double niveau)

| Paramètre | Table | Niveau | Description | Modèle |
|-----------|-------|--------|-------------|--------|
| `qty_seuil_mini` | `produit` | Produit global | Seuil mini réappro (écrit par CLASSIQUE & SEMOIS) | CLASSIQUE / SEMOIS |
| `qty_appro` | `produit` | Produit global | Qté de réapprovisionnement | CLASSIQUE |
| `seuil_mini` | `stock_produit` | Par emplacement | Seuil mini réserve → rayon (stockage physique) | REASSORT interne |
| `stock_reassort` | `stock_produit` | Par emplacement | Qté à transférer réserve → rayon | REASSORT interne |
| `stock_maxi` | `stock_produit` | Par emplacement | Stock maximum en rayon (overflow détection) | REASSORT interne |
| `vmm_calcule` | `semois_configuration` | Par produit | VMM pondérée calculée | SEMOIS |
| `marge_securite` | `semois_configuration` | Par produit | Stock de sécurité (= seuil mini dynamique) | SEMOIS |
| `stock_objectif_calcule` | `semois_configuration` | Par produit | Stock cible = VMM + Marge | SEMOIS |

### 1.3 Formules SEMOIS actuelles

```
VMM = Σ(Ventes_mois_i × Poids_i) / Σ(Poids_i)
      (poids décroissant : mois récent = poids maximal)

Marge de Sécurité = VMM × (Délai_livraison_jours × Coeff_sécurité / 30) × Facteur_saisonnier

Stock Objectif = VMM + Marge_de_Sécurité
                 [avec plafond : MIN(Stock Objectif, VMM × 3) si limite_peremption=true]

Qté à Commander = MAX(0, Stock Objectif - Stock Actuel)
```

### 1.4 Classification Criticité (Pareto ABC)

```
CA cumulé ≤ seuilAPlus (défaut 60%)  → A+  (produits de garde prioritaires)
CA cumulé ≤ seuilA    (défaut 75%)  → A
CA cumulé ≤ seuilB    (défaut 90%)  → B
CA cumulé ≤ seuilC    (défaut 97%)  → C
CA cumulé > seuilC                  → D   (ou fréquence < seuil)
```

---

## 2. Analyse des logiciels de référence officine

### 2.1 Winpharma (CERP / Be-Pharma)

**Modèle de stock min / max / réassort :**
- **CMM (Consommation Mensuelle Moyenne)** sur 3 ou 6 mois glissants, configurable par produit
- **Stock minimum = CMM × Délai livraison (en mois)**  
  → Calcul identique à Pharma-Smart CLASSIQUE (`seuilMin = avg × dayStock`)
- **Stock maximum** = `CMM × (Délai + Fréquence commande)` — calculé dynamiquement, pas saisi manuellement
- **Qté de réassort** = `Stock Max − Stock Actuel` (remplissage au stock max)
- **Correction saisonnière automatique** par détection de pics sur 2 ans d'historique
- **Intégration catalogue grossiste en temps réel** (prix, disponibilité)
- **Alertes dynamiques** : alerte dès que `Stock Actuel < Stock Min`
- **Gestion de la réserve ↔ rayon** native, avec seuils séparés par emplacement

**Points différenciants Winpharma :**
- Stock max calculé automatiquement (non saisi), recalculé à chaque commande validée
- Fréquence de commande par grossiste intégrée dans le calcul du stock max
- Proposition automatique de commande complète (pas seulement une suggestion)
- Génération de bon de commande PharmaML en un clic

---

### 2.2 LGPI (Groupe LGPI / PHR)

**Modèle de stock min / max / réassort :**
- **CMM dynamique** sur 1 à 12 mois paramétrables par classe ABC
- **Stock minimum** = `CMM × (Délai livraison + Délai de traitement interne)`
- **Stock maximum** = `CMM × (Délai + Fréquence de commande + Marge de sécurité)` → entièrement calculé
- **Classification ABC** : 3 classes (A, B, C) avec seuils CA paramétrables — similaire mais moins fin (pas de A+/D)
- **Fréquence de commande** : paramètre clé, permet de distinguer grossiste quotidien vs hebdomadaire
- **Suggestions de commande intelligentes** : tient compte des commandes en cours (stock virtuel/attendu)
- **Nouveaux produits** : période probatoire avant calcul CMM (évite sur-commande sur base insuffisante)
- **Blocage des produits en péremption** : décrémente le stock objectif si lots périmés > X%

---

### 2.3 Périscopie / Pharmagest Interactive

**Modèle de stock min / max / réassort :**
- **CMM étendue** avec option "mois équivalents" (exclure les mois de rupture fournisseur)
- **Stock minimum** = `CMM × (Délai + Coefficient de sécurité par classe)`  
  → Identique à la formule `Marge de Sécurité` de SEMOIS
- **Stock objectif** = `CMM × Couverture cible (en mois)` — paramètre de couverture en mois, pas en jours
- **Stock maximum** = `2 × Stock objectif` (règle simple, modifiable)
- **Saisonnalité automatique** : détection sur 24 mois, coefficient mensuel par produit — plus mature que le facteur saisonnier actuel de Pharma-Smart
- **Produits de garde** : surclassement automatique (identique à Pharma-Smart)
- **Gestion multi-dépôt** : suggestion de transfert réserve → rayon automatisée
- **Intégration DIRECT ORDER** : bon de commande électronique avec confirmation stock grossiste

---

### 2.4 BO-Pharma / Système Dolibarr adapté officines

**Modèle de stock min / max / réassort :**
- Saisie manuelle uniquement : stock min, stock max, qté de réappro
- Pas de calcul automatique de VMM
- Alertes visuelles simples (stock < min)
- **Pertinence** : Système adapté aux petites structures, faible niveau d'automatisation

---

## 3. Tableau comparatif synthétique

| Fonctionnalité | Pharma-Smart | Winpharma | LGPI | Périscopie |
|---------------|:---:|:---:|:---:|:---:|
| VMM pondérée (poids décroissant) | ✅ SEMOIS | ✅ | ✅ | ✅ |
| Stock min dynamique (calculé) | ✅ SEMOIS | ✅ | ✅ | ✅ |
| **Stock max dynamique (calculé)** | ❌ saisie manuelle | ✅ automatique | ✅ automatique | ✅ automatique |
| **Fréquence commande intégrée** | ❌ | ✅ | ✅ | ✅ |
| Coefficient sécurité par classe | ✅ | ✅ | ✅ | ✅ |
| Classification ABC Pareto auto | ✅ 5 niveaux | ✅ 3 niveaux | ✅ 3 niveaux | ✅ 3-4 niveaux |
| Hysterèse classification | ✅ | ❌ pas documenté | ❌ | ❌ |
| **Saisonnalité automatique** | ⚠️ facteur manuel | ✅ auto 24 mois | ✅ auto 12 mois | ✅ auto 24 mois |
| **Exclusion mois de rupture** | ❌ | ✅ | ✅ | ✅ |
| **Stock virtuel (cdes en cours)** | ❌ | ✅ | ✅ | ✅ |
| Suggestion réserve → rayon | ✅ | ✅ | ✅ | ✅ |
| **Stock max réserve calculé** | ❌ saisie | ❌ saisie | ⚠️ semi-auto | ✅ auto |
| Limite péremption (plafond VMM×3) | ✅ | ✅ | ✅ | ✅ |
| Période probatoire nouveaux produits | ✅ `nbMoisMinNouveauProduit` | ✅ | ✅ | ✅ |
| **Bon de commande auto (PharmaML)** | ✅ (envoi) | ✅ (full cycle) | ✅ (full cycle) | ✅ |
| Gel mensuel des ventes | ✅ J+7 | ✅ | ✅ | ✅ |
| Alerte surstock | ✅ (150% stock obj.) | ✅ | ✅ | ✅ |
| **Dashboard réappro temps réel** | ⚠️ partiel | ✅ complet | ✅ complet | ✅ complet |

---

## 4. Analyse des paramètres clés : stock mini, stock maxi, réassort réserve

### 4.1 Comment les références calculent le stock maximum

**Chez Winpharma / LGPI / Périscopie :**
```
Stock Maximum = CMM × (Délai_livraison + Fréquence_commande + Marge_sécurité)
```
Exemple concret :
- CMM = 30 unités/mois
- Délai livraison = 2 jours → 0,07 mois
- Fréquence commande = 7 jours → 0,23 mois
- Coeff sécurité = 1,2
- **Stock Max = 30 × (0,07 + 0,23) × 1,2 = ~11 unités** (renouvelé chaque semaine)

**Chez Pharma-Smart (actuel) :**
- `stock_maxi` dans `stock_produit` = **saisie manuelle** dans le formulaire de stock par emplacement
- `qty_appro` dans `produit` = calculé par CLASSIQUE ou SEMOIS mais représente le stock objectif (≈ stock max)
- **Lacune** : le vrai "stock maximum" au sens réapprovisionnement n'est pas calculé dynamiquement pour la gestion réserve/rayon

### 4.2 Gestion Réserve ↔ Rayon

**Chez Pharma-Smart (actuel) :**
- Seuil mini rayon (`seuil_mini` de `stock_produit`) : saisie manuelle
- Qté de réassort rayon (`stock_reassort` de `stock_produit`) : saisie manuelle
- Stock maxi rayon (`stock_maxi` de `stock_produit`) : saisie manuelle
- Trigger : lors d'une vente, si `qtyStock < seuilMini` → suggestion RAYON créée
- Overflow : si `stockActuel > stockMaxi` → suggestion RESERVE (retour réserve)
- **Lacune** : pas de calcul automatique de ces 3 paramètres depuis la CMM/VMM

**Ce que proposent les références :**
```
Seuil mini rayon  = CMM × Délai_réassort_interne (ex: 0,5 jour)
Stock réassort    = CMM × Fréquence_réassort_rayon (ex: 1 jour)
Stock maxi rayon  = CMM × (Délai + Fréquence) ← identique au niveau commande fournisseur
```

### 4.3 Qté de réassort réserve (qté à commander fournisseur)

**Formule cible recommandée par les références (Wilson adapté officines) :**
```
Qté_commande = Stock_Max - Stock_Virtuel
Stock_Virtuel = Stock_Actuel + Qtés_commandées_en_cours - Qtés_réservées_clients
```

**Chez Pharma-Smart (actuel) :**
```
Qté_commande = MAX(0, Stock_Objectif - Stock_Actuel)
```
→ **Lacune critique** : le stock virtuel (commandes en cours) n'est pas soustrait du calcul.
Si une commande est déjà passée pour 50 unités et que le stock objectif est 60,
le système suggère de commander encore 10 (en réalité 0 nécessaire).

---

## 5. Axes d'amélioration prioritaires

### 🔴 Axe 1 — Stock virtuel dans le calcul de suggestion (CRITIQUE)

**Problème :** La quantité déjà commandée (lignes de commande en cours : `ORDER_LINE` avec statut PENDING) n'est pas déduite du calcul de la quantité à commander.

**Proposition :**
```java
// Formule recommandée
int stockVirtuel = stockActuel + qtesCommandeesEnAttente;
int qteACommander = Math.max(0, stockObjectif - stockVirtuel);
```
**Implémentation :** Ajouter une requête dans `SuggestionProduitServiceImpl.getSuggestionLinesById()`
pour charger les quantités en attente par produit (`order_line` JOIN `commande` WHERE `order_statut IN ('PENDING', 'ORDERED')`).

---

### 🔴 Axe 2 — Calcul automatique des paramètres réserve/rayon

**Problème :** `seuil_mini`, `stock_reassort`, `stock_maxi` dans `stock_produit` sont 100% saisis manuellement. Résultat : données souvent non renseignées ou obsolètes, rendant le système de réassort rayon inefficace.

**Proposition :**  
Ajouter dans `SemoisCalculationService.processBatch()` le calcul automatique des paramètres rayon pour le magasin principal :
```java
// Auto-calculé depuis SEMOIS
stockProduit.setSeuilMini(marge_securite / 2);        // seuil mini rayon = moitié de la marge
stockProduit.setStockReassort(vmm / 4);               // réassort 1 semaine de ventes
stockProduit.setStockMaxi(vmm);                       // stock maxi rayon = 1 VMM
```

---

### 🟠 Axe 3 — Exclusion des mois de rupture dans le calcul VMM

**Problème :** Si un produit a été en rupture fournisseur pendant 2 mois, ses ventes sont artificiellement basses. La VMM sera sous-estimée, entraînant un stock objectif trop faible et perpétuant la rupture.

**Proposition :**  
Dans `VentesAgregeesService.aggregateOrUpdateMonth()`, détecter les mois de rupture (ventes = 0 ET commande existante refusée/partielle) et les exclure du calcul :
```sql
-- Ne pas inclure les mois où le produit était en rupture fournisseur
WHERE vma.quantite_vendue > 0 OR vma.rupture_fournisseur = false
```
Alternative : introduire un flag `est_rupture_fournisseur` dans `ventes_mensuelles_agregees`.

---

### 🟠 Axe 4 — Saisonnalité automatique (détection)

**Problème :** Le `facteur_saisonnier_actuel` dans `semois_configuration` est un paramètre saisi manuellement (défaut 1.0). Les références calculent ce facteur automatiquement sur 24 mois.

**Proposition :**  
Calculer un coefficient saisonnier mensuel par produit :
```java
// Coefficient saisonnier mois M = Ventes(M, année N-1) / CMM_annuelle
double coeffSaisonnier = ventesAgregeesRepository
    .findByProduitIdAndMois(produitId, moisActuelAnneePrecedente)
    .map(v -> v.getQuantiteVendue() / (vmm > 0 ? vmm : 1.0))
    .orElse(1.0);
// Borner entre 0.5 et 3.0 pour éviter les aberrations
coeffSaisonnier = Math.max(0.5, Math.min(3.0, coeffSaisonnier));
```
Intégrer dans `SemoisCalculationService.computeMarge()`.

---

### 🟡 Axe 5 — Fréquence de commande dans le stock max

**Problème :** Le stock objectif actuel = `VMM + Marge de sécurité`. Cela ne tient pas compte de la fréquence de commande (un pharmacien qui commande tous les jours n'a pas besoin du même stock max qu'un qui commande toutes les semaines).

**Proposition :**  
Ajouter un paramètre `frequence_commande_jours` sur `GroupeFournisseur` / `SemoisClasseConfig` :
```java
// Stock objectif enrichi
int stockObjectif = vmm                              // consommation mensuelle  
    + margeSecurite                                  // sécurité (délai livraison)
    + (int)(vmm * frequenceCommandeJours / 30.0);    // stock de rotation commande
```

---

### 🟡 Axe 6 — Dashboard réappro temps réel

**Problème :** Pas de vue consolidée du statut de réapprovisionnement en temps réel.

**Proposition :** Créer une vue matérialisée `mv_reappro_dashboard` consolidant :
- Produits en rupture (stock < marge sécurité)
- Produits sous seuil (marge sécurité ≤ stock < stock objectif)
- Produits OK
- Produits en surstock (stock > 1.5 × stock objectif)
- Valeur totale de rupture (CA manqué estimé)

---

### 🟡 Axe 7 — Couverture vs stock objectif : passer en mois

**Problème :** La notion de "stock objectif" est moins intuitive pour le pharmacien qu'un "nombre de mois de couverture". Winpharma et Périscopie affichent les deux.

**Proposition :** Enrichir `SemoisSuggestionDTO` et la vue SEMOIS :
```java
// Dans SemoisSuggestionDTO
public double getTauxCouvertureMois() → déjà implémenté ✅
// Ajouter : couverture cible en mois
public double getCouvertureCibleMois() {
    return vmm > 0 ? (double) stockObjectif / vmm : 0;
}
```

---

### 🟢 Axe 8 — Log de pilotage de la classification (déjà bien implémenté)

**Point fort à conserver :**
- Le système d'hysterèse est une innovation rare (non présente dans LGPI/Périscopie)
- Les 5 classes (A+, A, B, C, D) vs 3 classes chez les concurrents = granularité supérieure
- L'override manuel avec log d'audit est conforme aux bonnes pratiques pharmaceutiques

---

## 6. Synthèse des priorités

```
Priorité 1 (impact immédiat sur la qualité des suggestions) :
  → Axe 1 : Stock virtuel (commandes en cours)           ← Quick Win critique
  → Axe 3 : Exclusion mois de rupture VMM                ← Qualité des données

Priorité 2 (réduction du travail manuel de paramétrage) :
  → Axe 2 : Calcul auto seuil/maxi/réassort rayon        ← Productivité équipe
  → Axe 4 : Saisonnalité automatique                     ← Précision suggestions

Priorité 3 (alignement best practices secteur) :
  → Axe 5 : Fréquence commande dans stock max            ← Sophistication
  → Axe 6 : Dashboard réappro temps réel                 ← Expérience utilisateur
  → Axe 7 : Affichage couverture en mois                 ← Lisibilité
```

---

## 7. Points forts distinctifs de Pharma-Smart

| Point fort | Valeur |
|-----------|--------|
| **5 classes criticité** (A+, A, B, C, D) | Plus fin que LGPI/Périscopie (3 classes) |
| **Hysterèse de reclassification** | Stabilité des classes, évite les oscillations |
| **Gel progressif des ventes** (fenêtre J+7) | Données historiques fiables |
| **Séparation CLASSIQUE / SEMOIS** | Migration progressive possible |
| **Override manuel audité** | Conformité réglementaire (traçabilité) |
| **Intégration PharmaML complète** | Cycle de commande électronique end-to-end |
| **Inventaire tournant (Cycle Counting)** | Feature avancée (peu présente dans les références) |

---

*Document généré sur la base de l'analyse de `service/scheduler/` et des services associés (SemoisCalculationService, StockReapproServiceImpl, VentesAgregeesService, ClassificationCriticiteService, SuggestionReassortServiceImpl).*
