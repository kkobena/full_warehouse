-- Add cancellation audit fields to sales table
ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS cancel_comment VARCHAR(255),
    ADD COLUMN IF NOT EXISTS cancelled_by_id INTEGER REFERENCES app_user (id);
INSERT INTO app_configuration (name, value, description, value_type)
VALUES ('APP_CANCEL_SALE_MAX_DAYS', '30', 'Délai maximum en jours pour annuler une vente clôturée',
        'NUMBER')
ON CONFLICT (name) DO NOTHING;
