-- ============================================================================
-- 08_caisses.sql — Postes, utilisateurs de vente et caisses
--
-- PaymentTransaction.cashRegister est « optional = false » : aucun règlement
-- ne peut exister sans caisse. La chaîne à construire avant toute vente payée
-- est donc :
--     AppUser → CashFund → CashRegister → SalePayment
--
-- CashRegister.cashFund est @NotNull et mappé par « mappedBy » : c'est le
-- fonds de caisse qui porte la clé étrangère, chaque caisse doit donc en avoir
-- un qui la référence.
--
-- Une caisse par jour d'ouverture, toutes closes sauf celle du jour.
-- ============================================================================

\i _header.sql

\echo '>> 08_caisses : postes, vendeurs, caisses'

-- ---------------------------------------------------------------------------
-- 1. Postes de vente
--
-- address est NOT NULL : c'est l'identifiant réseau du poste, utilisé pour
-- retrouver la caisse depuis le client lourd.
-- ---------------------------------------------------------------------------
INSERT INTO poste (name, address, poste_number)
VALUES
    ('CAISSE 1', '192.168.1.11', 'P01'),
    ('CAISSE 2', '192.168.1.12', 'P02'),
    ('COMPTOIR', '192.168.1.13', 'P03')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. Utilisateurs de vente
--
-- Sans plusieurs vendeurs, les rapports « ventes par vendeur » et les tickets
-- Z n'ont qu'une seule ligne et ne démontrent rien.
--
-- Le hachage de mot de passe est celui du compte « admin » du référentiel :
-- ces comptes de démonstration partagent donc son mot de passe.
-- ---------------------------------------------------------------------------
INSERT INTO app_user (
    login, password_hash, first_name, last_name, email,
    activated, lang_key, magasin_id, created_by, created_date, last_modified_by
)
SELECT
    v.login,
    (SELECT password_hash FROM app_user WHERE login = 'admin'),
    v.prenom, v.nom, v.login || '@pharma-smart.example',
    true, 'fr', 1, 'system', NOW() - INTERVAL '400 days', 'system'
FROM (VALUES
    ('kkone',    'KOFFI',   'KONE'),
    ('atraore',  'AMINATA', 'TRAORE'),
    ('ybrou',    'YAO',     'BROU'),
    ('mdiallo',  'MARIAM',  'DIALLO')
  ) AS v(login, prenom, nom)
WHERE NOT EXISTS (SELECT 1 FROM app_user u WHERE u.login = v.login);

INSERT INTO user_authority (user_id, authority_name)
SELECT u.id, 'ROLE_VENDEUR'
FROM app_user u
WHERE u.login IN ('kkone', 'atraore', 'ybrou', 'mdiallo')
ON CONFLICT DO NOTHING;

INSERT INTO user_authority (user_id, authority_name)
SELECT u.id, 'ROLE_CAISSIER'
FROM app_user u
WHERE u.login IN ('kkone', 'atraore')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 3. Caisses — une par jour sur 180 jours
--
-- Le fonds initial et le montant final resteront à recaler par 09_ventes.sql,
-- une fois les encaissements connus : une caisse dont le final ne correspond
-- pas à ses règlements est le premier écart que relève un pharmacien.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_caisse AS
SELECT
    (CURRENT_DATE - (INTERVAL '1 day' * j))::date AS jour,
    j,
    -- Deux caissiers en alternance.
    (SELECT id FROM app_user WHERE login = CASE WHEN j % 2 = 0 THEN 'kkone' ELSE 'atraore' END) AS user_id
FROM generate_series(0, 180) AS j
-- L'officine ferme le lundi : c'est le jour de repos usuel après la garde
-- du week-end, et il doit exister dans les statistiques.
WHERE extract(dow FROM (CURRENT_DATE - (INTERVAL '1 day' * j))) <> 1;

INSERT INTO cash_register (
    user_id, init_amount, final_amount, cancele_amount,
    begin_time, end_time, created, updated, statut
)
SELECT
    c.user_id,
    50000,                              -- fonds de caisse d'ouverture
    50000,                              -- recalé par 09_ventes.sql
    0,
    c.jour + TIME '08:00:00',
    CASE WHEN c.j = 0 THEN NULL ELSE c.jour + TIME '20:00:00' END,
    c.jour + TIME '08:00:00',
    c.jour + TIME '20:00:00',
    -- La caisse du jour est encore ouverte ; les précédentes sont validées.
    CASE WHEN c.j = 0 THEN 'OPEN' ELSE 'VALIDATED' END
FROM tmp_caisse c;

-- ---------------------------------------------------------------------------
-- 4. Fonds de caisse — obligatoire, un par caisse
-- ---------------------------------------------------------------------------
INSERT INTO cash_fund (
    amount, cash_fund_type, statut, user_id, cash_register_id, validated_by_id,
    created, updated
)
SELECT
    cr.init_amount::int,
    'AUTO',
    CASE WHEN cr.statut = 'OPEN' THEN 'PENDING' ELSE 'VALIDETED' END,
    cr.user_id,
    cr.id,
    CASE WHEN cr.statut = 'OPEN' THEN NULL
         ELSE (SELECT id FROM app_user WHERE login = 'admin') END,
    cr.begin_time,
    cr.begin_time
FROM cash_register cr
WHERE NOT EXISTS (SELECT 1 FROM cash_fund f WHERE f.cash_register_id = cr.id);

DROP TABLE tmp_caisse;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_postes  int;
    v_caisses int;
    v_fonds   int;
    v_ouverte int;
    v_orphan  int;
BEGIN
    SELECT count(*) INTO v_postes  FROM poste;
    SELECT count(*) INTO v_caisses FROM cash_register;
    SELECT count(*) INTO v_fonds   FROM cash_fund;
    SELECT count(*) INTO v_ouverte FROM cash_register WHERE statut = 'OPEN';

    -- CashRegister.cashFund est @NotNull : une caisse sans fonds ne peut pas
    -- être chargée par l'application.
    SELECT count(*) INTO v_orphan FROM cash_register cr
     WHERE NOT EXISTS (SELECT 1 FROM cash_fund f WHERE f.cash_register_id = cr.id);

    IF v_postes < 3 THEN RAISE EXCEPTION 'Postes : % (attendu 3)', v_postes; END IF;
    IF v_caisses < 140 THEN RAISE EXCEPTION 'Caisses : % (attendu >= 140)', v_caisses; END IF;
    IF v_fonds <> v_caisses THEN RAISE EXCEPTION 'Fonds de caisse : % pour % caisses', v_fonds, v_caisses; END IF;
    IF v_ouverte <> 1 THEN RAISE EXCEPTION 'Caisses ouvertes : % (attendu 1)', v_ouverte; END IF;
    IF v_orphan > 0 THEN RAISE EXCEPTION '% caisse(s) sans fonds', v_orphan; END IF;

    RAISE NOTICE '% postes, % caisses, % fonds.', v_postes, v_caisses, v_fonds;
END $$;

\echo '<< 08_caisses : terminé'
