/*!40101 SET @OLD_CHARACTER_SET_CLIENT = @@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE = @@TIME_ZONE */;
/*!40103 SET TIME_ZONE = '+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS = 0 */;
/*!40101 SET @OLD_SQL_MODE = @@SQL_MODE, SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES = @@SQL_NOTES, SQL_NOTES = 0 */;


-- Listage de la structure de la base pour warehouse
CREATE DATABASE IF NOT EXISTS `warehouse` /*!40100 DEFAULT CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci */;
USE `warehouse`;

-- Listage de la structure de la table warehouse. ajust
CREATE TABLE IF NOT EXISTS `ajust`
(
    `id`                bigint(20)  NOT NULL AUTO_INCREMENT,
    `commentaire`       varchar(255) DEFAULT NULL,
    `date_mtv`          datetime(6) NOT NULL,
    `statut`            tinyint(4)  NOT NULL CHECK (`statut` between 0 and 1),
    `calendar_work_day` date        NOT NULL,
    `storage_id`        bigint(20)  NOT NULL,
    `user_id`           bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FKk2bd9m95mgcisluhfs7txg6gv` (`calendar_work_day`),
    KEY `FKbkc3o0mx6q4799rpbpgcd0e89` (`storage_id`),
    KEY `FKg4b7lwwiqkfq8ln82tbslcthu` (`user_id`),
    CONSTRAINT `FKbkc3o0mx6q4799rpbpgcd0e89` FOREIGN KEY (`storage_id`) REFERENCES `storage` (`id`),
    CONSTRAINT `FKg4b7lwwiqkfq8ln82tbslcthu` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `FKk2bd9m95mgcisluhfs7txg6gv` FOREIGN KEY (`calendar_work_day`) REFERENCES `warehouse_calendar` (`work_day`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.ajust : ~0 rows (environ)

-- Listage de la structure de la table warehouse. ajustement
CREATE TABLE IF NOT EXISTS `ajustement`
(
    `id`                  bigint(20)  NOT NULL AUTO_INCREMENT,
    `date_mtv`            datetime(6) NOT NULL,
    `qty_mvt`             int(11)     NOT NULL,
    `stock_after`         int(11)     NOT NULL,
    `stock_before`        int(11)     NOT NULL,
    `type_ajust`          tinyint(4)  NOT NULL CHECK (`type_ajust` between 0 and 1),
    `ajust_id`            bigint(20)  NOT NULL,
    `motif_ajustement_id` bigint(20) DEFAULT NULL,
    `produit_id`          bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKpl3ledfcmf6v758vp99whh00j` (`ajust_id`, `produit_id`),
    KEY `FKpstvc4bcssojfv5absxm5vtba` (`motif_ajustement_id`),
    KEY `FKgem9o7c8roq5g1lb555j4hnlh` (`produit_id`),
    CONSTRAINT `FKex9228479j8udon2wd8gm5dot` FOREIGN KEY (`ajust_id`) REFERENCES `ajust` (`id`),
    CONSTRAINT `FKgem9o7c8roq5g1lb555j4hnlh` FOREIGN KEY (`produit_id`) REFERENCES `produit` (`id`),
    CONSTRAINT `FKpstvc4bcssojfv5absxm5vtba` FOREIGN KEY (`motif_ajustement_id`) REFERENCES `motif_ajustement` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.ajustement : ~0 rows (environ)

-- Listage de la structure de la table warehouse. app_configuration
CREATE TABLE IF NOT EXISTS `app_configuration`
(
    `name`            varchar(50)                                                                              NOT NULL,
    `created`         datetime(6)  DEFAULT NULL,
    `description`     varchar(255)                                                                             NOT NULL,
    `other_value`     varchar(255) DEFAULT NULL,
    `updated`         datetime(6)  DEFAULT NULL,
    `value`           varchar(255)                                                                             NOT NULL,
    `value_type`      enum ('BOOLEAN','COLOR','DATE','DATE_TIME','FONT','LIST','MAP','NUMBER','STRING','TIME') NOT NULL,
    `validated_by_id` bigint(20)   DEFAULT NULL,
    PRIMARY KEY (`name`),
    KEY `FK5jspq15xojfrkynt2gwtgys1e` (`validated_by_id`),
    CONSTRAINT `FK5jspq15xojfrkynt2gwtgys1e` FOREIGN KEY (`validated_by_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.app_configuration : ~0 rows (environ)

-- Listage de la structure de la table warehouse. authority
CREATE TABLE IF NOT EXISTS `authority`
(
    `name`    varchar(50) NOT NULL,
    `libelle` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.authority : ~0 rows (environ)

-- Listage de la structure de la table warehouse. authority_menu
CREATE TABLE IF NOT EXISTS `authority_menu`
(
    `authority_name` varchar(50) NOT NULL,
    `menu_id`        bigint(20)  NOT NULL,
    PRIMARY KEY (`authority_name`, `menu_id`),
    KEY `FKldi65w2wsgdge8qfsjkmge2sl` (`menu_id`),
    CONSTRAINT `FKldi65w2wsgdge8qfsjkmge2sl` FOREIGN KEY (`menu_id`) REFERENCES `menu` (`id`),
    CONSTRAINT `FKrfrj2p0bpnosephpj61lmi9np` FOREIGN KEY (`authority_name`) REFERENCES `authority` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.authority_menu : ~0 rows (environ)

-- Listage de la structure de la table warehouse. authority_privilege
CREATE TABLE IF NOT EXISTS `authority_privilege`
(
    `id`             bigint(20)   NOT NULL AUTO_INCREMENT,
    `authority_name` varchar(50)  NOT NULL,
    `privilege_name` varchar(100) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKpuc964qxatpndyv71vc7pds1f` (`privilege_name`, `authority_name`),
    KEY `FK9prep679f0xewrgodv1lqstl7` (`authority_name`),
    CONSTRAINT `FK5xavalsq2c8nngb47jbgsl7mj` FOREIGN KEY (`privilege_name`) REFERENCES `privilege` (`name`),
    CONSTRAINT `FK9prep679f0xewrgodv1lqstl7` FOREIGN KEY (`authority_name`) REFERENCES `authority` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.authority_privilege : ~0 rows (environ)

-- Listage de la structure de la table warehouse. avoir
CREATE TABLE IF NOT EXISTS `avoir`
(
    `id`                bigint(20)  NOT NULL AUTO_INCREMENT,
    `date_mtv`          datetime(6) NOT NULL,
    `statut`            tinyint(4)  NOT NULL CHECK (`statut` between 0 and 1),
    `calendar_work_day` date        NOT NULL,
    `user_id`           bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FK6m25r6qf4v663pks1s8t7hetv` (`calendar_work_day`),
    KEY `FKptiih1fwc5n5cafgx8txm93yo` (`user_id`),
    CONSTRAINT `FK6m25r6qf4v663pks1s8t7hetv` FOREIGN KEY (`calendar_work_day`) REFERENCES `warehouse_calendar` (`work_day`),
    CONSTRAINT `FKptiih1fwc5n5cafgx8txm93yo` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.avoir : ~0 rows (environ)

-- Listage de la structure de la table warehouse. banque
CREATE TABLE IF NOT EXISTS `banque`
(
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT,
    `adresse`      varchar(255) DEFAULT NULL,
    `beneficiaire` varchar(255) DEFAULT NULL,
    `code`         varchar(100) DEFAULT NULL,
    `nom`          varchar(100) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.banque : ~0 rows (environ)

-- Listage de la structure de la table warehouse. cash_fund
CREATE TABLE IF NOT EXISTS `cash_fund`
(
    `id`               bigint(20)                                NOT NULL AUTO_INCREMENT,
    `amount`           int(11)                                   NOT NULL,
    `cash_fund_type`   enum ('AUTO','MANUAL')                    NOT NULL,
    `created`          datetime(6)                               NOT NULL,
    `statut`           enum ('PENDING','PROCESSING','VALIDETED') NOT NULL,
    `updated`          datetime(6) DEFAULT NULL,
    `cash_register_id` bigint(20)  DEFAULT NULL,
    `user_id`          bigint(20)                                NOT NULL,
    `validated_by_id`  bigint(20)  DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKkw6trpuogcthcnuixl416p6tc` (`cash_register_id`),
    KEY `FKe785pgxlc1vu3g47la9g7i8h1` (`user_id`),
    KEY `FK8dpju4dcbnn85y6nuut81k4fm` (`validated_by_id`),
    CONSTRAINT `FK8dpju4dcbnn85y6nuut81k4fm` FOREIGN KEY (`validated_by_id`) REFERENCES `user` (`id`),
    CONSTRAINT `FKe785pgxlc1vu3g47la9g7i8h1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `FKs2oiwqxkralpsy81kk2afnj60` FOREIGN KEY (`cash_register_id`) REFERENCES `cash_register` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.cash_fund : ~0 rows (environ)

-- Listage de la structure de la table warehouse. cash_register
CREATE TABLE IF NOT EXISTS `cash_register`
(
    `id`              bigint(20)                                   NOT NULL AUTO_INCREMENT,
    `begin_time`      datetime(6)                                  NOT NULL,
    `cancele_amount`  int(11)     DEFAULT NULL,
    `created`         datetime(6)                                  NOT NULL,
    `end_time`        datetime(6) DEFAULT NULL,
    `final_amount`    bigint(20)  DEFAULT NULL,
    `init_amount`     bigint(20)                                   NOT NULL,
    `statut`          enum ('CLOSED','OPEN','PENDING','VALIDATED') NOT NULL,
    `updated`         datetime(6)                                  NOT NULL,
    `updated_user_id` bigint(20)  DEFAULT NULL,
    `user_id`         bigint(20)                                   NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FK29gjscipf2nh9alyb3e9jia0q` (`updated_user_id`),
    KEY `FKdv6da1nxa2xkrlk3rkf807s6a` (`user_id`),
    CONSTRAINT `FK29gjscipf2nh9alyb3e9jia0q` FOREIGN KEY (`updated_user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `FKdv6da1nxa2xkrlk3rkf807s6a` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.cash_register : ~0 rows (environ)

-- Listage de la structure de la table warehouse. cash_register_item
CREATE TABLE IF NOT EXISTS `cash_register_item`
(
    `id`                bigint(20)  NOT NULL AUTO_INCREMENT,
    `amount`            bigint(20) DEFAULT NULL,
    `type_transaction`  tinyint(4)  NOT NULL CHECK (`type_transaction` between 0 and 9),
    `cash_register_id`  bigint(20)  NOT NULL,
    `payment_mode_code` varchar(50) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKovfm6oebekfa7449p67yvnnpf` (`cash_register_id`, `payment_mode_code`,
                                              `type_transaction`),
    KEY `FK27y8m06p8783unjcod1i5p97x` (`payment_mode_code`),
    CONSTRAINT `FK27y8m06p8783unjcod1i5p97x` FOREIGN KEY (`payment_mode_code`) REFERENCES `payment_mode` (`code`),
    CONSTRAINT `FKg7uv0e0n4k52n11ryawhjueef` FOREIGN KEY (`cash_register_id`) REFERENCES `cash_register` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.cash_register_item : ~0 rows (environ)

-- Listage de la structure de la table warehouse. categorie
CREATE TABLE IF NOT EXISTS `categorie`
(
    `id`      bigint(20)   NOT NULL AUTO_INCREMENT,
    `code`    varchar(255) DEFAULT NULL,
    `libelle` varchar(255) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK201klrwuww0os41kte46ac6lq` (`libelle`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.categorie : ~0 rows (environ)

-- Listage de la structure de la table warehouse. client_tiers_payant
CREATE TABLE IF NOT EXISTS `client_tiers_payant`
(
    `id`                  bigint(20)   NOT NULL AUTO_INCREMENT,
    `conso_mensuelle`     bigint(20)                                         DEFAULT NULL,
    `consommation_json`   longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`consommation_json`)),
    `created`             datetime(6)  NOT NULL,
    `num`                 varchar(100) NOT NULL,
    `plafond_absolu`      bit(1)                                             DEFAULT NULL,
    `plafond_conso`       bigint(20)                                         DEFAULT NULL,
    `plafond_journalier`  bigint(20)                                         DEFAULT NULL,
    `priorite`            tinyint(4)   NOT NULL CHECK (`priorite` between 0 and 3),
    `statut`              tinyint(4)   NOT NULL CHECK (`statut` between 0 and 2),
    `taux`                int(11)      NOT NULL,
    `updated`             datetime(6)  NOT NULL,
    `assured_customer_id` bigint(20)   NOT NULL,
    `tiers_payant_id`     bigint(20)   NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKmesf3j3moh06oir9mq5fxmmb9` (`tiers_payant_id`, `assured_customer_id`),
    UNIQUE KEY `UK2vls4uc89y3g75n6qnudmn0qs` (`tiers_payant_id`, `num`),
    KEY `FKb627stp58elnn5yel8hi27tju` (`assured_customer_id`),
    CONSTRAINT `FKb627stp58elnn5yel8hi27tju` FOREIGN KEY (`assured_customer_id`) REFERENCES `customer` (`id`),
    CONSTRAINT `FKp7d63xly6qydc32rbc5hwoxst` FOREIGN KEY (`tiers_payant_id`) REFERENCES `tiers_payant` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.client_tiers_payant : ~0 rows (environ)

-- Listage de la structure de la table warehouse. commande
CREATE TABLE IF NOT EXISTS `commande`
(
    `id`                bigint(20)  NOT NULL AUTO_INCREMENT,
    `created_at`        datetime(6) NOT NULL,
    `discount_amount`   int(11)                                            DEFAULT 0,
    `gross_amount`      int(11)     NOT NULL,
    `lots`              longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`lots`)),
    `net_amount`        int(11)                                            DEFAULT 0,
    `order_amount`      int(11)     NOT NULL,
    `order_refernce`    varchar(255)                                       DEFAULT NULL,
    `order_status`      tinyint(4)  NOT NULL CHECK (`order_status` between 0 and 6),
    `receipt_amount`    int(11)                                            DEFAULT NULL,
    `receipt_date`      date                                               DEFAULT NULL,
    `receipt_refernce`  varchar(255)                                       DEFAULT NULL,
    `sequence_bon`      varchar(255)                                       DEFAULT NULL,
    `tax_amount`        int(11)                                            DEFAULT 0,
    `type_suggession`   tinyint(4)                                         DEFAULT NULL CHECK (`type_suggession` between 0 and 1),
    `updated_at`        datetime(6) NOT NULL,
    `calendar_work_day` date        NOT NULL,
    `fournisseur_id`    bigint(20)  NOT NULL,
    `last_user_edit_id` bigint(20)  NOT NULL,
    `magasin_id`        bigint(20)  NOT NULL,
    `user_id`           bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FK75p00n32p02ajl5fi1bj8uilo` (`calendar_work_day`),
    KEY `FKe9u9pamnss31e4pn6twt1yk0q` (`fournisseur_id`),
    KEY `FK5eo0p8etrff7n4i6m5p3vkx0m` (`last_user_edit_id`),
    KEY `FK26t90jdsvtpx5gjurwblscma0` (`magasin_id`),
    KEY `FKp5deswt3amtfx764raq42rw2o` (`user_id`),
    CONSTRAINT `FK26t90jdsvtpx5gjurwblscma0` FOREIGN KEY (`magasin_id`) REFERENCES `magasin` (`id`),
    CONSTRAINT `FK5eo0p8etrff7n4i6m5p3vkx0m` FOREIGN KEY (`last_user_edit_id`) REFERENCES `user` (`id`),
    CONSTRAINT `FK75p00n32p02ajl5fi1bj8uilo` FOREIGN KEY (`calendar_work_day`) REFERENCES `warehouse_calendar` (`work_day`),
    CONSTRAINT `FKe9u9pamnss31e4pn6twt1yk0q` FOREIGN KEY (`fournisseur_id`) REFERENCES `fournisseur` (`id`),
    CONSTRAINT `FKp5deswt3amtfx764raq42rw2o` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.commande : ~0 rows (environ)

-- Listage de la structure de la table warehouse. customer
CREATE TABLE IF NOT EXISTS `customer`
(
    `dtype`               varchar(31)                      NOT NULL,
    `id`                  bigint(20)                       NOT NULL AUTO_INCREMENT,
    `code`                varchar(255)                     NOT NULL,
    `created_at`          datetime(6)                      NOT NULL,
    `email`               varchar(255) DEFAULT NULL,
    `first_name`          varchar(255)                     NOT NULL,
    `last_name`           varchar(255)                     NOT NULL,
    `phone`               varchar(255) DEFAULT NULL,
    `status`              tinyint(4)                       NOT NULL CHECK (`status` between 0 and 3),
    `type_assure`         enum ('AYANT_DROIT','PRINCIPAL') NOT NULL,
    `updated_at`          datetime(6)                      NOT NULL,
    `dat_naiss`           date         DEFAULT NULL,
    `num_ayant_droit`     varchar(100) DEFAULT NULL,
    `sexe`                varchar(255) DEFAULT NULL,
    `caution`             int(11)      DEFAULT NULL,
    `assure_principal_id` bigint(20)   DEFAULT NULL,
    `remise_id`           bigint(20)   DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKrm1bp9bhtiih5foj17t8l500j` (`code`),
    KEY `FKb1cv4y9rq1gego4buaykin5wh` (`assure_principal_id`),
    KEY `FK164wkcenb578dvt81hbag6dvp` (`remise_id`),
    CONSTRAINT `FK164wkcenb578dvt81hbag6dvp` FOREIGN KEY (`remise_id`) REFERENCES `remise` (`id`),
    CONSTRAINT `FKb1cv4y9rq1gego4buaykin5wh` FOREIGN KEY (`assure_principal_id`) REFERENCES `customer` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.customer : ~0 rows (environ)

-- Listage de la structure de la table warehouse. dci
CREATE TABLE IF NOT EXISTS `dci`
(
    `id`      bigint(20)   NOT NULL AUTO_INCREMENT,
    `code`    varchar(20)  NOT NULL,
    `libelle` varchar(255) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKe3c6g8lyasluveuhuckir2lod` (`code`),
    KEY `dci_libelle_index` (`libelle`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.dci : ~0 rows (environ)

-- Listage de la structure de la table warehouse. decondition
CREATE TABLE IF NOT EXISTS `decondition`
(
    `id`                     bigint(20)  NOT NULL AUTO_INCREMENT,
    `date_mtv`               datetime(6) NOT NULL,
    `qty_mvt`                int(11)     NOT NULL,
    `stock_after`            int(11)     NOT NULL,
    `stock_before`           int(11)     NOT NULL,
    `type_deconditionnement` tinyint(4)  NOT NULL CHECK (`type_deconditionnement` between 0 and 1),
    `calendar_work_day`      date        NOT NULL,
    `produit_id`             bigint(20)  NOT NULL,
    `user_id`                bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FKky48vqfjbbchwrwcnl6hnoei5` (`calendar_work_day`),
    KEY `FKdmoni6sfl6sh38ej7gngqhg6y` (`produit_id`),
    KEY `FKmele8h9p7330pu1kk6nujwk3q` (`user_id`),
    CONSTRAINT `FKdmoni6sfl6sh38ej7gngqhg6y` FOREIGN KEY (`produit_id`) REFERENCES `produit` (`id`),
    CONSTRAINT `FKky48vqfjbbchwrwcnl6hnoei5` FOREIGN KEY (`calendar_work_day`) REFERENCES `warehouse_calendar` (`work_day`),
    CONSTRAINT `FKmele8h9p7330pu1kk6nujwk3q` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.decondition : ~0 rows (environ)

-- Listage de la structure de la table warehouse. delivery_receipt
CREATE TABLE IF NOT EXISTS `delivery_receipt`
(
    `id`                 bigint(20)                        NOT NULL AUTO_INCREMENT,
    `created_date`       datetime(6)                       NOT NULL,
    `discount_amount`    int(11)      DEFAULT 0,
    `modified_date`      datetime(6)  DEFAULT NULL,
    `net_amount`         int(11)      DEFAULT 0,
    `number_transaction` varchar(255) DEFAULT NULL,
    `order_reference`    varchar(255) DEFAULT NULL,
    `paiment_status`     enum ('NOT_SOLD','PAID','UNPAID') NOT NULL,
    `receipt_amount`     int(11)      DEFAULT NULL,
    `receipt_date`       date                              NOT NULL,
    `receipt_refernce`   varchar(255) DEFAULT NULL,
    `receipt_status`     enum ('ANY','CLOSE','PENDING')    NOT NULL,
    `sequence_bon`       varchar(255) DEFAULT NULL,
    `tax_amount`         int(11)      DEFAULT 0,
    `receipt_type`       tinyint(4)                        NOT NULL CHECK (`receipt_type` between 0 and 1),
    `calendar_work_day`  date                              NOT NULL,
    `created_user_id`    bigint(20)                        NOT NULL,
    `fournisseur_id`     bigint(20)                        NOT NULL,
    `modified_user_id`   bigint(20)                        NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKgvydy4ot9g040hfo81yal2ayd` (`number_transaction`),
    UNIQUE KEY `UKrujm3jga75vvkajs98nfmvpj0` (`receipt_refernce`, `fournisseur_id`),
    KEY `receipt_date_index` (`receipt_date`),
    KEY `receipt_status_index` (`receipt_status`),
    KEY `receipt_paiment_status_index` (`paiment_status`),
    KEY `receipt_refernce_index` (`receipt_refernce`),
    KEY `number_transaction_index` (`number_transaction`),
    KEY `FK2jc0ra32yybd6r4iej7wk7pb1` (`calendar_work_day`),
    KEY `FKgag79pbmk0offb9m5o6oecnpv` (`created_user_id`),
    KEY `FKkqd2u5k3n47pll89yjockkakf` (`fournisseur_id`),
    KEY `FKi6f5vhbafbeqku3pjoi2x2t15` (`modified_user_id`),
    CONSTRAINT `FK2jc0ra32yybd6r4iej7wk7pb1` FOREIGN KEY (`calendar_work_day`) REFERENCES `warehouse_calendar` (`work_day`),
    CONSTRAINT `FKgag79pbmk0offb9m5o6oecnpv` FOREIGN KEY (`created_user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `FKi6f5vhbafbeqku3pjoi2x2t15` FOREIGN KEY (`modified_user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `FKkqd2u5k3n47pll89yjockkakf` FOREIGN KEY (`fournisseur_id`) REFERENCES `fournisseur` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.delivery_receipt : ~0 rows (environ)

-- Listage de la structure de la table warehouse. delivery_receipt_item
CREATE TABLE IF NOT EXISTS `delivery_receipt_item`
(
    `id`                     bigint(20)  NOT NULL AUTO_INCREMENT,
    `after_stock`            int(11)              DEFAULT NULL,
    `cost_amount`            int(11)              DEFAULT NULL,
    `created_date`           datetime(6) NOT NULL,
    `date_peremption`        date                 DEFAULT NULL,
    `discount_amount`        int(11)     NOT NULL DEFAULT 0,
    `init_stock`             int(11)              DEFAULT NULL,
    `net_amount`             int(11)              DEFAULT 0,
    `order_cost_amount`      int(11)     NOT NULL DEFAULT 0,
    `order_unit_price`       int(11)     NOT NULL,
    `quantity_received`      int(11)     NOT NULL,
    `quantity_requested`     int(11)     NOT NULL,
    `quantity_returned`      int(11)              DEFAULT NULL,
    `regular_unit_price`     int(11)     NOT NULL DEFAULT 0,
    `tax_amount`             int(11)              DEFAULT 0,
    `tva`                    int(11)              DEFAULT NULL,
    `quantity_ug`            int(11)              DEFAULT 0,
    `is_updated`             bit(1)               DEFAULT NULL,
    `updated_date`           datetime(6)          DEFAULT NULL,
    `delivery_receipt_id`    bigint(20)  NOT NULL,
    `fournisseur_produit_id` bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKkyngy84jy82h7e0vx04a2gavw` (`delivery_receipt_id`, `fournisseur_produit_id`),
    KEY `FK5h5m4lc8l68vkbhotus2yn0lp` (`fournisseur_produit_id`),
    CONSTRAINT `FK5h5m4lc8l68vkbhotus2yn0lp` FOREIGN KEY (`fournisseur_produit_id`) REFERENCES `fournisseur_produit` (`id`),
    CONSTRAINT `FKrdnxn65whsrxoprmdefiw3uop` FOREIGN KEY (`delivery_receipt_id`) REFERENCES `delivery_receipt` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.delivery_receipt_item : ~0 rows (environ)

-- Listage de la structure de la table warehouse. facture_tiers_payant
CREATE TABLE IF NOT EXISTS `facture_tiers_payant`
(
    `id`                             bigint(20)  NOT NULL AUTO_INCREMENT,
    `created`                        datetime(6) NOT NULL,
    `debut_periode`                  date                 DEFAULT NULL,
    `facture_provisoire`             bit(1)      NOT NULL,
    `fin_periode`                    date                 DEFAULT NULL,
    `montant_regle`                  int(11)              DEFAULT 0,
    `num_facture`                    varchar(20) NOT NULL,
    `remise_forfetaire`              bigint(20)           DEFAULT NULL,
    `statut`                         varchar(20) NOT NULL DEFAULT 'NOT_PAID',
    `updated`                        datetime(6)          DEFAULT NULL,
    `groupe_facture_tiers_payant_id` bigint(20)           DEFAULT NULL,
    `groupe_tiers_payant_id`         bigint(20)           DEFAULT NULL,
    `tiers_payant_id`                bigint(20)           DEFAULT NULL,
    `user_id`                        bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKdpie1csuti0gr5k8upvaatx8p` (`num_facture`),
    KEY `num_facture_index` (`num_facture`),
    KEY `FKcfeusk9tdn2w3nkkw8wmyxwge` (`groupe_facture_tiers_payant_id`),
    KEY `FKku32lbuahnuhx7f249jglins9` (`groupe_tiers_payant_id`),
    KEY `FK64mvu1jd0r3m57m1dil45h40p` (`tiers_payant_id`),
    KEY `FK3ew1b9jnhhu9x4myvi6c5f0ji` (`user_id`),
    CONSTRAINT `FK3ew1b9jnhhu9x4myvi6c5f0ji` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `FK64mvu1jd0r3m57m1dil45h40p` FOREIGN KEY (`tiers_payant_id`) REFERENCES `tiers_payant` (`id`),
    CONSTRAINT `FKcfeusk9tdn2w3nkkw8wmyxwge` FOREIGN KEY (`groupe_facture_tiers_payant_id`) REFERENCES `facture_tiers_payant` (`id`),
    CONSTRAINT `FKku32lbuahnuhx7f249jglins9` FOREIGN KEY (`groupe_tiers_payant_id`) REFERENCES `groupe_tiers_payant` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.facture_tiers_payant : ~0 rows (environ)

-- Listage de la structure de la table warehouse. famille_produit
CREATE TABLE IF NOT EXISTS `famille_produit`
(
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT,
    `code`         varchar(255) DEFAULT NULL,
    `libelle`      varchar(255) NOT NULL,
    `categorie_id` bigint(20)   NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK5qnbuviywmlttiympkaceoo4s` (`libelle`),
    KEY `FKkl1kdhwi96mrybwm5hw8sofhv` (`categorie_id`),
    CONSTRAINT `FKkl1kdhwi96mrybwm5hw8sofhv` FOREIGN KEY (`categorie_id`) REFERENCES `categorie` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.famille_produit : ~0 rows (environ)

-- Listage de la structure de la table warehouse. form_produit
CREATE TABLE IF NOT EXISTS `form_produit`
(
    `id`      bigint(20)   NOT NULL AUTO_INCREMENT,
    `libelle` varchar(255) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKdebj7ueu4wsokhi8ptd8emwht` (`libelle`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.form_produit : ~0 rows (environ)

-- Listage de la structure de la table warehouse. fournisseur
CREATE TABLE IF NOT EXISTS `fournisseur`
(
    `id`                      bigint(20)   NOT NULL AUTO_INCREMENT,
    `addresse_postal`         varchar(255) DEFAULT NULL,
    `code`                    varchar(70)  NOT NULL,
    `identifiant_repartiteur` varchar(255) DEFAULT NULL,
    `libelle`                 varchar(255) NOT NULL,
    `mobile`                  varchar(255) DEFAULT NULL,
    `num_faxe`                varchar(255) DEFAULT NULL,
    `phone`                   varchar(255) DEFAULT NULL,
    `site`                    varchar(255) DEFAULT NULL,
    `groupe_fournisseur_id`   bigint(20)   NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK97y5ak5ond8qgcml87p1wpk5e` (`libelle`),
    KEY `FK72cjbo0lulxc0wduppfw9qjyp` (`groupe_fournisseur_id`),
    CONSTRAINT `FK72cjbo0lulxc0wduppfw9qjyp` FOREIGN KEY (`groupe_fournisseur_id`) REFERENCES `groupe_fournisseur` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.fournisseur : ~0 rows (environ)

-- Listage de la structure de la table warehouse. fournisseur_produit
CREATE TABLE IF NOT EXISTS `fournisseur_produit`
(
    `id`                 bigint(20)   NOT NULL AUTO_INCREMENT,
    `created_by`         varchar(50)           DEFAULT NULL,
    `created_date`       datetime(6)           DEFAULT NULL,
    `last_modified_by`   varchar(50)           DEFAULT NULL,
    `last_modified_date` datetime(6)           DEFAULT NULL,
    `code_cip`           varchar(255) NOT NULL,
    `principal`          tinyint(1)   NOT NULL DEFAULT 0,
    `prix_achat`         int(11)      NOT NULL,
    `prix_uni`           int(11)      NOT NULL,
    `fournisseur_id`     bigint(20)   NOT NULL,
    `produit_id`         bigint(20)   NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKe88lr5v2edh60cq97jrtdgx14` (`produit_id`, `fournisseur_id`),
    UNIQUE KEY `UKaqrs8apy7lun5q6bicwmolxrr` (`code_cip`, `fournisseur_id`),
    KEY `code_cip_index` (`code_cip`),
    KEY `principal_index` (`principal`),
    KEY `FK7ovp4hph1va6aiw0hrjb6bo5b` (`fournisseur_id`),
    CONSTRAINT `FK7ovp4hph1va6aiw0hrjb6bo5b` FOREIGN KEY (`fournisseur_id`) REFERENCES `fournisseur` (`id`),
    CONSTRAINT `FKd2gc16hsakliy5idekx467yip` FOREIGN KEY (`produit_id`) REFERENCES `produit` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.fournisseur_produit : ~0 rows (environ)

-- Listage de la structure de la table warehouse. gamme_produit
CREATE TABLE IF NOT EXISTS `gamme_produit`
(
    `id`      bigint(20)   NOT NULL AUTO_INCREMENT,
    `code`    varchar(255) DEFAULT NULL,
    `libelle` varchar(255) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK1r9jeo0jvdg5pjhyvl2gnf2do` (`libelle`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.gamme_produit : ~0 rows (environ)

-- Listage de la structure de la table warehouse. grille_remise
CREATE TABLE IF NOT EXISTS `grille_remise`
(
    `id`                bigint(20)                                                                                                                                                                                        NOT NULL AUTO_INCREMENT,
    `code`              enum ('CODE_12','CODE_13','CODE_14','CODE_15','CODE_16','CODE_17','CODE_18','CODE_19','CODE_20','CODE_21','CODE_22','CODE_23','CODE_24','CODE_25','CODE_26','CODE_27','CODE_28','CODE_29','NONE') NOT NULL,
    `enable`            tinyint(1)                                                                                                                                                                                        NOT NULL DEFAULT 1,
    `remise_value`      float                                                                                                                                                                                             NOT NULL,
    `remise_produit_id` bigint(20)                                                                                                                                                                                        NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `grille_remise_code_un_index` (`code`),
    UNIQUE KEY `remise_produit_id_code_un_index` (`code`, `remise_produit_id`),
    KEY `FK7vy9l2sxkb174hmjojs1k3k1a` (`remise_produit_id`),
    CONSTRAINT `FK7vy9l2sxkb174hmjojs1k3k1a` FOREIGN KEY (`remise_produit_id`) REFERENCES `remise` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.grille_remise : ~0 rows (environ)

-- Listage de la structure de la table warehouse. groupe_fournisseur
CREATE TABLE IF NOT EXISTS `groupe_fournisseur`
(
    `id`             bigint(20)   NOT NULL AUTO_INCREMENT,
    `addresspostale` varchar(255) DEFAULT NULL,
    `email`          varchar(255) DEFAULT NULL,
    `libelle`        varchar(255) NOT NULL,
    `num_faxe`       varchar(255) DEFAULT NULL,
    `odre`           int(11)      NOT NULL,
    `tel`            varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKp0jg383si04bm4we9c61c6mn` (`libelle`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.groupe_fournisseur : ~0 rows (environ)

-- Listage de la structure de la table warehouse. groupe_tiers_payant
CREATE TABLE IF NOT EXISTS `groupe_tiers_payant`
(
    `id`                 bigint(20)   NOT NULL AUTO_INCREMENT,
    `adresse`            varchar(200)                                                                         DEFAULT NULL,
    `name`               varchar(100) NOT NULL,
    `ordre_tris_facture` enum ('CODE_FACTURE','DATE_FACTURE','DATE_FACTURE_DESC','MONTANT','NOM_TIER','TAUX') DEFAULT NULL,
    `telephone`          varchar(15)                                                                          DEFAULT NULL,
    `telephone_fixe`     varchar(15)                                                                          DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK7s14b63vib0vg70p8cl4gvyn1` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.groupe_tiers_payant : ~0 rows (environ)

-- Listage de la structure de la table warehouse. importation
CREATE TABLE IF NOT EXISTS `importation`
(
    `id`                 bigint(20)  NOT NULL AUTO_INCREMENT,
    `created_at`         datetime(6) NOT NULL,
    `error_size`         int(11)     NOT NULL,
    `importation_status` tinyint(4)  NOT NULL CHECK (`importation_status` between 0 and 4),
    `importation_type`   tinyint(4)  NOT NULL CHECK (`importation_type` between 0 and 4),
    `ligne_en_erreur`    longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`ligne_en_erreur`)),
    `size`               int(11)     NOT NULL,
    `total_zise`         int(11)     NOT NULL,
    `updated_at`         datetime(6)                                        DEFAULT NULL,
    `user_id`            bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `importation_status_index` (`importation_status`),
    KEY `importation_type_index` (`importation_type`),
    KEY `created_at_index` (`created_at`),
    KEY `FKkdc81pqyi8ddmoxrpxj7yi109` (`user_id`),
    CONSTRAINT `FKkdc81pqyi8ddmoxrpxj7yi109` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.importation : ~0 rows (environ)

-- Listage de la structure de la table warehouse. importation_echouee
CREATE TABLE IF NOT EXISTS `importation_echouee`
(
    `id`          bigint(20)  NOT NULL AUTO_INCREMENT,
    `created`     datetime(6) NOT NULL,
    `is_commande` bit(1)      NOT NULL,
    `object_id`   bigint(20) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.importation_echouee : ~0 rows (environ)

-- Listage de la structure de la table warehouse. importation_echouee_ligne
CREATE TABLE IF NOT EXISTS `importation_echouee_ligne`
(
    `id`                    bigint(20) NOT NULL AUTO_INCREMENT,
    `code_tva`              int(11)      DEFAULT NULL,
    `date_peremption`       date         DEFAULT NULL,
    `prix_achat`            int(11)      DEFAULT NULL CHECK (`prix_achat` >= 0),
    `prix_un`               int(11)      DEFAULT NULL CHECK (`prix_un` >= 0),
    `produit_cip`           varchar(255) DEFAULT NULL,
    `produit_ean`           varchar(255) DEFAULT NULL,
    `quantity_received`     int(11)      DEFAULT NULL CHECK (`quantity_received` >= 0),
    `ug`                    int(11)      DEFAULT NULL CHECK (`ug` >= 0),
    `importation_echoue_id` bigint(20) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FK9q8p018ys16lk3c5pptgflx6w` (`importation_echoue_id`),
    CONSTRAINT `FK9q8p018ys16lk3c5pptgflx6w` FOREIGN KEY (`importation_echoue_id`) REFERENCES `importation_echouee` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.importation_echouee_ligne : ~0 rows (environ)

-- Listage de la structure de la table warehouse. inventory_transaction
CREATE TABLE IF NOT EXISTS `inventory_transaction`
(
    `id`                           bigint(20)  NOT NULL AUTO_INCREMENT,
    `cost_amount`                  int(11)     NOT NULL,
    `created_at`                   datetime(6) NOT NULL,
    `quantity`                     int(11)     NOT NULL,
    `quantity_after`               int(11)     NOT NULL,
    `quantity_befor`               int(11)     NOT NULL,
    `regular_unit_price`           int(11)     NOT NULL,
    `transaction_type`             tinyint(4)  NOT NULL CHECK (`transaction_type` between 0 and 22),
    `ajustement_id`                bigint(20) DEFAULT NULL,
    `decondition_id`               bigint(20) DEFAULT NULL,
    `delivery_receipt_item_id`     bigint(20) DEFAULT NULL,
    `fournisseur_produit_id`       bigint(20) DEFAULT NULL,
    `magasin_id`                   bigint(20)  NOT NULL,
    `produit_id`                   bigint(20) DEFAULT NULL,
    `repartition_stock_produit_id` bigint(20) DEFAULT NULL,
    `sale_line_id`                 bigint(20) DEFAULT NULL,
    `user_id`                      bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `transaction_type_index` (`transaction_type`),
    KEY `createdAt_index` (`created_at`),
    KEY `FK6saiavquc76u5a44h5r974v5j` (`ajustement_id`),
    KEY `FKqlsoqqqko7hj9gii5wwfdqhqa` (`decondition_id`),
    KEY `FKeao881bm44imwilft8yt4tv9k` (`delivery_receipt_item_id`),
    KEY `FK9mkq9q3qawkmnsc635662phvl` (`fournisseur_produit_id`),
    KEY `FKa7i5348dxyy1dnnhvo05nr4ve` (`magasin_id`),
    KEY `FK30wj3ywp114bifpc3xu7rex3i` (`produit_id`),
    KEY `FKjoy3lg6699dgt74famwcquqhm` (`repartition_stock_produit_id`),
    KEY `FKst6t2ax1vlhtjk1okwpbx84hf` (`sale_line_id`),
    KEY `FKa3ij2sbmkbeipbln8tocfyg0` (`user_id`),
    CONSTRAINT `FK30wj3ywp114bifpc3xu7rex3i` FOREIGN KEY (`produit_id`) REFERENCES `produit` (`id`),
    CONSTRAINT `FK6saiavquc76u5a44h5r974v5j` FOREIGN KEY (`ajustement_id`) REFERENCES `ajustement` (`id`),
    CONSTRAINT `FK9mkq9q3qawkmnsc635662phvl` FOREIGN KEY (`fournisseur_produit_id`) REFERENCES `fournisseur_produit` (`id`),
    CONSTRAINT `FKa3ij2sbmkbeipbln8tocfyg0` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `FKa7i5348dxyy1dnnhvo05nr4ve` FOREIGN KEY (`magasin_id`) REFERENCES `magasin` (`id`),
    CONSTRAINT `FKeao881bm44imwilft8yt4tv9k` FOREIGN KEY (`delivery_receipt_item_id`) REFERENCES `delivery_receipt_item` (`id`),
    CONSTRAINT `FKjoy3lg6699dgt74famwcquqhm` FOREIGN KEY (`repartition_stock_produit_id`) REFERENCES `repartition_stock_produit` (`id`),
    CONSTRAINT `FKqlsoqqqko7hj9gii5wwfdqhqa` FOREIGN KEY (`decondition_id`) REFERENCES `decondition` (`id`),
    CONSTRAINT `FKst6t2ax1vlhtjk1okwpbx84hf` FOREIGN KEY (`sale_line_id`) REFERENCES `sales_line` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.inventory_transaction : ~0 rows (environ)

-- Listage de la structure de la table warehouse. invoice_payment
CREATE TABLE IF NOT EXISTS `invoice_payment`
(
    `id`                      bigint(20)  NOT NULL AUTO_INCREMENT,
    `montant_attendu`         int(11)     NOT NULL,
    `created`                 datetime(6) NOT NULL,
    `grouped`                 bit(1)      DEFAULT NULL,
    `invoice_date`            date        DEFAULT NULL,
    `montant_verse`           int(11)     DEFAULT NULL,
    `montant_paye`            int(11)     NOT NULL,
    `banque_id`               bigint(20)  DEFAULT NULL,
    `cash_register_id`        bigint(20)  NOT NULL,
    `facture_tiers_payant_id` bigint(20)  NOT NULL,
    `parent_id`               bigint(20)  DEFAULT NULL,
    `payment_mode_code`       varchar(50) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `FKktfofq520ch37ujiypb8lgjsx` (`banque_id`),
    KEY `FKnrvwnlyv50ome7nc2jpgytm7e` (`cash_register_id`),
    KEY `FKoayv76mnkulkjnbf66vpy26v4` (`facture_tiers_payant_id`),
    KEY `FK2jbx4pxbdrcih2ytx4nd8elfy` (`parent_id`),
    KEY `FKlqlnt6i5kab1htr25udh8ova9` (`payment_mode_code`),
    CONSTRAINT `FK2jbx4pxbdrcih2ytx4nd8elfy` FOREIGN KEY (`parent_id`) REFERENCES `invoice_payment` (`id`),
    CONSTRAINT `FKktfofq520ch37ujiypb8lgjsx` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`),
    CONSTRAINT `FKlqlnt6i5kab1htr25udh8ova9` FOREIGN KEY (`payment_mode_code`) REFERENCES `payment_mode` (`code`),
    CONSTRAINT `FKnrvwnlyv50ome7nc2jpgytm7e` FOREIGN KEY (`cash_register_id`) REFERENCES `cash_register` (`id`),
    CONSTRAINT `FKoayv76mnkulkjnbf66vpy26v4` FOREIGN KEY (`facture_tiers_payant_id`) REFERENCES `facture_tiers_payant` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.invoice_payment : ~0 rows (environ)

-- Listage de la structure de la table warehouse. invoice_payment_item
CREATE TABLE IF NOT EXISTS `invoice_payment_item`
(
    `id`                       bigint(20) NOT NULL AUTO_INCREMENT,
    `montant_attendu`          int(11)    NOT NULL,
    `montant_paye`             int(11)    NOT NULL,
    `invoice_payment_id`       bigint(20) NOT NULL,
    `third_party_sale_line_id` bigint(20) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FKmbkjv7f6nw0ygcvmikcyu34mm` (`invoice_payment_id`),
    KEY `FK6rjhg3s15cyvb1wt5pl7athr2` (`third_party_sale_line_id`),
    CONSTRAINT `FK6rjhg3s15cyvb1wt5pl7athr2` FOREIGN KEY (`third_party_sale_line_id`) REFERENCES `third_party_sale_line` (`id`),
    CONSTRAINT `FKmbkjv7f6nw0ygcvmikcyu34mm` FOREIGN KEY (`invoice_payment_id`) REFERENCES `invoice_payment` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.invoice_payment_item : ~0 rows (environ)

-- Listage de la structure de la table warehouse. laboratoire
CREATE TABLE IF NOT EXISTS `laboratoire`
(
    `id`      bigint(20)   NOT NULL AUTO_INCREMENT,
    `libelle` varchar(255) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKch0c4o3olc65u3yap006nxq0i` (`libelle`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.laboratoire : ~0 rows (environ)

-- Listage de la structure de la table warehouse. ligne_avoir
CREATE TABLE IF NOT EXISTS `ligne_avoir`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT,
    `qte`        int(11)    NOT NULL,
    `qte_servi`  int(11)    NOT NULL,
    `avoir_id`   bigint(20) NOT NULL,
    `produit_id` bigint(20) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK8jhcxa6a2lu7mjaoc9n2b0caj` (`avoir_id`, `produit_id`),
    KEY `FK1830n6vy54b68kip0wttwese1` (`produit_id`),
    CONSTRAINT `FK1830n6vy54b68kip0wttwese1` FOREIGN KEY (`produit_id`) REFERENCES `produit` (`id`),
    CONSTRAINT `FK4uge1m9g5j5grtnkc2ens3gs9` FOREIGN KEY (`avoir_id`) REFERENCES `avoir` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.ligne_avoir : ~0 rows (environ)

-- Listage de la structure de la table warehouse. logs
CREATE TABLE IF NOT EXISTS `logs`
(
    `id`               bigint(20)   NOT NULL AUTO_INCREMENT,
    `comments`         varchar(255) NOT NULL,
    `created_at`       datetime(6)  NOT NULL,
    `indentity_key`    varchar(255) NOT NULL,
    `new_object`       varchar(255) DEFAULT NULL,
    `old_object`       varchar(255) DEFAULT NULL,
    `transaction_type` tinyint(4)   NOT NULL CHECK (`transaction_type` between 0 and 22),
    `user_id`          bigint(20)   NOT NULL,
    PRIMARY KEY (`id`),
    KEY `transaction_type_index` (`transaction_type`),
    KEY `createdAt_index` (`created_at`),
    KEY `indentityKey_index` (`indentity_key`),
    KEY `FK6313q4colhy85u9nyh7c6hy50` (`user_id`),
    CONSTRAINT `FK6313q4colhy85u9nyh7c6hy50` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.logs : ~0 rows (environ)

-- Listage de la structure de la table warehouse. lot
CREATE TABLE IF NOT EXISTS `lot`
(
    `id`                   bigint(20)   NOT NULL AUTO_INCREMENT,
    `created_date`         datetime(6)  NOT NULL,
    `expiry_date`          date                  DEFAULT NULL,
    `manufacturing_date`   date                  DEFAULT NULL,
    `num_lot`              varchar(255) NOT NULL,
    `quantity`             int(11)      NOT NULL,
    `quantity_received`    int(11)      NOT NULL,
    `receipt_refernce`     varchar(255) NOT NULL,
    `quantity_received_ug` int(11)      NOT NULL DEFAULT 0,
    `receipt_item_id`      bigint(20)   NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK8qafssjc0vp90d8q7hvwytw29` (`num_lot`, `receipt_item_id`),
    KEY `num_lot_index` (`num_lot`),
    KEY `lot_receipt_refernce_index` (`receipt_refernce`),
    KEY `FKe8v8jf1io2l4tpnkeji62769g` (`receipt_item_id`),
    CONSTRAINT `FKe8v8jf1io2l4tpnkeji62769g` FOREIGN KEY (`receipt_item_id`) REFERENCES `delivery_receipt_item` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.lot : ~0 rows (environ)

-- Listage de la structure de la table warehouse. lot_sold
CREATE TABLE IF NOT EXISTS `lot_sold`
(
    `id`           bigint(20)  NOT NULL AUTO_INCREMENT,
    `created_date` datetime(6) NOT NULL,
    `quantity`     int(11)     NOT NULL,
    `lot_id`       bigint(20)  NOT NULL,
    `sale_line_id` bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKa6wqp0hlgo298d08cb01e3e6k` (`lot_id`, `sale_line_id`),
    KEY `FKq26nrtuvl710oxkgty3xrxvbv` (`sale_line_id`),
    CONSTRAINT `FKmffug438lanx9iamtbwaoxh22` FOREIGN KEY (`lot_id`) REFERENCES `lot` (`id`),
    CONSTRAINT `FKq26nrtuvl710oxkgty3xrxvbv` FOREIGN KEY (`sale_line_id`) REFERENCES `sales_line` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.lot_sold : ~0 rows (environ)

-- Listage de la structure de la table warehouse. magasin
CREATE TABLE IF NOT EXISTS `magasin`
(
    `id`                  bigint(20)                               NOT NULL AUTO_INCREMENT,
    `address`             varchar(255) DEFAULT NULL,
    `compte_bancaire`     varchar(255) DEFAULT NULL,
    `compte_contribuable` varchar(255) DEFAULT NULL,
    `email`               varchar(255) DEFAULT NULL,
    `full_name`           varchar(255)                             NOT NULL,
    `name`                varchar(255)                             NOT NULL,
    `note`                varchar(255) DEFAULT NULL,
    `num_comptable`       varchar(255) DEFAULT NULL,
    `phone`               varchar(255) DEFAULT NULL,
    `registre`            varchar(255) DEFAULT NULL,
    `registre_imposition` varchar(255) DEFAULT NULL,
    `type_magasin`        enum ('DEPOT','DEPOT_AGGREE','OFFICINE') NOT NULL,
    `welcome_message`     varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKjn6wxi7t8rmrg6rbcemf4yr1h` (`full_name`),
    UNIQUE KEY `UKjj7fulne1dmx3boof0itbmj8n` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.magasin : ~0 rows (environ)

-- Listage de la structure de la table warehouse. menu
CREATE TABLE IF NOT EXISTS `menu`
(
    `id`               bigint(20)                       NOT NULL AUTO_INCREMENT,
    `enable`           bit(1)                           NOT NULL,
    `icon_java_client` varchar(255) DEFAULT NULL,
    `icon_web`         varchar(255) DEFAULT NULL,
    `libelle`          varchar(255)                     NOT NULL,
    `name`             varchar(70)                      NOT NULL,
    `ordre`            int(11)                          NOT NULL,
    `racine`           bit(1)                           NOT NULL,
    `type_menu`        enum ('ALL','JAVA_CLIENT','WEB') NOT NULL,
    `parent_id`        bigint(20)   DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKm05sb1hgsv38qjb4ksyh5eat2` (`name`),
    KEY `FKgeupubdqncc1lpgf2cn4fqwbc` (`parent_id`),
    CONSTRAINT `FKgeupubdqncc1lpgf2cn4fqwbc` FOREIGN KEY (`parent_id`) REFERENCES `menu` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.menu : ~0 rows (environ)

-- Listage de la structure de la table warehouse. motif_ajustement
CREATE TABLE IF NOT EXISTS `motif_ajustement`
(
    `id`      bigint(20)   NOT NULL AUTO_INCREMENT,
    `libelle` varchar(255) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKjcn7824hyqd0s0yml6cemqxj1` (`libelle`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.motif_ajustement : ~0 rows (environ)

-- Listage de la structure de la table warehouse. motif_retour_produit
CREATE TABLE IF NOT EXISTS `motif_retour_produit`
(
    `id`      bigint(20)   NOT NULL AUTO_INCREMENT,
    `libelle` varchar(255) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKkhj7gugyhyfcgw17t02j2mxlr` (`libelle`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.motif_retour_produit : ~0 rows (environ)

-- Listage de la structure de la table warehouse. order_line
CREATE TABLE IF NOT EXISTS `order_line`
(
    `id`                     bigint(20)  NOT NULL AUTO_INCREMENT,
    `cost_amount`            int(11)     NOT NULL,
    `created_at`             datetime(6) NOT NULL,
    `discount_amount`        int(11)     NOT NULL DEFAULT 0,
    `gross_amount`           int(11)     NOT NULL,
    `init_stock`             int(11)     NOT NULL,
    `net_amount`             int(11)              DEFAULT 0,
    `order_amount`           int(11)     NOT NULL,
    `order_cost_amount`      int(11)     NOT NULL DEFAULT 0,
    `order_unit_price`       int(11)     NOT NULL,
    `provisional_code`       bit(1)               DEFAULT NULL,
    `quantity_received`      int(11)              DEFAULT NULL,
    `quantity_requested`     int(11)     NOT NULL,
    `quantity_returned`      int(11)              DEFAULT NULL,
    `quantity_ug`            int(11)              DEFAULT 0,
    `receipt_date`           datetime(6)          DEFAULT NULL,
    `regular_unit_price`     int(11)     NOT NULL DEFAULT 0,
    `tax_amount`             int(11)              DEFAULT 0,
    `updated_at`             datetime(6) NOT NULL,
    `commande_id`            bigint(20)  NOT NULL,
    `fournisseur_produit_id` bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKsfcar1lpmp9ytui9cg75le9d5` (`commande_id`, `fournisseur_produit_id`),
    KEY `FKkt4tdexu7oo36cobryunswmjh` (`fournisseur_produit_id`),
    CONSTRAINT `FKjelph47crh3lyf09c5c7sqjnq` FOREIGN KEY (`commande_id`) REFERENCES `commande` (`id`),
    CONSTRAINT `FKkt4tdexu7oo36cobryunswmjh` FOREIGN KEY (`fournisseur_produit_id`) REFERENCES `fournisseur_produit` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.order_line : ~0 rows (environ)

-- Listage de la structure de la table warehouse. payment
CREATE TABLE IF NOT EXISTS `payment`
(
    `id`                    bigint(20)  NOT NULL AUTO_INCREMENT,
    `canceled`              tinyint(1)  NOT NULL DEFAULT 0,
    `created_at`            datetime(6) NOT NULL,
    `effective_update_date` datetime(6) NOT NULL,
    `montant_verse`         int(11)     NOT NULL,
    `net_amount`            int(11)     NOT NULL,
    `paid_amount`           int(11)     NOT NULL,
    `part_assure`           int(11)              DEFAULT 0,
    `part_tiers_payant`     int(11)              DEFAULT 0,
    `statut`                tinyint(4)  NOT NULL CHECK (`statut` between 0 and 6),
    `ticket_code`           varchar(50)          DEFAULT NULL,
    `updated_at`            datetime(6) NOT NULL,
    `customer_id`           bigint(20)           DEFAULT NULL,
    `payment_mode_code`     varchar(50) NOT NULL,
    `sales_id`              bigint(20)           DEFAULT NULL,
    `user_id`               bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `ticket_code_index` (`ticket_code`),
    KEY `FKby2skjf3ov608yb6nm16b49lg` (`customer_id`),
    KEY `FKd4y7p2b9ky7wnt9j8s68p39nq` (`payment_mode_code`),
    KEY `FKft95hgc3p39d9hedhmpe4pk7w` (`sales_id`),
    KEY `FK4spfnm9si9dowsatcqs5or42i` (`user_id`),
    CONSTRAINT `FK4spfnm9si9dowsatcqs5or42i` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `FKby2skjf3ov608yb6nm16b49lg` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`),
    CONSTRAINT `FKd4y7p2b9ky7wnt9j8s68p39nq` FOREIGN KEY (`payment_mode_code`) REFERENCES `payment_mode` (`code`),
    CONSTRAINT `FKft95hgc3p39d9hedhmpe4pk7w` FOREIGN KEY (`sales_id`) REFERENCES `sales` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.payment : ~0 rows (environ)

-- Listage de la structure de la table warehouse. payment_fournisseur
CREATE TABLE IF NOT EXISTS `payment_fournisseur`
(
    `id`                  bigint(20)  NOT NULL AUTO_INCREMENT,
    `created_at`          datetime(6) NOT NULL,
    `net_amount`          int(11)     NOT NULL,
    `paid_amount`         int(11)     NOT NULL,
    `rest_to_pay`         int(11)     NOT NULL,
    `updated_at`          datetime(6) NOT NULL,
    `calendar_work_day`   date        NOT NULL,
    `delivery_receipt_id` bigint(20)  DEFAULT NULL,
    `payment_mode_code`   varchar(50) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `FKfurw6ynv37qdjnrraxqiemtsw` (`calendar_work_day`),
    KEY `FK4ww4ollko2ixtltphqfb72bd8` (`delivery_receipt_id`),
    KEY `FKpwcxxxlon8gco04nj6nn95mkc` (`payment_mode_code`),
    CONSTRAINT `FK4ww4ollko2ixtltphqfb72bd8` FOREIGN KEY (`delivery_receipt_id`) REFERENCES `delivery_receipt` (`id`),
    CONSTRAINT `FKfurw6ynv37qdjnrraxqiemtsw` FOREIGN KEY (`calendar_work_day`) REFERENCES `warehouse_calendar` (`work_day`),
    CONSTRAINT `FKpwcxxxlon8gco04nj6nn95mkc` FOREIGN KEY (`payment_mode_code`) REFERENCES `payment_mode` (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.payment_fournisseur : ~0 rows (environ)

-- Listage de la structure de la table warehouse. payment_mode
CREATE TABLE IF NOT EXISTS `payment_mode`
(
    `code`          varchar(50)  NOT NULL,
    `enable`        bit(1)       NOT NULL,
    `payment_group` tinyint(4)   NOT NULL CHECK (`payment_group` between 0 and 6),
    `icon_url`      varchar(255) DEFAULT NULL,
    `libelle`       varchar(255) NOT NULL,
    `ordre_tri`     smallint(6)  NOT NULL,
    PRIMARY KEY (`code`),
    UNIQUE KEY `UKtfqn29lfkm2lkujuptvoiiyki` (`libelle`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.payment_mode : ~0 rows (environ)

-- Listage de la structure de la table warehouse. payment_transaction
CREATE TABLE IF NOT EXISTS `payment_transaction`
(
    `id`                bigint(20)  NOT NULL AUTO_INCREMENT,
    `amount`            int(11)     NOT NULL,
    `categorie_ca`      tinyint(4)  NOT NULL CHECK (`categorie_ca` between 0 and 3),
    `commentaire`       varchar(255) DEFAULT NULL,
    `created_at`        datetime(6) NOT NULL,
    `credit`            bit(1)      NOT NULL,
    `organisme_id`      bigint(20)   DEFAULT NULL,
    `transaction_date`  datetime(6) NOT NULL,
    `type_transaction`  tinyint(4)  NOT NULL CHECK (`type_transaction` between 0 and 9),
    `calendar_work_day` date        NOT NULL,
    `cash_register_id`  bigint(20)  NOT NULL,
    `payment_mode_code` varchar(50) NOT NULL,
    `user_id`           bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `organisme_id_index` (`organisme_id`),
    KEY `FKtd5v4m1mt78c1lkx40hrsx8gk` (`calendar_work_day`),
    KEY `FKgq9ocwvjtdlt49fd0vhibjd4t` (`cash_register_id`),
    KEY `FKllqlbjskwsugxh20pc7h43wd3` (`payment_mode_code`),
    KEY `FKgb2j41meoqx9t219ccuyiui43` (`user_id`),
    CONSTRAINT `FKgb2j41meoqx9t219ccuyiui43` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `FKgq9ocwvjtdlt49fd0vhibjd4t` FOREIGN KEY (`cash_register_id`) REFERENCES `cash_register` (`id`),
    CONSTRAINT `FKllqlbjskwsugxh20pc7h43wd3` FOREIGN KEY (`payment_mode_code`) REFERENCES `payment_mode` (`code`),
    CONSTRAINT `FKtd5v4m1mt78c1lkx40hrsx8gk` FOREIGN KEY (`calendar_work_day`) REFERENCES `warehouse_calendar` (`work_day`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.payment_transaction : ~0 rows (environ)

-- Listage de la structure de la table warehouse. persistent_audit_event
CREATE TABLE IF NOT EXISTS `persistent_audit_event`
(
    `event_id`   bigint(20)   NOT NULL AUTO_INCREMENT,
    `event_date` datetime(6)  DEFAULT NULL,
    `event_type` varchar(255) DEFAULT NULL,
    `principal`  varchar(255) NOT NULL,
    PRIMARY KEY (`event_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.persistent_audit_event : ~0 rows (environ)

-- Listage de la structure de la table warehouse. persistent_audit_evt_data
CREATE TABLE IF NOT EXISTS `persistent_audit_evt_data`
(
    `event_id` bigint(20)   NOT NULL,
    `value`    varchar(255) DEFAULT NULL,
    `name`     varchar(255) NOT NULL,
    PRIMARY KEY (`event_id`, `name`),
    CONSTRAINT `FK9ynvwlu7w4uqpjlxvk9kiscqs` FOREIGN KEY (`event_id`) REFERENCES `persistent_audit_event` (`event_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.persistent_audit_evt_data : ~0 rows (environ)

-- Listage de la structure de la table warehouse. persistent_token
CREATE TABLE IF NOT EXISTS `persistent_token`
(
    `series`      varchar(255) NOT NULL,
    `ip_address`  varchar(39)  DEFAULT NULL,
    `token_date`  date         DEFAULT NULL,
    `token_value` varchar(255) NOT NULL,
    `user_agent`  varchar(255) DEFAULT NULL,
    `user_id`     bigint(20)   DEFAULT NULL,
    PRIMARY KEY (`series`),
    KEY `FKqiuyia9rgw42uksrgy3ayk1lc` (`user_id`),
    CONSTRAINT `FKqiuyia9rgw42uksrgy3ayk1lc` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.persistent_token : ~0 rows (environ)

-- Listage de la structure de la table warehouse. poste
CREATE TABLE IF NOT EXISTS `poste`
(
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT,
    `address`      varchar(255) NOT NULL,
    `name`         varchar(255) NOT NULL,
    `poste_number` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKk7d8e8fhttgpyrppl6unbvybx` (`name`),
    KEY `poste_name_index` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.poste : ~0 rows (environ)

-- Listage de la structure de la table warehouse. printer
CREATE TABLE IF NOT EXISTS `printer`
(
    `id`              bigint(20)   NOT NULL AUTO_INCREMENT,
    `address`         varchar(255) DEFAULT NULL,
    `default_printer` bit(1)       DEFAULT NULL,
    `length`          int(11)      NOT NULL,
    `name`            varchar(255) NOT NULL,
    `width`           int(11)      NOT NULL,
    `poste_id`        bigint(20)   DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKrr7bfntxwxqe91dsa9458f582` (`name`),
    KEY `name_index` (`name`),
    KEY `FKcip3p5i9uwm6mgqpsnrfk5vcu` (`poste_id`),
    CONSTRAINT `FKcip3p5i9uwm6mgqpsnrfk5vcu` FOREIGN KEY (`poste_id`) REFERENCES `poste` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.printer : ~0 rows (environ)

-- Listage de la structure de la table warehouse. privilege
CREATE TABLE IF NOT EXISTS `privilege`
(
    `name`    varchar(100) NOT NULL,
    `libelle` varchar(255) NOT NULL,
    `menu_id` bigint(20)   NOT NULL,
    PRIMARY KEY (`name`),
    KEY `FKqckdmdk8jouw4r8o53uq88xlo` (`menu_id`),
    CONSTRAINT `FKqckdmdk8jouw4r8o53uq88xlo` FOREIGN KEY (`menu_id`) REFERENCES `menu` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.privilege : ~0 rows (environ)

-- Listage de la structure de la table warehouse. product_state
CREATE TABLE IF NOT EXISTS `product_state`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT,
    `state`      tinyint(4) NOT NULL CHECK (`state` between 0 and 4),
    `updated`    datetime(6) DEFAULT NULL,
    `produit_id` bigint(20) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `state_index` (`state`),
    KEY `FKfgyq10ugf4wnf7uk2vxsc3ryg` (`produit_id`),
    CONSTRAINT `FKfgyq10ugf4wnf7uk2vxsc3ryg` FOREIGN KEY (`produit_id`) REFERENCES `produit` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.product_state : ~0 rows (environ)

-- Listage de la structure de la table warehouse. produit
CREATE TABLE IF NOT EXISTS `produit`
(
    `id`                      bigint(20)   NOT NULL AUTO_INCREMENT,
    `check_expiry_date`       tinyint(1)                                         DEFAULT 0,
    `chiffre`                 tinyint(1)                                         DEFAULT 1,
    `cmu_amount`              int(11)                                            DEFAULT 0 CHECK (`cmu_amount` >= 0),
    `code_ean`                varchar(255)                                       DEFAULT NULL,
    `code_remise`             varchar(6)                                         DEFAULT 'CODE_0' COMMENT 'Code de remise qui seront mappés sur les grilles de remises',
    `cost_amount`             int(11)      NOT NULL,
    `created_at`              datetime(6)  NOT NULL,
    `daily_stock_json`        longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`daily_stock_json`)),
    `deconditionnable`        bit(1)       NOT NULL,
    `item_cost_amount`        int(11)      NOT NULL CHECK (`item_cost_amount` >= 0),
    `item_qty`                int(11)      NOT NULL CHECK (`item_qty` >= 0),
    `item_regular_unit_price` int(11)      NOT NULL CHECK (`item_regular_unit_price` >= 0),
    `libelle`                 varchar(255) NOT NULL,
    `net_unit_price`          int(11)      NOT NULL,
    `perime_at`               date                                               DEFAULT NULL,
    `prix_mnp`                int(11)      NOT NULL                              DEFAULT 0,
    `prix_reference`          int(11)                                            DEFAULT NULL CHECK (`prix_reference` >= 0),
    `qty_appro`               int(11)                                            DEFAULT 0,
    `qty_seuil_mini`          int(11)                                            DEFAULT 0,
    `regular_unit_price`      int(11)      NOT NULL,
    `scheduled`               tinyint(1)                                         DEFAULT 0 COMMENT 'pour les produits avec une obligation ordonnance',
    `seuil_decond`            int(11)                                            DEFAULT NULL CHECK (`seuil_decond` >= 0),
    `seuil_reassort`          int(11)                                            DEFAULT NULL CHECK (`seuil_reassort` >= 0),
    `status`                  tinyint(4)   NOT NULL CHECK (`status` between 0 and 3),
    `type_produit`            tinyint(4)   NOT NULL CHECK (`type_produit` between 0 and 1),
    `updated_at`              datetime(6)  NOT NULL,
    `dci_id`                  bigint(20)                                         DEFAULT NULL,
    `famille_id`              bigint(20)   NOT NULL,
    `forme_id`                bigint(20)                                         DEFAULT NULL,
    `gamme_id`                bigint(20)                                         DEFAULT NULL,
    `laboratoire_id`          bigint(20)                                         DEFAULT NULL,
    `parent_id`               bigint(20)                                         DEFAULT NULL,
    `tableau_id`              bigint(20)                                         DEFAULT NULL,
    `tva_id`                  bigint(20)   NOT NULL,
    `type_etyquette_id`       bigint(20)                                         DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKhaaprmfc9pf1bp3n5g9dnkx91` (`libelle`, `type_produit`),
    KEY `libelle_index` (`libelle`),
    KEY `codeEan_index` (`code_ean`),
    KEY `status_index` (`status`),
    KEY `FK1a3bimdll1wc7by0ng16rl58y` (`dci_id`),
    KEY `FK5m918v7ukauswsdgdo3pv74fa` (`famille_id`),
    KEY `FKl718rk1riol8vlo4ynew1bv08` (`forme_id`),
    KEY `FK5nonk9ie64lu7b27m1a8fs741` (`gamme_id`),
    KEY `FKhowc8u7evv6pewlrvhs0q1lap` (`laboratoire_id`),
    KEY `FKchxb0k9i70n6x9sa3mcpgfqei` (`parent_id`),
    KEY `FKh4p706en3rc1tbafjh6goa7yp` (`tableau_id`),
    KEY `FKabkfkm5f6kst7099gv3dafaac` (`tva_id`),
    KEY `FKg3ysemtb7iudo9q67fo0r0d2o` (`type_etyquette_id`),
    CONSTRAINT `FK1a3bimdll1wc7by0ng16rl58y` FOREIGN KEY (`dci_id`) REFERENCES `dci` (`id`),
    CONSTRAINT `FK5m918v7ukauswsdgdo3pv74fa` FOREIGN KEY (`famille_id`) REFERENCES `famille_produit` (`id`),
    CONSTRAINT `FK5nonk9ie64lu7b27m1a8fs741` FOREIGN KEY (`gamme_id`) REFERENCES `gamme_produit` (`id`),
    CONSTRAINT `FKabkfkm5f6kst7099gv3dafaac` FOREIGN KEY (`tva_id`) REFERENCES `tva` (`id`),
    CONSTRAINT `FKchxb0k9i70n6x9sa3mcpgfqei` FOREIGN KEY (`parent_id`) REFERENCES `produit` (`id`),
    CONSTRAINT `FKg3ysemtb7iudo9q67fo0r0d2o` FOREIGN KEY (`type_etyquette_id`) REFERENCES `type_etiquette` (`id`),
    CONSTRAINT `FKh4p706en3rc1tbafjh6goa7yp` FOREIGN KEY (`tableau_id`) REFERENCES `tableau` (`id`),
    CONSTRAINT `FKhowc8u7evv6pewlrvhs0q1lap` FOREIGN KEY (`laboratoire_id`) REFERENCES `laboratoire` (`id`),
    CONSTRAINT `FKl718rk1riol8vlo4ynew1bv08` FOREIGN KEY (`forme_id`) REFERENCES `form_produit` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.produit : ~0 rows (environ)

-- Listage de la structure de la table warehouse. produit_aud
CREATE TABLE IF NOT EXISTS `produit_aud`
(
    `id`                      bigint(20) NOT NULL,
    `rev`                     int(11)    NOT NULL,
    `revtype`                 tinyint(4)   DEFAULT NULL,
    `check_expiry_date`       tinyint(1)   DEFAULT 0,
    `cmu_amount`              int(11)      DEFAULT 0,
    `code_ean`                varchar(255) DEFAULT NULL,
    `code_remise`             varchar(6)   DEFAULT 'CODE_0' COMMENT 'Code de remise qui seront mappés sur les grilles de remises',
    `cost_amount`             int(11)      DEFAULT NULL,
    `created_at`              datetime(6)  DEFAULT NULL,
    `deconditionnable`        bit(1)       DEFAULT NULL,
    `item_cost_amount`        int(11)      DEFAULT NULL,
    `item_qty`                int(11)      DEFAULT NULL,
    `item_regular_unit_price` int(11)      DEFAULT NULL,
    `libelle`                 varchar(255) DEFAULT NULL,
    `net_unit_price`          int(11)      DEFAULT NULL,
    `perime_at`               date         DEFAULT NULL,
    `prix_mnp`                int(11)      DEFAULT 0,
    `prix_reference`          int(11)      DEFAULT NULL,
    `qty_appro`               int(11)      DEFAULT 0,
    `qty_seuil_mini`          int(11)      DEFAULT 0,
    `regular_unit_price`      int(11)      DEFAULT NULL,
    `scheduled`               tinyint(1)   DEFAULT 0 COMMENT 'pour les produits avec une obligation ordonnance',
    `seuil_decond`            int(11)      DEFAULT NULL,
    `seuil_reassort`          int(11)      DEFAULT NULL,
    `status`                  tinyint(4)   DEFAULT NULL CHECK (`status` between 0 and 3),
    `type_produit`            tinyint(4)   DEFAULT NULL CHECK (`type_produit` between 0 and 1),
    `updated_at`              datetime(6)  DEFAULT NULL,
    `tableau_id`              bigint(20)   DEFAULT NULL,
    PRIMARY KEY (`rev`, `id`),
    CONSTRAINT `FKlalyyb5rcjm7w9wu2uhknh61j` FOREIGN KEY (`rev`) REFERENCES `revinfo` (`rev`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.produit_aud : ~0 rows (environ)

-- Listage de la structure de la table warehouse. produit_perime
CREATE TABLE IF NOT EXISTS `produit_perime`
(
    `id`                bigint(20)  NOT NULL AUTO_INCREMENT,
    `after_stock`       int(11)     NOT NULL,
    `created`           datetime(6) NOT NULL,
    `init_stock`        int(11)     NOT NULL CHECK (`init_stock` >= 1),
    `peremption_date`   date        NOT NULL,
    `quantity`          int(11)     NOT NULL CHECK (`quantity` >= 1),
    `calendar_work_day` date        NOT NULL,
    `lot_id`            bigint(20) DEFAULT NULL,
    `produit_id`        bigint(20)  NOT NULL,
    `user_id`           bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `produit_perime_index` (`peremption_date`),
    KEY `FK71lr6b63m767peqs6nsceiyqq` (`calendar_work_day`),
    KEY `FK7qyne8q37phmm5hld2e6byxco` (`lot_id`),
    KEY `FKg4s7frhwa9ci11yr77momll5r` (`produit_id`),
    KEY `FK3aelnwbpfeqjrtq990unyjs56` (`user_id`),
    CONSTRAINT `FK3aelnwbpfeqjrtq990unyjs56` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `FK71lr6b63m767peqs6nsceiyqq` FOREIGN KEY (`calendar_work_day`) REFERENCES `warehouse_calendar` (`work_day`),
    CONSTRAINT `FK7qyne8q37phmm5hld2e6byxco` FOREIGN KEY (`lot_id`) REFERENCES `lot` (`id`),
    CONSTRAINT `FKg4s7frhwa9ci11yr77momll5r` FOREIGN KEY (`produit_id`) REFERENCES `produit` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.produit_perime : ~0 rows (environ)

-- Listage de la structure de la table warehouse. rayon
CREATE TABLE IF NOT EXISTS `rayon`
(
    `id`         bigint(20)   NOT NULL AUTO_INCREMENT,
    `code`       varchar(255) NOT NULL,
    `exclude`    tinyint(1) DEFAULT 0,
    `libelle`    varchar(255) NOT NULL,
    `storage_id` bigint(20)   NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK6c4mu7mhf4f0shby67qh7g0b7` (`libelle`, `storage_id`),
    UNIQUE KEY `UKc7q7hrcb4fgtu9nx1hv84amdx` (`code`, `storage_id`),
    KEY `FK4vkox6f9rrh2asaji8wwu7ruu` (`storage_id`),
    CONSTRAINT `FK4vkox6f9rrh2asaji8wwu7ruu` FOREIGN KEY (`storage_id`) REFERENCES `storage` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.rayon : ~0 rows (environ)

-- Listage de la structure de la table warehouse. rayon_produit
CREATE TABLE IF NOT EXISTS `rayon_produit`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT,
    `produit_id` bigint(20) NOT NULL,
    `rayon_id`   bigint(20) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKarionkb6k29slypt62myqg6sb` (`produit_id`, `rayon_id`),
    KEY `FKjqh6g957rhoboh4l4atf6t78t` (`rayon_id`),
    CONSTRAINT `FK8ux9wik1mhtffce4o7s08dahe` FOREIGN KEY (`produit_id`) REFERENCES `produit` (`id`),
    CONSTRAINT `FKjqh6g957rhoboh4l4atf6t78t` FOREIGN KEY (`rayon_id`) REFERENCES `rayon` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.rayon_produit : ~0 rows (environ)

-- Listage de la structure de la table warehouse. reference
CREATE TABLE IF NOT EXISTS `reference`
(
    `id`             bigint(20)   NOT NULL AUTO_INCREMENT,
    `mvt_date`       date         NOT NULL,
    `num`            varchar(255) NOT NULL,
    `number_transac` int(11)      NOT NULL CHECK (`number_transac` >= 0),
    `d_type`         int(11)      NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKpj0iecdrn6o15jiuqnbj5l80o` (`mvt_date`, `d_type`, `num`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.reference : ~0 rows (environ)

-- Listage de la structure de la table warehouse. remise
CREATE TABLE IF NOT EXISTS `remise`
(
    `dtype`        varchar(31) NOT NULL,
    `id`           bigint(20)  NOT NULL AUTO_INCREMENT,
    `enable`       tinyint(1)  NOT NULL DEFAULT 1,
    `libelle`      varchar(100)         DEFAULT NULL,
    `remise_value` float                DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.remise : ~0 rows (environ)

-- Listage de la structure de la table warehouse. repartition_stock_produit
CREATE TABLE IF NOT EXISTS `repartition_stock_produit`
(
    `id`                           bigint(20)  NOT NULL AUTO_INCREMENT,
    `created_at`                   datetime(6) NOT NULL,
    `dest_final_stock`             int(11)     NOT NULL,
    `dest_init_stock`              int(11)     NOT NULL,
    `qty_mvt`                      int(11)     NOT NULL,
    `source_final_stock`           int(11)     NOT NULL,
    `source_init_stock`            int(11)     NOT NULL,
    `produit_id`                   bigint(20)  NOT NULL,
    `stock_produit_destination_id` bigint(20)  NOT NULL,
    `stock_produit_source_id`      bigint(20)  NOT NULL,
    `user_id`                      bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FKj2oonew83hnf8pmi8nmfiejpo` (`produit_id`),
    KEY `FKrxga8j2mjur3aw43h3bq6l909` (`stock_produit_destination_id`),
    KEY `FKgyu4w5fylq30wrdc1n284yuqg` (`stock_produit_source_id`),
    KEY `FKl09sdq1y926wgof99ifw42ipd` (`user_id`),
    CONSTRAINT `FKgyu4w5fylq30wrdc1n284yuqg` FOREIGN KEY (`stock_produit_source_id`) REFERENCES `stock_produit` (`id`),
    CONSTRAINT `FKj2oonew83hnf8pmi8nmfiejpo` FOREIGN KEY (`produit_id`) REFERENCES `produit` (`id`),
    CONSTRAINT `FKl09sdq1y926wgof99ifw42ipd` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `FKrxga8j2mjur3aw43h3bq6l909` FOREIGN KEY (`stock_produit_destination_id`) REFERENCES `stock_produit` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.repartition_stock_produit : ~0 rows (environ)

-- Listage de la structure de la table warehouse. reponse_retour_bon
CREATE TABLE IF NOT EXISTS `reponse_retour_bon`
(
    `id`                bigint(20)  NOT NULL AUTO_INCREMENT,
    `commentaire`       varchar(150) DEFAULT NULL,
    `date_mtv`          datetime(6) NOT NULL,
    `modified_date`     datetime(6) NOT NULL,
    `statut`            tinyint(4)  NOT NULL CHECK (`statut` between 0 and 1),
    `calendar_work_day` date        NOT NULL,
    `retour_bon_id`     bigint(20)  NOT NULL,
    `user_id`           bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FKh846yv5iwq8wslk1598bqi2lj` (`calendar_work_day`),
    KEY `FKpl4rf8a6nr9cmn7lur7su1b1` (`retour_bon_id`),
    KEY `FK45ub257h046pqyss9wujxfvnq` (`user_id`),
    CONSTRAINT `FK45ub257h046pqyss9wujxfvnq` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `FKh846yv5iwq8wslk1598bqi2lj` FOREIGN KEY (`calendar_work_day`) REFERENCES `warehouse_calendar` (`work_day`),
    CONSTRAINT `FKpl4rf8a6nr9cmn7lur7su1b1` FOREIGN KEY (`retour_bon_id`) REFERENCES `retour_bon` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.reponse_retour_bon : ~0 rows (environ)

-- Listage de la structure de la table warehouse. reponse_retour_bon_item
CREATE TABLE IF NOT EXISTS `reponse_retour_bon_item`
(
    `id`                    bigint(20)  NOT NULL AUTO_INCREMENT,
    `after_stock`           int(11) DEFAULT NULL,
    `date_mtv`              datetime(6) NOT NULL,
    `init_stock`            int(11)     NOT NULL,
    `qty_mvt`               int(11)     NOT NULL CHECK (`qty_mvt` >= 0),
    `reponse_retour_bon_id` bigint(20)  NOT NULL,
    `retour_bon_item_id`    bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FK3kiwqjsi0e810eo3x76r3b9b` (`reponse_retour_bon_id`),
    KEY `FKdygaih0a7jw1diyja06px1aa9` (`retour_bon_item_id`),
    CONSTRAINT `FK3kiwqjsi0e810eo3x76r3b9b` FOREIGN KEY (`reponse_retour_bon_id`) REFERENCES `reponse_retour_bon` (`id`),
    CONSTRAINT `FKdygaih0a7jw1diyja06px1aa9` FOREIGN KEY (`retour_bon_item_id`) REFERENCES `retour_bon_item` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.reponse_retour_bon_item : ~0 rows (environ)

-- Listage de la structure de la table warehouse. retour_bon
CREATE TABLE IF NOT EXISTS `retour_bon`
(
    `id`                  bigint(20)  NOT NULL AUTO_INCREMENT,
    `commentaire`         varchar(150) DEFAULT NULL,
    `date_mtv`            datetime(6) NOT NULL,
    `statut`              tinyint(4)  NOT NULL CHECK (`statut` between 0 and 1),
    `calendar_work_day`   date        NOT NULL,
    `delivery_receipt_id` bigint(20)  NOT NULL,
    `user_id`             bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FK8wol7cdf0cme6rieetb0q81fv` (`calendar_work_day`),
    KEY `FKn7y8mtq6pp8rrln0cugxfu89e` (`delivery_receipt_id`),
    KEY `FKgbybxbktf9oy669tyn6rqlaw1` (`user_id`),
    CONSTRAINT `FK8wol7cdf0cme6rieetb0q81fv` FOREIGN KEY (`calendar_work_day`) REFERENCES `warehouse_calendar` (`work_day`),
    CONSTRAINT `FKgbybxbktf9oy669tyn6rqlaw1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `FKn7y8mtq6pp8rrln0cugxfu89e` FOREIGN KEY (`delivery_receipt_id`) REFERENCES `delivery_receipt` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.retour_bon : ~0 rows (environ)

-- Listage de la structure de la table warehouse. retour_bon_item
CREATE TABLE IF NOT EXISTS `retour_bon_item`
(
    `id`                       bigint(20)  NOT NULL AUTO_INCREMENT,
    `after_stock`              int(11)    DEFAULT NULL,
    `date_mtv`                 datetime(6) NOT NULL,
    `init_stock`               int(11)     NOT NULL,
    `qty_mvt`                  int(11)     NOT NULL CHECK (`qty_mvt` >= 1),
    `delivery_receipt_item_id` bigint(20)  NOT NULL,
    `lot_id`                   bigint(20) DEFAULT NULL,
    `motif_retour_id`          bigint(20)  NOT NULL,
    `retour_bon_id`            bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FKlano0peweywbomvctcihup38y` (`delivery_receipt_item_id`),
    KEY `FKsq7ju65jlgejnnvj1ep3d1vmo` (`lot_id`),
    KEY `FK9yua85has78k1itu1pjp8rvyg` (`motif_retour_id`),
    KEY `FK7f3y7rovqwt9wygs25l6baqky` (`retour_bon_id`),
    CONSTRAINT `FK7f3y7rovqwt9wygs25l6baqky` FOREIGN KEY (`retour_bon_id`) REFERENCES `retour_bon` (`id`),
    CONSTRAINT `FK9yua85has78k1itu1pjp8rvyg` FOREIGN KEY (`motif_retour_id`) REFERENCES `motif_retour_produit` (`id`),
    CONSTRAINT `FKlano0peweywbomvctcihup38y` FOREIGN KEY (`delivery_receipt_item_id`) REFERENCES `delivery_receipt_item` (`id`),
    CONSTRAINT `FKsq7ju65jlgejnnvj1ep3d1vmo` FOREIGN KEY (`lot_id`) REFERENCES `lot` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.retour_bon_item : ~0 rows (environ)

-- Listage de la structure de la table warehouse. revinfo
CREATE TABLE IF NOT EXISTS `revinfo`
(
    `rev`      int(11) NOT NULL AUTO_INCREMENT,
    `revtstmp` bigint(20) DEFAULT NULL,
    PRIMARY KEY (`rev`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.revinfo : ~0 rows (environ)

-- Listage de la structure de la table warehouse. sales
CREATE TABLE IF NOT EXISTS `sales`
(
    `dtype`                           varchar(31)                                                                    NOT NULL,
    `id`                              bigint(20)                                                                     NOT NULL AUTO_INCREMENT,
    `amount_to_be_paid`               int(11)                                                                        NOT NULL DEFAULT 0,
    `amount_to_be_taken_into_account` int(11)                                                                        NOT NULL DEFAULT 0,
    `canceled`                        tinyint(1)                                                                     NOT NULL DEFAULT 0,
    `ca`                              enum ('CA','CALLEBASE','CA_DEPOT','TO_IGNORE')                                 NOT NULL,
    `cmu_amount`                      int(11)                                                                        NOT NULL DEFAULT 0,
    `commentaire`                     varchar(255)                                                                            DEFAULT NULL,
    `copy`                            tinyint(1)                                                                     NOT NULL DEFAULT 0,
    `cost_amount`                     int(11)                                                                        NOT NULL DEFAULT 0,
    `created_at`                      datetime(6)                                                                    NOT NULL,
    `differe`                         tinyint(1)                                                                     NOT NULL DEFAULT 0,
    `discount_amount`                 int(11)                                                                        NOT NULL DEFAULT 0,
    `discount_amount_hors_ug`         int(11)                                                                        NOT NULL DEFAULT 0,
    `discount_amount_ug`              int(11)                                                                        NOT NULL DEFAULT 0,
    `effective_update_date`           datetime(6)                                                                    NOT NULL,
    `ht_amount`                       int(11)                                                                        NOT NULL DEFAULT 0,
    `ht_amount_ug`                    int(11)                                                                        NOT NULL DEFAULT 0,
    `imported`                        tinyint(1)                                                                     NOT NULL DEFAULT 0,
    `marge_ug`                        int(11)                                                                                 DEFAULT NULL,
    `monnaie`                         int(11)                                                                        NOT NULL DEFAULT 0,
    `montant_tva_ug`                  int(11)                                                                                 DEFAULT 0,
    `montant_net_ug`                  int(11)                                                                                 DEFAULT 0,
    `montant_ttc_ug`                  int(11)                                                                                 DEFAULT 0,
    `nature_vente`                    enum ('ASSURANCE','CARNET','COMPTANT')                                         NOT NULL,
    `net_amount`                      int(11)                                                                        NOT NULL DEFAULT 0,
    `net_ug_amount`                   int(11)                                                                        NOT NULL DEFAULT 0,
    `number_transaction`              varchar(255)                                                                   NOT NULL,
    `origine_vente`                   tinyint(4)                                                                     NOT NULL CHECK (`origine_vente` between 0 and 2),
    `payment_status`                  enum ('ALL','IMPAYE','PAYE')                                                   NOT NULL,
    `payroll_amount`                  int(11)                                                                        NOT NULL DEFAULT 0,
    `rest_to_pay`                     int(11)                                                                        NOT NULL DEFAULT 0,
    `sales_amount`                    int(11)                                                                        NOT NULL DEFAULT 0,
    `statut`                          enum ('ACTIVE','CANCELED','CLOSED','DESABLED','PENDING','PROCESSING','REMOVE') NOT NULL,
    `statut_caisse`                   tinyint(4)                                                                     NOT NULL CHECK (`statut_caisse` between 0 and 6),
    `tax_amount`                      int(11)                                                                        NOT NULL DEFAULT 0,
    `to_ignore`                       tinyint(1)                                                                     NOT NULL DEFAULT 0,
    `tva_embeded`                     varchar(100)                                                                            DEFAULT NULL,
    `type_prescription`               enum ('CONSEIL','DEPOT','PRESCRIPTION')                                        NOT NULL,
    `updated_at`                      datetime(6)                                                                    NOT NULL,
    `num_bon`                         varchar(50)                                                                             DEFAULT NULL,
    `part_assure`                     int(11)                                                                                 DEFAULT 0,
    `part_tiers_payant`               int(11)                                                                                 DEFAULT 0,
    `avoir_id`                        bigint(20)                                                                              DEFAULT NULL,
    `caisse_id`                       bigint(20)                                                                              DEFAULT NULL,
    `calendar_work_day`               date                                                                           NOT NULL,
    `canceled_sale_id`                bigint(20)                                                                              DEFAULT NULL,
    `cash_register_id`                bigint(20)                                                                              DEFAULT NULL,
    `cassier_id`                      bigint(20)                                                                     NOT NULL,
    `customer_id`                     bigint(20)                                                                              DEFAULT NULL,
    `last_caisse_id`                  bigint(20)                                                                              DEFAULT NULL,
    `last_user_edit_id`               bigint(20)                                                                     NOT NULL,
    `magasin_id`                      bigint(20)                                                                     NOT NULL,
    `remise_id`                       bigint(20)                                                                              DEFAULT NULL,
    `seller_id`                       bigint(20)                                                                     NOT NULL,
    `user_id`                         bigint(20)                                                                     NOT NULL,
    `ayant_droit_id`                  bigint(20)                                                                              DEFAULT NULL,
    `depot_id`                        bigint(20)                                                                              DEFAULT NULL,
    `depot_agree_id`                  bigint(20)                                                                              DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `vente_statut_index` (`statut`),
    KEY `vente_number_transaction_index` (`number_transaction`),
    KEY `vente_created_at_index` (`created_at`),
    KEY `vente_updated_at_index` (`updated_at`),
    KEY `vente_effective_update_index` (`effective_update_date`),
    KEY `vente_to_ignore_index` (`to_ignore`),
    KEY `vente_payment_status_index` (`payment_status`),
    KEY `vente_nature_vente_index` (`nature_vente`),
    KEY `FK1dkprbd8yrrmpeeqi57dy39ox` (`avoir_id`),
    KEY `FKol9pmxkqx60x3slvu2ijoex43` (`caisse_id`),
    KEY `FK1k4ojfo1vl8wn6o0a4sgvi2oo` (`calendar_work_day`),
    KEY `FK1vjccyu9gpb80n5l8b58q9xcj` (`canceled_sale_id`),
    KEY `FK8d36jdbjf6kif9hexfg1321s` (`cash_register_id`),
    KEY `FKctt2nduttdge3kvjefuxr6689` (`cassier_id`),
    KEY `FK72ep16wuoj7nllumicmk2ie3s` (`customer_id`),
    KEY `FK7wj3u1v9ll6qu8ju53142afm6` (`last_caisse_id`),
    KEY `FKhoe19wv2k8i34v9eb8o3g2k4m` (`last_user_edit_id`),
    KEY `FKd98rul87ffrih39pgn4xh0x3i` (`magasin_id`),
    KEY `FKjjnyagbe68u4fn7k5btsuqqoh` (`remise_id`),
    KEY `FKaoq0nuq3h1e1d1swcv1i0knc2` (`seller_id`),
    KEY `FKu5lyewcf0mgbldqrf8rhmjf6` (`user_id`),
    KEY `FKpyxoaae6i92epy849wvdfdlke` (`ayant_droit_id`),
    KEY `FK21r69rm6qvqoecj2qn8dodi7w` (`depot_id`),
    KEY `FK8jn843nshbh0rxx99oil0xbev` (`depot_agree_id`),
    CONSTRAINT `FK1dkprbd8yrrmpeeqi57dy39ox` FOREIGN KEY (`avoir_id`) REFERENCES `avoir` (`id`),
    CONSTRAINT `FK1k4ojfo1vl8wn6o0a4sgvi2oo` FOREIGN KEY (`calendar_work_day`) REFERENCES `warehouse_calendar` (`work_day`),
    CONSTRAINT `FK1vjccyu9gpb80n5l8b58q9xcj` FOREIGN KEY (`canceled_sale_id`) REFERENCES `sales` (`id`),
    CONSTRAINT `FK21r69rm6qvqoecj2qn8dodi7w` FOREIGN KEY (`depot_id`) REFERENCES `magasin` (`id`),
    CONSTRAINT `FK72ep16wuoj7nllumicmk2ie3s` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`),
    CONSTRAINT `FK7wj3u1v9ll6qu8ju53142afm6` FOREIGN KEY (`last_caisse_id`) REFERENCES `poste` (`id`),
    CONSTRAINT `FK8d36jdbjf6kif9hexfg1321s` FOREIGN KEY (`cash_register_id`) REFERENCES `cash_register` (`id`),
    CONSTRAINT `FK8jn843nshbh0rxx99oil0xbev` FOREIGN KEY (`depot_agree_id`) REFERENCES `magasin` (`id`),
    CONSTRAINT `FKaoq0nuq3h1e1d1swcv1i0knc2` FOREIGN KEY (`seller_id`) REFERENCES `user` (`id`),
    CONSTRAINT `FKctt2nduttdge3kvjefuxr6689` FOREIGN KEY (`cassier_id`) REFERENCES `user` (`id`),
    CONSTRAINT `FKd98rul87ffrih39pgn4xh0x3i` FOREIGN KEY (`magasin_id`) REFERENCES `magasin` (`id`),
    CONSTRAINT `FKhoe19wv2k8i34v9eb8o3g2k4m` FOREIGN KEY (`last_user_edit_id`) REFERENCES `user` (`id`),
    CONSTRAINT `FKjjnyagbe68u4fn7k5btsuqqoh` FOREIGN KEY (`remise_id`) REFERENCES `remise` (`id`),
    CONSTRAINT `FKol9pmxkqx60x3slvu2ijoex43` FOREIGN KEY (`caisse_id`) REFERENCES `poste` (`id`),
    CONSTRAINT `FKpyxoaae6i92epy849wvdfdlke` FOREIGN KEY (`ayant_droit_id`) REFERENCES `customer` (`id`),
    CONSTRAINT `FKu5lyewcf0mgbldqrf8rhmjf6` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.sales : ~0 rows (environ)

-- Listage de la structure de la table warehouse. sales_line
CREATE TABLE IF NOT EXISTS `sales_line`
(
    `id`                              bigint(20)  NOT NULL AUTO_INCREMENT,
    `after_stock`                     int(11)              DEFAULT NULL,
    `amount_to_be_taken_into_account` int(11)     NOT NULL DEFAULT 0,
    `cmu_amount`                      int(11)     NOT NULL DEFAULT 0,
    `cost_amount`                     int(11)     NOT NULL DEFAULT 0,
    `created_at`                      datetime(6) NOT NULL,
    `discount_amount`                 int(11)     NOT NULL DEFAULT 0,
    `discount_amount_hors_ug`         int(11)     NOT NULL DEFAULT 0,
    `discount_amount_ug`              int(11)     NOT NULL DEFAULT 0,
    `discount_unit_price`             int(11)     NOT NULL DEFAULT 0,
    `effective_update_date`           datetime(6) NOT NULL,
    `ht_amount`                       int(11)     NOT NULL DEFAULT 0,
    `init_stock`                      int(11)              DEFAULT NULL,
    `montant_tva_ug`                  int(11)     NOT NULL DEFAULT 0,
    `net_amount`                      int(11)     NOT NULL DEFAULT 0,
    `net_unit_price`                  int(11)     NOT NULL DEFAULT 0,
    `quantity_avoir`                  int(11)     NOT NULL DEFAULT 0,
    `quantity_requested`              int(11)     NOT NULL,
    `quantity_sold`                   int(11)     NOT NULL,
    `quantity_ug`                     int(11)     NOT NULL DEFAULT 0,
    `regular_unit_price`              int(11)     NOT NULL DEFAULT 0,
    `sales_amount`                    int(11)     NOT NULL DEFAULT 0,
    `tax_amount`                      int(11)     NOT NULL DEFAULT 0,
    `tax_value`                       int(11)     NOT NULL DEFAULT 0,
    `to_ignore`                       bit(1)      NOT NULL,
    `updated_at`                      datetime(6) NOT NULL,
    `produit_id`                      bigint(20)  NOT NULL,
    `sales_id`                        bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKony71tc7l1kgdmant1eqockbv` (`produit_id`, `sales_id`),
    KEY `FK2rpcx9v572xhylfle13e130w6` (`sales_id`),
    CONSTRAINT `FK2rpcx9v572xhylfle13e130w6` FOREIGN KEY (`sales_id`) REFERENCES `sales` (`id`),
    CONSTRAINT `FKg41n8hm3d58j50hsogv0vv2er` FOREIGN KEY (`produit_id`) REFERENCES `produit` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.sales_line : ~0 rows (environ)

-- Listage de la structure de la table warehouse. stock_produit
CREATE TABLE IF NOT EXISTS `stock_produit`
(
    `id`               bigint(20)  NOT NULL AUTO_INCREMENT,
    `created_at`       datetime(6) NOT NULL,
    `last_modified_by` varchar(50) DEFAULT NULL,
    `qty_stock`        int(11)     NOT NULL,
    `qty_ug`           int(11)     NOT NULL CHECK (`qty_ug` >= 0),
    `qty_virtual`      int(11)     NOT NULL,
    `updated_at`       datetime(6) NOT NULL,
    `produit_id`       bigint(20)  NOT NULL,
    `storage_id`       bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK209l3yi42tlex9i3yft8nb4n9` (`storage_id`, `produit_id`),
    KEY `FK1lnsn9h1evyxwnnktixqo2wc` (`produit_id`),
    CONSTRAINT `FK1lnsn9h1evyxwnnktixqo2wc` FOREIGN KEY (`produit_id`) REFERENCES `produit` (`id`),
    CONSTRAINT `FK8kuouqq6nv3lx0eb3wd73kf95` FOREIGN KEY (`storage_id`) REFERENCES `storage` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.stock_produit : ~0 rows (environ)

-- Listage de la structure de la table warehouse. stock_produit_aud
CREATE TABLE IF NOT EXISTS `stock_produit_aud`
(
    `id`               bigint(20) NOT NULL,
    `rev`              int(11)    NOT NULL,
    `revtype`          tinyint(4)  DEFAULT NULL,
    `created_at`       datetime(6) DEFAULT NULL,
    `last_modified_by` varchar(50) DEFAULT NULL,
    `qty_stock`        int(11)     DEFAULT NULL,
    `qty_ug`           int(11)     DEFAULT NULL,
    `qty_virtual`      int(11)     DEFAULT NULL,
    `updated_at`       datetime(6) DEFAULT NULL,
    `produit_id`       bigint(20)  DEFAULT NULL,
    `storage_id`       bigint(20)  DEFAULT NULL,
    PRIMARY KEY (`rev`, `id`),
    CONSTRAINT `FK260o2tvdlkglda2ab2tt1r60l` FOREIGN KEY (`rev`) REFERENCES `revinfo` (`rev`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.stock_produit_aud : ~0 rows (environ)

-- Listage de la structure de la table warehouse. storage
CREATE TABLE IF NOT EXISTS `storage`
(
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT,
    `name`         varchar(255) NOT NULL,
    `storage_type` tinyint(4)   NOT NULL CHECK (`storage_type` between 0 and 2),
    `magasin_id`   bigint(20)   NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK5fe37ity4pov1usxcqr3b03nd` (`name`),
    KEY `FKn1jj5tjhmgepsc4nwqsf1db9r` (`magasin_id`),
    CONSTRAINT `FKn1jj5tjhmgepsc4nwqsf1db9r` FOREIGN KEY (`magasin_id`) REFERENCES `magasin` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.storage : ~0 rows (environ)

-- Listage de la structure de la table warehouse. store_inventory
CREATE TABLE IF NOT EXISTS `store_inventory`
(
    `id`                         bigint(20)  NOT NULL AUTO_INCREMENT,
    `created_at`                 datetime(6) NOT NULL,
    `gap_amount`                 int(11)    DEFAULT NULL,
    `gap_cost`                   int(11)    DEFAULT NULL,
    `inventory_amount_after`     bigint(20)  NOT NULL,
    `inventory_amount_begin`     bigint(20)  NOT NULL,
    `inventory_category`         tinyint(4)  NOT NULL CHECK (`inventory_category` between 0 and 3),
    `inventory_type`             tinyint(4)  NOT NULL CHECK (`inventory_type` between 0 and 1),
    `inventory_value_cost_after` bigint(20)  NOT NULL,
    `inventory_value_cost_begin` bigint(20)  NOT NULL,
    `statut`                     tinyint(4)  NOT NULL CHECK (`statut` between 0 and 2),
    `updated_at`                 datetime(6) NOT NULL,
    `calendar_work_day`          date        NOT NULL,
    `rayon_id`                   bigint(20) DEFAULT NULL,
    `storage_id`                 bigint(20) DEFAULT NULL,
    `user_id`                    bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FKoaajc2cnjubfvqy0m9lc3elxj` (`calendar_work_day`),
    KEY `FKnwue82ru709xwwxkj5if2v6qq` (`rayon_id`),
    KEY `FK88teyake2uy873xolysdpwfl7` (`storage_id`),
    KEY `FK6gntnjhmeu8dahq204wmk1a2r` (`user_id`),
    CONSTRAINT `FK6gntnjhmeu8dahq204wmk1a2r` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `FK88teyake2uy873xolysdpwfl7` FOREIGN KEY (`storage_id`) REFERENCES `storage` (`id`),
    CONSTRAINT `FKnwue82ru709xwwxkj5if2v6qq` FOREIGN KEY (`rayon_id`) REFERENCES `rayon` (`id`),
    CONSTRAINT `FKoaajc2cnjubfvqy0m9lc3elxj` FOREIGN KEY (`calendar_work_day`) REFERENCES `warehouse_calendar` (`work_day`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.store_inventory : ~0 rows (environ)

-- Listage de la structure de la table warehouse. store_inventory_line
CREATE TABLE IF NOT EXISTS `store_inventory_line`
(
    `id`                   bigint(20)  NOT NULL AUTO_INCREMENT,
    `gap`                  int(11) DEFAULT NULL,
    `inventory_value_cost` int(11) DEFAULT NULL,
    `last_unit_price`      int(11) DEFAULT NULL,
    `quantity_init`        int(11) DEFAULT NULL,
    `quantity_on_hand`     int(11) DEFAULT NULL,
    `quantity_sold`        int(11) DEFAULT NULL,
    `updated`              bit(1)      NOT NULL,
    `updated_at`           datetime(6) NOT NULL,
    `produit_id`           bigint(20)  NOT NULL,
    `store_inventory_id`   bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKhidvm20io56axybnk34jqvs4c` (`produit_id`, `store_inventory_id`),
    KEY `FKoe713l7vns3jb1eo2uhniy4q3` (`store_inventory_id`),
    CONSTRAINT `FKg8d5ld2v2vy7tr54mwar1rh9` FOREIGN KEY (`produit_id`) REFERENCES `produit` (`id`),
    CONSTRAINT `FKoe713l7vns3jb1eo2uhniy4q3` FOREIGN KEY (`store_inventory_id`) REFERENCES `store_inventory` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.store_inventory_line : ~0 rows (environ)

-- Listage de la structure de la table warehouse. tableau
CREATE TABLE IF NOT EXISTS `tableau`
(
    `id`     bigint(20)   NOT NULL AUTO_INCREMENT,
    `code`   varchar(255) NOT NULL,
    `valeur` int(11)      NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKdhugqone5nf7sq7t9c178lfb5` (`code`),
    KEY `code_index` (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.tableau : ~0 rows (environ)

-- Listage de la structure de la table warehouse. third_party_sale_line
CREATE TABLE IF NOT EXISTS `third_party_sale_line`
(
    `id`                      bigint(20)                                          NOT NULL AUTO_INCREMENT,
    `created_at`              datetime(6)                                         NOT NULL,
    `effective_update_date`   datetime(6)                                         NOT NULL,
    `montant`                 int(11)                                             NOT NULL,
    `montant_regle`           int(11)     DEFAULT NULL,
    `num_bon`                 varchar(50) DEFAULT NULL,
    `statut`                  enum ('ACTIF','CLOSED','DELETE','HALF_PAID','PAID') NOT NULL,
    `taux`                    smallint(6)                                         NOT NULL,
    `updated_at`              datetime(6)                                         NOT NULL,
    `client_tiers_payant_id`  bigint(20)                                          NOT NULL,
    `facture_tiers_payant_id` bigint(20)  DEFAULT NULL,
    `sale_id`                 bigint(20)                                          NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKdq87q92pm83m8suaievy9uhu0` (`client_tiers_payant_id`, `sale_id`),
    KEY `FK8ybq48g2pr953xk6r5ospdkjw` (`facture_tiers_payant_id`),
    KEY `FKn8mjv4h5y4o993349xkyd2bs0` (`sale_id`),
    CONSTRAINT `FK8ybq48g2pr953xk6r5ospdkjw` FOREIGN KEY (`facture_tiers_payant_id`) REFERENCES `facture_tiers_payant` (`id`),
    CONSTRAINT `FKn8mjv4h5y4o993349xkyd2bs0` FOREIGN KEY (`sale_id`) REFERENCES `sales` (`id`),
    CONSTRAINT `FKs9p5hj252j6gdbewvphdbnw9y` FOREIGN KEY (`client_tiers_payant_id`) REFERENCES `client_tiers_payant` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.third_party_sale_line : ~0 rows (environ)

-- Listage de la structure de la table warehouse. ticket
CREATE TABLE IF NOT EXISTS `ticket`
(
    `code`              varchar(50) NOT NULL,
    `canceled`          tinyint(1)  NOT NULL DEFAULT 0,
    `created_at`        datetime(6) NOT NULL,
    `montant_attendu`   int(11)     NOT NULL,
    `montant_paye`      int(11)     NOT NULL,
    `montant_rendu`     int(11)     NOT NULL,
    `montant_verse`     int(11)     NOT NULL,
    `part_assure`       int(11)              DEFAULT 0,
    `part_tiers_payant` int(11)              DEFAULT 0,
    `rest_to_pay`       int(11)     NOT NULL,
    `tva`               varchar(100)         DEFAULT NULL,
    `customer_id`       bigint(20)           DEFAULT NULL,
    `sale_id`           bigint(20)           DEFAULT NULL,
    `user_id`           bigint(20)  NOT NULL,
    PRIMARY KEY (`code`),
    KEY `FKmli0eqrecnnqvdgv3kcx7d9m8` (`customer_id`),
    KEY `FKbnxaqkhaqf1h70d24ug7m80xb` (`sale_id`),
    KEY `FKdvt57mcco3ogsosi97odw563o` (`user_id`),
    CONSTRAINT `FKbnxaqkhaqf1h70d24ug7m80xb` FOREIGN KEY (`sale_id`) REFERENCES `sales` (`id`),
    CONSTRAINT `FKdvt57mcco3ogsosi97odw563o` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `FKmli0eqrecnnqvdgv3kcx7d9m8` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.ticket : ~0 rows (environ)

-- Listage de la structure de la table warehouse. ticketing
CREATE TABLE IF NOT EXISTS `ticketing`
(
    `id`                  bigint(20)  NOT NULL AUTO_INCREMENT,
    `created`             datetime(6) NOT NULL,
    `number_of1`          int(11)     NOT NULL,
    `number_of10`         int(11)     NOT NULL,
    `number_of100hundred` int(11)     NOT NULL,
    `number_of10thousand` int(11)     NOT NULL,
    `number_of1thousand`  int(11)     NOT NULL,
    `number_of200hundred` int(11)     NOT NULL,
    `number_of25`         int(11)     NOT NULL,
    `number_of2thousand`  int(11)     NOT NULL,
    `number_of5`          int(11)     NOT NULL,
    `number_of50`         int(11)     NOT NULL,
    `number_of500hundred` int(11)     NOT NULL,
    `number_of5thousand`  int(11)     NOT NULL,
    `other_amount`        int(11)     NOT NULL,
    `total_amount`        bigint(20)  NOT NULL,
    `cash_register_id`    bigint(20)  NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKck44ngcp2bue83xo845mbu962` (`cash_register_id`),
    CONSTRAINT `FKmq8ij1eptnvb7xprhsf8w3ys1` FOREIGN KEY (`cash_register_id`) REFERENCES `cash_register` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.ticketing : ~0 rows (environ)

-- Listage de la structure de la table warehouse. tiers_payant
CREATE TABLE IF NOT EXISTS `tiers_payant`
(
    `id`                     bigint(20)                          NOT NULL AUTO_INCREMENT,
    `adresse`                varchar(200)                                       DEFAULT NULL,
    `categorie`              enum ('ASSURANCE','CARNET','DEPOT') NOT NULL,
    `is_cmu`                 bit(1)                              NOT NULL,
    `code_organisme`         varchar(100)                                       DEFAULT NULL,
    `conso_mensuelle`        bigint(20)                                         DEFAULT NULL,
    `consommation_json`      longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`consommation_json`)),
    `created`                datetime(6)                         NOT NULL,
    `email`                  varchar(50)                                        DEFAULT NULL,
    `full_name`              varchar(200)                        NOT NULL,
    `model_facture`          varchar(20)                                        DEFAULT NULL,
    `montant_max_sur_fact`   bigint(20)                                         DEFAULT NULL,
    `name`                   varchar(150)                        NOT NULL,
    `nbre_bons_max_sur_fact` int(11)                                            DEFAULT NULL,
    `nbre_bordereau`         int(11)                             NOT NULL       DEFAULT 1,
    `plafond_absolu`         bit(1)                                             DEFAULT NULL,
    `plafond_conso`          bigint(20)                                         DEFAULT NULL,
    `remise_forfaitaire`     bigint(20)                                         DEFAULT NULL,
    `statut`                 tinyint(4)                          NOT NULL CHECK (`statut` between 0 and 2),
    `telephone`              varchar(15)                                        DEFAULT NULL,
    `telephone_fixe`         varchar(15)                                        DEFAULT NULL,
    `to_be_exclude`          tinyint(1)                                         DEFAULT 0,
    `updated`                datetime(6)                         NOT NULL,
    `use_referenced_rrice`   bit(1)                                             DEFAULT NULL,
    `groupe_tiers_payant_id` bigint(20)                                         DEFAULT NULL,
    `updated_by_id`          bigint(20)                                         DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKsmbw04nasi7yrcjwcg95tsh6t` (`name`),
    UNIQUE KEY `UK3moh75ah5g6ce1dm9n28psmvo` (`full_name`),
    KEY `FKeq4f34e6hgun4oad7iuxnnrs7` (`groupe_tiers_payant_id`),
    KEY `FKj72u7uuhf7tar5ecub95sr6sa` (`updated_by_id`),
    CONSTRAINT `FKeq4f34e6hgun4oad7iuxnnrs7` FOREIGN KEY (`groupe_tiers_payant_id`) REFERENCES `groupe_tiers_payant` (`id`),
    CONSTRAINT `FKj72u7uuhf7tar5ecub95sr6sa` FOREIGN KEY (`updated_by_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.tiers_payant : ~0 rows (environ)

-- Listage de la structure de la table warehouse. tva
CREATE TABLE IF NOT EXISTS `tva`
(
    `id`   bigint(20) NOT NULL AUTO_INCREMENT,
    `taux` int(11)    NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKsx5rjlj9hynan0fr5tugf2ycv` (`taux`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.tva : ~0 rows (environ)

-- Listage de la structure de la table warehouse. type_etiquette
CREATE TABLE IF NOT EXISTS `type_etiquette`
(
    `id`      bigint(20)   NOT NULL AUTO_INCREMENT,
    `libelle` varchar(255) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKnrxyveu79xqb48la1r8hdrun9` (`libelle`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.type_etiquette : ~0 rows (environ)

-- Listage de la structure de la table warehouse. user
CREATE TABLE IF NOT EXISTS `user`
(
    `id`                   bigint(20)  NOT NULL AUTO_INCREMENT,
    `created_by`           varchar(50)  DEFAULT NULL,
    `created_date`         datetime(6)  DEFAULT NULL,
    `last_modified_by`     varchar(50)  DEFAULT NULL,
    `last_modified_date`   datetime(6)  DEFAULT NULL,
    `action_authority_key` varchar(255) DEFAULT NULL,
    `activated`            bit(1)      NOT NULL,
    `activation_key`       varchar(20)  DEFAULT NULL,
    `email`                varchar(254) DEFAULT NULL,
    `first_name`           varchar(50)  DEFAULT NULL,
    `image_url`            varchar(256) DEFAULT NULL,
    `lang_key`             varchar(10)  DEFAULT NULL,
    `last_name`            varchar(50)  DEFAULT NULL,
    `login`                varchar(50) NOT NULL,
    `password_hash`        varchar(60) NOT NULL,
    `reset_date`           datetime(6)  DEFAULT NULL,
    `reset_key`            varchar(20)  DEFAULT NULL,
    `magasin_id`           bigint(20)  NOT NULL,
    `printer_id`           bigint(20)   DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKew1hvam8uwaknuaellwhqchhb` (`login`),
    UNIQUE KEY `UKob8kqyqqgmefl0aco34akdtpe` (`email`),
    KEY `FK4g7bc724viqykr1u3tjnddf4c` (`magasin_id`),
    KEY `FKgyjkk8y07medpp3x0ru5etgno` (`printer_id`),
    CONSTRAINT `FK4g7bc724viqykr1u3tjnddf4c` FOREIGN KEY (`magasin_id`) REFERENCES `magasin` (`id`),
    CONSTRAINT `FKgyjkk8y07medpp3x0ru5etgno` FOREIGN KEY (`printer_id`) REFERENCES `printer` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.user : ~0 rows (environ)

-- Listage de la structure de la table warehouse. user_authority
CREATE TABLE IF NOT EXISTS `user_authority`
(
    `user_id`        bigint(20)  NOT NULL,
    `authority_name` varchar(50) NOT NULL,
    PRIMARY KEY (`user_id`, `authority_name`),
    KEY `FK6ktglpl5mjosa283rvken2py5` (`authority_name`),
    CONSTRAINT `FK6ktglpl5mjosa283rvken2py5` FOREIGN KEY (`authority_name`) REFERENCES `authority` (`name`),
    CONSTRAINT `FKpqlsjpkybgos9w2svcri7j8xy` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.user_authority : ~0 rows (environ)

-- Listage de la structure de la table warehouse. utilisation_cle_securite
CREATE TABLE IF NOT EXISTS `utilisation_cle_securite`
(
    `id`                    bigint(20)   NOT NULL AUTO_INCREMENT,
    `caisse`                varchar(255) NOT NULL,
    `commentaire`           varchar(255) DEFAULT NULL,
    `entity_id`             bigint(20)   DEFAULT NULL,
    `entity_name`           varchar(255) DEFAULT NULL,
    `mvt_date`              datetime(6)  NOT NULL,
    `cle_securite_owner_id` bigint(20)   NOT NULL,
    `connected_user_id`     bigint(20)   NOT NULL,
    `privilege_name`        varchar(100) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FK2atxw2tml077su8hsoeaot6a0` (`cle_securite_owner_id`),
    KEY `FK7ar2ex342qykfi2r6i7fcdcgk` (`connected_user_id`),
    KEY `FKn7rvgfahogx5tsdpa5yvtc9s6` (`privilege_name`),
    CONSTRAINT `FK2atxw2tml077su8hsoeaot6a0` FOREIGN KEY (`cle_securite_owner_id`) REFERENCES `user` (`id`),
    CONSTRAINT `FK7ar2ex342qykfi2r6i7fcdcgk` FOREIGN KEY (`connected_user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `FKn7rvgfahogx5tsdpa5yvtc9s6` FOREIGN KEY (`privilege_name`) REFERENCES `privilege` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.utilisation_cle_securite : ~0 rows (environ)

-- Listage de la structure de la table warehouse. warehouse_calendar
CREATE TABLE IF NOT EXISTS `warehouse_calendar`
(
    `work_day`   date    NOT NULL,
    `work_month` int(11) NOT NULL,
    `work_year`  int(11) NOT NULL,
    PRIMARY KEY (`work_day`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.warehouse_calendar : ~0 rows (environ)

-- Listage de la structure de la table warehouse. warehouse_sequence
CREATE TABLE IF NOT EXISTS `warehouse_sequence`
(
    `name`      varchar(255) NOT NULL,
    `increment` int(4)  DEFAULT 1,
    `seq_value` int(11) DEFAULT 0,
    PRIMARY KEY (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3
  COLLATE = utf8mb3_general_ci;

-- Listage des données de la table warehouse.warehouse_sequence : ~0 rows (environ)

/*!40103 SET TIME_ZONE = IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE = IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS = IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT = @OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES = IFNULL(@OLD_SQL_NOTES, 1) */;
