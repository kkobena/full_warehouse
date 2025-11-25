## Phase 2 : Analytics & Notifications
**Durée estimée : 3-4 sprints | Priorité : Important**

### 🎯 Objectif
Ajouter des rapports analytiques et des notifications intelligentes.

---

### 2.1 Rapports de Performance (Semaine/Mois)
**Priorité : P1 - Important**

#### Écran "Performances"

```
┌────────────────────────────────┐
│  Performances     📊   [Filtre]│
├────────────────────────────────┤
│  📅 Cette semaine              │
│  ┌──────────────────────────┐ │
│  │  CA: 15,450,000 FCFA     │ │
│  │  ↗ +8% vs semaine dern.  │ │
│  │                           │ │
│  │  [Graphique barres 7j]   │ │
│  └──────────────────────────┘ │
│                                │
│  💳 Modes de Paiement          │
│  ┌──────────────────────────┐ │
│  │  [Donut chart]           │ │
│  │  Espèces    45%          │ │
│  │  Mobile Money 30%        │ │
│  │  CB          20%         │ │
│  │  Crédit      5%          │ │
│  └──────────────────────────┘ │
│                                │
│  📦 Top Produits (7j)          │
│  ┌──────────────────────────┐ │
│  │ 1. Paracétamol ████████  │ │
│  │    1,850,000 FCFA        │ │
│  │ 2. Amoxicilline ██████   │ │
│  │    1,250,000 FCFA        │ │
│  │ 3. Ibuprofène ████       │ │
│  │    980,000 FCFA          │ │
│  └──────────────────────────┘ │
└────────────────────────────────┘
      👆 Swipe pour mensuel
```

**Swipeable Tabs (Semaine / Mois / Année) :**
```typescript
import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs';

const Tab = createMaterialTopTabNavigator();

const PerformanceScreen: React.FC = () => {
  return (
    <Tab.Navigator
      screenOptions={{
        tabBarScrollEnabled: true,
        tabBarIndicatorStyle: { backgroundColor: 'blue' }
      }}
    >
      <Tab.Screen name="Semaine" component={WeeklyPerformance} />
      <Tab.Screen name="Mois" component={MonthlyPerformance} />
      <Tab.Screen name="Année" component={YearlyPerformance} />
    </Tab.Navigator>
  );
};
```

---

### 2.2 Graphiques Interactifs Avancés
**Priorité : P1 - Important**

**Bibliothèque recommandée : Victory Native (React Native)**

```typescript
import { VictoryBar, VictoryChart, VictoryTheme, VictoryAxis } from 'victory-native';

const SalesChartCard: React.FC<{ data }> = ({ data }) => {
  return (
    <Card>
      <Card.Title>CA des 30 derniers jours</Card.Title>
      <Card.Content>
        <VictoryChart theme={VictoryTheme.material} height={250}>
          <VictoryAxis
            tickFormat={(x) => `${x}`}
            style={{ tickLabels: { fontSize: 10 } }}
          />
          <VictoryAxis
            dependentAxis
            tickFormat={(y) => `${y / 1000}K`}
          />
          <VictoryBar
            data={data}
            x="day"
            y="amount"
            style={{
              data: { fill: "#3b82f6" }
            }}
            animate={{
              duration: 500,
              onLoad: { duration: 300 }
            }}
          />
        </VictoryChart>
      </Card.Content>
    </Card>
  );
};
```

**Interactions tactiles :**
- **Pinch to zoom** : Zoomer sur une période spécifique
- **Tap on bar** : Afficher détails du jour
- **Swipe** : Naviguer entre différentes périodes

---

### 2.3 Notifications Push Avancées
**Priorité : P1 - Important**

#### Notifications Intelligentes

**1. Notifications personnalisées par rôle :**
```java
@Service
public class SmartNotificationService {

    public void sendDailyDigest() {
        // Pour les gérants
        List<User> managers = userRepository.findByAuthority("ROLE_MANAGER");
        managers.forEach(manager -> {
            DailyDigestDTO digest = reportService.generateDailyDigest(LocalDate.now());

            sendNotification(manager, NotificationTemplate.builder()
                .title("📊 Résumé quotidien")
                .body(String.format(
                    "CA: %s FCFA (+%.1f%%) | %d ventes | %d alertes",
                    digest.getTotalCA(),
                    digest.getVariation(),
                    digest.getTransactionCount(),
                    digest.getAlertsCount()
                ))
                .data(Map.of("type", "DAILY_DIGEST", "date", LocalDate.now().toString()))
                .build()
            );
        });

        // Pour les vendeurs
        List<User> sellers = userRepository.findByAuthority("ROLE_USER");
        sellers.forEach(seller -> {
            UserPerformanceDTO perf = reportService.getUserPerformance(seller.getId(), LocalDate.now());

            sendNotification(seller, NotificationTemplate.builder()
                .title("💪 Votre performance du jour")
                .body(String.format(
                    "CA: %s FCFA | %d ventes | Panier moyen: %s FCFA",
                    perf.getTotalCA(),
                    perf.getSalesCount(),
                    perf.getAverageBasket()
                ))
                .build()
            );
        });
    }

    @Scheduled(cron = "0 0 18 * * *") // Tous les jours à 18h
    public void sendDailyDigest() {
        sendDailyDigest();
    }
}
```

**2. Notifications contextuelles :**
```java
// Notification quand objectif atteint
@EventListener
public void onSalesTargetReached(SalesTargetReachedEvent event) {
    sendNotification(
        "TARGET_REACHED",
        "🎯 Objectif atteint!",
        "Bravo! L'objectif de CA quotidien a été dépassé.",
        Map.of("ca", event.getAmount(), "target", event.getTarget())
    );
}

// Notification en cas de vente importante
@EventListener
public void onHighValueSale(HighValueSaleEvent event) {
    if (event.getAmount() > 500_000) {
        sendNotification(
            "HIGH_VALUE_SALE",
            "💰 Grosse vente!",
            String.format("Vente de %s FCFA enregistrée", event.getAmount()),
            Map.of("saleId", event.getSaleId())
        );
    }
}
```

**3. Groupement intelligent :**
```java
// Grouper plusieurs alertes du même type
public void sendBatchedAlerts() {
    Map<AlertType, List<Alert>> groupedAlerts = alertRepository
        .findUnsentAlerts()
        .stream()
        .collect(Collectors.groupingBy(Alert::getType));

    groupedAlerts.forEach((type, alerts) -> {
        if (alerts.size() == 1) {
            sendSingleAlert(alerts.get(0));
        } else {
            sendBatchedAlert(type, alerts.size());
        }
    });
}
```

---

### 2.4 Widgets Home Screen
**Priorité : P2 - Souhaitable**

**iOS Widget (Swift UI) :**
```swift
// PharmaSmartWidget.swift
import WidgetKit
import SwiftUI

struct CAWidgetEntryView : View {
    var entry: CAEntry

    var body: some View {
        VStack(alignment: .leading) {
            Text("CA Aujourd'hui")
                .font(.caption)
                .foregroundColor(.secondary)

            Text(entry.amount)
                .font(.title2)
                .fontWeight(.bold)

            HStack {
                Image(systemName: entry.isUp ? "arrow.up.right" : "arrow.down.right")
                    .foregroundColor(entry.isUp ? .green : .red)
                Text(entry.variation)
                    .font(.caption)
            }
        }
        .padding()
    }
}

@main
struct PharmaSmartWidget: Widget {
    let kind: String = "PharmaSmartWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: CAProvider()) { entry in
            CAWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("CA Quotidien")
        .description("Suivi du chiffre d'affaires en temps réel")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
```

**Android Widget (Jetpack Glance) :**
```kotlin
// PharmaSmartWidget.kt
@Composable
fun CAWidget(data: CAData) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            text = "CA Aujourd'hui",
            style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color.Gray))
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "${data.amount} FCFA",
            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
        )
        Row {
            Icon(
                imageVector = if (data.isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                tint = if (data.isUp) Color.Green else Color.Red
            )
            Text(
                text = "${data.variation}%",
                style = TextStyle(fontSize = 12.sp)
            )
        }
    }
}
```

---

### 📦 Livrables Phase 2

- ✅ Écran performances avec graphiques interactifs
- ✅ Tabs swipeables (Semaine/Mois/Année)
- ✅ Notifications push intelligentes et groupées
- ✅ Widgets iOS et Android (CA + Alertes)
- ✅ Système de badges sur icône app
- ✅ Deep linking depuis notifications

---
