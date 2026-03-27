-- flag de protection pour les lignes modifiées manuellement
-- Quand le batch SEMOIS nocturne (creerSuggestionBatch) met à jour les quantités,
-- il ne touche pas les lignes dont quantite_modifiee_manuel = true.
-- Le pharmacien peut réinitialiser ce flag via l'UI ("Réinitialiser qté").

ALTER TABLE suggestion_line
    ADD COLUMN IF NOT EXISTS quantite_modifiee_manuel boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN suggestion_line.quantite_modifiee_manuel IS
    'true = quantité modifiée manuellement par le pharmacien → le batch ne l écrase pas';

