-- Dashboard personnalisable — phase 2, lot 1a : widgets issus des tableaux de bord existants.
--
-- Chaque widget est un nav_item WIDGET « widget.<clé> » ; la clé est celle du WidgetDataProvider
-- côté serveur et du registre côté front. Les rôles par défaut suivent le § 6.2 de
-- docs/PLAN-DASHBOARD-PERSONNALISABLE.md : l'administrateur les ajuste ensuite dans l'écran
-- d'attribution des menus. Un rôle absent de la base est ignoré.

WITH widgets(code, libelle, icon, categorie, ordre, roles) AS (
    VALUES
        -- Ventes : chiffres de l'officine, réservés à la gestion
        ('widget.ca-net',              'CA net',                     'pi pi-shopping-cart',       'ventes',   10, 'ROLE_ADMIN,ROLE_PHARMACIEN'),
        ('widget.marge-brute',         'Marge brute',                'pi pi-percentage',          'ventes',   20, 'ROLE_ADMIN,ROLE_PHARMACIEN'),
        ('widget.panier-moyen',        'Panier moyen',               'pi pi-shopping-bag',        'ventes',   30, 'ROLE_ADMIN,ROLE_PHARMACIEN'),
        ('widget.ventes-annulees',     'Ventes annulées',            'pi pi-ban',                 'ventes',   40, 'ROLE_ADMIN,ROLE_PHARMACIEN'),
        ('widget.ventes-par-type',     'Ventes par type',            'pi pi-chart-pie',           'ventes',   50, 'ROLE_ADMIN,ROLE_PHARMACIEN'),
        ('widget.modes-reglement',     'Modes de règlement',         'pi pi-credit-card',         'ventes',   60, 'ROLE_ADMIN,ROLE_PHARMACIEN'),
        ('widget.ca-evolution',        'Évolution du CA',            'pi pi-chart-line',          'ventes',   70, 'ROLE_ADMIN,ROLE_PHARMACIEN'),
        ('widget.top-produits',        'Meilleures ventes',          'pi pi-star',                'ventes',   80, 'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_RESPONSABLE_COMMANDE'),
        -- Caisse : « ma caisse » et « mes » widgets ne lisent que la caisse de l'utilisateur
        ('widget.ma-caisse',           'Ma caisse',                  'pi pi-wallet',              'caisse',   10, 'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_CAISSIER'),
        ('widget.mes-encaissements',   'Mes encaissements',          'pi pi-money-bill',          'caisse',   20, 'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_CAISSIER'),
        ('widget.mes-ventes-recentes', 'Mes dernières ventes',       'pi pi-list',                'caisse',   30, 'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_CAISSIER'),
        ('widget.differes-a-relancer', 'Différés à relancer',        'pi pi-calendar-times',      'caisse',   40, 'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_CAISSIER'),
        -- Stock
        ('widget.alertes-officine',    'Alertes de l''officine',     'pi pi-bell',                'stock',    10, 'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_RESPONSABLE_COMMANDE'),
        ('widget.alertes-stock',       'Alertes de stock',           'pi pi-exclamation-triangle','stock',    20, 'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_RESPONSABLE_COMMANDE'),
        ('widget.peremptions',         'Péremptions à venir',        'pi pi-clock',               'stock',    30, 'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_RESPONSABLE_COMMANDE'),
        ('widget.rotation-stock',      'Rotation du stock',          'pi pi-sync',                'stock',    40, 'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_RESPONSABLE_COMMANDE'),
        ('widget.analyse-abc',         'Analyse ABC',                'pi pi-sort-amount-down',    'stock',    50, 'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_RESPONSABLE_COMMANDE'),
        -- Achats
        ('widget.achats-periode',      'Achats de la période',       'pi pi-truck',               'achats',   10, 'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_RESPONSABLE_COMMANDE'),
        ('widget.commandes-en-cours',  'Commandes en cours',         'pi pi-inbox',               'achats',   20, 'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_RESPONSABLE_COMMANDE'),
        ('widget.suggestions-reappro', 'Suggestions de commande',    'pi pi-lightbulb',           'achats',   30, 'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_RESPONSABLE_COMMANDE'),
        ('widget.performance-fournisseurs', 'Performance fournisseurs', 'pi pi-chart-bar',        'achats',   40, 'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_RESPONSABLE_COMMANDE'),
        ('widget.livraisons-du-jour',  'Livraisons du jour',         'pi pi-box',                 'achats',   50, 'ROLE_ADMIN,ROLE_PHARMACIEN,ROLE_CAISSIER,ROLE_RESPONSABLE_COMMANDE')
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
