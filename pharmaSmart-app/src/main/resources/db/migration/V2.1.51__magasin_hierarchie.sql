-- V2.1.51 — La hierarchie des magasins, d'un seul tenant
--
-- CE FICHIER EN REMPLACE QUATRE
-- La hierarchie des magasins s'est ecrite en quatre temps : rattachement, type UNITE_GESTION,
-- magasin porte par les caisses et les commandes, droit de supervision. Aucun de ces scripts n'a
-- tourne ailleurs que sur les bases de recette — l'installation de reference s'arrete a 2.1.47.
-- Les livrer separement ferait rejouer quatre fois la meme montee, dont une qui ne fait que
-- reparer l'oubli d'une autre.
--
-- L'ORDRE COMPTE
-- La section 2 vient juste apres la 1 parce qu'elle repare son oubli. Les sections 3 et 4
-- s'appuient sur les colonnes posees par la 1.

-- ============================================================================
-- Section 1. Rattacher un magasin a son magasin principal
-- ============================================================================

ALTER TABLE magasin
  ADD COLUMN parent_id INTEGER;

ALTER TABLE magasin
  ADD CONSTRAINT fk_magasin_parent
    FOREIGN KEY (parent_id) REFERENCES magasin (id);

CREATE INDEX idx_magasin_parent ON magasin (parent_id);

-- Un magasin ne peut pas être son propre parent. Les cycles plus longs sont impossibles en
-- pratique — l'arbre est profond d'un niveau, une officine ne pouvant pas dépendre d'une autre —
-- et sont tenus côté service, où le message peut expliquer la règle.
ALTER TABLE magasin
  ADD CONSTRAINT magasin_parent_pas_soi_meme CHECK (parent_id IS DISTINCT FROM id);

-- COMMANDER EN PROPRE N'EST PAS UNE PROPRIÉTÉ DU TYPE
-- Une parapharmacie commande sa cosmétique elle-même alors qu'elle n'est pas un établissement ;
-- un dépôt, qui en est un, ne commande pas — il s'approvisionne auprès de son officine. La
-- capacité à commander ne se déduit donc ni du type, ni de la nature juridique : elle est portée
-- à part.
ALTER TABLE magasin
  ADD COLUMN commande_en_propre BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE magasin
   SET commande_en_propre = FALSE
 WHERE type_magasin = 'DEPOT';


UPDATE magasin m
   SET parent_id = (SELECT o.id FROM magasin o WHERE o.type_magasin = 'OFFICINE' ORDER BY o.id LIMIT 1)
 WHERE m.type_magasin <> 'OFFICINE'
   AND m.parent_id IS NULL
   AND (SELECT COUNT(*) FROM magasin o WHERE o.type_magasin = 'OFFICINE') = 1;

COMMENT ON COLUMN magasin.parent_id IS
  'Magasin principal dont ce magasin depend. NULL pour une officine, qui est racine.';

COMMENT ON COLUMN magasin.commande_en_propre IS
  'Ce magasin passe-t-il ses propres commandes fournisseur ? Faux pour un depot, approvisionne par son officine.';


-- ============================================================================
-- Section 2. Autoriser le type UNITE_GESTION
-- ============================================================================
--
-- `TypeMagasin.UNITE_GESTION` est le type d'une parapharmacie : unite interne au meme lieu que
-- l'officine, qui tient son stock et commande en propre sans etre un etablissement distinct.
--
-- Le controle pose par V1.0.1 n'a jamais ete elargi : il n'acceptait que 'OFFICINE' et 'DEPOT'.
-- UNE PARAPHARMACIE NE POUVAIT DONC PAS EXISTER EN BASE. Tout ce qui repose sur elle —
-- `commande_en_propre`, l'affectation d'un utilisateur ailleurs qu'a l'officine, la portee de
-- supervision, le reassort par magasin — etait ecrit, eprouve par des essais unitaires, et
-- impossible a exercer pour de vrai. Trouve le 2026-11-16 en voulant creer une parapharmacie.
--
-- `DEPOT_AGGREE` n'a jamais figure dans ce controle et a ete retire de l'enumeration Java. La
-- liste ci-dessous est donc exactement celle de `TypeMagasin` — et c'est ce qu'il faut tenir : un
-- type ajoute cote Java sans l'etre ici ne se voit qu'au premier enregistrement, en production.

ALTER TABLE magasin
  DROP CONSTRAINT IF EXISTS magasin_type_magasin_check;

ALTER TABLE magasin
  ADD CONSTRAINT magasin_type_magasin_check
    CHECK (type_magasin IN ('OFFICINE', 'DEPOT', 'UNITE_GESTION'));


-- ============================================================================
-- Section 3. Caisses et commandes portent leur magasin
-- ============================================================================

ALTER TABLE cash_register ADD COLUMN magasin_id INTEGER;
ALTER TABLE commande      ADD COLUMN magasin_id INTEGER;

UPDATE cash_register c
   SET magasin_id = u.magasin_id
  FROM app_user u
 WHERE u.id = c.user_id
   AND c.magasin_id IS NULL;

UPDATE commande o
   SET magasin_id = u.magasin_id
  FROM app_user u
 WHERE u.id = o.user_id
   AND o.magasin_id IS NULL;

-- Filet : une commande sans utilisateur, s'il en existe, est rattachee a l'officine. Le cas ne se
-- presente pas sur les bases connues, mais un NOT NULL qui echoue bloquerait le demarrage.
UPDATE commande
   SET magasin_id = (SELECT m.id FROM magasin m WHERE m.type_magasin = 'OFFICINE' ORDER BY m.id LIMIT 1)
 WHERE magasin_id IS NULL;

UPDATE cash_register
   SET magasin_id = (SELECT m.id FROM magasin m WHERE m.type_magasin = 'OFFICINE' ORDER BY m.id LIMIT 1)
 WHERE magasin_id IS NULL;

ALTER TABLE cash_register ALTER COLUMN magasin_id SET NOT NULL;
ALTER TABLE commande      ALTER COLUMN magasin_id SET NOT NULL;

ALTER TABLE cash_register
  ADD CONSTRAINT fk_cash_register_magasin FOREIGN KEY (magasin_id) REFERENCES magasin (id);
ALTER TABLE commande
  ADD CONSTRAINT fk_commande_magasin FOREIGN KEY (magasin_id) REFERENCES magasin (id);

CREATE INDEX idx_cash_register_magasin ON cash_register (magasin_id);
CREATE INDEX idx_commande_magasin      ON commande (magasin_id);

COMMENT ON COLUMN cash_register.magasin_id IS
  'Magasin ou cette caisse a ete ouverte. Fige a l''ouverture : une caisse ne se reaffecte pas.';

COMMENT ON COLUMN commande.magasin_id IS
  'Magasin qui a passe cette commande. Fige a la creation, non deduit de l''utilisateur courant.';


-- ============================================================================
-- Section 4. Le droit de superviser les autres magasins
-- ============================================================================



INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'supervision.magasins',
       'Superviser les autres magasins',
       NULL,
       NULL,
       id,
       99,
       2,
       'ACTION',
       TRUE
FROM nav_item
WHERE code = 'administration'
ON CONFLICT (code) DO NOTHING;

-- Le droit est accorde au PHARMACIEN et a l'ADMIN.

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT i.id, a.name, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE
  FROM nav_item i
 CROSS JOIN authority a
 WHERE i.code = 'supervision.magasins'
   AND a.name IN ('ROLE_PHARMACIEN', 'ROLE_ADMIN')
ON CONFLICT (nav_item_id, role_name) DO NOTHING;
