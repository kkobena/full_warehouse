-- ═══════════════════════════════════════════════════════════════════════════
-- BED (Bon d'Entrée Diverse) — champs sur la table commande
-- ═══════════════════════════════════════════════════════════════════════════

ALTER TABLE commande
    ADD COLUMN IF NOT EXISTS motif_bed       VARCHAR(25),
    ADD COLUMN IF NOT EXISTS commentaire_bed VARCHAR(255);

-- Rendre fournisseur_id nullable pour les BED (type = DIRECT, fournisseur optionnel)
ALTER TABLE commande
    ALTER COLUMN fournisseur_id DROP NOT NULL;

-- ═══════════════════════════════════════════════════════════════════════════
-- Navigation — onglet Bons d'Entrée Diverses dans le module commande
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'commande.bed', 'Bons d''Entrée Diverses', 'pi pi-file-plus', NULL, id, 50, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'commande'
ON CONFLICT (code) DO NOTHING;

-- Accès complet ROLE_ADMIN
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item WHERE code = 'commande.bed'
ON CONFLICT (nav_item_id, role_name) DO NOTHING;

-- Accès ROLE_RESPONSABLE_COMMANDE (pas de suppression)
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_RESPONSABLE_COMMANDE', TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE
FROM nav_item WHERE code = 'commande.bed'
ON CONFLICT (nav_item_id, role_name) DO NOTHING;

-- Accès lecture seule ROLE_PHARMACIEN
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item WHERE code = 'commande.bed'
ON CONFLICT (nav_item_id, role_name) DO NOTHING;
