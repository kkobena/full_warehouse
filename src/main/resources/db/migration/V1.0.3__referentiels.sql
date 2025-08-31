INSERT INTO magasin (id, address, full_name, name, note, phone, registre, type_magasin)
VALUES (1, '85 boulevard de l Europe, 69310', 'Pharma Smart', 'Pharma Smart', 'Bienvenue !',
        '+33652926383', '',
        'OFFICINE') ON CONFLICT (id) DO NOTHING;

INSERT INTO authority (name, libelle)
VALUES ('ROLE_ADMIN', 'Administrateur') ON CONFLICT (name) DO NOTHING;
INSERT INTO authority (name, libelle)
VALUES ('ROLE_USER', 'Utilisateur') ON CONFLICT (name) DO NOTHING;

INSERT INTO app_user (id, login, password_hash, first_name, last_name, email, image_url,
                         activated, lang_key, activation_key, reset_key, created_by, created_date,
                         reset_date, last_modified_by, last_modified_date, magasin_id)
VALUES (1, 'system', '$2a$10$mE.qmcV0mFU5NcKh73TZx.z4ueI/.bDWbj0T1BYyqP481kGGarKLG', 'System',
        'System', 'system@localhost', '', true, 'fr', null, null, 'system', null, null, 'system',
        null, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO app_user (id, login, password_hash, first_name, last_name, email, image_url,
                         activated, lang_key, activation_key, reset_key, created_by, created_date,
                         reset_date, last_modified_by, last_modified_date, magasin_id)
VALUES (2, 'anonymoususer', '$2a$10$j8S5d7Sr7.8VTOYNviDPOeWX8KcYILUVJBsYV83Y5NtECayypx9lO',
        'Anonymous', 'User', 'anonymous@localhost', '', true, 'fr', null, null, 'system', null,
        null, 'system', null, 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO app_user (id, login, password_hash, first_name, last_name, email, image_url,
                         activated, lang_key, activation_key, reset_key, created_by, created_date,
                         reset_date, last_modified_by, last_modified_date, magasin_id)
VALUES (3, 'admin', '$2a$10$gSAhZrxMllrbgj/kkK9UceBPpChGWJA7SYIb1Mqo.n5aNLq1/oRrC', 'Administrator',
        'Administrator', 'admin@localhost', '', true, 'fr', null, null, 'system', null, null,
        'system', null, 1) ON CONFLICT (id) DO NOTHING;

INSERT INTO user_authority (user_id, authority_name)
VALUES (1, 'ROLE_ADMIN') ON CONFLICT (user_id, authority_name) DO NOTHING;
INSERT INTO user_authority (user_id, authority_name)
VALUES (1, 'ROLE_USER') ON CONFLICT (user_id, authority_name) DO NOTHING;
INSERT INTO user_authority (user_id, authority_name)
VALUES (3, 'ROLE_ADMIN') ON CONFLICT (user_id, authority_name) DO NOTHING;
INSERT INTO user_authority (user_id, authority_name)
VALUES (3, 'ROLE_USER') ON CONFLICT (user_id, authority_name) DO NOTHING;


INSERT INTO storage("id", "name", "storage_type", "magasin_id")
VALUES (1, 'ENTREPOT', 'PRINCIPAL', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO storage("id", "name", "storage_type", "magasin_id")
VALUES (2, 'POINT DE VENTE', 'POINT_DE_VENTE', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO storage("id", "name", "storage_type", "magasin_id")
VALUES (3, 'RESERVE', 'SAFETY_STOCK', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO tva (id, taux)
VALUES (1, 0),
       (2, 18),
       (3, 9) ON CONFLICT (id) DO NOTHING;
INSERT INTO categorie(id, code, libelle)
VALUES (1, '01', 'Grand Public'),
       (2, '02', 'Dietetique'),
       (3, '10', 'Specialité'),
       (4, '30', 'Accessoires'),
       (5, '40', 'Veterinaires'),
       (6, '20', 'Parfumerie') ON CONFLICT (id) DO NOTHING;

INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150002, '1050', 'MEDICAMENTS FRANCE', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150003, '1041', 'H', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150004, '3020', 'PRODUITS CLARINS', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150005, '3040', 'PRODUITS BUCCO-DENTAIRES', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150006, '1060', 'PRODUITS CANCERO ROCHE', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150007, '8010', 'OSTEO-SYNTHESE', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150008, '6002', 'PIECES MONNAIE MESURE', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150009, '1070', 'PRODUITS CANCERO AVENTIS', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150010, '1080', 'MEDICAMENTS MARGE REDUITE', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150011, '1000', 'SPECIALITES PUBLIQUES', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150012, '1010', 'SPECIALITES HOPITAL', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150013, '1020', 'DECONDITIONNES', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150014, '1030', 'GENERIQUES', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150015, '1040', 'HOMEOPATHIE', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150016, '1900', 'PART 1/3 PAYANT', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150017, '2000', 'VETERINAIRES', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150018, '3000', 'PARFUMERIE LOCALE N1', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150019, '3010', 'PARFUMERIE LOCALE N2', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150020, '4000', 'ORTHOPEDIE', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150021, '5000', 'LAITS/ FARINES/ DIETETIQUE INF.', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150022, '7000', 'CHIMIQUES', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150023, '6000', 'DIETETIQUE ADULTE', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150024, '8000', 'ACCESSOIRES', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150025, '9000', 'DIVERS', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO famille_produit (id, code, libelle, categorie_id)
VALUES (150026, '3030', 'PARFUMERIE FRANCE', 6) ON CONFLICT (id) DO NOTHING;

INSERT INTO groupe_fournisseur (id, libelle, odre)
VALUES (1, 'LABOREX-CI', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO groupe_fournisseur (id, libelle, odre)
VALUES (2, 'DPCI', 2) ON CONFLICT (id) DO NOTHING;
INSERT INTO groupe_fournisseur (id, libelle, odre)
VALUES (3, 'COPHARMED', 3) ON CONFLICT (id) DO NOTHING;
INSERT INTO groupe_fournisseur (id, libelle, odre)
VALUES (4, 'TEDIS PHAR.', 4) ON CONFLICT (id) DO NOTHING;
INSERT INTO groupe_fournisseur (id, libelle, odre)
VALUES (5, 'AUTRES', 100) ON CONFLICT (id) DO NOTHING;

INSERT INTO form_produit(id, libelle)
VALUES (1, 'Comprimés'),
       (2, 'Sachets'),
       (3, 'Gélules'),
       (4, 'Ampoules'),
       (5, 'Flacons'),
       (6, 'Poudres'),
       (7, 'Crèmes'),
       (8, 'Gels'),
       (9, 'Sprays'),
       (10, 'Solutions'),
       (11, 'Pommades'),
       (12, 'Suppositoires'),
       (13, 'Inhalateurs'),
       (14, 'Patchs'),
       (15, 'Injectables'),
       (16, 'Collyres'),
       (17, 'Onguents'),
       (18, 'Granulés'),
       (19, 'Sérums')ON CONFLICT (id) DO NOTHING;

INSERT INTO app_configuration ("name", "value", "description", "value_type")
VALUES ('APP_GESTION_STOCK', '0',
        'Ce paramètre détermine le mode de stockage utilisé. <br>0 : stockage unique (un seul emplacement).<br> 1 : stockage multiple (plusieurs emplacements possibles)',
        'BOOLEAN') ON CONFLICT (name) DO NOTHING;
INSERT INTO payment_mode ("code", "libelle", "payment_group", "ordre_tri", "enable")
VALUES ('CASH', 'ESPECE', 'CASH', 1, true) ON CONFLICT (code) DO NOTHING;
INSERT INTO payment_mode ("code", "libelle", "payment_group", "ordre_tri", "enable")
VALUES ('OM', 'ORANGE', 'MOBILE', 2, true) ON CONFLICT (code) DO NOTHING;
INSERT INTO payment_mode ("code", "libelle", "payment_group", "ordre_tri", "enable")
VALUES ('MTN', 'MTN', 'MOBILE', 3, true) ON CONFLICT (code) DO NOTHING;
INSERT INTO payment_mode ("code", "libelle", "payment_group", "ordre_tri", "enable")
VALUES ('MOOV', 'MOOV', 'MOBILE', 4, true) ON CONFLICT (code) DO NOTHING;
INSERT INTO payment_mode ("code", "libelle", "payment_group", "ordre_tri", "enable")
VALUES ('WAVE', 'WAVE', 'MOBILE', 5, true) ON CONFLICT (code) DO NOTHING;
INSERT INTO payment_mode ("code", "libelle", "payment_group", "ordre_tri", "enable")
VALUES ('CB', 'CARTE BANCAIRE', 'CB', 6, true) ON CONFLICT (code) DO NOTHING;
INSERT INTO payment_mode ("code", "libelle", "payment_group", "ordre_tri", "enable")
VALUES ('VIREMENT', 'VIREMENT', 'VIREMENT', 7, true) ON CONFLICT (code) DO NOTHING;
INSERT INTO payment_mode ("code", "libelle", "payment_group", "ordre_tri", "enable")
VALUES ('CH', 'CHEQUE', 'CHEQUE', 8, true) ON CONFLICT (code) DO NOTHING;
INSERT INTO rayon (id, code, to_exclude, libelle, storage_id)
VALUES (1, 'SANS', false, 'SANS EMPLACEMENT', 2) ON CONFLICT (id) DO NOTHING;
INSERT INTO rayon (id, code, to_exclude, libelle, storage_id)
VALUES (2, 'SANS', false, 'SANS EMPLACEMENT', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO rayon (id, code, to_exclude, libelle, storage_id)
VALUES (3, 'SANS', false, 'SANS EMPLACEMENT', 3) ON CONFLICT (id) DO NOTHING;
INSERT INTO app_configuration ("name", "value", "description", "value_type")
VALUES ('APP_QTY_MAX', '999', 'Quantité maximale à vendre ', 'NUMBER') ON CONFLICT (name) DO NOTHING;
INSERT
INTO app_configuration(name, description, value, other_value, "value_type")
values ('APP_CASH_FUND', 'Ouverture automatique de la caisse du caissier', '0', null, 'BOOLEAN') ON CONFLICT (name) DO NOTHING;
INSERT
INTO app_configuration(name, description, value, other_value, "value_type")
values ('APP_SANS_NUM_BON', 'Autorisation de vente sans numéro de bon', '0', null, 'BOOLEAN') ON CONFLICT (name) DO NOTHING;
INSERT
INTO app_configuration(name, description, value, other_value, "value_type")
values ('APP_ENTREE_STOCK_SANS_EXPIRY_DATE',
        'Autorisation entrée stock sans control date péremption', '0', null, 'BOOLEAN') ON CONFLICT (name) DO NOTHING;
INSERT
INTO warehouse_sequence("name", "increment", "seq_value")
VALUES ('ENTREE_STOCK', 1, 1) ON CONFLICT (name) DO NOTHING;

INSERT INTO app_configuration(name, description, value, other_value, "value_type")
values ('APP_DAY_STOCK',
        'Nombre de jours par stock', '10', null, 'NUMBER') ON CONFLICT (name) DO NOTHING;
INSERT INTO app_configuration(name, description, value, other_value, "value_type")
values ('APP_LAST_DAY_REAPPRO',
        'Dernière date de mise à jour des seuils de réappro', '2000-01-01', null, 'DATE') ON CONFLICT (name) DO NOTHING;

INSERT INTO app_configuration(name, description, value, other_value, "value_type")
values ('APP_LIMIT_NBR_DAY_REAPPRO',
        'Nombre de jour de delai de réapprovisionnement', '8', null, 'NUMBER') ON CONFLICT (name) DO NOTHING;
INSERT INTO app_configuration(name, description, value, other_value, "value_type")
values ('APP_DENOMINATEUR_REAPPRO',
        'denominateur du calcul de réappro', '84', null, 'NUMBER') ON CONFLICT (name) DO NOTHING;
INSERT INTO app_configuration(name, description, value, other_value, "value_type")
values ('APP_GESTION_LOT',
        'Votre pharmacie gère les lots', '0', null, 'BOOLEAN') ON CONFLICT (name) DO NOTHING;
INSERT INTO app_configuration(name, description, value, other_value, "value_type")
values ('APP_RESET_INVOICE_NUMBER',
        'Votre pharmacie préfixe les numéro de facture par l''année en cours', '0', null,
        'BOOLEAN') ON CONFLICT (name) DO NOTHING;

INSERT INTO app_configuration(name, description, value, other_value, "value_type")
values ('APP_SUGGESTION_RETENTION',
        'Nombre de jours de conservation des suggestions dans votre pharamcie', '90', null,
        'NUMBER') ON CONFLICT (name) DO NOTHING;

INSERT INTO app_configuration(name, description, value, other_value, "value_type")
values ('APP_NOMBRE_JOUR_AVANT_PEREMPTION',
        'Nombre de jours restants avant la date de péremption d''un produit pour sa mise en vente',
        '90', null,
        'NUMBER') ON CONFLICT (name) DO NOTHING;

INSERT INTO app_configuration(name, description, value, other_value, "value_type")
values ('APP_EXPIRY_ALERT_DAYS_BEFORE',
        'Seuil de déclenchement d’alerte avant la date de péremption',
        '30,7', null,
        'LIST') ON CONFLICT (name) DO NOTHING;

INSERT INTO authority (name, libelle)
VALUES ('ROLE_VENDEUR', 'Vendeur') ON CONFLICT (name) DO NOTHING;
INSERT INTO authority (name, libelle)
VALUES ('ROLE_CAISSIER', 'Caissier') ON CONFLICT (name) DO NOTHING;
INSERT INTO authority (name, libelle)
VALUES ('ROLE_RESPONSABLE_COMMANDE', 'Responsable de commande') ON CONFLICT (name) DO NOTHING;
