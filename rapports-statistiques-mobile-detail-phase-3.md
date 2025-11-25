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
- ✅ Queue d\'actions pour sync différée
- ✅ Builder de rapports personnalisés
- ✅ Indicateur de connectivité
- ✅ Optimisations performance (lazy loading, pagination infinie)

---
