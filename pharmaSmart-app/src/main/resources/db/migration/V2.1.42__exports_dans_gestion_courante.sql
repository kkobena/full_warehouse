
WITH exports AS (SELECT id, ordre
                 FROM nav_item
                 WHERE code = 'exports'
                   AND parent_id IS NULL),
     decalage AS (UPDATE nav_item n
       SET ordre = n.ordre - 1
       FROM exports e
       WHERE n.parent_id IS NULL
         AND n.code <> 'exports'
         AND n.ordre > e.ordre
         AND n.ordre < 900)
UPDATE nav_item n
SET parent_id = g.id,
    niveau    = 2,
    ordre     = g.dernier_ordre + 1
FROM exports e,
     (SELECT p.id, COALESCE(max(c.ordre), -1) AS dernier_ordre
      FROM nav_item p
             LEFT JOIN nav_item c ON c.parent_id = p.id
      WHERE p.code = 'gestion-courante'
      GROUP BY p.id) g
WHERE n.id = e.id;

UPDATE nav_item
SET actif = false
WHERE
   code = 'poste';

