-- Onglet « Rentabilité & remises » du pilotage (docs/PLAN-PILOTAGE-ONGLETS.md §4).
--
-- Chaque vente porte désormais, dans les deux agrégats de ventes :
--   * taux_remise   : remise de la vente en % de son montant brut, arrondi (0 sans remise ; au moins 1 dès qu'il y en a une).
--                     Les tranches (0, < 5 %, 5-10 %…) se calculent à la lecture : changer les seuils ne demande aucun recalcul ;
--   * octroi_remise : AUCUNE, PRIVILEGE (le vendeur détient le privilège PR_AJOUTER_REMISE_VENTE) ou AUTORISEE (un détenteur
--                     a saisi sa clé de sécurité : ligne de utilisation_cle_securite sur la vente) ;
--   * autorisant_id : le propriétaire de la clé, pour une remise autorisée.

ALTER TABLE pilotage_vente_jour ADD COLUMN IF NOT EXISTS taux_remise SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE pilotage_vente_jour ADD COLUMN IF NOT EXISTS octroi_remise VARCHAR(12) NOT NULL DEFAULT 'AUCUNE';
ALTER TABLE pilotage_vente_jour ADD COLUMN IF NOT EXISTS autorisant_id INTEGER;
ALTER TABLE pilotage_vente_ligne_jour ADD COLUMN IF NOT EXISTS taux_remise SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE pilotage_vente_ligne_jour ADD COLUMN IF NOT EXISTS octroi_remise VARCHAR(12) NOT NULL DEFAULT 'AUCUNE';
ALTER TABLE pilotage_vente_ligne_jour ADD COLUMN IF NOT EXISTS autorisant_id INTEGER;

INSERT INTO app_configuration (name, value, description, created, updated, value_type)
VALUES ('APP_PILOTAGE_TRANCHES_REMISE', '5,10,20',
        'Seuils (en % du montant de la vente) des tranches de remise du pilotage, séparés par des virgules (défaut : 5,10,20).',
        NOW(), NOW(), 'STRING'),
       ('APP_PILOTAGE_SEUIL_FAIBLE_MARGE', '15',
        'Taux de marge (en %) sous lequel un produit est signalé « à faible marge » dans le pilotage (défaut : 15).',
        NOW(), NOW(), 'NUMBER'),
       ('APP_PILOTAGE_ALERTE_REMISE_VENDEUR', '2',
        'Multiple du taux de remise moyen de l''équipe au-delà duquel le taux d''un vendeur est signalé (défaut : 2).',
        NOW(), NOW(), 'NUMBER')
ON CONFLICT (name) DO NOTHING;

-- Taux, octroi et autorisant de chaque vente clôturée de la période ; lue par les deux agrégats.
CREATE OR REPLACE FUNCTION pilotage_remises_ventes(p_du DATE, p_au DATE)
  RETURNS TABLE
          (
            vente_id      BIGINT,
            vente_date    DATE,
            taux_remise   SMALLINT,
            octroi_remise VARCHAR(12),
            autorisant_id INTEGER
          )
  LANGUAGE sql
  STABLE AS
$$
SELECT s.id,
       s.sale_date,
       CASE
         WHEN COALESCE(s.discount_amount, 0) > 0 AND s.sales_amount > 0
           THEN GREATEST(1, LEAST(100, ROUND(s.discount_amount * 100.0 / s.sales_amount)))::SMALLINT
         ELSE 0::SMALLINT
       END,
       (CASE
          WHEN COALESCE(s.discount_amount, 0) = 0 THEN 'AUCUNE'
          WHEN a.autorisant_id IS NOT NULL THEN 'AUTORISEE'
          ELSE 'PRIVILEGE'
        END)::VARCHAR(12),
       CASE WHEN COALESCE(s.discount_amount, 0) > 0 THEN a.autorisant_id END
FROM sales s
       LEFT JOIN (SELECT u.entity_id AS vente_id, MAX(u.clesecuriteowner_id) AS autorisant_id
                  FROM utilisation_cle_securite u
                         JOIN nav_item n ON n.id = u.navitem_id
                  WHERE n.code = 'PR_AJOUTER_REMISE_VENTE'
                  GROUP BY u.entity_id) a ON a.vente_id = s.id
WHERE s.sale_date BETWEEN p_du AND p_au
  AND s.statut = 'CLOSED'
$$;

CREATE OR REPLACE FUNCTION pilotage_recalculer_ventes(p_du DATE, p_au DATE) RETURNS VOID
  LANGUAGE plpgsql AS
$$
BEGIN
  DELETE FROM pilotage_vente_ligne_jour WHERE jour BETWEEN p_du AND p_au;
  INSERT INTO pilotage_vente_ligne_jour (jour, produit_id, magasin_id, vendeur_id, nature_vente, type_prescription, categorie_ca,
                                         taux_remise, octroi_remise, autorisant_id, quantite_demandee, quantite_servie, quantite_avoir,
                                         montant_ttc, montant_ht, remise, cout_ttc, cout_ht, nb_lignes)
  SELECT s.sale_date,
         sl.produit_id,
         s.magasin_id,
         s.seller_id,
         s.nature_vente,
         s.type_prescription,
         s.ca,
         r.taux_remise,
         r.octroi_remise,
         r.autorisant_id,
         SUM(sl.quantity_requested),
         SUM(sl.quantity_sold),
         SUM(COALESCE(sl.quantity_avoir, 0)),
         SUM(sl.sales_amount),
         SUM(ROUND(sl.sales_amount * 100.0 / (100 + COALESCE(sl.tax_value, 0)))),
         SUM(COALESCE(sl.discount_amount, 0)),
         SUM(COALESCE(sl.cost_amount, 0)::BIGINT * sl.quantity_requested),
         SUM(ROUND(COALESCE(sl.cost_amount, 0)::BIGINT * sl.quantity_requested * 100.0 / (100 + COALESCE(sl.tax_value, 0)))),
         COUNT(*)
  FROM sales s
         JOIN sales_line sl ON sl.sales_id = s.id AND sl.sales_sale_date = s.sale_date
         JOIN pilotage_remises_ventes(p_du, p_au) r ON r.vente_id = s.id AND r.vente_date = s.sale_date
  WHERE s.sale_date BETWEEN p_du AND p_au
    AND s.statut = 'CLOSED'
    AND NOT s.canceled
  GROUP BY s.sale_date, sl.produit_id, s.magasin_id, s.seller_id, s.nature_vente, s.type_prescription, s.ca, r.taux_remise,
           r.octroi_remise, r.autorisant_id;

  DELETE FROM pilotage_vente_jour WHERE jour BETWEEN p_du AND p_au;
  INSERT INTO pilotage_vente_jour (jour, heure, magasin_id, vendeur_id, caissier_id, nature_vente, type_prescription, categorie_ca,
                                   taux_remise, octroi_remise, autorisant_id, annulee, nb_ventes, montant_ttc, montant_ht,
                                   montant_net, remise, part_tiers_payant, nb_ventes_client_connu)
  SELECT s.sale_date,
         EXTRACT(HOUR FROM s.created_at)::SMALLINT,
         s.magasin_id,
         s.seller_id,
         s.caissier_id,
         s.nature_vente,
         s.type_prescription,
         s.ca,
         r.taux_remise,
         r.octroi_remise,
         r.autorisant_id,
         s.canceled,
         COUNT(*),
         SUM(s.sales_amount),
         SUM(COALESCE(s.ht_amount, 0)),
         SUM(COALESCE(s.net_amount, 0)),
         SUM(COALESCE(s.discount_amount, 0)),
         SUM(COALESCE(s.part_tiers_payant, 0)),
         COUNT(s.customer_id)
  FROM sales s
         JOIN pilotage_remises_ventes(p_du, p_au) r ON r.vente_id = s.id AND r.vente_date = s.sale_date
  WHERE s.sale_date BETWEEN p_du AND p_au
    AND s.statut = 'CLOSED'
  GROUP BY s.sale_date, EXTRACT(HOUR FROM s.created_at), s.magasin_id, s.seller_id, s.caissier_id, s.nature_vente,
           s.type_prescription, s.ca, r.taux_remise, r.octroi_remise, r.autorisant_id, s.canceled;
END;
$$;

-- Tout l'historique, pour que les tranches et l'octroi couvrent aussi les années comparées.
SELECT pilotage_recalculer_ventes(COALESCE(MIN(sale_date), CURRENT_DATE), COALESCE(MAX(sale_date), CURRENT_DATE))
FROM sales;
