# Workflow complet : Architect → Code Writer → Code Reviewer

## Phase 1 : Fondations Essentielles

### 🎯 Objectif

Mettre en place les rapports indispensables au pilotage quotidien de la pharmacie.

### Rapports à Implémenter

#### 1.1 Dashboard Principal (Tableau de Bord CA)

**Priorité : P0 - Critique**

**Fonctionnalités :**

- CA global par période (jour, semaine, mois, année)
- Évolution du CA avec graphique de tendance (courbe)
- Comparaison avec période précédente (% variation)
- CA par type de vente (VNO, VO, ) - Graphique en barres
- CA par mode de paiement - Graphique circulaire

**Endpoints REST :**

```java
GET /api/dashboard/ca?startDate=2024-01-01&endDate=2024-12-31
GET /api/dashboard/ca-by-type-vente?period=MONTH
GET /api/dashboard/ca-by-mode-paiment?period=WEEK
GET /api/dashboard/ca-by-periode?groupBy=DAY&startDate=...
```

**Technologies :**

- **Backend** : Services existants dans
  `D:\projet\full_warehouse\src\main\java\com\kobe\warehouse\service\report` (déjà partiellement
  implémenté)
- **Frontend** : Chart.js pour les graphiques
- **Optimisation** : Cache Caffeine (15 min TTL pour les stats du jour)

**Composants Angular :**

```typescript
// dashboard/
├── dashboard.component.ts
├── ca - summary - card.component.ts
├── ca - trend - chart.component.ts
├── ca - by - type - chart.component.ts
└── ca - by - payment - chart.component.ts
// rapport//
D:\
projet\full_warehouse\src\main\webapp\app\entities\reports

```

---

#### 1.2 Gestion de Caisse

**Priorité : P0 - Critique**

**Rapports :**

- **État journalier des caisses**
  - Solde d'ouverture/clôture
  - Encaissements par mode de paiement
  - Écarts de caisse (alertes si > seuil)
  - Nombre de transactions

- **Historique des mouvements de caisse**
  - Filtres : date, utilisateur, caisse

**Modèle de données :**
Utiliser les entités existantes : `CashRegister`, `CashRegisterItem`, `CashFund`

**Endpoints REST :**

```java
GET /api/cash-register/daily-report?date=2024-01-15
GET /api/cash-register/movements?startDate=...&endDate=...&userId=...
GET /api/cash-register/summary?period=WEEK
```

---

#### 1.3 Stock Critique (Alertes)

**Priorité : P0 - Critique**

**Rapports :**

- **Produits en rupture de stock** (quantité = 0)
- **Produits en alerte stock** (quantité < seuil minimum)
- **Produits proches de péremption** (< 3 mois)

**Endpoints REST :**

```java
GET /api/stock/alerts?type=RUPTURE,ALERTE,PEREMPTION
GET /api/stock/alerts/count
GET /api/stock/alerts/export?format=PDF
```

---

#### 1.4 Facturation Tiers-Payants (Créances)

**Priorité : P1 - Important**

**Rapports :**

- **Factures en attente de règlement** par organisme
- **Créances par ancienneté** (< 30j, 30-60j, 60-90j, > 90j)
- **Montant total des impayés**
- **Historique des paiements reçus**

---

### 📦 Livrables Phase 1

- ✅ Dashboard CA opérationnel avec 4 widgets
- ✅ Rapport journalier de caisse automatique
- ✅ Système d'alertes stock (ruptures, péremptions)
- ✅ Tableau de bord créances tiers-payants

---
