-- Dashboard personnalisable — widgets composites : une tuile = plusieurs métriques connexes.
--
-- Même convention que V2.1.9 : nav_item WIDGET « widget.<clé> », rôles ajustables dans
-- l'écran d'attribution des menus.

WITH widgets(code, libelle, icon, categorie, ordre, roles) AS (
    VALUES
        ('widget.synthese-activite', 'Synthèse d''activité',  'pi pi-chart-line', 'ventes',   12, 'ROLE_ADMIN,ROLE_PHARMACIEN'),
        ('widget.sante-stock',       'Santé du stock',        'pi pi-box',        'stock',    22, 'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_RESPONSABLE_COMMANDE'),
        ('widget.synthese-achats',   'Synthèse achats',       'pi pi-truck',      'achats',   12, 'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_RESPONSABLE_COMMANDE'),
        ('widget.bilan-finances',    'Bilan financier',       'pi pi-wallet',     'finances', 12, 'ROLE_ADMIN,ROLE_PHARMACIEN')
),
inserted AS (
    INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
    SELECT w.code, w.libelle, w.icon, NULL, p.id, w.ordre, 3, 'WIDGET', TRUE
    FROM widgets w
             JOIN nav_item p ON p.code = 'dashboard-perso.widgets.' || w.categorie
    ON CONFLICT (code) DO NOTHING
    RETURNING id, code
)
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT i.id, r.role_name, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE
FROM inserted i
         JOIN widgets w ON w.code = i.code
         CROSS JOIN LATERAL unnest(string_to_array(w.roles, ',')) AS r(role_name)
         JOIN authority a ON a.name = r.role_name
ON CONFLICT (nav_item_id, role_name) DO NOTHING;
