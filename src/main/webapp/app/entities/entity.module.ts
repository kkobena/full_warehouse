import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: 'inventory-transaction',
        loadChildren: () => import('./inventory-transaction/inventory-transaction.module').then(m => m.WarehouseInventoryTransactionModule),
      },
      {
        path: 'categorie',
        loadChildren: () => import('./categorie/categorie.module').then(m => m.WarehouseCategorieModule),
      },
      {
        path: 'produit',
        loadChildren: () => import('./produit/produit.module').then(m => m.WarehouseProduitModule),
      },
      {
        path: 'customer',
        loadChildren: () => import('./customer/customer.module').then(m => m.WarehouseCustomerModule),
      },
      {
        path: 'sales-line',
        loadChildren: () => import('./sales-line/sales-line.module').then(m => m.WarehouseSalesLineModule),
      },
      {
        path: 'sales',
        loadChildren: () => import('./sales/sales.module').then(m => m.WarehouseSalesModule),
      },
      {
        path: 'payment',
        loadChildren: () => import('./payment/payment.module').then(m => m.WarehousePaymentModule),
      },
      {
        path: 'payment-mode',
        loadChildren: () => import('./payment-mode/payment-mode.module').then(m => m.WarehousePaymentModeModule),
      },
      {
        path: 'order-line',
        loadChildren: () => import('./order-line/order-line.module').then(m => m.WarehouseOrderLineModule),
      },
      {
        path: 'commande',
        loadChildren: () => import('./commande/commande.module').then(m => m.WarehouseCommandeModule),
      },
      {
        path: 'payment-fournisseur',
        loadChildren: () => import('./payment-fournisseur/payment-fournisseur.module').then(m => m.WarehousePaymentFournisseurModule),
      },

      {
        path: 'store-inventory',
        loadChildren: () => import('./store-inventory/store-inventory.module').then(m => m.WarehouseStoreInventoryModule),
      },
      {
        path: 'menu',
        loadChildren: () => import('./menu/menu.module').then(m => m.WarehouseMenuModule),
      },
      {
        path: 'magasin',
        loadChildren: () => import('./magasin/magasin.module').then(m => m.WarehouseMagasinModule),
      },
      {
        path: 'decondition',
        loadChildren: () => import('./decondition/decondition.module').then(m => m.WarehouseDeconditionModule),
      },
      {
        path: 'ajustement',
        loadChildren: () => import('./ajustement/ajustement.module').then(m => m.WarehouseAjustementModule),
      },
      /* jhipster-needle-add-entity-route - JHipster will add entity modules routes here */
    ]),
  ],
})
export class WarehouseEntityModule {}
