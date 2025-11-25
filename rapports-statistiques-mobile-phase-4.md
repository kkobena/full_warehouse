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
