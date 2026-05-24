import { Routes } from '@angular/router';

const RAYON_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./feature/rayon-home/rayon-home.component').then(m => m.RayonHomeComponent),
    data: { pageTitle: 'Rayons' },
  },
];

export default RAYON_ROUTES;
