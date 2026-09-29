import { CanDeactivateFn, Routes } from '@angular/router';
import type { DashboardEditorComponent } from './feature/dashboard-editor/dashboard-editor.component';

/** Demande confirmation avant de quitter l'éditeur avec des modifications non enregistrées. */
export const unsavedDashboardGuard: CanDeactivateFn<DashboardEditorComponent> = component => component.confirmLeave();

const editor = () => import('./feature/dashboard-editor/dashboard-editor.component').then(m => m.DashboardEditorComponent);

export const DASHBOARD_ROUTES: Routes = [
  { path: '', loadComponent: editor, canDeactivate: [unsavedDashboardGuard] },
  { path: ':id', loadComponent: editor, canDeactivate: [unsavedDashboardGuard] },
];

export default DASHBOARD_ROUTES;
