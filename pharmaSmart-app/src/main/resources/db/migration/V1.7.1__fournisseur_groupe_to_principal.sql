UPDATE fournisseur SET groupe_pournisseur_id = NULL WHERE groupe_pournisseur_id = 5;

DO $$
DECLARE
    r           RECORD;
    v_parent_id INTEGER;
    v_code      VARCHAR(70);
    v_libelle   VARCHAR(255);
BEGIN
    FOR r IN
        SELECT
            gf.id                       AS gf_id,
            gf.libelle                  AS libelle,
            gf.odre                     AS odre,
            gf.delai_livraison_jours    AS delai_livraison_jours,
            gf.frequence_commande_jours AS frequence_commande_jours,
            gf.jours_credit             AS jours_credit,
            gf.jours_critique           AS jours_critique,
            gf.url_pharma_ml            AS url_pharma_ml,
            gf.code_office_pharma_ml    AS code_office_pharma_ml,
            gf.code_recepteur_pharma_ml AS code_recepteur_pharma_ml,
            gf.id_recepteur_pharma_ml   AS id_recepteur_pharma_ml
        FROM groupe_fournisseur gf
        WHERE EXISTS (
            SELECT 1 FROM fournisseur f
            WHERE f.groupe_pournisseur_id = gf.id
        )
        ORDER BY gf.odre, gf.id
    LOOP
        -- Générer un code alphanumérique à partir du libellé (max 20 chars)
        v_code    := LEFT(UPPER(REGEXP_REPLACE(r.libelle, '[^A-Za-z0-9]', '', 'g')), 20);

        -- Garantir l'unicité du libellé en cas de collision avec un fournisseur existant
        v_libelle := r.libelle;
        IF EXISTS (SELECT 1 FROM fournisseur WHERE libelle = v_libelle AND parent_id IS NULL) THEN
            RAISE NOTICE 'Groupe % : libellé "%" déjà utilisé par un fournisseur principal existant — ignoré.',
                r.gf_id, v_libelle;
            CONTINUE;
        END IF;

        INSERT INTO fournisseur (
            libelle,
            code,
            odre,
            delai_livraison_jours,
            frequence_commande_jours,
            jours_credit,
            jours_critique,
            url_pharma_ml,
            code_office_pharma_ml,
            code_recepteur_pharma_ml,
            id_recepteur_pharma_ml
            -- parent_id      : NULL  → c'est un principal
            -- groupe_pournisseur_id : NULL  → n'appartient pas à l'ancien modèle
        ) VALUES (
            v_libelle,
            v_code,
            r.odre,
            r.delai_livraison_jours,
            r.frequence_commande_jours,
            r.jours_credit,
            r.jours_critique,
            r.url_pharma_ml,
            r.code_office_pharma_ml,
            r.code_recepteur_pharma_ml,
            r.id_recepteur_pharma_ml
        )
        RETURNING id INTO v_parent_id;

        RAISE NOTICE 'Groupe % "%" → fournisseur principal créé avec id=%.',
            r.gf_id, v_libelle, v_parent_id;

        -- Transformer les fournisseurs liés à ce groupe en agences
        UPDATE fournisseur
        SET parent_id = v_parent_id
        WHERE groupe_pournisseur_id = r.gf_id
          AND parent_id IS NULL;

        RAISE NOTICE '  → % agence(s) rattachée(s).',
            (SELECT COUNT(*) FROM fournisseur WHERE parent_id = v_parent_id);

    END LOOP;
END;
$$;

-- Transférer les fournisseur_produit des agences vers leur fournisseur parent
-- 1. Mettre à jour les lignes sans conflit (produit non encore présent chez le parent)
WITH agences AS (
    SELECT fp.id AS fp_id,
           fp.produit_id,
           f.parent_id
    FROM fournisseur_produit fp
             JOIN fournisseur f ON f.id = fp.fournisseur_id
    WHERE f.parent_id IS NOT NULL
),
sans_conflit AS (
    SELECT a.fp_id, a.parent_id
    FROM agences a
    WHERE NOT EXISTS (
        SELECT 1 FROM fournisseur_produit fp2
        WHERE fp2.produit_id     = a.produit_id
          AND fp2.fournisseur_id = a.parent_id
    )
)
UPDATE fournisseur_produit
SET fournisseur_id = sc.parent_id
FROM sans_conflit sc
WHERE fournisseur_produit.id = sc.fp_id;

-- 2. Avant suppression des doublons, rediriger produit.fournisseur_produit_principal_id
--    vers la ligne survivante chez le parent pour ne pas casser la FK
UPDATE produit
SET fournisseur_produit_principal_id = (
    SELECT fp_parent.id
    FROM fournisseur_produit fp_agence
             JOIN fournisseur f ON f.id = fp_agence.fournisseur_id AND f.parent_id IS NOT NULL
             JOIN fournisseur_produit fp_parent ON fp_parent.fournisseur_id = f.parent_id
                                               AND fp_parent.produit_id     = produit.id
    WHERE fp_agence.id = produit.fournisseur_produit_principal_id
    LIMIT 1
)
WHERE EXISTS (
    SELECT 1
    FROM fournisseur_produit fp_a
             JOIN fournisseur f ON f.id = fp_a.fournisseur_id AND f.parent_id IS NOT NULL
    WHERE fp_a.id = produit.fournisseur_produit_principal_id
);

-- 3. Supprimer les doublons restants (le parent possède déjà une entrée pour ce produit)
DELETE FROM fournisseur_produit
USING fournisseur f
WHERE fournisseur_produit.fournisseur_id = f.id
  AND f.parent_id IS NOT NULL;
DELETE FROM  suggestion_line ;
DELETE FROM suggestion  ;
