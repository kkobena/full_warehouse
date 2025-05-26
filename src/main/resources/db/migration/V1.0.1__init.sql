create table authority
(
  name    varchar(50)  not null
    primary key,
  libelle varchar(100) null
);

create table banque
(
  id           bigint auto_increment
    primary key,
  adresse      varchar(255) null,
  beneficiaire varchar(255) null,
  code         varchar(100) null,
  nom          varchar(100) not null
);

create table categorie
(
  id      bigint auto_increment
    primary key,
  code    varchar(255) null,
  libelle varchar(255) not null,
  constraint UK201klrwuww0os41kte46ac6lq
    unique (libelle)
);

create table dci
(
  id      bigint auto_increment
    primary key,
  code    varchar(20)  not null,
  libelle varchar(255) not null,
  constraint UKe3c6g8lyasluveuhuckir2lod
    unique (code),
  constraint UKitjoajb16gqh53rhwby9jeoh4
    unique (libelle)
);

create index dci_libelle_index
  on dci (libelle);

create table famille_produit
(
  id           bigint auto_increment
    primary key,
  code         varchar(255) null,
  libelle      varchar(255) not null,
  categorie_id bigint       not null,
  constraint UK5qnbuviywmlttiympkaceoo4s
    unique (libelle),
  constraint FKkl1kdhwi96mrybwm5hw8sofhv
    foreign key (categorie_id) references categorie (id)
);

create table form_produit
(
  id      bigint auto_increment
    primary key,
  libelle varchar(255) not null,
  constraint UKdebj7ueu4wsokhi8ptd8emwht
    unique (libelle)
);

create table gamme_produit
(
  id      bigint auto_increment
    primary key,
  code    varchar(255) null,
  libelle varchar(255) not null,
  constraint UK1r9jeo0jvdg5pjhyvl2gnf2do
    unique (libelle)
);

create table groupe_fournisseur
(
  id             bigint auto_increment
    primary key,
  addresspostale varchar(255) null,
  email          varchar(255) null,
  libelle        varchar(255) not null,
  num_faxe       varchar(255) null,
  odre           int          not null,
  tel            varchar(255) null,
  constraint UKp0jg383si04bm4we9c61c6mn
    unique (libelle)
);

create table fournisseur
(
  id                      bigint auto_increment
    primary key,
  addresse_postal         varchar(255) null,
  code                    varchar(70)  not null,
  identifiant_repartiteur varchar(255) null,
  libelle                 varchar(255) not null,
  mobile                  varchar(255) null,
  num_faxe                varchar(255) null,
  phone                   varchar(255) null,
  site                    varchar(255) null,
  groupe_fournisseur_id   bigint       not null,
  constraint UK97y5ak5ond8qgcml87p1wpk5e
    unique (libelle),
  constraint FK72cjbo0lulxc0wduppfw9qjyp
    foreign key (groupe_fournisseur_id) references groupe_fournisseur (id)
);

create table groupe_tiers_payant
(
  id                 bigint auto_increment
    primary key,
  adresse            varchar(200)                                                                              null,
  name               varchar(100)                                                                              not null,
  ordre_tris_facture enum ('CODE_FACTURE', 'DATE_FACTURE', 'DATE_FACTURE_DESC', 'MONTANT', 'NOM_TIER', 'TAUX') null,
  telephone          varchar(15)                                                                               null,
  telephone_fixe     varchar(15)                                                                               null,
  constraint UK7s14b63vib0vg70p8cl4gvyn1
    unique (name)
);

create table importation_echouee
(
  id          bigint auto_increment
    primary key,
  created     datetime(6) not null,
  is_commande bit         not null,
  object_id   bigint      null
);

create table importation_echouee_ligne
(
  id                    bigint auto_increment
    primary key,
  code_tva              int          null,
  date_peremption       date         null,
  prix_achat            int          null
    check (`prix_achat` >= 0),
  prix_un               int          null
    check (`prix_un` >= 0),
  produit_cip           varchar(255) null,
  produit_ean           varchar(255) null,
  quantity_received     int          null
    check (`quantity_received` >= 0),
  ug                    int          null
    check (`ug` >= 0),
  importation_echoue_id bigint       not null,
  constraint FK9q8p018ys16lk3c5pptgflx6w
    foreign key (importation_echoue_id) references importation_echouee (id)
);

create table laboratoire
(
  id      bigint auto_increment
    primary key,
  libelle varchar(255) not null,
  constraint UKch0c4o3olc65u3yap006nxq0i
    unique (libelle)
);

create table magasin
(
  id                  bigint auto_increment
    primary key,
  address             varchar(255)                               null,
  compte_bancaire     varchar(255)                               null,
  compte_contribuable varchar(255)                               null,
  email               varchar(255)                               null,
  full_name           varchar(255)                               not null,
  name                varchar(255)                               not null,
  note                varchar(255)                               null,
  num_comptable       varchar(255)                               null,
  phone               varchar(255)                               null,
  registre            varchar(255)                               null,
  registre_imposition varchar(255)                               null,
  type_magasin        enum ('DEPOT', 'DEPOT_AGGREE', 'OFFICINE') not null,
  welcome_message     varchar(255)                               null,
  constraint UKjj7fulne1dmx3boof0itbmj8n
    unique (name),
  constraint UKjn6wxi7t8rmrg6rbcemf4yr1h
    unique (full_name)
);

create table menu
(
  id               bigint auto_increment
    primary key,
  enable           bit                                not null,
  icon_java_client varchar(255)                       null,
  icon_web         varchar(255)                       null,
  libelle          varchar(255)                       not null,
  name             varchar(70)                        not null,
  ordre            int                                not null,
  racine           bit                                not null,
  type_menu        enum ('ALL', 'JAVA_CLIENT', 'WEB') not null,
  parent_id        bigint                             null,
  constraint UKm05sb1hgsv38qjb4ksyh5eat2
    unique (name),
  constraint FKgeupubdqncc1lpgf2cn4fqwbc
    foreign key (parent_id) references menu (id)
);

create table authority_menu
(
  authority_name varchar(50) not null,
  menu_id        bigint      not null,
  primary key (authority_name, menu_id),
  constraint FKldi65w2wsgdge8qfsjkmge2sl
    foreign key (menu_id) references menu (id),
  constraint FKrfrj2p0bpnosephpj61lmi9np
    foreign key (authority_name) references authority (name)
);

create table motif_ajustement
(
  id      bigint auto_increment
    primary key,
  libelle varchar(255) not null,
  constraint UKjcn7824hyqd0s0yml6cemqxj1
    unique (libelle)
);

create table motif_retour_produit
(
  id      bigint auto_increment
    primary key,
  libelle varchar(255) not null,
  constraint UKkhj7gugyhyfcgw17t02j2mxlr
    unique (libelle)
);

create table payment_mode
(
  code          varchar(50)                                                              not null
    primary key,
  enable        bit                                                                      not null,
  payment_group enum ('CASH', 'CAUTION', 'CB', 'CHEQUE', 'CREDIT', 'MOBILE', 'VIREMENT') not null,
  icon_url      varchar(255)                                                             null,
  libelle       varchar(255)                                                             not null,
  ordre_tri     smallint                                                                 not null,
  constraint UKtfqn29lfkm2lkujuptvoiiyki
    unique (libelle)
);

create table persistent_audit_event
(
  event_id   bigint auto_increment
    primary key,
  event_date datetime(6)  null,
  event_type varchar(255) null,
  principal  varchar(255) not null
);

create table persistent_audit_evt_data
(
  event_id bigint       not null,
  value    varchar(255) null,
  name     varchar(255) not null,
  primary key (event_id, name),
  constraint FK9ynvwlu7w4uqpjlxvk9kiscqs
    foreign key (event_id) references persistent_audit_event (event_id)
);

create table poste
(
  id           bigint auto_increment
    primary key,
  address      varchar(255) not null,
  name         varchar(255) not null,
  poste_number varchar(255) null,
  constraint UKk7d8e8fhttgpyrppl6unbvybx
    unique (name)
);

create index poste_name_index
  on poste (name);

create table printer
(
  id                    bigint auto_increment
    primary key,
  address               varchar(255)    null,
  length                int             not null,
  margin_left_and_right int default 10  not null,
  margin_top            int default 15  not null,
  name                  varchar(255)    not null,
  width                 int default 576 not null,
  poste_id              bigint          not null,
  constraint UKh8bxb2mqx8w24n0jh4vymkk0
    unique (name, poste_id),
  constraint UKrr7bfntxwxqe91dsa9458f582
    unique (name),
  constraint FKcip3p5i9uwm6mgqpsnrfk5vcu
    foreign key (poste_id) references poste (id)
);

create index name_index
  on printer (name);

create table privilege
(
  name    varchar(100) not null
    primary key,
  libelle varchar(255) not null,
  menu_id bigint       not null,
  constraint FKqckdmdk8jouw4r8o53uq88xlo
    foreign key (menu_id) references menu (id)
);

create table authority_privilege
(
  id             bigint auto_increment
    primary key,
  authority_name varchar(50)  not null,
  privilege_name varchar(100) not null,
  constraint UKpuc964qxatpndyv71vc7pds1f
    unique (privilege_name, authority_name),
  constraint FK5xavalsq2c8nngb47jbgsl7mj
    foreign key (privilege_name) references privilege (name),
  constraint FK9prep679f0xewrgodv1lqstl7
    foreign key (authority_name) references authority (name)
);

create table reference
(
  id             bigint auto_increment
    primary key,
  mvt_date       date         not null,
  num            varchar(255) not null,
  number_transac int          not null
    check (`number_transac` >= 0),
  d_type         int          not null,
  constraint UKpj0iecdrn6o15jiuqnbj5l80o
    unique (mvt_date, d_type, num)
);

create table remise
(
  dtype        varchar(31)          not null,
  id           bigint auto_increment
    primary key,
  enable       tinyint(1) default 1 not null,
  libelle      varchar(100)         null,
  remise_value float                null
);

create table customer
(
  dtype               varchar(31)                                     not null,
  id                  bigint auto_increment
    primary key,
  code                varchar(255)                                    not null,
  created_at          datetime(6)                                     not null,
  email               varchar(255)                                    null,
  first_name          varchar(255)                                    not null,
  last_name           varchar(255)                                    not null,
  phone               varchar(255)                                    null,
  status              enum ('CLOSED', 'DELETED', 'DISABLE', 'ENABLE') not null,
  type_assure         enum ('AYANT_DROIT', 'PRINCIPAL')               not null,
  updated_at          datetime(6)                                     not null,
  dat_naiss           date                                            null,
  num_ayant_droit     varchar(100)                                    null,
  sexe                varchar(255)                                    null,
  assure_principal_id bigint                                          null,
  remise_id           bigint                                          null,
  remise_client_id    bigint                                          null,
  constraint UKrm1bp9bhtiih5foj17t8l500j
    unique (code),
  constraint FK164wkcenb578dvt81hbag6dvp
    foreign key (remise_id) references remise (id),
  constraint FK8qi44u4eqwuqlvq67cmffahpw
    foreign key (remise_client_id) references remise (id),
  constraint FKb1cv4y9rq1gego4buaykin5wh
    foreign key (assure_principal_id) references customer (id)
);

create table customer_account
(
  id           bigint auto_increment
    primary key,
  account_type enum ('CARNET', 'CAUTION') not null,
  balance      int(8) default 0           not null,
  created_at   datetime(6)                not null,
  enabled      bit                        not null,
  updated_at   datetime(6)                not null,
  customer_id  bigint                     not null,
  constraint FK6c5oqutth35p5vmw0svg56msa
    foreign key (customer_id) references customer (id)
);

create table grille_remise
(
  id                bigint auto_increment
    primary key,
  code              enum ('CODE_12', 'CODE_13', 'CODE_14', 'CODE_15', 'CODE_16', 'CODE_17', 'CODE_18', 'CODE_19', 'CODE_20', 'CODE_21', 'CODE_22', 'CODE_23', 'CODE_24', 'CODE_25', 'CODE_26', 'CODE_27', 'CODE_28', 'CODE_29', 'NONE') not null,
  enable            tinyint(1) default 1                                                                                                                                                                                                not null,
  remise_value      float                                                                                                                                                                                                               not null,
  remise_produit_id bigint                                                                                                                                                                                                              not null,
  constraint grille_remise_code_un_index
    unique (code),
  constraint remise_produit_id_code_un_index
    unique (code, remise_produit_id),
  constraint FK7vy9l2sxkb174hmjojs1k3k1a
    foreign key (remise_produit_id) references remise (id)
);

create table revinfo
(
  rev      int auto_increment
    primary key,
  revtstmp bigint null
);

create table produit_aud
(
  id                      bigint                                          not null,
  rev                     int                                             not null,
  revtype                 tinyint                                         null,
  categorie               enum ('A', 'B', 'C')                            null,
  check_expiry_date       tinyint(1) default 0                            null,
  code_ean                varchar(255)                                    null,
  code_remise             varchar(6) default 'CODE_0'                     null comment 'Code de remise qui seront mappés sur les grilles de remises',
  cost_amount             int                                             null,
  created_at              datetime(6)                                     null,
  deconditionnable        bit                                             null,
  item_cost_amount        int                                             null,
  item_qty                int                                             null,
  item_regular_unit_price int                                             null,
  libelle                 varchar(255)                                    null,
  net_unit_price          int                                             null,
  perime_at               date                                            null,
  prix_mnp                int        default 0                            null,
  qty_appro               int        default 0                            null,
  qty_seuil_mini          int        default 0                            null,
  regular_unit_price      int                                             null,
  scheduled               tinyint(1) default 0                            null comment 'pour les produits avec une obligation ordonnance',
  seuil_decond            int                                             null,
  seuil_reassort          int                                             null,
  status                  enum ('CLOSED', 'DELETED', 'DISABLE', 'ENABLE') null,
  type_produit            enum ('DETAIL', 'PACKAGE')                      null,
  updated_at              datetime(6)                                     null,
  tableau_id              bigint                                          null,
  primary key (rev, id),
  constraint FKlalyyb5rcjm7w9wu2uhknh61j
    foreign key (rev) references revinfo (rev)
);

create table stock_produit_aud
(
  id               bigint      not null,
  rev              int         not null,
  revtype          tinyint     null,
  created_at       datetime(6) null,
  last_modified_by varchar(50) null,
  qty_stock        int         null,
  qty_ug           int         null,
  qty_virtual      int         null,
  updated_at       datetime(6) null,
  produit_id       bigint      null,
  storage_id       bigint      null,
  primary key (rev, id),
  constraint FK260o2tvdlkglda2ab2tt1r60l
    foreign key (rev) references revinfo (rev)
);

create table storage
(
  id           bigint auto_increment
    primary key,
  name         varchar(255)                                         not null,
  storage_type enum ('POINT_DE_VENTE', 'PRINCIPAL', 'SAFETY_STOCK') not null,
  magasin_id   bigint                                               not null,
  constraint UK5fe37ity4pov1usxcqr3b03nd
    unique (name),
  constraint FKn1jj5tjhmgepsc4nwqsf1db9r
    foreign key (magasin_id) references magasin (id)
);

create table rayon
(
  id         bigint auto_increment
    primary key,
  code       varchar(255)         not null,
  exclude    tinyint(1) default 0 null,
  libelle    varchar(255)         not null,
  storage_id bigint               not null,
  constraint UK6c4mu7mhf4f0shby67qh7g0b7
    unique (libelle, storage_id),
  constraint UKc7q7hrcb4fgtu9nx1hv84amdx
    unique (code, storage_id),
  constraint FK4vkox6f9rrh2asaji8wwu7ruu
    foreign key (storage_id) references storage (id)
);

create table tableau
(
  id     bigint auto_increment
    primary key,
  code   varchar(255) not null,
  valeur int          not null,
  constraint UKdhugqone5nf7sq7t9c178lfb5
    unique (code)
);

create index code_index
  on tableau (code);

create table tva
(
  id   bigint auto_increment
    primary key,
  taux int not null,
  constraint UKsx5rjlj9hynan0fr5tugf2ycv
    unique (taux)
);

create table produit
(
  id                      bigint auto_increment
    primary key,
  categorie               enum ('A', 'B', 'C')                            not null,
  check_expiry_date       tinyint(1) default 0                            null,
  chiffre                 tinyint(1) default 1                            null,
  code_ean                varchar(255)                                    null,
  code_remise             varchar(6) default 'CODE_0'                     null comment 'Code de remise qui seront mappés sur les grilles de remises',
  cost_amount             int                                             not null,
  created_at              datetime(6)                                     not null,
  deconditionnable        bit                                             not null,
  item_cost_amount        int                                             not null
    check (`item_cost_amount` >= 0),
  item_qty                int                                             not null
    check (`item_qty` >= 0),
  item_regular_unit_price int                                             not null
    check (`item_regular_unit_price` >= 0),
  libelle                 varchar(255)                                    not null,
  net_unit_price          int                                             not null,
  perime_at               date                                            null,
  prix_mnp                int        default 0                            not null,
  qty_appro               int        default 0                            null,
  qty_seuil_mini          int        default 0                            null,
  regular_unit_price      int                                             not null,
  scheduled               tinyint(1) default 0                            null comment 'pour les produits avec une obligation ordonnance',
  seuil_decond            int                                             null
    check (`seuil_decond` >= 0),
  seuil_reassort          int                                             null
    check (`seuil_reassort` >= 0),
  status                  enum ('CLOSED', 'DELETED', 'DISABLE', 'ENABLE') not null,
  type_produit            enum ('DETAIL', 'PACKAGE')                      not null,
  updated_at              datetime(6)                                     not null,
  dci_id                  bigint                                          null,
  famille_id              bigint                                          not null,
  forme_id                bigint                                          null,
  gamme_id                bigint                                          null,
  laboratoire_id          bigint                                          null,
  parent_id               bigint                                          null,
  tableau_id              bigint                                          null,
  tva_id                  bigint                                          not null,
  constraint UKhaaprmfc9pf1bp3n5g9dnkx91
    unique (libelle, type_produit),
  constraint FK1a3bimdll1wc7by0ng16rl58y
    foreign key (dci_id) references dci (id),
  constraint FK5m918v7ukauswsdgdo3pv74fa
    foreign key (famille_id) references famille_produit (id),
  constraint FK5nonk9ie64lu7b27m1a8fs741
    foreign key (gamme_id) references gamme_produit (id),
  constraint FKabkfkm5f6kst7099gv3dafaac
    foreign key (tva_id) references tva (id),
  constraint FKchxb0k9i70n6x9sa3mcpgfqei
    foreign key (parent_id) references produit (id),
  constraint FKh4p706en3rc1tbafjh6goa7yp
    foreign key (tableau_id) references tableau (id),
  constraint FKhowc8u7evv6pewlrvhs0q1lap
    foreign key (laboratoire_id) references laboratoire (id),
  constraint FKl718rk1riol8vlo4ynew1bv08
    foreign key (forme_id) references form_produit (id)
);

create table daily_stock
(
  id         bigint auto_increment
    primary key,
  date_key   date          not null,
  stock      int default 0 not null,
  produit_id bigint        not null,
  constraint UKjr0utsqdnaa78gdjpseilkpcy
    unique (date_key, produit_id),
  constraint FKck7yy4s9kk0r3jq9j7541ms03
    foreign key (produit_id) references produit (id)
);

create table fournisseur_produit
(
  id                 bigint auto_increment
    primary key,
  created_by         varchar(50)          null,
  created_date       datetime(6)          null,
  last_modified_by   varchar(50)          null,
  last_modified_date datetime(6)          null,
  code_cip           varchar(255)         not null,
  principal          tinyint(1) default 0 not null,
  prix_achat         int                  not null,
  prix_uni           int                  not null,
  fournisseur_id     bigint               not null,
  produit_id         bigint               not null,
  constraint UKaqrs8apy7lun5q6bicwmolxrr
    unique (code_cip, fournisseur_id),
  constraint UKe88lr5v2edh60cq97jrtdgx14
    unique (produit_id, fournisseur_id),
  constraint FK7ovp4hph1va6aiw0hrjb6bo5b
    foreign key (fournisseur_id) references fournisseur (id),
  constraint FKd2gc16hsakliy5idekx467yip
    foreign key (produit_id) references produit (id)
);

create index code_cip_index
  on fournisseur_produit (code_cip);

create index principal_index
  on fournisseur_produit (principal);

create index codeEan_index
  on produit (code_ean);

create index libelle_index
  on produit (libelle);

create index status_index
  on produit (status);

create table rayon_produit
(
  id         bigint auto_increment
    primary key,
  produit_id bigint not null,
  rayon_id   bigint not null,
  constraint UKarionkb6k29slypt62myqg6sb
    unique (produit_id, rayon_id),
  constraint FK8ux9wik1mhtffce4o7s08dahe
    foreign key (produit_id) references produit (id),
  constraint FKjqh6g957rhoboh4l4atf6t78t
    foreign key (rayon_id) references rayon (id)
);

create table stock_produit
(
  id               bigint auto_increment
    primary key,
  created_at       datetime(6) not null,
  last_modified_by varchar(50) null,
  qty_stock        int         not null,
  qty_ug           int         not null
    check (`qty_ug` >= 0),
  qty_virtual      int         not null,
  updated_at       datetime(6) not null,
  produit_id       bigint      not null,
  storage_id       bigint      not null,
  constraint UK209l3yi42tlex9i3yft8nb4n9
    unique (storage_id, produit_id),
  constraint FK1lnsn9h1evyxwnnktixqo2wc
    foreign key (produit_id) references produit (id),
  constraint FK8kuouqq6nv3lx0eb3wd73kf95
    foreign key (storage_id) references storage (id)
);

create table substitut
(
  id             bigint auto_increment
    primary key,
  type_substitut enum ('GENERIQUE', 'THERAPEUTIQUE') not null,
  produit_id     bigint                              not null,
  substitut_id   bigint                              not null,
  constraint UK3sjp95psbhioyym08o2hh58ww
    unique (produit_id, substitut_id),
  constraint FKhko54cwr8duquqk53yxv43qm
    foreign key (substitut_id) references produit (id),
  constraint FKpng5sgxh6pebef0amhpns5ndj
    foreign key (produit_id) references produit (id)
);

create table user
(
  id                   bigint auto_increment
    primary key,
  created_by           varchar(50)  null,
  created_date         datetime(6)  null,
  last_modified_by     varchar(50)  null,
  last_modified_date   datetime(6)  null,
  action_authority_key varchar(255) null,
  activated            bit          not null,
  activation_key       varchar(20)  null,
  email                varchar(254) null,
  first_name           varchar(50)  null,
  image_url            varchar(256) null,
  lang_key             varchar(10)  null,
  last_name            varchar(50)  null,
  login                varchar(50)  not null,
  password_hash        varchar(60)  not null,
  reset_date           datetime(6)  null,
  reset_key            varchar(20)  null,
  magasin_id           bigint       not null,
  constraint UKew1hvam8uwaknuaellwhqchhb
    unique (login),
  constraint UKob8kqyqqgmefl0aco34akdtpe
    unique (email),
  constraint FK4g7bc724viqykr1u3tjnddf4c
    foreign key (magasin_id) references magasin (id)
);

create table ajust
(
  id          bigint auto_increment
    primary key,
  commentaire varchar(255)               null,
  date_mtv    datetime(6)                not null,
  statut      enum ('CLOSED', 'PENDING') not null,
  storage_id  bigint                     not null,
  user_id     bigint                     not null,
  constraint FKbkc3o0mx6q4799rpbpgcd0e89
    foreign key (storage_id) references storage (id),
  constraint FKg4b7lwwiqkfq8ln82tbslcthu
    foreign key (user_id) references user (id)
);

create table ajustement
(
  id                  bigint auto_increment
    primary key,
  date_mtv            datetime(6)                              not null,
  qty_mvt             int                                      not null,
  stock_after         int                                      not null,
  stock_before        int                                      not null,
  type_ajust          enum ('AJUSTEMENT_IN', 'AJUSTEMENT_OUT') not null,
  ajust_id            bigint                                   not null,
  motif_ajustement_id bigint                                   null,
  produit_id          bigint                                   not null,
  constraint UKpl3ledfcmf6v758vp99whh00j
    unique (ajust_id, produit_id),
  constraint FKex9228479j8udon2wd8gm5dot
    foreign key (ajust_id) references ajust (id),
  constraint FKgem9o7c8roq5g1lb555j4hnlh
    foreign key (produit_id) references produit (id),
  constraint FKpstvc4bcssojfv5absxm5vtba
    foreign key (motif_ajustement_id) references motif_ajustement (id)
);

create table app_configuration
(
  name            varchar(50)                                                                                       not null
    primary key,
  created         datetime(6)                                                                                       null,
  description     varchar(255)                                                                                      not null,
  other_value     varchar(255)                                                                                      null,
  updated         datetime(6)                                                                                       null,
  value           varchar(255)                                                                                      not null,
  value_type      enum ('BOOLEAN', 'COLOR', 'DATE', 'DATE_TIME', 'FONT', 'LIST', 'MAP', 'NUMBER', 'STRING', 'TIME') not null,
  validated_by_id bigint                                                                                            null,
  constraint FK5jspq15xojfrkynt2gwtgys1e
    foreign key (validated_by_id) references user (id)
);

create table cash_register
(
  id              bigint auto_increment
    primary key,
  begin_time      datetime(6)                                     not null,
  cancele_amount  int                                             null,
  created         datetime(6)                                     not null,
  end_time        datetime(6)                                     null,
  final_amount    bigint                                          null,
  init_amount     bigint                                          not null,
  statut          enum ('CLOSED', 'OPEN', 'PENDING', 'VALIDATED') not null,
  updated         datetime(6)                                     not null,
  updated_user_id bigint                                          null,
  user_id         bigint                                          not null,
  constraint FK29gjscipf2nh9alyb3e9jia0q
    foreign key (updated_user_id) references user (id),
  constraint FKdv6da1nxa2xkrlk3rkf807s6a
    foreign key (user_id) references user (id)
);

create table cash_fund
(
  id               bigint auto_increment
    primary key,
  amount           int                                         not null,
  cash_fund_type   enum ('AUTO', 'MANUAL')                     not null,
  created          datetime(6)                                 not null,
  statut           enum ('PENDING', 'PROCESSING', 'VALIDETED') not null,
  updated          datetime(6)                                 null,
  cash_register_id bigint                                      null,
  user_id          bigint                                      not null,
  validated_by_id  bigint                                      null,
  constraint UKkw6trpuogcthcnuixl416p6tc
    unique (cash_register_id),
  constraint FK8dpju4dcbnn85y6nuut81k4fm
    foreign key (validated_by_id) references user (id),
  constraint FKe785pgxlc1vu3g47la9g7i8h1
    foreign key (user_id) references user (id),
  constraint FKs2oiwqxkralpsy81kk2afnj60
    foreign key (cash_register_id) references cash_register (id)
);

create table cash_register_item
(
  id                bigint auto_increment
    primary key,
  amount            bigint                                                                                                                                                                                                        null,
  type_transaction  enum ('CASH_SALE', 'CAUTION', 'CREDIT_SALE', 'ENTREE_CAISSE', 'FONDS_CAISSE', 'REGLEMENT_DIFFERE', 'REGLEMENT_TIERS_PAYANT', 'REGLMENT_FOURNISSEUR', 'SORTIE_CAISSE', 'VENTES_DEPOTS', 'VENTES_DEPOTS_AGREE') not null,
  cash_register_id  bigint                                                                                                                                                                                                        not null,
  payment_mode_code varchar(50)                                                                                                                                                                                                   not null,
  constraint UKovfm6oebekfa7449p67yvnnpf
    unique (cash_register_id, payment_mode_code, type_transaction),
  constraint FK27y8m06p8783unjcod1i5p97x
    foreign key (payment_mode_code) references payment_mode (code),
  constraint FKg7uv0e0n4k52n11ryawhjueef
    foreign key (cash_register_id) references cash_register (id)
);

create table commande
(
  id                bigint auto_increment
    primary key,
  created_at        datetime(6)                              not null,
  discount_amount   int default 0                            null,
  final_amount      int                                      null comment 'montant vente de la commande finalisée',
  gross_amount      int                                      not null comment 'montant achat de la commande',
  ht_amount         int default 0                            null,
  order_amount      int                                      not null comment 'montant vente de la commande en cours de traitement',
  order_reference   varchar(20)                              null,
  order_status      enum ('CLOSED', 'RECEIVED', 'REQUESTED') not null,
  paiment_status    enum ('NOT_SOLD', 'PAID', 'UNPAID')      not null,
  receipt_date      date                                     null,
  receipt_reference varchar(20)                              null,
  tax_amount        int default 0                            null,
  receipt_type      enum ('DIRECT', 'ORDER')                 not null,
  updated_at        datetime(6)                              not null,
  fournisseur_id    bigint                                   not null,
  user_id           bigint                                   not null,
  constraint UKaiq6eeql26l3q8gm9k1p810lr
    unique (receipt_reference, fournisseur_id),
  constraint FKe9u9pamnss31e4pn6twt1yk0q
    foreign key (fournisseur_id) references fournisseur (id),
  constraint FKp5deswt3amtfx764raq42rw2o
    foreign key (user_id) references user (id)
);

create index order_status_index
  on commande (order_status);

create index receipt_paiment_status_index
  on commande (paiment_status);

create index receipt_reference_index
  on commande (receipt_reference);

create table importation
(
  id                 bigint auto_increment
    primary key,
  created_at         datetime(6)                                                                 not null,
  error_size         int                                                                         not null,
  importation_status enum ('COMPLETED', 'COMPLETED_ERRORS', 'FAIL', 'INTERRUPTED', 'PROCESSING') not null,
  importation_type   enum ('CLIENTS', 'FICHE_ARTICLE', 'STOCK_PRODUIT', 'TIERS_PAYANT', 'VENTE') not null,
  ligne_en_erreur    longtext collate utf8mb4_bin                                                null
    check (json_valid(`ligne_en_erreur`)),
  size               int                                                                         not null,
  total_zise         int                                                                         not null,
  updated_at         datetime(6)                                                                 null,
  user_id            bigint                                                                      not null,
  constraint FKkdc81pqyi8ddmoxrpxj7yi109
    foreign key (user_id) references user (id)
);

create index created_at_index
  on importation (created_at);

create index importation_status_index
  on importation (importation_status);

create index importation_type_index
  on importation (importation_type);

create table logs
(
  id               bigint auto_increment
    primary key,
  comments         varchar(255)                                                                                                                                                                                                                                                                                                                                                                                                                              not null,
  created_at       datetime(6)                                                                                                                                                                                                                                                                                                                                                                                                                               not null,
  indentity_key    varchar(255)                                                                                                                                                                                                                                                                                                                                                                                                                              not null,
  new_object       varchar(255)                                                                                                                                                                                                                                                                                                                                                                                                                              null,
  old_object       varchar(255)                                                                                                                                                                                                                                                                                                                                                                                                                              null,
  transaction_type enum ('ACTIVATION_PRIVILEGE', 'AJUSTEMENT_IN', 'AJUSTEMENT_OUT', 'CANCEL_SALE', 'COMMANDE', 'CREATE_PRODUCT', 'DECONDTION_IN', 'DECONDTION_OUT', 'DELETE_PRODUCT', 'DELETE_SALE', 'DISABLE_PRODUCT', 'ENABLE_PRODUCT', 'ENTREE_STOCK', 'FORCE_STOCK', 'INVENTAIRE', 'MODIFICATION_PRIX_PRODUCT', 'MODIFICATION_PRIX_PRODUCT_A_LA_VENTE', 'MOUVEMENT_STOCK_IN', 'MOUVEMENT_STOCK_OUT', 'REAPPRO', 'SALE', 'SUPPRESSION', 'UPDATE_PRODUCT') not null,
  user_id          bigint                                                                                                                                                                                                                                                                                                                                                                                                                                    not null,
  constraint FK6313q4colhy85u9nyh7c6hy50
    foreign key (user_id) references user (id)
);

create index createdAt_index
  on logs (created_at);

create index indentityKey_index
  on logs (indentity_key);

create index transaction_type_index
  on logs (transaction_type);

create table order_line
(
  id                     bigint auto_increment
    primary key,
  created_at             datetime(6)      not null,
  discount_amount        int(6) default 0 not null,
  final_stock            int(6)           null,
  free_qty               int(4) default 0 null,
  init_stock             int(6)           not null,
  net_amount             int(8) default 0 null,
  order_cost_amount      int(8) default 0 not null,
  order_unit_price       int(8)           not null,
  provisional_code       bit              null,
  quantity_received      int(6)           null,
  quantity_requested     int(6)           not null,
  quantity_returned      int(6)           null,
  receipt_date           datetime(6)      null,
  tax_amount             int(6) default 0 null,
  is_updated             bit              null,
  updated_at             datetime(6)      not null,
  commande_id            bigint           not null,
  fournisseur_produit_id bigint           not null,
  tva_id                 bigint           null,
  constraint UKsfcar1lpmp9ytui9cg75le9d5
    unique (commande_id, fournisseur_produit_id),
  constraint FKjelph47crh3lyf09c5c7sqjnq
    foreign key (commande_id) references commande (id),
  constraint FKkt4tdexu7oo36cobryunswmjh
    foreign key (fournisseur_produit_id) references fournisseur_produit (id),
  constraint FKm1m4fkcj2tgcc2m9d0r65v43q
    foreign key (tva_id) references tva (id)
);

create table lot
(
  id                   bigint auto_increment
    primary key,
  created_date         datetime(6)      not null,
  expiry_date          date             null,
  quantity_received_ug int(4) default 0 not null,
  manufacturing_date   date             null,
  num_lot              varchar(255)     not null,
  quantity             int(6) default 0 not null,
  order_line_id        bigint           not null,
  constraint UKe0v6fopjvn1ms2recdlpftbb0
    unique (num_lot, order_line_id),
  constraint FKiwoiokswfe6bx1si74kcni476
    foreign key (order_line_id) references order_line (id)
);

create index num_lot_index
  on lot (num_lot);

create table persistent_token
(
  series      varchar(255) not null
    primary key,
  ip_address  varchar(39)  null,
  token_date  date         null,
  token_value varchar(255) not null,
  user_agent  varchar(255) null,
  user_id     bigint       null,
  constraint FKqiuyia9rgw42uksrgy3ayk1lc
    foreign key (user_id) references user (id)
);

create table produit_perime
(
  id              bigint auto_increment
    primary key,
  after_stock     int         not null,
  created         datetime(6) not null,
  init_stock      int         not null
    check (`init_stock` >= 1),
  peremption_date date        not null,
  quantity        int         not null
    check (`quantity` >= 1),
  lot_id          bigint      null,
  produit_id      bigint      not null,
  user_id         bigint      not null,
  constraint FK3aelnwbpfeqjrtq990unyjs56
    foreign key (user_id) references user (id),
  constraint FK7qyne8q37phmm5hld2e6byxco
    foreign key (lot_id) references lot (id),
  constraint FKg4s7frhwa9ci11yr77momll5r
    foreign key (produit_id) references produit (id)
);

create index produit_perime_index
  on produit_perime (peremption_date);

create table repartition_stock_produit
(
  id                           bigint auto_increment
    primary key,
  created_at                   datetime(6) not null,
  dest_final_stock             int         not null,
  dest_init_stock              int         not null,
  qty_mvt                      int         not null,
  source_final_stock           int         not null,
  source_init_stock            int         not null,
  produit_id                   bigint      not null,
  stock_produit_destination_id bigint      not null,
  stock_produit_source_id      bigint      not null,
  user_id                      bigint      not null,
  constraint FKgyu4w5fylq30wrdc1n284yuqg
    foreign key (stock_produit_source_id) references stock_produit (id),
  constraint FKj2oonew83hnf8pmi8nmfiejpo
    foreign key (produit_id) references produit (id),
  constraint FKl09sdq1y926wgof99ifw42ipd
    foreign key (user_id) references user (id),
  constraint FKrxga8j2mjur3aw43h3bq6l909
    foreign key (stock_produit_destination_id) references stock_produit (id)
);

create table retour_bon
(
  id          bigint auto_increment
    primary key,
  commentaire varchar(150)                  null,
  date_mtv    datetime(6)                   not null,
  statut      enum ('CLOSED', 'PROCESSING') not null,
  commande_id bigint                        not null,
  user_id     bigint                        not null,
  constraint FKgbybxbktf9oy669tyn6rqlaw1
    foreign key (user_id) references user (id),
  constraint FKtqs59qxfvlt4nrgq8mqagenga
    foreign key (commande_id) references commande (id)
);

create table retour_bon_item
(
  id              bigint auto_increment
    primary key,
  after_stock     int         null,
  date_mtv        datetime(6) not null,
  init_stock      int         not null,
  qty_mvt         int         not null
    check (`qty_mvt` >= 1),
  lot_id          bigint      null,
  motif_retour_id bigint      not null,
  order_line_id   bigint      not null,
  retour_bon_id   bigint      not null,
  constraint FK7f3y7rovqwt9wygs25l6baqky
    foreign key (retour_bon_id) references retour_bon (id),
  constraint FK8kuyifpxr0rjr8y2y5l572mid
    foreign key (order_line_id) references order_line (id),
  constraint FK9yua85has78k1itu1pjp8rvyg
    foreign key (motif_retour_id) references motif_retour_produit (id),
  constraint FKsq7ju65jlgejnnvj1ep3d1vmo
    foreign key (lot_id) references lot (id)
);

create table store_inventory
(
  id                         bigint auto_increment
    primary key,
  created_at                 datetime(6)                                     not null,
  gap_amount                 int                                             null,
  gap_cost                   int                                             null,
  inventory_amount_after     bigint                                          not null,
  inventory_amount_begin     bigint                                          not null,
  inventory_category         enum ('FAMILLY', 'MAGASIN', 'RAYON', 'STORAGE') not null,
  inventory_type             enum ('MANUEL', 'PROGRAMME')                    not null,
  inventory_value_cost_after bigint                                          not null,
  inventory_value_cost_begin bigint                                          not null,
  statut                     enum ('CLOSED', 'CREATE', 'PROCESSING')         not null,
  updated_at                 datetime(6)                                     not null,
  rayon_id                   bigint                                          null,
  storage_id                 bigint                                          null,
  user_id                    bigint                                          not null,
  constraint FK6gntnjhmeu8dahq204wmk1a2r
    foreign key (user_id) references user (id),
  constraint FK88teyake2uy873xolysdpwfl7
    foreign key (storage_id) references storage (id),
  constraint FKnwue82ru709xwwxkj5if2v6qq
    foreign key (rayon_id) references rayon (id)
);

create table store_inventory_line
(
  id                   bigint auto_increment
    primary key,
  gap                  int         null,
  inventory_value_cost int         null,
  last_unit_price      int         null,
  quantity_init        int         null,
  quantity_on_hand     int         null,
  quantity_sold        int         null,
  updated              bit         not null,
  updated_at           datetime(6) not null,
  produit_id           bigint      not null,
  store_inventory_id   bigint      not null,
  constraint UKhidvm20io56axybnk34jqvs4c
    unique (produit_id, store_inventory_id),
  constraint FKg8d5ld2v2vy7tr54mwar1rh9
    foreign key (produit_id) references produit (id),
  constraint FKoe713l7vns3jb1eo2uhniy4q3
    foreign key (store_inventory_id) references store_inventory (id)
);

create table suggestion
(
  id                   bigint auto_increment
    primary key,
  created_at           datetime(6)               not null,
  statut               enum ('CLOSED', 'OPEN')   null,
  suggession_reference varchar(255)              null,
  type_suggession      enum ('AUTO', 'MANUELLE') null,
  updated_at           datetime(6)               not null,
  fournisseur_id       bigint                    not null,
  last_user_edit_id    bigint                    null,
  magasin_id           bigint                    not null,
  constraint FKba6svq3le6ej41368lpg26w2u
    foreign key (magasin_id) references magasin (id),
  constraint FKk2x8077hbpyca66ww9vgam3ae
    foreign key (fournisseur_id) references fournisseur (id),
  constraint FKq4kj4wnipt2h2kuyy6fl0fgvu
    foreign key (last_user_edit_id) references user (id)
);

create index type_suggession_index
  on suggestion (type_suggession);

create table suggestion_line
(
  id                     bigint auto_increment
    primary key,
  created_at             datetime(6) not null,
  quantity               int         null,
  updated_at             datetime(6) not null,
  fournisseur_produit_id bigint      not null,
  suggestion_id          bigint      not null,
  constraint UKtc1xgbkc01q1yrxe0nif3ajtk
    unique (suggestion_id, fournisseur_produit_id),
  constraint FKcac8uukf3y1701wji8aey0eck
    foreign key (suggestion_id) references suggestion (id),
  constraint FKjtyays176u1dvjkafkf798nua
    foreign key (fournisseur_produit_id) references fournisseur_produit (id)
);

create table ticketing
(
  id                  bigint auto_increment
    primary key,
  created             datetime(6) not null,
  number_of1          int         not null,
  number_of10         int         not null,
  number_of100hundred int         not null,
  number_of10thousand int         not null,
  number_of1thousand  int         not null,
  number_of200hundred int         not null,
  number_of25         int         not null,
  number_of2thousand  int         not null,
  number_of5          int         not null,
  number_of50         int         not null,
  number_of500hundred int         not null,
  number_of5thousand  int         not null,
  other_amount        int         not null,
  total_amount        bigint      not null,
  cash_register_id    bigint      not null,
  constraint UKck44ngcp2bue83xo845mbu962
    unique (cash_register_id),
  constraint FKmq8ij1eptnvb7xprhsf8w3ys1
    foreign key (cash_register_id) references cash_register (id)
);

create table tiers_payant
(
  id                        bigint auto_increment
    primary key,
  adresse                   varchar(200)                          null,
  to_be_exclude             bit    default b'0'                   null,
  categorie                 enum ('ASSURANCE', 'CARNET', 'DEPOT') not null,
  code_organisme            varchar(100)                          null,
  conso_mensuelle           bigint                                null,
  consommation_json         longtext collate utf8mb4_bin          null
    check (json_valid(`consommation_json`)),
  created                   datetime(6)                           not null,
  email                     varchar(50)                           null,
  full_name                 varchar(200)                          not null,
  model_facture             varchar(20)                           null,
  montant_max_sur_fact      bigint                                null,
  name                      varchar(150)                          not null,
  nbre_bons_max_sur_fact    int(8)                                null,
  nbre_bordereau            int(6) default 1                      not null,
  plafond_absolu            bit    default b'0'                   null,
  plafond_absolu_client     bit    default b'0'                   null,
  plafond_conso             bigint                                null,
  plafond_conso_client      int                                   null,
  plafond_journalier_client int                                   null,
  remise_forfaitaire        int    default 0                      null,
  statut                    enum ('ACTIF', 'DISABLED', 'LOCK')    not null,
  telephone                 varchar(15)                           null,
  telephone_fixe            varchar(15)                           null,
  updated                   datetime(6)                           not null,
  groupe_tiers_payant_id    bigint                                null,
  user_id                   bigint                                not null,
  constraint UK3moh75ah5g6ce1dm9n28psmvo
    unique (full_name),
  constraint UKsmbw04nasi7yrcjwcg95tsh6t
    unique (name),
  constraint FKeq4f34e6hgun4oad7iuxnnrs7
    foreign key (groupe_tiers_payant_id) references groupe_tiers_payant (id),
  constraint FKhp0141pj7prgtctwef8gbnxf7
    foreign key (user_id) references user (id)
);

create table client_tiers_payant
(
  id                  bigint auto_increment
    primary key,
  conso_mensuelle     bigint                             null,
  consommation_json   longtext collate utf8mb4_bin       null
    check (json_valid(`consommation_json`)),
  created             datetime(6)                        not null,
  num                 varchar(100)                       not null,
  priorite            enum ('R0', 'R1', 'R2', 'R3')      not null,
  statut              enum ('ACTIF', 'DISABLED', 'LOCK') not null,
  taux                int(3)                             not null,
  updated             datetime(6)                        not null,
  assured_customer_id bigint                             not null,
  tiers_payant_id     bigint                             not null,
  constraint UK2vls4uc89y3g75n6qnudmn0qs
    unique (tiers_payant_id, num),
  constraint UKmesf3j3moh06oir9mq5fxmmb9
    unique (tiers_payant_id, assured_customer_id),
  constraint FKb627stp58elnn5yel8hi27tju
    foreign key (assured_customer_id) references customer (id),
  constraint FKp7d63xly6qydc32rbc5hwoxst
    foreign key (tiers_payant_id) references tiers_payant (id)
);

create table facture_tiers_payant
(
  id                             bigint auto_increment
    primary key,
  created                        datetime(6)                    not null,
  debut_periode                  date                           null,
  facture_provisoire             bit                            not null,
  fin_periode                    date                           null,
  montant_regle                  int         default 0          null,
  num_facture                    varchar(20)                    not null,
  remise_forfetaire              int                            not null,
  statut                         varchar(20) default 'NOT_PAID' not null,
  updated                        datetime(6)                    null,
  groupe_facture_tiers_payant_id bigint                         null,
  groupe_tiers_payant_id         bigint                         null,
  tiers_payant_id                bigint                         null,
  user_id                        bigint                         not null,
  constraint UKdpie1csuti0gr5k8upvaatx8p
    unique (num_facture),
  constraint FK3ew1b9jnhhu9x4myvi6c5f0ji
    foreign key (user_id) references user (id),
  constraint FK64mvu1jd0r3m57m1dil45h40p
    foreign key (tiers_payant_id) references tiers_payant (id),
  constraint FKcfeusk9tdn2w3nkkw8wmyxwge
    foreign key (groupe_facture_tiers_payant_id) references facture_tiers_payant (id),
  constraint FKku32lbuahnuhx7f249jglins9
    foreign key (groupe_tiers_payant_id) references groupe_tiers_payant (id)
);

create index num_facture_index
  on facture_tiers_payant (num_facture);

create table produit_tiers_payant_prix
(
  id              bigint auto_increment
    primary key,
  created         datetime(6) default current_timestamp(6) not null,
  enabled         bit         default b'1'                 not null,
  prix_type       enum ('POURCENTAGE', 'RERERENCE')        not null,
  updated         datetime(6) default current_timestamp(6) not null,
  valeur          int                                      not null
    check (`valeur` >= 5),
  produit_id      bigint                                   not null,
  tiers_payant_id bigint                                   not null,
  user_id         bigint                                   not null,
  constraint UK13e9w8dannsq7dxs809frd99o
    unique (produit_id, tiers_payant_id, enabled),
  constraint UKg0g38ar8rjkvia2ia8syledf9
    unique (produit_id, tiers_payant_id, prix_type),
  constraint FKdhdiuc2eglbqm449qauj1cp0r
    foreign key (produit_id) references produit (id),
  constraint FKgu8051oe6gpi4n69pqxrbt4op
    foreign key (user_id) references user (id),
  constraint FKpfl0w0p1erkmdlla0lqtmuwa9
    foreign key (tiers_payant_id) references tiers_payant (id)
);

create table user_authority
(
  user_id        bigint      not null,
  authority_name varchar(50) not null,
  primary key (user_id, authority_name),
  constraint FK6ktglpl5mjosa283rvken2py5
    foreign key (authority_name) references authority (name),
  constraint FKpqlsjpkybgos9w2svcri7j8xy
    foreign key (user_id) references user (id)
);

create table utilisation_cle_securite
(
  id                    bigint auto_increment
    primary key,
  caisse                varchar(255) not null,
  commentaire           varchar(255) null,
  entity_id             bigint       null,
  entity_name           varchar(255) null,
  mvt_date              datetime(6)  not null,
  cle_securite_owner_id bigint       not null,
  connected_user_id     bigint       not null,
  privilege_name        varchar(100) not null,
  constraint FK2atxw2tml077su8hsoeaot6a0
    foreign key (cle_securite_owner_id) references user (id),
  constraint FK7ar2ex342qykfi2r6i7fcdcgk
    foreign key (connected_user_id) references user (id),
  constraint FKn7rvgfahogx5tsdpa5yvtc9s6
    foreign key (privilege_name) references privilege (name)
);

create table warehouse_calendar
(
  work_day   date not null
    primary key,
  work_month int  not null,
  work_year  int  not null
);

create table avoir
(
  id                bigint auto_increment
    primary key,
  date_mtv          datetime(6)                not null,
  statut            enum ('EN_COURS', 'SERVI') not null,
  calendar_work_day date                       not null,
  user_id           bigint                     not null,
  constraint FK6m25r6qf4v663pks1s8t7hetv
    foreign key (calendar_work_day) references warehouse_calendar (work_day),
  constraint FKptiih1fwc5n5cafgx8txm93yo
    foreign key (user_id) references user (id)
);

create table decondition
(
  id                     bigint auto_increment
    primary key,
  date_mtv               datetime(6)                              not null,
  qty_mvt                int                                      not null,
  stock_after            int                                      not null,
  stock_before           int                                      not null,
  type_deconditionnement enum ('DECONDTION_IN', 'DECONDTION_OUT') not null,
  calendar_work_day      date                                     not null,
  produit_id             bigint                                   not null,
  user_id                bigint                                   not null,
  constraint FKdmoni6sfl6sh38ej7gngqhg6y
    foreign key (produit_id) references produit (id),
  constraint FKky48vqfjbbchwrwcnl6hnoei5
    foreign key (calendar_work_day) references warehouse_calendar (work_day),
  constraint FKmele8h9p7330pu1kk6nujwk3q
    foreign key (user_id) references user (id)
);

create table ligne_avoir
(
  id         bigint auto_increment
    primary key,
  qte        int    not null,
  qte_servi  int    not null,
  avoir_id   bigint not null,
  produit_id bigint not null,
  constraint UK8jhcxa6a2lu7mjaoc9n2b0caj
    unique (avoir_id, produit_id),
  constraint FK1830n6vy54b68kip0wttwese1
    foreign key (produit_id) references produit (id),
  constraint FK4uge1m9g5j5grtnkc2ens3gs9
    foreign key (avoir_id) references avoir (id)
);

create table reponse_retour_bon
(
  id                bigint auto_increment
    primary key,
  commentaire       varchar(150)                  null,
  date_mtv          datetime(6)                   not null,
  modified_date     datetime(6)                   not null,
  statut            enum ('CLOSED', 'PROCESSING') not null,
  calendar_work_day date                          not null,
  retour_bon_id     bigint                        not null,
  user_id           bigint                        not null,
  constraint FK45ub257h046pqyss9wujxfvnq
    foreign key (user_id) references user (id),
  constraint FKh846yv5iwq8wslk1598bqi2lj
    foreign key (calendar_work_day) references warehouse_calendar (work_day),
  constraint FKpl4rf8a6nr9cmn7lur7su1b1
    foreign key (retour_bon_id) references retour_bon (id)
);

create table reponse_retour_bon_item
(
  id                    bigint auto_increment
    primary key,
  after_stock           int         null,
  date_mtv              datetime(6) not null,
  init_stock            int         not null,
  qty_mvt               int         not null
    check (`qty_mvt` >= 0),
  reponse_retour_bon_id bigint      not null,
  retour_bon_item_id    bigint      not null,
  constraint FK3kiwqjsi0e810eo3x76r3b9b
    foreign key (reponse_retour_bon_id) references reponse_retour_bon (id),
  constraint FKdygaih0a7jw1diyja06px1aa9
    foreign key (retour_bon_item_id) references retour_bon_item (id)
);

create table sales
(
  dtype                           varchar(31)                                                                          not null,
  id                              bigint auto_increment
    primary key,
  amount_to_be_paid               int        default 0                                                                 not null,
  amount_to_be_taken_into_account int        default 0                                                                 not null,
  canceled                        tinyint(1) default 0                                                                 not null,
  ca                              enum ('CA', 'CALLEBASE', 'CA_DEPOT', 'TO_IGNORE')                                    not null,
  commentaire                     varchar(255)                                                                         null,
  copy                            tinyint(1) default 0                                                                 not null,
  cost_amount                     int        default 0                                                                 not null,
  created_at                      datetime(6)                                                                          not null,
  differe                         tinyint(1) default 0                                                                 not null,
  discount_amount                 int        default 0                                                                 not null,
  discount_amount_hors_ug         int        default 0                                                                 not null,
  discount_amount_ug              int        default 0                                                                 not null,
  effective_update_date           datetime(6)                                                                          not null,
  ht_amount                       int        default 0                                                                 not null,
  ht_amount_ug                    int        default 0                                                                 not null,
  imported                        tinyint(1) default 0                                                                 not null,
  marge_ug                        int                                                                                  null,
  monnaie                         int        default 0                                                                 not null,
  montant_tva_ug                  int        default 0                                                                 null,
  montant_net_ug                  int        default 0                                                                 null,
  montant_ttc_ug                  int        default 0                                                                 null,
  nature_vente                    enum ('ASSURANCE', 'CARNET', 'COMPTANT')                                             not null,
  net_amount                      int        default 0                                                                 not null,
  net_ug_amount                   int        default 0                                                                 not null,
  number_transaction              varchar(255)                                                                         not null,
  origine_vente                   enum ('DIRECT', 'DIVIS', 'IMPORTE')                                                  not null,
  payment_status                  enum ('ALL', 'IMPAYE', 'PAYE')                                                       not null,
  payroll_amount                  int        default 0                                                                 not null,
  rest_to_pay                     int        default 0                                                                 not null,
  sales_amount                    int        default 0                                                                 not null,
  statut                          enum ('ACTIVE', 'CANCELED', 'CLOSED', 'DESABLED', 'PENDING', 'PROCESSING', 'REMOVE') not null,
  tax_amount                      int        default 0                                                                 not null,
  to_ignore                       tinyint(1) default 0                                                                 not null,
  tva_embeded                     varchar(100)                                                                         null,
  type_prescription               enum ('CONSEIL', 'DEPOT', 'PRESCRIPTION')                                            not null,
  updated_at                      datetime(6)                                                                          not null,
  num_bon                         varchar(50)                                                                          null,
  part_assure                     int        default 0                                                                 null,
  part_tiers_payant               int        default 0                                                                 null,
  avoir_id                        bigint                                                                               null,
  caisse_id                       bigint                                                                               null,
  caissier_id                     bigint                                                                               not null,
  calendar_work_day               date                                                                                 not null,
  canceled_sale_id                bigint                                                                               null,
  cash_register_id                bigint                                                                               null,
  customer_id                     bigint                                                                               null,
  last_caisse_id                  bigint                                                                               null,
  last_user_edit_id               bigint                                                                               not null,
  magasin_id                      bigint                                                                               not null,
  remise_id                       bigint                                                                               null,
  seller_id                       bigint                                                                               not null,
  user_id                         bigint                                                                               not null,
  account_id                      bigint                                                                               null,
  ayant_droit_id                  bigint                                                                               null,
  depot_id                        bigint                                                                               null,
  depot_agree_id                  bigint                                                                               null,
  constraint FK1dkprbd8yrrmpeeqi57dy39ox
    foreign key (avoir_id) references avoir (id),
  constraint FK1k4ojfo1vl8wn6o0a4sgvi2oo
    foreign key (calendar_work_day) references warehouse_calendar (work_day),
  constraint FK1nngwi74wjuy96pbjmr6k9cmc
    foreign key (account_id) references customer_account (id),
  constraint FK1vjccyu9gpb80n5l8b58q9xcj
    foreign key (canceled_sale_id) references sales (id),
  constraint FK21r69rm6qvqoecj2qn8dodi7w
    foreign key (depot_id) references magasin (id),
  constraint FK72ep16wuoj7nllumicmk2ie3s
    foreign key (customer_id) references customer (id),
  constraint FK7wj3u1v9ll6qu8ju53142afm6
    foreign key (last_caisse_id) references poste (id),
  constraint FK8d36jdbjf6kif9hexfg1321s
    foreign key (cash_register_id) references cash_register (id),
  constraint FK8jn843nshbh0rxx99oil0xbev
    foreign key (depot_agree_id) references magasin (id),
  constraint FKaoq0nuq3h1e1d1swcv1i0knc2
    foreign key (seller_id) references user (id),
  constraint FKd98rul87ffrih39pgn4xh0x3i
    foreign key (magasin_id) references magasin (id),
  constraint FKhoe19wv2k8i34v9eb8o3g2k4m
    foreign key (last_user_edit_id) references user (id),
  constraint FKjjnyagbe68u4fn7k5btsuqqoh
    foreign key (remise_id) references remise (id),
  constraint FKjmka8om7114crri3l6ucotdum
    foreign key (caissier_id) references user (id),
  constraint FKol9pmxkqx60x3slvu2ijoex43
    foreign key (caisse_id) references poste (id),
  constraint FKpyxoaae6i92epy849wvdfdlke
    foreign key (ayant_droit_id) references customer (id),
  constraint FKu5lyewcf0mgbldqrf8rhmjf6
    foreign key (user_id) references user (id)
);

create table payment_transaction
(
  dtype                   varchar(31)                                                                                                                                                                                                   not null,
  id                      bigint auto_increment
    primary key,
  categorie_ca            enum ('CA', 'CALLEBASE', 'CA_DEPOT', 'TO_IGNORE')                                                                                                                                                             not null,
  commentaire             varchar(255)                                                                                                                                                                                                  null,
  created_at              datetime(6)                                                                                                                                                                                                   not null,
  credit                  bit                                                                                                                                                                                                           not null,
  expected_amount         int                                                                                                                                                                                                           not null,
  montant_verse           int                                                                                                                                                                                                           not null,
  paid_amount             int                                                                                                                                                                                                           not null,
  reel_amount             int                                                                                                                                                                                                           not null,
  transaction_date        date                                                                                                                                                                                                          not null,
  type_transaction        enum ('CASH_SALE', 'CAUTION', 'CREDIT_SALE', 'ENTREE_CAISSE', 'FONDS_CAISSE', 'REGLEMENT_DIFFERE', 'REGLEMENT_TIERS_PAYANT', 'REGLMENT_FOURNISSEUR', 'SORTIE_CAISSE', 'VENTES_DEPOTS', 'VENTES_DEPOTS_AGREE') not null,
  grouped                 bit                                                                                                                                                                                                           null,
  part_assure             int default 0                                                                                                                                                                                                 null,
  part_tiers_payant       int default 0                                                                                                                                                                                                 null,
  banque_id               bigint                                                                                                                                                                                                        null,
  cash_register_id        bigint                                                                                                                                                                                                        not null,
  payment_mode_code       varchar(50)                                                                                                                                                                                                   not null,
  account_id              bigint                                                                                                                                                                                                        null,
  differe_customer_id     bigint                                                                                                                                                                                                        null,
  facture_tiers_payant_id bigint                                                                                                                                                                                                        null,
  parent_id               bigint                                                                                                                                                                                                        null,
  commande_id             bigint                                                                                                                                                                                                        null,
  sale_id                 bigint                                                                                                                                                                                                        null,
  constraint FK6972n014se2yx902jv033f6dc
    foreign key (parent_id) references payment_transaction (id),
  constraint FK823dgt89dnv33jbhorxi0c1q7
    foreign key (account_id) references customer_account (id),
  constraint FKct30csqexaj2yx31f8q5v7eg3
    foreign key (commande_id) references commande (id),
  constraint FKgq9ocwvjtdlt49fd0vhibjd4t
    foreign key (cash_register_id) references cash_register (id),
  constraint FKidf4n2nq1dnta4vd5nqfpxolm
    foreign key (sale_id) references sales (id),
  constraint FKisvg65ny10fvvwvvwl5w5kged
    foreign key (banque_id) references banque (id),
  constraint FKiymrpfgpt865yi648of4i1qhb
    foreign key (differe_customer_id) references customer (id),
  constraint FKllqlbjskwsugxh20pc7h43wd3
    foreign key (payment_mode_code) references payment_mode (code),
  constraint FKslxt8smktjygypqgfhkc891gs
    foreign key (facture_tiers_payant_id) references facture_tiers_payant (id)
);

create table differe_payment_item
(
  id                 bigint auto_increment
    primary key,
  expected_amount    int    not null,
  paid_amount        int    not null,
  differe_payment_id bigint not null,
  sale_id            bigint not null,
  constraint FK80ktxvufyyohmgi801iynj0h5
    foreign key (sale_id) references sales (id),
  constraint FKbbc7tcygixkp2yit0yfpl2pcc
    foreign key (differe_payment_id) references payment_transaction (id)
);

create index pt_categorie_ca_id_index
  on payment_transaction (categorie_ca);

create index vente_created_at_index
  on sales (created_at);

create index vente_effective_update_index
  on sales (effective_update_date);

create index vente_nature_vente_index
  on sales (nature_vente);

create index vente_number_transaction_index
  on sales (number_transaction);

create index vente_payment_status_index
  on sales (payment_status);

create index vente_statut_index
  on sales (statut);

create index vente_to_ignore_index
  on sales (to_ignore);

create index vente_updated_at_index
  on sales (updated_at);

create table sales_line
(
  id                              bigint auto_increment
    primary key,
  after_stock                     int           null,
  amount_to_be_taken_into_account int default 0 not null,
  cost_amount                     int default 0 not null,
  created_at                      datetime(6)   not null,
  discount_amount                 int default 0 not null,
  discount_amount_hors_ug         int default 0 not null,
  discount_amount_ug              int default 0 not null,
  discount_unit_price             int default 0 not null,
  effective_update_date           datetime(6)   not null,
  ht_amount                       int default 0 not null,
  init_stock                      int           null,
  montant_tva_ug                  int default 0 not null,
  net_amount                      int default 0 not null,
  net_unit_price                  int default 0 not null,
  quantity_avoir                  int default 0 not null,
  quantity_requested              int           not null,
  quantity_sold                   int           not null,
  quantity_ug                     int default 0 not null,
  regular_unit_price              int default 0 not null,
  sales_amount                    int default 0 not null,
  tax_amount                      int default 0 not null,
  tax_value                       int default 0 not null,
  to_ignore                       bit           not null,
  updated_at                      datetime(6)   not null,
  produit_id                      bigint        not null,
  sales_id                        bigint        not null,
  constraint UKony71tc7l1kgdmant1eqockbv
    unique (produit_id, sales_id),
  constraint FK2rpcx9v572xhylfle13e130w6
    foreign key (sales_id) references sales (id),
  constraint FKg41n8hm3d58j50hsogv0vv2er
    foreign key (produit_id) references produit (id)
);

create table inventory_transaction
(
  id                           bigint auto_increment
    primary key,
  cost_amount                  int                                                                                                                                                                                                                                                                                                                                                                                                                                       not null,
  created_at                   datetime(6)                                                                                                                                                                                                                                                                                                                                                                                                                               not null,
  quantity                     int                                                                                                                                                                                                                                                                                                                                                                                                                                       not null,
  quantity_after               int                                                                                                                                                                                                                                                                                                                                                                                                                                       not null,
  quantity_befor               int                                                                                                                                                                                                                                                                                                                                                                                                                                       not null,
  regular_unit_price           int                                                                                                                                                                                                                                                                                                                                                                                                                                       not null,
  transaction_type             enum ('ACTIVATION_PRIVILEGE', 'AJUSTEMENT_IN', 'AJUSTEMENT_OUT', 'CANCEL_SALE', 'COMMANDE', 'CREATE_PRODUCT', 'DECONDTION_IN', 'DECONDTION_OUT', 'DELETE_PRODUCT', 'DELETE_SALE', 'DISABLE_PRODUCT', 'ENABLE_PRODUCT', 'ENTREE_STOCK', 'FORCE_STOCK', 'INVENTAIRE', 'MODIFICATION_PRIX_PRODUCT', 'MODIFICATION_PRIX_PRODUCT_A_LA_VENTE', 'MOUVEMENT_STOCK_IN', 'MOUVEMENT_STOCK_OUT', 'REAPPRO', 'SALE', 'SUPPRESSION', 'UPDATE_PRODUCT') not null,
  ajustement_id                bigint                                                                                                                                                                                                                                                                                                                                                                                                                                    null,
  decondition_id               bigint                                                                                                                                                                                                                                                                                                                                                                                                                                    null,
  fournisseur_produit_id       bigint                                                                                                                                                                                                                                                                                                                                                                                                                                    null,
  magasin_id                   bigint                                                                                                                                                                                                                                                                                                                                                                                                                                    not null,
  order_line_id                bigint                                                                                                                                                                                                                                                                                                                                                                                                                                    null,
  produit_id                   bigint                                                                                                                                                                                                                                                                                                                                                                                                                                    null,
  repartition_stock_produit_id bigint                                                                                                                                                                                                                                                                                                                                                                                                                                    null,
  sale_line_id                 bigint                                                                                                                                                                                                                                                                                                                                                                                                                                    null,
  user_id                      bigint                                                                                                                                                                                                                                                                                                                                                                                                                                    not null,
  constraint FK30wj3ywp114bifpc3xu7rex3i
    foreign key (produit_id) references produit (id),
  constraint FK6saiavquc76u5a44h5r974v5j
    foreign key (ajustement_id) references ajustement (id),
  constraint FK9mkq9q3qawkmnsc635662phvl
    foreign key (fournisseur_produit_id) references fournisseur_produit (id),
  constraint FKa3ij2sbmkbeipbln8tocfyg0
    foreign key (user_id) references user (id),
  constraint FKa7i5348dxyy1dnnhvo05nr4ve
    foreign key (magasin_id) references magasin (id),
  constraint FKjoy3lg6699dgt74famwcquqhm
    foreign key (repartition_stock_produit_id) references repartition_stock_produit (id),
  constraint FKqlsoqqqko7hj9gii5wwfdqhqa
    foreign key (decondition_id) references decondition (id),
  constraint FKst6t2ax1vlhtjk1okwpbx84hf
    foreign key (sale_line_id) references sales_line (id),
  constraint FKtbn30nahrd1gv1ba9dew8tbki
    foreign key (order_line_id) references order_line (id)
);

create index createdAt_index
  on inventory_transaction (created_at);

create index transaction_type_index
  on inventory_transaction (transaction_type);

create table lot_sold
(
  id           bigint auto_increment
    primary key,
  created_date datetime(6) not null,
  quantity     int         not null,
  lot_id       bigint      not null,
  sale_line_id bigint      not null,
  constraint UKa6wqp0hlgo298d08cb01e3e6k
    unique (lot_id, sale_line_id),
  constraint FKmffug438lanx9iamtbwaoxh22
    foreign key (lot_id) references lot (id),
  constraint FKq26nrtuvl710oxkgty3xrxvbv
    foreign key (sale_line_id) references sales_line (id)
);

create table sales_line_price
(
  id           bigint auto_increment
    primary key,
  montant      int    not null,
  prix         int    not null,
  reference_id bigint not null,
  sale_line_id bigint not null,
  constraint UK8dss9lmebucxk211x6ps2jegh
    unique (reference_id, sale_line_id),
  constraint FK9nmuhva8j8x5ibcuq0ldh829m
    foreign key (sale_line_id) references sales_line (id),
  constraint FKgkc3gw2y3mm5v1gbuawnpi9hb
    foreign key (reference_id) references produit_tiers_payant_prix (id)
);

create table third_party_sale_line
(
  id                      bigint auto_increment
    primary key,
  created_at              datetime(6)                                             not null,
  effective_update_date   datetime(6)                                             not null,
  montant                 int                                                     not null,
  montant_regle           int                                                     null,
  num_bon                 varchar(50)                                             null,
  statut                  enum ('ACTIF', 'CLOSED', 'DELETE', 'HALF_PAID', 'PAID') not null,
  taux                    smallint                                                not null,
  updated_at              datetime(6)                                             not null,
  client_tiers_payant_id  bigint                                                  not null,
  facture_tiers_payant_id bigint                                                  null,
  sale_id                 bigint                                                  not null,
  constraint UKdq87q92pm83m8suaievy9uhu0
    unique (client_tiers_payant_id, sale_id),
  constraint FK8ybq48g2pr953xk6r5ospdkjw
    foreign key (facture_tiers_payant_id) references facture_tiers_payant (id),
  constraint FKn8mjv4h5y4o993349xkyd2bs0
    foreign key (sale_id) references sales (id),
  constraint FKs9p5hj252j6gdbewvphdbnw9y
    foreign key (client_tiers_payant_id) references client_tiers_payant (id)
);

create table invoice_payment_item
(
  id                       bigint auto_increment
    primary key,
  montant_attendu          int    not null,
  montant_paye             int    not null,
  invoice_payment_id       bigint not null,
  third_party_sale_line_id bigint not null,
  constraint FK6rjhg3s15cyvb1wt5pl7athr2
    foreign key (third_party_sale_line_id) references third_party_sale_line (id),
  constraint FKmhbj2xqnqn61nr4p8yxejj3iv
    foreign key (invoice_payment_id) references payment_transaction (id)
);

create table warehouse_sequence
(
  name      varchar(255)     not null
    primary key,
  increment int(4) default 1 null,
  seq_value int    default 0 null
);
