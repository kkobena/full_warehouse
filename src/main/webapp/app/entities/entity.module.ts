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
      {
        path: 'tva',
        loadChildren: () => import('./tva/tva.module').then(m => m.WarehouseTvaModule),
      },
      {
        path: 'forme-produit',
        loadChildren: () => import('./forme-produit/forme-produit.module').then(m => m.WarehouseFormeProduitModule),
      },
      {
        path: 'famille-produit',
        loadChildren: () => import('./famille-produit/forme-produit.module').then(m => m.WarehouseFamilleProduitModule),
      },
      {
        path: 'rayon',
        loadChildren: () => import('./rayon/rayon.module').then(m => m.WarehouseRayonModule),
      },
      {
        path: 'fournisseur',
        loadChildren: () => import('./fournisseur/fournisseur.module').then(m => m.WarehouseFournisseurModule),
      },
      {
        path: 'groupe-fournisseur',
        loadChildren: () => import('./groupe-fournisseur/groupe-fournisseur.module').then(m => m.WarehouseGroupeFournisseurModule),
      },

      {
        path: 'laboratoire',
        loadChildren: () => import('./laboratoire-produit/laboratoire-produit.module').then(m => m.WarehouseLaboratoireProduitModule),
      },
      {
        path: 'gamme-produit',
        loadChildren: () => import('./gamme-produit/gamme-produit.module').then(m => m.WarehouseGammeProduitModule),
      },
      {
        path: 'type-etiquette',
        loadChildren: () => import('./type-etiquette/type-etiquette.module').then(m => m.WarehouseTypeEtiquetteModule),
      },
    ]),
  ],
})
export class WarehouseEntityModule {}
