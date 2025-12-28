# Plan d'Intégration - Rapports Principaux Mobile

## Objectif

Intégrer les rapports de gestion de caisse du module web Angular dans l'application mobile `pharma-mobile-report`, en conservant **toutes les données** affichées dans le frontend web.

---

## 1. TABLEAU PHARMACIEN

### 1.1 Données Web (Angular)

**Source:** `tableau-pharmacien.component.ts`
**Endpoint existant:** `GET /api/tableau-pharmacien`

#### Paramètres de requête
```typescript
{
  fromDate: string,      // Format YYYY-MM-DD
  toDate: string,        // Format YYYY-MM-DD
  statuts: string[],     // ['CLOSED']
  groupBy: string        // 'daily' | 'month'
}
```

#### Structure de réponse (TableauPharmacienWrapper)
```typescript
interface TableauPharmacienWrapper {
  // === VENTES ===
  montantVenteComptant: number;      // Ventes comptant
  montantVenteCredit: number;        // Ventes crédit
  montantVenteRemise: number;        // Remises accordées
  montantVenteNet: number;           // Ventes nettes (HT)
  montantVenteTtc: number;           // Ventes TTC
  numberCount: number;               // Nombre de transactions

  // === ACHATS ===
  montantAchatNet: number;           // Achats nets
  montantAchatTaxe: number;          // TVA sur achats
  montantAchatRemise: number;        // Remises fournisseurs
  montantAvoirFournisseur: number;   // Avoirs fournisseurs

  // === RATIOS ===
  ratioVenteAchat: number;           // Ratio Ventes/Achats
  ratioAchatVente: number;           // Ratio Achats/Ventes

  // === DETAILS PAR FOURNISSEUR ===
  fournisseurAchats: FournisseurAchat[];

  // === GRAPHIQUES ===
  // Données pour graphique barres verticales
}

interface FournisseurAchat {
  id: number;
  libelle: string;                   // Nom fournisseur
  montantAchatNet: number;           // Montant achats
  montantAchatTaxe: number;          // TVA
  montantAchatRemise: number;        // Remises
  montantAvoirFournisseur: number;   // Avoirs
}
```

#### Exports disponibles
- `GET /api/tableau-pharmacien/pdf` - Export PDF
- `GET /api/tableau-pharmacien/excel` - Export Excel

### 1.2 Endpoint Mobile à créer

```
GET /api/mobile/reports/pharmacist-dashboard
```

#### DTO Mobile (Java)
```java
public record MobilePharmacistDashboardDTO(
    // Période
    LocalDate fromDate,
    LocalDate toDate,
    String periodLabel,

    // Ventes
    long montantVenteComptant,
    long montantVenteCredit,
    long montantVenteRemise,
    long montantVenteNet,
    long montantVenteTtc,
    int transactionsCount,

    // Achats
    long montantAchatNet,
    long montantAchatTaxe,
    long montantAchatRemise,
    long montantAvoirFournisseur,

    // Ratios
    double ratioVenteAchat,
    double ratioAchatVente,

    // Marge calculée
    long marge,
    double margePercent,

    // Variations vs période précédente
    double ventesVariation,
    double achatsVariation,

    // Détails fournisseurs (top 10)
    List<FournisseurAchatDTO> topFournisseurs,

    // Données graphique
    List<ChartDataPoint> chartVentesAchats
) {}

public record FournisseurAchatDTO(
    long id,
    String libelle,
    long montantNet,
    long montantTaxe,
    double percentTotal
) {}
```

---

## 2. BALANCE CAISSE

### 2.1 Données Web (Angular)

**Source:** `balance-mvt-caisse.component.ts`
**Endpoint existant:** `GET /api/balances`

#### Paramètres de requête
```typescript
{
  fromDate: string,
  toDate: string,
  statuts: string[],
  categorieChiffreAffaires: string[]  // Types de ventes
}
```

#### Structure de réponse (BalanceCaisseWrapper)
```typescript
interface BalanceCaisseWrapper {
  // === TOTAUX GENERAUX ===
  count: number;                     // Nombre transactions
  montantTtc: number;                // Total TTC
  montantHt: number;                 // Total HT
  montantDiscount: number;           // Total remises
  montantNet: number;                // Total net
  montantTaxe: number;               // Total TVA
  panierMoyen: number;               // Panier moyen

  // === PAR MODE DE PAIEMENT ===
  montantCash: number;               // Espèces
  montantCheck: number;              // Chèques
  montantCard: number;               // Cartes bancaires
  montantVirement: number;           // Virements
  montantMobileMoney: number;        // Mobile Money

  // === CATEGORIES SPECIALES ===
  montantCredit: number;             // Ventes à crédit
  montantDiffere: number;            // Paiements différés
  partTiersPayant: number;           // Part tiers payant

  // === METRIQUES BUSINESS ===
  montantAchat: number;              // Total achats
  montantMarge: number;              // Marge brute
  montantPaye: number;               // Montant encaissé
  ratioVenteAchat: number;
  ratioAchatVente: number;

  // === DETAILS ===
  mvtCaissesByModes: Tuple[];        // Mouvements par mode
  mvtCaisses: MvtCaisse[];           // Tous les mouvements
  balanceCaisses: BalanceCaisse[];   // Balance par type vente
}

interface Tuple {
  libelle: string;
  value: number;
}

interface BalanceCaisse {
  categorieChiffreAffaire: string;   // Type: VO, VNO, etc.
  count: number;
  montantTtc: number;
  montantHt: number;
  montantNet: number;
  montantDiscount: number;
  montantTaxe: number;
  montantAchat: number;
  montantMarge: number;
  panierMoyen: number;
  // Paiements par mode pour cette catégorie
  montantCash: number;
  montantCard: number;
  montantCheck: number;
  montantVirement: number;
  montantMobileMoney: number;
  montantCredit: number;
  montantDiffere: number;
  partTiersPayant: number;
}

interface MvtCaisse {
  id: number;
  libelle: string;
  montant: number;
  typeMvt: string;                   // ENTREE | SORTIE
  dateMvt: string;
  comment: string;
}
```

### 2.2 Endpoint Mobile à créer

```
GET /api/mobile/reports/cash-balance
```

#### DTO Mobile (Java)
```java
public record MobileCashBalanceDTO(
    LocalDate fromDate,
    LocalDate toDate,

    // Totaux
    int transactionsCount,
    long montantTtc,
    long montantHt,
    long montantNet,
    long montantRemise,
    long montantTva,
    long panierMoyen,

    // Par mode de paiement
    long montantEspeces,
    long montantCartes,
    long montantCheques,
    long montantVirements,
    long montantMobileMoney,
    long montantCredit,
    long montantDiffere,
    long montantTiersPayant,

    // Métriques
    long montantAchats,
    long montantMarge,
    double ratioVenteAchat,

    // Répartition par mode (pour graphique)
    List<PaymentModeBreakdown> paymentBreakdown,

    // Balance par catégorie de vente
    List<CategoryBalanceDTO> categoryBalances,

    // Mouvements de caisse
    List<CashMovementDTO> cashMovements
) {}

public record PaymentModeBreakdown(
    String mode,
    String label,
    long montant,
    double percent,
    String color
) {}

public record CategoryBalanceDTO(
    String category,
    String label,
    int count,
    long montantTtc,
    long montantNet,
    long marge,
    long panierMoyen
) {}

public record CashMovementDTO(
    long id,
    String libelle,
    long montant,
    String type,  // ENTREE | SORTIE
    LocalDateTime date
) {}
```

---

## 3. RAPPORT TVA

### 3.1 Données Web (Angular)

**Source:** `taxe-report.component.ts`
**Endpoint existant:** `GET /api/taxe-report`

#### Paramètres de requête
```typescript
{
  fromDate: string,
  toDate: string,
  statuts: string[],
  groupBy: string,           // 'codeTva' | 'daily'
  typeVente: string          // TypeFinancialTransaction (optionnel)
}
```

#### Structure de réponse (TaxeWrapper)
```typescript
interface TaxeWrapper {
  // === TOTAUX ===
  montantHt: number;                 // Total HT
  montantTaxe: number;               // Total TVA
  montantTtc: number;                // Total TTC
  montantNet: number;                // Total net
  montantRemise: number;             // Total remises
  montantAchat: number;              // Total achats

  // === LIGNES DETAIL ===
  taxeReports: TaxeReport[];

  // === GRAPHIQUE ===
  chart: {
    labeles: string[];               // Codes TVA ou dates
    data: number[];                  // Montants correspondants
  }
}

interface TaxeReport {
  codeTva: string;                   // Code TVA (A, B, C, D, E)
  tauxTva: number;                   // Taux (0%, 5%, 10%, 18%)
  montantHt: number;
  montantTaxe: number;
  montantTtc: number;
  mvtDate: string;                   // Date si groupBy=daily
}
```

### 3.2 Endpoint Mobile à créer

```
GET /api/mobile/reports/vat
```

#### DTO Mobile (Java)
```java
public record MobileVatReportDTO(
    LocalDate fromDate,
    LocalDate toDate,

    // Totaux
    long montantHt,
    long montantTva,
    long montantTtc,
    long montantNet,
    long montantRemise,

    // Détail par code TVA
    List<VatLineDTO> vatLines,

    // Graphique (camembert)
    List<ChartDataPoint> chartData
) {}

public record VatLineDTO(
    String codeTva,
    String libelle,           // "TVA 18%", "Exonéré", etc.
    double tauxTva,
    long montantHt,
    long montantTva,
    long montantTtc,
    double percentTotal
) {}
```

---

## 4. RAPPORT D'ACTIVITE

### 4.1 Données Web (Angular)

**Source:** `activity-summary.component.ts`
**Endpoints existants:**
- `GET /api/activity-summary/ca` - Chiffre d'affaires
- `GET /api/activity-summary/recettes` - Recettes par mode
- `GET /api/activity-summary/mouvements-caisse` - Mouvements
- `GET /api/activity-summary/achats` - Achats par fournisseur
- `GET /api/activity-summary/reglements-tiers-payants` - Règlements TP
- `GET /api/activity-summary/achats-tiers-payants` - Achats TP

#### Structure ChiffreAffaire
```typescript
interface ChiffreAffaire {
  montantTtc: number;                // CA TTC
  montantTva: number;                // TVA collectée
  montantHt: number;                 // CA HT
  montantRemise: number;             // Remises
  montantNet: number;                // CA Net
  montantEspece: number;             // Encaissements espèces
  montantAutreModePaiement: number;  // Autres modes
  montantCredit: number;             // Ventes crédit
  marge: number;                     // Marge brute
}
```

#### Structure Recette
```typescript
interface Recette {
  modePaimentLibelle: string;        // Mode de paiement
  montantReel: number;               // Montant encaissé
}
```

#### Structure MouvementCaisse
```typescript
interface MouvementCaisse {
  libelle: string;
  montant: number;
}
```

#### Structure GroupeFournisseurAchat
```typescript
interface GroupeFournisseurAchat {
  libelle: string;                   // Nom groupe fournisseur
  montantTtc: number;
  montantTva: number;
  montantHt: number;
}
```

#### Structure ReglementTiersPayant
```typescript
interface ReglementTiersPayant {
  libelle: string;                   // Nom tiers payant
  type: string;                      // Catégorie
  factureNumber: string;             // N° facture
  montantFacture: number;            // Montant facturé
  montantReglement: number;          // Montant payé
  montantRestant: number;            // Reste à payer
}
```

#### Structure AchatTiersPayant
```typescript
interface AchatTiersPayant {
  libelle: string;                   // Nom tiers payant
  categorie: string;                 // Catégorie
  bonsCount: number;                 // Nombre de bons
  montant: number;                   // Montant total
  clientCount: number;               // Nombre de clients
}
```

### 4.2 Endpoint Mobile à créer

```
GET /api/mobile/reports/activity
```

#### DTO Mobile (Java)
```java
public record MobileActivityReportDTO(
    LocalDate fromDate,
    LocalDate toDate,

    // === CHIFFRE D'AFFAIRES ===
    ChiffreAffaireDTO chiffreAffaire,

    // === RECETTES PAR MODE ===
    List<RecetteDTO> recettes,
    long totalRecettes,

    // === MOUVEMENTS CAISSE ===
    List<MouvementCaisseDTO> mouvementsCaisse,
    long totalEntrees,
    long totalSorties,

    // === ACHATS FOURNISSEURS ===
    List<GroupeFournisseurAchatDTO> achatsFournisseurs,
    long totalAchats,

    // === TIERS PAYANTS ===
    TiersPayantSummaryDTO tiersPayants
) {}

public record ChiffreAffaireDTO(
    long montantTtc,
    long montantTva,
    long montantHt,
    long montantRemise,
    long montantNet,
    long montantEspece,
    long montantAutreMode,
    long montantCredit,
    long marge,
    double margePercent
) {}

public record RecetteDTO(
    String mode,
    String libelle,
    long montant,
    double percent,
    String color
) {}

public record MouvementCaisseDTO(
    String libelle,
    long montant,
    String type  // ENTREE | SORTIE
) {}

public record GroupeFournisseurAchatDTO(
    long id,
    String libelle,
    long montantTtc,
    long montantTva,
    long montantHt,
    double percentTotal
) {}

public record TiersPayantSummaryDTO(
    // Règlements reçus
    List<ReglementTiersPayantDTO> reglements,
    long totalFacture,
    long totalRegle,
    long totalRestant,

    // Achats/Bons
    List<AchatTiersPayantDTO> achats,
    int totalBons,
    long totalMontantAchats,
    int totalClients
) {}

public record ReglementTiersPayantDTO(
    String libelle,
    String type,
    String factureNumber,
    long montantFacture,
    long montantReglement,
    long montantRestant
) {}

public record AchatTiersPayantDTO(
    String libelle,
    String categorie,
    int bonsCount,
    long montant,
    int clientCount
) {}
```

---

## 5. RECAPITULATIF CAISSE (TICKET Z)

### 5.1 Données Web (Angular)

**Source:** `recapitualtif-caisse.component.ts`
**Endpoint existant:** `GET /api/ticketz`

#### Paramètres de requête
```typescript
{
  fromDate: string,
  toDate: string,
  fromTime: string,          // HH:mm (optionnel)
  toTime: string,            // HH:mm (optionnel)
  statuts: string[],
  userId: number[],          // Filtrer par utilisateurs
  onlyVente: boolean         // true = ventes seules
}
```

#### Structure de réponse (Ticket)
```typescript
interface Ticket {
  // === RESUME GLOBAL ===
  summaries: RecapItem[];

  // === DETAILS PAR CAISSIER ===
  recaps: Recap[];
}

interface RecapItem {
  libelle: string;                   // Label
  value: number;                     // Valeur
}

interface Recap {
  userName: string;                  // Nom du caissier
  datas: RecapItem[];                // Détails transactions
  summary: RecapItem[];              // Sous-totaux caissier
}
```

#### Labels typiques RecapItem
```
- "Vente comptoir"
- "Vente VO"
- "Vente VNO"
- "Espèces"
- "Cartes"
- "Chèques"
- "Mobile Money"
- "Virements"
- "Crédit"
- "Remises"
- "Avoirs"
- "Total TTC"
- "Total HT"
- "TVA"
- "Nombre de tickets"
- "Panier moyen"
```

#### Exports disponibles
- `GET /api/ticketz/pdf` - Export PDF
- `GET /api/ticketz/print` - Impression directe
- `GET /api/ticketz/print-tauri` - Données ESC-POS
- `GET /api/ticketz/email` - Envoi email

### 5.2 Endpoint Mobile à créer

```
GET /api/mobile/reports/cash-summary
```

#### DTO Mobile (Java)
```java
public record MobileCashSummaryDTO(
    LocalDate fromDate,
    LocalDate toDate,
    String fromTime,
    String toTime,

    // === RESUME GLOBAL ===
    GlobalSummaryDTO globalSummary,

    // === PAR CAISSIER ===
    List<CashierRecapDTO> cashierRecaps,

    // === TOTAUX CALCULES ===
    long totalTtc,
    long totalHt,
    long totalTva,
    long totalEspeces,
    long totalCartes,
    long totalMobileMoney,
    long totalCheques,
    long totalVirements,
    long totalCredit,
    long totalRemises,
    int totalTransactions,
    long panierMoyen
) {}

public record GlobalSummaryDTO(
    List<SummaryItemDTO> items
) {}

public record SummaryItemDTO(
    String key,
    String libelle,
    long value,
    String type  // AMOUNT | COUNT | PERCENT
) {}

public record CashierRecapDTO(
    long userId,
    String userName,
    String userInitials,

    // Résumé caissier
    long totalVentes,
    int nbTransactions,
    long panierMoyen,

    // Détail par type
    List<SummaryItemDTO> details,

    // Sous-totaux
    List<SummaryItemDTO> summary
) {}
```

---

## 6. STRUCTURE FICHIERS MOBILE

### 6.1 Backend (Java)

```
src/main/java/com/kobe/warehouse/
├── web/rest/mobile/
│   └── MobileReportsResource.java           // Controller
│
├── service/mobile/
│   ├── MobileReportsService.java            // Service principal
│   ├── MobilePharmacistDashboardService.java
│   ├── MobileCashBalanceService.java
│   ├── MobileVatReportService.java
│   ├── MobileActivityReportService.java
│   └── MobileCashSummaryService.java
│
└── service/dto/mobile/
    ├── MobilePharmacistDashboardDTO.java
    ├── MobileCashBalanceDTO.java
    ├── MobileVatReportDTO.java
    ├── MobileActivityReportDTO.java
    ├── MobileCashSummaryDTO.java
    └── common/
        ├── ChartDataPoint.java
        ├── PaymentModeBreakdown.java
        └── SummaryItemDTO.java
```

### 6.2 Mobile (Kotlin)

```
pharma-mobile-report/src/main/java/com/kobe/warehouse/reports/
├── data/
│   ├── model/
│   │   ├── PharmacistDashboard.kt
│   │   ├── CashBalance.kt
│   │   ├── VatReport.kt
│   │   ├── ActivityReport.kt
│   │   └── CashSummary.kt
│   │
│   ├── api/
│   │   └── ReportApiService.kt              // Ajouts endpoints
│   │
│   └── repository/
│       └── ReportRepository.kt              // Ajouts méthodes
│
├── ui/
│   ├── activity/
│   │   ├── ReportsMenuActivity.kt           // Menu rapports
│   │   ├── PharmacistDashboardActivity.kt
│   │   ├── CashBalanceActivity.kt
│   │   ├── VatReportActivity.kt
│   │   ├── ActivityReportActivity.kt
│   │   └── CashSummaryActivity.kt
│   │
│   ├── viewmodel/
│   │   ├── PharmacistDashboardViewModel.kt
│   │   ├── CashBalanceViewModel.kt
│   │   ├── VatReportViewModel.kt
│   │   ├── ActivityReportViewModel.kt
│   │   └── CashSummaryViewModel.kt
│   │
│   ├── adapter/
│   │   ├── SupplierPurchaseAdapter.kt
│   │   ├── PaymentBreakdownAdapter.kt
│   │   ├── VatLineAdapter.kt
│   │   ├── CashierRecapAdapter.kt
│   │   └── ThirdPartyAdapter.kt
│   │
│   └── fragment/
│       └── DateRangePickerFragment.kt       // Sélecteur période
│
└── res/
    ├── layout/
    │   ├── activity_reports_menu.xml
    │   ├── activity_pharmacist_dashboard.xml
    │   ├── activity_cash_balance.xml
    │   ├── activity_vat_report.xml
    │   ├── activity_activity_report.xml
    │   ├── activity_cash_summary.xml
    │   ├── item_supplier_purchase.xml
    │   ├── item_payment_breakdown.xml
    │   ├── item_vat_line.xml
    │   ├── item_cashier_recap.xml
    │   ├── skeleton_report.xml
    │   └── fragment_date_range_picker.xml
    │
    ├── menu/
    │   └── bottom_nav_menu.xml              // Ajout tab Rapports
    │
    └── values/
        └── strings_reports.xml
```

---

## 7. NAVIGATION MOBILE

### 7.1 Modification BottomNavigationView

```xml
<!-- Ajouter dans bottom_nav_menu.xml -->
<item
    android:id="@+id/nav_reports"
    android:icon="@drawable/ic_reports"
    android:title="@string/nav_reports" />
```

### 7.2 Menu Rapports (ReportsMenuActivity)

```
┌─────────────────────────────────┐
│ ◀ Rapports                      │
├─────────────────────────────────┤
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 📊 Tableau Pharmacien       │ │
│ │ Vision globale ventes/achats│ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 🧾 Récapitulatif Caisse     │ │
│ │ Ticket Z - Clôture journée  │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 📋 Rapport d'Activité       │ │
│ │ CA, recettes, achats, TP    │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 💰 Balance Caisse           │ │
│ │ Détail par mode paiement    │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 📄 Rapport TVA              │ │
│ │ Ventilation par taux        │ │
│ └─────────────────────────────┘ │
│                                 │
└─────────────────────────────────┘
```

---

## 8. ENDPOINTS BACKEND RESUME

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/mobile/reports/pharmacist-dashboard` | GET | Tableau pharmacien |
| `/api/mobile/reports/pharmacist-dashboard/pdf` | GET | Export PDF |
| `/api/mobile/reports/cash-balance` | GET | Balance caisse |
| `/api/mobile/reports/cash-balance/pdf` | GET | Export PDF |
| `/api/mobile/reports/vat` | GET | Rapport TVA |
| `/api/mobile/reports/vat/pdf` | GET | Export PDF |
| `/api/mobile/reports/activity` | GET | Rapport d'activité |
| `/api/mobile/reports/activity/pdf` | GET | Export PDF |
| `/api/mobile/reports/cash-summary` | GET | Récap caisse (Ticket Z) |
| `/api/mobile/reports/cash-summary/pdf` | GET | Export PDF |
| `/api/mobile/reports/cash-summary/share` | POST | Partager par email |

### Paramètres communs

```
?fromDate=2024-12-01&toDate=2024-12-27
```

---

## 9. ORDRE D'IMPLEMENTATION

| Phase | Rapport | Priorité | Effort |
|-------|---------|----------|--------|
| 1 | Tableau Pharmacien | 🔴 Critique | 2 jours |
| 2 | Récapitulatif Caisse | 🔴 Critique | 2 jours |
| 3 | Rapport d'Activité | 🟠 Haute | 2 jours |
| 4 | Balance Caisse | 🟠 Haute | 1.5 jours |
| 5 | Rapport TVA | 🟡 Moyenne | 1 jour |
| 6 | Menu & Navigation | - | 0.5 jour |
| 7 | Export PDF & Partage | - | 1 jour |
| **Total** | | | **10 jours** |

---

## 10. GRAPHIQUES (MPAndroidChart)

### Types de graphiques par rapport

| Rapport | Type | Données |
|---------|------|---------|
| Tableau Pharmacien | BarChart | Ventes vs Achats |
| Tableau Pharmacien | HorizontalBarChart | Top fournisseurs |
| Balance Caisse | PieChart | Répartition paiements |
| Rapport TVA | PieChart | Répartition par taux |
| Rapport d'Activité | PieChart | Recettes par mode |

### Dépendance à ajouter

```kotlin
// build.gradle.kts
implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
```

---

## 11. FONCTIONNALITES TRANSVERSES

### 11.1 Sélecteur de période

- Aujourd'hui
- Hier
- Cette semaine
- Ce mois
- Mois dernier
- Personnalisé (DateRangePicker)

### 11.2 Export PDF

- Téléchargement local
- Partage via Intent (email, WhatsApp, etc.)
- Aperçu dans WebView

### 11.3 Skeleton Loading

- Réutiliser le pattern existant avec Shimmer
- Skeleton spécifique pour chaque rapport

### 11.4 Pull-to-Refresh

- SwipeRefreshLayout sur tous les rapports

### 11.5 Offline

- Cache des dernières données consultées
- Indication "Données du XX/XX/XXXX"
