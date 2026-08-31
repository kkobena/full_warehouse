\i _header.sql

-- ============================================================================
-- 12b_retours_fournisseurs.sql — Retours fournisseurs et avoirs recus
--
-- L'onglet « Retours fournisseurs » s'ouvrait sur « Aucun retour en attente » :
-- la demo n'en creait aucun. Six ecrans en dependent — la liste, la saisie de
-- la reponse du fournisseur, les avoirs, le bordereau groupe, l'historique et
-- l'export — et tous montraient un tableau vide.
--
-- LE CYCLE DE VIE, qui est ce que le jeu doit rendre visible :
--
--   VALIDATED           le retour est saisi, rien n'est parti  -> modifiable
--   PROCESSING          le fournisseur en a ete avise          -> fige
--   PARTIALLY_ACCEPTED  il n'a repris qu'une partie            -> avoir partiel
--   CLOSED              l'affaire est soldee                   -> historique
--
-- PIEGES DU MODELE, verifies sur la base :
--
--   * `retour_bon` reference la commande par sa CLE COMPOSITE
--     (commande_id, commande_order_date), et `retour_bon_item` la ligne par
--     (orderline_id, orderline_order_date). Les deux sont NULLABLES depuis
--     V1.4.1 : un retour « hors commande » porte alors `fournisseur_id`.
--   * `prix_achat` est sur l'ITEM, pas sur le bon : c'est le prix d'achat du
--     jour du retour, qui peut differer de celui de la commande.
--   * `accepted_qty` reste NULL tant que le fournisseur n'a pas repondu — c'est
--     ce qui distingue un retour en attente d'un retour arbitre.
--   * `avoir_fournisseur` pointait jadis une `reponse_retour_bon` ; V1.5.5 a
--     supprime cette table et la colonne au profit d'un `retour_bon_id` direct.
--     L'avoir se rattache donc au BON, sans intermediaire.
--
-- Se place apres 12_destruction.sql : les lots perimes doivent exister.
-- ============================================================================

\echo '>> 12b_retours_fournisseurs : retours et avoirs fournisseurs'

DELETE FROM avoir_fournisseur_line;
DELETE FROM avoir_fournisseur;
DELETE FROM retour_bon_item;
DELETE FROM retour_bon;

DO $$
DECLARE
    v_user_id      INTEGER;
    v_rang         INTEGER := 0;
    v_statut       TEXT;
    v_bon_id       INTEGER;
    v_avoir_id     INTEGER;
    v_montant      INTEGER;
    v_motif_id     INTEGER;
    v_qty_retenue  INTEGER;
    c              RECORD;
    l              RECORD;
    v_commentaires TEXT[] := ARRAY[
        'Colis recu ouvert, trois boites ecrasees',
        'Peremption a moins de deux mois a la livraison',
        'Reference non commandee, livree par erreur',
        'Double livraison du meme bon',
        'Rupture de la chaine du froid constatee a l ouverture'
    ];
BEGIN
    SELECT id INTO v_user_id FROM app_user ORDER BY id LIMIT 1;
    SELECT id INTO v_motif_id FROM motif_retour_produit ORDER BY id LIMIT 1;

    FOR c IN
        SELECT cmd.id, cmd.order_date, cmd.fournisseur_id
          FROM commande cmd
         WHERE cmd.order_status IN ('RECEIVED', 'CLOSED')
         ORDER BY cmd.order_date DESC
         LIMIT 8
    LOOP
        v_rang := v_rang + 1;

        -- Deux retours en attente, deux avises, deux partiellement acceptes,
        -- deux soldes : chaque transition a de quoi se montrer.
        v_statut := CASE
            WHEN v_rang <= 2 THEN 'VALIDATED'
            WHEN v_rang <= 4 THEN 'PROCESSING'
            WHEN v_rang <= 6 THEN 'PARTIALLY_ACCEPTED'
            ELSE 'CLOSED'
        END;

        INSERT INTO retour_bon (
            reference, date_mtv, statut, commentaire,
            commande_id, commande_order_date, fournisseur_id,
            hors_commande, hors_stock, user_id
        ) VALUES (
            'RET-' || to_char(CURRENT_DATE, 'YYYY') || '-' || to_char(v_rang, 'FM0000'),
            (CURRENT_DATE - (v_rang * 4))::timestamp + TIME '10:30:00',
            v_statut,
            v_commentaires[1 + (v_rang % array_length(v_commentaires, 1))],
            c.id, c.order_date, c.fournisseur_id,
            false, false, v_user_id
        )
        RETURNING id INTO v_bon_id;

        v_montant := 0;

        -- Trois lignes au plus par retour : on ne renvoie jamais toute une
        -- commande, sauf retour complet (ACH-53).
        FOR l IN
            SELECT ol.id, ol.commande_order_date, ol.order_cost_amount,
                   greatest(ol.quantity_received / 10, 1) AS qty
              FROM order_line ol
             WHERE ol.commande_id = c.id
               AND ol.commande_order_date = c.order_date
               AND ol.quantity_received > 0
             ORDER BY ol.id
             LIMIT 3
        LOOP
            -- Ce que le fournisseur a finalement repris : tout s'il a solde,
            -- la moitie s'il a discute, rien de decide tant qu'il n'a pas repondu.
            v_qty_retenue := CASE
                WHEN v_statut = 'PARTIALLY_ACCEPTED' THEN greatest(l.qty / 2, 1)
                WHEN v_statut = 'CLOSED' THEN l.qty
                ELSE NULL
            END;

            INSERT INTO retour_bon_item (
                date_mtv, init_stock, after_stock, qty_mvt, accepted_qty,
                prix_achat, motif_retour_id, orderline_id, orderline_order_date,
                retour_bon_id
            ) VALUES (
                (CURRENT_DATE - (v_rang * 4))::timestamp + TIME '10:30:00',
                l.qty * 10,
                l.qty * 10 - l.qty,
                l.qty,
                v_qty_retenue,
                l.order_cost_amount,
                v_motif_id, l.id, l.commande_order_date, v_bon_id
            );

            v_montant := v_montant + (l.order_cost_amount * coalesce(v_qty_retenue, l.qty));
        END LOOP;

        -- Un avoir n'existe que si le fournisseur a repondu.
        IF v_statut IN ('PARTIALLY_ACCEPTED', 'CLOSED') THEN
            INSERT INTO avoir_fournisseur (
                reference, date_mtv, montant, statut, commentaire,
                user_id, retour_bon_id, fournisseur_id
            ) VALUES (
                'AVF-' || to_char(CURRENT_DATE, 'YYYY') || '-' || to_char(v_rang, 'FM0000'),
                (CURRENT_DATE - (v_rang * 3))::timestamp + TIME '15:00:00',
                v_montant,
                -- Un avoir solde a ete rembourse ; un avoir partiel est encore attendu.
                CASE WHEN v_statut = 'CLOSED' THEN 'REMBOURSE' ELSE 'EN_ATTENTE' END,
                CASE WHEN v_statut = 'CLOSED'
                     THEN 'Avoir recu et impute sur la facture suivante'
                     ELSE 'Acceptation partielle : avoir en attente de reglement'
                END,
                v_user_id, v_bon_id, c.fournisseur_id
            )
            RETURNING id INTO v_avoir_id;

            INSERT INTO avoir_fournisseur_line (
                avoir_fournisseur_id, retour_bon_item_id, qty_mvt, prix_achat, commentaire
            )
            SELECT v_avoir_id,
                   i.id,
                   coalesce(i.accepted_qty, i.qty_mvt),
                   i.prix_achat,
                   NULL
              FROM retour_bon_item i
             WHERE i.retour_bon_id = v_bon_id;
        END IF;
    END LOOP;

    RAISE NOTICE '% retour(s) fournisseur generes.', v_rang;
END $$;

-- ---------------------------------------------------------------------------
-- Controles
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_n      INTEGER;
    v_manque TEXT;
BEGIN
    SELECT count(*) INTO v_n FROM retour_bon;
    IF v_n = 0 THEN
        RAISE EXCEPTION 'Aucun retour fournisseur : l onglet resterait vide';
    END IF;

    SELECT string_agg(s, ', ') INTO v_manque
      FROM unnest(ARRAY['VALIDATED', 'PROCESSING', 'PARTIALLY_ACCEPTED', 'CLOSED']) s
     WHERE NOT EXISTS (SELECT 1 FROM retour_bon r WHERE r.statut = s);
    IF v_manque IS NOT NULL THEN
        RAISE EXCEPTION 'Statut(s) de retour sans exemple : %', v_manque;
    END IF;

    -- Un retour sans ligne n'a pas de sens et fausserait tous les totaux.
    SELECT count(*) INTO v_n FROM retour_bon r
     WHERE NOT EXISTS (SELECT 1 FROM retour_bon_item i WHERE i.retour_bon_id = r.id);
    IF v_n > 0 THEN
        RAISE EXCEPTION '% retour(s) sans aucune ligne', v_n;
    END IF;

    -- L'arbitrage du fournisseur ne se devine pas : il n'existe que sur les
    -- retours qu'il a traites.
    SELECT count(*) INTO v_n FROM retour_bon_item i
      JOIN retour_bon r ON r.id = i.retour_bon_id
     WHERE (r.statut IN ('VALIDATED', 'PROCESSING') AND i.accepted_qty IS NOT NULL)
        OR (r.statut IN ('PARTIALLY_ACCEPTED', 'CLOSED') AND i.accepted_qty IS NULL);
    IF v_n > 0 THEN
        RAISE EXCEPTION '% ligne(s) dont la quantite acceptee contredit le statut', v_n;
    END IF;

    -- Un avoir accompagne toute reponse, et seulement elle.
    SELECT count(*) INTO v_n FROM retour_bon r
     WHERE r.statut IN ('PARTIALLY_ACCEPTED', 'CLOSED')
       AND NOT EXISTS (SELECT 1 FROM avoir_fournisseur a WHERE a.retour_bon_id = r.id);
    IF v_n > 0 THEN
        RAISE EXCEPTION '% retour(s) traite(s) sans avoir fournisseur', v_n;
    END IF;

    SELECT count(*) INTO v_n FROM avoir_fournisseur WHERE montant <= 0;
    IF v_n > 0 THEN
        RAISE EXCEPTION '% avoir(s) fournisseur de montant nul', v_n;
    END IF;

    RAISE NOTICE '% retour(s), % avoir(s) : controles OK.',
                 (SELECT count(*) FROM retour_bon),
                 (SELECT count(*) FROM avoir_fournisseur);
END $$;

\echo '<< 12b_retours_fournisseurs : termine'
