import { Routes } from '@angular/router';

const modePaymentRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./mode-payment.component').then(m => m.ModePaymentComponent),
  },
];

export default modePaymentRoute;
