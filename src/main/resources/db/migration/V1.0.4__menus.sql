INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (27, 'gestion-courant', 'Menu Gestion courante', true, null, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (28, 'gestion-stock', 'Menu Gestion stock', true, null, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (29, 'referentiel', 'Menu referentiel', true, null, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (30, 'gestion-tiers-payant', 'Menu gestion tiers-payant', true, null, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (31, 'gestion-facturation', 'Menu gestion de la facturation', true, null, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (32, 'administration', 'Menu administration', true, null, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (33, 'menu', 'Gestion des menus', false, 32, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (34, 'privilege', 'Gestion des privilège', false, 32, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (1, 'admin', 'Menu administration', false, 32, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (2, 'inventory-transaction', 'Menu suivi produit', false, 28, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (3, 'store-inventory', 'Menu inventaire', false, 28, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (4, 'tableaux', 'Menu gestion des tableau', false, 29, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (35, 'gestion-entree', 'Menu gestion des entrées en stock', false, 28, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (5, 'tiers-payant', 'Menu gestion des tiers-payants', false, 30, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (6, 'groupe-tiers-payant', 'Menu gestion des groupes de  tiers-payants', false, 30, true,10) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (7, 'motif-ajustement', 'Menu motifs ajustement', false, 29, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (8, 'type-etiquette', 'Menu type étiquette', false, 29, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (9, 'gamme-produit', 'Menu gamme de produits', false, 29, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (10, 'laboratoire', 'Menu laboratoire', false, 29, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (11, 'groupe-fournisseur', 'Menu groupe de fournisseurs', false, 29, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (12, 'fournisseur', 'Menu fournisseurs', false, 29, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (13, 'rayon', 'Menu rayons', false, 29, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (14, 'famille-produit', 'Menu de famille de produits', false, 29, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (15, 'forme-produit', 'Menu forme de produits', false, 29, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (16, 'tva', 'Menu tva', false, 29, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (17, 'ajustement', 'Menu ajustement', false, 28, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (18, 'decondition', 'Menu déconditionnement de produits', false, 28, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (19, 'magasin', 'Menu magasin', false, 32, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (20, 'commande', 'Menu gestion de commande', false, 28, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (21, 'payment', 'Menu paiements', false, 31, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (22, 'presale', 'Menu préventes', false, 27, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (23, 'sales-line', 'Menu details de vente', false, 28, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (24, 'customer', 'Menu des clients', false, 31, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (25, 'produit', 'Menu produits', false, 28, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (26, 'categorie', 'Menu catégorie', false, 29, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (36, 'ventes-en-cours', 'Menu ventes en cours', false, 27, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (37, 'sales', 'Menu journal des ventes', false, 27, true, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (38, 'user-management', 'Menu utilisateurs', false, 32, true, 1) ON CONFLICT (id) DO NOTHING;

INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (39, 'mobile_admin', 'Privilège pour toutes les fonctionnaliés de l Application mobile',
        true, null, true, 1) ON CONFLICT (id) DO NOTHING;

INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (40, 'mobile_user', 'Privilège d utilisation l Application pour les utilisateurs',
        true, null, true, 1) ON CONFLICT (id) DO NOTHING;

INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (41, 'MENU_DEPOT', 'Gestion des dépôts',
        true, null, true, 10) ON CONFLICT (id) DO NOTHING;

INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (42, 'MENU_POSTE', 'Menu postes de travail',
        true, null, true, 10) ON CONFLICT (id) DO NOTHING;

INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (43, 'remise', 'Gestion des remises',
        true, null, true, 10) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (44, 'gestion-perimes', 'Gestion des produits périmés',
        true, null, true, 10) ON CONFLICT (id) DO NOTHING;
INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (45, 'gestion-differe', 'Gestion des ventes à différé',
        true, null, true, 10) ON CONFLICT (id) DO NOTHING;

INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (46, 'reglement-facture', 'Gestion des règlements de factures',
        true, null, true, 10) ON CONFLICT (id) DO NOTHING;

INSERT INTO menu (id, name, libelle, racine, parent_id, enable, "ordre")
values (47, 'my-cash-register', 'Menu ma caisse',
        true, null, true, 10) ON CONFLICT (id) DO NOTHING;



INSERT INTO privilege (name, libelle, menu_id)
VALUES ('PR_FORCE_STOCK', 'Privilège de Forçage du stock à la vente', 37) ON CONFLICT (name) DO NOTHING;
INSERT INTO privilege (name, libelle, menu_id)
VALUES ('PR_MODIFIER_PRIX', 'Privilège de modification du prix de vente du produit à la vente', 37) ON CONFLICT (name) DO NOTHING;
INSERT INTO privilege (name, libelle, menu_id)
VALUES ('PR_MODIFICATION_VENTE', 'Privilège de modification de vente', 37) ON CONFLICT (name) DO NOTHING;

INSERT INTO privilege (name, libelle, menu_id)
VALUES ('PR_ANNULATION_VENTE', 'Privilège annulation se vente', 37) ON CONFLICT (name) DO NOTHING;
INSERT INTO privilege (name, libelle, menu_id)
VALUES ('PR_SUPPRIME_PRODUIT_VENTE', 'Privilège suppression d une ligne de produit à la vente', 37) ON CONFLICT (name) DO NOTHING;
INSERT INTO privilege (name, libelle, menu_id)
VALUES ('PR_AJOUTER_REMISE_VENTE', 'Privilège appliquer une remise à une vente', 37) ON CONFLICT (name) DO NOTHING;
INSERT INTO privilege (name, libelle, menu_id)
VALUES ('PR_VOIR_STOCK_INVENTAIRE', 'Privilège affichage du stock des produits inventoriés', 3) ON CONFLICT (name) DO NOTHING;


INSERT INTO authority_privilege (authority_name, privilege_name)
VALUES ('ROLE_ADMIN', 'PR_FORCE_STOCK') ON CONFLICT (authority_name, privilege_name) DO NOTHING;
INSERT INTO authority_privilege (authority_name, privilege_name)
VALUES ('ROLE_ADMIN', 'PR_MODIFIER_PRIX') ON CONFLICT (authority_name, privilege_name) DO NOTHING;

INSERT INTO authority_privilege (authority_name, privilege_name)
VALUES ('ROLE_ADMIN', 'PR_MODIFICATION_VENTE') ON CONFLICT (authority_name, privilege_name) DO NOTHING;
INSERT INTO authority_privilege (authority_name, privilege_name)
VALUES ('ROLE_ADMIN', 'PR_ANNULATION_VENTE') ON CONFLICT (authority_name, privilege_name) DO NOTHING;
INSERT INTO authority_privilege (authority_name, privilege_name)
VALUES ('ROLE_ADMIN', 'PR_VOIR_STOCK_INVENTAIRE') ON CONFLICT (authority_name, privilege_name) DO NOTHING;

INSERT INTO tableau(code, valeur)
VALUES ('A', 0) ON CONFLICT (code) DO NOTHING;
INSERT INTO tableau(code, valeur)
VALUES ('C', 0) ON CONFLICT (code) DO NOTHING;

INSERT INTO user_authority
(user_id, authority_name)
VALUES (3, 'ROLE_CAISSIER') ON CONFLICT (user_id, authority_name) DO NOTHING;
INSERT INTO user_authority
(user_id, authority_name)
VALUES (3, 'ROLE_RESPONSABLE_COMMANDE') ON CONFLICT (user_id, authority_name) DO NOTHING;
INSERT INTO user_authority
(user_id, authority_name)
VALUES (3, 'ROLE_USER') ON CONFLICT (user_id, authority_name) DO NOTHING;

INSERT INTO user_authority
(user_id, authority_name)
VALUES (3, 'ROLE_VENDEUR') ON CONFLICT (user_id, authority_name) DO NOTHING;
INSERT INTO user_authority
(user_id, authority_name)
VALUES (3, 'ROLE_ADMIN') ON CONFLICT (user_id, authority_name) DO NOTHING;

