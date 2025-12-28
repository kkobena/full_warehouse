# Fonctionnalités à Implémenter - Pharma Mobile Report

Ce document liste les fonctionnalités supprimées temporairement pour permettre la compilation du projet.
Ces fonctionnalités devront être ré-implémentées ultérieurement.

---

## 1. Module Prévisions (Forecast)

### Fichiers supprimés

#### UI / Activities
- `ui/forecast/ForecastFragment.kt` - Fragment principal des prévisions
- `ui/forecast/ForecastViewModel.kt` - ViewModel pour les prévisions

#### Layouts requis
- `res/layout/fragment_forecast.xml` - Layout du fragment prévisions
- `res/layout/item_forecast_day.xml` - Item pour afficher une prévision journalière
- `res/layout/item_recommendation.xml` - Item pour les recommandations

#### Adapters requis
- `ui/adapter/ForecastDayAdapter.kt` - Adapter pour la liste des prévisions
- `ui/adapter/RecommendationsAdapter.kt` - Adapter pour les recommandations

#### Data / Repository
- `data/repository/ForecastRepository.kt` - Repository pour les données de prévision

#### Dépendances
- Nécessite `RetrofitClient` ou utiliser `ApiClient` existant
- Nécessite les endpoints API:
  - `GET /api/mobile/forecast/history` ✅ (existe déjà dans ReportApiService)
  - `GET /api/mobile/forecast/statistics` ✅ (existe déjà dans ReportApiService)

### Fonctionnalités prévues
- [ ] Prévisions de ventes basées sur l'historique
- [ ] Graphiques de tendances
- [ ] Recommandations ML (TensorFlow Lite intégré)
- [ ] Alertes de stock basées sur les prévisions

---

## 2. Module Rapports Personnalisés (Custom Reports)

### Fichiers supprimés

#### UI / Screens
- `ui/customreport/CustomReportBuilderScreen.kt` - Écran de construction de rapports
- `ui/customreport/CustomReportBuilderViewModel.kt` - ViewModel

#### Layouts requis
- `res/layout/fragment_custom_report_builder.xml` - Layout du constructeur de rapports

#### Adapters requis
- `ui/adapter/MetricsSelectionAdapter.kt` - Sélection des métriques
- `ui/adapter/ReportTemplatesAdapter.kt` - Templates de rapports

#### Data / Repository
- `data/repository/CustomReportRepository.kt` - Repository pour les rapports personnalisés

#### Services
- `service/CustomReportService.kt` - Service de génération de rapports

#### Models
- `data/model/CustomReport.kt` - Modèle de rapport personnalisé (supprimé car doublon avec ChartDataPoint)

#### Dépendances
- Nécessite `RetrofitClient` ou utiliser `ApiClient` existant
- Nécessite les endpoints API:
  - `POST /api/mobile/custom-reports/generate` ✅ (existe déjà)
  - `GET /api/mobile/custom-reports/available-metrics` ✅ (existe déjà)

### Fonctionnalités prévues
- [ ] Constructeur de rapports drag-and-drop
- [ ] Sélection de métriques personnalisées
- [ ] Templates de rapports prédéfinis
- [ ] Export PDF/Excel des rapports
- [ ] Sauvegarde des configurations de rapports

---

## 3. Corrections à apporter

### Classes à vérifier/corriger

| Fichier | Problème | Solution |
|---------|----------|----------|
| `data/model/CustomReport.kt` | Doublon de `ChartDataPoint` | Utiliser celui de `PharmacistDashboard.kt` |
| `ForecastRepository.kt` | Référence à `ApiService` inexistant | Utiliser `ReportApiService` via `ApiClient` |
| `CustomReportRepository.kt` | Méthodes DAO manquantes | Implémenter dans `AppDatabase` |

### Endpoints API Backend requis

Les endpoints suivants doivent être implémentés côté backend Spring Boot:

```java
// Forecast endpoints
@GetMapping("/api/mobile/forecast/history")
@GetMapping("/api/mobile/forecast/statistics")

// Custom reports endpoints
@PostMapping("/api/mobile/custom-reports/generate")
@GetMapping("/api/mobile/custom-reports/available-metrics")
```

---

## 4. Ressources manquantes créées

Les ressources suivantes ont été créées comme placeholders:

### Drawables
- [x] `ic_notification.xml`
- [x] `ic_account_balance.xml`
- [x] `ic_category.xml`
- [x] `ic_analytics.xml`
- [x] `ic_price_tag.xml`
- [x] `ic_sale.xml`
- [x] `ic_alert_circle.xml`
- [x] `skeleton_chip.xml`
- [x] `widget_preview.xml`

### Layouts
- [x] `skeleton_row.xml`

### Colors
- [x] `border`
- [x] `purple_500`
- [x] `warning_dark`

---

## 5. Configuration Firebase

Le fichier `google-services.json` actuel est un placeholder.

### Pour la production:
1. Aller sur [Firebase Console](https://console.firebase.google.com)
2. Créer/sélectionner le projet
3. Ajouter l'application Android avec le package:
   - Debug: `com.kobe.warehouse.reports.debug`
   - Release: `com.kobe.warehouse.reports`
4. Télécharger le vrai `google-services.json`
5. Remplacer le fichier placeholder

---

## 6. Priorité d'implémentation suggérée

### Phase 1 - Rapports personnalisés (Priorité haute)
1. Créer le layout `fragment_custom_report_builder.xml`
2. Implémenter `CustomReportBuilderViewModel.kt`
3. Implémenter `CustomReportBuilderScreen.kt`
4. Créer les adapters nécessaires

### Phase 2 - Prévisions ML (Priorité moyenne)
1. Créer les layouts forecast
2. Implémenter `ForecastRepository.kt` avec `ApiClient`
3. Implémenter `ForecastViewModel.kt`
4. Intégrer TensorFlow Lite pour les prévisions locales

### Phase 3 - Améliorations (Priorité basse)
1. Ajouter export PDF/Excel
2. Améliorer les graphiques
3. Ajouter notifications push pour les prévisions

---

## 7. Notes techniques

### Dépendances déjà configurées
```kotlin
// TensorFlow Lite (dans build.gradle.kts)
implementation("org.tensorflow:tensorflow-lite:2.14.0")
implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
```

### Versions mises à jour pour compilation
- Kotlin: `2.2.0`
- Room: `2.7.0-alpha12`
- AGP: `8.13.2`

---

*Document créé le: 2024-12-28*
*Dernière mise à jour: 2024-12-28*
