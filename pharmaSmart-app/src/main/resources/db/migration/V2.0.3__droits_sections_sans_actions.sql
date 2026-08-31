-- ============================================================================
-- Correction de V2.0.2 : les ONGLETS, pas les ACTIONS
--
-- V2.0.2 recopiait les droits du parent sur TOUS ses enfants pour ouvrir les
-- onglets d'inventaire et de péremptions aux rôles qui accèdent déjà à l'écran.
-- Or l'arbre de navigation ne contient pas que des onglets : les PRIVILÈGES y
-- sont eux aussi des enfants, avec `target_type = 'ACTION'`. Sous `inventaire`
-- vit ainsi `pr-voir-stock-inventaire`, « Voir stock lors de l'inventaire » —
-- celui-là même qui décide du comptage à l'aveugle.
--
-- La copie l'a donc accordé au responsable stock et au pharmacien, qui doivent
-- précisément ne pas l'avoir : ils comptent, et compter en voyant la quantité
-- attendue n'est plus compter. Un droit gagné par ricochet est le pire des
-- droits — personne ne l'a décidé, et rien ne le signale.
--
-- On retire donc ce que V2.0.2 a donné de trop : les ACTIONS, pour tout rôle
-- autre que l'administrateur. Les onglets (`SECTION`) restent ouverts.
-- ============================================================================

DELETE FROM nav_item_role r
 USING nav_item action, nav_item parent
 WHERE r.nav_item_id = action.id
   AND action.parent_id = parent.id
   AND parent.code IN ('inventaire', 'peremptions')
   AND action.target_type = 'ACTION'
   AND r.role_name <> 'ROLE_ADMIN';
