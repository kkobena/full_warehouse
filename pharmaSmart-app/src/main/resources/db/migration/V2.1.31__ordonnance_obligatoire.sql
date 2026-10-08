-- Produit sur ordonnance : la vente doit porter une ordonnance OU un prescripteur (décision de l'utilisateur).
--
-- vente_prescripteur : prescripteur déclaré sur une vente qui n'a pas d'ordonnance saisie. Quand une
-- ordonnance est rattachée (ordonnance_vente), le prescripteur se lit par elle : pas de doublon ici.
CREATE TABLE vente_prescripteur (
  sales_id        bigint  NOT NULL,
  sales_date      date    NOT NULL,
  prescripteur_id integer NOT NULL REFERENCES prescripteur (id),
  created_at      timestamp NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
  created_by_id   integer REFERENCES app_user (id),
  PRIMARY KEY (sales_id, sales_date),
  CONSTRAINT vente_prescripteur_sales_fk FOREIGN KEY (sales_id, sales_date)
    REFERENCES sales (id, sale_date) ON DELETE CASCADE
);

CREATE INDEX vente_prescripteur_prescripteur_idx ON vente_prescripteur (prescripteur_id);

-- 1 = la clôture d'une vente contenant un produit dont le statut légal exige une ordonnance est refusée
-- tant qu'aucune ordonnance ni aucun prescripteur n'est rattaché ; 0 = pas d'exigence.
INSERT INTO app_configuration (name, value, description, value_type)
VALUES ('APP_VENTE_ORDONNANCE_OBLIGATOIRE', '1',
        'Vente d''un produit sur ordonnance : exiger une ordonnance ou un prescripteur à la clôture (1) ou non (0)', 'NUMBER')
ON CONFLICT (name) DO NOTHING;
