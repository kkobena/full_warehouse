import { Routes } from '@angular/router';

const semoisRoutes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('../../features/commande/feature/semois-dashboard/semois-dashboard.component').then(
        m => m.SemoisDashboardComponent,
      ),
    data: { pageTitle: 'Dashboard SEMOIS' },
  },
  {
    path: 'suggestions',
    loadComponent: () =>
      import('../../features/commande/feature/semois-suggestions/semois-suggestions.component').then(
        m => m.SemoisSuggestionsComponent,
      ),
    data: { pageTitle: 'Suggestions SEMOIS' },
  },
  {
    path: 'config-masse',
    loadComponent: () =>
      import('../../features/commande/feature/semois-config-masse/semois-config-masse.component').then(
        m => m.SemoisConfigMasseComponent,
      ),
    data: { pageTitle: 'Configuration SEMOIS en Masse' },
  },
  {
    path: 'model-config',
    loadComponent: () =>
      import('../../features/commande/feature/semois-model-config/semois-model-config.component').then(
        m => m.SemoisModelConfigComponent,
      ),
    data: { pageTitle: 'Configuration Modèle Réapprovisionnement' },
  },
];

export default semoisRoutes;
