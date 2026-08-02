
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'pr-cloture-inventaire',
       'Clôturer un inventaire',
       NULL,
       NULL,
       id,
       8,
       3,
       'ACTION',
       TRUE
FROM nav_item
WHERE code = 'inventaire' ON CONFLICT (code) DO NOTHING;


INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete,
                           can_export, can_execute)
SELECT id,
       role_name,
       TRUE,
       TRUE,
       FALSE,
       FALSE,
       FALSE,
       FALSE,
       TRUE
FROM nav_item,
     (VALUES ('ROLE_ADMIN'), ('ROLE_PHARMACIEN')) AS roles(role_name)
WHERE code = 'pr-cloture-inventaire' ON CONFLICT (nav_item_id, role_name) DO
UPDATE SET can_execute = TRUE;

ALTER TABLE store_inventory_line
    ADD COLUMN IF NOT EXISTS counted_by_id INTEGER NULL;

ALTER TABLE store_inventory_line
    DROP CONSTRAINT IF EXISTS fk_sil_counted_by;

ALTER TABLE store_inventory_line
    ADD CONSTRAINT fk_sil_counted_by
        FOREIGN KEY (counted_by_id) REFERENCES app_user (id);


CREATE INDEX IF NOT EXISTS idx_sil_counted_by
    ON store_inventory_line (store_inventory_id, counted_by_id);


ALTER TABLE store_inventory_line
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
