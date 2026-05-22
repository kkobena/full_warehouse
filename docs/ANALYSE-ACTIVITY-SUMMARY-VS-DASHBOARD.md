# Comparaison : activity-summary vs Tableau de bord exécutif
# Analyse du module mvt-caisse

**Date** : 2026-05-21  
**Fichiers analysés** :
- `entities/raport-gestion/activity-summary/activity-summary.component.html`
- `entities/mvt-caisse/mvt-caisse.component.html`

---

## 1. Ce que couvre l'`activity-summary`

Le composant `ActivitySummaryComponent` est un **arrêté de caisse étendu** : il agrège en une
seule vue tout ce qui s'est passé financièrement sur une période (dates libres).

### Données présentes

| Section | Champs |
|---|---|
| **Chiffre d'affaires** | montantTtc, montantTva, montantHt, montantRemise, montantNet, montantEspece, montantAutreModePaiement, montantCredit, **marge** |
| **Achats fournisseurs** | montantTtc, montantTva, montantHt (global) |
| **Recettes** | Détail dynamique par mode de paiement + total calculé côté front |
| **Mouvements de caisse** | Libellé + montant par mouvement + total |
| **Achats par groupe fournisseur** | Tableau : libelle, montantTtc, montantTva, montantHt |
| **Règlements tiers-payants** | Tableau : libelle, type, factureNumber, montantFacture, montantReglement, **montantRestant** |
| **Crédits accordés** | Tableau : libelle, categorie, bonsCount, montant, clientCount |

### 4 appels API en parallèle au chargement

```typescript
queryCa(query)                   // → ChiffreAffaire (CA + recettes + mvts caisse)
getGroupeFournisseurAchat(query) // → GroupeFournisseurAchat[]
getReglementTiersPayants(query)  // → ReglementTiersPayant[]
getAchatTiersPayant(query)       // → AchatTiersPayant[]
```

---

## 2. Comparaison avec le Tableau de bord exécutif idéal

### Ce qu'il couvre déjà ✅

- Chiffre d'affaires complet (TTC, HT, TVA, Remises, Net, Marge)
- Répartition des encaissements par mode de paiement
- Achats fournisseurs sur la période
- Mouvements de caisse (entrées/sorties)
- Encours tiers-payants (réglé / restant par facture)
- Crédits accordés aux organismes TP
- Export PDF (Tauri + Web)

### Ce qu'il manque ❌

| Manque | Impact métier |
|---|---|
| Pas d'indicateurs d'évolution (% vs M-1, vs hier) | Impossible de juger si c'est bon ou mauvais |
| Pas de presets rapides (Aujourd'hui / Semaine / Mois) | Saisie manuelle des dates obligatoire |
| Pas de graphiques tendance | Aucune visualisation temporelle |
| Pas d'alertes stock (ruptures, péremptions) | Dimension stock totalement absente |
| Pas de panier moyen ni de nb transactions | Métriques de performance absentes |
| Pas de top familles / top produits | Impossible d'identifier ce qui tire le CA |

### Différence fondamentale de nature

| Critère | activity-summary | Tableau de bord exécutif |
|---|---|---|
| **Question répondue** | Qu'est-ce qui s'est passé ? | Comment se porte l'officine ? |
| **Usage typique** | Arrêté comptable, vérification fin de journée | Pilotage quotidien du titulaire |
| **Granularité** | Période libre (du/au) | Temps réel + comparaison automatique |
| **Dimensions** | CA + Achats + Caisse + TP | CA + Stock + TP + Tendances |

Ils sont **complémentaires, pas redondants** — l'activity-summary est un outil de vérification
comptable, pas un outil de pilotage.

---

## 3. Chevauchements avec `entities/reports`

| Données | activity-summary | entities/reports | Verdict |
|---|---|---|---|
| CA + Marge | ✅ Global (période libre) | ✅ DashboardCAComponent | Complémentaires — DashboardCA est analytique, activity-summary est comptable |
| Recettes par mode de paiement | ✅ Détail par libellé | ✅ DashboardCA (pie chart) | **Doublon partiel** sur les totaux par mode |
| Règlements tiers-payants | ✅ Par facture (restant) | ✅ VieillissementCreances (aging) | Complémentaires — granularités différentes |
| Achats par groupe fournisseur | ✅ Oui | ❌ Absent | **Unique** à activity-summary |
| Crédits accordés TP | ✅ Oui | ❌ Absent | **Unique** à activity-summary |
| Mouvements de caisse | ✅ Oui | ❌ Absent | **Unique** à activity-summary |

---

## 4. Problème architectural : mvt-caisse est trop large

### Contenu actuel du module `mvt-caisse`

```
mvt-caisse.component.html
├── jhi-visualisation-mvt-caisse  → Mouvements de caisse (opérationnel)
├── jhi-balance-mvt-caisse        → Balance caisse (opérationnel)
├── jhi-taxe-report               → Rapport TVA (reporting)
├── jhi-tableau-pharmacien        → Tableau pharmacien (reporting)
├── jhi-recapitualtif-caisse      → Récapitulatif caisse (reporting)
├── jhi-gestion-caisse            → Gestion de caisse (opérationnel)
├── jhi-activity-summary          → Rapport d'activité (reporting) ← mal placé
├── app-declaration-tva           → Déclaration TVA (comptabilité)
└── app-export-comptable          → Export comptable (comptabilité)
```

**Constat** : le module mélange 3 catégories fonctionnelles distinctes dans une seule navigation
verticale. Un titulaire qui cherche son rapport d'activité doit naviguer dans "Gestion de caisse",
ce qui est contre-intuitif.

### Découpage recommandé

```
Module "Caisse" (opérationnel — garder dans mvt-caisse)
├── Mouvements de caisse
├── Balance caisse
└── Gestion de caisse

Module "Finance & Reporting" (déplacer vers entities/reports/finance)
├── Rapport d'activité (activity-summary)   ← nouvel onglet dans reports/finance
├── Récapitulatif caisse
├── Tableau pharmacien
└── Rapport TVA

Module "Comptabilité" (nouveau module dédié ou conserver séparément)
├── Déclaration TVA
└── Export comptable
```

---

## 5. Recommandation : enrichir l'activity-summary plutôt que créer un doublon

Plutôt que de construire un nouveau "tableau de bord exécutif" from scratch dans `entities/reports`,
il serait plus efficace d'**enrichir l'`activity-summary`** avec :

1. **Bande de KPIs comparatifs** en haut : CA aujourd'hui vs hier, CA mois vs M-1 (avec %)
2. **Presets de période** : Aujourd'hui / Cette semaine / Ce mois / Personnalisée
3. **Compteur d'alertes stock** (badge) : nb ruptures + nb produits périmant dans 30j
4. **Panier moyen + nb transactions** dans la card Chiffre d'affaires
5. **Déplacement** du composant vers `entities/reports/finance` ou en tant que route racine des reports

Cela évite la duplication et donne au composant existant la visibilité qu'il mérite.

---

## 6. Résumé des actions recommandées

| Priorité | Action | Effort |
|---|---|---|
| Haute | Déplacer `activity-summary` de `mvt-caisse` vers `reports/finance` | Faible (changement de route) |
| Haute | Ajouter presets de période (Aujourd'hui, Semaine, Mois) | Faible |
| Haute | Ajouter % évolution vs période précédente sur la card CA | Moyen |
| Moyenne | Ajouter compteur d'alertes stock (badge + lien vers stock-alerts) | Moyen |
| Moyenne | Ajouter panier moyen + nb transactions dans la card CA | Faible |
| Basse | Extraire Déclaration TVA + Export comptable dans un module comptabilité dédié | Fort |
