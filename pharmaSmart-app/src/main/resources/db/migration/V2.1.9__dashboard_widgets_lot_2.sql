-- Dashboard personnalisable — phase 3, lot 2 : premiers widgets à valeur ajoutée (§ 6.3 du plan).
--
-- Même convention que V2.1.7 : nav_item WIDGET « widget.<clé> », rôles par défaut ajustables dans
-- l'écran d'attribution des menus. Tous reposent sur des services existants, sans requête nouvelle.

WITH widgets(code, libelle, icon, categorie, ordre, roles) AS (
    VALUES
        ('widget.ca-vs-n1',               'CA comparé à l''an dernier', 'pi pi-calendar',        'ventes', 15, 'ROLE_ADMIN,ROLE_PHARMACIEN'),
        ('widget.a-faire',                'À faire aujourd''hui',       'pi pi-check-square',    'stock',  5,  'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_RESPONSABLE_COMMANDE'),
        ('widget.peremptions-valorisees', 'Péremptions valorisées',     'pi pi-hourglass',       'stock',  35, 'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_RESPONSABLE_COMMANDE'),
        ('widget.stock-dormant',          'Stock dormant',              'pi pi-pause-circle',    'stock',  45, 'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_RESPONSABLE_COMMANDE')
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
SELECT i.id, a.name, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE
FROM inserted i
         JOIN widgets w ON w.code = i.code
         CROSS JOIN LATERAL unnest(string_to_array(w.roles, ',')) AS r(role_name)
         JOIN authority a ON a.name = r.role_name
ON CONFLICT (nav_item_id, role_name) DO NOTHING;
