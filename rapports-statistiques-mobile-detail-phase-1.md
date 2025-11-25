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
