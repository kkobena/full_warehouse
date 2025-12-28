# Plan d'Intégration des Rapports - Pharma Mobile Report

## Analyse Comparative

### Rapports Web vs Mobile

| Rapport Web | Statut Mobile | Priorité |
|-------------|---------------|----------|
| Dashboard CA | Partiellement (DashboardActivity + PerformanceActivity) | - |
| Synthèse des Ventes | **Non implémenté** | Haute |
| Top Produits | Partiellement (Dashboard) | Moyenne |
| Analyse de Rentabilité | **Non implémenté** | Haute |
| Tableaux Comparatifs | ~~Non implémenté~~ | **Exclu** |
| Prévisions de Ventes | **Non implémenté** (TensorFlow désactivé) | Basse |
| Analyse du Panier | **Non implémenté** | Basse |
| Alertes de Stock | Implémenté (AlertsActivity) | - |
| Valorisation du Stock | **Non implémenté** | Haute |
| Rotation du Stock | **Non implémenté** | Moyenne |
| Analyse ABC Pareto | **Non implémenté** | Moyenne |
| Performance Fournisseurs | **Non implémenté** | Haute |
| Rapport de Caisse | ~~Partiellement~~ | **Exclu** |
| Segmentation Clients | ~~Non implémenté~~ | **Exclu** |
| Créances Tiers Payant | **Non implémenté** | Haute |
| Récapitulatif Produits | ~~Non implémenté~~ | **Exclu** |

---

## Rapports Prioritaires à Implémenter

### Phase 1 - Priorité Haute (Essentiel pour le pharmacien)

#### 1.1 Créances Tiers Payant
**Justification:** Critique pour le suivi financier et la trésorerie.

**Fonctionnalités:**
- [ ] Liste des factures impayées avec aging
- [ ] Filtrage par ancienneté (<30j, 30-60j, 60-90j, >90j)
- [ ] Total des créances par catégorie
- [ ] Code couleur selon l'ancienneté
- [ ] Pull-to-refresh

**Endpoints API requis:**
```
GET /api/mobile/tiers-payant/creances/summary
GET /api/mobile/tiers-payant/creances/unpaid?ageCategory={category}
```

**Fichiers à créer:**
- `TiersPayantCreancesActivity.kt`
- `activity_tiers_payant_creances.xml`
- `TiersPayantCreancesViewModel.kt`
- `CreanceAdapter.kt`
- `item_creance.xml`

**Estimation:** 2-3 jours

---

#### 1.2 Performance Fournisseurs
**Justification:** Aide à la décision pour les commandes et négociations.

**Fonctionnalités:**
- [ ] Score de performance (0-100) avec indicateur visuel
- [ ] Délai de livraison moyen
- [ ] Taux de conformité
- [ ] Volume d'achats (30j et 12 mois)
- [ ] Filtrage (Top 10, excellents, moyens, faibles)
- [ ] Recherche par nom/code

**Endpoints API requis:**
```
GET /api/mobile/supplier-performance/all
GET /api/mobile/supplier-performance/top
GET /api/mobile/supplier-performance/summary
```

**Fichiers à créer:**
- `SupplierPerformanceActivity.kt`
- `activity_supplier_performance.xml`
- `SupplierPerformanceViewModel.kt`
- `SupplierAdapter.kt`
- `item_supplier_performance.xml`

**Estimation:** 2-3 jours

---

#### 1.3 Valorisation du Stock
**Justification:** Vue globale de la valeur immobilisée.

**Fonctionnalités:**
- [ ] Valeur totale du stock (PA et PV)
- [ ] Marge potentielle
- [ ] Filtrage par catégorie/rayon
- [ ] Liste des produits avec valeurs
- [ ] Graphique répartition par catégorie

**Endpoints API requis:**
```
GET /api/mobile/stock-valuation/summary
GET /api/mobile/stock-valuation/all
GET /api/mobile/stock-valuation/by-category?categoryId={id}
```

**Fichiers à créer:**
- `StockValuationActivity.kt`
- `activity_stock_valuation.xml`
- `StockValuationViewModel.kt`
- `StockValueAdapter.kt`
- `item_stock_value.xml`

**Estimation:** 2 jours

---

#### 1.4 Analyse de Rentabilité
**Justification:** Identifier les produits rentables vs non rentables.

**Fonctionnalités:**
- [ ] Liste produits avec % marge
- [ ] Classification BCG (Stars, Cash Cows, Question Marks, Dogs)
- [ ] Filtrage par catégorie BCG
- [ ] Produits à faible marge (< seuil)
- [ ] Code couleur selon rentabilité

**Endpoints API requis:**
```
GET /api/mobile/profitability/summary
GET /api/mobile/profitability/all
GET /api/mobile/profitability/by-bcg-category?category={category}
GET /api/mobile/profitability/low-margin?threshold={threshold}
```

**Fichiers à créer:**
- `ProfitabilityActivity.kt`
- `activity_profitability.xml`
- `ProfitabilityViewModel.kt`
- `ProfitabilityAdapter.kt`
- `item_profitability.xml`

**Estimation:** 2-3 jours

---

### Phase 2 - Priorité Moyenne (Utile pour l'analyse)

#### 2.1 Rotation du Stock
**Justification:** Identifier les produits à rotation lente.

**Fonctionnalités:**
- [ ] Taux de rotation annuel
- [ ] Classification ABC
- [ ] Produits à rotation lente
- [ ] CA 12 mois vs stock actuel

**Endpoints API requis:**
```
GET /api/mobile/stock-rotation/all
GET /api/mobile/stock-rotation/slow-moving
GET /api/mobile/stock-rotation/abc-counts
```

**Fichiers à créer:**
- `StockRotationActivity.kt`
- `activity_stock_rotation.xml`
- `StockRotationViewModel.kt`
- `StockRotationAdapter.kt`

**Estimation:** 2 jours

---

#### 2.2 Analyse ABC Pareto
**Justification:** Prioriser les produits générant 80% du CA.

**Fonctionnalités:**
- [ ] Classification A (80% CA), B (15%), C (5%)
- [ ] Top contributeurs au CA
- [ ] Pourcentage cumulé
- [ ] Filtrage par classe

**Endpoints API requis:**
```
GET /api/mobile/abc-pareto/summary
GET /api/mobile/abc-pareto/all
GET /api/mobile/abc-pareto/by-class?class={A|B|C}
```

**Fichiers à créer:**
- `AbcParetoActivity.kt`
- `activity_abc_pareto.xml`
- `AbcParetoViewModel.kt`
- `AbcParetoAdapter.kt`

**Estimation:** 2 jours

---

### Phase 3 - Priorité Basse (Fonctionnalités avancées)

#### 3.1 Prévisions de Ventes (ML)
**Statut:** Désactivé - En attente TensorFlow Lite 2.16.0+ compatible 16KB

**Fonctionnalités prévues:**
- [ ] Prévisions basées sur l'historique
- [ ] Intervalle de confiance
- [ ] Détection saisonnalité
- [ ] Recommandations

**Note:** Réactiver quand TensorFlow Lite supportera les pages 16KB.

---

#### 3.2 Analyse du Panier (Market Basket)
**Justification:** Associations produits pour cross-selling.

**Fonctionnalités:**
- [ ] Produits fréquemment achetés ensemble
- [ ] Métriques Support, Confiance, Lift
- [ ] Règles d'association

**Endpoints API requis:**
```
GET /api/mobile/market-basket/associations
GET /api/mobile/market-basket/summary
```

**Estimation:** 2-3 jours

---

## Architecture Technique - Réutilisation des Services Existants

### Services Backend Existants à Réutiliser

Les services de rapport existants dans `com.kobe.warehouse.service.report` seront réutilisés pour garantir la cohérence des données entre le web et le mobile.

| Rapport Mobile | Service Existant | DTOs Existants |
|----------------|------------------|----------------|
| Créances TP | `TiersPayantReportService` | `TiersPayantInvoiceDTO`, `TiersPayantCreancesSummaryDTO` |
| Performance Fournisseurs | `SupplierPerformanceReportService` | `SupplierPerformanceDTO`, `SupplierPerformanceSummaryDTO` |
| Valorisation Stock | `StockValuationReportService` | `StockValuationDTO`, `StockValuationSummaryDTO` |
| Rentabilité | `ProfitabilityReportService` | `ProductProfitabilityDTO`, `ProfitabilitySummaryDTO` |
| Rotation Stock | `StockRotationReportService` | `StockRotationDTO` |
| ABC Pareto | `ABCParetoReportService` | `ABCParetoDTO`, `ABCParetoSummaryDTO` |

### Méthodes Disponibles par Service

#### TiersPayantReportService
```java
List<TiersPayantInvoiceDTO> getUnpaidInvoices(Integer groupeTiersPayantId, AgeCategory ageCategory);
List<TiersPayantCreancesSummaryDTO> getCreancesSummary();
List<TiersPayantInvoiceDTO> getPaymentHistory(Integer groupeTiersPayantId, LocalDate startDate, LocalDate endDate);
```

#### SupplierPerformanceReportService
```java
List<SupplierPerformanceDTO> getAllSupplierPerformance();
SupplierPerformanceDTO getSupplierPerformance(Integer fournisseurId);
List<SupplierPerformanceDTO> getTopSuppliersByVolume(Integer limit);
List<SupplierPerformanceDTO> getSuppliersByPerformanceScore(Double minScore);
List<SupplierPerformanceDTO> getSuppliersWithDeliveryIssues();
SupplierPerformanceSummaryDTO getSupplierPerformanceSummary();
```

#### StockValuationReportService
```java
List<StockValuationDTO> getAllStockValuation();
List<StockValuationDTO> getStockValuationByCategory(String categorie);
List<StockValuationDTO> getStockValuationByStorage(String storageLocation);
StockValuationSummaryDTO getStockValuationSummary();
```

#### ProfitabilityReportService
```java
List<ProductProfitabilityDTO> getAllProductProfitability();
List<ProductProfitabilityDTO> getProductProfitabilityByCategory(String categorie);
List<ProductProfitabilityDTO> getProductProfitabilityByBCGCategory(BCGCategory bcgCategory);
List<ProductProfitabilityDTO> getTopProfitableProducts(int limit);
List<ProductProfitabilityDTO> getLowMarginProducts();
ProfitabilitySummaryDTO getProfitabilitySummary();
```

#### StockRotationReportService
```java
List<StockRotationDTO> getAllStockRotation();
List<StockRotationDTO> getStockRotationByCategory(String categorie);
List<StockRotationDTO> getStockRotationByABCClassification(CategorieABC categorieABC);
Map<CategorieABC, Long> getStockRotationCountByABCClassification();
List<StockRotationDTO> getSlowMovingProducts();
```

#### ABCParetoReportService
```java
List<ABCParetoDTO> getAllABCParetoAnalysis();
List<ABCParetoDTO> getABCParetoByCategory(String categorie);
List<ABCParetoDTO> getABCParetoByClass(ClassePareto classePareto);
List<ABCParetoDTO> getTopRevenueContributors(int limit);
ABCParetoSummaryDTO getABCParetoSummary();
```

### Structure des fichiers mobile

```
pharma-mobile-report/src/main/java/com/kobe/warehouse/reports/
├── data/
│   ├── model/
│   │   ├── TiersPayantCreance.kt     (mapping de TiersPayantInvoiceDTO)
│   │   ├── SupplierPerformance.kt    (mapping de SupplierPerformanceDTO)
│   │   ├── StockValuation.kt         (mapping de StockValuationDTO)
│   │   ├── Profitability.kt          (mapping de ProductProfitabilityDTO)
│   │   ├── StockRotation.kt          (mapping de StockRotationDTO)
│   │   └── AbcPareto.kt              (mapping de ABCParetoDTO)
│   └── api/
│       └── ReportApiService.kt (ajouter endpoints)
├── ui/
│   ├── activity/
│   │   ├── TiersPayantCreancesActivity.kt
│   │   ├── SupplierPerformanceActivity.kt
│   │   ├── StockValuationActivity.kt
│   │   ├── ProfitabilityActivity.kt
│   │   ├── StockRotationActivity.kt
│   │   └── AbcParetoActivity.kt
│   ├── viewmodel/
│   │   └── [ViewModels correspondants]
│   └── adapter/
│       └── [Adapters correspondants]
└── res/
    └── layout/
        └── [Layouts correspondants]
```

---

## Calendrier Suggéré

| Phase | Rapports | Durée Estimée | Dépendances |
|-------|----------|---------------|-------------|
| **Phase 1** | Créances TP, Fournisseurs, Valorisation, Rentabilité | 1-2 semaines | Endpoints REST uniquement |
| **Phase 2** | Rotation, ABC | 1 semaine | Phase 1 complète |
| **Phase 3** | Prévisions ML, Panier | 1-2 semaines | TensorFlow 2.16.0+ |

**Note:** Durées réduites car les services backend existent déjà.

---

## Prérequis Backend

### Endpoints REST à ajouter dans MobileReportResource.java

Les endpoints REST exposent directement les services existants :

```java
@RestController
@RequestMapping("/api/mobile/reports")
public class MobileReportResource {

    private final TiersPayantReportService tiersPayantReportService;
    private final SupplierPerformanceReportService supplierPerformanceReportService;
    private final StockValuationReportService stockValuationReportService;
    private final ProfitabilityReportService profitabilityReportService;
    private final StockRotationReportService stockRotationReportService;
    private final ABCParetoReportService abcParetoReportService;

    // Créances Tiers Payant
    @GetMapping("/tiers-payant/creances/summary")
    public List<TiersPayantCreancesSummaryDTO> getCreancesSummary() {
        return tiersPayantReportService.getCreancesSummary();
    }

    @GetMapping("/tiers-payant/creances/unpaid")
    public List<TiersPayantInvoiceDTO> getUnpaidInvoices(
            @RequestParam(required = false) Integer groupeId,
            @RequestParam(required = false) TiersPayantInvoiceDTO.AgeCategory ageCategory) {
        return tiersPayantReportService.getUnpaidInvoices(groupeId, ageCategory);
    }

    // Performance Fournisseurs
    @GetMapping("/supplier-performance/all")
    public List<SupplierPerformanceDTO> getAllSupplierPerformance() {
        return supplierPerformanceReportService.getAllSupplierPerformance();
    }

    @GetMapping("/supplier-performance/top")
    public List<SupplierPerformanceDTO> getTopSuppliers(@RequestParam(defaultValue = "10") Integer limit) {
        return supplierPerformanceReportService.getTopSuppliersByVolume(limit);
    }

    @GetMapping("/supplier-performance/summary")
    public SupplierPerformanceSummaryDTO getSupplierPerformanceSummary() {
        return supplierPerformanceReportService.getSupplierPerformanceSummary();
    }

    // Valorisation Stock
    @GetMapping("/stock-valuation/all")
    public List<StockValuationDTO> getAllStockValuation() {
        return stockValuationReportService.getAllStockValuation();
    }

    @GetMapping("/stock-valuation/summary")
    public StockValuationSummaryDTO getStockValuationSummary() {
        return stockValuationReportService.getStockValuationSummary();
    }

    @GetMapping("/stock-valuation/by-category")
    public List<StockValuationDTO> getStockValuationByCategory(@RequestParam String category) {
        return stockValuationReportService.getStockValuationByCategory(category);
    }

    // Rentabilité
    @GetMapping("/profitability/all")
    public List<ProductProfitabilityDTO> getAllProductProfitability() {
        return profitabilityReportService.getAllProductProfitability();
    }

    @GetMapping("/profitability/summary")
    public ProfitabilitySummaryDTO getProfitabilitySummary() {
        return profitabilityReportService.getProfitabilitySummary();
    }

    @GetMapping("/profitability/by-bcg")
    public List<ProductProfitabilityDTO> getByBCGCategory(@RequestParam BCGCategory category) {
        return profitabilityReportService.getProductProfitabilityByBCGCategory(category);
    }

    @GetMapping("/profitability/low-margin")
    public List<ProductProfitabilityDTO> getLowMarginProducts() {
        return profitabilityReportService.getLowMarginProducts();
    }

    // Rotation Stock
    @GetMapping("/stock-rotation/all")
    public List<StockRotationDTO> getAllStockRotation() {
        return stockRotationReportService.getAllStockRotation();
    }

    @GetMapping("/stock-rotation/slow-moving")
    public List<StockRotationDTO> getSlowMovingProducts() {
        return stockRotationReportService.getSlowMovingProducts();
    }

    @GetMapping("/stock-rotation/abc-counts")
    public Map<CategorieABC, Long> getStockRotationABCCounts() {
        return stockRotationReportService.getStockRotationCountByABCClassification();
    }

    // ABC Pareto
    @GetMapping("/abc-pareto/all")
    public List<ABCParetoDTO> getAllABCPareto() {
        return abcParetoReportService.getAllABCParetoAnalysis();
    }

    @GetMapping("/abc-pareto/summary")
    public ABCParetoSummaryDTO getABCParetoSummary() {
        return abcParetoReportService.getABCParetoSummary();
    }

    @GetMapping("/abc-pareto/by-class")
    public List<ABCParetoDTO> getByParetoClass(@RequestParam ClassePareto classePareto) {
        return abcParetoReportService.getABCParetoByClass(classePareto);
    }
}
```

### Aucun nouveau service à créer

✅ **Tous les services nécessaires existent déjà** dans `com.kobe.warehouse.service.report`

Les seuls travaux backend requis :
1. Ajouter les endpoints REST dans `MobileReportResource.java`
2. Injecter les services existants

---

## Mise à jour ReportsActivity

Ajouter les nouveaux rapports dans `ReportsActivity`:

```kotlin
// Nouvelle grille avec plus de rapports
Row 1: Tableau Pharmacien | Récap. Caisse
Row 2: Rapport Activité | Balance Caisse
Row 3: Rapport TVA | Performances
Row 4: Créances TP | Fournisseurs
Row 5: Valorisation Stock | Rentabilité
Row 6: Rotation Stock | ABC Pareto
```

---


