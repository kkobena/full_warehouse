
WITH dashboard AS (SELECT d.id, d.ordre, d.parent_id
                   FROM nav_item d
                            JOIN nav_item p ON p.id = d.parent_id
                   WHERE d.code = 'dashboard-perso'
                     AND p.code = 'rapports'),
     decalage AS (UPDATE nav_item n
         SET ordre = n.ordre - 1
         FROM dashboard d
         WHERE n.parent_id = d.parent_id
           AND n.id <> d.id
           AND n.ordre > d.ordre)
UPDATE nav_item n
SET parent_id = NULL,
    niveau    = 1,
    ordre     = 991
FROM dashboard d
WHERE n.id = d.id;
