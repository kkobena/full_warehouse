-- ============================================================================
-- Motifs d'ajustement de stock — référentiel manquant
--
-- Même constat que pour les motifs de retour (V2.0.0) : `motif_ajustement`
-- existe depuis V1.0.1 et n'a jamais été alimentée, alors que le formulaire
-- d'ajustement le marque obligatoire (« Motif d'ajustement * »). Sur une
-- installation neuve, aucune ligne d'ajustement ne peut donc être ajoutée.
--
-- Le motif n'est pas une formalité : c'est lui qui distingue, dans l'historique
-- des mouvements, une casse d'un vol, une correction de saisie d'un don. Sans
-- lui, un écart de stock n'a plus d'explication six mois plus tard.
-- ============================================================================

INSERT INTO motif_ajustement (libelle)
VALUES
    ('Casse'),
    ('Vol ou disparition'),
    ('Erreur de saisie'),
    ('Erreur de comptage'),
    ('Produit périmé retiré'),
    ('Don ou échantillon'),
    ('Retour client non revendable'),
    ('Consommation interne'),
    ('Régularisation d''inventaire'),
    ('Transfert entre officines')
ON CONFLICT (libelle) DO NOTHING;
