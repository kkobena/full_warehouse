-- ============================================================================
-- 10c_unite_gestion.sql — L'espace parapharmacie : un magasin UNITE_GESTION
--
-- Le troisième type de magasin, après l'officine et le dépôt. Une unité de
-- gestion est une unité INTERNE AU MÊME LIEU PHYSIQUE : une parapharmacie qui
-- tient son stock, prend sa propre caisse et passe ses propres commandes, sans
-- être un établissement distinct. Elle n'a aucune existence juridique propre et
-- facture sous l'identité de son officine — d'où l'absence d'identité FNE et de
-- registre, là où le dépôt, établissement rattaché, en porte.
--
-- Ce qui la distingue du DÉPÔT, et pourquoi les deux ne se démontrent pas
-- pareil :
--
--     DÉPÔT           autre localité, approvisionné en consignation par
--                     l'officine (vente dépôt), NE COMMANDE PAS lui-même,
--                     aucun utilisateur ne lui est affecté
--     UNITE_GESTION   même lieu, stock propre, caisse propre, COMMANDE EN
--                     PROPRE, du personnel y travaille
--
-- `commande_en_propre = TRUE` est donc la colonne qui porte la différence, et
-- `parent_id` celle qui dit de quelle officine l'unité tient son identité.
--
-- ── Ce que ce script pose, et ce qu'il laisse aux parcours ───────────────────
-- Il pose le DÉCOR : le magasin, son stockage, son rayon, son catalogue en
-- stock, son personnel, son réassort et un historique de caisses closes.
--
-- Il ne pose NI vente NI commande. Ce ne sont pas des oublis :
--
--   * les commandes portent une vingtaine d'invariants dans 99_verification
--     (total = somme des lignes, stock avant / après, chronologie…) ; en
--     fabriquer une à la main ferait porter au jeu de démonstration un risque
--     que rien ne justifie, alors que « l'unité passe sa propre commande » est
--     précisément ce qu'un parcours doit montrer en le FAISANT ;
--   * aucune caisse de l'unité n'est laissée OUVERTE, parce que le jeu tient
--     l'invariant « une seule caisse ouverte » (99_verification). Ouvrir une
--     caisse à la parapharmacie est, là encore, un geste de parcours.
--
-- ── Idempotence ─────────────────────────────────────────────────────────────
-- `00_reset.sql` CONSERVE magasin, storage, rayon, app_user et user_authority,
-- et TRONQUE le reste. Les premiers sont donc posés sous condition, les seconds
-- insérés franchement. `semois_configuration` figure dans la liste conservée
-- mais référence `produit` : le TRUNCATE CASCADE l'emporte avec le catalogue,
-- elle est donc à reconstruire comme le fait 17_suggestions pour l'officine.
--
-- Le stock de l'unité est SANS LOT, comme celui du dépôt : l'invariant « stock =
-- somme des emplacements » de 99_verification ne porte que sur le magasin 1.
-- ============================================================================

\i _header.sql

\echo '>> 10c_unite_gestion : magasin parapharmacie, stock, personnel, caisses'

-- ---------------------------------------------------------------------------
-- 1. Le magasin et son stockage
--
-- `name` et `full_name` sont UNIQUES : l'unité ne peut reprendre ni l'un ni
-- l'autre de l'officine.
--
-- `parent_id` la rattache à l'officine — celle dont elle emprunte la licence et
-- l'identité de facturation. Sans ce rattachement, la supervision ne la voit
-- pas : elle remonte l'arbre par `parent_id`.
-- ---------------------------------------------------------------------------
INSERT INTO magasin (
    name, full_name, type_magasin, address, phone, email,
    note, welcome_message, registre, compte_contribuable,
    parent_id, commande_en_propre
)
SELECT
    'PHARMA SMART PARA',
    'PHARMA SMART — ESPACE PARAPHARMACIE',
    'UNITE_GESTION',
    'Angle rue des Jardins, Cocody, Abidjan',
    '27 22 44 18 00',
    'parapharmacie@pharma-smart.example',
    'Espace parapharmacie attenant à l''officine : stock, caisse et commandes propres, facturation sous l''identité de l''officine',
    'Bienvenue à l''espace parapharmacie',
    -- Ni registre ni compte contribuable : l'unité n'est pas un établissement.
    '', '',
    o.id,
    -- La colonne qui la sépare du dépôt : elle commande pour elle-même.
    TRUE
FROM magasin o
WHERE o.type_magasin = 'OFFICINE'
ORDER BY o.id
LIMIT 1
ON CONFLICT (name) DO NOTHING;

-- Magasin.primaryStorage est une @JoinFormula filtrant sur magasin_id : sans
-- stockage PRINCIPAL elle renvoie null, et tout ce qui touche au stock de
-- l'unité échoue sans que l'écran dise pourquoi.
INSERT INTO storage (name, storage_type, magasin_id)
SELECT 'Stock parapharmacie', 'PRINCIPAL', m.id
FROM magasin m WHERE m.name = 'PHARMA SMART PARA'
ON CONFLICT (storage_type, magasin_id) DO NOTHING;

-- Une RÉSERVE, vide, et ce n'est pas une coquetterie : la réserve du magasin courant est
-- résolue par type, et une absence ne lève pas d'erreur — elle rend un stockage nul, que
-- l'écran suivant déréférence. Un compte affecté à une unité sans réserve tomberait donc sur
-- une erreur serveur, loin de la cause. Elle reste vide : une unité ouverte depuis un mois
-- range tout en rayon, et c'est le réassort qui la remplira.
INSERT INTO storage (name, storage_type, magasin_id)
SELECT 'Réserve parapharmacie', 'SAFETY_STOCK', m.id
FROM magasin m WHERE m.name = 'PHARMA SMART PARA'
ON CONFLICT (storage_type, magasin_id) DO NOTHING;

-- L'unité est désignée par son NOM, jamais par son seul type : la remise à zéro
-- préserve la table magasin, si bien qu'une unité créée depuis l'écran survit au
-- rechargement. La chercher par `type_magasin` renverrait alors plusieurs
-- lignes, et verserait le stock du jeu dans toutes.
CREATE TEMP TABLE tmp_para_mag AS
SELECT m.id AS magasin_id, s.id AS storage_id
  FROM magasin m
  JOIN storage s ON s.magasin_id = m.id AND s.storage_type = 'PRINCIPAL'
 WHERE m.name = 'PHARMA SMART PARA';

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM tmp_para_mag) THEN
        RAISE EXCEPTION 'Magasin « PHARMA SMART PARA » absent ou sans stockage principal.';
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 2. Son catalogue : les familles de parapharmacie
--
-- Une parapharmacie ne vend pas de médicament. On prend donc les quatre
-- familles qui n'en sont pas — parfumerie, accessoires, diététique infantile et
-- adulte — soit une centaine de références déjà au catalogue (03_parapharmacie).
--
-- Le même produit peut être tenu par l'officine ET par l'unité : ce sont deux
-- lignes de `stock_produit`, une par stockage. C'est tout l'intérêt du
-- multi-magasin — et le piège de toute requête qui sommerait les deux.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_para_prod AS
SELECT p.id AS produit_id, p.classe_criticite
  FROM produit p
  JOIN famille_produit f ON f.id = p.famille_id
 WHERE p.status = 'ENABLE'
   AND f.code IN ('3000', '8000', '5000', '6000');

-- ---------------------------------------------------------------------------
-- 3. Le rayon de l'unité, et le rangement de ses références
--
-- `rayon` est lié au STOCKAGE, pas au magasin : un rayon de l'officine ne peut
-- donc pas ranger le stock de l'unité. Sans rayon propre, les écrans de
-- rangement de l'unité s'ouvrent vides.
--
-- LE RANGEMENT EST PROPRE À CHAQUE MAGASIN. `rayon_produit` est unique sur
-- (produit_id, rayon_id) et l'affectation se contrôle par STOCKAGE : le même
-- produit porte donc une adresse dans chaque établissement, et c'est la règle
-- retenue — l'emplacement appartient au site, jamais au produit. Les références
-- de la parapharmacie sont rangées ICI, alors qu'elles le sont déjà dans un
-- rayon de l'officine : c'est précisément le cas que le logiciel doit savoir
-- distinguer.
--
-- `position` est un repère ALPHANUMÉRIQUE ('A1'…'F2' pour l'officine), pas un
-- rang : 'G1' prolonge la série sans empiéter.
-- ---------------------------------------------------------------------------
INSERT INTO rayon (code, libelle, to_exclude, storage_id, type_zone, position)
SELECT 'PARA', 'ESPACE PARAPHARMACIE', false, t.storage_id, 'PARA', 'G1'
FROM tmp_para_mag t
-- DO UPDATE et non DO NOTHING : `rayon` figure dans la liste de préservation de
-- 00_reset.sql, ses lignes survivent au rechargement. Le script doit converger.
ON CONFLICT (code, storage_id) DO UPDATE
   SET libelle   = EXCLUDED.libelle,
       type_zone = EXCLUDED.type_zone,
       position  = EXCLUDED.position;

-- Les mêmes produits que ceux de l'officine, rangés à l'adresse de l'unité.
INSERT INTO rayon_produit (produit_id, rayon_id)
SELECT pr.produit_id, r.id
FROM tmp_para_prod pr
CROSS JOIN tmp_para_mag t
JOIN rayon r ON r.storage_id = t.storage_id AND r.code = 'PARA'
ON CONFLICT (produit_id, rayon_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 4. Le personnel de l'unité
--
-- Un utilisateur affecté AILLEURS QU'À L'OFFICINE : c'est ce que le jeu de
-- démonstration ne portait pas, et sans quoi rien ne vérifie qu'une session
-- ouverte dans une unité voit bien le stock, la caisse et les commandes de son
-- magasin — et non ceux de l'officine.
--
-- Le hachage de mot de passe est celui du compte « admin » : ce compte de
-- démonstration partage donc son mot de passe.
-- ---------------------------------------------------------------------------
INSERT INTO app_user (
    login, password_hash, first_name, last_name, email,
    activated, lang_key, magasin_id, created_by, created_date, last_modified_by
)
SELECT
    'nsanogo',
    (SELECT password_hash FROM app_user WHERE login = 'admin'),
    'NADIA', 'SANOGO', 'nsanogo@pharma-smart.example',
    true, 'fr', t.magasin_id, 'system',
    NOW() - (INTERVAL '1 day' * (pg_temp.horizon() + 60)), 'system'
FROM tmp_para_mag t
WHERE NOT EXISTS (SELECT 1 FROM app_user u WHERE u.login = 'nsanogo');

-- Si l'unité a été recréée après coup, le compte conservé par la remise à zéro
-- pourrait pointer sur un magasin disparu : on le recale.
UPDATE app_user u
   SET magasin_id = t.magasin_id
  FROM tmp_para_mag t
 WHERE u.login = 'nsanogo' AND u.magasin_id IS DISTINCT FROM t.magasin_id;

INSERT INTO user_authority (user_id, authority_name)
SELECT u.id, a.role
FROM app_user u
CROSS JOIN (VALUES ('ROLE_VENDEUR'), ('ROLE_CAISSIER')) AS a(role)
WHERE u.login = 'nsanogo'
ON CONFLICT DO NOTHING;

-- Un second compte, RESPONSABLE DE COMMANDE : il ouvre le catalogue et les
-- commandes, que la vendeuse n'a pas. C'est le profil qui exploite la
-- commande en propre de l'unité, et le seul qui permette de vérifier qu'un
-- écran de référentiel ouvert ICI parle bien du stock d'ICI. Il ne SUPERVISE
-- rien : `supervision.magasins` n'est accordé qu'à ADMIN et PHARMACIEN, donc
-- ce compte ne voit que son propre magasin — ce qui est l'objet du contrôle.
INSERT INTO app_user (
    login, password_hash, first_name, last_name, email,
    activated, lang_key, magasin_id, created_by, created_date, last_modified_by
)
SELECT
    'ykouame',
    (SELECT password_hash FROM app_user WHERE login = 'admin'),
    'YAO', 'KOUAME', 'ykouame@pharma-smart.example',
    true, 'fr', t.magasin_id, 'system',
    NOW() - (INTERVAL '1 day' * (pg_temp.horizon() + 60)), 'system'
FROM tmp_para_mag t
WHERE NOT EXISTS (SELECT 1 FROM app_user u WHERE u.login = 'ykouame');

UPDATE app_user u
   SET magasin_id = t.magasin_id
  FROM tmp_para_mag t
 WHERE u.login = 'ykouame' AND u.magasin_id IS DISTINCT FROM t.magasin_id;

INSERT INTO user_authority (user_id, authority_name)
SELECT u.id, 'ROLE_RESPONSABLE_COMMANDE'
FROM app_user u
WHERE u.login = 'ykouame'
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 5. Son stock
--
-- Un produit sur cinq est volontairement SOUS son objectif : sans cela le
-- réassort de l'unité s'ouvre sans une ligne, et l'on ne voit pas que le calcul
-- se fait par magasin.
-- ---------------------------------------------------------------------------
INSERT INTO stock_produit (
    produit_id, storage_id, qty_stock, qty_virtual, qty_ug,
    seuil_mini, stock_maxi, stock_reassort,
    version, last_modified_by, created_at, updated_at
)
SELECT
    pr.produit_id,
    t.storage_id,
    q.qty, q.qty, 0,
    GREATEST(3, q.qty / 6),
    GREATEST(20, q.qty * 2),
    0,
    0, 'system',
    NOW() - (INTERVAL '1 day' * (pg_temp.horizon() + 40)),
    NOW()
FROM tmp_para_prod pr
CROSS JOIN tmp_para_mag t
CROSS JOIN LATERAL (
    SELECT CASE WHEN pr.produit_id % 5 = 0 THEN 1 ELSE 12 + (pr.produit_id % 25) END AS qty
) q
ON CONFLICT (storage_id, produit_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 6. Son réassort
--
-- `semois_configuration` est propre au couple (produit, magasin) depuis
-- V2.1.53 : l'unité commandant en propre, elle a besoin de la sienne. Sans
-- elle, son tableau de bord stock reste vide quoi qu'elle détienne.
--
-- La VMM n'est pas calculée sur des ventes — l'unité n'en a pas encore — mais
-- posée : c'est une parapharmacie qui vient d'ouvrir, et l'objectif se déduit
-- du délai de livraison et de la marge de sécurité de la classe.
-- ---------------------------------------------------------------------------
INSERT INTO semois_configuration (
    produit_id, magasin_id, classe_criticite, delai_livraison_jours, frequence_commande_jours,
    facteur_saisonnier_actuel, facteur_saisonnier_manuel, limite_peremption,
    marge_securite, vmm_calcule, stock_objectif_calcule,
    date_dernier_calcul, created_at, updated_at, exclusion_duree_jours
)
SELECT
    pr.produit_id,
    t.magasin_id,
    pr.classe_criticite,
    2,                                  -- les grossistes livrent en 48 h
    7,                                  -- une commande par semaine
    1.00, false, false,
    m.marge,
    v.vmm,
    GREATEST(1, (v.vmm * 2 / 30)::int + m.marge),
    NOW() - INTERVAL '6 hours',
    NOW() - INTERVAL '30 days',         -- l'unité est ouverte depuis un mois
    NOW() - INTERVAL '6 hours',
    NULL
FROM tmp_para_prod pr
CROSS JOIN tmp_para_mag t
CROSS JOIN LATERAL (
    SELECT CASE pr.classe_criticite
               WHEN 'A_PLUS' THEN 15 WHEN 'A' THEN 10 WHEN 'B' THEN 7 ELSE 5 END AS marge
) m
CROSS JOIN LATERAL (
    SELECT GREATEST(1, 1 + (pr.produit_id % 4)) AS vmm
) v
ON CONFLICT (produit_id, magasin_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 7. Ses caisses
--
-- Un mois d'exploitation, toutes CLOSES : l'invariant du jeu est qu'une seule
-- caisse reste ouverte, celle de l'officine.
--
-- `final_amount = init_amount` et aucune ligne de détail : 99_verification
-- vérifie que le fond de tiroir d'une caisse close s'explique exactement par
-- son détail espèces. Une caisse sans encaissement doit donc rendre son fonds
-- d'ouverture, au franc près.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_caisse_para AS
SELECT
    (CURRENT_DATE - (INTERVAL '1 day' * j))::date AS jour,
    (SELECT id FROM app_user WHERE login = 'nsanogo') AS user_id,
    (SELECT magasin_id FROM tmp_para_mag) AS magasin_id
FROM generate_series(1, 30) AS j
-- L'unité ferme quand l'officine ferme : le lundi.
WHERE extract(dow FROM (CURRENT_DATE - (INTERVAL '1 day' * j))) <> 1;

INSERT INTO cash_register (
    user_id, magasin_id, init_amount, final_amount, cancele_amount,
    begin_time, end_time, created, updated, statut
)
SELECT
    c.user_id, c.magasin_id,
    30000, 30000, 0,
    c.jour + TIME '09:00:00',
    c.jour + TIME '19:00:00',
    c.jour + TIME '09:00:00',
    c.jour + TIME '19:00:00',
    'VALIDATED'
FROM tmp_caisse_para c;

-- CashRegister.cashFund est @NotNull et mappé par « mappedBy » : une caisse sans
-- fonds ne se charge pas.
INSERT INTO cash_fund (
    amount, cash_fund_type, statut, user_id, cash_register_id, validated_by_id,
    created, updated
)
SELECT
    cr.init_amount::int, 'AUTO', 'VALIDETED',
    cr.user_id, cr.id,
    (SELECT id FROM app_user WHERE login = 'admin'),
    cr.begin_time, cr.begin_time
FROM cash_register cr
WHERE cr.magasin_id = (SELECT magasin_id FROM tmp_para_mag)
  AND NOT EXISTS (SELECT 1 FROM cash_fund f WHERE f.cash_register_id = cr.id);

DROP TABLE tmp_caisse_para;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_magasin   int;
    v_parent    int;
    v_propre    boolean;
    v_stock     int;
    v_courts    int;
    v_config    int;
    v_caisses   int;
    v_ouvertes  int;
    v_rayon     int;
    v_ranges    int;
    v_user      int;
BEGIN
    SELECT count(*) INTO v_magasin FROM magasin WHERE type_magasin = 'UNITE_GESTION';

    SELECT m.parent_id, m.commande_en_propre INTO v_parent, v_propre
      FROM magasin m WHERE m.name = 'PHARMA SMART PARA';

    SELECT count(*) INTO v_stock
      FROM stock_produit sp JOIN tmp_para_mag t ON t.storage_id = sp.storage_id;

    SELECT count(*) INTO v_courts
      FROM stock_produit sp
      JOIN tmp_para_mag t ON t.storage_id = sp.storage_id
      JOIN semois_configuration sc
        ON sc.produit_id = sp.produit_id AND sc.magasin_id = t.magasin_id
     WHERE sp.qty_stock + sp.qty_ug < sc.stock_objectif_calcule;

    SELECT count(*) INTO v_config
      FROM semois_configuration sc JOIN tmp_para_mag t ON t.magasin_id = sc.magasin_id;

    SELECT count(*) INTO v_caisses
      FROM cash_register cr JOIN tmp_para_mag t ON t.magasin_id = cr.magasin_id;

    SELECT count(*) INTO v_ouvertes FROM cash_register WHERE statut = 'OPEN';

    SELECT count(*) INTO v_rayon
      FROM rayon r JOIN tmp_para_mag t ON t.storage_id = r.storage_id;

    SELECT count(*) INTO v_ranges
      FROM rayon_produit rp
      JOIN rayon r ON r.id = rp.rayon_id
      JOIN tmp_para_mag t ON t.storage_id = r.storage_id;

    SELECT count(*) INTO v_user
      FROM app_user u JOIN tmp_para_mag t ON t.magasin_id = u.magasin_id;

    IF v_magasin < 1 THEN
        RAISE EXCEPTION 'Aucun magasin UNITE_GESTION.';
    END IF;
    IF v_parent IS NULL THEN
        RAISE EXCEPTION 'L''unité de gestion n''est rattachée à aucune officine.';
    END IF;
    IF NOT v_propre THEN
        RAISE EXCEPTION 'L''unité de gestion devrait commander en propre.';
    END IF;
    IF v_stock < 90 THEN
        RAISE EXCEPTION 'Stock de l''unité : % référence(s) (attendu >= 90)', v_stock;
    END IF;
    -- Sans produit sous objectif, le réassort de l'unité s'ouvre vide et ne
    -- démontre rien.
    IF v_courts < 10 THEN
        RAISE EXCEPTION 'Produits sous objectif dans l''unité : % (attendu >= 10)', v_courts;
    END IF;
    IF v_config <> v_stock THEN
        RAISE EXCEPTION 'Réassort : % configuration(s) pour % référence(s) en stock', v_config, v_stock;
    END IF;
    IF v_caisses < 20 THEN
        RAISE EXCEPTION 'Caisses de l''unité : % (attendu >= 20)', v_caisses;
    END IF;
    -- L'invariant du jeu : la seule caisse ouverte reste celle de l'officine.
    IF v_ouvertes <> 1 THEN
        RAISE EXCEPTION 'Caisses ouvertes toutes magasins confondus : % (attendu 1)', v_ouvertes;
    END IF;
    IF v_rayon < 1 THEN
        RAISE EXCEPTION 'L''unité de gestion n''a aucun rayon.';
    END IF;
    -- Un stock sans adresse : le comptoir de l'unité n'annoncerait aucun rayon
    -- pour ses propres références.
    IF v_ranges <> v_stock THEN
        RAISE EXCEPTION 'Rangement de l''unité : % référence(s) rangée(s) pour % en stock', v_ranges, v_stock;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM storage s JOIN magasin m ON m.id = s.magasin_id
         WHERE m.name = 'PHARMA SMART PARA' AND s.storage_type = 'SAFETY_STOCK'
    ) THEN
        RAISE EXCEPTION 'L''unité de gestion n''a pas de réserve : les écrans qui la cherchent tomberaient sur un stockage nul.';
    END IF;
    IF v_user < 2 THEN
        RAISE EXCEPTION 'Utilisateurs de l''unité : % (attendu 2 : la vendeuse et le responsable de commande)', v_user;
    END IF;

    RAISE NOTICE 'Unité de gestion : % références en stock et rangées (dont % sous objectif), % caisses closes, % utilisateur(s).',
        v_stock, v_courts, v_caisses, v_user;
END $$;

DROP TABLE tmp_para_prod;
DROP TABLE tmp_para_mag;

\echo '<< 10c_unite_gestion : terminé'
