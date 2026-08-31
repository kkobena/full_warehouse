\i _header.sql

-- ============================================================================
-- 19_declaration_ca.sql — Retraitement du chiffre d'affaires a declarer
--
-- Le module « Retraitement du CA » s'ouvrait entierement vide : zero rayon
-- exclu, zero tiers-payant exclu, zero ligne journalisee, zero ponction. Les
-- douze ecrans montraient des tableaux a zero, et rien n'y disait ce que le
-- module fait.
--
-- CE QUE LE MODELE ATTEND, verifie sur la base :
--
--   * `rayon.to_exclude` et `tiers_payant.to_be_exclude` portent la REGLE :
--     ce qui, desormais, ne doit plus entrer dans l'assiette.
--   * `sales_line.exclusion_motif` porte l'EFFET, ligne a ligne
--     (RAYON / TIERS_PAYANT / UG / PONCTION / MANUEL), et
--     `amount_to_be_taken_into_account` le montant qui reste declarable.
--   * Regler la regle ne retraite PAS le passe : l'ecran le dit lui-meme,
--     « cocher comme decocher n'agit que sur les ventes a venir ». Les lignes
--     deja journalisees doivent donc etre posees ici explicitement — c'est
--     exactement ce qu'aurait produit une officine ayant regle ses exclusions
--     il y a plusieurs mois.
--   * Une vente dont une ligne porte deja un motif n'est PAS ponctionnable
--     (cf. ExclusionMotif) : les lignes exclues et les lignes ponctionnees ne
--     se recouvrent jamais.
--   * `ca_ponction` s'identifie par une SEQUENCE nommee, `id_ca_ponction_seq`,
--     et non par une colonne identite : oublier `nextval` viole la cle primaire.
--   * `sales_line` rattache sa vente par `sales_id` / `sales_sale_date` — au
--     PLURIEL, et non `sale_id` : le champ Java s'appelle `sales`, Hibernate a
--     concatene sans underscore. La colonne `sale_date` existe aussi, mais
--     c'est la date de la LIGNE.
--
-- Se place apres 18_repartitions_stock.sql : les ventes doivent exister.
-- ============================================================================

\echo '>> 19_declaration_ca : exclusions, journaux et ponctions'

DELETE FROM ca_ponction;
UPDATE sales_line
   SET exclusion_motif = NULL,
       amount_to_be_taken_into_account = sales_amount
 WHERE exclusion_motif IS NOT NULL;
UPDATE rayon SET to_exclude = false WHERE to_exclude;
UPDATE tiers_payant SET to_be_exclude = false WHERE to_be_exclude;

-- ---------------------------------------------------------------------------
-- 1. Les REGLES : ce qui sort de l'assiette
--
-- Un rayon de parapharmacie et un organisme dont le chiffre d'affaires est
-- declare ailleurs : les deux cas reels qui motivent le module.
-- ---------------------------------------------------------------------------
UPDATE rayon
   SET to_exclude = true
 -- NUTRITION ET DIETETIQUE porte de vraies ventes ; PARAPHARMACIE est exclue
 -- par principe mais n'en a aucune dans la demo, et un journal vide ne
 -- demontrerait rien.
 WHERE libelle IN ('NUTRITION ET DIETETIQUE', 'PARAPHARMACIE');

UPDATE tiers_payant
   SET to_be_exclude = true
 WHERE name = 'CARNET SOC';

-- La cle est `EXCLUDE_FREE_UNIT` (cf. EntityConstant) : un nom invente ici ne
-- serait relu par personne, et l'interrupteur resterait eteint sans erreur.
INSERT INTO app_configuration (name, description, value, created, updated, value_type)
VALUES ('EXCLUDE_FREE_UNIT',
        'Exclure les unites gratuites du chiffre d''affaires a declarer',
        'true', now(), now(), 'BOOLEAN')
ON CONFLICT (name) DO UPDATE SET value = 'true', updated = now();

-- ---------------------------------------------------------------------------
-- 2. L'EFFET : les lignes deja retraitees
--
-- Trois motifs, trois populations disjointes, comme en exploitation.
-- ---------------------------------------------------------------------------

-- 2a. Produits d'un rayon exclu : la ligne sort ENTIEREMENT de l'assiette.
-- Le rayon retenu est celui du stockage PRINCIPAL, exactement celui que
-- l'ecran d'exclusion a presente (cf. DeclarationCaServiceImpl) : lire les
-- autres rayons appliquerait une decision prise ailleurs.
UPDATE sales_line sl
   SET exclusion_motif = 'RAYON',
       amount_to_be_taken_into_account = 0
  FROM rayon_produit rp
  JOIN rayon r ON r.id = rp.rayon_id AND r.to_exclude
  JOIN storage st ON st.id = r.storage_id AND st.storage_type = 'PRINCIPAL'
 WHERE sl.produit_id = rp.produit_id
   AND sl.exclusion_motif IS NULL;

-- 2b. Ventes d'un tiers-payant exclu : toutes leurs lignes sortent, part
--     patient comprise.
UPDATE sales_line sl
   SET exclusion_motif = 'TIERS_PAYANT',
       amount_to_be_taken_into_account = 0
  FROM sales s
  JOIN third_party_sale_line tpsl
    ON tpsl.sale_id = s.id AND tpsl.sale_date = s.sale_date
  JOIN client_tiers_payant ctp ON ctp.id = tpsl.client_tiers_payant_id
  JOIN tiers_payant tp ON tp.id = ctp.tierspayant_id AND tp.to_be_exclude
 WHERE sl.sales_id = s.id
   AND sl.sales_sale_date = s.sale_date
   AND sl.exclusion_motif IS NULL;

-- 2c. Unites gratuites : retrait PARTIEL. Seule la portion gratuite sort ; le
--     reste de la ligne demeure declarable. C'est ce qui distingue ce motif
--     des deux autres, et le journal doit pouvoir le montrer.
-- Le chargeur de ventes ne pose jamais d'unite gratuite : sans elles, ni le
-- journal ni le retrait partiel n'auraient d'exemple. On en accorde une sur
-- les lignes d'au moins deux unites, une fois sur quarante — la frequence
-- reelle d'une UG passee au client.
--
-- La quantite vendue et le montant de la ligne ne bougent PAS : la convention
-- du logiciel est que `sales_amount` couvre toutes les unites, la portion
-- gratuite n'etant deduite qu'a la declaration
-- (montantUg = quantityUg x regularUnitPrice, cf. DeclarationCaServiceImpl).
UPDATE sales_line
   SET quantity_ug = 1
 WHERE quantity_sold >= 2
   AND id % 40 = 0;

UPDATE sales_line sl
   SET exclusion_motif = 'UG',
       amount_to_be_taken_into_account = greatest(
           sl.sales_amount - (sl.regular_unit_price * sl.quantity_ug), 0)
 WHERE sl.quantity_ug > 0
   AND sl.exclusion_motif IS NULL;

-- ---------------------------------------------------------------------------
-- 3. Les PONCTIONS : une validee, une annulee
--
-- Une seule ligne d'historique ne montrerait pas qu'une ponction se defait.
-- Les montants sont deduits des ventes reelles de la periode, pour que les
-- cumuls de l'ecran ne contredisent pas les ventes.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_user_id    INTEGER;
    v_magasin_id INTEGER;
    v_debut      DATE := date_trunc('month', CURRENT_DATE - INTERVAL '2 months')::date;
    v_fin        DATE := (date_trunc('month', CURRENT_DATE - INTERVAL '2 months')
                          + INTERVAL '1 month - 1 day')::date;
    v_ca_reel    BIGINT;
    v_ca_apres   BIGINT;
    v_ponctionne BIGINT;
    v_ventes     INTEGER;
    v_id         INTEGER;
BEGIN
    SELECT id INTO v_user_id FROM app_user ORDER BY id LIMIT 1;
    SELECT id INTO v_magasin_id FROM magasin ORDER BY id LIMIT 1;

    SELECT coalesce(sum(s.sales_amount), 0),
           coalesce(sum(sl.declarable), 0),
           count(DISTINCT s.id)
      INTO v_ca_reel, v_ca_apres, v_ventes
      FROM sales s
      JOIN LATERAL (
          SELECT sum(x.amount_to_be_taken_into_account) AS declarable
            FROM sales_line x
           WHERE x.sales_id = s.id AND x.sales_sale_date = s.sale_date
      ) sl ON true
     WHERE s.sale_date BETWEEN v_debut AND v_fin
       AND s.statut::text = 'CLOSED'
       AND s.canceled = false;

    IF v_ca_reel = 0 THEN
        RAISE EXCEPTION 'Aucune vente sur la periode % a % : la ponction ne se demontre pas',
                        v_debut, v_fin;
    END IF;

    -- Un objectif modeste, tenu par le plafond de 35 % par vente.
    v_ponctionne := (v_ca_apres * 3) / 100;

    v_id := nextval('id_ca_ponction_seq');
    INSERT INTO ca_ponction (
        id, magasin_id, date_debut, date_fin, mode_calcul, valeur_saisie,
        plafond_par_vente, strategie, modes_reglement, taux_tva_eligibles,
        ca_reel, ca_apres_exclusions, ca_assiette_tva0, montant_ponctionnable,
        montant_objectif, montant_ponctionne, ca_declare, nombre_ventes,
        statut, commentaire, created_by, created_at, validated_by, validated_at
    ) VALUES (
        v_id, v_magasin_id, v_debut, v_fin, 'POURCENTAGE', 3.00,
        35.00, 'DECROISSANT', 'CASH', '0',
        v_ca_reel, v_ca_apres, v_ca_apres, (v_ca_apres * 35) / 100,
        v_ponctionne, v_ponctionne, v_ca_apres - v_ponctionne, v_ventes,
        'VALIDEE', 'Ponction mensuelle validee', v_user_id,
        v_fin + INTERVAL '2 days', v_user_id, v_fin + INTERVAL '2 days'
    );

    -- Une ponction ANNULEE : elle reste a l'historique, sans effet sur le CA.
    v_id := nextval('id_ca_ponction_seq');
    INSERT INTO ca_ponction (
        id, magasin_id, date_debut, date_fin, mode_calcul, valeur_saisie,
        plafond_par_vente, strategie, modes_reglement, taux_tva_eligibles,
        ca_reel, ca_apres_exclusions, ca_assiette_tva0, montant_ponctionnable,
        montant_objectif, montant_ponctionne, ca_declare, nombre_ventes,
        statut, commentaire, created_by, created_at, canceled_by, canceled_at
    ) VALUES (
        v_id, v_magasin_id, v_debut, v_fin, 'MONTANT_FIXE', 50000.00,
        35.00, 'DECROISSANT', 'CASH', '0',
        v_ca_reel, v_ca_apres, v_ca_apres, (v_ca_apres * 35) / 100,
        50000, 0, v_ca_apres, 0,
        'ANNULEE', 'Objectif errone, annulee le lendemain', v_user_id,
        v_fin + INTERVAL '3 days', v_user_id, v_fin + INTERVAL '4 days'
    );

    RAISE NOTICE 'Ponctions : 1 validee (% F sur % ventes), 1 annulee.', v_ponctionne, v_ventes;
END $$;

-- ---------------------------------------------------------------------------
-- Controles
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_n      INTEGER;
    v_manque TEXT;
BEGIN
    SELECT count(*) INTO v_n FROM rayon WHERE to_exclude;
    IF v_n = 0 THEN
        RAISE EXCEPTION 'Aucun rayon exclu : l ecran d exclusion resterait vide';
    END IF;

    SELECT count(*) INTO v_n FROM tiers_payant WHERE to_be_exclude;
    IF v_n = 0 THEN
        RAISE EXCEPTION 'Aucun tiers-payant exclu : le journal correspondant resterait vide';
    END IF;

    -- Les trois motifs d'exclusion doivent avoir un exemple : un journal vide
    -- ne demontre pas la regle qu'il illustre.
    SELECT string_agg(m, ', ') INTO v_manque
      FROM unnest(ARRAY['RAYON', 'TIERS_PAYANT', 'UG']) m
     WHERE NOT EXISTS (SELECT 1 FROM sales_line sl WHERE sl.exclusion_motif = m);
    IF v_manque IS NOT NULL THEN
        RAISE EXCEPTION 'Motif(s) d exclusion sans ligne journalisee : %', v_manque;
    END IF;

    -- Une exclusion TOTALE laisse zero declarable ; une exclusion d unites
    -- gratuites en laisse. Confondre les deux viderait le CA declare.
    SELECT count(*) INTO v_n FROM sales_line
     WHERE exclusion_motif IN ('RAYON', 'TIERS_PAYANT')
       AND amount_to_be_taken_into_account <> 0;
    IF v_n > 0 THEN
        RAISE EXCEPTION '% ligne(s) exclue(s) en totalite mais encore declarable(s)', v_n;
    END IF;

    SELECT count(*) INTO v_n FROM sales_line
     WHERE exclusion_motif = 'UG'
       AND amount_to_be_taken_into_account >= sales_amount;
    IF v_n > 0 THEN
        RAISE EXCEPTION '% ligne(s) UG dont rien n a ete retire', v_n;
    END IF;

    -- Le declarable ne depasse jamais le reel.
    SELECT count(*) INTO v_n FROM sales_line
     WHERE amount_to_be_taken_into_account > sales_amount;
    IF v_n > 0 THEN
        RAISE EXCEPTION '% ligne(s) dont le montant declarable depasse le montant reel', v_n;
    END IF;

    SELECT count(*) INTO v_n FROM ca_ponction WHERE statut = 'VALIDEE';
    IF v_n = 0 THEN
        RAISE EXCEPTION 'Aucune ponction validee : l historique resterait vide';
    END IF;

    SELECT count(*) INTO v_n FROM ca_ponction WHERE statut = 'ANNULEE';
    IF v_n = 0 THEN
        RAISE EXCEPTION 'Aucune ponction annulee : on ne verrait pas qu une ponction se defait';
    END IF;

    RAISE NOTICE '% ligne(s) retraitee(s), % ponction(s) : controles OK.',
                 (SELECT count(*) FROM sales_line WHERE exclusion_motif IS NOT NULL),
                 (SELECT count(*) FROM ca_ponction);
END $$;

\echo '<< 19_declaration_ca : termine'
