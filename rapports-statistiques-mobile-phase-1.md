## Phase 1 : Fondations Essentielles
**Durée estimée : 2-3 sprints | Priorité : Critique**

### 🎯 Objectif
Mettre en place les rapports indispensables au pilotage quotidien de la pharmacie.

### Rapports à Implémenter

#### 1.1 Dashboard Principal (Tableau de Bord CA)
**Priorité : P0 - Critique**

**Fonctionnalités :**
- CA global par période (jour, semaine, mois, année)
- Évolution du CA avec graphique de tendance (courbe)
- Comparaison avec période précédente (% variation)
- CA par type de vente (VNO, VO, VE, VA) - Graphique en barres
- CA par mode de paiement - Graphique circulaire

**Endpoints REST :**
```java
GET /api/dashboard/ca?startDate=2024-01-01&endDate=2024-12-31
GET /api/dashboard/ca-by-type-vente?period=MONTH
GET /api/dashboard/ca-by-mode-paiment?period=WEEK
GET /api/dashboard/ca-by-periode?groupBy=DAY&startDate=...
```

**Technologies :**
- **Backend** : Services existants dans `DashboardService` (déjà partiellement implémenté)
- **Frontend** : Chart.js pour les graphiques
- **Optimisation** : Cache Caffeine (15 min TTL pour les stats du jour)

**Composants Angular :**
```typescript
// dashboard/
├── dashboard.component.ts
├── ca-summary-card.component.ts
├── ca-trend-chart.component.ts
├── ca-by-type-chart.component.ts
└── ca-by-payment-chart.component.ts
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

**Requête SQL optimisée :**
```sql
-- Vue matérialisée pour alertes stock
CREATE MATERIALIZED VIEW mv_stock_alerts AS
SELECT
    p.id,
    p.libelle,
    sp.stock_quantity,
    sp.seuil_min,
    l.expiry_date,
    CASE
        WHEN sp.stock_quantity = 0 THEN 'RUPTURE'
        WHEN sp.stock_quantity < sp.seuil_min THEN 'ALERTE'
        WHEN l.expiry_date < CURRENT_DATE + INTERVAL '3 months' THEN 'PEREMPTION'
    END as alert_type
FROM produit p
INNER JOIN stock_produit sp ON p.id = sp.produit_id
LEFT JOIN lot l ON sp.produit_id = l.produit_id
WHERE sp.stock_quantity = 0
   OR sp.stock_quantity < sp.seuil_min
   OR l.expiry_date < CURRENT_DATE + INTERVAL '3 months';

-- Rafraîchir toutes les 30 minutes
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_stock_alerts;
```

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

**Requête SQL :**
```sql
SELECT
    gtp.libelle as tiers_payant,
    COUNT(f.id) as nb_factures,
    SUM(f.montant_facture) as montant_total,
    SUM(CASE WHEN f.date_facture > CURRENT_DATE - 30 THEN f.montant_facture ELSE 0 END) as moins_30j,
    SUM(CASE WHEN f.date_facture BETWEEN CURRENT_DATE - 60 AND CURRENT_DATE - 30 THEN f.montant_facture ELSE 0 END) as entre_30_60j,
    SUM(CASE WHEN f.date_facture BETWEEN CURRENT_DATE - 90 AND CURRENT_DATE - 60 THEN f.montant_facture ELSE 0 END) as entre_60_90j,
    SUM(CASE WHEN f.date_facture < CURRENT_DATE - 90 THEN f.montant_facture ELSE 0 END) as plus_90j
FROM facture_tiers_payant f
INNER JOIN groupe_tiers_payant gtp ON f.groupe_tiers_payant_id = gtp.id
WHERE f.statut = 'UNPAID'
GROUP BY gtp.id, gtp.libelle
ORDER BY montant_total DESC;
```

**Export PDF :**
Utiliser Thymeleaf + Flying Saucer (déjà configuré)

---

### 📦 Livrables Phase 1

- ✅ Dashboard CA opérationnel avec 4 widgets
- ✅ Rapport journalier de caisse automatique
- ✅ Système d'alertes stock (ruptures, péremptions)
- ✅ Tableau de bord créances tiers-payants
- ✅ Exports PDF pour tous les rapports

---
