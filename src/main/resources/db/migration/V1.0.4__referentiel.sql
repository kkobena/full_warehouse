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
