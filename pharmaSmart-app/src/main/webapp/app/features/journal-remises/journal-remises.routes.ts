import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./feature/journal-remises-layout/journal-remises-layout.component').then(m => m.JournalRemisesLayoutComponent),
    data: { pageTitle: 'Journal des remises' },
  },
];

export default routes;
