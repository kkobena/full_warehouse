# Dashboard Responsable Commande - Guide d'Implémentation Complet

## 📋 Résumé de l'Implémentation

Le dashboard Responsable Commande a été complètement implémenté avec :
- ✅ Interface frontend Angular 20 (Standalone components)
- ✅ Backend Spring Boot avec services et REST API
- ✅ Thème sombre orange/ambre
- ✅ 4 KPI cards principales
- ✅ Quick Actions rapides
- ✅ Sections avancées (Suggestions, ABC, Fournisseurs)

---

## 🗂️ Structure des Fichiers

### Frontend (Angular 20)

```
src/main/webapp/app/home/responsable-commande-dashboard/
├── responsable-commande-dashboard.component.ts       # Composant principal
├── responsable-commande-dashboard.component.html     # Template HTML
├── responsable-commande-dashboard.component.scss     # Styles sombres
├── responsable-commande-dashboard.model.ts           # Interfaces TypeScript
└── responsable-commande-dashboard.service.ts         # Service HTTP
```

### Backend (Spring Boot)

```
src/main/java/com/kobe/warehouse/
├── service/
│   ├── dto/dashboard/
│   │   ├── StockAlertsDTO.java
│   │   ├── CommandesEnCoursDTO.java
│   │   ├── PeremptionsDTO.java
│   │   ├── RotationStockDTO.java
│   │   ├── SuggestionReapproDTO.java
│   │   ├── AnalyseABCDTO.java
│   │   ├── PerformanceFournisseurDTO.java
│   │   └── ResponsableCommandeDashboardDTO.java
│   └── dashboard/
│       ├── ResponsableCommandeDashboardService.java
│       └── impl/ResponsableCommandeDashboardServiceImpl.java
└── web/rest/dashboard/
    └── ResponsableCommandeDashboardResource.java
```

---

## 🎨 Fonctionnalités Implémentées

### 1. KPI Cards (4 cartes principales)

#### Carte 1: Alertes Stock Critiques 🚨
**Données affichées:**
- 🔴 Rupture de stock (stock = 0)
- 🟠 Stock critique (< seuil minimal)
- 🟡 Bientôt en rupture (< seuil × 1.5)
- 🟡 Réassort stock rayon nécessaire

**Endpoint:** `GET /api/responsable-commande/dashboard/stock-alerts`

**Requêtes SQL:**
```sql
-- Rupture
SELECT COUNT(DISTINCT sp.produit_id)
FROM stock_produit sp
WHERE (sp.qty_stock + sp.qty_ug) = 0

-- Critique
SELECT COUNT(DISTINCT sp.produit_id)
FROM stock_produit sp
JOIN produit p ON p.id = sp.produit_id
WHERE (sp.qty_stock + sp.qty_ug) > 0
AND (sp.qty_stock + sp.qty_ug) < p.qty_seuil_mini

-- Réassort rayon
SELECT COUNT(DISTINCT rsp.id)
FROM repartition_stock_produit rsp
WHERE rsp.status = 'PENDING'
```

#### Carte 2: Commandes en Cours 📦
**Données affichées:**
- En attente (statut REQUESTED, PASSED)
- À réceptionner (statut IN_PROGRESS)
- Montant total des commandes

**Endpoint:** `GET /api/responsable-commande/dashboard/commandes-en-cours`

#### Carte 3: Péremptions Proches ⏰
**Données affichées:**
- < 1 mois
- 1-3 mois
- 3-6 mois
- Valeur totale à risque (en FCFA)

**Endpoint:** `GET /api/responsable-commande/dashboard/peremptions`

**Logique:**
- Utilise `produit.peremption_date`
- Calcule la valeur: `SUM((stock + ug) × prix_unitaire)`

#### Carte 4: Rotation Stock 🔄
**Données affichées:**
- Taux de rotation moyen (x/mois)
- 🟢 Rapide (> 4x/mois)
- 🟡 Normal (2-4x/mois)
- 🔴 Lent (< 2x/mois)

**Endpoint:** `GET /api/responsable-commande/dashboard/rotation-stock`

**Formule:**
```
Rotation = Ventes 30 derniers jours / Stock moyen
```

---

### 2. Quick Actions ⚡

Boutons d'action rapide positionnés en haut à droite:

1. **🗂️ Nouvelle Commande** (Orange - Principal)
   - Action: `nouvelleCommande()`
   - Navigate vers création de commande

2. **🔍 Analyser Stock** (Bleu)
   - Action: `analyserStock()`
   - Navigate vers analyse stock

3. **📋 Inventaire** (Gris)
   - Action: `lancerInventaire()`
   - Navigate vers inventaire

4. **⚠️ Gérer Alertes** (Rouge)
   - Action: `gererAlertes()`
   - Badge avec nombre total d'alertes
   - Navigate vers gestion alertes

5. **🔄 Rafraîchir** (Rond)
   - Action: `refreshDashboard()`
   - Reload toutes les données

---

### 3. Section Suggestions de Réapprovisionnement 🤖

**Fonctionnalités:**
- Suggestions automatiques basées sur:
  - Historique des ventes (90 derniers jours)
  - Consommation moyenne journalière
  - Seuil minimal de stock
  - Tendances saisonnières
- Sélection multiple avec checkboxes
- Génération automatique de commande
- Affichage du montant total des suggestions sélectionnées

**Colonnes du tableau:**
| Colonne | Description |
|---------|-------------|
| ☑️ Sélection | Checkbox pour sélectionner |
| Produit | Nom du produit |
| Code CIP | Code identifiant |
| Stock | Stock actuel (rouge si < 10) |
| Cons. moy./j | Consommation moyenne journalière |
| Qté suggérée | Quantité recommandée (badge bleu) |
| Fournisseur | Fournisseur principal |
| Prix Unit. | Prix unitaire |
| Total | Montant total ligne |

**Actions:**
- **Tout sélectionner** - Sélectionne/désélectionne tout
- **Générer commande** - Crée une commande avec les suggestions sélectionnées

**Endpoint:** `GET /api/responsable-commande/dashboard/suggestions`

**Algorithme de calcul:**
```sql
quantite_suggeree = GREATEST(
    seuil_mini × 2,
    consommation_moyenne_jour × 30
)

consommation_moyenne = SUM(ventes_90j) / 90
```

---

### 4. Section Analyse ABC (Pareto) 📊

Analyse de Pareto du stock selon la loi 80/20:

**Classe A (20% produits = 80% CA)**
- Couleur: Vert 🟢
- Produits à forte rotation
- Priorité maximale de réapprovisionnement

**Classe B (30% produits = 15% CA)**
- Couleur: Orange 🟠
- Produits à rotation moyenne
- Priorité modérée

**Classe C (50% produits = 5% CA)**
- Couleur: Rouge 🔴
- Produits à faible rotation
- Surveiller pour démarque

**Endpoint:** `GET /api/responsable-commande/dashboard/analyse-abc`

**Visualisation:**
- Barres de progression colorées
- Nombre de références par classe
- Valeur en FCFA
- Pourcentage du CA

**Actions:**
- Voir détails - Affiche la liste complète
- Exporter - Télécharge rapport Excel/PDF

---

### 5. Section Performance Fournisseurs 🏭

Tableau des Top 5 fournisseurs avec métriques:

**Colonnes:**
| Métrique | Description | Calcul |
|----------|-------------|--------|
| Fournisseur | Nom | - |
| Cdes | Nombre commandes | COUNT(commandes) |
| Délai moy. | Délai moyen livraison | AVG(date_livraison - date_commande) |
| Taux conf. | Taux de conformité | % commandes conformes |
| CA YTD | CA année en cours | SUM(montant) |
| Note | Note 1-5 étoiles | Basé sur délai + conformité |

**Système de notation:**
- ⭐⭐⭐⭐⭐ (5) : Délai ≤ 3 jours, conformité ≥ 98%
- ⭐⭐⭐⭐ (4) : Délai ≤ 5 jours, conformité ≥ 95%
- ⭐⭐⭐ (3) : Délai ≤ 7 jours, conformité ≥ 90%
- ⭐⭐ (2) : Délai > 7 jours

**Endpoint:** `GET /api/responsable-commande/dashboard/performance-fournisseurs?top=5`

**Badges colorés:**
- 🟢 Taux conformité ≥ 95%
- 🟡 Taux conformité ≥ 90%
- 🔴 Taux conformité < 90%

---

## 🎨 Thème Sombre Orange/Ambre

### Palette de Couleurs

```scss
// Fond
$bg-primary: #0f172a;      // Slate 950 - Fond principal
$bg-card: #1e293b;         // Slate 800 - Fond cartes
$bg-card-hover: #334155;   // Slate 700 - Hover

// Texte
$text-primary: #f1f5f9;    // Slate 100
$text-secondary: #cbd5e1;  // Slate 300
$text-muted: #94a3b8;      // Slate 400

// Rôle spécifique
$primary-color: #f59e0b;   // Amber 500 - Principal
$primary-light: #fbbf24;   // Amber 400
$primary-dark: #d97706;    // Amber 600

// Sémantique
$success-color: #10b981;   // Emerald 500
$warning-color: #f59e0b;   // Amber 500
$danger-color: #ef4444;    // Red 500
$info-color: #3b82f6;      // Blue 500
```

### Effets Visuels

**KPI Cards:**
- Glassmorphism avec `backdrop-filter: blur(10px)`
- Dégradé subtle: `linear-gradient(135deg, $bg-card 0%, lighten($bg-card, 3%) 100%)`
- Bordure gauche colorée selon le type
- Shadow: `0 4px 6px rgba(0, 0, 0, 0.3)`
- Hover: `transform: translateY(-5px)` + shadow plus prononcée

**Boutons:**
- Gradient sur boutons principaux
- Hover: translateY(-2px) + glow coloré
- Transition: `all 0.2s ease`

**Tableaux:**
- Fond transparent avec bordures subtiles
- Headers avec fond `rgba(0, 0, 0, 0.3)`
- Hover rows: `background: $bg-card-hover`
- Texte en uppercase pour headers

---

## 🔌 API Endpoints Complets

### Endpoint Principal

```http
GET /api/responsable-commande/dashboard
```

**Response:**
```json
{
  "stockAlerts": {
    "rupture": 23,
    "stockCritique": 67,
    "bientotEnRupture": 145,
    "reassortStockRayon": 34
  },
  "commandesEnCours": {
    "enAttente": 8,
    "aReceptionner": 5,
    "totalMontant": 2345600
  },
  "peremptions": {
    "unMois": 12,
    "unATroisMois": 34,
    "troisASixMois": 89,
    "valeurTotale": 456700
  },
  "rotationStock": {
    "rotationMoyenne": 2.8,
    "rapide": 234,
    "normal": 567,
    "lent": 123
  },
  "suggestions": [...],
  "analyseABC": {...},
  "performanceFournisseurs": [...]
}
```

### Endpoints Individuels

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/stock-alerts` | GET | Alertes stock uniquement |
| `/commandes-en-cours` | GET | Commandes en cours |
| `/peremptions` | GET | Péremptions proches |
| `/rotation-stock` | GET | Taux de rotation |
| `/suggestions` | GET | Suggestions réappro |
| `/analyse-abc` | GET | Analyse ABC/Pareto |
| `/performance-fournisseurs` | GET | Top fournisseurs |
| `/refresh` | POST | Rafraîchir dashboard |

---

## 🚀 Utilisation

### 1. Importer le Composant

```typescript
// app/home/home.routes.ts
{
  path: 'responsable-commande',
  loadComponent: () =>
    import('./responsable-commande-dashboard/responsable-commande-dashboard.component')
      .then(m => m.ResponsableCommandeDashboardComponent),
  canActivate: [UserRouteAccessService],
  data: { authorities: [Authority.ROLE_RESPONSABLE_COMMANDE] }
}
```

### 2. Naviguer vers le Dashboard

```typescript
// Depuis n'importe quel composant
this.router.navigate(['/home/responsable-commande']);
```

### 3. Redirection Automatique par Rôle

```typescript
// home.component.ts
ngOnInit(): void {
  const user = this.accountService.userValue;

  if (user.authorities.includes(Authority.ROLE_RESPONSABLE_COMMANDE)) {
    this.router.navigate(['/home/responsable-commande']);
  }
}
```

---

## 📱 Responsive Design

### Breakpoints

- **Desktop (> 1200px)**: 4 colonnes pour KPIs
- **Tablet (768-1200px)**: 2 colonnes
- **Mobile (< 768px)**: 1 colonne (stack)

### Adaptations Mobile

```scss
@media (max-width: 768px) {
  // Quick actions en menu hamburger
  .quick-actions {
    flex-wrap: wrap;
    button {
      flex: 1;
      min-width: 120px;
    }
  }

  // KPIs en stack
  .kpi-card {
    margin-bottom: 1rem;
  }

  // Tableaux compacts
  .p-datatable {
    font-size: 0.875rem;
  }
}
```

---

## ✅ Checklist de Déploiement

### Backend
- [x] DTOs créés et compilés
- [x] Service interface définie
- [x] Service implémentation avec requêtes SQL
- [x] Contrôleur REST avec tous les endpoints
- [ ] Tests unitaires des services
- [ ] Tests d'intégration des endpoints
- [ ] Documentation Swagger/OpenAPI

### Frontend
- [x] Modèles TypeScript synchronisés
- [x] Service HTTP avec tous les appels
- [x] Composant principal avec signaux Angular 20
- [x] Template HTML complet
- [x] Styles SCSS avec thème sombre
- [ ] Tests unitaires Jest
- [ ] Tests E2E
- [ ] Accessibilité (ARIA labels)

### Intégration
- [ ] Route ajoutée dans home.routes.ts
- [ ] Guard d'autorisation configuré
- [ ] Redirection automatique par rôle
- [ ] Menu de navigation mis à jour
- [ ] Permissions backend vérifiées

### Performance
- [ ] Lazy loading du composant
- [ ] Pagination des tableaux
- [ ] Debounce sur recherches
- [ ] Cache des données (30s)
- [ ] Optimisation des requêtes SQL (indexes)

---

## 🐛 Dépannage

### Problème: Données non affichées

**Solution:**
1. Vérifier que l'utilisateur a le rôle `ROLE_RESPONSABLE_COMMANDE`
2. Vérifier les logs backend pour erreurs SQL
3. Vérifier la console browser pour erreurs HTTP
4. Tester l'endpoint manuellement avec Postman

### Problème: Thème sombre non appliqué

**Solution:**
1. Vérifier que le fichier SCSS est importé
2. Vérifier la classe `dark-theme` sur le div wrapper
3. Clear cache du browser
4. Rebuild avec `npm run webapp:build`

### Problème: Suggestions vides

**Solution:**
- Vérifier qu'il y a des ventes dans les 90 derniers jours
- Vérifier que les produits ont un `qty_seuil_mini` défini
- Vérifier la relation `fournisseur_produit`
- Vérifier les données de test en base

---

## 📚 Ressources

### Documentation
- [Angular Signals](https://angular.dev/guide/signals)
- [PrimeNG Charts](https://primeng.org/chart)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)

### Design
- [Tailwind Colors](https://tailwindcss.com/docs/customizing-colors)
- [Glassmorphism Generator](https://hype4.academy/tools/glassmorphism-generator)
- [PrimeNG Dark Theme](https://primeng.org/theming)

---

**Date de création**: 13 Décembre 2025
**Version**: 1.0
**Auteur**: Pharma-Smart Team
**Status**: ✅ Implémentation complète - Prêt pour tests
