-- =====================================================================================
-- Module « Declaration du CA » : ponction et navigation
-- =====================================================================================


create extension if not exists btree_gist;


-- Identifiants attribues par sequence nommee, comme les autres entites du domaine
-- (cf. AbstractIdGeneratorService) : la colonne reste un integer simple.
create sequence if not exists id_ca_ponction_seq start with 1 increment by 1;


-- Un traitement de ponction. Une ligne = une decision du pharmacien.
create table if not exists ca_ponction (
  id                    integer primary key,
  magasin_id            integer        not null references magasin (id),
  date_debut            date          not null,
  date_fin              date          not null,
  mode_calcul           varchar(15)   not null,
  valeur_saisie         numeric(12, 2) not null,
  plafond_par_vente     numeric(5, 2) not null default 35.00,
  strategie             varchar(15)   not null default 'DECROISSANT',
  modes_reglement       varchar(80)   not null default 'CASH',
  taux_tva_eligibles    varchar(40)   not null default '0',
  ca_reel               bigint        not null default 0,
  ca_apres_exclusions   bigint        not null default 0,
  ca_assiette_tva0      bigint        not null default 0,
  montant_ponctionnable bigint        not null default 0,
  montant_objectif      bigint        not null default 0,
  montant_ponctionne    bigint        not null default 0,
  ca_declare            bigint        not null default 0,
  nombre_ventes         integer       not null default 0,
  statut                varchar(15)   not null,
  features_actives      varchar(120),
  commentaire           varchar(255),
  created_by            integer        not null references app_user (id),
  created_at            timestamp     not null default now(),
  validated_by          integer        references app_user (id),
  validated_at          timestamp,
  canceled_by           integer        references app_user (id),
  canceled_at           timestamp,
  constraint ca_ponction_periode_ck check (date_fin >= date_debut),
  constraint ca_ponction_plafond_ck check (plafond_par_vente > 0 and plafond_par_vente <= 100),
  constraint ca_ponction_mode_ck check (mode_calcul in ('MONTANT_FIXE', 'POURCENTAGE')),
  constraint ca_ponction_strategie_ck check (strategie in ('DECROISSANT', 'UNIFORME')),
  constraint ca_ponction_statut_ck check (statut in ('SIMULATION', 'VALIDEE', 'ANNULEE'))
);




-- Deux periodes actives ne peuvent pas se chevaucher. Une simulation ou une ponction
-- annulee n'occupe rien : seule VALIDEE reserve la periode.
alter table ca_ponction
  drop constraint if exists ca_ponction_periode_no_overlap;

alter table ca_ponction
  add constraint ca_ponction_periode_no_overlap
  exclude using gist (
    magasin_id with =,
    daterange(date_debut, date_fin, '[]') with &&
  ) where (statut = 'VALIDEE');


-- Le detail : ce qui a ete retire, vente par vente. Rend l'annulation exacte et la
-- ponction justifiable ligne a ligne.
create table if not exists ca_ponction_detail (
  ponction_id        integer  not null references ca_ponction (id) on delete cascade,
  sale_id            bigint  not null,
  sale_date          date    not null,
  montant_vente      integer not null,
  montant_base       integer not null,
  montant_ponctionne integer not null,
  rang               integer not null,
  -- Recopie de la vente a la validation : le justificatif est un document fige, il ne doit pas
  -- dependre d'une jointure sur une table partitionnee pour afficher une reference.
  numero_transaction varchar(20),
  primary key (ponction_id, sale_id, sale_date),
  constraint ca_ponction_detail_plafond_ck
    check (montant_ponctionne >= 0 and montant_ponctionne <= montant_base)
);

comment on column ca_ponction_detail.montant_vente is
  'Total de la vente : assiette du plafond par vente.';
comment on column ca_ponction_detail.montant_base is
  'Part a TVA 0 de la vente : assiette reellement ponctionnable.';

create index if not exists ca_ponction_detail_sale_idx
  on ca_ponction_detail (sale_id, sale_date);


-- Rattache une vente a la ponction qui l'a reduite. Rend l'historique lisible et
-- l'annulation triviale.
alter table sales
  add column if not exists ponction_id integer;

create index if not exists sales_ponction_idx on sales (ponction_id) where ponction_id is not null;


-- ============================ Navigation du module ============================


INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
VALUES ('declaration-ca', 'Retraitement du CA', 'pi pi-percentage', '/declaration-ca', NULL, 86, 2, 'ROUTE', TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'declaration-ca.exclusion-rayon', 'Exclusion de rayons', 'pi pi-th-large', NULL, id, 10, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'declaration-ca'
ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'declaration-ca.exclusion-tp', 'Exclusion de tiers-payants', 'pi pi-building', NULL, id, 20, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'declaration-ca'
ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'declaration-ca.parametres', 'Unités gratuites', 'pi pi-gift', NULL, id, 30, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'declaration-ca'
ON CONFLICT (code) DO NOTHING;


-- Chaque onglet n'apparait que si son module est souscrit.
UPDATE nav_item SET required_feature = 'EXCLUSION_RAYON' WHERE code = 'declaration-ca.exclusion-rayon';
UPDATE nav_item SET required_feature = 'EXCLUSION_TP'    WHERE code = 'declaration-ca.exclusion-tp';
UPDATE nav_item SET required_feature = 'EXCLUSION_UG'    WHERE code = 'declaration-ca.parametres';



INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item
WHERE code IN ('declaration-ca', 'declaration-ca.exclusion-rayon', 'declaration-ca.exclusion-tp', 'declaration-ca.parametres')
ON CONFLICT (nav_item_id, role_name) DO NOTHING;

-- ---------------------------- Ponction et historique ----------------------------

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'declaration-ca.ponction', 'Ponction', 'pi pi-percentage', NULL, id, 40, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'declaration-ca'
ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'declaration-ca.ponction-historique', 'Historique des ponctions', 'pi pi-history', NULL, id, 50, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'declaration-ca'
ON CONFLICT (code) DO NOTHING;

UPDATE nav_item SET required_feature = 'CALLEBASSE'
 WHERE code IN ('declaration-ca.ponction', 'declaration-ca.ponction-historique');

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item
WHERE code IN ('declaration-ca.ponction', 'declaration-ca.ponction-historique')
ON CONFLICT (nav_item_id, role_name) DO NOTHING;




INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'declaration-ca.balance-reelle', 'Balance caisse (CA encaissé)', 'pi pi-calculator', NULL, id, 60, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'declaration-ca'
ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'declaration-ca.taxe-report-reel', 'Rapport TVA (CA encaissé)', 'pi pi-file-pdf', NULL, id, 70, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'declaration-ca'
ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'declaration-ca.tableau-pharmacien-reel', 'Tableau pharmacien (CA encaissé)', 'pi pi-table', NULL, id, 80, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'declaration-ca'
ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item
WHERE code IN ('declaration-ca.balance-reelle', 'declaration-ca.taxe-report-reel', 'declaration-ca.tableau-pharmacien-reel')
ON CONFLICT (nav_item_id, role_name) DO NOTHING;




-- ---------------------------- Controle de coherence ----------------------------


INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'declaration-ca.audit', 'Contrôle de cohérence', 'pi pi-shield', NULL, id, 90, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'declaration-ca'
ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, TRUE
FROM nav_item WHERE code = 'declaration-ca.audit'
ON CONFLICT (nav_item_id, role_name) DO NOTHING;



-- ============================ Privileges du module ============================

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'pr-declaration-ca-exclusion', 'Gérer les exclusions du CA', NULL, NULL, id, 1, 4, 'ACTION', TRUE
FROM nav_item WHERE code = 'declaration-ca'
ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'pr-declaration-ca-ponction', 'Créer et annuler les ponctions', NULL, NULL, id, 2, 4, 'ACTION', TRUE
FROM nav_item WHERE code = 'declaration-ca'
ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'pr-declaration-ca-audit', 'Consulter le contrôle de cohérence', NULL, NULL, id, 3, 4, 'ACTION', TRUE
FROM nav_item WHERE code = 'declaration-ca'
ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item
WHERE code IN ('pr-declaration-ca-exclusion', 'pr-declaration-ca-ponction', 'pr-declaration-ca-audit')
ON CONFLICT (nav_item_id, role_name) DO NOTHING;



INSERT INTO app_configuration (name, value, description, value_type)
VALUES ('APP_PONCTION_ANNULATION_MAX_DAYS', '1',
        'Délai en jours pendant lequel une ponction validée reste annulable', 'NUMBER')
ON CONFLICT (name) DO NOTHING;




INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'declaration-ca.journal-tp', 'Ventes tiers-payant exclues', 'pi pi-building', NULL, id, 31, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'declaration-ca'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'declaration-ca.journal-ug', 'Unites gratuites vendues', 'pi pi-gift', NULL, id, 32, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'declaration-ca'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'declaration-ca.journal-rayon', 'Produits de rayons exclus', 'pi pi-th-large', NULL, id, 33, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'declaration-ca'
  ON CONFLICT (code) DO NOTHING;


-- Chaque journal depend du module qui l'alimente : sans le module souscrit,
-- aucune ligne n'a pu etre ecartee et le menu n'aurait rien a montrer.
UPDATE nav_item SET required_feature = 'EXCLUSION_TP'    WHERE code = 'declaration-ca.journal-tp';
UPDATE nav_item SET required_feature = 'EXCLUSION_UG'    WHERE code = 'declaration-ca.journal-ug';
UPDATE nav_item SET required_feature = 'EXCLUSION_RAYON' WHERE code = 'declaration-ca.journal-rayon';



INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item
WHERE code IN ('declaration-ca.journal-tp', 'declaration-ca.journal-ug', 'declaration-ca.journal-rayon')
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;


-- Les requetes filtrent sur le motif et la periode. sales_line etant partitionnee
-- par sale_date, l'index partiel ne porte que sur les lignes retraitees : elles
-- sont une petite minorite, et l'index reste des lors tres compact.
CREATE INDEX IF NOT EXISTS sales_line_exclusion_motif_idx
  ON sales_line (exclusion_motif, sale_date)
  WHERE exclusion_motif IS NOT NULL;


ALTER TABLE sales_line
  ADD CONSTRAINT sales_line_declarable_ck
    CHECK (amount_to_be_taken_into_account IS NULL
      OR (amount_to_be_taken_into_account
        BETWEEN LEAST(0, quantity_requested * regular_unit_price)
        AND GREATEST(0, quantity_requested * regular_unit_price)))
  NOT VALID;


-- Plafond par defaut d'une ponction, en pourcentage du montant d'une vente.
-- Surchargeable a chaque ponction depuis l'ecran.
INSERT INTO app_configuration (name, value, description, value_type)
VALUES ('APP_PONCTION_PLAFOND_DEFAUT', '35',
        'Part maximale d''une vente qu''une ponction peut retirer, en pourcentage', 'NUMBER')
  ON CONFLICT (name) DO NOTHING;

