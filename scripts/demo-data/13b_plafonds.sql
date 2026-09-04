-- ============================================================================
-- 13b_plafonds.sql — Plafonds de prise en charge tiers payant
--
-- Un plafond ne se démontre pas avec une valeur choisie au hasard : il faut
-- qu'un assuré soit DÉJÀ proche de le franchir, faute de quoi la règle ne se
-- déclenche jamais et l'écran de vente ne montre rien de particulier.
--
-- D'où la place de ce script : APRÈS 13_consommations.sql, qui recalcule la
-- consommation mensuelle de chaque contrat depuis les ventes réelles. Le
-- plafond est ensuite CALÉ SUR cette consommation — et non l'inverse. Une
-- valeur posée avant 13 serait silencieusement écrasée.
--
-- Rappel du mécanisme (TiersPayantCalculationService.applyMonthlyCeiling) :
--   * `tiers_payant.plafond_conso_client` borne ce qu'un organisme rembourse à
--     UN client sur le mois ;
--   * `client_tiers_payant.conso_mensuelle` porte ce qu'il a déjà consommé ;
--   * au-delà, la part assurance est ramenée au reliquat et la part patient
--     augmente d'autant ; avec `plafond_absolu_client`, elle tombe à zéro.
-- ============================================================================

\i _header.sql

\echo '>> 13b_plafonds : plafonds de prise en charge calés sur la consommation'

-- ---------------------------------------------------------------------------
-- Deux organismes plafonnent leurs assurés, chacun à sa façon.
--
-- Le plafond est fixé à 105 % de la plus forte consommation constatée chez
-- leurs assurés : les plus gros consommateurs se retrouvent ainsi à ~95 % de
-- leur droit, et la vente suivante déclenche le plafonnement.
-- ---------------------------------------------------------------------------
UPDATE tiers_payant tp
   SET plafond_conso_client = GREATEST((c.conso_max * 1.05)::bigint, 1000),
       plafond_absolu_client = (tp.name = 'MUGEFCI')
  FROM (
      SELECT ctp.tierspayant_id, max(ctp.conso_mensuelle) AS conso_max
        FROM client_tiers_payant ctp
       GROUP BY ctp.tierspayant_id
  ) c
 WHERE c.tierspayant_id = tp.id
   AND tp.name IN ('MUGEFCI', 'CNAM')
   AND c.conso_max > 0;

-- Filet : si aucune vente n'a alimenté ces organismes ce mois-ci, le plafond
-- resterait nul et le scénario muet. On pose alors une consommation d'amorce
-- sur deux contrats, cohérente avec le plafond retenu.
UPDATE client_tiers_payant ctp
   SET conso_mensuelle = (tp.plafond_conso_client * 0.95)::bigint
  FROM tiers_payant tp
 WHERE tp.id = ctp.tierspayant_id
   AND tp.name IN ('MUGEFCI', 'CNAM')
   AND tp.plafond_conso_client IS NOT NULL
   AND NOT EXISTS (
       SELECT 1 FROM client_tiers_payant c2
        WHERE c2.tierspayant_id = tp.id
          AND c2.conso_mensuelle > tp.plafond_conso_client * 0.8
   )
   AND ctp.id IN (
       SELECT id FROM client_tiers_payant c3
        WHERE c3.tierspayant_id = tp.id
        ORDER BY id
        LIMIT 2
   );

-- ---------------------------------------------------------------------------
-- Un cas CALIBRÉ, en chiffres ronds
--
-- Les plafonds ci-dessus se calent sur la consommation générée, qui change à
-- chaque chargement : les montants affichés à l'écran changent avec elle, et
-- un manuel ne peut pas les citer. On fige donc un cas nommé, en chiffres
-- ronds, sur lequel s'appuient la documentation et le parcours VTE-50 :
--
--   CNAM plafonne chaque assuré à 50 000 par mois ;
--   l'assuré CNAM01-000098 en a déjà consommé 35 000.
--
-- Il lui reste 15 000 de droit : toute vente dont la part CNAM dépasserait ce
-- reliquat est ramenée à 15 000, et la différence retombe sur le patient.
-- ---------------------------------------------------------------------------
UPDATE tiers_payant
   SET plafond_conso_client = 50000,
       plafond_absolu_client = FALSE
 WHERE name = 'CNAM';

UPDATE client_tiers_payant
   SET conso_mensuelle = 35000
 WHERE num = 'CNAM01-000098';

-- ---------------------------------------------------------------------------
-- Le carnet a lui aussi son plafond
--
-- Un carnet est un CRÉDIT accordé par l'employeur : il est plafonné, sinon il
-- n'aurait pas de sens. Le mécanisme est le même que pour une assurance —
-- `tiers_payant.plafond_conso_client` face à `client_tiers_payant.conso_mensuelle` —
-- et il produit le même effet : au-delà du reliquat, la part prise en charge
-- est ramenée à ce qui reste, et la différence revient au porteur.
--
-- Chiffres ronds, pour que le manuel puisse les citer :
--   le carnet plafonne chaque porteur à 50 000 par mois ;
--   le porteur CAR01-000021 en a déjà consommé 45 000, il lui reste 5 000.
--
-- Aucun parcours ne s'appuie sur ce cas : VTE-50 illustre le plafond sur une
-- assurance, où le taux rend visible ce que le plafond change. Il reste ici
-- pour la démonstration manuelle du carnet plafonné.
--
-- Le porteur de VTE-30 (CAR01-000045) reste à zéro de consommation : sa vente
-- de 38 620 passe sous le plafond et n'est donc pas affectée.
-- ---------------------------------------------------------------------------
UPDATE tiers_payant
   SET plafond_conso_client = 50000,
       plafond_absolu_client = FALSE
 WHERE categorie = 'CARNET';

UPDATE client_tiers_payant
   SET conso_mensuelle = 45000
 WHERE num = 'CAR01-000021';

UPDATE client_tiers_payant ctp
   SET conso_mensuelle = 0
  FROM tiers_payant tp
 WHERE tp.id = ctp.tierspayant_id
   AND tp.categorie = 'CARNET'
   AND ctp.num <> 'CAR01-000021';

\echo '   plafonds posés sur MUGEFCI (absolu) et CNAM (simple)'
