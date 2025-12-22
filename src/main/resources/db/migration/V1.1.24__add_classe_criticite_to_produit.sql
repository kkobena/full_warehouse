
ALTER TABLE produit
    ADD COLUMN classe_criticite VARCHAR(10);

-- Index pour performances (index partiel: seulement si NOT NULL)
CREATE INDEX idx_produit_classe_criticite ON produit(classe_criticite)
    WHERE classe_criticite IS NOT NULL;

ALTER TABLE  produit drop  column  categorie;
