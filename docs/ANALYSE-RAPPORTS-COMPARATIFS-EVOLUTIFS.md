# Analyse — Rapports Comparatifs & Évolutifs (Statistiques dans le temps)

## 1. Ce que proposent les autres logiciels

### 1.1 Standard du marché (présent dans presque tous les logiciels)

| Fonctionnalité | Logiciels représentatifs | Description |
|---|---|---|
| **CA N vs N-1 global** | Winpharma, Pharmagest, LGPI | Comparaison mensuelle/trimestrielle/annuelle du CA entre deux années |
| **Évolution panier moyen** | Winpharma, LGPI, Vega | Panier moyen mensuel sur 12 à 24 mois — indicateur de montée en gamme |
| **Trafic transactionnel** | Pharmagest, Everys | Nombre de transactions par heure × jour de semaine (matrice) |
| **Top produits avec tendance** | Tous | Produit top ce mois vs. mois précédent (+ delta rang) |
| **CA par rayon/famille N vs N-1** | Winpharma, LGPI, Pharmagest | Comparaison par famille de produit entre deux périodes |
| **Glissement annuel 12 mois** | Winpharma, Pharmagest | Cumul mobile 12 mois — lisse les effets de saisonnalité |
| **YTD (Year-To-Date)** | Tous | Cumul depuis le 1er janvier vs. même cumul N-1 |

### 1.2 Avancé — différenciateurs des meilleures solutions

| Fonctionnalité | Logiciels représentatifs | Description |
|---|---|---|
| **Heat map saisonnière** | Pharmagest iSYPHAX, Officine Insight | Matrice produit/famille × mois (couleur = intensité des ventes) |
| **Drill-down comparatif multi-niveaux** | LGPI G5, Pharmavitale | Famille → Sous-famille → Fournisseur → Produit avec la même vue N vs N-1 |
| **Graphe multi-axes** | Everys, Pharmavitale | CA + nombre de transactions + marge brute superposés sur un seul graphe |
| **Moyenne mobile glissante** | LGPI G5, Officine Insight | Overlay MM4 (4 semaines) ou MM3 (3 mois) sur le graphe historique — élimine le bruit |
| **Comparaison 3+ périodes** | Pharmagest iSYPHAX | Comparaison simultanée de 3 années sur un même graphe en barres groupées |
| **Trend TP vs. comptant** | LGPI G5 | Évolution de la part du tiers payant dans le CA total sur 12–24 mois |
| **Performance fournisseur dans le temps** | Winpharma, LGPI G5 | Coût moyen d'achat × mois, taux de service livraison, délai moyen |
| **Tendance marges par famille** | Officine Insight, Pharmavitale | Marge brute % par famille de produit sur 6–12 mois |

---

## 2. État des lieux Pharma-Smart

### 2.1 Ce qui existe déjà (à ne pas redévelopper)

| Rapport | Chemin | Capacités comparatives existantes | Limite |
|---|---|---|---|
| **Comparative Analysis** | `entities/reports/comparative-analysis` | CA N vs N-1 mensuel/trimestriel/annuel ; comparaison par type de vente (VO/VNO/Dépôt) | Global uniquement — pas de dimension famille/fournisseur |
| **Dashboard CA** | `entities/reports/dashboard-ca` | Comparaisons J/J-1, S/S-1, M/M-1, A/A-1 ; top 10 familles (snapshot) ; graphe journalier CA | Pas de comparaison multi-périodes sur les familles ; vue snapshot seulement |
| **Sales Forecast** | `entities/reports/sales-forecast` | Régression linéaire, moyenne mobile, saisonnalité ; IC 95% ; horizon 3/6/12 mois | Prévision globale uniquement ; pas de prévision par famille/fournisseur |
| **Top Products** | `entities/reports/top-products` | Top N par CA ou quantité pour un mois donné | Un seul mois à la fois ; pas de delta vs mois précédent, pas de tendance |
| **Sales Summary** | `entities/reports/sales-summary` | Détail quotidien par type de vente sur une plage | Pas de comparaison, pas de tendance |
| **Profitability Analysis** | `entities/reports/profitability-analysis` | Marge brute par famille pour une période | Snapshot — pas d'évolution dans le temps |
| **Supplier Performance** | `entities/reports/supplier-performance` | Performance fournisseur (taux de service, délais) | Snapshot — pas d'évolution mensuelle |

### 2.2 Ce qui manque — GAPs comparatifs/évolutifs

| GAP | Description | Priorité | Complexité backend | Complexité frontend |
|---|---|:---:|:---:|:---:|
| **GAP-C1** | N vs N-1 par famille de produit | 🔴 Haute | Faible (query SQL agrégée) | Moyenne (graphe grouped bars) |
| **GAP-C2** | Évolution du panier moyen sur 12–24 mois | 🔴 Haute | Faible | Faible (courbe simple) |
| **GAP-C3** | Évolution part TP vs. comptant dans le CA (12 mois) | 🔴 Haute | Faible | Faible (aire empilée) |
| **GAP-C4** | Tendance marge brute par famille (6–12 mois) | 🟡 Moyenne | Moyenne | Moyenne |
| **GAP-C5** | Heat map trafic transactionnel (heure × jour) | 🟡 Moyenne | Moyenne | Haute (composant matrice) |
| **GAP-C6** | Top produits avec delta rang vs. période précédente | 🟡 Moyenne | Faible | Faible |
| **GAP-C7** | N vs N-1 par fournisseur (CA, coût, marge) | 🟡 Moyenne | Moyenne | Moyenne |
| **GAP-C8** | Comparaison 3 années simultanées (barres groupées) | 🟠 Basse | Faible | Moyenne |
| **GAP-C9** | Overlay moyenne mobile 3/6 mois sur historique | 🟠 Basse | Faible | Moyenne |
| **GAP-C10** | Drill-down famille → produit dans vue comparative | 🟠 Basse | Faible | Haute |

---

## 3. Analyse des 3 GAPs prioritaires

### GAP-C1 — N vs N-1 par famille de produit

**Besoin** : Aujourd'hui `Comparative Analysis` compare uniquement le CA global et par type de vente (VO/VNO). La pharmacie ne peut pas répondre à "Est-ce que mes ventes d'antipaludéens ont progressé cette année ?".

**Ce que proposent les concurrents** : Winpharma, LGPI et Pharmagest proposent tous une vue "par rayon/famille" en N vs N-1, considérée comme standard depuis 5+ ans.

**Données disponibles** : `sales_line.product_id → produit.famille_id → famille.libelle` — tout est présent.

**Intégration proposée** : Nouvel onglet dans le conteneur `Comparative Analysis` existant (pattern tab déjà en place), pas un nouveau rapport. Toggle "Vue globale / Vue famille".

```
┌─────────────────────────────────────────────────────────────────┐
│  ANALYSE COMPARATIVE                [Année ▼] [Mois/Trim/An ▼] │
│  Vue : [Globale ●]  [Par famille ○]  [Par fournisseur ○]        │
├──────────────────────────────────────┬──────────────────────────┤
│  Famille               N (2026)      │  N-1 (2025)   Évol.      │
│  Antibiotiques         8,2M F        │  7,1M F       +15,5% ↗   │
│  Antipaludéens         3,9M F        │  4,3M F       −9,3%  ↘   │
│  Antalgiques           5,6M F        │  5,4M F       +3,7%  →   │
│  Dermatologie          3,2M F        │  2,8M F       +14,3% ↗   │
│  [Voir les 40 familles]              │  [tri Évol. ▼]           │
└─────────────────────────────────────────────────────────────────┘
```

---

### GAP-C2 — Évolution du panier moyen sur 12–24 mois

**Besoin** : Le panier moyen est un KPI de montée en gamme et d'efficacité commerciale. Une tendance à la baisse sur 6 mois signal un problème (substitutions vers moins cher, perte de patients fidèles, pression des génériques).

**Données disponibles** : `SUM(montant_net) / COUNT(DISTINCT sale_id)` par mois — calculable directement.

**Intégration proposée** : Nouvelle carte dans `Dashboard CA` (section "Tendances") + courbe 12 mois.

```
┌─────────────────────────────────────┐
│  PANIER MOYEN  — 12 mois            │
│  Ce mois : 4 850 F  ↗ +220 F vs M-1│
│  [■■■■■■■■■■■■] courbe 12 mois      │
│  Meilleur mois : Nov 2025 (5 120 F) │
│  Tendance 6M : +3,2% annualisé      │
└─────────────────────────────────────┘
```

---

### GAP-C3 — Évolution part TP vs. comptant dans le CA

**Besoin** : Dans le contexte africain (CNAM, INAM, mutuelles entreprises), la proportion du tiers payant dans le CA est un indicateur de dépendance institutionnelle et de risque de trésorerie. Une montée du TP non accompagnée d'un bon DSO dégrade le BFR.

**Données disponibles** : `type_vente = 'VO' (TP) vs 'VNO' (comptant)` par mois — déjà disponible dans `Comparative Analysis`.

**Intégration proposée** : Graphe en aires empilées (stacked area) dans le rapport `Vieillissement Créances` (section tendance) ou dans `Comparative Analysis`.

```
  CA mensuel par mode — 12 mois
  ████████████████   100%
  ██TP  ██Comptant
  Janv : 62% TP / 38% Comptant
  Déc  : 68% TP / 32% Comptant  ← dépendance croissante ↗
```

---

## 4. Roadmap recommandée

### Phase 1 — Améliorations des rapports existants (faible effort, fort impact)

Ces items sont des **améliorations de rapports déjà en production** — pas de nouveaux conteneurs.

| Item | Rapport cible | Effort estimé |
|---|---|---|
| GAP-C1 : Vue famille dans Comparative Analysis | `comparative-analysis` (nouvel onglet) | 3–5 jours |
| GAP-C2 : Panier moyen 12 mois | `dashboard-ca` (nouvelle carte) | 2 jours |
| GAP-C3 : Part TP vs. comptant dans le temps | `comparative-analysis` ou `vieillissement-creances` | 2 jours |
| GAP-C6 : Delta rang dans Top Products | `top-products` (colonne delta) | 1 jour |

### Phase 2 — Nouveaux rapports analytiques

| Item | Rapport | Effort estimé |
|---|---|---|
| GAP-C4 : Tendance marge brute par famille | Nouveau dans `/finance` | 5–7 jours |
| GAP-C7 : N vs N-1 par fournisseur | Amélioration `supplier-performance` | 3–5 jours |
| GAP-C9 : Overlay moyenne mobile | Amélioration `comparative-analysis` | 2 jours |

### Phase 3 — Backlog (complexité élevée)

| Item | Rapport | Blocage |
|---|---|---|
| GAP-C5 : Heat map trafic | Nouveau | Composant matrice à développer |
| GAP-C8 : Comparaison 3 années | `comparative-analysis` | Refonte du modèle de données du graphe |
| GAP-C10 : Drill-down famille → produit | `comparative-analysis` | Routing imbriqué + query côté back |

---

## 5. Synthèse — Positionnement Pharma-Smart

```
                    COUVERTURE COMPARATIVE & ÉVOLUTIVE
                    
  Fonctionnalité                   Marché   Pharma-Smart  
  ────────────────────────────────────────────────────────
  CA global N vs N-1               ✅ Std   ✅ Fait
  YTD et glissement 12 mois        ✅ Std   ✅ Fait (Dashboard CA)
  Prévisions avec tendance         ✅ Std   ✅ Fait (3 algos + IC)
  Comparaison par type de vente    ✅ Std   ✅ Fait (VO/VNO/Dépôt)
  ────────────────────────────────────────────────────────
  N vs N-1 par famille             ✅ Std   ❌ GAP-C1 (priorité 1)
  Évolution panier moyen           ✅ Std   ❌ GAP-C2 (priorité 1)
  Part TP vs. comptant dans temps  ✅ Std   ❌ GAP-C3 (priorité 1)
  Delta rang Top Produits          ✅ Std   ❌ GAP-C6 (facile)
  ────────────────────────────────────────────────────────
  Tendance marge par famille       🔵 Avancé ❌ GAP-C4 (priorité 2)
  N vs N-1 par fournisseur         🔵 Avancé ❌ GAP-C7 (priorité 2)
  Heat map trafic                  🔵 Avancé ❌ GAP-C5 (backlog)
  Graphe multi-axes                🔵 Avancé ❌ GAP-C9 (backlog)
  Drill-down comparatif            🔵 Avancé ❌ GAP-C10 (backlog)
```

**Conclusion** : Pharma-Smart couvre correctement les comparaisons globales et les prévisions, mais manque des vues comparatives **dimensionnées** (par famille, par fournisseur) qui sont désormais standard. Les 4 items de Phase 1 (GAP-C1/C2/C3/C6) représentent environ 8–10 jours de développement et comblent l'essentiel du retard marché.
