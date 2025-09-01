-- CREATE TABLE third_party_sale_line_y2025 PARTITION OF third_party_sale_line_archives FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
-- CREATE TABLE third_party_sale_line_y2026 PARTITION OF third_party_sale_line_archives FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
-- CREATE TABLE third_party_sale_line_y2027 PARTITION OF third_party_sale_line_archives FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');
-- CREATE TABLE third_party_sale_line_y2028 PARTITION OF third_party_sale_line_archives FOR VALUES FROM ('2028-01-01') TO ('2029-01-01');
-- CREATE TABLE third_party_sale_line_y2029 PARTITION OF third_party_sale_line_archives FOR VALUES FROM ('2029-01-01') TO ('2030-01-01');
--
--


-- CREATE TABLE sales_line_y2025 PARTITION OF sales_line FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
-- CREATE TABLE sales_line_y2026 PARTITION OF sales_line FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
-- CREATE TABLE sales_line_y2027 PARTITION OF sales_line FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');
-- CREATE TABLE sales_line_y2028 PARTITION OF sales_line FOR VALUES FROM ('2028-01-01') TO ('2029-01-01');
-- CREATE TABLE sales_line_y2029 PARTITION OF sales_line FOR VALUES FROM ('2029-01-01') TO ('2030-01-01');

-- test
CREATE TABLE sales_line_y30 PARTITION OF sales_line FOR VALUES FROM ('2025-08-30') TO ('2026-08-31');
CREATE TABLE sales_line_y31 PARTITION OF sales_line FOR VALUES FROM ('2026-08-31') TO ('2026-09-01');
CREATE TABLE sales_line_y32 PARTITION OF sales_line FOR VALUES FROM ('2026-09-01') TO ('2026-09-02');

CREATE TABLE third_party_sale_line_y30 PARTITION OF third_party_sale_line FOR VALUES FROM ('2025-08-30') TO ('2026-08-31');
CREATE TABLE third_party_sale_line_y31 PARTITION OF third_party_sale_line FOR VALUES FROM ('2026-08-31') TO ('2026-09-01');
CREATE TABLE third_party_sale_line_y32 PARTITION OF third_party_sale_line FOR VALUES FROM ('2026-09-01') TO ('2026-09-02');


CREATE TABLE sales_y30 PARTITION OF sales FOR VALUES FROM ('2025-08-30') TO ('2026-08-31');
CREATE TABLE sales_y31 PARTITION OF sales FOR VALUES FROM ('2026-08-31') TO ('2026-09-01');
CREATE TABLE sales_y32 PARTITION OF sales FOR VALUES FROM ('2026-09-01') TO ('2026-09-02');



-- PARTITION BY RANGE (updated_at)
-- CREATE TABLE sales_y2025 PARTITION OF sales FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
-- CREATE TABLE sales_y2026 PARTITION OF sales FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
-- CREATE TABLE sales_y2027 PARTITION OF sales FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');
-- CREATE TABLE sales_y2028 PARTITION OF sales FOR VALUES FROM ('2028-01-01') TO ('2029-01-01');
-- CREATE TABLE sales_y2029 PARTITION OF sales FOR VALUES FROM ('2029-01-01') TO ('2030-01-01');

-- PARTITION BY RANGE (updated_at)
CREATE TABLE commande_y2025 PARTITION OF commande FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
CREATE TABLE commande_y2026 PARTITION OF commande FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
CREATE TABLE commande_y2027 PARTITION OF commande FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');
CREATE TABLE commande_y2028 PARTITION OF commande FOR VALUES FROM ('2028-01-01') TO ('2029-01-01');
CREATE TABLE commande_y2029 PARTITION OF commande FOR VALUES FROM ('2029-01-01') TO ('2030-01-01');

-- ORDER LINE
CREATE TABLE order_line_y2025 PARTITION OF order_line FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
CREATE TABLE order_line_y2026 PARTITION OF order_line FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
CREATE TABLE order_line_y2027 PARTITION OF order_line FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');
CREATE TABLE order_line_y2028 PARTITION OF order_line FOR VALUES FROM ('2028-01-01') TO ('2029-01-01');
CREATE TABLE order_line_y2029 PARTITION OF order_line FOR VALUES FROM ('2029-01-01') TO ('2030-01-01');

-- facture_tiers_payant PARTITION BY RANGE (created)
CREATE TABLE facture_tiers_payant_y2025 PARTITION OF facture_tiers_payant FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
CREATE TABLE facture_tiers_payant_y2026 PARTITION OF facture_tiers_payant FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
CREATE TABLE facture_tiers_payant_y2027 PARTITION OF facture_tiers_payant FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');
CREATE TABLE facture_tiers_payant_y2028 PARTITION OF facture_tiers_payant FOR VALUES FROM ('2028-01-01') TO ('2029-01-01');
CREATE TABLE facture_tiers_payant_y2029 PARTITION OF facture_tiers_payant FOR VALUES FROM ('2029-01-01') TO ('2030-01-01');


-- inventory_transaction PARTITION BY RANGE (created_at)
CREATE TABLE inventory_transaction_y2025 PARTITION OF inventory_transaction FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
CREATE TABLE inventory_transaction_y2026 PARTITION OF inventory_transaction FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
CREATE TABLE inventory_transaction_y2027 PARTITION OF inventory_transaction FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');
CREATE TABLE inventory_transaction_y2028 PARTITION OF inventory_transaction FOR VALUES FROM ('2028-01-01') TO ('2029-01-01');
CREATE TABLE inventory_transaction_y2029 PARTITION OF inventory_transaction FOR VALUES FROM ('2029-01-01') TO ('2030-01-01');


CREATE TABLE payment_transaction_y2025 PARTITION OF payment_transaction FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
CREATE TABLE payment_transaction_y2026 PARTITION OF payment_transaction FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
CREATE TABLE payment_transaction_y2027 PARTITION OF payment_transaction FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');
CREATE TABLE payment_transaction_y2028 PARTITION OF payment_transaction FOR VALUES FROM ('2028-01-01') TO ('2029-01-01');
CREATE TABLE payment_transaction_y2029 PARTITION OF payment_transaction FOR VALUES FROM ('2029-01-01') TO ('2030-01-01');


CREATE TABLE invoice_payment_item_y2025 PARTITION OF invoice_payment_item FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
CREATE TABLE invoice_payment_item_y2026 PARTITION OF invoice_payment_item FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
CREATE TABLE invoice_payment_item_y2027 PARTITION OF invoice_payment_item FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');
CREATE TABLE invoice_payment_item_y2028 PARTITION OF invoice_payment_item FOR VALUES FROM ('2028-01-01') TO ('2029-01-01');
CREATE TABLE invoice_payment_item_y2029 PARTITION OF invoice_payment_item FOR VALUES FROM ('2029-01-01') TO ('2030-01-01');


CREATE TABLE differe_payment_item_y2025 PARTITION OF differe_payment_item FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
CREATE TABLE differe_payment_item_y2026 PARTITION OF differe_payment_item FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
CREATE TABLE differe_payment_item_y2027 PARTITION OF differe_payment_item FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');
CREATE TABLE differe_payment_item_y2028 PARTITION OF differe_payment_item FOR VALUES FROM ('2028-01-01') TO ('2029-01-01');
CREATE TABLE differe_payment_item_y2029 PARTITION OF differe_payment_item FOR VALUES FROM ('2029-01-01') TO ('2030-01-01');
