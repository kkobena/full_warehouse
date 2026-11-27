import { Routes } from '@angular/router';

const PROMOTION_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./feature/promotion-layout/promotion-layout.component').then(m => m.PromotionLayoutComponent),
    data: { pageTitle: 'Promotions' },
  },
];

export default PROMOTION_ROUTES;
