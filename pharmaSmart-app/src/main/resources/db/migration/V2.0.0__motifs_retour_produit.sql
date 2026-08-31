-- ============================================================================
-- Motifs de retour produit — référentiel manquant
--
-- `motif_retour_produit` existe depuis V1.0.1 mais n'a JAMAIS été alimentée.
-- Or le motif est OBLIGATOIRE dès qu'une quantité à retourner est saisie :
-- retour complet d'un bon, retour ligne à ligne (avoir fournisseur), retour de
-- produits périmés. Sur une installation neuve, la liste des motifs s'ouvre donc
-- sur « Aucun résultat », et aucun retour ne peut être créé — sans qu'aucun
-- message n'explique pourquoi.
--
-- Les libellés retenus sont ceux qu'une officine oppose réellement à son
-- grossiste ; ils restent modifiables et complétables par l'utilisateur.
-- ============================================================================

INSERT INTO motif_retour_produit (libelle)
VALUES
    ('Produit périmé'),
    ('Péremption trop proche'),
    ('Produit endommagé / casse'),
    ('Erreur de préparation du grossiste'),
    ('Produit non commandé'),
    ('Livraison en double'),
    ('Rupture de la chaîne du froid'),
    ('Retrait de lot / rappel'),
    ('Emballage non conforme'),
    ('Erreur de commande de l''officine')
ON CONFLICT (libelle) DO NOTHING;
