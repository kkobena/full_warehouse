import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./feature/pilotage-page/pilotage-page.component').then(m => m.PilotagePageComponent),
    data: { pageTitle: "Pilotage de l'officine" },
  },
];

export default routes;
