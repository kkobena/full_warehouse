-- ============================================================================
-- Droits sur les ONGLETS d'inventaire et de péremptions
--
-- Les écrans « Inventaire » et « Péremptions » sont découpés en sections
-- (`inventaire.en-cours`, `inventaire.tournant`, `inventaire.clotures`,
-- `peremptions.lot-perimes`, `peremptions.lot-a-detruire`) et le composant
-- n'affiche un onglet que si le rôle porte le droit `display` sur la SECTION.
--
-- Or ces sections n'ont été attribuées qu'à ROLE_ADMIN (V1.4.8), alors que
-- l'écran parent est ouvert à d'autres rôles : ROLE_RESPONSABLE_COMMANDE et
-- ROLE_PHARMACIEN (V1.4.7). Ces rôles ouvraient donc un écran d'inventaire
-- VIDE — titre, bouton « Nouveau », et pas un seul onglet. L'API, elle,
-- répondait normalement : rien à l'écran ne pouvait mettre sur la piste.
--
-- Les droits de la section sont recopiés de ceux du parent : un rôle qui peut
-- créer un inventaire peut créer dans ses onglets, un rôle en lecture y reste
-- en lecture.
-- ============================================================================

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create,
                           can_edit, can_delete, can_export, can_execute)
SELECT section.id,
       parent_role.role_name,
       parent_role.can_display,
       parent_role.can_access,
       parent_role.can_create,
       parent_role.can_edit,
       parent_role.can_delete,
       parent_role.can_export,
       parent_role.can_execute
  FROM nav_item section
  JOIN nav_item parent ON parent.id = section.parent_id
  JOIN nav_item_role parent_role ON parent_role.nav_item_id = parent.id
 WHERE parent.code IN ('inventaire', 'peremptions')
ON CONFLICT (nav_item_id, role_name) DO NOTHING;
