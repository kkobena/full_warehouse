import { Routes } from '@angular/router';

const DCI_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./feature/dci-home/dci-home.component').then(m => m.DciHomeComponent),
    data: { pageTitle: 'DCI' },
  },
];

export default DCI_ROUTES;
