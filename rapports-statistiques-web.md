# Rapports Statistiques - Application Web
## Pharma-Smart Warehouse Management System

---

## 📋 Table des Matières

1. [Introduction](#introduction)
2. [Architecture Technique Recommandée](#architecture-technique)
3. [Phase 1 : Fondations Essentielles](#phase-1)
4. [Phase 2 : Optimisation Opérationnelle](#phase-2)
5. [Phase 3 : Intelligence Décisionnelle](#phase-3)
6. [Phase 4 : Analytics Avancés](#phase-4)
7. [Stack Technologique](#stack-technologique)
8. [Bonnes Pratiques](#bonnes-pratiques)

---

## Introduction

Ce document définit la roadmap des rapports statistiques pour l'application web Pharma-Smart. Les rapports sont organisés en 4 phases selon leur **priorité business** et leur **complexité technique**.

### Objectifs
- **Visibilité** : Donner une vue complète de l'activité de la pharmacie
- **Décision** : Fournir des indicateurs pour la prise de décision
- **Conformité** : Assurer le suivi réglementaire
- **Optimisation** : Identifier les axes d'amélioration

---

## Architecture Technique Recommandée

### Architecture Globale

```
┌─────────────────────────────────────────────────────────┐
│                  Frontend (Angular 20)                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  Dashboard   │  │   Reports    │  │   Exports    │  │
│  │  Components  │  │  Components  │  │   Service    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                           ↓ REST API
┌─────────────────────────────────────────────────────────┐
│              Backend (Spring Boot 4.0.0)                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Report     │  │    Stats     │  │    Export    │  │
│  │  Resources   │  │   Services   │  │   Services   │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│          PostgreSQL + Vues Matérialisées                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  Stats Views │  │  Aggregates  │  │   Indexes    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

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

## Phase 2 : Optimisation Opérationnelle
**Durée estimée : 3-4 sprints | Priorité : Important**

### 🎯 Objectif
Ajouter des rapports pour optimiser la gestion des stocks, des ventes et des fournisseurs.

### Rapports à Implémenter

#### 2.1 Analyse des Ventes
**Priorité : P1 - Important**

**Rapports :**
- **Top produits vendus** (Top 20 par quantité et CA)
  - Filtres : période, type de vente, catégorie
- **Produits à rotation lente** (< 5 ventes/mois)
- **Panier moyen** (évolution par période)
- **Taux de remise moyen** par type de vente

**Vue matérialisée pour performance :**
```sql
CREATE MATERIALIZED VIEW mv_produits_top_ventes AS
SELECT
    p.id,
    p.libelle,
    p.code_cip,
    COUNT(DISTINCT sl.sale_id) as nb_ventes,
    SUM(sl.quantity_sold) as quantite_vendue,
    SUM(sl.sales_amount) as ca_genere,
    AVG(sl.sales_amount / sl.quantity_sold) as prix_moyen
FROM produit p
INNER JOIN sales_line sl ON p.id = sl.produit_id
INNER JOIN sales s ON sl.sale_id = s.id
WHERE s.sale_date >= CURRENT_DATE - INTERVAL '30 days'
  AND s.statut = 'CLOSED'
GROUP BY p.id, p.libelle, p.code_cip
ORDER BY ca_genere DESC;
```

**Composant Angular :**
```typescript
@Component({
  selector: 'jhi-top-products-report',
  template: `
    <p-table [value]="products" [paginator]="true" [rows]="20">
      <ng-template #header>
        <tr>
          <th>Produit</th>
          <th>Qté vendue</th>
          <th>CA généré</th>
          <th>Nb ventes</th>
        </tr>
      </ng-template>
      <ng-template #body let-product>
        <tr>
          <td>{{ product.libelle }}</td>
          <td>{{ product.quantiteVendue }}</td>
          <td>{{ product.caGenere | currency:'XOF' }}</td>
          <td>{{ product.nbVentes }}</td>
        </tr>
      </ng-template>
    </p-table>
  `
})
export class TopProductsReportComponent implements OnInit {
  products: ProductSalesDTO[] = [];

  ngOnInit() {
    this.reportService.getTopProducts({ period: 'MONTH', limit: 20 })
      .subscribe(data => this.products = data);
  }
}
```

---

#### 2.2 Gestion de Stock Avancée
**Priorité : P1 - Important**

**Rapports :**
- **Valeur totale du stock** (prix d'achat vs prix de vente)
- **Taux de rotation des stocks** par catégorie
- **Produits en surstockage** (stock > seuil maximum)
- **Historique des ajustements** (inventaires, casse, vol)

**KPIs à calculer :**
```sql
-- Taux de rotation stock (formule : CA / Stock moyen)
SELECT
    c.libelle as categorie,
    SUM(sp.stock_quantity * sp.cost_amount) as valeur_stock,
    SUM(ventes_30j.ca) as ca_30j,
    CASE
        WHEN SUM(sp.stock_quantity * sp.cost_amount) > 0
        THEN ROUND((SUM(ventes_30j.ca) / SUM(sp.stock_quantity * sp.cost_amount)) * 12, 2)
        ELSE 0
    END as taux_rotation_annuel
FROM categorie c
INNER JOIN produit p ON c.id = p.categorie_id
INNER JOIN stock_produit sp ON p.id = sp.produit_id
LEFT JOIN (
    SELECT
        sl.produit_id,
        SUM(sl.sales_amount) as ca
    FROM sales_line sl
    INNER JOIN sales s ON sl.sale_id = s.id
    WHERE s.sale_date >= CURRENT_DATE - 30
    GROUP BY sl.produit_id
) ventes_30j ON p.id = ventes_30j.produit_id
GROUP BY c.id, c.libelle
ORDER BY taux_rotation_annuel DESC;
```

---

#### 2.3 Performance Fournisseurs
**Priorité : P2 - Souhaitable**

**Rapports :**
- **Volume d'achats par fournisseur** (30 derniers jours, 12 derniers mois)
- **Délai moyen de livraison**
- **Taux de conformité des livraisons**
- **Historique des prix** (évolution par produit)

**Service Java :**
```java
@Service
public class FournisseurReportService {

    public List<FournisseurPerformanceDTO> getPerformanceReport(
        LocalDate startDate,
        LocalDate endDate
    ) {
        return fournisseurRepository.findPerformanceMetrics(startDate, endDate);
    }

    // Calcul délai moyen de livraison
    public Map<Long, Double> getAverageDeliveryTime(LocalDate startDate, LocalDate endDate) {
        List<Commande> commandes = commandeRepository
            .findByCreatedDateBetween(startDate, endDate);

        return commandes.stream()
            .filter(c -> c.getStatut() == CommandeStatut.RECEIVED)
            .collect(Collectors.groupingBy(
                c -> c.getFournisseur().getId(),
                Collectors.averagingLong(c ->
                    ChronoUnit.DAYS.between(c.getCreatedDate(), c.getReceivedDate())
                )
            ));
    }
}
```

---

#### 2.4 Analyse Clients
**Priorité : P2 - Souhaitable**

**Rapports :**
- **Segmentation clients** (actifs, inactifs, nouveaux)
- **Clients fidèles** (> 10 achats/an)
- **Clients à risque** (inactifs > 90 jours)
- **CA moyen par client**

**Segmentation RFM (Récence, Fréquence, Montant) :**
```sql
WITH customer_metrics AS (
    SELECT
        c.id,
        c.first_name || ' ' || c.last_name as nom_complet,
        COUNT(DISTINCT s.id) as nb_achats,
        SUM(s.sales_amount) as ca_total,
        MAX(s.sale_date) as derniere_vente,
        CURRENT_DATE - MAX(s.sale_date) as jours_depuis_derniere_vente
    FROM customer c
    LEFT JOIN sales s ON c.id = s.customer_id
    WHERE s.sale_date >= CURRENT_DATE - INTERVAL '1 year'
    GROUP BY c.id
)
SELECT
    *,
    CASE
        WHEN jours_depuis_derniere_vente <= 30 THEN 'Actif'
        WHEN jours_depuis_derniere_vente BETWEEN 31 AND 90 THEN 'Risque'
        ELSE 'Inactif'
    END as segment,
    CASE
        WHEN nb_achats >= 10 THEN 'Fidèle'
        WHEN nb_achats >= 5 THEN 'Régulier'
        ELSE 'Occasionnel'
    END as fidelite
FROM customer_metrics
ORDER BY ca_total DESC;
```

---

### 📦 Livrables Phase 2

- ✅ Rapport top produits avec filtres avancés
- ✅ Dashboard stock avec taux de rotation
- ✅ Tableau de bord performance fournisseurs
- ✅ Segmentation client RFM
- ✅ Exports Excel pour analyse approfondie

---

## Phase 3 : Intelligence Décisionnelle
**Durée estimée : 4-5 sprints | Priorité : Optimisation**

### 🎯 Objectif
Ajouter des analyses avancées pour la prise de décision stratégique.

### Rapports à Implémenter

#### 3.1 Analyse de Rentabilité
**Priorité : P1 - Important**

**Rapports :**
- **Marge brute globale et par produit**
- **Top 20 produits les plus rentables**
- **Produits à faible marge** (< 10%)
- **Ratio marge/volume** (matrice BCG)

**Calcul de marge :**
```sql
SELECT
    p.libelle,
    SUM(sl.quantity_sold) as qte_vendue,
    SUM(sl.sales_amount) as ca,
    SUM(sl.cost_amount) as cout_achat,
    SUM(sl.sales_amount - sl.cost_amount) as marge_brute,
    ROUND(
        (SUM(sl.sales_amount - sl.cost_amount) / NULLIF(SUM(sl.sales_amount), 0)) * 100,
        2
    ) as taux_marge_pct
FROM produit p
INNER JOIN sales_line sl ON p.id = sl.produit_id
INNER JOIN sales s ON sl.sale_id = s.id
WHERE s.sale_date BETWEEN :startDate AND :endDate
  AND s.statut = 'CLOSED'
GROUP BY p.id, p.libelle
HAVING SUM(sl.sales_amount) > 0
ORDER BY marge_brute DESC
LIMIT 20;
```

**Matrice BCG (Boston Consulting Group) :**
Classifier les produits en 4 quadrants :
- **Stars** : Forte marge + Forte rotation
- **Cash Cows** : Forte marge + Faible rotation
- **Question Marks** : Faible marge + Forte rotation
- **Dogs** : Faible marge + Faible rotation

**Visualisation Chart.js :**
```typescript
const bcgChartConfig = {
  type: 'scatter',
  data: {
    datasets: [{
      label: 'Stars',
      data: products.filter(p => p.margin > 20 && p.rotation > 6),
      backgroundColor: 'green'
    }, {
      label: 'Cash Cows',
      data: products.filter(p => p.margin > 20 && p.rotation <= 6),
      backgroundColor: 'blue'
    }, {
      label: 'Question Marks',
      data: products.filter(p => p.margin <= 20 && p.rotation > 6),
      backgroundColor: 'orange'
    }, {
      label: 'Dogs',
      data: products.filter(p => p.margin <= 20 && p.rotation <= 6),
      backgroundColor: 'red'
    }]
  },
  options: {
    scales: {
      x: { title: { display: true, text: 'Taux de rotation' } },
      y: { title: { display: true, text: 'Marge (%)' } }
    }
  }
};
```

---

#### 3.2 Analyse ABC (Pareto)
**Priorité : P1 - Important**

**Objectif :** Classifier les produits selon la loi 80/20
- **Classe A** : 80% du CA (produits stratégiques)
- **Classe B** : 15% du CA (produits importants)
- **Classe C** : 5% du CA (produits secondaires)

**Algorithme :**
```java
@Service
public class ABCAnalysisService {

    public List<ProductABCClassification> performABCAnalysis(
        LocalDate startDate,
        LocalDate endDate
    ) {
        // 1. Récupérer tous les produits avec leur CA
        List<ProductSalesDTO> products = salesRepository
            .findProductSalesByPeriod(startDate, endDate);

        // 2. Trier par CA décroissant
        products.sort(Comparator.comparing(
            ProductSalesDTO::getSalesAmount).reversed()
        );

        // 3. Calculer le CA total
        double totalCA = products.stream()
            .mapToDouble(ProductSalesDTO::getSalesAmount)
            .sum();

        // 4. Classifier les produits
        double cumulativeCA = 0;
        List<ProductABCClassification> result = new ArrayList<>();

        for (ProductSalesDTO product : products) {
            cumulativeCA += product.getSalesAmount();
            double cumulativePercent = (cumulativeCA / totalCA) * 100;

            String classe;
            if (cumulativePercent <= 80) {
                classe = "A";
            } else if (cumulativePercent <= 95) {
                classe = "B";
            } else {
                classe = "C";
            }

            result.add(new ProductABCClassification(
                product.getProductId(),
                product.getLibelle(),
                product.getSalesAmount(),
                cumulativePercent,
                classe
            ));
        }

        return result;
    }
}
```

**Endpoint REST :**
```java
@GetMapping("/api/reports/abc-analysis")
public ResponseEntity<List<ProductABCClassification>> getABCAnalysis(
    @RequestParam LocalDate startDate,
    @RequestParam LocalDate endDate
) {
    return ResponseEntity.ok(abcAnalysisService.performABCAnalysis(startDate, endDate));
}
```

---

#### 3.3 Tableaux Comparatifs
**Priorité : P2 - Souhaitable**

**Rapports :**
- **Comparaison CA année N vs N-1**
- **Comparaison mois vs mois** (évolution mensuelle)
- **Comparaison par type de vente** (VNO, VO, VE)

**Requête SQL optimisée :**
```sql
WITH current_year AS (
    SELECT
        EXTRACT(MONTH FROM sale_date) as mois,
        SUM(sales_amount) as ca
    FROM sales
    WHERE EXTRACT(YEAR FROM sale_date) = EXTRACT(YEAR FROM CURRENT_DATE)
      AND statut = 'CLOSED'
    GROUP BY EXTRACT(MONTH FROM sale_date)
),
previous_year AS (
    SELECT
        EXTRACT(MONTH FROM sale_date) as mois,
        SUM(sales_amount) as ca
    FROM sales
    WHERE EXTRACT(YEAR FROM sale_date) = EXTRACT(YEAR FROM CURRENT_DATE) - 1
      AND statut = 'CLOSED'
    GROUP BY EXTRACT(MONTH FROM sale_date)
)
SELECT
    cy.mois,
    cy.ca as ca_annee_courante,
    py.ca as ca_annee_precedente,
    cy.ca - py.ca as difference,
    ROUND(((cy.ca - py.ca) / NULLIF(py.ca, 0)) * 100, 2) as evolution_pct
FROM current_year cy
LEFT JOIN previous_year py ON cy.mois = py.mois
ORDER BY cy.mois;
```

---

#### 3.4 Rapports de Conformité
**Priorité : P2 - Souhaitable**

**Rapports réglementaires :**
- **Ventes de stupéfiants** (traçabilité obligatoire)
- **Ventes à prescription obligatoire**
- **Traçabilité des lots vendus**
- **Produits sous surveillance** (psychotropes)

**Service de conformité :**
```java
@Service
public class ComplianceReportService {

    public List<ControlledSubstanceSaleDTO> getControlledSubstanceSales(
        LocalDate startDate,
        LocalDate endDate
    ) {
        return salesRepository.findControlledSubstanceSales(
            startDate,
            endDate,
            List.of(
                TypePrescription.STUPEFIANT,
                TypePrescription.PSYCHOTROPE
            )
        );
    }

    // Export PDF avec Thymeleaf pour autorités
    public byte[] generateComplianceReport(LocalDate startDate, LocalDate endDate) {
        List<ControlledSubstanceSaleDTO> sales =
            getControlledSubstanceSales(startDate, endDate);

        Context context = new Context();
        context.setVariable("sales", sales);
        context.setVariable("startDate", startDate);
        context.setVariable("endDate", endDate);

        String html = templateEngine.process("reports/compliance-report", context);
        return pdfService.generatePdf(html);
    }
}
```

---

### 📦 Livrables Phase 3

- ✅ Dashboard rentabilité avec matrice BCG
- ✅ Analyse ABC automatisée
- ✅ Tableaux comparatifs année N vs N-1
- ✅ Module de conformité réglementaire
- ✅ Exports PDF certifiés pour autorités

---

## Phase 4 : Analytics Avancés
**Durée estimée : 5-6 sprints | Priorité : Innovation**

### 🎯 Objectif
Intégrer des analyses prédictives et des tableaux de bord personnalisables.

### Rapports à Implémenter

#### 4.1 Prévisions de Ventes (Machine Learning)
**Priorité : P2 - Innovation**

**Fonctionnalités :**
- **Prévisions de CA** sur 3/6/12 mois
- **Détection de saisonnalité** (produits saisonniers)
- **Prévisions de besoins en stock**

**Approche technique :**

**Option 1 : Modèle simple (Moyenne mobile + Tendance linéaire)**
```java
@Service
public class ForecastingService {

    // Prévision simple par régression linéaire
    public List<SalesForecastDTO> forecastSales(
        Long productId,
        int monthsAhead
    ) {
        // 1. Récupérer historique 12 derniers mois
        List<MonthlySales> history = salesRepository
            .getMonthlyProductSales(productId, 12);

        // 2. Calculer la tendance (régression linéaire simple)
        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < history.size(); i++) {
            regression.addData(i, history.get(i).getSalesAmount());
        }

        // 3. Prédire les N prochains mois
        List<SalesForecastDTO> forecasts = new ArrayList<>();
        for (int i = 1; i <= monthsAhead; i++) {
            double predictedSales = regression.predict(history.size() + i);
            forecasts.add(new SalesForecastDTO(
                LocalDate.now().plusMonths(i),
                predictedSales,
                calculateConfidenceInterval(regression, history.size() + i)
            ));
        }

        return forecasts;
    }
}
```

**Option 2 : Modèle avancé (ARIMA / Prophet)**
Intégrer Python via API REST :
```python
# forecast_service.py (Python Flask API)
from flask import Flask, request, jsonify
from prophet import Prophet
import pandas as pd

app = Flask(__name__)

@app.route('/api/forecast', methods=['POST'])
def forecast():
    data = request.json

    # Préparer les données
    df = pd.DataFrame(data['history'])
    df.columns = ['ds', 'y']  # Date et valeur

    # Entraîner le modèle Prophet
    model = Prophet(
        yearly_seasonality=True,
        weekly_seasonality=False,
        daily_seasonality=False
    )
    model.fit(df)

    # Prédire
    future = model.make_future_dataframe(periods=data['months_ahead'], freq='M')
    forecast = model.predict(future)

    return jsonify({
        'forecast': forecast[['ds', 'yhat', 'yhat_lower', 'yhat_upper']].to_dict('records')
    })

if __name__ == '__main__':
    app.run(port=5000)
```

**Intégration Spring Boot :**
```java
@Service
public class MLForecastingService {

    private final RestTemplate restTemplate;

    public List<SalesForecastDTO> forecastWithML(Long productId, int monthsAhead) {
        // Récupérer l'historique
        List<MonthlySales> history = salesRepository.getMonthlyProductSales(productId, 24);

        // Appeler le service Python
        ForecastRequest request = new ForecastRequest(history, monthsAhead);
        ForecastResponse response = restTemplate.postForObject(
            "http://localhost:5000/api/forecast",
            request,
            ForecastResponse.class
        );

        return response.getForecast();
    }
}
```

---

#### 4.2 Dashboard Personnalisable
**Priorité : P2 - Innovation**

**Fonctionnalités :**
- **Drag & drop widgets** (repositionnement)
- **Création de widgets personnalisés**
- **Sauvegarde de layouts par utilisateur**
- **Partage de dashboards**

**Technologies recommandées :**
- **Frontend** : Angular CDK Drag & Drop + GridStack.js
- **Backend** : Stockage JSON des configurations dans PostgreSQL

**Structure de données :**
```json
{
  "userId": 123,
  "dashboardName": "Mon tableau de bord",
  "widgets": [
    {
      "id": "widget-1",
      "type": "ca-chart",
      "position": { "x": 0, "y": 0, "w": 6, "h": 4 },
      "config": {
        "period": "MONTH",
        "chartType": "line"
      }
    },
    {
      "id": "widget-2",
      "type": "stock-alerts",
      "position": { "x": 6, "y": 0, "w": 6, "h": 4 },
      "config": {
        "alertTypes": ["RUPTURE", "PEREMPTION"]
      }
    }
  ]
}
```

**Composant Angular :**
```typescript
@Component({
  selector: 'jhi-custom-dashboard',
  template: `
    <gridster [options]="gridsterOptions">
      @for (widget of widgets; track widget.id) {
        <gridster-item [item]="widget.position">
          <jhi-dynamic-widget
            [type]="widget.type"
            [config]="widget.config"
            (remove)="removeWidget(widget.id)">
          </jhi-dynamic-widget>
        </gridster-item>
      }
    </gridster>

    <p-button
      label="Ajouter un widget"
      icon="pi pi-plus"
      (onClick)="showWidgetSelector()">
    </p-button>
  `
})
export class CustomDashboardComponent implements OnInit {
  widgets: DashboardWidget[] = [];
  gridsterOptions: GridsterConfig = {
    draggable: { enabled: true },
    resizable: { enabled: true },
    pushItems: true
  };

  ngOnInit() {
    this.dashboardService.loadUserDashboard()
      .subscribe(config => this.widgets = config.widgets);
  }

  saveDashboard() {
    this.dashboardService.saveDashboard({
      userId: this.currentUser.id,
      widgets: this.widgets
    }).subscribe();
  }
}
```

---

#### 4.3 Analyses Croisées
**Priorité : P3 - Avancé**

**Rapports :**
- **Corrélation produits** (produits achetés ensemble)
- **Impact des remises sur le CA**
- **Ventes par prescription et tiers-payant**

**Algorithme Market Basket Analysis (Apriori) :**
```java
@Service
public class MarketBasketAnalysisService {

    public List<ProductAssociationRule> findProductAssociations(
        double minSupport,
        double minConfidence
    ) {
        // 1. Récupérer toutes les ventes
        List<Sales> sales = salesRepository.findAll();

        // 2. Créer des transactions (panier d'achats)
        List<Set<Long>> transactions = sales.stream()
            .map(sale -> sale.getSalesLines().stream()
                .map(line -> line.getProduit().getId())
                .collect(Collectors.toSet()))
            .collect(Collectors.toList());

        // 3. Appliquer l'algorithme Apriori
        AprioriAlgorithm apriori = new AprioriAlgorithm(minSupport, minConfidence);
        List<AssociationRule> rules = apriori.findAssociationRules(transactions);

        // 4. Convertir en DTO
        return rules.stream()
            .map(rule -> new ProductAssociationRule(
                productRepository.findById(rule.getAntecedent()),
                productRepository.findById(rule.getConsequent()),
                rule.getSupport(),
                rule.getConfidence(),
                rule.getLift()
            ))
            .collect(Collectors.toList());
    }
}
```

**Utilisation :**
- **Recommandations** : "Les clients qui achètent X achètent aussi Y"
- **Merchandising** : Placement optimal des produits
- **Promotions croisées** : Offres groupées

---

#### 4.4 Rapports Planifiés (Scheduled Reports)
**Priorité : P2 - Automatisation**

**Fonctionnalités :**
- **Génération automatique** de rapports (quotidien, hebdomadaire, mensuel)
- **Envoi par email** (PDF attaché)
- **Notifications** sur événements (ex: stock critique)

**Configuration Spring Scheduler :**
```java
@Service
public class ScheduledReportService {

    @Scheduled(cron = "0 0 8 * * MON") // Tous les lundis à 8h
    public void sendWeeklySalesReport() {
        LocalDate startDate = LocalDate.now().minusWeeks(1);
        LocalDate endDate = LocalDate.now();

        // Générer le rapport
        byte[] pdfReport = reportService.generateWeeklySalesReport(startDate, endDate);

        // Envoyer par email aux managers
        List<User> managers = userRepository.findByAuthority("ROLE_MANAGER");
        managers.forEach(manager -> {
            emailService.sendEmailWithAttachment(
                manager.getEmail(),
                "Rapport hebdomadaire des ventes",
                "Veuillez trouver ci-joint le rapport des ventes de la semaine.",
                pdfReport,
                "rapport-ventes-" + startDate + ".pdf"
            );
        });
    }

    @Scheduled(cron = "0 0 9 * * *") // Tous les jours à 9h
    public void checkCriticalStock() {
        List<StockAlertDTO> alerts = stockService.getCriticalAlerts();

        if (!alerts.isEmpty()) {
            notificationService.sendPushNotification(
                "STOCK_ALERT",
                alerts.size() + " produits en alerte stock",
                "/stock/alerts"
            );
        }
    }
}
```

---

### 📦 Livrables Phase 4

- ✅ Système de prévisions de ventes (ML)
- ✅ Dashboard 100% personnalisable
- ✅ Module d'analyses croisées (Market Basket)
- ✅ Rapports planifiés avec envoi automatique
- ✅ Notifications intelligentes

---

## Stack Technologique

### Backend (Java/Spring Boot)

#### Frameworks & Librairies

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Core Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>4.0.0-RC1</version>
    </dependency>

    <!-- Data & Database -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>

    <!-- Caching -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
        <version>3.1.8</version>
    </dependency>

    <!-- PDF Generation -->
    <dependency>
        <groupId>org.xhtmlrenderer</groupId>
        <artifactId>flying-saucer-pdf</artifactId>
        <version>9.7.2</version>
    </dependency>

    <!-- Excel Export -->
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
        <version>5.2.5</version>
    </dependency>

    <!-- Statistics & Math -->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-math3</artifactId>
        <version>3.6.1</version>
    </dependency>

    <!-- Scheduled Tasks -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-quartz</artifactId>
    </dependency>
</dependencies>
```

---

### Frontend (Angular 20)

#### Dépendances NPM

```json
{
  "dependencies": {
    "@angular/core": "20.3.7",
    "@angular/common": "20.3.7",
    "@angular/cdk": "20.3.7",

    "primeng": "20.2.0",
    "primeicons": "^7.0.0",

    "chart.js": "^4.4.1",
    "ng2-charts": "^6.0.1",

    "gridster2": "^20.0.0",

    "file-saver": "^2.0.5",
    "xlsx": "^0.18.5",

    "date-fns": "^3.3.1",

    "rxjs": "7.8.2"
  }
}
```

#### Structure des Composants

```
src/main/webapp/app/
├── reports/
│   ├── dashboard/
│   │   ├── dashboard.component.ts
│   │   ├── ca-summary-card/
│   │   ├── ca-trend-chart/
│   │   └── ca-by-type-chart/
│   ├── sales/
│   │   ├── sales-report.component.ts
│   │   ├── top-products/
│   │   └── abc-analysis/
│   ├── stock/
│   │   ├── stock-report.component.ts
│   │   ├── stock-alerts/
│   │   └── stock-valuation/
│   ├── suppliers/
│   │   ├── supplier-performance/
│   │   └── purchase-history/
│   ├── customers/
│   │   ├── customer-segmentation/
│   │   └── customer-retention/
│   ├── cash-register/
│   │   ├── daily-cash-report/
│   │   └── cash-movements/
│   ├── tiers-payant/
│   │   ├── invoices-report/
│   │   └── payment-aging/
│   └── shared/
│       ├── report-filters/
│       ├── export-button/
│       └── date-range-picker/
└── services/
    ├── report.service.ts
    ├── export.service.ts
    └── dashboard.service.ts
```

---

### Database (PostgreSQL)

#### Optimisations Recommandées

```sql
-- 1. Index pour améliorer les performances des rapports

-- Sales (déjà existants)
CREATE INDEX IF NOT EXISTS idx_sales_date ON sales(sale_date);
CREATE INDEX IF NOT EXISTS idx_sales_statut ON sales(statut);
CREATE INDEX IF NOT EXISTS idx_sales_dtype ON sales(dtype);

-- Sales Lines
CREATE INDEX IF NOT EXISTS idx_sales_line_produit ON sales_line(produit_id);
CREATE INDEX IF NOT EXISTS idx_sales_line_sale ON sales_line(sale_id, sale_date);

-- Stock
CREATE INDEX IF NOT EXISTS idx_stock_produit_qty ON stock_produit(produit_id, stock_quantity);

-- Lot (péremptions)
CREATE INDEX IF NOT EXISTS idx_lot_expiry ON lot(expiry_date) WHERE expiry_date IS NOT NULL;

-- Factures tiers-payant
CREATE INDEX IF NOT EXISTS idx_facture_tp_statut ON facture_tiers_payant(statut, date_facture);

-- 2. Vues matérialisées pour rapports fréquents

-- CA journalier
CREATE MATERIALIZED VIEW mv_daily_sales_summary AS
SELECT
    sale_date,
    dtype as type_vente,
    COUNT(*) as nb_ventes,
    SUM(sales_amount) as ca_total,
    SUM(sales_amount - discount_amount) as ca_net,
    AVG(sales_amount) as panier_moyen
FROM sales
WHERE statut = 'CLOSED'
GROUP BY sale_date, dtype;

CREATE UNIQUE INDEX idx_mv_daily_sales ON mv_daily_sales_summary(sale_date, type_vente);

-- Top produits du mois
CREATE MATERIALIZED VIEW mv_monthly_top_products AS
SELECT
    DATE_TRUNC('month', s.sale_date) as mois,
    p.id as produit_id,
    p.libelle,
    p.code_cip,
    COUNT(DISTINCT s.id) as nb_ventes,
    SUM(sl.quantity_sold) as qte_vendue,
    SUM(sl.sales_amount) as ca_genere
FROM sales_line sl
INNER JOIN sales s ON sl.sale_id = s.id AND sl.sale_date = s.sale_date
INNER JOIN produit p ON sl.produit_id = p.id
WHERE s.statut = 'CLOSED'
  AND s.sale_date >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '6 months'
GROUP BY DATE_TRUNC('month', s.sale_date), p.id, p.libelle, p.code_cip;

CREATE INDEX idx_mv_monthly_top_products ON mv_monthly_top_products(mois, ca_genere DESC);

-- 3. Procédures stockées pour calculs complexes

CREATE OR REPLACE FUNCTION calculate_abc_classification(
    p_start_date DATE,
    p_end_date DATE
)
RETURNS TABLE (
    produit_id BIGINT,
    libelle VARCHAR,
    ca_total NUMERIC,
    ca_cumul_pct NUMERIC,
    classe VARCHAR(1)
) AS $$
BEGIN
    RETURN QUERY
    WITH product_sales AS (
        SELECT
            p.id,
            p.libelle,
            SUM(sl.sales_amount) as ca
        FROM produit p
        INNER JOIN sales_line sl ON p.id = sl.produit_id
        INNER JOIN sales s ON sl.sale_id = s.id
        WHERE s.sale_date BETWEEN p_start_date AND p_end_date
          AND s.statut = 'CLOSED'
        GROUP BY p.id, p.libelle
        ORDER BY SUM(sl.sales_amount) DESC
    ),
    cumulative AS (
        SELECT
            id,
            libelle,
            ca,
            SUM(ca) OVER (ORDER BY ca DESC ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) as ca_cumul,
            SUM(ca) OVER () as ca_total
        FROM product_sales
    )
    SELECT
        id,
        libelle,
        ca,
        ROUND((ca_cumul / ca_total) * 100, 2) as ca_cumul_pct,
        CASE
            WHEN (ca_cumul / ca_total) * 100 <= 80 THEN 'A'
            WHEN (ca_cumul / ca_total) * 100 <= 95 THEN 'B'
            ELSE 'C'
        END as classe
    FROM cumulative;
END;
$$ LANGUAGE plpgsql;

-- 4. Jobs de rafraîchissement des vues matérialisées

-- Configurer pg_cron (extension PostgreSQL)
-- Rafraîchir toutes les heures
SELECT cron.schedule('refresh-daily-sales', '0 * * * *',
    'REFRESH MATERIALIZED VIEW CONCURRENTLY mv_daily_sales_summary'
);

SELECT cron.schedule('refresh-monthly-products', '0 1 * * *',
    'REFRESH MATERIALIZED VIEW CONCURRENTLY mv_monthly_top_products'
);
```

---

## Bonnes Pratiques

### 1. Performance & Optimisation

#### Mise en Cache Stratégique
```java
@Service
public class CachedReportService {

    // Cache avec Caffeine
    @Cacheable(value = "dailySalesReport", key = "#date")
    public DailySalesReportDTO getDailySalesReport(LocalDate date) {
        // Générer le rapport (calculs lourds)
        return reportRepository.generateDailySalesReport(date);
    }

    // Invalider le cache quand une vente est fermée
    @CacheEvict(value = "dailySalesReport", key = "#sale.saleDate")
    public void onSaleClosed(Sales sale) {
        // Le cache sera invalidé automatiquement
    }
}
```

**Configuration Caffeine :**
```java
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "dailySalesReport",
            "topProducts",
            "stockAlerts"
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES) // TTL 15 min
            .maximumSize(1000) // Max 1000 entrées
            .recordStats() // Métriques
        );

        return cacheManager;
    }
}
```

---

#### Pagination & Lazy Loading
```java
@GetMapping("/api/reports/sales")
public ResponseEntity<Page<SalesReportDTO>> getSalesReport(
    @RequestParam LocalDate startDate,
    @RequestParam LocalDate endDate,
    Pageable pageable // Pagination automatique
) {
    Page<SalesReportDTO> page = reportService.getSalesReport(startDate, endDate, pageable);
    HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
        ServletUriComponentsBuilder.fromCurrentRequest(), page
    );
    return ResponseEntity.ok().headers(headers).body(page);
}
```

---

#### Requêtes Optimisées
```java
// Mauvais : N+1 queries
List<Sales> sales = salesRepository.findAll();
sales.forEach(sale -> {
    List<SalesLine> lines = sale.getSalesLines(); // Query pour chaque vente
});

// Bon : Fetch join
@Query("SELECT DISTINCT s FROM Sales s LEFT JOIN FETCH s.salesLines WHERE s.saleDate BETWEEN :start AND :end")
List<Sales> findAllWithLines(@Param("start") LocalDate start, @Param("end") LocalDate end);
```

---

### 2. Sécurité

#### Contrôle d'Accès aux Rapports
```java
@RestController
@RequestMapping("/api/reports")
public class ReportResource {

    // Seuls les managers peuvent voir les rapports de rentabilité
    @GetMapping("/profitability")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<ProfitabilityReportDTO> getProfitabilityReport() {
        return ResponseEntity.ok(reportService.getProfitabilityReport());
    }

    // Les vendeurs ne voient que leurs propres stats
    @GetMapping("/my-sales")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<UserSalesReportDTO> getMySalesReport() {
        User currentUser = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(reportService.getUserSalesReport(currentUser.getId()));
    }
}
```

---

### 3. Exports

#### Service d'Export Unifié
```java
@Service
public class ExportService {

    public byte[] exportToPDF(String templateName, Map<String, Object> data) {
        Context context = new Context();
        context.setVariables(data);

        String html = templateEngine.process("reports/" + templateName, context);
        return flyingSaucerPdfService.generate(html);
    }

    public byte[] exportToExcel(List<?> data, String sheetName) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet(sheetName);

        // Header
        Row headerRow = sheet.createRow(0);
        // ... créer les headers dynamiquement

        // Data
        int rowNum = 1;
        for (Object item : data) {
            Row row = sheet.createRow(rowNum++);
            // ... remplir les cellules
        }

        // Auto-size columns
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }
}
```

**Frontend (Angular) :**
```typescript
@Injectable()
export class ExportService {

  downloadPDF(reportType: string, params: any) {
    return this.http.get(`/api/reports/${reportType}/pdf`, {
      params: params,
      responseType: 'blob'
    }).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${reportType}-${new Date().toISOString()}.pdf`;
      link.click();
      window.URL.revokeObjectURL(url);
    });
  }

  downloadExcel(reportType: string, params: any) {
    return this.http.get(`/api/reports/${reportType}/excel`, {
      params: params,
      responseType: 'blob'
    }).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${reportType}-${new Date().toISOString()}.xlsx`;
      link.click();
      window.URL.revokeObjectURL(url);
    });
  }
}
```

---

### 4. Tests

#### Tests Unitaires
```java
@SpringBootTest
class ReportServiceTest {

    @Autowired
    private ReportService reportService;

    @MockBean
    private SalesRepository salesRepository;

    @Test
    void testGetDailySalesReport() {
        // Given
        LocalDate date = LocalDate.of(2024, 1, 15);
        List<Sales> mockSales = createMockSales(date, 10);
        when(salesRepository.findBySaleDate(date)).thenReturn(mockSales);

        // When
        DailySalesReportDTO report = reportService.getDailySalesReport(date);

        // Then
        assertThat(report.getTotalSales()).isEqualTo(10);
        assertThat(report.getTotalAmount()).isGreaterThan(0);
    }
}
```

---

### 5. Monitoring

#### Métriques avec Micrometer
```java
@Service
public class MonitoredReportService {

    private final MeterRegistry meterRegistry;

    @Timed(value = "report.generation.time", description = "Time to generate report")
    public ReportDTO generateReport(ReportParams params) {
        Counter.builder("report.generation.count")
            .tag("type", params.getType())
            .register(meterRegistry)
            .increment();

        return doGenerateReport(params);
    }
}
```

---

## Conclusion

Ce plan de rapports statistiques pour l'application web est conçu pour être :
- **Évolutif** : Phases progressives selon les besoins
- **Performant** : Optimisations SQL, cache, vues matérialisées
- **Sécurisé** : Contrôle d'accès, audit trail
- **Maintenable** : Code propre, tests, documentation

### Prochaines Étapes

1. **Valider les priorités** avec les parties prenantes
2. **Estimer les charges** pour chaque phase
3. **Créer les tickets** dans le backlog
4. **Démarrer la Phase 1** (sprints 1-3)

---

**Document créé le :** 2025-01-23
**Version :** 1.0
**Auteur :** Architecture Team
