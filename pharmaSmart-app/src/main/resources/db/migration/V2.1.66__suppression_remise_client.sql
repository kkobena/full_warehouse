-- La remise client, héritage jamais exercé, est retirée : seule la remise produit (grille par code
-- remise) s'applique à une vente. À ne pas confondre avec la fidélisation client, qui reste.
--
-- Une vente qui aurait porté une remise client garde ses montants (remise, net) : seul le lien vers
-- la remise disparaît, la remise elle-même n'existant plus.

UPDATE sales
SET remise_id = NULL
WHERE remise_id IN (SELECT id FROM remise WHERE dtype = 'RemiseClient');

ALTER TABLE customer DROP COLUMN IF EXISTS remise_client_id;

DELETE FROM remise WHERE dtype = 'RemiseClient';

ALTER TABLE remise DROP CONSTRAINT IF EXISTS remise_dtype_check;
ALTER TABLE remise
    ADD CONSTRAINT remise_dtype_check CHECK (dtype IN ('Remise', 'RemiseProduit'));

-- Le taux de la remise client ; celui d'une remise produit vit sur grille_remise.remise_value.
ALTER TABLE remise DROP COLUMN IF EXISTS remise_value;
