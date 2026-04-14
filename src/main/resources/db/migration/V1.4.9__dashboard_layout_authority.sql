
ALTER TABLE dashboard_layout
    ADD COLUMN is_route        BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN component_key   VARCHAR(30)  NOT NULL DEFAULT 'ROUTE';

COMMENT ON COLUMN dashboard_layout.is_route IS
    'Si true : name est une route Angular (redirection). Si false : component_key + layout_config sont utilisés.';
COMMENT ON COLUMN dashboard_layout.component_key IS
    'Clé de dispatch Angular : PHARMACIEN | CAISSIER | COMMANDE | ROUTE. Indépendant du rôle — permet à un nouveau rôle de réutiliser un composant existant.';


CREATE TABLE dashboard_layout_authority (
    layout_id       INTEGER     NOT NULL REFERENCES dashboard_layout(id) ON DELETE CASCADE,
    authority_name  VARCHAR(50) NOT NULL REFERENCES authority(name) ON DELETE CASCADE,
    is_default      BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_dashboard_layout_authority PRIMARY KEY (layout_id, authority_name)
);

CREATE INDEX idx_dla_authority ON dashboard_layout_authority(authority_name);
CREATE INDEX idx_dla_authority_default ON dashboard_layout_authority(authority_name, is_default) WHERE is_default = TRUE;

-- Seed : layouts par rôle
INSERT INTO dashboard_layout (name, description, scope, is_default, is_route, component_key, layout_config)
VALUES

    (
        'home-base',
        'Tableau de board Pharmacien',
        'PUBLIC', FALSE, FALSE, 'PHARMACIEN', NULL
    ),
    (
        '/sales-home/prevente',
        'Redirection vers l''interface de prévente',
        'PUBLIC', FALSE, TRUE, 'ROUTE', NULL
    ),
    (
        'caissier-dashboard',
        'Tableau de board caissier',
        'PUBLIC', FALSE, FALSE, 'CAISSIER', NULL
    ),
    (
        '/commande',
        'Tableau de board responsable stock/commande',
        'PUBLIC', FALSE, FALSE, 'COMMANDE', NULL
    );

INSERT INTO dashboard_layout_authority (layout_id, authority_name, is_default)
SELECT dl.id, 'ROLE_ADMIN', TRUE
FROM dashboard_layout dl WHERE dl.name = 'home-base';

INSERT INTO dashboard_layout_authority (layout_id, authority_name, is_default)
SELECT dl.id, 'ROLE_PHARMACIEN', TRUE
FROM dashboard_layout dl WHERE dl.name = 'home-base';

INSERT INTO dashboard_layout_authority (layout_id, authority_name, is_default)
SELECT dl.id, 'ROLE_VENDEUR', TRUE
FROM dashboard_layout dl WHERE dl.name = '/sales-home/prevente';

INSERT INTO dashboard_layout_authority (layout_id, authority_name, is_default)
SELECT dl.id, 'ROLE_CAISSIER', TRUE
FROM dashboard_layout dl WHERE dl.name = 'caissier-dashboard';

INSERT INTO dashboard_layout_authority (layout_id, authority_name, is_default)
SELECT dl.id, 'ROLE_RESPONSABLE_COMMANDE', TRUE
FROM dashboard_layout dl WHERE dl.name = '/commande';
