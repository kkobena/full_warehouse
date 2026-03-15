
CREATE TABLE planning_inventaire_tournant
(
  id integer PRIMARY KEY,
  libelle                VARCHAR(200) NOT NULL,
  frequence              VARCHAR(20)  NOT NULL,
  critere                VARCHAR(30)  NOT NULL,
  storage_id             INT REFERENCES storage (id),

  user_id                INT REFERENCES app_user (id),
  prochaine_execution    DATE         NOT NULL,
  actif                  BOOLEAN      NOT NULL DEFAULT true,
  critere_index_courant  INT          NOT NULL DEFAULT 0, -- index de rotation dans la liste des critères
  classe_pareto_courante VARCHAR(1)   NULL,               -- Pour critère ABC : 'A', 'B' ou 'C'
  nb_executions          INT          NOT NULL DEFAULT 0,
  derniere_execution     DATE         NULL,
  created_at             TIMESTAMP    NOT NULL DEFAULT NOW(),
  updated_at             TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pit_storage_actif ON planning_inventaire_tournant (storage_id, actif);
CREATE INDEX idx_pit_prochaine_exec ON planning_inventaire_tournant (prochaine_execution) WHERE actif = true;
CREATE INDEX idx_pit_user_id ON planning_inventaire_tournant (user_id);


ALTER TABLE store_inventory
  DROP CONSTRAINT store_inventory_inventory_category_check;

ALTER TABLE store_inventory
  ADD CONSTRAINT store_inventory_inventory_category_check
    CHECK (
      inventory_category IN (
                             'MAGASIN',
                             'STORAGE',
                             'RAYON',
                             'FAMILLY',
                             'PERIME',
                             'ALERTE_PEREMPTION',
                             'VENDU',
                             'INVENDU',
                             'SOUS_SEUIL',
                             'EN_RUPTURE',
                             'GROSSISTE',
                             'SELECTION_PRODUIT',
                             'ABC'
        )
      );

