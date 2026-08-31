\i _header.sql

-- ============================================================================
-- 16b_ajustements.sql — Bons d'ajustement et demarque
--
-- Le rapport « Demarque & Ajustements » s'ouvrait sur « Aucune demarque sur
-- cette periode », quelle que soit la periode : la demonstration ne creait
-- AUCUN bon d'ajustement. Les 387 ajustements que comptait la pastille du
-- tableau de bord etaient des mouvements d'inventaire, qui ne passent pas par
-- `ajust` et n'entrent donc pas dans la demarque.
--
-- CE QUE LE RAPPORT LIT, exactement (DemarqueReportServiceImpl) :
--
--     ajust.statut = 'CLOSED'
--       ET ajustement.type_ajust = 'AJUSTEMENT_OUT'
--       ET ajust.date_mtv dans la periode
--
-- Un bon PENDING ne compte pas — il n'est pas valide —, et une entree de stock
-- non plus : la demarque, c'est ce qui SORT sans avoir ete vendu. Le jeu pose
-- donc les quatre cas, pour que le filtrage se demontre.
--
-- LE STOCK N'EST PAS DEPLACE. C'est un choix, et il demande un mot : le stock
-- de la demonstration est deja recale par 07_stock.sql puis consomme par les
-- ventes, et le reappliquer ici mettrait en defaut les controles qui verifient
-- que le stock d'un produit egale la somme de ses lots. Les bons sont donc
-- ecrits comme une HISTOIRE — leurs colonnes `stock_before` / `stock_after`
-- decrivent l'etat au moment du mouvement — sans que le stock courant en soit
-- affecte une seconde fois.
--
-- Se place apres 16_mouvements.sql : le stock courant doit etre stabilise.
-- ============================================================================

\echo '>> 16b_ajustements : bons d ajustement et demarque'

DELETE FROM ajustement;
DELETE FROM ajust;

DO $$
DECLARE
    v_user_id   INTEGER;
    v_bon_id    INTEGER;
    v_statut    TEXT;
    v_type      TEXT;
    v_motif_id  INTEGER;
    v_qty       INTEGER;
    v_stock     INTEGER;
    v_lignes    INTEGER;
    v_rang      INTEGER := 0;
    sp          RECORD;
    v_commentaires TEXT[] := ARRAY[
        'Casse constatee au rangement du matin',
        'Ecart releve au comptage hebdomadaire du rayon',
        'Boites retirees : conditionnement abime a la livraison',
        'Regularisation apres inventaire tournant',
        'Produits sortis pour usage au comptoir'
    ];
    -- Les motifs de sortie sont ceux qui font la demarque reelle d'une
    -- officine ; « Regularisation d'inventaire » et « Erreur de saisie »
    -- restent pour les ecarts qui ne sont pas des pertes.
    -- Les libelles sont ceux de V2.0.1, ACCENTS COMPRIS : la recherche se fait sur
    -- l'egalite exacte, et « Produit perime retire » ne trouvait rien -- six sorties
    -- se retrouvaient alors sans motif, ce que le controle final a rattrape.
    v_motifs_out TEXT[] := ARRAY[
        'Casse',
        'Vol ou disparition',
        'Produit périmé retiré',
        'Erreur de comptage',
        'Consommation interne',
        'Retour client non revendable'
    ];
    v_motif_libelle TEXT;
    v_absents       TEXT;
BEGIN
    -- Un motif introuvable ne doit pas se decouvrir a la fin, sous la forme d'un
    -- comptage de lignes sans motif : on nomme tout de suite celui qui manque.
    SELECT string_agg(m, ', ') INTO v_absents
      FROM unnest(v_motifs_out || ARRAY['Erreur de saisie']) m
     WHERE NOT EXISTS (SELECT 1 FROM motif_ajustement ma WHERE ma.libelle = m);
    IF v_absents IS NOT NULL THEN
        RAISE EXCEPTION 'Motif(s) d ajustement absent(s) du referentiel : %', v_absents;
    END IF;

    SELECT id INTO v_user_id FROM app_user WHERE login = 'admin';
    IF v_user_id IS NULL THEN
        SELECT id INTO v_user_id FROM app_user ORDER BY id LIMIT 1;
    END IF;

    -- Trente bons etales sur six mois, dont les deux derniers datent d'hier et
    -- d'aujourd'hui : la pastille « Ajustements » du tableau de bord ne compte
    -- que les mouvements des dernieres vingt-quatre heures, et resterait sinon
    -- a zero le jour de la demonstration.
    FOR v_rang IN 1..30 LOOP
        v_statut := CASE WHEN v_rang % 10 = 0 THEN 'PENDING' ELSE 'CLOSED' END;

        INSERT INTO ajust (commentaire, date_mtv, statut, user_id)
        VALUES (
            v_commentaires[1 + (v_rang % array_length(v_commentaires, 1))],
            CASE
                WHEN v_rang = 29 THEN (CURRENT_DATE - 1)::timestamp + TIME '09:15:00'
                WHEN v_rang = 30 THEN CURRENT_DATE::timestamp + TIME '08:40:00'
                ELSE (CURRENT_DATE - (v_rang * 6))::timestamp + TIME '11:20:00'
            END,
            v_statut,
            v_user_id
        )
        RETURNING id INTO v_bon_id;

        v_lignes := 1 + (v_rang % 4);

        FOR sp IN
            SELECT s.id AS stock_produit_id, s.qty_stock, p.id AS produit_id
              FROM stock_produit s
              JOIN produit p ON p.id = s.produit_id
             WHERE s.qty_stock > 20
               AND p.status = 'ENABLE'
             ORDER BY (s.id * 7 + v_rang * 13) % 1000
             LIMIT v_lignes
        LOOP
            -- Un bon sur six est une ENTREE : un carton retrouve, une erreur de
            -- saisie corrigee a la hausse. Il ne doit pas compter dans la
            -- demarque, et c'est ce que le rapport doit demontrer.
            v_type := CASE WHEN v_rang % 6 = 0 THEN 'AJUSTEMENT_IN' ELSE 'AJUSTEMENT_OUT' END;

            v_motif_libelle := CASE
                WHEN v_type = 'AJUSTEMENT_OUT'
                THEN v_motifs_out[1 + ((v_rang + sp.produit_id) % array_length(v_motifs_out, 1))]
                ELSE 'Erreur de saisie'
            END;
            SELECT id INTO v_motif_id FROM motif_ajustement WHERE libelle = v_motif_libelle;

            -- Des quantites de perte credibles : quelques unites, jamais un
            -- rayon entier. Une demarque massive ferait douter du jeu.
            v_qty := 1 + ((sp.stock_produit_id + v_rang) % 5);
            v_stock := sp.qty_stock;

            INSERT INTO ajustement (
                date_mtv, qty_mvt, stock_before, stock_after,
                type_ajust, ajust_id, motif_ajustement_id, stock_produit_id
            ) VALUES (
                (SELECT date_mtv FROM ajust WHERE id = v_bon_id),
                CASE WHEN v_type = 'AJUSTEMENT_OUT' THEN -v_qty ELSE v_qty END,
                v_stock,
                CASE WHEN v_type = 'AJUSTEMENT_OUT' THEN v_stock - v_qty ELSE v_stock + v_qty END,
                v_type, v_bon_id, v_motif_id, sp.stock_produit_id
            );
        END LOOP;
    END LOOP;

    -- La variable d'une boucle FOR est LOCALE a la boucle : v_rang y est nul apres coup.
    RAISE NOTICE '% bon(s) d ajustement generes.', (SELECT count(*) FROM ajust);
END $$;

-- ---------------------------------------------------------------------------
-- Controles
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_n       INTEGER;
    v_demarque INTEGER;
BEGIN
    SELECT count(*) INTO v_n FROM ajust;
    IF v_n < 20 THEN
        RAISE EXCEPTION 'Bons d ajustement : % (attendu >= 20)', v_n;
    END IF;

    -- Un bon sans ligne n'a aucun sens et fausserait le comptage des bons.
    SELECT count(*) INTO v_n FROM ajust a
     WHERE NOT EXISTS (SELECT 1 FROM ajustement l WHERE l.ajust_id = a.id);
    IF v_n > 0 THEN
        RAISE EXCEPTION '% bon(s) d ajustement sans ligne', v_n;
    END IF;

    -- Le snapshot doit etre coherent avec le mouvement : c'est ce qu'affiche
    -- l'historique produit, et un ecart y serait illisible.
    SELECT count(*) INTO v_n FROM ajustement
     WHERE stock_after <> stock_before + qty_mvt;
    IF v_n > 0 THEN
        RAISE EXCEPTION '% ligne(s) dont le stock apres contredit le mouvement', v_n;
    END IF;

    -- Le sens du mouvement et son signe doivent s'accorder.
    SELECT count(*) INTO v_n FROM ajustement
     WHERE (type_ajust = 'AJUSTEMENT_OUT' AND qty_mvt >= 0)
        OR (type_ajust = 'AJUSTEMENT_IN'  AND qty_mvt <= 0);
    IF v_n > 0 THEN
        RAISE EXCEPTION '% ligne(s) dont le signe contredit le type', v_n;
    END IF;

    -- Une sortie sans motif ne se justifie pas : c'est le motif qui rend la
    -- demarque analysable.
    SELECT count(*) INTO v_n FROM ajustement
     WHERE type_ajust = 'AJUSTEMENT_OUT' AND motif_ajustement_id IS NULL;
    IF v_n > 0 THEN
        RAISE EXCEPTION '% sortie(s) d ajustement sans motif', v_n;
    END IF;

    -- Et surtout : le rapport de demarque doit avoir de quoi s'afficher.
    SELECT count(*) INTO v_demarque
      FROM ajust a
      JOIN ajustement l ON l.ajust_id = a.id
     WHERE a.statut = 'CLOSED' AND l.type_ajust = 'AJUSTEMENT_OUT';
    IF v_demarque < 20 THEN
        RAISE EXCEPTION 'Lignes de demarque : % (attendu >= 20)', v_demarque;
    END IF;

    RAISE NOTICE '% bon(s), % ligne(s) de demarque : controles OK.',
                 (SELECT count(*) FROM ajust), v_demarque;
END $$;

\echo '<< 16b_ajustements : termine'
