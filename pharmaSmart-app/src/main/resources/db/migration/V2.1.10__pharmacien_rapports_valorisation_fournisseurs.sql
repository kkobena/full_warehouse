-- Le pharmacien n'avait pas accès aux rapports « Valorisation du stock » et « Performance
-- fournisseurs », alors que son accueil en affiche les chiffres : un oubli d'attribution.
-- Mêmes droits que sur ses autres sections de rapport : afficher, ouvrir, exporter.

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT n.id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item n
WHERE n.code IN ('rapport-stock.stock-valuation', 'rapport-partners.supplier-performance')
  AND EXISTS (SELECT 1 FROM authority a WHERE a.name = 'ROLE_PHARMACIEN')
ON CONFLICT (nav_item_id, role_name) DO UPDATE
    SET can_display = TRUE,
        can_access  = TRUE,
        can_export  = TRUE;
