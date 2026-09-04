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
    -- NUL sur une facture individuelle : ce champ ne dit pas que l'organisme
    -- appartient à un groupe — cela se lit sur tiers_payant — mais que la
    -- FACTURE est une facture de groupe. Seul buildGroupeFacture le pose. Le
    -- recopier ici, alors que les douze organismes ont tous un groupe, rendait
    -- TOUTES les factures indiscernables de factures de groupe : bannière
    -- « Individuelles » à zéro, liste « Groupées » comptant un tableau vide,
    -- règlements tous classés groupés. Les vraies : étape 5.
    NULL::int,
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

-- ---------------------------------------------------------------------------
-- 4bis. Recalage du règlement de la facture sur celui de ses bons
--
-- Les deux montants étaient calculés séparément, chacun avec sa division
-- entière : 40 % du total de la facture d'un côté, 40 % de CHAQUE bon de
-- l'autre. Or la somme des arrondis inférieurs est plus petite que l'arrondi
-- inférieur de la somme — d'un franc par bon dans le pire cas. La facture
-- affichait donc un réglé que ses bons ne justifiaient pas, et l'encaissement
-- de 14b, dont le montant vient de la facture et les items des bons, ne
-- s'égalait pas à la somme de ses propres lignes.
--
-- Le bon fait foi : c'est lui que le suivi des créances détaille. La facture
-- totalise, et son statut suit — un arrondi peut ramener à zéro le règlement
-- d'une petite facture, qui n'est alors plus « partiellement réglée ».
-- ---------------------------------------------------------------------------
UPDATE facture_tiers_payant f
   SET montant_regle = agg.total,
       statut = CASE WHEN agg.total = 0                     THEN 'NOT_PAID'
                     WHEN agg.total >= f.montant_ttc::int   THEN 'PAID'
                     ELSE 'PARTIALLY_PAID' END
  FROM (SELECT t.facture_tiers_payant_id AS id,
               t.invoice_date,
               sum(t.montant_regle)::int AS total
          FROM third_party_sale_line t
         WHERE t.facture_tiers_payant_id IS NOT NULL
         GROUP BY t.facture_tiers_payant_id, t.invoice_date) agg
 WHERE f.id = agg.id
   AND f.invoice_date = agg.invoice_date
   AND f.montant_regle <> agg.total;

DROP TABLE tmp_facture_tva;
DROP TABLE tmp_facture;

-- ---------------------------------------------------------------------------
-- 5. Factures de GROUPE
--
-- Une facture de groupe est une facture PARENTE : tiers_payant_id nul,
-- groupe_tiers_payant_id renseigné, aucun bon en propre, et des factures filles
-- qui la désignent par (groupe_facture_tiers_payant_id,
-- groupe_facture_tiers_payant_invoice_date). C'est la forme que produit
-- EditionByGroupTiersService, et la seule que la liste « Groupées » sait lire :
-- elle joint les filles en INNER JOIN, donc une parente sans fille n'apparaît
-- jamais à l'écran.
--
-- Le jeu de données n'en contenait aucune : l'onglet « Groupées », le règlement
-- groupé et la bannière du même nom n'avaient rien à montrer.
--
-- On en fabrique pour un seul groupe — MUTUELLES PUBLIQUES, soit CNAM, MUGEFCI
-- et CNPS — sur les deux dernières éditions. Les neuf autres organismes gardent
-- des factures individuelles : l'écran doit montrer les deux natures.
--
-- Les montants de la parente sont la SOMME de ceux de ses filles, comme le fait
-- AbstractEditionFactureService. Une parente laissée à zéro ferait afficher à la
-- bannière un total facturé nul et un reste à recouvrer négatif, puisqu'elle
-- somme montant_net sur les factures racines.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_facture_groupe AS
SELECT
    nextval('id_facture_seq') AS id,
    g.*,
    row_number() OVER (ORDER BY g.invoice_date) AS rang
FROM (
    SELECT tp.groupe_tiers_payant_id       AS groupe_id,
           f.invoice_date,
           min(f.debut_periode)            AS debut_periode,
           max(f.fin_periode)              AS fin_periode,
           sum(f.montant_ttc)              AS montant_ttc,
           sum(f.montant_ht)               AS montant_ht,
           sum(f.montant_tva)              AS montant_tva,
           sum(f.montant_net)              AS montant_net,
           sum(f.montant_regle)::int       AS montant_regle
      FROM facture_tiers_payant f
      JOIN tiers_payant tp          ON tp.id = f.tiers_payant_id
      JOIN groupe_tiers_payant gtp  ON gtp.id = tp.groupe_tiers_payant_id
     WHERE gtp.name = 'MUTUELLES PUBLIQUES'
       AND f.groupe_facture_tiers_payant_id IS NULL
       -- Les deux dernières éditions : les plus récentes sont celles que
       -- l'écran montre en premier, période par défaut à un mois glissant.
       AND f.invoice_date IN (SELECT invoice_date
                                FROM facture_tiers_payant
                               GROUP BY invoice_date
                               ORDER BY invoice_date DESC
                               LIMIT 2)
     GROUP BY tp.groupe_tiers_payant_id, f.invoice_date
) g;

INSERT INTO facture_tiers_payant (
    id, invoice_date, num_facture,
    debut_periode, fin_periode, facture_provisoire,
    montant_regle, remise_forfetaire, generation_code,
    montant_ttc, montant_ht, montant_tva, montant_net,
    repartitions, statut, origine_generation,
    tiers_payant_id, groupe_tiers_payant_id, user_id, created, updated
)
SELECT
    g.id, g.invoice_date,
    -- Le compteur reprend au dernier numéro émis, comme getLastFactureNumero.
    to_char(g.invoice_date, 'YYYY') || '_' || lpad((n.dernier + g.rang)::text, 4, '0'),
    g.debut_periode, g.fin_periode, false,
    g.montant_regle, 0,
    -- Toute l'édition d'un groupe partage UN code de génération : c'est par lui
    -- que l'impression en lot retrouve les factures d'un même passage. Les
    -- filles sont recalées juste après.
    9000 + g.rang,
    g.montant_ttc, g.montant_ht, g.montant_tva, g.montant_net,
    -- Pas de répartitions TVA sur la parente : applyTotalsAndRepartitions ne
    -- s'applique qu'aux filles, la parente n'agrège que les totaux.
    '[]'::jsonb,
    CASE WHEN g.montant_regle = 0                   THEN 'NOT_PAID'
         WHEN g.montant_regle >= g.montant_ttc::int THEN 'PAID'
         ELSE 'PARTIALLY_PAID' END,
    'MANUELLE',
    -- tiers_payant_id NUL : une facture de groupe ne vise pas un organisme mais
    -- le groupe entier. C'est ce qui la distingue d'une facture individuelle,
    -- et pourquoi la liste individuelle la laisse de côté.
    NULL::int, g.groupe_id,
    u.id,
    g.invoice_date + TIME '09:30:00',
    g.invoice_date + TIME '09:30:00'
FROM tmp_facture_groupe g
CROSS JOIN LATERAL (SELECT id FROM app_user WHERE login = 'admin' LIMIT 1) u
CROSS JOIN LATERAL (SELECT COALESCE(max(split_part(num_facture, '_', 2)::int), 0) AS dernier
                      FROM facture_tiers_payant) n;

-- Rattachement des filles. La clé étrangère porte sur les DEUX colonnes de la
-- clé composite de la parente.
UPDATE facture_tiers_payant fille
   SET groupe_facture_tiers_payant_id           = g.id,
       groupe_facture_tiers_payant_invoice_date = g.invoice_date,
       generation_code                          = 9000 + g.rang
  FROM tmp_facture_groupe g
  JOIN tiers_payant tp ON tp.groupe_tiers_payant_id = g.groupe_id
 WHERE fille.tiers_payant_id = tp.id
   AND fille.invoice_date = g.invoice_date
   AND fille.id <> g.id;

DROP TABLE tmp_facture_groupe;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_fact int; v_ecart int; v_periode int; v_regle int; v_statuts int;
    v_groupe int; v_orphelines int; v_ecart_groupe int; v_melange int;
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

    -- Factures de groupe : une parente se reconnaît à son organisme nul.
    SELECT count(*) INTO v_groupe FROM facture_tiers_payant
     WHERE tiers_payant_id IS NULL AND groupe_tiers_payant_id IS NOT NULL;

    -- Une parente sans fille n'apparaît jamais dans la liste groupée, qui joint
    -- les filles en INNER JOIN — mais elle serait comptée par le paginateur.
    SELECT count(*) INTO v_orphelines FROM facture_tiers_payant p
     WHERE p.tiers_payant_id IS NULL
       AND NOT EXISTS (SELECT 1 FROM facture_tiers_payant f
                        WHERE f.groupe_facture_tiers_payant_id = p.id
                          AND f.groupe_facture_tiers_payant_invoice_date = p.invoice_date);

    -- La parente totalise ses filles : c'est ce que somme la bannière.
    SELECT count(*) INTO v_ecart_groupe FROM (
        SELECT p.id FROM facture_tiers_payant p
          JOIN facture_tiers_payant f
            ON f.groupe_facture_tiers_payant_id = p.id
           AND f.groupe_facture_tiers_payant_invoice_date = p.invoice_date
         WHERE p.tiers_payant_id IS NULL
         GROUP BY p.id, p.montant_net, p.montant_regle
        HAVING p.montant_net <> sum(f.montant_net)
            OR p.montant_regle <> sum(f.montant_regle)::int) y;

    -- Une facture individuelle ne porte JAMAIS de groupe : sinon la bannière
    -- « Individuelles » l'ignore et la liste « Groupées » la compte à tort.
    SELECT count(*) INTO v_melange FROM facture_tiers_payant
     WHERE tiers_payant_id IS NOT NULL AND groupe_tiers_payant_id IS NOT NULL;

    IF v_fact < 20 THEN RAISE EXCEPTION 'Factures : % (attendu >= 20)', v_fact; END IF;
    IF v_ecart > 0 THEN RAISE EXCEPTION '% facture(s) dont le total contredit les bons', v_ecart; END IF;
    IF v_periode > 0 THEN RAISE EXCEPTION '% bon(s) hors de la période facturée', v_periode; END IF;
    IF v_regle > 0 THEN RAISE EXCEPTION '% facture(s) au règlement incohérent', v_regle; END IF;
    IF v_statuts > 0 THEN RAISE EXCEPTION '% facture(s) au statut hors contrainte', v_statuts; END IF;
    IF v_groupe < 2 THEN RAISE EXCEPTION 'Factures de groupe : % (attendu >= 2) — onglet « Groupées » vide', v_groupe; END IF;
    IF v_orphelines > 0 THEN RAISE EXCEPTION '% facture(s) de groupe sans fille : comptees mais jamais affichees', v_orphelines; END IF;
    IF v_ecart_groupe > 0 THEN RAISE EXCEPTION '% facture(s) de groupe dont le total contredit ses filles', v_ecart_groupe; END IF;
    IF v_melange > 0 THEN RAISE EXCEPTION '% facture(s) individuelle(s) portant un groupe : indiscernables des factures de groupe', v_melange; END IF;

    RAISE NOTICE '% factures tiers-payant, dont % de groupe.', v_fact, v_groupe;
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
