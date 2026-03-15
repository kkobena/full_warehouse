CREATE TABLE inventory_gap_analysis
(
    id                      BIGSERIAL    PRIMARY KEY,
    store_inventory_line_id BIGINT       NOT NULL REFERENCES store_inventory_line (id) ON DELETE CASCADE,
    cause                   VARCHAR(30)  NOT NULL,
    quantity                INT          NOT NULL,
    commentaire             TEXT,
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_iga_line_id     ON inventory_gap_analysis (store_inventory_line_id);
CREATE INDEX idx_iga_inventory   ON inventory_gap_analysis (store_inventory_line_id, cause);
