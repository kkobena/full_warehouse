import { Routes } from '@angular/router';
import { AuthGuard } from 'app/core/auth/auth.guard';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./app-config-editor.component').then(m => m.AppConfigEditorComponent),
    canActivate: [AuthGuard],
    data: { pageTitle: 'Configuration avancée PharmaSmart' },
  },
];

export default routes;
