# Spécification des Dashboards par Profil - Pharma-Smart

## Vue d'ensemble

Cette spécification définit les dashboards spécifiques pour chaque profil utilisateur avec une interface sombre, moderne et intuitive. Chaque dashboard est optimisé pour les tâches quotidiennes de chaque rôle.

---

## 1. Dashboard CAISSIER (ROLE_CAISSIER)

### 🎯 Objectif
Interface rapide et épurée pour les opérations de caisse quotidiennes avec focus sur les ventes et les encaissements.

### 🎨 Thème
- **Couleur principale**: Vert émeraude (#10B981) - symbolise les transactions
- **Fond**: Gris foncé (#1F2937) avec cartes sur fond (#111827)
- **Accents**: Vert clair (#34D399) pour les actions positives

### 📊 KPIs Principaux (4 cartes en haut)

#### Carte 1: Mes Ventes du Jour
```
┌─────────────────────────────────┐
│ 💰 MES VENTES DU JOUR          │
│                                 │
│     1,234,500 FCFA             │
│                                 │
│ 🔵 45 ventes  🟢 Objectif: 80% │
└─────────────────────────────────┘
```
- Montant total des ventes de l'utilisateur connecté
- Nombre de ventes effectuées
- Progression vers l'objectif journalier (barre de progression)

#### Carte 2: Caisse Ouverte
```
┌─────────────────────────────────┐
│ 💵 CAISSE OUVERTE              │
│                                 │
│     Ouverte à 08:00            │
│     Fond de caisse: 50,000     │
│                                 │
│ 🟢 État: ACTIVE  ⏱️ 05:23:12   │
└─────────────────────────────────┘
```
- Heure d'ouverture de caisse
- Fond de caisse initial
- Temps écoulé depuis l'ouverture
- État de la caisse (badge coloré)

#### Carte 3: Paiements Reçus
```
┌─────────────────────────────────┐
│ 💳 PAIEMENTS REÇUS             │
│                                 │
│ 💵 Espèces:    850,000         │
│ 💳 Carte:      250,000         │
│ 📱 Mobile:     134,500         │
│                                 │
│ Total:      1,234,500 FCFA     │
└─────────────────────────────────┘
```
- Répartition par mode de paiement
- Graphique en anneau (doughnut) en mode graphique

#### Carte 4: Dernière Vente
```
┌─────────────────────────────────┐
│ 🛒 DERNIÈRE VENTE              │
│                                 │
│     45,600 FCFA                │
│                                 │
│ Client: KOUASSI Jean           │
│ 🕐 Il y a 2 minutes            │
└─────────────────────────────────┘
```
- Montant de la dernière vente
- Nom du client (ou "Client anonyme")
- Temps écoulé depuis la vente

### ⚡ Quick Actions (Boutons d'action rapide)

Positionnés en haut à droite, gros boutons avec icônes:

```
┌──────────────────────────────────────────────────────────┐
│                                                          │
│  🛒 NOUVELLE    💰 FERMER      📊 MES            │
│     VENTE         CAISSE         STATS                  │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

1. **🛒 NOUVELLE VENTE** (Vert, très proéminent)
   - Ouvre l'interface de vente
   - Raccourci: Ctrl+N

2. **💰 FERMER CAISSE** (Orange/Rouge)
   - Lance la procédure de clôture de caisse
   - Avec confirmation obligatoire

3. **📊 MES STATS** (Bleu)
   - Statistiques personnelles du caissier
   - Historique des ventes



### 📈 Sections Additionnelles

#### Section 1: Ventes en Attente
```
┌─────────────────────────────────────────────────────────┐
│ 📋 VENTES EN ATTENTE (3)                               │
│                                                         │
│ ┌─────────────────┬──────────┬─────────────┬─────────┐│
│ │ Client          │ Montant  │ Articles    │ Action  ││
│ ├─────────────────┼──────────┼─────────────┼─────────┤│
│ │ YAO Marie       │ 12,500   │ 3 articles  │ [Reprendre]││
│ │ KONAN Pierre    │ 45,000   │ 8 articles  │ [Reprendre]││
│ │ Anonyme         │ 8,900    │ 2 articles  │ [Reprendre]││
│ └─────────────────┴──────────┴─────────────┴─────────┘│
└─────────────────────────────────────────────────────────┘
```

#### Section 2: Alertes du Jour
```
┌─────────────────────────────────────────────────────────┐
│ ⚠️ ALERTES                                              │
│                                                         │
│ 🟢 Objectif journalier atteint à 14:30                │
└─────────────────────────────────────────────────────────┘
```

#### Section 3: Mes 5 Dernières Ventes (Mini-tableau)
```
┌─────────────────────────────────────────────────────────┐
│ 🕐 HISTORIQUE RÉCENT                                   │
│                                                         │
│ Heure   │ Client          │ Montant   │ Mode Paiem.   │
│ ────────┼─────────────────┼───────────┼───────────────│
│ 13:45   │ KONE Aya        │ 23,400    │ Espèces       │
│ 13:38   │ BAMBA Sekou     │ 56,700    │ Carte         │
│ 13:25   │ Anonyme         │ 12,000    │ Mobile Money  │
│ 13:10   │ DIABATE Emma    │ 89,500    │ Espèces       │
│ 12:58   │ OUATTARA Marc   │ 15,600    │ Carte         │
└─────────────────────────────────────────────────────────┘
```

---

## 2. Dashboard VENDEUR (ROLE_VENDEUR)

### 🎯 Objectif
Focus sur la performance commerciale, les clients et le conseil pharmaceutique.

### 🎨 Thème
- **Couleur principale**: Bleu océan (#3B82F6) - symbolise la confiance
- **Fond**: Gris anthracite (#1E293B) avec cartes sur fond (#0F172A)
- **Accents**: Cyan (#06B6D4) pour les actions

### 📊 KPIs Principaux (4 cartes en haut)

#### Carte 1: Mes Performances
```
┌─────────────────────────────────┐
│ 🎯 MES PERFORMANCES            │
│                                 │
│  CA du jour: 2,345,600 FCFA    │
│                                 │
│ ██████████████░░░░░░ 70%       │
│ Objectif: 3,000,000 FCFA       │
│                                 │
│ 🏆 Rang: 2/8 vendeurs          │
└─────────────────────────────────┘
```
- Chiffre d'affaires personnel
- Barre de progression vers objectif
- Classement par rapport aux autres vendeurs
- Badge de performance (Bronze/Argent/Or)

#### Carte 2: Mes Clients du Jour
```
┌─────────────────────────────────┐
│ 👥 MES CLIENTS                 │
│                                 │
│     28 clients servis          │
│                                 │
│ 🆕 5 nouveaux                  │
│ 🔁 23 fidèles                  │
│                                 │
│ 💳 Taux fidélité: 82%          │
└─────────────────────────────────┘
```
- Nombre total de clients servis
- Nouveaux vs fidèles
- Taux de fidélisation
- Panier moyen

#### Carte 3: Ventes par Type
```
┌─────────────────────────────────┐
│ 🏥 TYPES DE VENTES             │
│                                 │
│ 💊 Ordonnance:   1,234,500     │
│ 🛍️  Conseil:       890,100     │
│ 🔬 Parapharma:     221,000     │
│                                 │
│ Graphique: [===] [==] [=]      │
└─────────────────────────────────┘
```
- Ventes sur ordonnance
- Ventes conseil
- Parapharmacie
- Graphique en barres horizontales



### ⚡ Quick Actions

```
┌──────────────────────────────────────────────────────────────────────┐
│                                                                      │
│  🛒 VENDRE    👤 NOUVEAU      🔍 PRODUIT    📋 MES        📊 PERFS  │
│               CLIENT           INFO          STATS                  │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

1. **🛒 NOUVELLE VENTE** (Bleu, principal)
   - Créer une vente

2. **👤 NOUVEAU CLIENT** (Vert)
   - Enregistrer un nouveau client
   - Formulaire rapide

3. **🔍 INFO PRODUIT** (Cyan)
   - Recherche rapide de produit
   - Stock, prix, interactions

4. **📋 MES STATS** (Violet)
   - Statistiques détaillées personnelles
   - Historique des ventes

5. **📊 PERFORMANCES** (Orange)
   - Dashboard de performance
   - Objectifs et classements

### 📈 Sections Additionnelles

#### Section 1: Top 10 Produits Vendus par Moi
```
┌──────────────────────────────────────────────────────────┐
│ 🏆 MES TOP PRODUITS DU MOIS                             │
│                                                          │
│ Produit                    │ Qté  │ CA      │ Marge    │
│ ──────────────────────────┼──────┼─────────┼──────────│
│ 1. DOLIPRANE 1000mg       │ 245  │ 122,500 │ 18,375  │
│ 2. PARACÉTAMOL 500mg      │ 189  │  94,500 │ 14,175  │
│ 3. AMOXICILLINE 500mg     │ 156  │ 234,000 │ 35,100  │
│ ...                       │      │         │          │
└──────────────────────────────────────────────────────────┘
```


#### Section 2: Opportunités de Vente
```
┌──────────────────────────────────────────────────────────┐
│ 💡 OPPORTUNITÉS                                          │
│                                                          │
│ 🎯 Clients avec ordonnances répétitives (12)           │
│    → Proposer abonnement mensuel                        │
│                                                          │
│ 🎯 Produits complémentaires non vendus (8 clients)     │
│    → Doliprane vendu sans vitamine C                    │
│                                                          │
│ 🎯 Clients à fort potentiel (5)                        │
│    → CA moyen > 50,000 FCFA/mois                        │
└──────────────────────────────────────────────────────────┘
```

#### Section 3: Mes Objectifs
```
┌──────────────────────────────────────────────────────────┐
│ 🎯 OBJECTIFS DU MOIS                                    │
│                                                          │
│ CA Mensuel:      ████████████░░░░ 60% (12/20 jours)    │
│ Target: 15,000,000 FCFA                                 │
│                                                          │
│ Nouveaux clients: ██████████████░░ 70% (14/20)         │
│ Target: 20 nouveaux clients                             │
│                                                          │
│ Taux satisfaction: ████████████████ 95% ⭐             │
│ Target: 90%                                             │
└──────────────────────────────────────────────────────────┘
```

---

## 3. Dashboard RESPONSABLE COMMANDE (ROLE_RESPONSABLE_COMMANDE)

### 🎯 Objectif
Gestion des stocks, commandes fournisseurs,Réassort du stock,répartition entre le stock rayon et la reserve, réapprovisionnement et inventaires.

### 🎨 Thème
- **Couleur principale**: Orange/Ambre (#F59E0B) - symbolise l'approvisionnement
- **Fond**: Gris ardoise foncé (#0F172A) avec cartes sur fond (#020617)
- **Accents**: Jaune doré (#FBBF24) pour les alertes

### 📊 KPIs Principaux (4 cartes en haut)

#### Carte 1: Alertes Stock Critiques
```
┌─────────────────────────────────┐
│ 🚨 ALERTES STOCK               │
│                                 │
│  🔴 Rupture:        23         │
│  🟠 Stock critique: 67         │
│  🟡 Bientôt en rupture: 145    │
│  🟡 Réassort stock rayon: 145    │
│                                 │
│ ⚡ Action requise immédiate    │
└─────────────────────────────────┘
```
- Produits en rupture de stock
- Produits sous seuil minimal
- Produits approchant du seuil
- Bouton  pour les suggestions de réassort
- Bouton d'action directe

#### Carte 2: Commandes en Cours
```
┌─────────────────────────────────┐
│ 📦 COMMANDES EN COURS          │
│                                 │
│  📝 En attente:      8         │    │
│  📥 À réceptionner:  5         │
│                                 │
│ Total: 2,345,600 FCFA          │
└─────────────────────────────────┘
```
- Commandes par statut
- Valeur totale des commandes
- Graphique d'évolution

#### Carte 3: Péremptions Proches
```
┌─────────────────────────────────┐
│ ⏰ PÉREMPTIONS                 │
│                                 │
│  🔴 < 1 mois:       12 produits│
│  🟠 1-3 mois:       34 produits│
│  🟡 3-6 mois:       89 produits│
│                                 │
│ Valeur: 456,700 FCFA           │
└─────────────────────────────────┘
```
- Produits périmés ou proches péremption
- Classification par urgence
- Valeur estimée à risque

#### Carte 4: Taux de Rotation Stock
```
┌─────────────────────────────────┐
│ 🔄 ROTATION STOCK              │
│                                 │
│  Rotation moyenne: 2.8x/mois   │
│                                 │
│ 🟢 Rapide (>4x):   234 produits│
│ 🟡 Normal (2-4x):  567 produits│
│ 🔴 Lent (<2x):     123 produits│
└─────────────────────────────────┘
```
- Taux de rotation moyen
- Classification des produits
- Indicateurs de performance

### ⚡ Quick Actions

```
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│  📝 NOUVELLE    🔍 ANALYSER   📊 INVENTAIRE   ⚠️ ALERTES   📈 RAPPORT │
│     COMMANDE      STOCK                                                │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

1. **📝 NOUVELLE COMMANDE** (Orange, principal)
   - Créer une commande fournisseur
   - Suggestions automatiques basées sur stock

2. **🔍 ANALYSER STOCK** (Bleu)
   - Vue détaillée du stock
   - Filtres avancés, recherche

3. **📊 INVENTAIRE** (Violet)
   - Lancer un inventaire
   - Consulter inventaires en cours

4. **⚠️ GÉRER ALERTES** (Rouge)
   - Traiter les alertes stock
   - Ruptures et péremptions

5. **📈 RAPPORTS** (Vert)
   - Rapports de stock
   - Analyses ABC, Pareto

### 📈 Sections Additionnelles

#### Section 1: Suggestions de Commande Automatique
```
┌──────────────────────────────────────────────────────────────────┐
│ 🤖 SUGGESTIONS INTELLIGENTES DE RÉAPPROVISIONNEMENT            │
│                                                                  │
│ Basé sur: Historique 3 mois, Tendances, Saisonnalité           │
│                                                                  │
│ Produit              │ Stock │ Cons.moy │ Suggéré │ Fournisseur│
│ ─────────────────────┼───────┼──────────┼─────────┼────────────│
│ DOLIPRANE 1000mg    │   12  │  45/j    │   500   │ SANOFI    │
│ AMOXICILLINE 500mg  │    8  │  28/j    │   300   │ PFIZER    │
│ PARACÉTAMOL 500mg   │   25  │  67/j    │   800   │ CIPLA     │
│                                                                  │
│ [✓ Tout sélectionner] [Générer commande] [Modifier quantités] │
└──────────────────────────────────────────────────────────────────┘
```



#### Section 2: Analyse ABC Stock (Pareto)
```
┌──────────────────────────────────────────────────────────────────┐
│ 📊 ANALYSE ABC DU STOCK (Loi de Pareto)                        │
│                                                                  │
│ Classe A (20% produits = 80% CA): 234 références               │
│ ██████████████████████████████░░ Valeur: 12,345,600 FCFA      │
│                                                                  │
│ Classe B (30% produits = 15% CA): 351 références               │
│ ████████░░░░░░░░░░░░░░░░░░░░░░░░ Valeur: 2,314,800 FCFA      │
│                                                                  │
│ Classe C (50% produits = 5% CA): 585 références                │
│ ███░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ Valeur: 771,600 FCFA        │
│                                                                  │
│ [Voir détails] [Exporter rapport]                              │
└──────────────────────────────────────────────────────────────────┘
```

#### Section 3: Tableau de Bord Fournisseurs
```
┌──────────────────────────────────────────────────────────────────┐
│ 🏭 PERFORMANCE FOURNISSEURS (Top 5)                            │
│                                                                  │
│ Fournisseur │ Cdes  │ Délai moy │ Taux conf. │ CA YTD    │ Note│
│ ────────────┼───────┼───────────┼────────────┼───────────┼─────│
│ SANOFI      │  145  │  3.2 jours│    98%     │ 23,456,700│ ⭐⭐⭐⭐⭐│
│ PFIZER      │  123  │  4.1 jours│    95%     │ 18,234,500│ ⭐⭐⭐⭐ │
│ LABOREX     │   98  │  5.5 jours│    92%     │ 15,678,900│ ⭐⭐⭐⭐ │
│ CIPLA       │   87  │  6.2 jours│    88%     │ 12,345,600│ ⭐⭐⭐  │
│ DENK PHARMA │   76  │  4.8 jours│    94%     │ 10,234,500│ ⭐⭐⭐⭐ │
└──────────────────────────────────────────────────────────────────┘
```

#### Section 4: Alertes et Notifications
```
┌──────────────────────────────────────────────────────────────────┐
│ 🔔 NOTIFICATIONS & ALERTES                                      │
│                                                                  │
│ 🔴 URGENT: 23 produits en rupture nécessitent commande immédiate│
│    → [Générer bon de commande]                                  │
│                                                                  │
│ 🟠 ATTENTION: 12 produits périment dans moins de 30 jours      │
│    → [Voir liste détaillée] [Planifier démarque]               │
│                                                                  │
│ 🟡 INFO: Commande CMD-2340 (SANOFI) en retard de 2 jours       │
│    → [Contacter fournisseur] [Voir détails]                    │
│                                                                  │
│ 🟢 OK: Inventaire du 01/12 validé - Écart: 0.3%                │
│    → [Voir rapport]                                             │
└──────────────────────────────────────────────────────────────────┘
```

---

## 🎨 Palette de Couleurs Sombres Commune

### Fond de Base
- **Background principal**: `#0F172A` (Slate 950)
- **Background cards**: `#1E293B` (Slate 800)
- **Background hover**: `#334155` (Slate 700)

### Textes
- **Texte principal**: `#F1F5F9` (Slate 100)
- **Texte secondaire**: `#CBD5E1` (Slate 300)
- **Texte muted**: `#94A3B8` (Slate 400)

### Couleurs Sémantiques (Mode Sombre)
- **Success**: `#10B981` (Emerald 500)
- **Warning**: `#F59E0B` (Amber 500)
- **Danger**: `#EF4444` (Red 500)
- **Info**: `#3B82F6` (Blue 500)
- **Primary (Caissier)**: `#10B981` (Emerald 500)
- **Primary (Vendeur)**: `#3B82F6` (Blue 500)
- **Primary (Responsable)**: `#F59E0B` (Amber 500)

### Bordures et Séparateurs
- **Border**: `#334155` (Slate 700)
- **Border subtle**: `#1E293B` (Slate 800)

---

## 🏗️ Structure HTML Recommandée

### Layout Global
```html
<div class="dark-theme-wrapper" [ngClass]="'theme-' + currentRole">
  <!-- Header avec Quick Actions -->
  <div class="quick-actions-bar">
    <!-- Boutons d'action rapide -->
  </div>

  <!-- KPI Cards Row -->
  <!-- utilise angular 20+ flow -->
  <div class="row kpi-row">
    <div class="col-xl-3 col-md-6 mb-3" *ngFor="let kpi of kpiCards">
      <app-kpi-card [data]="kpi" [theme]="currentRole"></app-kpi-card>
    </div>
  </div>

  <!-- Content Sections -->
  <div class="dashboard-content">
    <!-- Sections spécifiques au rôle -->
  </div>
</div>
```

### Composant KPI Card
```html
<div class="kpi-card" [ngClass]="'kpi-' + theme">
  <div class="kpi-header">
    <div class="kpi-icon">
      <i [class]="iconClass"></i>
    </div>
    <div class="kpi-content">
      <p class="kpi-label">{{ label }}</p>
      <h3 class="kpi-value">{{ value | number }}</h3>
      <div class="kpi-badges">
        <!-- Badges informatifs -->
      </div>
    </div>
  </div>
  <div class="kpi-footer" *ngIf="footerText">
    <span>{{ footerText }}</span>
    <span class="badge">{{ footerValue }}</span>
  </div>
</div>
```

---

## 📱 Responsive Design

### Breakpoints
- **Mobile (< 768px)**: 1 colonne, KPIs en stack
- **Tablet (768px - 1024px)**: 2 colonnes
- **Desktop (> 1024px)**: 4 colonnes pour KPIs, 2-3 pour sections

### Adaptations Mobile
- Quick Actions transformés en menu hamburger
- KPIs réduits (icône + valeur principale uniquement)
- Tableaux convertis en cartes empilées
- Graphiques simplifiés

---

## 🔐 Logique de Routage et Permissions

### Routes Spécifiques
```typescript
// app/home/home.routes.ts
export const homeRoutes: Routes = [
  {
    path: '',
    component: HomeComponent,
    children: [
      {
        path: 'caissier',
        loadComponent: () => import('./caissier-dashboard/caissier-dashboard.component'),
        canActivate: [UserRouteAccessService],
        data: { authorities: [Authority.ROLE_CAISSIER] }
      },
      {
        path: 'vendeur',
        loadComponent: () => import('./vendeur-dashboard/vendeur-dashboard.component'),
        canActivate: [UserRouteAccessService],
        data: { authorities: [Authority.ROLE_VENDEUR] }
      },
      {
        path: 'responsable-commande',
        loadComponent: () => import('./responsable-commande-dashboard/responsable-commande-dashboard.component'),
        canActivate: [UserRouteAccessService],
        data: { authorities: [Authority.ROLE_RESPONSABLE_COMMANDE] }
      }
    ]
  }
];
```

### Redirection Automatique
```typescript
// home.component.ts
ngOnInit(): void {
  const user = this.accountService.userValue;

  if (user.authorities.includes(Authority.ROLE_CAISSIER)) {
    this.router.navigate(['/home/caissier']);
  } else if (user.authorities.includes(Authority.ROLE_VENDEUR)) {
    this.router.navigate(['/home/vendeur']);
  } else if (user.authorities.includes(Authority.ROLE_RESPONSABLE_COMMANDE)) {
    this.router.navigate(['/home/responsable-commande']);
  } else {
    this.router.navigate(['/home/admin']); // Dashboard par défaut
  }
}
```

---

## 🎯 Métriques de Performance à Tracker

### Temps de Chargement
- **Objectif**: < 1 seconde pour afficher les KPIs
- **Stratégie**: Lazy loading des graphiques et sections lourdes

### Rafraîchissement en Temps Réel
- **Fréquence**: Toutes les 30 secondes pour données critiques
- **WebSocket** pour notifications instantanées (ventes, alertes)

### Interactions Utilisateur
- **Quick Actions**: 1 clic maximum pour actions principales
- **Recherche**: Résultats instantanés (< 300ms)

---

## 🚀 Plan d'Implémentation Suggéré

### Phase 1: Structure de Base (Semaine 1)
1. Créer les composants de dashboard pour chaque rôle
2. Implémenter le système de thème sombre
3. Développer les composants KPI réutilisables

### Phase 2: Quick Actions (Semaine 2)
4. Implémenter les boutons d'action rapide
5. Connecter aux services backend existants
6. Ajouter les raccourcis clavier

### Phase 3: Sections Spécifiques (Semaine 3-4)
7. Développer les sections additionnelles par rôle
8. Intégrer les graphiques Chart.js/PrimeNG
9. Implémenter les tableaux avec filtres

### Phase 4: Optimisation (Semaine 5)
10. Ajouter le rafraîchissement temps réel
11. Optimiser les performances (lazy loading)
12. Tests utilisateurs et ajustements

---

## 📝 Notes Techniques

### Services Backend Requis

#### Pour CAISSIER
- `GET /api/cash-register/my-sales-today` - Ventes du caissier
- `GET /api/cash-register/current-session` - Session de caisse
- `GET /api/cash-register/pending-sales` - Ventes en attente

#### Pour VENDEUR
- `GET /api/sales/my-performance` - Performances personnelles
- `GET /api/sales/my-clients-today` - Clients servis
- `GET /api/sales/my-commission` - Commission calculée
- `GET /api/sales/my-top-products` - Top produits vendus

#### Pour RESPONSABLE COMMANDE
- `GET /api/stock/alerts` - Alertes stock
- `GET /api/commande/in-progress` - Commandes en cours
- `GET /api/stock/expirations` - Produits proches péremption
- `GET /api/stock/rotation-rate` - Taux de rotation
- `POST /api/commande/auto-suggest` - Suggestions commande auto

### Composants Réutilisables à Créer
1. `KpiCardComponent` - Carte KPI générique
2. `QuickActionButtonComponent` - Bouton action rapide
3. `AlertBannerComponent` - Bannière d'alerte
4. `MiniTableComponent` - Petit tableau avec actions
5. `ProgressBarComponent` - Barre de progression avec label
6. `StatBadgeComponent` - Badge statistique

---

## ✅ Checklist de Validation

- [ ] Thème sombre cohérent et agréable
- [ ] Tous les KPIs affichent les vraies données
- [ ] Quick Actions fonctionnelles et rapides (< 500ms)
- [ ] Responsive sur mobile/tablet/desktop
- [ ] Rafraîchissement automatique des données
- [ ] Graphiques interactifs et informatifs
- [ ] Alertes visuelles claires (couleurs, icônes)
- [ ] Accessibilité (contraste, focus, ARIA labels)
- [ ] Performance (< 1s chargement initial)
- [ ] Tests utilisateurs avec chaque profil

---

## 📚 Références

- **Design inspiration**: [Tailwind UI Dark](https://tailwindui.com/components/dark)
- **PrimeNG Dark Theme**: [PrimeNG Lara Dark](https://primeng.org/theming#builtinthemes)
- **Chart.js Dark Mode**: [Chart.js Dark Mode](https://www.chartjs.org/docs/latest/configuration/responsive.html)

---

**Date de création**: 13 Décembre 2025
**Version**: 1.0
**Auteur**: Pharma-Smart Team
