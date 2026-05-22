-- Fix: comptabilite doit être un ROUTE direct de niveau 1 (lien direct dans la navbar).
-- Les enfants restent SECTION : onglets gérés par la sidebar dans la page.

UPDATE nav_item
SET target_type = 'ROUTE',
    niveau      = 1,
    ordre       = 5,
    parent_id   = NULL,
    router_link = '/comptabilite'
WHERE code = 'comptabilite';

UPDATE nav_item
SET target_type = 'SECTION',
    router_link = NULL
WHERE code IN (
    'comptabilite.balance',
    'comptabilite.taxe-report',
    'comptabilite.tableau-pharmacien',
    'comptabilite.recapitulatif-caisse',
    'comptabilite.raport-activite'
);
