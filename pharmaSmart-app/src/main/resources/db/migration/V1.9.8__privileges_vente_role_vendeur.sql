-- ============================================================================
-- V1.9.8 — Le vendeur ne supprime pas une ligne de vente sans autorisation
--
-- L'écran de vente prévoit une autorisation par clé de sécurité pour trois
-- gestes sensibles : supprimer une ligne, accorder une remise, modifier un
-- prix. La demande n'apparaît que si l'utilisateur ne détient PAS le privilège
-- — or ROLE_VENDEUR les détenait tous les trois, si bien que le contrôle ne se
-- déclenchait jamais et que le geste passait sans trace.
--
-- Ces trois privilèges reviennent donc à l'encadrement seul — administrateur et
-- pharmacien titulaire. Vendeur comme caissier demandent la clé, et chaque
-- utilisation est journalisée dans `utilisation_cle_securite` avec son motif.
-- ============================================================================

DELETE FROM nav_item_role r
 USING nav_item n
 WHERE n.id = r.nav_item_id
   AND r.role_name IN ('ROLE_VENDEUR', 'ROLE_CAISSIER')
   AND n.code IN ('pr-supprimer-ligne-vente', 'pr-remise-vente', 'pr-modifier-prix');

-- Le pharmacien titulaire, lui, doit pouvoir autoriser : sans un détenteur
-- présent en officine, la clé de sécurité ne servirait à rien.
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_execute)
SELECT n.id, 'ROLE_PHARMACIEN', TRUE, TRUE
  FROM nav_item n
 WHERE n.code IN ('pr-supprimer-ligne-vente', 'pr-modifier-prix', 'pr-remise-vente', 'pr-annuler-vente')
   AND NOT EXISTS (
       SELECT 1 FROM nav_item_role r
        WHERE r.nav_item_id = n.id AND r.role_name = 'ROLE_PHARMACIEN'
   );
