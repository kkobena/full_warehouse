import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: 'ajustement',
    data: { pageTitle: 'warehouseApp.ajustement.home.title' },
    loadChildren: () => import('./ajustement/ajustement.route'),
  },
  {
    path: 'categorie',
    data: { pageTitle: 'warehouseApp.categorie.home.title' },
    loadChildren: () => import('./categorie/categorie.route'),
  },
  {
    path: 'gestion-entree',
    data: { pageTitle: 'warehouseApp.delivery.home.title' },
    loadChildren: () => import('./commande/delevery/delivery.route'),
  },
  {
    path: 'commande',
    data: { pageTitle: 'warehouseApp.commande.home.title' },
    loadChildren: () => import('./commande/commande.route'),
  },
  {
    path: 'customer',
    data: { pageTitle: 'warehouseApp.customer.home.title' },
    loadChildren: () => import('./customer/customer.route'),
  },

  {
    path: 'famille-produit',
    data: { pageTitle: 'warehouseApp.familleProduit.home.title' },
    loadChildren: () => import('./famille-produit/famille-produit.route'),
  },
  {
    path: 'forme-produit',
    data: { pageTitle: 'warehouseApp.formeProduit.home.title' },
    loadChildren: () => import('./forme-produit/forme.route'),
  },
  {
    path: 'fournisseur',
    data: { pageTitle: 'warehouseApp.fournisseur.home.title' },
    loadChildren: () => import('./fournisseur/fournisseur.route'),
  },

  {
    path: 'gamme-produit',
    data: { pageTitle: 'warehouseApp.gammeProduit.home.title' },
    loadChildren: () => import('./gamme-produit/gamme-produit.route'),
  },
  {
    path: 'groupe-fournisseur',
    data: { pageTitle: 'warehouseApp.groupeFournisseur.home.title' },
    loadChildren: () => import('./groupe-fournisseur/groupe-fournisseur.route'),
  },
  {
    path: 'groupe-tiers-payant',
    data: { pageTitle: 'warehouseApp.groupeTiersPayant.home.title' },
    loadChildren: () => import('./groupe-tiers-payant/groupe-tiers-payant.route'),
  },
  {
    path: 'inventory-transaction',
    data: { pageTitle: 'warehouseApp.inventoryTransaction.home.title' },
    loadChildren: () => import('./inventory-transaction/inventory-transaction.route'),
  },
  {
    path: 'laboratoire',
    data: { pageTitle: 'warehouseApp.laboratoire.home.title' },
    loadChildren: () => import('./laboratoire-produit/laboratoire-produit.route'),
  },
  {
    path: 'magasin',
    data: { pageTitle: 'warehouseApp.magasin.home.title' },
    loadChildren: () => import('./magasin/magasin.route'),
  },
  {
    path: 'menu',
    data: { pageTitle: 'warehouseApp.menu.home.title' },
    loadChildren: () => import('./menu/menu.route'),
  },
  {
    path: 'motif-ajustement',
    data: { pageTitle: 'warehouseApp.motifAjustement.home.title' },
    loadChildren: () => import('./modif-ajustement/motif-ajustement.route'),
  },
  {
    path: 'rayon',
    data: { pageTitle: 'warehouseApp.rayon.home.title' },
    loadChildren: () => import('./rayon/rayon.route'),
  },
  {
    path: 'tableaux',
    data: { pageTitle: 'warehouseApp.tableaux.home.title' },
    loadChildren: () => import('./tableau-produit/tableau-produit.route'),
  },
  {
    path: 'tva',
    data: { pageTitle: 'warehouseApp.tva.home.title' },
    loadChildren: () => import('./tva/tva.route'),
  },
  {
    path: 'type-etiquette',
    data: { pageTitle: 'warehouseApp.typeEtiquette.home.title' },
    loadChildren: () => import('./type-etiquette/type-etiquette.route'),
  },
  {
    path: 'produit',
    data: { pageTitle: 'warehouseApp.produit.home.title' },
    loadChildren: () => import('./produit/produit.route'),
  },
  {
    path: 'tiers-payant',
    data: { pageTitle: 'warehouseApp.tiersPayant.home.title' },
    loadChildren: () => import('./tiers-payant/tiers-payant.route'),
  },
  {
    path: 'store-inventory',
    data: { pageTitle: 'warehouseApp.storeInventory.home.title' },
    loadChildren: () => import('./store-inventory/store-inventory.route'),
  },
  {
    path: 'sales',
    data: { pageTitle: 'warehouseApp.storeInventory.home.title' },
    loadChildren: () => import('./sales/sales.route'),
  },
  {
    path: 'remises',
    data: { pageTitle: 'warehouseApp.remise.home.title' },
    loadChildren: () => import('./remise/remise.route'),
  },
  {
    path: 'my-cash-register',
    data: { pageTitle: 'warehouseApp.userCash.home.title' },
    loadChildren: () => import('./cash-register/user-cash-register/user-cash-register.route'),
  },

  {
    path: 'mvt-caisse',
    data: { pageTitle: 'warehouseApp.mvtCaisse.home.title' },
    loadChildren: () => import('./mvt-caisse/mvt-caisse.route'),
  },
  {
    path: 'parametre',
    data: { pageTitle: 'Paramètres' },
    loadChildren: () => import('./parametre/app.route'),
  },
  {
    path: 'edition-factures',
    data: { pageTitle: 'Gestion facturation' },
    loadChildren: () => import('./facturation/facture.route'),
  },
  {
    path: 'reglement-facture',
    data: { pageTitle: 'Réglement de facture' },
    loadChildren: () => import('./reglement/reglement.route'),
  },
];

export default routes;
