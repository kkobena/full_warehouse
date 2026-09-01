-- ============================================================================
-- 14_facturation.sql — Factures tiers-payant
--
-- facture_tiers_payant a une PK COMPOSITE (id, invoice_date). Les ventilations
-- lui sont rattachées par une FK sur DEUX colonnes :
--     third_party_sale_line.facture_tiers_payant_id
--   + third_party_sale_line.invoice_date
--
-- Une facture regroupe les ventilations d'un organisme sur un mois. On ne
-- facture QUE les mois révolus : le mois courant est encore en cours de
-- constitution, et le facturer donnerait un montant qui bougerait encore.
--
-- statut : contrainte CHECK sur PAID / NOT_PAID / PARTIALLY_PAID.
-- origine_generation est NOT NULL : MANUELLE ou AUTO.
-- ============================================================================

\i _header.sql

\echo '>> 14_facturation : factures tiers-payant'

-- ---------------------------------------------------------------------------
-- 1. Regroupement des ventilations par organisme et par mois révolu
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_facture AS
SELECT
    nextval('id_facture_seq') AS id,
    x.tierspayant_id,
    x.annee,
    x.mois,
    -- La facture est éditée le 5 du mois suivant.
    (make_date(x.annee, x.mois, 1) + INTERVAL '1 month' + INTERVAL '4 days')::date AS invoice_date,
    make_date(x.annee, x.mois, 1) AS debut_periode,
    (make_date(x.annee, x.mois, 1) + INTERVAL '1 month' - INTERVAL '1 day')::date AS fin_periode,
    x.montant,
    row_number() OVER (ORDER BY x.annee, x.mois, x.tierspayant_id) AS rang
FROM (
    SELECT
        ctp.tierspayant_id,
        extract(year  FROM t.sale_date)::int AS annee,
        extract(month FROM t.sale_date)::int AS mois,
        sum(t.montant)::int AS montant
    FROM third_party_sale_line t
    JOIN client_tiers_payant ctp ON ctp.id = t.client_tiers_payant_id
    GROUP BY ctp.tierspayant_id,
             extract(year FROM t.sale_date),
             extract(month FROM t.sale_date)
) x
-- Mois révolus uniquement.
WHERE make_date(x.annee, x.mois, 1) < date_trunc('month', CURRENT_DATE)::date
  AND x.montant > 0;

-- ---------------------------------------------------------------------------
-- 2. Ventilation TVA de la facture
--
-- Agrégée depuis les répartitions déjà calculées sur chaque ligne : la facture
-- ne recalcule rien, elle totalise. C'est ce qui garantit qu'elle se recoupe
-- avec les bons qu'elle regroupe.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_facture_tva AS
SELECT
    f.id AS facture_id,
    (e->>'tva')::int                    AS tva,
    sum((e->>'montantTtc')::numeric)    AS ttc,
    sum((e->>'montantHt')::numeric)     AS ht,
    sum((e->>'montantTva')::numeric)    AS tva_montant
FROM tmp_facture f
JOIN client_tiers_payant ctp ON ctp.tierspayant_id = f.tierspayant_id
JOIN third_party_sale_line t ON t.client_tiers_payant_id = ctp.id
                            AND extract(year  FROM t.sale_date)::int = f.annee
                            AND extract(month FROM t.sale_date)::int = f.mois
CROSS JOIN LATERAL jsonb_array_elements(t.repartitions) e
GROUP BY f.id, (e->>'tva')::int;

-- ---------------------------------------------------------------------------
-- 3. Insertion des factures
--
-- Le statut suit l'ancienneté : les plus anciennes sont réglées, les récentes
-- encore en attente. Un tiers-payant qui n'aurait que des factures payées ne
-- montrerait pas l'écran de suivi des créances.
-- ---------------------------------------------------------------------------
INSERT INTO facture_tiers_payant (
    id, invoice_date, num_facture,
    debut_periode, fin_periode, facture_provisoire,
    montant_regle, remise_forfetaire, generation_code,
    montant_ttc, montant_ht, montant_tva, montant_net,
    repartitions, statut, origine_generation,
    tiers_payant_id, groupe_tiers_payant_id, user_id, created, updated
)
SELECT
    f.id, f.invoice_date,
    -- Le numero suit le format de l'APPLICATION : ANNEE_0001 (voir
    -- AbstractEditionFactureService.getFactureNumber). Le loader ecrivait
    -- « FA2026080049 », une forme inventee ici : l'ecran, qui n'affiche que ce
    -- qui suit le souligne, rendait alors le numero entier, et le manuel
    -- montrait une numerotation que le logiciel ne produit jamais. Le rang est
    -- global a l'annee, comme le compteur du service.
    to_char(f.invoice_date, 'YYYY') || '_' || lpad(f.rang::text, 4, '0'),
    f.debut_periode, f.fin_periode, false,
    -- Réglée intégralement, partiellement, ou pas du tout.
    CASE WHEN f.rang % 3 = 0 THEN f.montant
         WHEN f.rang % 3 = 1 THEN (f.montant * 4 / 10)
         ELSE 0 END,
    0,
    f.rang,
    COALESCE(a.ttc, f.montant), COALESCE(a.ht, f.montant),
    COALESCE(a.tva, 0), COALESCE(a.ttc, f.montant),
    COALESCE(a.rep, '[]'::jsonb),
    CASE WHEN f.rang % 3 = 0 THEN 'PAID'
         WHEN f.rang % 3 = 1 THEN 'PARTIALLY_PAID'
         ELSE 'NOT_PAID' END,
    'MANUELLE',
    f.tierspayant_id,
    -- Le groupe du tiers payant est RECOPIÉ sur la facture. Sans lui, toute la
    -- chaîne des créances part à zéro : la synthèse par groupe, la bande
    -- d'indicateurs de l'accueil et le vieillissement des créances regroupent
    -- par ce champ, laissé nul jusqu'ici.
    tp.groupe_tiers_payant_id,
    u.id,
    f.invoice_date + TIME '09:00:00',
    f.invoice_date + TIME '09:00:00'
FROM tmp_facture f
JOIN tiers_payant tp ON tp.id = f.tierspayant_id
CROSS JOIN LATERAL (SELECT id FROM app_user WHERE login = 'admin' LIMIT 1) u
LEFT JOIN (
    SELECT facture_id,
           sum(ttc) AS ttc, sum(ht) AS ht, sum(tva_montant) AS tva,
           jsonb_agg(jsonb_build_object(
               'montantTtc', ttc::float8, 'montantHt', ht::float8,
               'montantTva', tva_montant::float8, 'montantNet', ttc::float8,
               'tva', tva
           ) ORDER BY tva) AS rep
      FROM tmp_facture_tva GROUP BY facture_id
) a ON a.facture_id = f.id;

-- ---------------------------------------------------------------------------
-- 4. Rattachement des ventilations à leur facture
--
-- La clé étrangère porte sur les DEUX colonnes de la clé composite.
-- montant_regle de la ligne suit le statut de la facture : c'est ce qui
-- alimente le suivi des créances bon par bon.
-- ---------------------------------------------------------------------------
UPDATE third_party_sale_line t
   SET facture_tiers_payant_id = f.id,
       invoice_date = f.invoice_date,
       montant_regle = CASE ft.statut
                           WHEN 'PAID'           THEN t.montant
                           WHEN 'PARTIALLY_PAID' THEN (t.montant * 4 / 10)
                           ELSE 0 END,
       statut = CASE ft.statut
                    WHEN 'PAID'           THEN 'PAID'
                    WHEN 'PARTIALLY_PAID' THEN 'HALF_PAID'
                    ELSE 'ACTIF' END,
       updated_at = NOW()
  FROM tmp_facture f
  JOIN facture_tiers_payant ft ON ft.id = f.id AND ft.invoice_date = f.invoice_date
  JOIN client_tiers_payant ctp ON ctp.tierspayant_id = f.tierspayant_id
 WHERE t.client_tiers_payant_id = ctp.id
   AND extract(year  FROM t.sale_date)::int = f.annee
   AND extract(month FROM t.sale_date)::int = f.mois;

DROP TABLE tmp_facture_tva;
DROP TABLE tmp_facture;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_fact int; v_ecart int; v_periode int; v_regle int; v_statuts int;
BEGIN
    SELECT count(*) INTO v_fact FROM facture_tiers_payant;

    -- Le montant de la facture doit égaler la somme des bons regroupés.
    SELECT count(*) INTO v_ecart FROM (
        SELECT ft.id FROM facture_tiers_payant ft
          JOIN third_party_sale_line t ON t.facture_tiers_payant_id = ft.id
                                      AND t.invoice_date = ft.invoice_date
         GROUP BY ft.id, ft.montant_ttc
        HAVING ft.montant_ttc::int <> sum(t.montant)) x;

    -- Les bons regroupés doivent tomber dans la période facturée.
    SELECT count(*) INTO v_periode FROM third_party_sale_line t
      JOIN facture_tiers_payant ft ON ft.id = t.facture_tiers_payant_id
                                  AND ft.invoice_date = t.invoice_date
     WHERE t.sale_date < ft.debut_periode OR t.sale_date > ft.fin_periode;

    SELECT count(*) INTO v_regle FROM facture_tiers_payant
     WHERE montant_regle > montant_ttc OR montant_regle < 0;

    SELECT count(*) INTO v_statuts FROM facture_tiers_payant
     WHERE statut NOT IN ('PAID', 'NOT_PAID', 'PARTIALLY_PAID');

    IF v_fact < 20 THEN RAISE EXCEPTION 'Factures : % (attendu >= 20)', v_fact; END IF;
    IF v_ecart > 0 THEN RAISE EXCEPTION '% facture(s) dont le total contredit les bons', v_ecart; END IF;
    IF v_periode > 0 THEN RAISE EXCEPTION '% bon(s) hors de la période facturée', v_periode; END IF;
    IF v_regle > 0 THEN RAISE EXCEPTION '% facture(s) au règlement incohérent', v_regle; END IF;
    IF v_statuts > 0 THEN RAISE EXCEPTION '% facture(s) au statut hors contrainte', v_statuts; END IF;

    RAISE NOTICE '% factures tiers-payant.', v_fact;
END $$;


-- ---------------------------------------------------------------------------
-- Une facture IMPAYEE pour l'organisme sans email
--
-- La certification fiscale (FNE) n'est offerte que sur une facture definitive
-- ET impayee. Or c'est justement sur l'organisme incomplet — ASACI, prive
-- d'adresse electronique a dessein — qu'il faut pouvoir tenter la
-- certification, pour voir le garde-fou la refuser. Sans facture impayee chez
-- lui, le bouton n'apparait sur aucune ligne et le cas reste indemontrable.
--
-- On force les deux plus recentes de ses factures a l'impaye. Un parcours de
-- règlement exécuté avant FAC-30 peut en consommer une ; la seconde préserve le
-- cas de refus FNE. Aucun encaissement initial ne les vise : 14b_reglements ne
-- regle que les factures dont montant_regle est deja non nul.
-- ---------------------------------------------------------------------------
UPDATE facture_tiers_payant f
   SET statut = 'NOT_PAID',
       montant_regle = 0,
       updated = now()
 WHERE (f.id, f.invoice_date) IN (
     SELECT ftp.id, ftp.invoice_date
       FROM facture_tiers_payant ftp
       JOIN tiers_payant tp ON tp.id = ftp.tiers_payant_id
      WHERE tp.name = 'ASACI' AND NOT ftp.facture_provisoire
      ORDER BY ftp.invoice_date DESC
      LIMIT 2
 );

-- Les LIGNES suivent la facture : une facture impayee dont les bons se disent
-- regles est une incoherence que le controle « Reglement de ligne coherent avec
-- la facture » releve aussitot.
UPDATE third_party_sale_line t
   SET montant_regle = 0
  FROM facture_tiers_payant f
  JOIN tiers_payant tp ON tp.id = f.tiers_payant_id
 WHERE t.facture_tiers_payant_id = f.id
   AND t.invoice_date = f.invoice_date
   AND tp.name = 'ASACI'
   AND f.statut = 'NOT_PAID';

DO $$
DECLARE v_n INTEGER;
BEGIN
    SELECT count(*) INTO v_n
      FROM facture_tiers_payant f
      JOIN tiers_payant tp ON tp.id = f.tiers_payant_id
     WHERE tp.name = 'ASACI' AND f.statut = 'NOT_PAID' AND NOT f.facture_provisoire;
    IF v_n < 2 THEN
        RAISE EXCEPTION 'Seulement % facture(s) impayee(s) chez l''organisme sans email : un parcours anterieur peut consommer l''unique cas FNE', v_n;
    END IF;
END $$;

\echo '<< 14_facturation : terminé'
