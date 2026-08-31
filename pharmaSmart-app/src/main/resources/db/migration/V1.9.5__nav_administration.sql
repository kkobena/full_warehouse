
UPDATE nav_item child
   SET parent_id = parent.id,
       niveau    = 2,
       ordre     = COALESCE(
           (SELECT MAX(c.ordre) FROM nav_item c WHERE c.parent_id = parent.id), 0
       ) + 1,
       updated   = NOW()
  FROM nav_item parent
 WHERE parent.code = 'administration'
   AND child.code  = 'declaration-ca'
   AND child.parent_id IS DISTINCT FROM parent.id;

-- Création de « Guide des fonctionnalités », jusqu'ici absent de la base.
INSERT INTO nav_item (
    code, libelle, titre_long, icon, router_link,
    parent_id, ordre, niveau, actif, created, updated
)
SELECT
    'cahier-recette',
    'Guide des fonctionnalités',
    'Guide des fonctionnalités de l''application',
    'pi pi-book',
    '/cahier-recette',
    p.id,
    COALESCE((SELECT MAX(c.ordre) FROM nav_item c WHERE c.parent_id = p.id), 0) + 1,
    2,
    TRUE,
    NOW(),
    NOW()
FROM nav_item p
WHERE p.code = 'administration'
  AND NOT EXISTS (SELECT 1 FROM nav_item x WHERE x.code = 'cahier-recette');

-- Droits : réservé aux administrateurs, comme l'était la version câblée en dur.
INSERT INTO nav_item_role (
    nav_item_id, role_name,
    can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute
)
SELECT n.id, 'ROLE_ADMIN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, TRUE
FROM nav_item n
WHERE n.code = 'cahier-recette'
ON CONFLICT (nav_item_id, role_name) DO NOTHING;

-- Désactivation des deux actions de la barre de navigation.
UPDATE nav_item
   SET actif = FALSE,
       updated = NOW()
 WHERE code IN ('nouvelle-vente', 'nouvelle-prevente')
   AND actif;
alter table classification_criticite_log
  alter column ancienne_classe type varchar(10) using ancienne_classe::varchar(10);
alter table semois_configuration
  alter column classe_criticite type varchar(10) using classe_criticite::varchar(10);
