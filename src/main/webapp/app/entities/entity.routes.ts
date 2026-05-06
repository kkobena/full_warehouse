import { Routes } from "@angular/router";
import { AuthGuard } from "../core/auth/auth.guard";

const routes: Routes = [

  // ── Référentiel Produits ───────────────────────────────────────────────────
  {
    path: "categorie",
    data: { pageTitle: "warehouseApp.categorie.home.title" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./categorie/categorie.route")
  },
  {
    path: "rayon",
    data: { pageTitle: "warehouseApp.rayon.home.title", abilitySubject: "rayon" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./rayon/rayon.route")
  },
  {
    path: "forme-produit",
    data: { pageTitle: "warehouseApp.formeProduit.home.title", abilitySubject: "forme-produit" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./forme-produit/forme.route")
  },
  {
    path: "famille-produit",
    data: { pageTitle: "warehouseApp.familleProduit.home.title", abilitySubject: "famille-produit" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./famille-produit/famille-produit.route")
  },
  {
    path: "gamme-produit",
    data: { pageTitle: "warehouseApp.gammeProduit.home.title", abilitySubject: "gamme-produit" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./gamme-produit/gamme-produit.route")
  },
  {
    path: "laboratoire",
    data: { pageTitle: "warehouseApp.laboratoire.home.title", abilitySubject: "laboratoire" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./laboratoire-produit/laboratoire-produit.route")
  },

  // ── Référentiel Commercial ─────────────────────────────────────────────────
  {
    path: "remises",
    data: { pageTitle: "warehouseApp.remise.home.title", abilitySubject: "remises" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./remise/remise.route")
  },
  {
    path: "tableaux",
    data: { pageTitle: "warehouseApp.tableaux.home.title", abilitySubject: "tableaux" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./tableau-produit/tableau-produit.route")
  },
  {
    path: "tva",
    data: { pageTitle: "warehouseApp.tva.home.title", abilitySubject: "tva" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./tva/tva.route")
  },
  {
    path: "mode-payments",
    data: { pageTitle: "Modes de paiement", abilitySubject: "mode-payments" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./mode-payments/mode-payment.route")
  },

  // ── Référentiel Organisation ───────────────────────────────────────────────
  {
    path: "motif-ajustement",
    data: { pageTitle: "warehouseApp.motifAjustement.home.title", abilitySubject: "motif-ajustement" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./modif-ajustement/motif-ajustement.route")
  },
  {
    path: "motif-retour-produit",
    data: { pageTitle: "Motifs rétours produits", abilitySubject: "motif-retour-produit" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./motif-retour-produit/motif-retour-produit.route")
  },
  {
    // nav_item code = 'parametres' (avec 's') — voir V1.4.7
    path: "parametre",
    data: { pageTitle: "Paramètres", abilitySubject: "parametres" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./parametre/app.route")
  },

  // ── Admin — nav_items sous 'administration' ────────────────────────────────
  {
    path: "magasin",
    data: { pageTitle: "warehouseApp.magasin.home.title", abilitySubject: "magasin" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./magasin/magasin.route")
  },
  {
    path: "poste",
    data: { pageTitle: "Gestion des Postes", abilitySubject: "poste" },
    canActivate: [AuthGuard],
    loadChildren: () => import("../features/settings/feature/poste/poste.route")
  },

  // ── Compte utilisateur (auth only) ─────────────────────────────────────────
  {
    path: "my-cash-register",
    data: { pageTitle: "warehouseApp.userCash.home.title" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./cash-register/user-cash-register/user-cash-register.route")
  },

  // ── Dashboard (auth only) ──────────────────────────────────────────────────
  {
    path: "dashboard",
    data: { pageTitle: "Dashboard Personnalisable" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./dashboard/dashboard.route")
  },

  // ── Semois (auth only) ─────────────────────────────────────────────────────
  {
    path: "semois",
    data: { pageTitle: "SEMOIS - Gestion Stock" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./semois/semois.route")
  },

  // ── Business — contrôle ABAC via NavItemRole.canAccess ────────────────────
  {
    path: "commande",
    data: { pageTitle: "warehouseApp.commande.home.title", abilitySubject: "commande" },
    canActivate: [AuthGuard],
    loadChildren: () => import("../features/commande/commande.route")
  },
  {
    path: "customer",
    data: { pageTitle: "warehouseApp.customer.home.title", abilitySubject: "customer" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./customer/customer.route")
  },
  {
    path: "fournisseur",
    data: { pageTitle: "warehouseApp.fournisseur.home.title", abilitySubject: "fournisseurs" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./fournisseur/fournisseur.route")
  },
  {
    path: "mvt-caisse",
    data: { pageTitle: "warehouseApp.mvtCaisse.home.title", abilitySubject: "mvt-caisse" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./mvt-caisse/mvt-caisse.route")
  },
  {
    path: "tiers-payant",
    data: { pageTitle: "warehouseApp.tiersPayant.home.title", abilitySubject: "tiers-payant" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./tiers-payant/tiers-payant.route")
  },
  {
    path: "gestion-peremption",
    data: { pageTitle: "warehouseApp.gestionPerimes.title", abilitySubject: "peremptions" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./gestion-peremption/gestion-peremtion.route")
  },
  {
    path: "depot",
    data: { pageTitle: "Dépôts", abilitySubject: "depot" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./depot/depot.route")
  },

  // ── Redirections ───────────────────────────────────────────────────────────
  {
    path: "reglement-facture",
    redirectTo: "facturation",
    pathMatch: "prefix"
  },
  {
    path: "gestion-differe",
    redirectTo: "differes",
    pathMatch: "prefix"
  },

  // ── Facturation (ABAC) ─────────────────────────────────────────────────────
  {
    path: "facturation",
    data: { pageTitle: "Facturation & Règlements", abilitySubject: "factures" },
    canActivate: [AuthGuard],
    loadChildren: () => import("../features/facturation/facturation.routes")
  },
  {
    path: "differes",
    data: { pageTitle: "Différés" },
    canActivate: [AuthGuard],
    loadChildren: () => import("../features/differes/differes.routes")
  },

  // ── Features (ABAC) ────────────────────────────────────────────────────────
  {
    path: "sales-home",
    data: { pageTitle: "Point de vente" },
    canActivate: [AuthGuard],
    loadChildren: () => import("../features/sales/sales.routes").then(m => m.SALES_ROUTES)
  },
  {
    path: "inventaire",
    data: { pageTitle: "Inventaires", abilitySubject: "inventaire" },
    canActivate: [AuthGuard],
    loadChildren: () => import("../features/inventory/inventory.routes").then(m => m.INVENTORY_ROUTES)
  },
  {
    path: "produits",
    data: { pageTitle: "Catalogue produits", abilitySubject: "catalogue" },
    canActivate: [AuthGuard],
    loadChildren: () => import("../features/products/products.routes").then(m => m.PRODUCTS_ROUTES)
  },
  {
    path: "features-ajustement",
    data: { pageTitle: "Ajustements de stock", abilitySubject: "ajustements" },
    canActivate: [AuthGuard],
    loadChildren: () => import("../features/ajustement/ajustement.routes").then(m => m.AJUSTEMENT_ROUTES)
  },

  // ── Rapports (ABAC par sous-route dans reports.route.ts) ──────────────────
  {
    path: "reports",
    data: { pageTitle: "Rapports" },
    canActivate: [AuthGuard],
    loadChildren: () => import("./reports/reports.route")
  },

  // ── Finances (ABAC) ───────────────────────────────────────────────────────
  {
    path: "finances",
    data: { pageTitle: "Finances", abilitySubject: "finances" },
    canActivate: [AuthGuard],
    loadChildren: () => import("../features/finances/finances.routes")
  }
];

export default routes;
