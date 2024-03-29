INSERT IGNORE INTO magasin (id, address, name, note, phone, registre) VALUES (1, '17 rue de bruxelle', 'Easy shop', 'Bienvenue !', '075759146', '');
INSERT IGNORE  INTO authority (name) VALUES ('ROLE_ADMIN');
INSERT IGNORE INTO authority (name) VALUES ('ROLE_USER');
INSERT IGNORE INTO magasin (id, address, name, note, phone, registre) VALUES (1, '', '', 'Bienvenue !', '', '');
INSERT IGNORE INTO user (id, login, password_hash, first_name, last_name, email, image_url, activated, lang_key, activation_key, reset_key, created_by, created_date, reset_date, last_modified_by, last_modified_date,magasin_id) VALUES (1, 'system', '$2a$10$mE.qmcV0mFU5NcKh73TZx.z4ueI/.bDWbj0T1BYyqP481kGGarKLG', 'System', 'System', 'system@localhost', '', true, 'fr', null, null, 'system', null, null, 'system', null,1);
INSERT IGNORE INTO user (id, login, password_hash, first_name, last_name, email, image_url, activated, lang_key, activation_key, reset_key, created_by, created_date, reset_date, last_modified_by, last_modified_date,magasin_id) VALUES (2, 'anonymoususer', '$2a$10$j8S5d7Sr7.8VTOYNviDPOeWX8KcYILUVJBsYV83Y5NtECayypx9lO', 'Anonymous', 'User', 'anonymous@localhost', '', true, 'fr', null, null, 'system', null, null, 'system', null,1);
INSERT IGNORE INTO user (id, login, password_hash, first_name, last_name, email, image_url, activated, lang_key, activation_key, reset_key, created_by, created_date, reset_date, last_modified_by, last_modified_date,magasin_id) VALUES (3, 'admin', '$2a$10$gSAhZrxMllrbgj/kkK9UceBPpChGWJA7SYIb1Mqo.n5aNLq1/oRrC', 'Administrator', 'Administrator', 'admin@localhost', '', true, 'fr', null, null, 'system', null, null, 'system', null,1);
INSERT IGNORE INTO user (id, login, password_hash, first_name, last_name, email, image_url, activated, lang_key, activation_key, reset_key, created_by, created_date, reset_date, last_modified_by, last_modified_date,magasin_id) VALUES (4, 'user', '$2a$10$VEjxo0jq2YG9Rbk2HmX9S.k1uZBGYUHdUcid3g/vfiEl7lwWgOH/K', 'User', 'User', 'user@localhost', '', true, 'fr', null, null, 'system', null, null, 'system', null,1);


INSERT IGNORE INTO user_authority (user_id, authority_name) VALUES (1, 'ROLE_ADMIN');
INSERT IGNORE INTO user_authority (user_id, authority_name) VALUES (1, 'ROLE_USER');
INSERT IGNORE INTO user_authority (user_id, authority_name) VALUES (3, 'ROLE_ADMIN');
INSERT IGNORE INTO user_authority (user_id, authority_name) VALUES (3, 'ROLE_USER');
INSERT IGNORE INTO user_authority (user_id, authority_name) VALUES (4, 'ROLE_USER');

INSERT IGNORE INTO storage(	`id` ,	`name` ,`storage_type`,`magasin_id`) VALUES (1, 'ENTREPOT',0,1);
INSERT IGNORE INTO storage(	`id` ,	`name` ,`storage_type`,`magasin_id`) VALUES (2, 'POINT DE VENTE',2,1);
INSERT IGNORE INTO storage(	`id` ,	`name` ,`storage_type`,`magasin_id`) VALUES (3, 'RESERVE',1,1);
INSERT IGNORE INTO tva (id,taux) VALUES (1,0),(2,18),(3,9);
INSERT IGNORE INTO categorie(id,code,libelle) VALUES (1,'01','Grand Public'), (2,'02','Dietetique'),
                                                      (3,'10','Specialité'), (4,'30','Accessoires'), (5,'40','Veterinaires'),
                                                      (6,'20','Parfumerie');

INSERT IGNORE INTO `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150002, '1050', 'MEDICAMENTS FRANCE', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150003, '1041', 'H', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150004, '3020', 'PRODUITS CLARINS', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150005, '3040', 'PRODUITS BUCCO-DENTAIRES', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150006, '1060', 'PRODUITS CANCERO ROCHE', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150007, '8010', 'OSTEO-SYNTHESE', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150008, '6002', 'PIECES MONNAIE MESURE', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150009, '1070', 'PRODUITS CANCERO AVENTIS', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150010, '1080', 'MEDICAMENTS MARGE REDUITE', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150011, '1000', 'SPECIALITES PUBLIQUES', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150012, '1010', 'SPECIALITES HOPITAL', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150013, '1020', 'DECONDITIONNES', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150014, '1030', 'GENERIQUES', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150015, '1040', 'HOMEOPATHIE', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150016, '1900', 'PART 1/3 PAYANT', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150017, '2000', 'VETERINAIRES', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150018, '3000', 'PARFUMERIE LOCALE N1', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150019, '3010', 'PARFUMERIE LOCALE N2', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150020, '4000', 'ORTHOPEDIE', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150021, '5000', 'LAITS/ FARINES/ DIETETIQUE INF.', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150022, '7000', 'CHIMIQUES', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150023, '6000', 'DIETETIQUE ADULTE', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150024, '8000', 'ACCESSOIRES', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150025, '9000', 'DIVERS', 1);
INSERT IGNORE INTO  `famille_produit` (`id`, `code`, `libelle`, `categorie_id`) VALUES (150026, '3030', 'PARFUMERIE FRANCE', 6);

INSERT IGNORE INTO `groupe_fournisseur` (`id`,`libelle`,`odre`) VALUES (1,'LABOREX-CI',1);
INSERT IGNORE INTO `groupe_fournisseur` (`id`,`libelle`,`odre`) VALUES (2,'DPCI',2);
INSERT IGNORE INTO `groupe_fournisseur` (`id`,`libelle`,`odre`) VALUES (3,'COPHARMED',3);
INSERT IGNORE INTO `groupe_fournisseur` (`id`,`libelle`,`odre`) VALUES (4,'TEDIS PHAR.',4);
INSERT IGNORE INTO `groupe_fournisseur` (`id`,`libelle`,`odre`) VALUES (5,'AUTRES',100);

INSERT IGNORE INTO type_etiquette(id,libelle) VALUES (1,'CIP'), (2,'CIP_PRIX'),
                                              (3,'CIP_DESIGNATION'), (4,'CIP_PRIX_DESIGNATION'),(5,'POSITION');
INSERT IGNORE INTO form_produit(id,libelle) VALUES(1,'Comprimés'),(2,'Sachets');
INSERT IGNORE INTO `app_configuration` (`name`,`value`,`description`) VALUES ('APP_GESTION_STOCK','0','Parametre qui permet de gestion du stock ; deux valeurs sont autorisées: 0 ==> pour un seul stockage et 1===> pour un stockage multiple');
INSERT IGNORE INTO `payment_mode` ( `code`, `libelle`, `payment_group`) VALUES ( 'CASH', 'ESPECE', 0);
INSERT IGNORE INTO `payment_mode` ( `code`, `libelle`, `payment_group`) VALUES ( 'OM', 'ORANGE', 2);
INSERT IGNORE INTO `payment_mode` ( `code`, `libelle`, `payment_group`) VALUES ( 'MTN', 'MTN', 2);
INSERT IGNORE INTO `payment_mode` ( `code`, `libelle`, `payment_group`) VALUES ( 'MOOV', 'MOOV', 2);
INSERT IGNORE INTO `payment_mode` ( `code`, `libelle`, `payment_group`) VALUES ( 'WAVE', 'WAVE', 2);
INSERT IGNORE INTO `payment_mode` ( `code`, `libelle`, `payment_group`) VALUES ( 'CB', 'CARTE BANCAIRE', 3);
INSERT IGNORE INTO `payment_mode` ( `code`, `libelle`, `payment_group`) VALUES ( 'VIREMENT', 'VIREMENT', 4);
INSERT IGNORE INTO `payment_mode` ( `code`, `libelle`, `payment_group`) VALUES ( 'CH', 'CHEQUE', 5);
INSERT IGNORE INTO  rayon (id,code,exclude,libelle,updated_at,storage_id,created_at) VALUES(1,'SANS',0,'SANS EMPLACEMENT',CURRENT_DATE,2,CURRENT_DATE);
INSERT IGNORE INTO  rayon (id,code,exclude,libelle,updated_at,storage_id,created_at) VALUES(2,'SANS',0,'SANS EMPLACEMENT',CURRENT_DATE,1,CURRENT_DATE);
INSERT IGNORE INTO  rayon (id,code,exclude,libelle,updated_at,storage_id,created_at) VALUES(3,'SANS',0,'SANS EMPLACEMENT',CURRENT_DATE,3,CURRENT_DATE);
INSERT IGNORE INTO `app_configuration` (`name`,`value`,`description`) VALUES ('APP_QTY_MAX','9999999','Quantité maximale à vendre ');
INSERT
IGNORE
INTO app_configuration(name, description, value, other_value)
values ('APP_CASH_FUND', 'Ouverture automatique de la caisse du caissier', '0', null);
INSERT
IGNORE
INTO app_configuration(name, description, value, other_value)
values ('APP_SANS_NUM_BON', 'Autorisation de vente sans numéro de bon', '0', null);
INSERT
IGNORE
INTO app_configuration(name, description, value, other_value)
values ('APP_ENTREE_STOCK_SANS_EXPIRY_DATE',
        'Autorisation entrée stock sans control date péremption', '0', null);
INSERT
IGNORE
INTO warehouse_sequence(`name`, `increment`, `seq_value`)
VALUES ('ENTREE_STOCK', 1, 1);

INSERT IGNORE INTO app_configuration(name, description, value, other_value)
values ('APP_DAY_STOCK',
        'Nombre de jours par stock', '10', null);
INSERT IGNORE INTO app_configuration(name, description, value, other_value)
values ('APP_LAST_DAY_REAPPRO',
        'Dernière date de mise à jour des seuils de réappro', null, null);

INSERT IGNORE INTO app_configuration(name, description, value, other_value)
values ('APP_LIMIT_NBR_DAY_REAPPRO',
        'Nombre de jour de delai de réapprovisionnement', '8', null);
INSERT IGNORE INTO app_configuration(name, description, value, other_value)
values ('APP_DENOMINATEUR_REAPPRO',
        'denominateur du calcul de réappro', '84', null);

