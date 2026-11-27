-- Promotions, phase 2 (docs/PLAN-GESTION-PROMOTIONS.md §5.8) : le pack, plusieurs articles
-- différents vendus ensemble à un prix (« shampooing + après-shampooing = 8 000 F »).
--
-- Le pack n'est pas un article stocké : chaque composant garde sa ligne de vente, sort de son propre
-- stock en FEFO, garde sa TVA. Le moteur reconnaît les packs complets du panier et répartit la remise
-- (somme des prix de référence - prix du pack) au plus fort reste sur leurs lignes.
--
-- Écart assumé avec le plan : les composants ne vivent pas dans une table à part
-- (promotion_pack_composant) mais dans promotion_produit, qui gagne une quantité. Une seule liste
-- d'articles par promotion, quelle qu'en soit la mécanique : formulaire, contrôles et badge restent
-- les mêmes.

ALTER TABLE promotion DROP CONSTRAINT IF EXISTS promotion_type_ck;
ALTER TABLE promotion ADD CONSTRAINT promotion_type_ck
  CHECK (type IN ('PROMO_POURCENTAGE', 'PROMO_MONTANT', 'PROMO_PRIX_FIXE', 'PROMO_OFFRE_NM', 'PROMO_PACK'));

-- Le prix du pack, en francs.
ALTER TABLE promotion ADD CONSTRAINT promotion_pack_ck
  CHECK (type <> 'PROMO_PACK' OR (valeur IS NOT NULL AND valeur >= 1));

-- Imprimé sur l'étiquette collée sur l'emballage : le scanner du comptoir ajoute alors les
-- composants d'un coup. Généré à la création (« PK » + 8 chiffres, lisible en Code 39).
ALTER TABLE promotion ADD COLUMN IF NOT EXISTS code_barre_pack VARCHAR(20);
CREATE UNIQUE INDEX IF NOT EXISTS promotion_code_barre_pack_uk ON promotion (code_barre_pack)
  WHERE code_barre_pack IS NOT NULL;

ALTER TABLE promotion_produit ADD COLUMN IF NOT EXISTS quantite INTEGER NOT NULL DEFAULT 1;
ALTER TABLE promotion_produit ADD CONSTRAINT promotion_produit_quantite_ck CHECK (quantite >= 1);

COMMENT ON COLUMN promotion.valeur IS
  'Taux en % (PROMO_POURCENTAGE), montant unitaire en F (PROMO_MONTANT), nombre d''unites offertes M (PROMO_OFFRE_NM), prix du pack en F (PROMO_PACK) ; nul pour PROMO_PRIX_FIXE, dont le prix est par produit.';
COMMENT ON COLUMN promotion_produit.quantite IS
  'Unites du composant dans un pack (PROMO_PACK) ; 1 pour les autres mecaniques.';
COMMENT ON COLUMN promotion.code_barre_pack IS
  'Code-barres du pack monte par l''officine (PROMO_PACK) : au scan, le comptoir ajoute les composants.';
