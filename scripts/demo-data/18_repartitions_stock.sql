-- ============================================================================
-- 18_repartitions_stock.sql — Historique des répartitions rayon / réserve
--
-- 07_stock.sql déplace bien une part des lots vers la réserve, mais il le fait
-- « à la main », sans écrire la trace que l'application produirait : l'écran
-- « Répartition & Transferts » s'ouvre donc sur un tableau vide, et avec lui
-- ACH-69 à ACH-72 — historique, réassort rayon, rangement automatique après
-- réception, répartition manuelle.
--
-- Ce script écrit cette trace pour les produits qui ONT effectivement les deux
-- stocks, en deux temps :
--   * des répartitions AUTOMATIQUES, celles que produit une réception dont le
--     surplus part en réserve ;
--   * des répartitions MANUELLES, celles d'un réassort de rayon décidé par un
--     opérateur.
--
-- Les stocks avant/après sont cohérents avec la quantité déplacée : c'est ce
-- que l'écran affiche, et un historique qui ne se recoupe pas ne vaut rien.
-- ============================================================================

\i _header.sql

\echo '>> 18_repartitions_stock : historique des mouvements rayon / réserve'

-- ---------------------------------------------------------------------------
-- Les couples (rayon, réserve) d'un même produit, pris parmi ceux qui portent
-- réellement du stock des deux côtés.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_couples AS
SELECT
    row_number() OVER (ORDER BY p.id) AS rn,
    p.id                              AS produit_id,
    rayon.id                          AS sp_rayon_id,
    rayon.qty_stock                   AS qty_rayon,
    reserve.id                        AS sp_reserve_id,
    reserve.qty_stock                 AS qty_reserve
FROM produit p
JOIN stock_produit rayon
  ON rayon.produit_id = p.id
JOIN storage st_r
  ON st_r.id = rayon.storage_id AND st_r.storage_type = 'PRINCIPAL'
JOIN stock_produit reserve
  ON reserve.produit_id = p.id
JOIN storage st_s
  ON st_s.id = reserve.storage_id AND st_s.storage_type = 'SAFETY_STOCK'
WHERE rayon.qty_stock >= 4
  AND reserve.qty_stock >= 4
LIMIT 40;

-- ---------------------------------------------------------------------------
-- 1. Répartitions AUTOMATIQUES : rayon -> réserve, le surplus d'une réception.
-- ---------------------------------------------------------------------------
INSERT INTO repartition_stock_produit (
    created_at, qty_mvt,
    source_init_stock, source_final_stock,
    dest_init_stock, dest_final_stock,
    type_repartition, stock_produit_source_id, stock_produit_destination_id, user_id
)
SELECT
    NOW() - ((c.rn % 60) || ' days')::interval - ((c.rn % 7) || ' hours')::interval,
    q.qty,
    c.qty_rayon + q.qty, c.qty_rayon,
    c.qty_reserve - q.qty, c.qty_reserve,
    0,                                   -- AUTO
    c.sp_rayon_id, c.sp_reserve_id,
    (SELECT id FROM app_user WHERE login = 'admin')
FROM tmp_couples c
CROSS JOIN LATERAL (SELECT greatest(1, least(c.qty_reserve - 1, 1 + (c.rn % 5))) AS qty) q
WHERE c.rn % 2 = 1;

-- ---------------------------------------------------------------------------
-- 2. Répartitions MANUELLES : réserve -> rayon, le réassort du comptoir.
-- ---------------------------------------------------------------------------
INSERT INTO repartition_stock_produit (
    created_at, qty_mvt,
    source_init_stock, source_final_stock,
    dest_init_stock, dest_final_stock,
    type_repartition, stock_produit_source_id, stock_produit_destination_id, user_id
)
SELECT
    NOW() - ((c.rn % 45) || ' days')::interval - ((c.rn % 5) || ' hours')::interval,
    q.qty,
    c.qty_reserve + q.qty, c.qty_reserve,
    c.qty_rayon - q.qty, c.qty_rayon,
    1,                                   -- MANUEL
    c.sp_reserve_id, c.sp_rayon_id,
    (SELECT id FROM app_user WHERE login = 'admin')
FROM tmp_couples c
CROSS JOIN LATERAL (SELECT greatest(1, least(c.qty_rayon - 1, 1 + (c.rn % 4))) AS qty) q
WHERE c.rn % 2 = 0;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_total int;
    v_auto  int;
    v_manu  int;
    v_incoh int;
BEGIN
    SELECT count(*) INTO v_total FROM repartition_stock_produit;
    SELECT count(*) INTO v_auto  FROM repartition_stock_produit WHERE type_repartition = 0;
    SELECT count(*) INTO v_manu  FROM repartition_stock_produit WHERE type_repartition = 1;

    -- Le mouvement doit se retrouver des deux côtés : source qui baisse, destination
    -- qui monte, de la même quantité.
    SELECT count(*) INTO v_incoh
      FROM repartition_stock_produit
     WHERE source_init_stock - source_final_stock <> qty_mvt
        OR dest_final_stock - dest_init_stock <> qty_mvt;

    IF v_total < 20 THEN RAISE EXCEPTION 'Répartitions : % (attendu >= 20)', v_total; END IF;
    IF v_auto  < 5  THEN RAISE EXCEPTION 'Répartitions AUTO : % (attendu >= 5)', v_auto; END IF;
    IF v_manu  < 5  THEN RAISE EXCEPTION 'Répartitions MANUELLES : % (attendu >= 5)', v_manu; END IF;
    IF v_incoh > 0  THEN RAISE EXCEPTION '% répartition(s) dont les stocks ne se recoupent pas', v_incoh; END IF;

    RAISE NOTICE '18_repartitions_stock : % mouvements (% auto, % manuels).', v_total, v_auto, v_manu;
END $$;

DROP TABLE tmp_couples;
