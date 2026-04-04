# Analyse Comparative — Système de Navigation Pharma-Smart
> **Révision 2 — 2026-04-03**
> Prise en compte des retours utilisateur : dé-priorisation accessibilité, implémentation navigation par rôle, breadcrumb dynamique, regroupement référentiel.

---

## Sommaire

1. [État d'avancement — ce qui est déjà fait](#1-état-davancement--ce-qui-est-déjà-fait)
2. [Révision des priorités](#2-révision-des-priorités)
3. [Navigation par rôle — proposition détaillée](#3-navigation-par-rôle--proposition-détaillée)
4. [Plan d'action révisé](#4-plan-daction-révisé)
5. [Utilisation du BreadcrumbService dans les pages à onglets](#5-utilisation-du-breadcrumbservice-dans-les-pages-à-onglets)

---

## 1. État d'avancement — ce qui est déjà fait

| Tâche | Statut | Notes |
|---|---|---|
| LayoutService → Signals | ✅ Fait | `layoutMode`, `sidebarCollapsed` en signals |
| `@HostListener` → `fromEvent` | ✅ Fait | `main.component.ts` et `sidebar.component.ts` |
| `@HostBinding` → `host: {}` | ✅ Fait | Décorateur `@Component` |
| Alt+V raccourci Nouvelle Vente | ✅ Fait | Route corrigée vers `/sales-home` |
| Overlay mobile sidebar | ✅ Fait | `.sidebar-backdrop` + signal `isMobile` |
| Auto-switch responsive (< 768px) | ✅ Fait | Dans `loadEffectiveLayoutMode()` |
| ARIA navbar / sidebar | ✅ Fait | `aria-label`, `aria-expanded`, `role` |
| Tooltip accessible (`pTooltip`) | ✅ Fait | Sidebar collapsed |
| Breadcrumb composant de base | ✅ Fait | `BreadcrumbComponent` avec route data |
| Dashboards par rôle (composants) | ✅ Fait | `CaissierDashboard`, `VendeurDashboard`, `ResponsableCommandeDashboard` |
| HomeComponent routing par rôle | ✅ Fait | `isAdmin()`, `isCaissier()`, `isVendeur()`, `isResponsableCommande()` |
| **AX8 — Regroupement Référentiel** | ✅ **Fait** | 3 groupes : Produits / Commercial / Organisation |
| **BreadcrumbService (tabs dynamiques)** | ✅ **Fait** | Signal `tabCrumb`, `setTabCrumb()`, `clearTabCrumb()` |
| **"Mon Tableau de Bord" dans nav** | ✅ **Fait** | Visible pour Caissier, Vendeur, Responsable, Pharmacien |
| **`ROLE_PHARMACIEN`** | ✅ **Fait** | Ajouté dans `authority.constants.ts` |

---

## 2. Révision des priorités

### Ce qui est dé-priorisé (backlog optionnel)

L'application est un logiciel de gestion interne utilisé par des professionnels de santé. L'accessibilité pour les non-voyants n'est pas une exigence opérationnelle.

| Tâche initiale | Nouvelle priorité | Raison |
|---|---|---|
| Skip link WCAG 2.4.1 | 🔵 Backlog | Déjà présent dans le HTML, non bloquant |
| ARIA complet navbar | 🔵 Backlog | Déjà partiellement fait, non bloquant |
| Navigation clavier sidebar complète | 🔵 Backlog | Non requis dans le contexte métier |
| Contraste couleurs WCAG | 🔵 Backlog | Application pro, non soumise au RGAA |

### Ce qui reste à faire (priorités révisées)

| Sprint | Tâche | Priorité |
|---|---|---|
| 1 | Activation des dashboards dans les routes (connexion router) | **P0** |
| 1 | Intégrer `BreadcrumbService.setTabCrumb()` dans les pages à onglets | **P0** |
| 2 | Ajout `data.breadcrumb` sur les routes manquantes | **P1** |
| 2 | Homepage Pharmacien (vue admin dashboard pour `ROLE_PHARMACIEN`) | **P1** |
| 3 | Recherche globale dans la navigation | **P2** |
| 3 | Système de favoris / pages récentes | **P2** |

---

## 3. Navigation par rôle — proposition détaillée

### Matrice des accès

| Section | Admin | Pharmacien | Caissier | Vendeur | Resp. Commande |
|---|---|---|---|---|---|
| Tableau de Bord (`/`) | ✅ Stats | ✅ Stats | ✅ Dashboard Caissier | ✅ Dashboard Vendeur | ➡️ → `/commande` |
| **Accès direct** | Nouvelle Vente `/sales-home` | Nouvelle Vente `/sales-home` | **Nouvelle Vente** `/sales-home` | **Nouvelle Prévente** `/sales-home/prevente` | **Tableau de Bord Appro** `/commande` |
| Gestion Courante (Ventes, Caisse) | ✅ | ✅ | ✅ | ✅ | ❌ |
| Gestion Stock | ✅ | ✅ | ❌ | ❌ | ✅ |
| Facturation | ✅ | ✅ | ❌ | ❌ | ❌ |
| Référentiel | ✅ | ✅ | ❌ | ❌ | ❌ |
| Rapports — CA | ✅ | ✅ | ❌ | ❌ | ✅ |
| Rapports — Stock & Inventaire | ✅ | ✅ | ❌ | ❌ | ✅ |
| Rapports — Clients & Fournisseurs | ✅ | ✅ | ❌ | ❌ | ✅ |
| Rapports — Trésorerie & Finance | ✅ | ✅ | ✅ | ❌ | ❌ |
| Administration | ✅ | ❌ | ❌ | ❌ | ❌ |
| Mon Compte | ✅ | ✅ | ✅ | ✅ | ✅ |

> **Justification Vendeur → `/sales-home/prevente` :** les vendeurs ne font que des préventes (sans encaissement). La route `/sales-home` (comptant) leur est inopportune.
>
> **Justification Resp. Commande → `/commande` :** `CommandeHomeComponent` est bien plus riche que le dashboard générique (`/`). Il intègre : Dashboard appro, Suggestions, Retours fournisseur, Répartition stock, SEMOIS.
>
> **Justification rapports Resp. Commande :** CA (impact achats), Stock (cœur de métier), Clients/Fournisseurs (performance fournisseurs). Trésorerie réservée au Caissier (encaissements).

### Roles Angular ↔ Authority

| Profil | Constante `Authority` | Dashboard cible |
|---|---|---|
| Administrateur | `ROLE_ADMIN` | `HomeComponent` → stats journalier/hebdo/mensuel/semestriel/annuel |
| Pharmacien/Titulaire | `ROLE_PHARMACIEN` ou `HOME_DASHBOARD` | Même vue que Admin |
| Caissier | `ROLE_CAISSIER` | `CaissierDashboardComponent` |
| Vendeur | `ROLE_VENDEUR` | `VendeurDashboardComponent` |
| Responsable Commande | `ROLE_RESPONSABLE_COMMANDE` | `ResponsableCommandeDashboardComponent` |

### Fonctionnement actuel du filtrage

Le `NavigationService.filterByAuthority()` filtre automatiquement les items selon les `authorities` déclarées sur chaque item de menu. Aucun duplicata de code n'est nécessaire — un seul menu est construit, les items non autorisés sont supprimés à l'affichage.

```
buildNavItems()
  │
  ├─ Nouvelle Vente (Caissier/Admin/SALES) → /sales-home
  ├─ Nouvelle Prévente (Vendeur seul)       → /sales-home/prevente
  ├─ Tableau de Bord Appro (Resp. Commande) → /commande  [CommandeHomeComponent]
  ├─ Mon Tableau de Bord (Caissier/Vendeur/Pharmacien) → /
  ├─ Gestion Courante → [GESTION_COURANT, ADMIN, ROLE_CAISSIER, ROLE_VENDEUR, SALES]
  ├─ Gestion Stock    → [ROLE_RESPONSABLE_COMMANDE, GESTION_STOCK, ADMIN, COMMANDE]
  ├─ Facturation      → [ADMIN, GESTION_FACTURATION]
  ├─ Référentiel      → [REFERENTIEL, ADMIN, ROLE_PHARMACIEN]
  ├─ Rapports
  │    ├─ CA                  → [ADMIN, ROLE_RESPONSABLE_COMMANDE]
  │    ├─ Stock & Inventaire  → [ADMIN, ROLE_RESPONSABLE_COMMANDE]
  │    ├─ Clients/Fournisseurs→ [ADMIN, ROLE_RESPONSABLE_COMMANDE]
  │    └─ Trésorerie/Finance  → [ADMIN, ROLE_CAISSIER]
  ├─ Administration   → [ADMIN, MENU_ADMIN, USER_MANAGEMENT, MAGASIN]
  └─ Compte           → [tous]
       │
       └─ filterByAuthority(userAuthorities) → menu personnalisé par profil
```

---

## 4. Plan d'action révisé

### Sprint 1 — Connexion des dashboards et breadcrumb dynamique

#### T1 — Ajouter `data.breadcrumb` sur les routes critiques

Dans `app.routes.ts` et `entity.routes.ts`, enrichir les routes sans `pageTitle` :

```typescript
// app.routes.ts
{ path: '', loadComponent: () => import('./home/home.component'), title: 'home.title', data: { breadcrumb: 'Accueil' } },

// entity.routes.ts
{ path: 'sales-home',    data: { pageTitle: 'Gestion des ventes',   breadcrumb: 'Ventes' } },
{ path: 'my-cash-register', data: { breadcrumb: 'Ma Caisse' } },
{ path: 'reports/sales', data: { breadcrumb: "Chiffre d'Affaires" } },
```

#### T2 — Intégrer `BreadcrumbService` dans les pages à onglets

Les composants qui utilisent des onglets internes (`ngbNav`, tabs PrimeNG) doivent appeler `BreadcrumbService.setTabCrumb()` à chaque changement d'onglet actif et `clearTabCrumb()` au destroy :

```typescript
// Exemple dans sales-home.component.ts
import { BreadcrumbService } from 'app/shared/components/breadcrumb/breadcrumb.service';
import { DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export class SalesHomeComponent implements OnInit {
  private readonly breadcrumbService = inject(BreadcrumbService);
  private readonly destroyRef = inject(DestroyRef);

  // Labels des onglets — adapter selon les composants réels
  private readonly TAB_LABELS: Record<string, string> = {
    GESTION:      'Gestion des ventes',
    SUGGESTIONS:  'Suggestions réappro',
    RETOUR:       'Retours fournisseur',
  };

  onTabChange(tabId: string): void {
    this.breadcrumbService.setTabCrumb(this.TAB_LABELS[tabId] ?? tabId);
  }

  constructor() {
    inject(DestroyRef).onDestroy(() => this.breadcrumbService.clearTabCrumb());
  }
}
```

**Résultat dans le fil d'Ariane :**
```
Accueil › Ventes › Gestion des ventes
Accueil › Ventes › Suggestions réappro
```

#### T3 — Connecter `ROLE_PHARMACIEN` au dashboard Admin

Dans `home.component.ts`, étendre `isAdmin()` :

```typescript
protected isAdmin(): boolean {
  const u = this.account();
  if (!u) return false;
  return u.authorities.includes(Authority.ADMIN)
      || u.authorities.includes(Authority.HOME_DASHBOARD)
      || u.authorities.includes(Authority.ROLE_PHARMACIEN);
}
```

### Sprint 2 — Référentiel groupé (AX8) — CSS

Ajouter les styles pour les séparateurs et labels de groupe dans `sidebar.component.scss` :

```scss
// Séparateur entre groupes
.submenu-divider {
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  margin: 0.4rem 0.75rem;
}

// En-tête de groupe non-cliquable
.submenu-group-label {
  padding: 0.5rem 0.75rem 0.25rem;
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: rgba(255, 255, 255, 0.45);
  pointer-events: none;
}
```

Et dans `navbar.component.scss` (dropdown-header Bootstrap est déjà stylisé nativement).

---

## 5. Utilisation du BreadcrumbService dans les pages à onglets

### API du service

```typescript
// Injecter
private readonly breadcrumbService = inject(BreadcrumbService);

// Définir l'onglet actif (appelé à chaque changement d'onglet)
this.breadcrumbService.setTabCrumb('Gestion des ventes');

// Avec URL optionnelle (pour rendre le crumb cliquable)
this.breadcrumbService.setTabCrumb('Suggestions', '/commande?tab=suggestions');

// Nettoyer à la destruction du composant
inject(DestroyRef).onDestroy(() => this.breadcrumbService.clearTabCrumb());
```

### Pages concernées

| Page | Onglets à gérer |
|---|---|
| `/sales-home` | Gestion des ventes, Ventes différées... |
| `/commande` | Commandes, Suggestions, Retours fournisseur, SEMOIS |
| `/facturation` | Factures, Règlements, Tiers-Payant |
| `/my-cash-register` | Caisse ouverte, Historique |
| `/gestion-peremption` | En cours, Traitées |

---

## Récapitulatif des fichiers modifiés (révision 2)

| Fichier | Modification |
|---|---|
| `layouts/navbar/navbar-item.model.ts` | Ajout `divider?`, `groupLabel?` |
| `shared/constants/authority.constants.ts` | Ajout `ROLE_PHARMACIEN` |
| `core/config/navigation.service.ts` | AX8 Référentiel groupé + "Mon Tableau de Bord" |
| `layouts/sidebar/sidebar.component.html` | Rendu `divider` et `groupLabel` |
| `layouts/navbar/navbar.component.html` | Rendu `divider` et `groupLabel` |
| `shared/components/breadcrumb/breadcrumb.service.ts` | **Nouveau** — `setTabCrumb`, `clearTabCrumb` |
| `shared/components/breadcrumb/breadcrumb.component.ts` | `tabCrumb` signal + fallback labels |

### Prochaines actions (non encore implémentées)

| Fichier | Action |
|---|---|
| `app.routes.ts` + `entity.routes.ts` | Ajouter `data.breadcrumb` sur les routes |
| `home/home.component.ts` | Étendre `isAdmin()` avec `ROLE_PHARMACIEN` |
| `layouts/sidebar/sidebar.component.scss` | CSS `.submenu-divider` et `.submenu-group-label` |
| `features/sales-home/sales-home.component.ts` | Appeler `BreadcrumbService.setTabCrumb()` |
| `features/commande/commande-home.component.ts` | Appeler `BreadcrumbService.setTabCrumb()` |
| `features/facturation/facturation.component.ts` | Appeler `BreadcrumbService.setTabCrumb()` |
