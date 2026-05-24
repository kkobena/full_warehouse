import { Routes } from '@angular/router';

const gestionPerimesRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./gestion-peremption.component').then(m => m.GestionPeremptionComponent),
  },
  {
    path: 'edit',
    loadComponent: () => import('./ajout-perimes/ajout-perimes.component').then(m => m.AjoutPerimesComponent),
  },
];

export default gestionPerimesRoute;
