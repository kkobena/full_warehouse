# Rapports Statistiques - Application Mobile
## Pharma-Smart Mobile Analytics

---

## 📋 Table des Matières

1. [Introduction](#introduction)
2. [Architecture Technique Mobile](#architecture-technique)
3. [Phase 1 : MVP Mobile Essentiel](#phase-1)
4. [Phase 2 : Analytics & Notifications](#phase-2)
5. [Phase 3 : Intelligence & Offline](#phase-3)
6. [Stack Technologique Mobile](#stack-technologique)
7. [Design Patterns Mobile](#design-patterns)
8. [Bonnes Pratiques Mobile](#bonnes-pratiques)

---

## Introduction

Ce document définit la roadmap des rapports statistiques pour l'application mobile Pharma-Smart. L'approche mobile privilégie :

### Principes Directeurs
- **📱 Mobile-First** : Interface optimisée pour écrans tactiles
- **⚡ Temps Réel** : Données à jour avec synchronisation intelligente
- **🔔 Proactif** : Alertes et notifications push pertinentes
- **📴 Offline-First** : Fonctionnement même sans connexion
- **🔋 Économie** : Optimisation batterie et consommation data
- **👆 Actions Rapides** : Swipe, quick actions, widgets

### Différences Web vs Mobile

| Critère | Application Web | Application Mobile |
|---------|----------------|-------------------|
| **Cas d'usage** | Analyse détaillée, création rapports | Consultation rapide, supervision |
| **Interface** | Tableaux complexes, filtres multiples | Cards, listes scrollables, graphiques simples |
| **Interactions** | Clics, hover, filtres | Swipe, tap, pull-to-refresh |
| **Export** | PDF, Excel détaillés | Partage rapide, captures d'écran |
| **Notifications** | Emails planifiés | Push notifications temps réel |
| **Connexion** | Online requis | Offline-first avec sync |

---

## Architecture Technique Mobile

### Architecture Globale

```
┌─────────────────────────────────────────────────────────┐
│         Application Mobile (React Native / Flutter)     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  Dashboard   │  │   Alerts     │  │   Widgets    │  │
│  │  Screens     │  │   Service    │  │   Home       │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Offline    │  │     Push     │  │    Charts    │  │
│  │   Storage    │  │Notifications │  │   Library    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
            ↓ REST API / WebSocket / GraphQL
┌─────────────────────────────────────────────────────────┐
│              Backend (Spring Boot 4.0.0)                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Mobile     │  │  Push Notif  │  │  WebSocket   │  │
│  │  Resources   │  │   Service    │  │   Handler    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
            ↓ Real-time events
┌─────────────────────────────────────────────────────────┐
│          Firebase / OneSignal (Push Notifications)      │
└─────────────────────────────────────────────────────────┘
```

---

## Phase 1 : MVP Mobile Essentiel
**Durée estimée : 2-3 sprints | Priorité : Critique**

### 🎯 Objectif
Fournir aux gérants une vue rapide et actionnable de l'activité de la pharmacie en mobilité.

---

### 1.1 Dashboard Quotidien du Gérant
**Priorité : P0 - Critique**

#### Fonctionnalités

**Écran principal (Home Dashboard) :**
```
┌────────────────────────────────┐
│  Pharma-Smart     🔔(3)   👤  │
├────────────────────────────────┤
│  CA Aujourd'hui                │
│  ┌──────────────────────────┐ │
│  │  2,450,000 FCFA   ↗ +12% │ │
│  │  Objectif: 2,800,000      │ │
│  │  ████████░░░░░ 87%        │ │
│  └──────────────────────────┘ │
│                                │
│  Ventes du Jour                │
│  ┌──────────────────────────┐ │
│  │  147 transactions         │ │
│  │  Panier moyen: 16,667F    │ │
│  └──────────────────────────┘ │
│                                │
│  ⚠️ Alertes (3)                │
│  ┌──────────────────────────┐ │
│  │ 🔴 5 ruptures de stock    │ │
│  │ 🟠 12 péremptions < 30j   │ │
│  │ 🟡 Écart caisse: 5,000F   │ │
│  └──────────────────────────┘ │
│                                │
│  Top Produits Aujourd'hui      │
│  ┌──────────────────────────┐ │
│  │ 1. Paracétamol   250,000F │ │
│  │ 2. Amoxicilline  180,000F │ │
│  │ 3. Ibuprofène    145,000F │ │
│  └──────────────────────────┘ │
└────────────────────────────────┘
```

**Composants (React Native) :**
```typescript
// screens/DashboardScreen.tsx
import React, { useEffect, useState } from 'react';
import { View, ScrollView, RefreshControl } from 'react-native';
import { Card, Text, ProgressBar } from 'react-native-paper';
import { useDashboard } from '../hooks/useDashboard';

export const DashboardScreen: React.FC = () => {
  const { data, loading, refresh } = useDashboard();
  const [refreshing, setRefreshing] = useState(false);

  const onRefresh = async () => {
    setRefreshing(true);
    await refresh();
    setRefreshing(false);
  };

  return (
    <ScrollView
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
      }
    >
      {/* CA du Jour Card */}
      <CACard
        amount={data.dailyCA}
        target={data.dailyTarget}
        variation={data.variation}
      />

      {/* Ventes du Jour Card */}
      <SalesCard
        transactions={data.transactionsCount}
        averageBasket={data.averageBasket}
      />

      {/* Alertes Card */}
      <AlertsCard alerts={data.alerts} />

      {/* Top Produits Card */}
      <TopProductsCard products={data.topProducts} />
    </ScrollView>
  );
};
```

**API Backend (GraphQL pour optimisation mobile) :**
```graphql
# schema.graphql
type DashboardData {
  dailyCA: Float!
  dailyTarget: Float!
  variation: Float!
  transactionsCount: Int!
  averageBasket: Float!
  alerts: [Alert!]!
  topProducts: [ProductSummary!]!
  lastUpdate: DateTime!
}

type Query {
  dashboard(date: Date!): DashboardData!
}

# Usage
query GetDashboard {
  dashboard(date: "2024-01-23") {
    dailyCA
    dailyTarget
    variation
    transactionsCount
    averageBasket
    alerts {
      type
      severity
      message
      count
    }
    topProducts {
      id
      name
      salesAmount
    }
  }
}
```

**Avantage GraphQL :**
- **Une seule requête** au lieu de 5 REST calls
- **Payload réduit** (uniquement les champs demandés)
- **Économie de batterie** et de data

---

#### Graphiques Interactifs (Évolution CA)

**Chart Card - CA des 7 derniers jours :**
```typescript
import { LineChart } from 'react-native-chart-kit';

const CATrendChart: React.FC = ({ data }) => {
  return (
    <Card>
      <Card.Title>Évolution CA (7 jours)</Card.Title>
      <Card.Content>
        <LineChart
          data={{
            labels: data.labels, // ["Lun", "Mar", "Mer", ...]
            datasets: [{
              data: data.values,
              color: (opacity = 1) => `rgba(37, 99, 235, ${opacity})`,
              strokeWidth: 2
            }]
          }}
          width={320}
          height={180}
          chartConfig={{
            backgroundColor: '#fff',
            backgroundGradientFrom: '#fff',
            backgroundGradientTo: '#fff',
            decimalPlaces: 0,
            color: (opacity = 1) => `rgba(0, 0, 0, ${opacity})`,
          }}
          bezier
          style={{ borderRadius: 16 }}
        />
      </Card.Content>
    </Card>
  );
};
```

---

### 1.2 Système d'Alertes Critiques
**Priorité : P0 - Critique**

#### Notifications Push

**Types d'alertes :**
1. **🔴 Ruptures de stock** (temps réel)
2. **🟠 Péremptions imminentes** (< 30 jours)
3. **🟡 Écarts de caisse** (> seuil)
4. **🔵 Factures impayées** (> 90 jours)

**Backend - Service de Notifications :**
```java
@Service
public class MobilePushNotificationService {

    private final FirebaseMessaging firebaseMessaging;
    private final UserDeviceRepository userDeviceRepository;

    public void sendStockAlert(StockAlert alert) {
        List<UserDevice> devices = userDeviceRepository
            .findByUserAuthorityAndNotificationsEnabled("ROLE_MANAGER", true);

        Message message = Message.builder()
            .setNotification(Notification.builder()
                .setTitle("🔴 Rupture de stock")
                .setBody(alert.getProductName() + " - Stock épuisé")
                .build())
            .putData("type", "STOCK_ALERT")
            .putData("productId", alert.getProductId().toString())
            .putData("action", "VIEW_PRODUCT")
            .build();

        devices.forEach(device -> {
            try {
                message.setToken(device.getFcmToken());
                firebaseMessaging.send(message);
            } catch (FirebaseMessagingException e) {
                log.error("Failed to send notification to device: {}", device.getId(), e);
            }
        });
    }

    @Scheduled(cron = "0 0 9 * * *") // Tous les jours à 9h
    public void checkExpiringProducts() {
        List<Lot> expiringLots = lotRepository.findExpiringInDays(30);

        if (!expiringLots.isEmpty()) {
            sendBatchNotification(
                "EXPIRY_ALERT",
                "⚠️ Produits proches de péremption",
                expiringLots.size() + " produits expirent dans moins de 30 jours",
                Map.of("count", String.valueOf(expiringLots.size()))
            );
        }
    }
}
```

**Mobile - Gestion des Notifications :**
```typescript
// services/PushNotificationService.ts
import messaging from '@react-native-firebase/messaging';
import { useNavigation } from '@react-navigation/native';

export class PushNotificationService {

  async initialize() {
    // Demander permission
    const authStatus = await messaging().requestPermission();

    if (authStatus === messaging.AuthorizationStatus.AUTHORIZED) {
      // Récupérer FCM token
      const token = await messaging().getToken();
      await this.registerDeviceToken(token);
    }

    // Gérer notifications en foreground
    messaging().onMessage(async remoteMessage => {
      this.showLocalNotification(remoteMessage);
    });

    // Gérer tap sur notification
    messaging().onNotificationOpenedApp(remoteMessage => {
      this.handleNotificationAction(remoteMessage.data);
    });
  }

  handleNotificationAction(data: any) {
    switch (data.type) {
      case 'STOCK_ALERT':
        navigation.navigate('ProductDetail', { id: data.productId });
        break;
      case 'CASH_DISCREPANCY':
        navigation.navigate('CashRegister', { date: data.date });
        break;
      case 'INVOICE_OVERDUE':
        navigation.navigate('Invoices', { filter: 'overdue' });
        break;
    }
  }
}
```

---

#### Écran des Alertes

```typescript
// screens/AlertsScreen.tsx
const AlertsScreen: React.FC = () => {
  const { alerts } = useAlerts();

  return (
    <ScrollView>
      <FilterChips
        filters={['Toutes', 'Stock', 'Péremptions', 'Caisse', 'Factures']}
      />

      {alerts.map(alert => (
        <Swipeable
          key={alert.id}
          renderRightActions={() => (
            <TouchableOpacity onPress={() => resolveAlert(alert.id)}>
              <View style={styles.resolveButton}>
                <Text>✓ Résoudre</Text>
              </View>
            </TouchableOpacity>
          )}
        >
          <AlertCard
            type={alert.type}
            severity={alert.severity}
            message={alert.message}
            timestamp={alert.createdAt}
            onTap={() => handleAlertTap(alert)}
          />
        </Swipeable>
      ))}
    </ScrollView>
  );
};
```

**Fonctionnalités :**
- **Swipe pour résoudre** : Glisser vers la droite pour marquer comme résolu
- **Filtres rapides** : Chips pour filtrer par type
- **Badge de notification** : Nombre d'alertes non lues
- **Deep linking** : Tap pour aller directement à l'écran concerné

---

### 1.3 Vue Rapide Stock (Recherche + Scan)
**Priorité : P0 - Critique**

#### Recherche Rapide de Produit

```typescript
// screens/StockSearchScreen.tsx
import { Camera, useCameraDevice } from 'react-native-vision-camera';
import { BarcodeScanner } from 'react-native-barcode-scanner';

const StockSearchScreen: React.FC = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [scannerActive, setScannerActive] = useState(false);
  const device = useCameraDevice('back');

  const onBarCodeScanned = async (barcodes) => {
    const code = barcodes[0].displayValue;
    const product = await searchProductByCode(code);
    showProductModal(product);
    setScannerActive(false);
  };

  return (
    <View>
      {/* Barre de recherche */}
      <Searchbar
        placeholder="Rechercher un produit..."
        value={searchQuery}
        onChangeText={setSearchQuery}
        icon="barcode-scan"
        onIconPress={() => setScannerActive(true)}
      />

      {/* Scanner de code-barre */}
      {scannerActive && (
        <Modal visible={scannerActive}>
          <Camera
            device={device}
            isActive={true}
            codeScanner={{
              codeTypes: ['ean-13', 'code-128'],
              onCodeScanned: onBarCodeScanned
            }}
          />
          <Button onPress={() => setScannerActive(false)}>
            Annuler
          </Button>
        </Modal>
      )}

      {/* Résultats de recherche */}
      <ProductSearchResults query={searchQuery} />
    </View>
  );
};
```

#### Modal Détail Produit

```
┌────────────────────────────────┐
│  ← Paracétamol 500mg      ⋮   │
├────────────────────────────────┤
│  Code CIP: 3400930000000       │
│                                │
│  📦 Stock Actuel               │
│  ┌──────────────────────────┐ │
│  │  850 unités              │ │
│  │  Seuil min: 500          │ │
│  │  ✓ Stock OK              │ │
│  └──────────────────────────┘ │
│                                │
│  💰 Prix                       │
│  ┌──────────────────────────┐ │
│  │  Achat: 150 FCFA         │ │
│  │  Vente: 250 FCFA         │ │
│  │  Marge: 66%              │ │
│  └──────────────────────────┘ │
│                                │
│  📅 Péremptions               │
│  ┌──────────────────────────┐ │
│  │  Lot A: 31/12/2025 (500) │ │
│  │  Lot B: 30/06/2026 (350) │ │
│  └──────────────────────────┘ │
│                                │
│  📊 Dernières Ventes          │
│  ┌──────────────────────────┐ │
│  │  Aujourd'hui: 45 unités  │ │
│  │  Cette semaine: 280      │ │
│  │  Ce mois: 1,200          │ │
│  └──────────────────────────┘ │
│                                │
│  ┌──────────────────────────┐ │
│  │  📦 Commander            │ │
│  └──────────────────────────┘ │
└────────────────────────────────┘
```

**API Endpoint :**
```java
@GetMapping("/api/mobile/products/{id}/quick-info")
public ResponseEntity<ProductQuickInfoDTO> getProductQuickInfo(@PathVariable Long id) {
    ProductQuickInfoDTO info = productService.getQuickInfo(id);
    return ResponseEntity.ok(info);
}

// DTO optimisé pour mobile (payload minimal)
public record ProductQuickInfoDTO(
    Long id,
    String name,
    String codeCIP,
    StockInfo stock,
    PriceInfo price,
    List<LotInfo> lots,
    SalesStats salesStats
) {}
```

---

### 1.4 Liste d'Actions Prioritaires
**Priorité : P1 - Important**

#### Écran "À Faire Aujourd'hui"

```typescript
// screens/TodoScreen.tsx
const TodoScreen: React.FC = () => {
  const { todos, loading } = useTodos();

  return (
    <ScrollView>
      <SectionList
        sections={[
          { title: 'Urgent', data: todos.urgent, color: 'red' },
          { title: 'Important', data: todos.important, color: 'orange' },
          { title: 'Normal', data: todos.normal, color: 'blue' }
        ]}
        renderSectionHeader={({ section }) => (
          <SectionHeader title={section.title} color={section.color} />
        )}
        renderItem={({ item }) => (
          <TodoCard
            todo={item}
            onAction={() => handleAction(item)}
            onSwipeLeft={() => dismissTodo(item.id)}
          />
        )}
      />
    </ScrollView>
  );
};
```

**Types d'actions :**

1. **Produits à commander (Ruptures/Alerte stock)**
   - Bouton CTA : "📦 Commander maintenant"
   - Ouvre un formulaire de commande pré-rempli

2. **Factures à relancer (> 60 jours)**
   - Bouton CTA : "📞 Appeler le client"
   - Ouvre le dialer avec le numéro du tiers-payant

3. **Produits à démarquer (Péremption < 90j)**
   - Bouton CTA : "🏷️ Créer promotion"
   - Ouvre un formulaire de remise

4. **Inventaires en retard**
   - Bouton CTA : "📋 Démarrer inventaire"
   - Navigation vers module inventaire

**Backend - Génération des Todos :**
```java
@Service
public class MobileTodoService {

    public MobileTodoListDTO getTodoList(Long userId) {
        List<TodoItem> urgent = new ArrayList<>();
        List<TodoItem> important = new ArrayList<>();
        List<TodoItem> normal = new ArrayList<>();

        // 1. Produits en rupture (Urgent)
        List<StockProduit> outOfStock = stockRepository.findByQuantity(0);
        outOfStock.forEach(stock -> {
            urgent.add(new TodoItem(
                "REORDER",
                "Commander " + stock.getProduit().getLibelle(),
                "Rupture de stock",
                TodoPriority.URGENT,
                Map.of("productId", stock.getProduit().getId())
            ));
        });

        // 2. Factures impayées > 90j (Urgent)
        List<FactureTiersPayant> overdueInvoices =
            factureRepository.findOverdue(90);
        overdueInvoices.forEach(facture -> {
            urgent.add(new TodoItem(
                "CALL_CLIENT",
                "Relancer " + facture.getGroupeTiersPayant().getLibelle(),
                "Facture impayée depuis " + facture.getDaysOverdue() + " jours",
                TodoPriority.URGENT,
                Map.of("invoiceId", facture.getId(), "phone", facture.getPhone())
            ));
        });

        // 3. Produits proches péremption < 3 mois (Important)
        List<Lot> expiringLots = lotRepository.findExpiringInDays(90);
        expiringLots.forEach(lot -> {
            important.add(new TodoItem(
                "CREATE_DISCOUNT",
                "Démarquer " + lot.getProduit().getLibelle(),
                "Expire le " + lot.getExpiryDate(),
                TodoPriority.IMPORTANT,
                Map.of("lotId", lot.getId(), "productId", lot.getProduit().getId())
            ));
        });

        return new MobileTodoListDTO(urgent, important, normal);
    }
}
```

---

### 📦 Livrables Phase 1

- ✅ Dashboard quotidien avec 4 widgets interactifs
- ✅ Système de notifications push (Firebase)
- ✅ Écran alertes avec swipe actions
- ✅ Recherche produit + scanner code-barre
- ✅ Modal détail produit (stock, prix, péremptions)
- ✅ Liste d'actions prioritaires avec CTAs
- ✅ Pull-to-refresh sur tous les écrans
- ✅ Gestion offline basique (cache local)

---

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

## Phase 3 : Intelligence & Offline
**Durée estimée : 4-5 sprints | Priorité : Innovation**

### 🎯 Objectif
Ajouter des fonctionnalités avancées : prévisions, mode offline complet, et optimisations.

---

### 3.1 Prévisions de Ventes (ML Mobile)
**Priorité : P2 - Innovation**

**Approche : TensorFlow Lite sur Mobile**

```typescript
// services/ForecastingService.ts
import * as tf from '@tensorflow/tfjs';
import '@tensorflow/tfjs-react-native';

export class MobileForecastingService {

  private model: tf.LayersModel | null = null;

  async loadModel() {
    // Charger modèle TFLite pré-entraîné
    const modelJson = require('../assets/models/sales-forecast.json');
    const modelWeights = require('../assets/models/sales-forecast.weights.bin');

    this.model = await tf.loadLayersModel(
      bundleResourceIO(modelJson, modelWeights)
    );
  }

  async predictNextWeekSales(historicalData: number[]): Promise<number[]> {
    if (!this.model) {
      await this.loadModel();
    }

    // Normaliser les données
    const normalized = this.normalize(historicalData);

    // Prédiction
    const inputTensor = tf.tensor2d([normalized], [1, historicalData.length]);
    const prediction = this.model.predict(inputTensor) as tf.Tensor;

    // Dénormaliser
    const result = await prediction.array();
    return this.denormalize(result[0]);
  }

  normalize(data: number[]): number[] {
    const max = Math.max(...data);
    return data.map(v => v / max);
  }

  denormalize(data: number[]): number[] {
    const max = Math.max(...this.historicalData);
    return data.map(v => v * max);
  }
}
```

**Écran Prévisions :**
```typescript
const ForecastScreen: React.FC = () => {
  const [forecast, setForecast] = useState<ForecastData | null>(null);

  useEffect(() => {
    const loadForecast = async () => {
      const historical = await fetchHistoricalSales(30); // 30 derniers jours
      const service = new MobileForecastingService();
      const predicted = await service.predictNextWeekSales(historical);

      setForecast({
        historical,
        predicted,
        confidence: 0.85
      });
    };

    loadForecast();
  }, []);

  return (
    <ScrollView>
      <Card>
        <Card.Title>Prévisions CA (7 prochains jours)</Card.Title>
        <Card.Content>
          <LineChart
            data={{
              labels: ['J+1', 'J+2', 'J+3', 'J+4', 'J+5', 'J+6', 'J+7'],
              datasets: [
                {
                  data: forecast?.historical.slice(-7) || [],
                  color: () => 'gray',
                  strokeDashArray: [5, 5] // Ligne pointillée
                },
                {
                  data: forecast?.predicted || [],
                  color: () => 'blue'
                }
              ]
            }}
          />
          <Text>Niveau de confiance: {(forecast?.confidence * 100).toFixed(0)}%</Text>
        </Card.Content>
      </Card>

      <Card>
        <Card.Title>Recommandations</Card.Title>
        <List>
          <List.Item
            title="Augmenter le stock de Paracétamol"
            description="Ventes prévues: +25% cette semaine"
            left={() => <Icon name="trending-up" />}
          />
        </List>
      </Card>
    </ScrollView>
  );
};
```

---

### 3.2 Mode Offline Complet
**Priorité : P1 - Important**

**Architecture Offline-First :**

```
┌─────────────────────────────────┐
│      Application Mobile         │
│  ┌────────────────────────────┐ │
│  │    UI Components           │ │
│  └────────────────────────────┘ │
│              ↕                  │
│  ┌────────────────────────────┐ │
│  │  Redux/MobX State          │ │
│  └────────────────────────────┘ │
│              ↕                  │
│  ┌────────────────────────────┐ │
│  │  Offline Queue Manager     │ │
│  └────────────────────────────┘ │
│              ↕                  │
│  ┌────────────────────────────┐ │
│  │  Local Database (SQLite)   │ │
│  │  - Rapports (cache)        │ │
│  │  - Actions en attente      │ │
│  └────────────────────────────┘ │
└─────────────────────────────────┘
         ↕ (sync quand online)
┌─────────────────────────────────┐
│      Backend API                │
└─────────────────────────────────┘
```

**Implémentation avec WatermelonDB :**

```typescript
// database/schema.ts
import { appSchema, tableSchema } from '@nozbe/watermelondb';

export const schema = appSchema({
  version: 1,
  tables: [
    tableSchema({
      name: 'cached_reports',
      columns: [
        { name: 'report_type', type: 'string' },
        { name: 'report_date', type: 'number' },
        { name: 'data', type: 'string' }, // JSON
        { name: 'cached_at', type: 'number' },
        { name: 'expires_at', type: 'number' }
      ]
    }),
    tableSchema({
      name: 'pending_actions',
      columns: [
        { name: 'action_type', type: 'string' },
        { name: 'payload', type: 'string' }, // JSON
        { name: 'created_at', type: 'number' },
        { name: 'retry_count', type: 'number' }
      ]
    })
  ]
});

// services/OfflineManager.ts
export class OfflineManager {

  async cacheReport(reportType: string, data: any, ttl: number = 3600) {
    const expiresAt = Date.now() + (ttl * 1000);

    await database.write(async () => {
      await database.collections.get('cached_reports').create(report => {
        report.reportType = reportType;
        report.reportDate = Date.now();
        report.data = JSON.stringify(data);
        report.cachedAt = Date.now();
        report.expiresAt = expiresAt;
      });
    });
  }

  async getCachedReport(reportType: string): Promise<any | null> {
    const cached = await database.collections
      .get('cached_reports')
      .query(
        Q.where('report_type', reportType),
        Q.where('expires_at', Q.gt(Date.now())),
        Q.sortBy('cached_at', Q.desc),
        Q.take(1)
      )
      .fetch();

    if (cached.length > 0) {
      return JSON.parse(cached[0].data);
    }

    return null;
  }

  async queueAction(actionType: string, payload: any) {
    await database.write(async () => {
      await database.collections.get('pending_actions').create(action => {
        action.actionType = actionType;
        action.payload = JSON.stringify(payload);
        action.createdAt = Date.now();
        action.retryCount = 0;
      });
    });
  }

  async syncPendingActions() {
    const pending = await database.collections
      .get('pending_actions')
      .query()
      .fetch();

    for (const action of pending) {
      try {
        await this.executePendingAction(action);
        await action.destroyPermanently(); // Supprimer après succès
      } catch (error) {
        await action.update(a => {
          a.retryCount += 1;
        });

        if (action.retryCount > 3) {
          // Alerter l'utilisateur
          showToast('Impossible de synchroniser certaines actions');
        }
      }
    }
  }

  async executePendingAction(action: PendingAction) {
    const payload = JSON.parse(action.payload);

    switch (action.actionType) {
      case 'RESOLVE_ALERT':
        await api.resolveAlert(payload.alertId);
        break;
      case 'CREATE_ORDER':
        await api.createOrder(payload.order);
        break;
      // ... autres actions
    }
  }
}
```

**Stratégie de synchronisation :**
```typescript
// App.tsx
import NetInfo from '@react-native-community/netinfo';

const App: React.FC = () => {
  const offlineManager = new OfflineManager();

  useEffect(() => {
    // Écouter les changements de connectivité
    const unsubscribe = NetInfo.addEventListener(state => {
      if (state.isConnected) {
        // Synchroniser quand la connexion revient
        offlineManager.syncPendingActions();
      }
    });

    return () => unsubscribe();
  }, []);

  return <RootNavigator />;
};
```

**Indicateur de mode offline :**
```typescript
const OfflineBanner: React.FC = () => {
  const isOffline = useNetworkStatus();

  if (!isOffline) return null;

  return (
    <Banner
      visible={true}
      icon="wifi-off"
      actions={[
        {
          label: 'Réessayer',
          onPress: () => checkConnectivity()
        }
      ]}
    >
      Vous êtes hors ligne. Les modifications seront synchronisées automatiquement.
    </Banner>
  );
};
```

---

### 3.3 Rapports Personnalisables
**Priorité : P2 - Avancé**

**Builder de rapports simple :**
```typescript
const CustomReportBuilder: React.FC = () => {
  const [selectedMetrics, setSelectedMetrics] = useState<string[]>([]);
  const [period, setPeriod] = useState<'day' | 'week' | 'month'>('week');

  const availableMetrics = [
    { id: 'ca', label: 'Chiffre d\'affaires', icon: 'currency-usd' },
    { id: 'transactions', label: 'Nombre de ventes', icon: 'cart' },
    { id: 'basket', label: 'Panier moyen', icon: 'basket' },
    { id: 'margin', label: 'Marge brute', icon: 'percent' },
    { id: 'top_products', label: 'Top produits', icon: 'star' }
  ];

  return (
    <ScrollView>
      <Text>Sélectionnez les indicateurs à afficher :</Text>

      {availableMetrics.map(metric => (
        <Checkbox.Item
          key={metric.id}
          label={metric.label}
          status={selectedMetrics.includes(metric.id) ? 'checked' : 'unchecked'}
          onPress={() => toggleMetric(metric.id)}
        />
      ))}

      <SegmentedButtons
        value={period}
        onValueChange={setPeriod}
        buttons={[
          { value: 'day', label: 'Jour' },
          { value: 'week', label: 'Semaine' },
          { value: 'month', label: 'Mois' }
        ]}
      />

      <Button onPress={saveCustomReport}>
        Créer le rapport
      </Button>
    </ScrollView>
  );
};
```

---

### 📦 Livrables Phase 3

- ✅ Prévisions de ventes avec TensorFlow Lite
- ✅ Mode offline complet (WatermelonDB)
- ✅ Queue d'actions pour sync différée
- ✅ Builder de rapports personnalisés
- ✅ Indicateur de connectivité
- ✅ Optimisations performance (lazy loading, pagination infinie)

---

## Stack Technologique Mobile

### Framework Mobile

**Recommandation : React Native** (pour code partagé iOS/Android)

#### Alternative : Flutter
Si l'équipe préfère Dart ou veut des performances légèrement meilleures.

```json
// package.json (React Native)
{
  "dependencies": {
    "react": "18.2.0",
    "react-native": "0.73.0",
    "@react-navigation/native": "^6.1.9",
    "@react-navigation/material-top-tabs": "^6.6.5",

    // UI
    "react-native-paper": "^5.11.3",
    "react-native-vector-icons": "^10.0.3",

    // Charts
    "react-native-chart-kit": "^6.12.0",
    "victory-native": "^36.9.1",

    // State Management
    "@reduxjs/toolkit": "^2.0.1",
    "react-redux": "^9.0.4",

    // Offline & Database
    "@nozbe/watermelondb": "^0.27.1",
    "@react-native-community/netinfo": "^11.2.1",

    // Push Notifications
    "@react-native-firebase/app": "^19.0.1",
    "@react-native-firebase/messaging": "^19.0.1",

    // Camera & Barcode
    "react-native-vision-camera": "^3.6.17",
    "react-native-barcode-scanner": "^1.5.0",

    // ML
    "@tensorflow/tfjs": "^4.15.0",
    "@tensorflow/tfjs-react-native": "^0.8.0",

    // Utils
    "date-fns": "^3.0.6",
    "react-native-reanimated": "^3.6.1",
    "react-native-gesture-handler": "^2.14.1"
  }
}
```

---

### Backend API pour Mobile

**GraphQL pour optimisation :**

```java
// pom.xml
<dependency>
    <groupId>com.graphql-java-kickstart</groupId>
    <artifactId>graphql-spring-boot-starter</artifactId>
    <version>15.0.0</version>
</dependency>

<dependency>
    <groupId>com.graphql-java-kickstart</groupId>
    <artifactId>graphql-java-tools</artifactId>
    <version>13.1.1</version>
</dependency>
```

**Schema GraphQL :**
```graphql
# src/main/resources/graphql/schema.graphqls
type Query {
    dashboard(date: Date!): DashboardData!
    alerts(types: [AlertType!]): [Alert!]!
    productQuickInfo(id: ID!): ProductQuickInfo!
    performance(period: Period!): PerformanceData!
}

type Mutation {
    resolveAlert(id: ID!): Boolean!
    createOrder(input: CreateOrderInput!): Order!
}

type Subscription {
    alertAdded: Alert!
    salesUpdated: SalesUpdate!
}
```

**Resolver :**
```java
@Component
public class MobileQueryResolver implements GraphQLQueryResolver {

    public DashboardData dashboard(LocalDate date) {
        return dashboardService.getDashboardData(date);
    }

    public List<Alert> alerts(List<AlertType> types) {
        return alertService.getAlerts(types);
    }
}
```

---

### Push Notifications

**Firebase Cloud Messaging (FCM) :**

```java
// pom.xml
<dependency>
    <groupId>com.google.firebase</groupId>
    <artifactId>firebase-admin</artifactId>
    <version>9.2.0</version>
</dependency>
```

**Configuration :**
```java
@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp initializeFirebase() throws IOException {
        FileInputStream serviceAccount = new FileInputStream(
            "src/main/resources/firebase-service-account.json"
        );

        FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build();

        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
```

---

## Design Patterns Mobile

### 1. Repository Pattern (Abstraction Data Source)

```typescript
// repositories/ReportRepository.ts
export class ReportRepository {

  constructor(
    private api: ApiClient,
    private cache: CacheManager,
    private offline: OfflineManager
  ) {}

  async getDashboard(date: string): Promise<DashboardData> {
    // 1. Vérifier le cache
    const cached = await this.cache.get(`dashboard:${date}`);
    if (cached && !this.cache.isExpired(cached)) {
      return cached.data;
    }

    // 2. Fetch depuis API
    try {
      const data = await this.api.fetchDashboard(date);

      // 3. Mettre en cache
      await this.cache.set(`dashboard:${date}`, data, 300); // 5 min TTL

      return data;
    } catch (error) {
      // 4. Fallback vers données offline
      const offline = await this.offline.getCachedReport('dashboard');
      if (offline) {
        return offline;
      }

      throw error;
    }
  }
}
```

---

### 2. Observer Pattern (State Management)

```typescript
// stores/DashboardStore.ts (MobX)
import { makeAutoObservable } from 'mobx';

export class DashboardStore {
  data: DashboardData | null = null;
  loading = false;
  error: string | null = null;

  constructor(private repository: ReportRepository) {
    makeAutoObservable(this);
  }

  async loadDashboard(date: string) {
    this.loading = true;
    this.error = null;

    try {
      this.data = await this.repository.getDashboard(date);
    } catch (error) {
      this.error = error.message;
    } finally {
      this.loading = false;
    }
  }

  get caVariation(): number {
    if (!this.data) return 0;
    return ((this.data.dailyCA - this.data.previousCA) / this.data.previousCA) * 100;
  }
}

// Usage dans composant
const DashboardScreen = observer(() => {
  const { dashboardStore } = useStores();

  useEffect(() => {
    dashboardStore.loadDashboard(today);
  }, []);

  if (dashboardStore.loading) {
    return <LoadingSpinner />;
  }

  return (
    <View>
      <CACard amount={dashboardStore.data.dailyCA} variation={dashboardStore.caVariation} />
    </View>
  );
});
```

---

### 3. Strategy Pattern (Export Formats)

```typescript
// services/ExportStrategy.ts
interface ExportStrategy {
  export(data: any): Promise<void>;
}

class PDFExportStrategy implements ExportStrategy {
  async export(data: any) {
    const html = generateHTML(data);
    const pdf = await RNHTMLtoPDF.convert({ html });
    await Share.open({ url: pdf.filePath });
  }
}

class ImageExportStrategy implements ExportStrategy {
  async export(data: any) {
    const screenshot = await captureRef(viewRef);
    await CameraRoll.save(screenshot);
    showToast('Rapport sauvegardé dans la galerie');
  }
}

class CSVExportStrategy implements ExportStrategy {
  async export(data: any) {
    const csv = convertToCSV(data);
    const path = `${RNFS.DocumentDirectoryPath}/report.csv`;
    await RNFS.writeFile(path, csv);
    await Share.open({ url: `file://${path}` });
  }
}

// Usage
const ExportButton: React.FC = ({ data, format }) => {
  const strategies = {
    pdf: new PDFExportStrategy(),
    image: new ImageExportStrategy(),
    csv: new CSVExportStrategy()
  };

  const handleExport = async () => {
    await strategies[format].export(data);
  };

  return <Button onPress={handleExport}>Exporter ({format.toUpperCase()})</Button>;
};
```

---

## Bonnes Pratiques Mobile

### 1. Optimisation Performances

#### Lazy Loading & Code Splitting
```typescript
// Navigation avec lazy loading
import { lazy, Suspense } from 'react';

const DashboardScreen = lazy(() => import('./screens/DashboardScreen'));
const PerformanceScreen = lazy(() => import('./screens/PerformanceScreen'));

const RootNavigator = () => (
  <NavigationContainer>
    <Suspense fallback={<LoadingScreen />}>
      <Stack.Navigator>
        <Stack.Screen name="Dashboard" component={DashboardScreen} />
        <Stack.Screen name="Performance" component={PerformanceScreen} />
      </Stack.Navigator>
    </Suspense>
  </NavigationContainer>
);
```

---

#### Pagination Infinie (FlatList optimisée)
```typescript
const AlertsList: React.FC = () => {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);

  const loadMore = async () => {
    if (loading) return;

    setLoading(true);
    const newAlerts = await fetchAlerts(page, 20);
    setAlerts([...alerts, ...newAlerts]);
    setPage(page + 1);
    setLoading(false);
  };

  return (
    <FlatList
      data={alerts}
      renderItem={({ item }) => <AlertCard alert={item} />}
      keyExtractor={item => item.id}
      onEndReached={loadMore}
      onEndReachedThreshold={0.5}
      ListFooterComponent={loading ? <ActivityIndicator /> : null}
      initialNumToRender={10}
      maxToRenderPerBatch={10}
      windowSize={5}
      removeClippedSubviews={true}
    />
  );
};
```

---

### 2. Gestion de la Batterie

```typescript
// Ajuster la fréquence de sync selon le niveau de batterie
import { Battery } from 'react-native-battery';

export class SmartSyncService {

  async determineSyncInterval(): Promise<number> {
    const batteryLevel = await Battery.getBatteryLevel();

    if (batteryLevel < 0.2) {
      return 600_000; // 10 min
    } else if (batteryLevel < 0.5) {
      return 300_000; // 5 min
    } else {
      return 60_000; // 1 min
    }
  }

  async startSmartSync() {
    const interval = await this.determineSyncInterval();

    this.syncIntervalId = setInterval(() => {
      this.syncData();
    }, interval);
  }
}
```

---

### 3. Gestion de la Data

```typescript
// Compression des images avant upload
import ImageResizer from 'react-native-image-resizer';

export const compressImage = async (uri: string): Promise<string> => {
  const resized = await ImageResizer.createResizedImage(
    uri,
    800,        // max width
    600,        // max height
    'JPEG',
    80,         // quality
    0,          // rotation
    null
  );

  return resized.uri;
};

// Prefetch des données sur WiFi uniquement
import NetInfo from '@react-native-community/netinfo';

export const prefetchReports = async () => {
  const state = await NetInfo.fetch();

  if (state.type === 'wifi') {
    // Précharger les rapports les plus consultés
    await prefetchDashboard();
    await prefetchTopProducts();
  }
};
```

---

### 4. Sécurité Mobile

```typescript
// Stockage sécurisé des tokens
import * as Keychain from 'react-native-keychain';

export const secureStorage = {
  async setToken(token: string) {
    await Keychain.setGenericPassword('auth', token, {
      service: 'pharma-smart',
      accessible: Keychain.ACCESSIBLE.WHEN_UNLOCKED
    });
  },

  async getToken(): Promise<string | null> {
    const credentials = await Keychain.getGenericPassword({ service: 'pharma-smart' });
    return credentials ? credentials.password : null;
  },

  async deleteToken() {
    await Keychain.resetGenericPassword({ service: 'pharma-smart' });
  }
};

// Détection de jailbreak/root
import JailMonkey from 'jail-monkey';

if (JailMonkey.isJailBroken()) {
  Alert.alert(
    'Appareil non sécurisé',
    'Cette application ne peut pas fonctionner sur un appareil jailbreaké/rooté.'
  );
  // Bloquer l'accès
}
```

---

## Conclusion

Cette roadmap mobile est conçue pour offrir :
- **📱 Expérience mobile native** optimisée
- **⚡ Performance maximale** (offline-first, cache intelligent)
- **🔔 Notifications pertinentes** (pas de spam)
- **🔋 Économie ressources** (batterie, data)
- **📊 Insights actionnables** (pas juste de la data)

### Priorisation Recommandée

**Phase 1 (MVP) - 2-3 sprints :**
- Dashboard quotidien
- Alertes critiques + Push
- Recherche stock + Scan
- Actions prioritaires

**Phase 2 - 3-4 sprints :**
- Rapports performance
- Graphiques interactifs
- Notifications avancées
- Widgets

**Phase 3 - 4-5 sprints :**
- Prévisions ML
- Mode offline complet
- Rapports personnalisables

---

**Document créé le :** 2025-01-23
**Version :** 1.0
**Auteur :** Mobile Architecture Team
