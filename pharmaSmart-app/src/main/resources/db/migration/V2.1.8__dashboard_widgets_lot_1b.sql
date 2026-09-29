-- Dashboard personnalisable — phase 2, lot 1b : widgets issus de l'accueil pharmacien.
--
-- Même convention que V2.1.7 : nav_item WIDGET « widget.<clé> », rôles par défaut du § 6.2 de
-- docs/PLAN-DASHBOARD-PERSONNALISABLE.md, ajustables dans l'écran d'attribution des menus.
-- Marge, créances et différés sont des chiffres de gestion : réservés à l'administrateur et au
-- pharmacien.

WITH widgets(code, libelle, icon, categorie, ordre, roles) AS (
    VALUES
        ('widget.ventes-par-tiers-payant', 'Ventes par tiers payant',   'pi pi-id-card',          'ventes',   90,  'ROLE_ADMIN,ROLE_PHARMACIEN'),
        ('widget.pareto-produits',         'Pareto 20/80',              'pi pi-sort-amount-down', 'ventes',   100, 'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_RESPONSABLE_COMMANDE'),
        ('widget.stock-valorise',          'Stock valorisé',            'pi pi-box',              'stock',    60,  'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_RESPONSABLE_COMMANDE'),
        ('widget.achats-par-fournisseur',  'Achats par fournisseur',    'pi pi-truck',            'achats',   60,  'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_RESPONSABLE_COMMANDE'),
        ('widget.qualite-fournisseurs',    'Qualité fournisseurs',      'pi pi-verified',         'achats',   70,  'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_RESPONSABLE_COMMANDE'),
        ('widget.marge-12-mois',           'Marge sur 12 mois',         'pi pi-chart-line',       'finances', 10,  'ROLE_ADMIN,ROLE_PHARMACIEN'),
        ('widget.creances-tp',             'Créances tiers payant',     'pi pi-credit-card',      'finances', 20,  'ROLE_ADMIN,ROLE_PHARMACIEN'),
        ('widget.creances-par-organisme',  'Créances par organisme',    'pi pi-building',         'finances', 30,  'ROLE_ADMIN,ROLE_PHARMACIEN'),
        ('widget.differes-encours',        'Différés clients',          'pi pi-users',            'finances', 40,  'ROLE_ADMIN,ROLE_PHARMACIEN')
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
