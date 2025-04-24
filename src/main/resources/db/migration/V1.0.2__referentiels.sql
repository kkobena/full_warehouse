INSERT IGNORE INTO magasin (id, address, name, note, phone, registre, type_magasin)
VALUES (1, '85 boulevard de l Europe, 69310', 'Warehouse', 'Bienvenue !', '+33652926383', '',
        'OFFICINE');

INSERT IGNORE INTO authority (name, libelle)
VALUES ('ROLE_ADMIN', 'Administrateur');
INSERT IGNORE INTO authority (name, libelle)
VALUES ('ROLE_USER', 'Utilisateur');

INSERT IGNORE INTO user (id, login, password_hash, first_name, last_name, email, image_url,
                         activated, lang_key, activation_key, reset_key, created_by, created_date,
                         reset_date, last_modified_by, last_modified_date, magasin_id)
VALUES (1, 'system', '$2a$10$mE.qmcV0mFU5NcKh73TZx.z4ueI/.bDWbj0T1BYyqP481kGGarKLG', 'System',
        'System', 'system@localhost', '', true, 'fr', null, null, 'system', null, null, 'system',
        null, 1);
INSERT IGNORE INTO user (id, login, password_hash, first_name, last_name, email, image_url,
                         activated, lang_key, activation_key, reset_key, created_by, created_date,
                         reset_date, last_modified_by, last_modified_date, magasin_id)
VALUES (2, 'anonymoususer', '$2a$10$j8S5d7Sr7.8VTOYNviDPOeWX8KcYILUVJBsYV83Y5NtECayypx9lO',
        'Anonymous', 'User', 'anonymous@localhost', '', true, 'fr', null, null, 'system', null,
        null, 'system', null, 1);
INSERT IGNORE INTO user (id, login, password_hash, first_name, last_name, email, image_url,
                         activated, lang_key, activation_key, reset_key, created_by, created_date,
                         reset_date, last_modified_by, last_modified_date, magasin_id)
VALUES (3, 'admin', '$2a$10$gSAhZrxMllrbgj/kkK9UceBPpChGWJA7SYIb1Mqo.n5aNLq1/oRrC', 'Administrator',
        'Administrator', 'admin@localhost', '', true, 'fr', null, null, 'system', null, null,
        'system', null, 1);

INSERT IGNORE INTO user_authority (user_id, authority_name)
VALUES (1, 'ROLE_ADMIN');
INSERT IGNORE INTO user_authority (user_id, authority_name)
VALUES (1, 'ROLE_USER');
INSERT IGNORE INTO user_authority (user_id, authority_name)
VALUES (3, 'ROLE_ADMIN');
INSERT IGNORE INTO user_authority (user_id, authority_name)
VALUES (3, 'ROLE_USER');


INSERT IGNORE INTO storage(`id`, `name`, `storage_type`, `magasin_id`)
VALUES (1, 'ENTREPOT', 0, 1);
INSERT IGNORE INTO storage(`id`, `name`, `storage_type`, `magasin_id`)
VALUES (2, 'POINT DE VENTE', 2, 1);
INSERT IGNORE INTO storage(`id`, `name`, `storage_type`, `magasin_id`)
VALUES (3, 'RESERVE', 1, 1);
INSERT IGNORE INTO tva (id, taux)
VALUES (1, 0),
       (2, 18),
       (3, 9);
INSERT IGNORE INTO categorie(id, code, libelle)
VALUES (1, '01', 'Grand Public'),
       (2, '02', 'Dietetique'),
       (3, '10', 'Specialité'),
       (4, '30', 'Accessoires'),
       (5, '40', 'Veterinaires'),
       (6, '20', 'Parfumerie');

INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150002, '1050', 'MEDICAMENTS FRANCE', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150003, '1041', 'H', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150004, '3020', 'PRODUITS CLARINS', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150005, '3040', 'PRODUITS BUCCO-DENTAIRES', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150006, '1060', 'PRODUITS CANCERO ROCHE', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150007, '8010', 'OSTEO-SYNTHESE', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150008, '6002', 'PIECES MONNAIE MESURE', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150009, '1070', 'PRODUITS CANCERO AVENTIS', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150010, '1080', 'MEDICAMENTS MARGE REDUITE', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150011, '1000', 'SPECIALITES PUBLIQUES', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150012, '1010', 'SPECIALITES HOPITAL', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150013, '1020', 'DECONDITIONNES', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150014, '1030', 'GENERIQUES', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150015, '1040', 'HOMEOPATHIE', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150016, '1900', 'PART 1/3 PAYANT', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150017, '2000', 'VETERINAIRES', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150018, '3000', 'PARFUMERIE LOCALE N1', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150019, '3010', 'PARFUMERIE LOCALE N2', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150020, '4000', 'ORTHOPEDIE', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150021, '5000', 'LAITS/ FARINES/ DIETETIQUE INF.', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150022, '7000', 'CHIMIQUES', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150023, '6000', 'DIETETIQUE ADULTE', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150024, '8000', 'ACCESSOIRES', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150025, '9000', 'DIVERS', 1);
INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`)
VALUES (150026, '3030', 'PARFUMERIE FRANCE', 6);

INSERT IGNORE INTO `groupe_fournisseur` (`id`, `libelle`, `odre`)
VALUES (1, 'LABOREX-CI', 1);
INSERT IGNORE INTO `groupe_fournisseur` (`id`, `libelle`, `odre`)
VALUES (2, 'DPCI', 2);
INSERT IGNORE INTO `groupe_fournisseur` (`id`, `libelle`, `odre`)
VALUES (3, 'COPHARMED', 3);
INSERT IGNORE INTO `groupe_fournisseur` (`id`, `libelle`, `odre`)
VALUES (4, 'TEDIS PHAR.', 4);
INSERT IGNORE INTO `groupe_fournisseur` (`id`, `libelle`, `odre`)
VALUES (5, 'AUTRES', 100);

INSERT IGNORE INTO form_produit(id, libelle)
VALUES (1, 'Comprimés'),
       (2, 'Sachets');

INSERT IGNORE INTO `app_configuration` (`name`, `value`, `description`, `value_type`)
VALUES ('APP_GESTION_STOCK', '0',
        'Parametre qui permet la gestion du stock ; deux valeurs sont autorisées: 0 ==> pour un seul stockage et 1===> pour un stockage multiple',
        'BOOLEAN');
INSERT IGNORE INTO `payment_mode` (`code`, `libelle`, `payment_group`, `ordre_tri`, `enable`)
VALUES ('CASH', 'ESPECE', 0, 1, true);
INSERT IGNORE INTO `payment_mode` (`code`, `libelle`, `payment_group`, `ordre_tri`, `enable`)
VALUES ('OM', 'ORANGE', 2, 2, true);
INSERT IGNORE INTO `payment_mode` (`code`, `libelle`, `payment_group`, `ordre_tri`, `enable`)
VALUES ('MTN', 'MTN', 2, 3, true);
INSERT IGNORE INTO `payment_mode` (`code`, `libelle`, `payment_group`, `ordre_tri`, `enable`)
VALUES ('MOOV', 'MOOV', 2, 4, true);
INSERT IGNORE INTO `payment_mode` (`code`, `libelle`, `payment_group`, `ordre_tri`, `enable`)
VALUES ('WAVE', 'WAVE', 2, 5, true);
INSERT IGNORE INTO `payment_mode` (`code`, `libelle`, `payment_group`, `ordre_tri`, `enable`)
VALUES ('CB', 'CARTE BANCAIRE', 3, 6, true);
INSERT IGNORE INTO `payment_mode` (`code`, `libelle`, `payment_group`, `ordre_tri`, `enable`)
VALUES ('VIREMENT', 'VIREMENT', 4, 7, true);
INSERT IGNORE INTO `payment_mode` (`code`, `libelle`, `payment_group`, `ordre_tri`, `enable`)
VALUES ('CH', 'CHEQUE', 5, 8, true);
INSERT IGNORE INTO rayon (id, code, exclude, libelle, storage_id)
VALUES (1, 'SANS', 0, 'SANS EMPLACEMENT', 2);
INSERT IGNORE INTO rayon (id, code, exclude, libelle, storage_id)
VALUES (2, 'SANS', 0, 'SANS EMPLACEMENT', 1);
INSERT IGNORE INTO rayon (id, code, exclude, libelle, storage_id)
VALUES (3, 'SANS', 0, 'SANS EMPLACEMENT', 3);
INSERT IGNORE INTO `app_configuration` (`name`, `value`, `description`, `value_type`)
VALUES ('APP_QTY_MAX', '999', 'Quantité maximale à vendre ', 'NUMBER');
INSERT
    IGNORE
INTO app_configuration(name, description, value, other_value, `value_type`)
values ('APP_CASH_FUND', 'Ouverture automatique de la caisse du caissier', '0', null, 'BOOLEAN');
INSERT
    IGNORE
INTO app_configuration(name, description, value, other_value, `value_type`)
values ('APP_SANS_NUM_BON', 'Autorisation de vente sans numéro de bon', '0', null, 'BOOLEAN');
INSERT
    IGNORE
INTO app_configuration(name, description, value, other_value, `value_type`)
values ('APP_ENTREE_STOCK_SANS_EXPIRY_DATE',
        'Autorisation entrée stock sans control date péremption', '0', null, 'BOOLEAN');
INSERT
    IGNORE
INTO warehouse_sequence(`name`, `increment`, `seq_value`)
VALUES ('ENTREE_STOCK', 1, 1);

INSERT IGNORE INTO app_configuration(name, description, value, other_value, `value_type`)
values ('APP_DAY_STOCK',
        'Nombre de jours par stock', '10', null, 'NUMBER');
INSERT IGNORE INTO app_configuration(name, description, value, other_value, `value_type`)
values ('APP_LAST_DAY_REAPPRO',
        'Dernière date de mise à jour des seuils de réappro', null, null, 'DATE');

INSERT IGNORE INTO app_configuration(name, description, value, other_value, `value_type`)
values ('APP_LIMIT_NBR_DAY_REAPPRO',
        'Nombre de jour de delai de réapprovisionnement', '8', null, 'NUMBER');
INSERT IGNORE INTO app_configuration(name, description, value, other_value, `value_type`)
values ('APP_DENOMINATEUR_REAPPRO',
        'denominateur du calcul de réappro', '84', null, 'NUMBER');
INSERT IGNORE INTO authority (name, libelle)
VALUES ('ROLE_VENDEUR', 'Vendeur');
INSERT IGNORE INTO authority (name, libelle)
VALUES ('ROLE_CAISSIER', 'Caissier');
INSERT IGNORE INTO authority (name, libelle)
VALUES ('ROLE_RESPONSABLE_COMMANDE', 'Responsable de commande');

INSERT IGNORE INTO privilege (name, libelle, menu_id)
VALUES ('PR_FORCE_STOCK', 'Privilège de Forçage du stock à la vente', 37);
INSERT IGNORE INTO privilege (name, libelle, menu_id)
VALUES ('PR_MODIFIER_PRIX', 'Privilège de modification du prix de vente du produit à la vente', 37);
INSERT IGNORE INTO privilege (name, libelle, menu_id)
VALUES ('PR_MODIFICATION_VENTE', 'Privilège de modification de vente', 37);

INSERT IGNORE INTO privilege (name, libelle, menu_id)
VALUES ('PR_ANNULATION_VENTE', 'Privilège annulation se vente', 37);
INSERT IGNORE INTO privilege (name, libelle, menu_id)
VALUES ('PR_SUPPRIME_PRODUIT_VENTE', 'Privilège suppression d\'une ligne de produit à la vente',
        37);
INSERT IGNORE INTO privilege (name, libelle, menu_id)
VALUES ('PR_AJOUTER_REMISE_VENTE', 'Privilège appliquer une remise à une vente', 37);
INSERT IGNORE INTO privilege (name, libelle, menu_id)
VALUES ('PR_VOIR_STOCK_INVENTAIRE', 'Privilège affichage du stock des produits inventoriés', 3);


INSERT IGNORE INTO authority_privilege (authority_name, privilege_name)
VALUES ('ROLE_ADMIN', 'PR_FORCE_STOCK');
INSERT IGNORE INTO authority_privilege (authority_name, privilege_name)
VALUES ('ROLE_ADMIN', 'PR_MODIFIER_PRIX');

INSERT IGNORE INTO authority_privilege (authority_name, privilege_name)
VALUES ('ROLE_ADMIN', 'PR_MODIFICATION_VENTE');
INSERT IGNORE INTO authority_privilege (authority_name, privilege_name)
VALUES ('ROLE_ADMIN', 'PR_ANNULATION_VENTE');



INSERT IGNORE INTO authority_privilege (authority_name, privilege_name)
VALUES ('ROLE_ADMIN', 'PR_VOIR_STOCK_INVENTAIRE');
INSERT IGNORE INTO tableau(code, valeur)
VALUES ('A', 0);
INSERT IGNORE INTO tableau(code, valeur)
VALUES ('C', 0);

INSERT IGNORE INTO user_authority
(user_id, authority_name)
VALUES (3, 'ROLE_CAISSIER');
INSERT IGNORE INTO user_authority
(user_id, authority_name)
VALUES (3, 'ROLE_RESPONSABLE_COMMANDE');
INSERT IGNORE INTO user_authority
(user_id, authority_name)
VALUES (3, 'ROLE_USER');

INSERT IGNORE INTO user_authority
(user_id, authority_name)
VALUES (3, 'ROLE_VENDEUR');
INSERT IGNORE INTO user_authority
(user_id, authority_name)
VALUES (3, 'ROLE_ADMIN');
