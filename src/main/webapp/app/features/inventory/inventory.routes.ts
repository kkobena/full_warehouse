import { Routes } from '@angular/router';

export const INVENTORY_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./feature/inventory-home/inventory-home.component').then(m => m.InventoryHomeComponent),
    data: { pageTitle: 'Inventaires' },
  },
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./feature/inventory-editor/inventory-editor.component').then(m => m.InventoryEditorComponent),
    data: { pageTitle: "Éditeur d'inventaire" },
  },
];

export default INVENTORY_ROUTES;
