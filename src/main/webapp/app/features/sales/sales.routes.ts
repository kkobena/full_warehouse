import { Routes } from '@angular/router';
import { AuthGuard } from '../../core/auth/auth.guard';

/**
 * Sales feature routes
 *
 * Routes:
 * - /sales-home              → Nouvelle vente (tab COMPTANT par defaut)
 * - /sales-home/edit/:id     → Editer une vente existante
 *   Query params optionnels:
 *     ?saleDate=2024-01-15   → Date de la vente (requis pour l'API)
 *     ?presale=true          → Mode prevente
 * - /sales-home/prevente     → Mode prevente (pas d'encaissement)
 * - /sales-home/prevente/edit/:id → Editer une prevente existante
 *
 * Accès contrôlé par NavItemRole (ABAC) pour les routes business.
 * Les routes sans abilitySubject sont auth-only (tout utilisateur connecté).
 */
export const SALES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./feature/sales-home/sales-home.component').then(m => m.SalesHomeComponent),
    canActivate: [AuthGuard],
    data: { pageTitle: 'Point de vente' },
  },
  {
    path: 'prevente',
    loadComponent: () => import('./feature/presale-home/presale-home.component').then(m => m.PresaleHomeComponent),
    canActivate: [AuthGuard],
    data: { pageTitle: 'Prevente', abilitySubject: 'nouvelle-prevente' },
  },
  {
    path: 'devis',
    loadComponent: () => import('./feature/devis-home/devis-home.component').then(m => m.DevisHomeComponent),
    canActivate: [AuthGuard],
    data: { pageTitle: 'Devis' },
  },
  {
    path: 'gestion',
    loadComponent: () => import('./feature/sales-management-home/sales-management-home.component').then(m => m.SalesManagementHomeComponent),
    canActivate: [AuthGuard],
    data: {
      pageTitle: 'Gestion des ventes',
      abilitySubject: 'ventes',
    },
  },
  {
    path: 'vente-depot',
    loadComponent: () => import('./feature/vente-depot/vente-depot.component').then(m => m.VenteDepotComponent),
    canActivate: [AuthGuard],
    data: { pageTitle: 'Vente dépôt', abilitySubject: 'depot.liste-depots' },
  },
];
